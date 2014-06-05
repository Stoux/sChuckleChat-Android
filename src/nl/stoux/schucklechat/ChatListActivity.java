package nl.stoux.schucklechat;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import nl.stoux.schucklechat.R.layout;
import nl.stoux.schucklechat.chatdata.Chat;
import nl.stoux.schucklechat.chatdata.Refreshable;
import nl.stoux.schucklechat.contacttasks.ChangeNameTask;
import nl.stoux.schucklechat.contacttasks.GetNewTask;
import nl.stoux.schucklechat.data.CachedData;
import nl.stoux.schucklechat.data.Profile;
import nl.stoux.schucklechat.data.User;
import nl.stoux.schucklechat.json.JsonUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class ChatListActivity extends Activity implements Refreshable {
	
	private static Timer getNewTask;
	
	public static final String CHAT_ID = "nl.stoux.sChuckleChat.CHAT_ID";
	public static final String NEW_CHAT = "nl.stoux.sChuckleChat.NEW_CHAT";
	public static final String USER_SELECT_MULTIPLE = "nl.stoux.sChuckleChat.USER_SELECT_MULTIPLE";
	public static final String PREVENT_AUTO_LOGIN = "nl.stoux.sChuckleChat.PREVENT_AUTO_LOGIN";
	
	public static Context context;
	
	private ChatArrayAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat_list);
		
		setTitle("Chuckle Chats");
		
		//Set context
		context = this;
		
		//Users
		HashMap<Integer, User> users = null;
		//=> Load users
		if (CachedData.userMap != null) {
			users = CachedData.userMap;
		} else {
			users = CachedData.userMap = new HashMap<Integer, User>();
						
			//Load file
			JSONObject object = JsonUtil.loadJson(context, "user-index.json");
			if (object != null) {
				try {
					//Check owner
					int ownerID = object.getInt("owner-id");
					if (ownerID != Profile.getUserID()) {
						JsonUtil.removeFile(context, "user-index.json"); //Remove the file
						throw new JSONException("Not the owner");
					}
					
					//Array of users
					JSONArray data = object.getJSONArray("data");
					
					//Loop thru users
					for (int x = 0; x < data.length(); x++) {
						User user = User.parseJson(data.getJSONObject(x));
						if (user != null) {
							if (user.getUserID() != Profile.getUserID()) {
								users.put(user.getUserID(), user);
							}
						}
					}
				} catch (JSONException e) {
					
				}
			}
		}
		
		Log.d("deb", "Users: " + users.size());
		
		
		//New list
		ListView chatListView = (ListView) findViewById(R.id.chat_list);
		List<Chat> chats = null;
		
		//=> Load chats
		if (CachedData.chats != null) {
			chats = CachedData.chats;
			Log.d("deb", "Hier?");
		} else {
			chats = CachedData.chats = new ArrayList<Chat>();
			ConcurrentHashMap<Long, Chat> chatsMap = CachedData.chatsMap = new ConcurrentHashMap<Long, Chat>();
						
			//Load index
			JSONObject object = JsonUtil.loadJson(context, "chat-index.json");
			if (object != null) { //If index available
				try {
					//Check owner
					int ownerID = object.getInt("owner-id");
					if (ownerID != Profile.getUserID()) {
						JsonUtil.removeFile(context, "chat-index.json"); //Remove the file
						throw new JSONException("Not the owner");
					}
					
					JSONArray array = object.getJSONArray("data");
					for (int x = 0; x < array.length(); x++) {
						Chat chat = Chat.parseJson(array.getJSONObject(x)); //Parse the object into a Chat Object
						if (chat != null) {
							Log.d("deb", chat.getChatID() + "|" + chat.getChatDisplayname());
							chats.add(chat); //Add it to the list
							chatsMap.put(chat.getChatID(), chat);
						} else {
							Log.d("deb", "Failed, i: " + x);
						}
					}
				} catch (JSONException e) {
					Log.d("deb", "Chat index: " + e);
				}
			}
		}
		
		Log.d("deb", "Chats: " + chats.size());
		
		//Sort chats
		synchronized(chats) {
			Collections.sort(chats);
		}
		
		//Create adapter
		adapter = new ChatArrayAdapter(this, layout.chatlist_item_layout, chats);
		chatListView.setAdapter(adapter);
				
		//On click
		chatListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
				Chat chat = (Chat) parent.getItemAtPosition(pos);
				
				//Create intent
				Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
				intent.putExtra(CHAT_ID, chat.getChatID());
				startActivity(intent);
			}
		});
		
		//Refresh task (get_new)
		if (getNewTask == null) {
			getNewTask = new Timer();
			getNewTask.scheduleAtFixedRate(new TimerTask() {
				
				@Override
				public void run() {
					if (!CachedData.isChecking && (System.currentTimeMillis() - CachedData.lastCheck) > 10000) { //Check every ~10 seconds
						GetNewTask task = new GetNewTask(context);
						task.execute((Void) null);
					}
				}
			}, 10, 1000);
		}
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		CachedData.activeRefreshable = this;
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		CachedData.activeRefreshable = null;
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
		refreshLists();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (getNewTask != null) {
			getNewTask.cancel();
			getNewTask = null;
		}
		
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
		
		CachedData.activeRefreshable = null;
		CachedData.chats = null;
		CachedData.chatsMap = null;
		CachedData.userMap = null;
		CachedData.chatActivity = null;
		
		Profile.setPassword(null);
		Profile.setUsername(null);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.chat_list, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		switch(id) {
		case R.id.create_chat: case R.id.create_group_chat:
			//Create Intent
			Intent intent = new Intent(getApplicationContext(), UserSelectActivity.class);
			intent.putExtra(USER_SELECT_MULTIPLE, id == R.id.create_group_chat);
			startActivity(intent);
			break;
		case R.id.change_displayname:
			InputDialog.showInputDialog(context, "Vul uw nieuwe gebruikersnaam in", "Weergeef naam", "Annuleren", "Veranderen", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					ChangeNameTask nameTask = new ChangeNameTask(context, InputDialog.getInputText(dialog));
					nameTask.execute((Void) null);
				}
			});
			break;
		case R.id.logout_action:
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder
				.setTitle("Wilt u uitloggen?")
				.setPositiveButton("Ja", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent loginIntent = new Intent(getApplicationContext(), LoginActivity.class);
						loginIntent.putExtra(PREVENT_AUTO_LOGIN, true);
						startActivity(loginIntent);
						finish();
					}
				})
				.setNegativeButton("Nee", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss(); 
					}
				})
				.show();
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}
	
	private class ChatArrayAdapter extends ArrayAdapter<Chat> {
		
		private Context context;
		private List<Chat> objects;
		
		private DateFormat format;
		
	    public ChatArrayAdapter(Context context, int textViewResourceId, List<Chat> objects) {
	          super(context, textViewResourceId, objects);
	          this.context = context;
	          this.objects = objects;
	          
	          format = SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT);
	    }
	    
	    @Override
	    public View getView(int position, View convertView, ViewGroup parent) {
	    	//Create RowView
	    	LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    	View rowView = inflater.inflate(R.layout.chatlist_item_layout, parent, false);
	    	
	    	//Get views
	    	TextView chatname = (TextView) rowView.findViewById(R.id.chat_groupname);
	    	TextView time = (TextView) rowView.findViewById(R.id.chat_last_message_time);
	    	TextView message = (TextView) rowView.findViewById(R.id.chat_last_message);
	    	
	    	//=> Get Chat and fill
	    	Chat chat = objects.get(position);
	    	chatname.setText(chat.getChatDisplayname());
	    	time.setText(format.format(chat.getLastMessageTimestamp()));
	    	message.setText((chat.getLastMessageUserID() == Profile.getUserID() ? "> " : "< ") + chat.getLastMessage());
	    	
	    	//Set image
	    	ImageView image = (ImageView) rowView.findViewById(R.id.chat_icon);
	    	if (chat.isGroupChat()) {
	    		image.setImageResource(R.drawable.ic_action_group);
	    	} else {
	    		image.setImageResource(R.drawable.ic_action_person);
	    	}
	    	return rowView;
	    }
		
	}
	
	@Override
	public void refreshLists() {
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				adapter.notifyDataSetChanged();
				Log.d("deb", "Refreshed chats: " + CachedData.chats.size());
			}
		});
	}
	

}
