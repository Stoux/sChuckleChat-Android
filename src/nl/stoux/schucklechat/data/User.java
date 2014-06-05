package nl.stoux.schucklechat.data;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import nl.stoux.schucklechat.json.Jsonable;

public class User implements Jsonable,Comparable<User> {
	
	private int userID;
	private String username;
	private String displayName;
	
	public User(int userID, String username, String displayName) {
		this.userID = userID;
		this.username = username;
		this.displayName = displayName;
	}
	
	/**
	 * Get the Displayname
	 * @return the name
	 */
	public String getDisplayName() {
		return displayName;
	}
	
	/**
	 * Get the UserID
	 * @return the ID
	 */
	public int getUserID() {
		return userID;
	}
	
	/**
	 * Get the Username of this user
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}
	
	
	@Override
	public JSONObject asJson() {
		try {
			JSONObject json = new JSONObject();
			json.put("user-id", userID);
			json.put("username", username);
			json.put("displayname", displayName);
			return json;
		} catch(JSONException e) {
			return null;
		}
	}
	
	/**
	 * Parse a JSONObject into a User object
	 * @param object The JSONObject
	 * @return The User object or null
	 */
	public static User parseJson(JSONObject object) {
		try {
			int userID = object.getInt("user-id");
			String username = object.getString("username");
			String displayname = object.getString("displayname");
			return new User(userID, username, displayname);
		} catch (JSONException e) {
			Log.d("json", "Failed to parse user: " + object.toString() + " | Error: "+ e);
			return null;
		}
	}
	
	@Override
	public int compareTo(User another) {
		return getDisplayName().toLowerCase().compareTo(another.getDisplayName().toLowerCase());
	}
	

}
