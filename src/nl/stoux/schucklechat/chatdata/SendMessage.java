package nl.stoux.schucklechat.chatdata;

import org.json.JSONException;
import org.json.JSONObject;

public class SendMessage extends Message {
	
	//Statics
	public final static int SENDING = 1;
	public final static int SEND = 2;
	public final static int RECEIVED = 3;
	public final static int READ = 4;
	
	//Status of send message
	private int status;
	
	public SendMessage(long messageID, int userID, String message, long timestamp) {
		super(messageID, userID, message, timestamp);
		status = SENDING;
	}
	
	public SendMessage(long messageID, int userID, String message, long timestamp, int status) {
		super(messageID, userID, message, timestamp);
		this.status = status;
	}
	
	/**
	 * Get the status of the send message
	 * @return The message
	 */
	public int getStatus() {
		return status;
	}
	
	@Override
	public JSONObject asJson() {
		JSONObject object = super.asJson();
		if (object != null) {
			try {
				object.put("status", status);
			} catch (JSONException e) {}
		}
		return object;
	}
	
	public void setStatus(int status) {
		this.status = status;
	}
	
}
