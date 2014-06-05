package nl.stoux.schucklechat.chatdata;

import nl.stoux.schucklechat.json.Jsonable;

import org.json.JSONException;
import org.json.JSONObject;

public class Message extends ChatItem implements Jsonable {
	
	/**
	 * The ID of the message
	 */
	private long messageID;
	
	/**
	 * ID of the user that send this message
	 */
	private int userID;
	
	/**
	 * The actual message
	 */
	private String message;
	
	/**
	 * Timestamp of the message being sent
	 */
	private long timestamp;
	
	public Message(long messageID, int userID, String message, long timestamp) {
		this.messageID = messageID;
		this.userID = userID;
		this.message = message;
		this.timestamp = timestamp;
	}
	
	/**
	 * The message ID
	 * @return the ID
	 */
	public long getMessageID() {
		return messageID;
	}
	
	public void setMessageID(long messageID) {
		this.messageID = messageID;
	}
	
	/**
	 * Get the ID of the user that send this message
	 * @return the ID
	 */
	public int getUserID() {
		return userID;
	}
	
	/**
	 * Get the send message
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}
	
	/**
	 * The timestamp this message was aknowleged on the server
	 * @return the timestamp
	 */
	public long getTimestamp() {
		return timestamp;
	}
	
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	
	@Override
	public JSONObject asJson() {
		try {
			JSONObject container = new JSONObject();
			
			//General data
			container.put("message-id", messageID);
			container.put("user-id", userID);
			container.put("timestamp", timestamp);
			container.put("message", message);
			return container;
		} catch (JSONException e) {
			return null;
		}
	}
	
	/**
	 * Parse a JSONObject into a Message
	 * @param object The JSONObject
	 * @return The Message object (can be SendMessage object) or null if failed
	 */
	public static Message parseJson(JSONObject object) {
		try {
			long messageID = object.getLong("message-id");
			int userID = object.getInt("user-id");
			long timestamp = object.getLong("timestamp");
			String message = object.getString("message");
			
			if (object.has("status")) {
				int status = object.getInt("status");
				return new SendMessage(messageID, userID, message, timestamp, status);
			} else {
				return new Message(messageID, userID, message, timestamp);
			}
		} catch (JSONException e) {
			return null;
		}
	}

}
