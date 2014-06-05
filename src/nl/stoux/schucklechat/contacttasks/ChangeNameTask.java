package nl.stoux.schucklechat.contacttasks;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import nl.stoux.schucklechat.data.ConnectionHandler;
import nl.stoux.schucklechat.data.Profile;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

public class ChangeNameTask extends AsyncTask<Void, Void, Integer> {

	private Context context;
	private String newDisplayname;
	
	public ChangeNameTask(Context context, String newDisplayname) {
		this.context = context;
		this.newDisplayname = newDisplayname;
	}
	
	@Override
	protected Integer doInBackground(Void... params) {
		Socket s = null;
		BufferedReader reader;
		BufferedWriter writer;
					
		//Try to connect
		try {
			s = new Socket(ConnectionHandler.SERVER_IP, ConnectionHandler.SERVER_PORT);
			reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
			writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
			
			//Send Json new request
			JSONObject container = new JSONObject();
			container.put("type", "change_displayname");
			
			//=> Data
			JSONObject sendData = new JSONObject();
			sendData.put("user-id", Profile.getUserID());
			sendData.put("new-displayname", newDisplayname);
			container.put("data", sendData);
			
			//=> Send
			writer.write(container.toString());
			writer.newLine();
			writer.flush();
			
			//Wait for response
			String responseString = reader.readLine();
			s.close();
						
			//As JSON
			JSONObject response = new JSONObject(responseString);
			
			//Type check
			String type = response.getString("type");
			if (!type.equalsIgnoreCase("change_displaynameResponse")) {
				return 0;
			}
			
			//Get data
			JSONObject data = response.getJSONObject("data");
			int responseCode = data.getInt("code");
			return responseCode;
		} catch (IOException e) {
			
		} catch (JSONException e) {
			
		} finally {
			try {
				s.close();
			} catch (IOException e) {}
		}
		return 0;
	}
	
	@Override
	protected void onPostExecute(Integer result) {
		if (result == 202) {
			Toast.makeText(context, "Naam verandered: " + newDisplayname, Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(context, "Naam veranderen mislukt!", Toast.LENGTH_LONG).show();
		}
	}
	
}
