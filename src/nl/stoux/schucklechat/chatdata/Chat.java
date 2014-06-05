package nl.stoux.schucklechat.chatdata;

import java.util.ArrayList;
import java.util.List;

import nl.stoux.schucklechat.data.CachedData;
import nl.stoux.schucklechat.data.User;
import nl.stoux.schucklechat.json.Jsonable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class Chat implements Jsonable, Comparable<Chat> {
	
	//Chat info
	private long chatID;
	private String chatDisplayname; //Can also be null if only versus one person
	
	//Users => Groupchat based on which one is null
	private User otherPerson;
	private List<User> otherPeople;
	
	//Last message
	private int lastMessageUserID;
	private String lastMessage;
	private long lastMessageTimestamp;
	
	/**
	 * Create a new Chat with one person
	 * @param chatID The ID of the chat
	 * @param otherPerson The profile of the other person
	 */
	public Chat(long chatID, User otherPerson) {
		this.chatID = chatID;
		this.otherPerson = otherPerson;
		this.chatDisplayname = otherPerson.getDisplayName();
	}
	
	/**
	 * Create a new group Chat
	 * @param chatID The ID of the chat
	 * @param otherPeople The other people in the chat
	 */
	public Chat(long chatID, String chatDisplayname, List<User> otherPeople) {
		this.chatID = chatID;
		this.otherPeople = otherPeople;
		this.chatDisplayname = chatDisplayname;
	} 
	
	/**
	 * Check if this chat is a group chat
	 * @return is a group chat
	 */
	public boolean isGroupChat() {
		return (otherPeople != null);
	}
	
	public void setChatID(long chatID) {
		this.chatID = chatID;
	}
	
	/**
	 * Get the ID of the chat
	 * @return the ID
	 */
	public long getChatID() {
		return chatID;
	}
	
	/**
	 * Get the other person
	 * @return the other person
	 */
	public User getOtherPerson() {
		return otherPerson;
	}
	
	/**
	 * Get other people in this chat
	 * @return The other people
	 */
	public List<User> getOtherPeople() {
		return otherPeople;
	}
	
	/**
	 * Get the display name of this chat
	 * @return the name
	 */
	public String getChatDisplayname() {
		return chatDisplayname;
	}
	
	public String getLastMessage() {
		return lastMessage;
	}

	public void setLastMessage(String lastMessage) {
		this.lastMessage = lastMessage;
	}
	
	
	/**
	 * Get the timestamp of the last send message
	 * @return last message
	 */
	public long getLastMessageTimestamp() {
		return lastMessageTimestamp;
	}
	
	public void setLastMessageTimestamp(long lastMessageTimestamp) {
		this.lastMessageTimestamp = lastMessageTimestamp;
	}
	
	public int getLastMessageUserID() {
		return lastMessageUserID;
	}
	
	public void setLastMessageUserID(int lastMessageUserID) {
		this.lastMessageUserID = lastMessageUserID;
	}
	
	
	/*
	 ********************* 
	 * Message Functions * 
	 ********************* 
	 */
	
	
	
	@Override
	public JSONObject asJson() {
		try {
			JSONObject container = new JSONObject();
			
			//General data
			container.put("chat-id", chatID);
			
			if (lastMessage != null) {
				container.put("last-message-timestamp", lastMessageTimestamp);
				container.put("last-message", lastMessage);
				container.put("last-message-user", lastMessageUserID);
			}
			
			//Depends on Group chat or not
			container.put("group-chat", isGroupChat());
			
			if(isGroupChat()) {
				//=> Group Chat
				container.put("displayname", chatDisplayname);
				//	=> Users
				JSONArray userArray = new JSONArray();
				for (User user : otherPeople) {
					userArray.put(user.getUserID());
				}
				container.put("other-people", userArray);
			} else {
				//=> Not a group chat
				container.put("other-person", otherPerson.getUserID());
			}
			return container;
		} catch (JSONException e) {
			return null;
		}
	}
	
	/**
	 * Parse JSON into a Chat Object
	 * @param object The JSONObject
	 * @return the Chat Object or null
	 */
	public static Chat parseJson(JSONObject object) {
		Chat chat;
		try {
			long chatID = object.getLong("chat-id");
			boolean groupChat = object.getBoolean("group-chat");
			
			if (groupChat) {
				//=> Group Chat
				String displayname = object.getString("displayname");
				
				//	=> Load other people
				List<User> otherPeople = new ArrayList<User>();
				JSONArray otherPeopleArray = object.getJSONArray("other-people");
				for (int x = 0; x < otherPeopleArray.length(); x++) {
					otherPeople.add(CachedData.userMap.get(otherPeopleArray.getInt(x)));
				}
				
				//Create Chat object
				chat = new Chat(chatID, displayname, otherPeople);
			} else {
				//=> Not a group chat
				int userID = object.getInt("other-person");
				User user = CachedData.userMap.get(userID);
				chat = new Chat(chatID, user);
			}
			
			if (object.has("last-message")) {
				//Set last message
				String lastMessage = object.getString("last-message");
				long lastTimestamp = object.getLong("last-message-timestamp");
				int lastMessageUser = object.getInt("last-message-user");
				
				//	=> Set to Chat
				chat.setLastMessage(lastMessage);
				chat.setLastMessageTimestamp(lastTimestamp);
				chat.setLastMessageUserID(lastMessageUser);
			}
			
			//Return
			return chat;
		} catch (JSONException e) {
			Log.d("deb", e.toString());
		}
		return null;
	}
	
	
	@Override
	public int compareTo(Chat another) {
		return Long.valueOf(another.lastMessageTimestamp).compareTo(Long.valueOf(lastMessageTimestamp));
	}

}
