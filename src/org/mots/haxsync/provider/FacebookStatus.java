package org.mots.haxsync.provider;

import org.json.JSONException;
import org.json.JSONObject;
import org.mots.haxsync.R;
import org.mots.haxsync.utilities.FacebookUtil;

public class FacebookStatus implements Status {
	private JSONObject json;
	
	public FacebookStatus(JSONObject json){
		this.json = json;
	}

	@Override
	public String getMessage() {
		String message = null;
		try {
			message = json.getString("message");
		} catch (JSONException e) {}
		return 	message;
	}
	
	public String getCommentHtml(){
		String commentString = "";
		int comments = getCommentCount();
		int likes = getLikeCount();
		  if (!getSourceID().equals(getActorID())){
			  commentString += "<b>"+FacebookUtil.getFriendName(getActorID())+"</b>&nbsp;";
		  }
		  if (comments > 0){
			  commentString += "<img src=\"res://org.mots.haxsync/"+ R.drawable.comment+"\"/> " + comments;
		  }
		  if (likes > 0){
			  if (comments > 0){
				  commentString += "&nbsp;";
			  }
			  commentString += "<img src=\"res://org.mots.haxsync/"+ R.drawable.like+"\"/> " + likes;
		  }
		  return commentString;		
	}

	@Override
	public long getTimestamp() {
		long time = 0;
		try{
			time = json.getInt("created_time") * 1000L;
		} catch (JSONException e){}
		return time;
	}

	@Override
	public String getPermalink() {
		String link = null;
		try {
			link = json.getString("permalink");
		} catch (JSONException e) {}
		return 	link;	
		}

	@Override
	public String getID() {
		String id = null;
		try {
			id = json.getString("post_id");
		} catch (JSONException e) {}
		return 	id;
	}
	
	private String getSourceID() {
		String id = "";
		try {
			id = json.getString("source_id");
		} catch (JSONException e) {}
		return 	id;
	}
	
	public int getType(){
		int type = 0;
		try {
			type = json.getInt("type");
		} catch (JSONException e) {}
		return type;
	}
	
	public int getCommentCount(){
		int comments = 0;
		try {
			comments = json.getJSONObject("comments").getInt("count");
		} catch (JSONException e) {}
		return comments;
	}
	
	public JSONObject getAppData(){
		JSONObject appData = null;
		try{
			appData = json.getJSONObject("app_data");
		} catch (JSONException e) {}
		return appData;
	}
	
	public String getActorID(){
		String id = null;
		try {
			id = json.getString("actor_id");
		} catch (JSONException e) {}
		return 	id;
	}
	
	public int getLikeCount(){
		int likes = 0;
		try {
			likes = json.getJSONObject("likes").getInt("count");
		} catch (JSONException e) {}
		return likes;
	}
	
	@Override
	public String toString(){
		String msg = getMessage();
		if (msg == null){
			return "empty status";
		}
		return msg;
	}

}
