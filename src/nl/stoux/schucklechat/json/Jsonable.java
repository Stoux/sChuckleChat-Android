package nl.stoux.schucklechat.json;

import org.json.JSONObject;

public interface Jsonable {
		
	/**
	 * Create a JSONObject of this object
	 * @param object The object
	 * @return
	 */
	public JSONObject asJson();

}
