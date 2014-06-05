
package nl.stoux.schucklechat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import nl.stoux.schucklechat.chatdata.Chat;
import nl.stoux.schucklechat.chatdata.Refreshable;
import nl.stoux.schucklechat.data.CachedData;
import nl.stoux.schucklechat.data.User;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;

public class UserSelectActivity extends ListActivity implements Refreshable {
	
	private final Context context = this;
	
	private boolean multipleSelect;
	private HashSet<Integer> checkedUsers;
	
	private UserArrayAdapter adapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_user_select);
		
		//Check if multiple
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			multipleSelect = extras.getBoolean(ChatListActivity.USER_SELECT_MULTIPLE);
		} else {
			multipleSelect = false;
		}
		
		
		//Multiple
		if (multipleSelect) {
			checkedUsers = new HashSet<Integer>();
			setTitle("Selecteer 2+ personen");
		} else {
			setTitle("Selecteer persoon");
		}
		
		//Create users list & sort
		ArrayList<User> users = new ArrayList<User>(CachedData.userMap.values());
		Collections.sort(users);
		
		//Set adapter
		adapter = new UserArrayAdapter(getApplicationContext(), users);
		setListAdapter(adapter);
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (!multipleSelect) {
			User user = ((UserArrayAdapter) getListAdapter()).getItem(position);
			//Create new temp chat
			Chat newChat = new Chat(-1, user);
			CachedData.newChat = newChat;
			
			//Create intent
			Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
			intent.putExtra(ChatListActivity.NEW_CHAT, true);
			startActivity(intent);
			finish();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.user_select, menu);
		if (multipleSelect) {
			MenuItem item = menu.findItem(R.id.user_select_ready);
			item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			item.setVisible(checkedUsers.size() > 1);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		switch(id) {
		case R.id.user_select_ready:
			InputDialog.showInputDialog(context, "Vul de naam van de groep in", "Groepsnaam", "Annuleren", "OK", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					//Create new GroupChat
					ArrayList<User> selectedUsers = new ArrayList<User>();
					for (Integer userID : checkedUsers) {
						selectedUsers.add(CachedData.userMap.get(userID));
					}
					Chat newChat = new Chat(-1, InputDialog.getInputText(dialog), selectedUsers);
					
					//Set cached
					CachedData.newChat = newChat;
					
					//Create intent
					Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
					intent.putExtra(ChatListActivity.NEW_CHAT, true);
					startActivity(intent);
					finish();
				}
			});
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public class UserArrayAdapter extends ArrayAdapter<User> {
		
		private final Context context;
		
		public UserArrayAdapter(Context context, List<User> users) {
			super(context, R.layout.user_select_list_item, users);
			this.context = context;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
	    	//Create RowView
	    	LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    	View rowView = inflater.inflate(R.layout.user_select_list_item, parent, false);
	    	
	    	//Get user
	    	final User u = getItem(position); 
	    	
	    	//Set data
	    	TextView displaynameView = (TextView) rowView.findViewById(R.id.user_displayname);
	    	displaynameView.setText(u.getDisplayName());
	    	TextView emailView = (TextView) rowView.findViewById(R.id.user_email);
	    	emailView.setText(u.getUsername());
	    	
	    	//Checkbox
	    	CheckBox checkboxView = (CheckBox) rowView.findViewById(R.id.user_checkbox);
	    	if (multipleSelect) {
	    		//Set checked status
	    		checkboxView.setChecked(checkedUsers.contains(u.getUserID()));
	    		
	    		//Change listener
	    		checkboxView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						if (isChecked) {
							checkedUsers.add(u.getUserID());
						} else {
							checkedUsers.remove(u.getUserID());
						}
						invalidateOptionsMenu();
					}
				});
	    		
	    	} else {
	    		checkboxView.setVisibility(View.INVISIBLE);
	    	}
			return rowView;
		}
		
		
	}
	
	
	@Override
	public void refreshLists() {
		ArrayList<User> users = new ArrayList<User>(CachedData.userMap.values());
		Collections.sort(users);
		
		adapter.clear();
		adapter.addAll(users);
		adapter.notifyDataSetChanged();
	}
	
}
