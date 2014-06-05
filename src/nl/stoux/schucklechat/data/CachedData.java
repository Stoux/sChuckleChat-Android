package nl.stoux.schucklechat.data;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import nl.stoux.schucklechat.ChatActivity;
import nl.stoux.schucklechat.chatdata.Chat;
import nl.stoux.schucklechat.chatdata.ChatItem;
import nl.stoux.schucklechat.chatdata.Message;
import nl.stoux.schucklechat.chatdata.Refreshable;

public class CachedData {
	
	/**
	 * Map containing all Users
	 * K:[ID of the User] => V:[User object]
	 */
	public static HashMap<Integer, User> userMap;
	
	/**
	 * List off all Chats
	 * Ordered by time
	 */
	public static List<Chat> chats;
	
	/**
	 * Map containing all chats
	 * K:[ID of Chat] => V:[Chat object]
	 */
	public static ConcurrentHashMap<Long, Chat> chatsMap;
	
	/**
	 * New chat
	 */
	public static Chat newChat;

	/**
	 * Timestamp of the last check
	 */
	public static long lastCheck;
	
	/**
	 * Boolean is check is currently running
	 */
	public static boolean isChecking;
	
	/**
	 * An active refreshable
	 */
	public static Refreshable activeRefreshable;
	
	/**
	 * A possible active chat activity
	 */
	public static ActiveChatActivity chatActivity;
	
	
	public class ActiveChatActivity {
		
		public Chat chat;
		public ChatActivity chatActivity;
		
		public ActiveChatActivity(Chat chat, ChatActivity activity) {
			this.chat = chat;
			this.chatActivity = activity;
		}
		
		/**
		 * Get the ID of the chat
		 * @return the ID of the chat
		 */
		public long getChatID() {
			return chat.getChatID();
		}
		
		/**
		 * Add a message to the Activity
		 * @param m The message
		 */
		public void addMessage(Message m) {
			chatActivity.addMessage(m);
		}
		
	}
	
}
