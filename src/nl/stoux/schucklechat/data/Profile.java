package nl.stoux.schucklechat.data;

public class Profile {

	private static int userID;
	private static String username;
	private static String password;
	
	public static void setPassword(String password) {
		Profile.password = password;
	}
	public static void setUsername(String username) {
		Profile.username = username;
	}
	public static String getPassword() {
		return password;
	}
	public static String getUsername() {
		return username;
	}
	public static void setUserID(int userID) {
		Profile.userID = userID;
	}
	public static int getUserID() {
		return userID;
	}

}
