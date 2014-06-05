package nl.stoux.schucklechat;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import nl.stoux.schucklechat.chatdata.Chat;
import nl.stoux.schucklechat.chatdata.ChatItem;
import nl.stoux.schucklechat.chatdata.Message;
import nl.stoux.schucklechat.chatdata.Refreshable;
import nl.stoux.schucklechat.chatdata.SendMessage;
import nl.stoux.schucklechat.contacttasks.SendMessageTask;
import nl.stoux.schucklechat.data.CachedData;
import nl.stoux.schucklechat.data.Profile;
import nl.stoux.schucklechat.data.User;
import nl.stoux.schucklechat.json.JsonUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ChatActivity extends Activity implements Refreshable {

	private Context context = this;
	
	//Name colors
	private static final String[] COLORS = new String[]{"#CC0000", "#FF8800", "#669900", "#9933CC", "#0099CC", "#FF4444", "#FFBB33", "#99CC00", "#AA66CC", "#33B5E5"};
	private HashMap<Integer, Integer> colorMap;
	
	public boolean newChat;
	public boolean creatingChat = false;
	private Chat chat;
	
	//Adapter
	private ChatMessagesArrayAdapter adapter;
	
	//Last message day check
	private DateFormat dayYearFormat;
	private int actualLastDay;
	
	//JsonArray with messages
	private JSONArray jsonArrayMessages;
	
	//List with new messagess
	private ArrayList<Message> newMessages;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);
		
		//Get chat
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			if (extras.containsKey(ChatListActivity.CHAT_ID)) {
				long chatID = extras.getLong(ChatListActivity.CHAT_ID);
				for (Chat c : CachedData.chats) {
					if (c.getChatID() == chatID) {
						chat = c;
						break;
					}
				}
				newChat = false;
			} else if (extras.containsKey(ChatListActivity.NEW_CHAT)) {
				chat = CachedData.newChat;
				CachedData.newChat = null;
				newChat = true;
			}			
		}
		
		if (chat == null) {
			Toast.makeText(getApplicationContext(), "Kon de chat niet laden!", Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		
		//Change actionbar title
		setTitle(chat.getChatDisplayname());
		
		//Map the colors
		colorMap = new HashMap<Integer, Integer>();
		if (chat.isGroupChat()) {
			int x = 0;
			for (User user : chat.getOtherPeople()) {
				colorMap.put(user.getUserID(), Color.parseColor(COLORS[x++]));
			}
		} else {
			colorMap.put(chat.getOtherPerson().getUserID(), Color.parseColor(COLORS[0]));
		}
		
		//List with all message objects
		List<ChatItem> objects = new ArrayList<ChatItem>();
		
		JSONObject json;
		//Load messages
		if (chat.isGroupChat()) { //=> Load correct JSON file
			json = JsonUtil.loadJson(context, "groupchats", "g" + chat.getChatID() + ".json");
		} else {
			json = JsonUtil.loadJson(context, "chats", "u" + chat.getChatID() + ".json");
		}
		//=> Get all messages
		if (json != null) {
			try {
				//Check owner
				int owner = json.getInt("owner-id");
				if (owner != Profile.getUserID()) {
					if (chat.isGroupChat()) { //=> Load correct JSON file
						JsonUtil.removeFile(context, "groupchats", "g" + chat.getChatID() + ".json");
					} else {
						JsonUtil.removeFile(context, "chats", "u" + chat.getChatID() + ".json");
					}
					Toast.makeText(getApplicationContext(), "Kon de chat niet laden!", Toast.LENGTH_LONG).show();
					finish();
					return;
				}
				
				JSONArray array = json.getJSONArray("data");
				
				//Loop thru array
				for (int i = 0; i < array.length(); i++) {
					JSONObject obj = array.getJSONObject(i);
					Message m = Message.parseJson(obj);
					if (m != null) {
						objects.add(m);
					} else {
						Log.d("deb", "Failed to add.");
					}
				}
				
				jsonArrayMessages = array;
			} catch (JSONException e) {
				Log.d("deb", "Failed to read Object: " + e);
				jsonArrayMessages = new JSONArray();
			}
		} else {
			jsonArrayMessages = new JSONArray();
			Log.d("deb", "Json was null");
		}
		
		//Sort messages
		Collections.sort(objects);
		
		//Add days
		dayYearFormat = new SimpleDateFormat("D", Locale.getDefault());
		
		//=> Loop thru objects to find new dates 
		int objectsLength = objects.size();
		
		//=> Get last message
		int lastDay = 0;
		if (objectsLength - 1 >= 0) { //Determine last day
			try {
				lastDay = Integer.parseInt(dayYearFormat.format(objects.get(objectsLength -1).getTimestamp()));
				actualLastDay = lastDay;
			} catch (NumberFormatException e) {}
		}
		
		for (int i = objectsLength - 2; i >= 0; i--) { //Check for days
			ChatItem item = objects.get(i);
			try {
				int day = Integer.parseInt(dayYearFormat.format(item.getTimestamp()));
				if (day != lastDay) {
					lastDay = day;
					
					final ChatItem nextDayItem = objects.get(i+1);
					
					objects.add(i + 1, new ChatItem() {
						
						@Override
						public long getTimestamp() {
							return nextDayItem.getTimestamp();
						}
					});
				}
			} catch (NumberFormatException e) {}
		}
		
		if (objectsLength > 0) { //First day
			final ChatItem item = objects.get(0);
			objects.add(0, new ChatItem() {
				@Override
				public long getTimestamp() {
					return item.getTimestamp();
				}
			});
		}
		
		
		//Get listview
		final ListView messageListView = (ListView) findViewById(R.id.message_list);
		
		//Create arrayAdapter
		adapter = new ChatMessagesArrayAdapter(getApplicationContext(), R.layout.chat_message_item, objects);
		messageListView.setAdapter(adapter);
		
		//Scroll to bottom of ListView
		messageListView.post(new Runnable() {
		        @Override
		        public void run() {
		            // Select the last row so it will scroll into view...
		        	messageListView.setSelection(adapter.getCount() - 1);
		        }
		    });
		
		//Text view
		final EditText inputText = (EditText) findViewById(R.id.message_field);
		
		//Add listener to send message button
		ImageButton button = (ImageButton) findViewById(R.id.send_message_button);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String entered = inputText.getText().toString();
				if (!entered.isEmpty()) {
					//Reset input
					inputText.setText("");
					
					//Add message
					final SendMessage sm = new SendMessage(-1, Profile.getUserID(), entered, System.currentTimeMillis(), SendMessage.SENDING);
					addMessage(sm);
					
						if (newChat && creatingChat) {
							new Thread(new Runnable() {
								
								@Override
								public void run() {
									while (creatingChat) {
										try {
											Thread.sleep(500);
										} catch (InterruptedException e) {}
									}
									new SendMessageTask((ChatActivity) context, chat, sm, newChat).execute((Void)null);
								}
							}).start();
						} else {
							new SendMessageTask((ChatActivity) context, chat, sm, newChat).execute((Void)null);
						}
					
				}
			}
		});
		
		//Set active
		CachedData.chatActivity = (new CachedData()).new ActiveChatActivity(chat, this);
		
		//New messages
		newMessages = new ArrayList<Message>();
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
		refreshLists();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		CachedData.chatActivity = null;
		
		if (newMessages == null) return;
		
		if (jsonArrayMessages == null) {
			Log.d("deb", "Dit is belachelijk");
			return;
		}
		
		//Save new messages
		for (Message m : newMessages) {
			JSONObject json = m.asJson();
			if (m == null) {
				Log.d("deb", "lol null");
			}
			jsonArrayMessages.put(json);
		}
		
		try {
			JSONObject json = new JSONObject();
			json.put("owner-id", Profile.getUserID());
			json.put("data", jsonArrayMessages);
			
			if (chat.isGroupChat()) { //=> Load correct JSON file
				JsonUtil.writeJson(context, json, "groupchats", "g" + chat.getChatID() + ".json");
			} else {
				JsonUtil.writeJson(context, json, "chats", "u" + chat.getChatID() + ".json");
			}
		} catch (JSONException e) {
			
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.chat, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	
	private class ChatMessagesArrayAdapter extends ArrayAdapter<ChatItem> {
		
		private Context context;
		private List<ChatItem> objects;
		
		private DateFormat format;
		private DateFormat dayFormat;
		
	    public ChatMessagesArrayAdapter(Context context, int textViewResourceId, List<ChatItem> objects) {
	          super(context, textViewResourceId, objects);
	          this.context = context;
	          this.objects = objects;
	          
	          format = new SimpleDateFormat("HH:mm", Locale.getDefault());
	          dayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
	    }
	    
	    @Override
	    public View getView(int position, View convertView, ViewGroup parent) {
	    	LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    		    	
	    	//Object => Check type
	    	ChatItem o = objects.get(position);
	    	
	    	if (o instanceof Message) { //Its a message
	    		Message m = (Message) o;
	    		
	    		Log.d("deb", "Type: " + o.getClass().getName() + " | M: " + m.getMessage() + " | User: " + m.getUserID() + " | mID: "+ m.getMessageID());
	    		
	    		//Check who send it
	    		boolean thisUser;
	    		int layout;
	    		if (m.getUserID() == Profile.getUserID()) { //This user sends a message
	    			thisUser = true;
	    			layout = R.layout.chat_message_item_send;
	    		} else {
	    			thisUser = false;
	    			layout = R.layout.chat_message_item;
	    		}
	    		
	    		//Inflate row
	    		View rowView = inflater.inflate(layout, parent, false);
	    		
	    		//Fill views
	    		if (!thisUser) { //=> Person name
	    			//Get other user
	    			User user = CachedData.userMap.get(m.getUserID());
	    			
	    			//Fill person view
	    			TextView personView = (TextView) rowView.findViewById(R.id.chat_person_name);
	    			personView.setText(user.getDisplayName());
	    			
	    			//Set color
	    			personView.setTextColor(colorMap.get(user.getUserID()));
	    		}
	    		
	    		//=> Message
	    		TextView messageView = (TextView) rowView.findViewById(R.id.chat_message);
	    		messageView.setText(m.getMessage());
	    		
	    		//=> Timestamp
	    		TextView timestampView = (TextView) rowView.findViewById(R.id.chat_timestamp);
	    		timestampView.setText(format.format(m.getTimestamp()));
	    		
	    		if (thisUser) { //=> Status Icon
	    			//Object must be a SendMessage
	    			SendMessage sm = (SendMessage) m;
	    			
	    			//Get image
	    			ImageView statusIconView = (ImageView) rowView.findViewById(R.id.chat_status_icon);
	    			
	    			int statusIcon;
	    			//Base status icon on status
	    			switch(sm.getStatus()) {
	    			case SendMessage.SENDING:
	    				statusIcon = R.drawable.clock;
	    				break;
	    			case SendMessage.SEND:
	    				statusIcon = R.drawable.single_correct;
	    				break;
	    			case SendMessage.RECEIVED: 
	    				statusIcon = R.drawable.single_correct; //TODO => Double correct
	    				break;
	    			case SendMessage.READ: 
	    				statusIcon = R.drawable.single_correct; //TODO => Triple correct
	    				break;
	    			default:
	    				statusIcon = R.drawable.clock;
	    			}
	    			
	    			//Set image
	    			statusIconView.setColorFilter(android.R.color.primary_text_light, Mode.SRC_ATOP);
	    			statusIconView.setImageResource(statusIcon);
	    		}
	    		
	    		return rowView;
	    	} else {
	    		//Just a timestamp
	    		View rowView = inflater.inflate(R.layout.chat_message_day, parent, false);
	    		
	    		//Set day
	    		TextView dayView = (TextView) rowView.findViewById(R.id.message_day_text);
	    		dayView.setText(dayFormat.format(o.getTimestamp()));
	    		return rowView;
	    	}
	    }
		
	}
	
	
	@Override
	public void refreshLists() {
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				adapter.notifyDataSetChanged();
			}
		});
	}
	
	public void addMessage(final Message m) {
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				int day = Integer.parseInt(dayYearFormat.format(m.getTimestamp()));
				if (day != actualLastDay) {
					actualLastDay = day;
					ChatItem item = new ChatItem() {
						
						@Override
						public long getTimestamp() {
							return m.getTimestamp();
						}
					};
					adapter.add(item);
				}
				adapter.add(m);
				adapter.notifyDataSetChanged();
				
				newMessages.add(m);
			}
		});
	}
	
	

}
