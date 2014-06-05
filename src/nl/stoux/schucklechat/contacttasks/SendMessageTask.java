package nl.stoux.schucklechat.contacttasks;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Collections;

import nl.stoux.schucklechat.ChatActivity;
import nl.stoux.schucklechat.chatdata.Chat;
import nl.stoux.schucklechat.chatdata.SendMessage;
import nl.stoux.schucklechat.data.CachedData;
import nl.stoux.schucklechat.data.ConnectionHandler;
import nl.stoux.schucklechat.data.Profile;
import nl.stoux.schucklechat.json.JsonUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.util.Log;

public class SendMessageTask extends AsyncTask<Void, Void, Void> {

	private ChatActivity context;
	private Chat chat;
	private SendMessage sendMessage;
	private boolean newChat;
	
	public SendMessageTask(ChatActivity chatActivity, Chat chat, SendMessage sendMessage, boolean newChat) {
		this.context = chatActivity;
		this.chat = chat;
		this.sendMessage = sendMessage;
		this.newChat = newChat;
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		Socket s = null;
		BufferedReader reader;
		BufferedWriter writer;
					
		//Try to connect
		try {
			s = new Socket(ConnectionHandler.SERVER_IP, ConnectionHandler.SERVER_PORT);
			reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
			writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
			
			//Send Json new request
			JSONObject container = new JSONObject();
			container.put("type", "send_message");
			
			//Add data
			JSONObject sendData = new JSONObject();
			sendData.put("user-id", sendMessage.getUserID());
			sendData.put("message", sendMessage.getMessage());
			sendData.put("chat-id", chat.getChatID());
			
			long oldTimestamp = sendMessage.getTimestamp();
						
			//Optional data if first message
			if (newChat) {
				sendData.put("new-chat", chat.asJson());
			}
			
			//Set data
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
			if (!type.equalsIgnoreCase("send_messageResponse")) {
				return null;
			}
			
			//Get data
			JSONObject data = response.getJSONObject("data");
			long chatID = data.getLong("chat-id");
			long messageID = data.getLong("message-id");
			long messageTimestamp = data.getLong("message-timestamp");
			
			//Update chat if not given yet
			if (newChat) {
				chat.setChatID(chatID);
				synchronized (CachedData.chats) {
					CachedData.chats.add(chat);
					CachedData.chatsMap.put(chat.getChatID(), chat);
				}
			}
			
			//Update last message
			chat.setLastMessage(sendMessage.getMessage());
			chat.setLastMessageTimestamp(messageTimestamp);
			chat.setLastMessageUserID(Profile.getUserID());
			
			synchronized (CachedData.chats) {
				Collections.sort(CachedData.chats);
			}
			
			Log.d("deb", "From task: Size: " + CachedData.chats.size());
			
			sendMessage.setMessageID(messageID);
			sendMessage.setStatus(SendMessage.RECEIVED);
			sendMessage.setTimestamp(messageTimestamp);
			
			//Update view if still possible
			if (CachedData.chatActivity != null) {
				if (CachedData.chatActivity.chat.getChatID() == chatID) {
					CachedData.chatActivity.chatActivity.refreshLists();
				}
			} else {
				JSONObject json;
				if (chat.isGroupChat()) { //=> Load correct JSON file
					json = JsonUtil.loadJson(context, "groupchats", "g" + chat.getChatID() + ".json");
				} else {
					json = JsonUtil.loadJson(context, "chats", "u" + chat.getChatID() + ".json");
				}
				JSONArray array = json.getJSONArray("data");
				for (int i = 0; i < array.length(); i++) {
					JSONObject msg = array.getJSONObject(i);
					if (msg.getLong("timestamp") == oldTimestamp) {
						msg.put("timestamp", messageTimestamp);
						msg.put("message-id", messageID);
						msg.put("status", SendMessage.RECEIVED);
						break;
					}
				}
				if (chat.isGroupChat()) { //=> Load correct JSON file
					JsonUtil.writeJson(context, json, "groupchats", "g" + chat.getChatID() + ".json");
				} else {
					JsonUtil.writeJson(context, json, "chats", "u" + chat.getChatID() + ".json");
				}
				
				if (CachedData.activeRefreshable != null) {
					CachedData.activeRefreshable.refreshLists();
				}
			}
			
			context.creatingChat = false;
			context.newChat = false;
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
		}
		
		return null;
	}
	
}
