package nl.stoux.schucklechat.contacttasks;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import nl.stoux.schucklechat.chatdata.Chat;
import nl.stoux.schucklechat.chatdata.Message;
import nl.stoux.schucklechat.data.CachedData;
import nl.stoux.schucklechat.data.ConnectionHandler;
import nl.stoux.schucklechat.data.Profile;
import nl.stoux.schucklechat.data.User;
import nl.stoux.schucklechat.json.JsonUtil;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public class GetNewTask extends AsyncTask<Void, Void, Void> {

	public GetNewTask(Context context) {
		this.context = context;
	}
	
	private Context context;
	private Socket s;
	
	@Override
	protected Void doInBackground(Void... params) {
		CachedData.isChecking = true;
		
		s = null;
		BufferedReader reader;
		BufferedWriter writer;
					
		//Try to connect
		try {
			s = new Socket(ConnectionHandler.SERVER_IP, ConnectionHandler.SERVER_PORT);
			reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
			writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
			
			//Send Json new request
			JSONObject container = new JSONObject();
			container.put("type", "get_new");
			
			JSONObject sendData = new JSONObject();
			sendData.put("user-id", Profile.getUserID());
			sendData.put("last-check", CachedData.lastCheck);
			container.put("data", sendData);
			
			//=> Send
			writer.write(container.toString());
			writer.newLine();
			writer.flush();
			
			//Wait for response
			String responseString = reader.readLine();
			s.close();
						
			
			//As JSON
			JSONObject response = new JSONObject(responseString);
			
			//Type check
			String type = response.getString("type");
			if (!type.equalsIgnoreCase("get_newResponse")) {
				return null;
			}
			
			//Get data
			JSONObject data = response.getJSONObject("data");
			
			//=> New Last Check
			CachedData.lastCheck = data.getLong("last-check");
			try {
				JSONObject lastCredentials = JsonUtil.loadJson(context, "last-credentials.json");
				lastCredentials.put("last-check", CachedData.lastCheck);
				JsonUtil.writeJson(context, lastCredentials, "last-credentials.json");
			} catch (Exception e) {
				
			}
			
			
			
			//=> Users
			boolean saveUserMap = false;
			JSONArray userChanges = data.getJSONArray("users");
			int userChangesLength = userChanges.length();
			Log.d("task", "New users changes: " + userChangesLength);
			if (userChangesLength > 0) {
				synchronized (CachedData.userMap) { //Lock usermap
					for (int i = 0; i < userChangesLength; i++) { //Loop thru changes
						JSONObject userObj = userChanges.getJSONObject(i);
						User changedUser = User.parseJson(userObj);
						if (changedUser != null) {
							CachedData.userMap.put(changedUser.getUserID(), changedUser);
						} else {
							Log.d("task", "User failed to parse: " + userObj.toString());
						}
					}
				}
				saveUserMap = true;
			}
			
			//=> Chats
			boolean sortChats = false;
			JSONArray newChats = data.getJSONArray("chats");
			int newChatsLength = newChats.length();
			if (newChatsLength > 0) {
				synchronized(CachedData.chats) { //Lock chats
					for (int i = 0; i < newChatsLength; i++) {
						JSONObject chatObj = newChats.getJSONObject(i); //Parse object
						Chat newChat = Chat.parseJson(chatObj);
						if (newChat != null) {
							CachedData.chats.add(newChat);
							CachedData.chatsMap.put(newChat.getChatID(), newChat);
							Log.d("deb", "Found new chat: " + newChat.getChatID());
						} else {
							Log.d("task", "Chat failed to parse: " + chatObj.toString());
						}
					}
				}
				sortChats = true;
			}
			
			//=> Messages
			HashMap<Long, HashSet<Message>> newMsgMap = new HashMap<Long, HashSet<Message>>();
			JSONArray newMessages = data.getJSONArray("messages");
			int newMessagesLength = newMessages.length();
			if (newMessagesLength > 0) {
				synchronized (CachedData.chats) { //Lock chats
					for (int i = 0; i < newMessagesLength; i++) {
						JSONObject messageObj = newMessages.getJSONObject(i);
						Message newMessage = Message.parseJson(messageObj);
						if (newMessage != null) {
							try {
								//Get chatID
								long chatID = messageObj.getLong("chat-id");
								
								//Update the chat
								Log.d("deb", "New message (" + newMessage.getMessageID() + ") in chat: " + chatID);
								Chat c = CachedData.chatsMap.get(chatID);
								c.setLastMessage(newMessage.getMessage());
								c.setLastMessageTimestamp(newMessage.getTimestamp());
								c.setLastMessageUserID(newMessage.getUserID());
																
								//Set boolean
								sortChats = true;
								
								boolean addToMap = true;
								
								//Check Current Chat Activity
								if (CachedData.chatActivity != null) {
									if (CachedData.chatActivity.getChatID() == chatID) {
										CachedData.chatActivity.addMessage(newMessage);
										addToMap = false;
									}
								}
								
								if (addToMap) {
									//Add to map
									HashSet<Message> chatMessages = newMsgMap.get(chatID);
									if (chatMessages == null) {
										chatMessages = new HashSet<Message>();
										newMsgMap.put(chatID, chatMessages);
									}
									chatMessages.add(newMessage);
								}
								
							} catch (JSONException e) {
								
							}
						} else {
							Log.d("task", "Message failed to parse: " + messageObj.toString());
						}
					}
				}
			}
			
			
			
			if (saveUserMap) {
				//Save users
				try {
					JSONObject userObj = new JSONObject();
					userObj.put("owner-id", Profile.getUserID());
					
					JSONArray userArray = new JSONArray();
					synchronized (CachedData.userMap) {
						for (User u : CachedData.userMap.values()) {
							userArray.put(u.asJson());
						}
					}
					userObj.put("data", userArray);
					JsonUtil.writeJson(context, userObj, "user-index.json");
				} catch (JSONException e) {}
			}
			
			if (sortChats) {				
				//Save chats
				try {
					JSONObject chatsObj = new JSONObject();
					chatsObj.put("owner-id", Profile.getUserID());
					
					JSONArray chatsArray = new JSONArray();
					synchronized (CachedData.chats) {
						//Sort them first
						Collections.sort(CachedData.chats);
						
						//Save them
						for (Chat c : CachedData.chats) {
							chatsArray.put(c.asJson());
						}
					}
					chatsObj.put("data", chatsArray);
					JsonUtil.writeJson(context, chatsObj, "chat-index.json");
					
				} catch (JSONException e) {}
			}
			
			if (newMsgMap != null) {
				//Save new messages
				for (Entry<Long, HashSet<Message>> entry : newMsgMap.entrySet()) {
					Chat c = CachedData.chatsMap.get(entry.getKey());
					
					JSONObject json;
					//Load json
					if (c.isGroupChat()) { //=> Load correct JSON file
						json = JsonUtil.loadJson(context, "groupchats", "g" + c.getChatID() + ".json");
					} else {
						json = JsonUtil.loadJson(context, "chats", "u" + c.getChatID() + ".json");
					}
					
					if (json != null) { //Reset json if not the owner
						int ownerID = json.getInt("owner-id");
						if (ownerID != Profile.getUserID()) {
							json = null;
						}
					}
					
					//Check if loaded
					if (json == null) { //=> There was no saved file yet
						json = new JSONObject();
						json.put("owner-id", Profile.getUserID());
						json.put("data", new JSONArray());
					}
					
					//Add new messages
					JSONArray messages = json.getJSONArray("data");
					for (Message m : entry.getValue()) {
						messages.put(m.asJson());
					}
					
					//Save
					if (c.isGroupChat()) {
						JsonUtil.writeJson(context, json, "groupchats", "g" + c.getChatID() + ".json");
					} else {
						JsonUtil.writeJson(context, json, "chats", "u" + c.getChatID() + ".json");
					}
				}
			}
			
			//Refresh activity if any
			if (saveUserMap || sortChats || newMsgMap != null) {
				if (CachedData.activeRefreshable != null) {
					CachedData.activeRefreshable.refreshLists();
				}
			}
		} catch (IOException e) {
			
		} catch (JSONException e) { 
			
		} finally {
			try {
				if (s != null) {
					if (!s.isClosed()) {
						s.close();
					}
				}
			} catch (IOException e) {}
			CachedData.isChecking = false;
		}
		return null;
	}
	
	@Override
	protected void onCancelled(Void result) {
		super.onCancelled(result);
		if (s != null) {
			try { s.close(); } catch (IOException e) {}
		}
	}
	
	

}
