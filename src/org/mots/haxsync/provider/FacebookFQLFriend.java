package org.mots.haxsync.provider;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.mots.haxsync.utilities.FacebookUtil;

public class FacebookFQLFriend implements Friend {
	private JSONObject json;
	private List<String> defaultURLs = Arrays.asList("https://fbcdn-profile-a.akamaihd.net/static-ak/rsrc.php/v2/yL/r/HsTZSDw4avx.gif", "https://fbcdn-profile-a.akamaihd.net/static-ak/rsrc.php/v2/yp/r/yDnr5YfbJCH.gif");
	
	public FacebookFQLFriend(JSONObject json){
		this.json = json;
	}
	
	@Override
	public String toString(){
		return "Name: "+this.getName(true) +", FB-ID: "+this.getUserName();
	}

	@Override
	public String getName(boolean ignoreMiddleNames) {
		String name = null;
		try {
			name = json.getString("name");
		} catch (JSONException e) {}
		return 	name;
	}
	
	//returns lame @facebook.com email because the API doesn't allow anything else
	public String getEmail(){
		try {
			return json.getString("username") + "@facebook.com";
		} catch (JSONException e) {
			return null;
		}	}

	@Override
	public String getUserName() {
		String uid = null;
		try {
			uid = json.getString("uid");
		} catch (JSONException e) {}
		return 	uid;
	}

	@Override
	public String getPicURL() {
		String url = null;
		try {
			url = defaultURLs.contains(json.getString("pic_big")) ? null : json.getString("pic_big");
		} catch (JSONException e) {}
		return 	url;
	}

	@Override
	public long getPicTimestamp() {
		long timestamp = 0;
		try {
			timestamp = json.getLong("pic_modified");
		} catch (JSONException e) {}
		return 	timestamp;
	}
	
	public String getCountry(){
		String country = null;
		try {
			country = json.getJSONObject("current_location").getString("country");
		} catch (JSONException e) {}
		return 	country;
	}
	
	public String getState(){
		String state = null;
		try {
			state = json.getJSONObject("current_location").getString("state");
		} catch (JSONException e) {}
		return 	state;
	}
	
	public String getCity(){
		String city = null;
		try {
			city = json.getJSONObject("current_location").getString("city");
		} catch (JSONException e) {}
		return 	city;
	}
	
	public String getBirthday(){
		try {
			String birthday = json.getString("birthday_date");
			String[] birthdayArray = birthday.split("/");
			if (birthdayArray.length == 3){
				return birthdayArray[2] + "-" + birthdayArray[0] + "-" + birthdayArray[1];
			} else if (birthdayArray.length == 2){
				return "--" + birthdayArray[0]  + "-" + birthdayArray[1];
			}
					
		} catch (JSONException e) {}
		return 	null;
	}

	@Override
	public ArrayList<Status> getStatuses() {
		JSONObject status = null;
		ArrayList<Status> statuses = new ArrayList<Status>();
		try {
			status = json.getJSONObject("status");
		} catch (JSONException e) {	}
		if (status != null) {
			statuses.add(new FacebookStatus(status));
			return statuses;
		} else {
			String uid = this.getUserName();
			if (uid != null) {
				return FacebookUtil.getStatuses(uid, false);
			}

		}
		return statuses;
	}

}
