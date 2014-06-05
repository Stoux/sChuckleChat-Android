package nl.stoux.schucklechat.chatdata;

public abstract class ChatItem implements Comparable<ChatItem>  {

	/**
	 * Get the timestamp of this item
	 * @return the timestamp
	 */
	public abstract long getTimestamp();
	
	@Override
	public int compareTo(ChatItem another) {
		return Long.valueOf(getTimestamp()).compareTo(Long.valueOf(another.getTimestamp()));
	}
	
}
