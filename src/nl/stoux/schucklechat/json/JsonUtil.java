package nl.stoux.schucklechat.json;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

public class JsonUtil {

	/**
	 * Load a JSONOBject from the Internal Memory.
	 * @param filePath The path to the file, the last file must be the file including name
	 * @return The JsonObject or null
	 */
	public static JSONObject loadJson(Context context, String... filePath){
		//Create path
		String path = File.separator;
		for (int x = 0; x < (filePath.length - 1); x++) {
			path += filePath[x] + File.separator;
		}
		path += filePath[filePath.length - 1]; //Should be the filename
		
		//Get file
		File f = new File(context.getFilesDir() + path);
		//	=> Check if a file
		if (!f.exists() || !f.isFile()) {
			Log.d("deb", "File doesn't exist: " + f.getAbsolutePath());
			return null; //Return null if non existant
		}
		
		//Try to load the file
		try {
			//Read the text from the file
			BufferedReader reader = new BufferedReader(new FileReader(f));
			String json = "";
			String line;
			while ((line = reader.readLine()) != null) {
				json += line;
			}
			reader.close();
			 
			//Read as JSON
			return new JSONObject(json);
		} catch (Exception e) {
			Log.d("deb", "Failed to load JSON: " + e);
			return null;
		}
	}
	
	/**
	 * Write a JSONObject to the given filepath
	 * @param json The JsonObject
	 * @param filePath The path to the file, last arg must be the name of the file
	 * @return written
	 */
	public static boolean writeJson(Context context, JSONObject json, String... filePath) {
		//Create directory path
		String path = File.separator;
		for (int x = 0; x < (filePath.length - 1); x++) {
			path += filePath[x] + File.separator;
		}
		
		//Check if Directory exists
		File dir = new File(context.getFilesDir() + path);
		//	=> Checks
		if (!dir.exists()) { //If Dir does not exist yet
			boolean created = dir.mkdirs(); //Create it
			if (!created) {
				Log.d("deb", "Failed to create folder: " + dir.getAbsolutePath());
				return false; //Return false if not being able to create dir
			}
		}
		
		//Create file
		File f = new File(context.getFilesDir() + path + filePath[filePath.length -1]);
		
		//Write to the file
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(f));
			writer.write(json.toString());
			writer.flush();
			writer.close();
			return true;
		} catch (Exception e) {
			Log.d("deb", "Writer exception: " + e);
			return false;
		}
	}
	
	/**
	 * Remove a file
	 * @param context The context
	 * @param filePath The path to the file, last arg must be the name of the file
	 * @return removed
	 */
	public static boolean removeFile(Context context, String... filePath) {
		//Create path
		String path = File.separator;
		for (int x = 0; x < (filePath.length - 1); x++) {
			path += filePath[x] + File.separator;
		}
		path += filePath[filePath.length - 1]; //Should be the filename
		
		//Remove file
		File f = new File(context.getFilesDir() + path);
		return f.delete();
	}
	
}
