package org.mots.haxsync.provider;

import org.json.JSONException;
import org.json.JSONObject;
import org.mots.haxsync.utilities.FacebookUtil;

import android.util.Log;

public class EventAttendee {
	private String name;
	private String email;
	private int status;
	
	public EventAttendee(JSONObject json){
		
		//initialize status
		String statusString = "";
		try{
			statusString = json.getString("rsvp_status");
		}catch (JSONException e) {}
		status = FacebookUtil.convertStatus(statusString);
		
		//initialize lame @facebook.com email because the API doesn't allow anything else
		try {
			email =  json.getString("username") + "@facebook.com";
		} catch (JSONException e) {
			Log.e("Error", e.getLocalizedMessage());
		}
		
		
		//initialize name
		try {
			name =  json.getString("name");
		} catch (JSONException e) {
			Log.e("Error", e.getLocalizedMessage());
		}
	}
	
	public EventAttendee(String name, String email, int status){
		this.name = name;
		this.email = email;
		this.status = status;
	}
	
	

	
	public int getAttendeeStatus(){
		return status;	
	}
	
	//returns lame @facebook.com email because the API doesn't allow anything else
	public String getEmail(){
		return email;
	}

	
	public String getName(){
		return name;
	}
	
	@Override
	public String toString(){
		return "name: "+name +", email: "+email +", status: " +status; 
	}
	

}
