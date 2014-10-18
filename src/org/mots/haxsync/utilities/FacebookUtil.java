package org.mots.haxsync.utilities;

import java.io.IOException;
import java.net.MalformedURLException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mots.haxsync.BaseRequestListener;
import org.mots.haxsync.R;
import org.mots.haxsync.provider.*;
import org.mots.haxsync.provider.FacebookFQLFriend;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Debug;
import android.preference.PreferenceManager;
import android.provider.CalendarContract.Attendees;
import android.util.Log;
import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.ServiceListener;
import com.facebook.android.FacebookError;

public class FacebookUtil extends Application{
//	public static final String fbID = "176550339110536";
	public static final String fbID = "366633546716469";
	public static Facebook facebook = new Facebook(fbID);
	public static AsyncFacebookRunner mAsyncRunner = new AsyncFacebookRunner(facebook);
	public static Activity mActivity;
	public static String accessToken;
	private static final String TAG = "Utility";
	public static final int PERMISSION_LEVEL = 1;
	private static JSONObject selfInfo = null;
	private static final int FQL_LIMIT = 20;
	private static HashMap<String, Long> birthdays = new HashMap<String, Long>();
	public static boolean isExtendingToken = false;

	public static final boolean RESPECT_FACEBOOK_POLICY = true;

	private static JSONObject realSelfInfo = null;

	public static class PicInfo{
		public String url;
		public long timestamp;
	}

    public static boolean authorize(Context context, Account account) {
        if (isExtendingToken) {
            return false;
        }
        String token;
        AccountManager am = AccountManager.get(context);
        try {
            token = am.getPassword(account);
        } catch (SecurityException e) {
            Log.e(TAG, "error getting token");
            return false;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        long access_expires = prefs.getLong("access_expires", 0);
        Log.i("TOKEN EXPIRES", String.valueOf(access_expires));
        facebook.setAccessToken(token);
        if (access_expires != 0)
            facebook.setAccessExpires(access_expires);
        //Log.i("Facebook valid?", String.valueOf());

        DeviceUtil.showJellyBeanNotification(context);

        if (!DeviceUtil.isOnline(context))
            return false;

        boolean shouldExtend = false;
        try {
            shouldExtend = !checkToken() || facebook.shouldExtendAccessToken();
        } catch (IOException i) {
            Log.e(TAG, "error: " + i.getMessage());
            return false;
        }
        if (shouldExtend) {
            notifyToken(context);
            return false;
        }
        return true;
    }

    private static void notifyToken(Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setClassName("org.mots.haxsync", "org.mots.haxsync.activities.AuthorizationActivity");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        isExtendingToken = true;

        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Resources res = context.getResources();
        Notification.Builder builder = new Notification.Builder(context);

        builder.setContentIntent(contentIntent)
                    .setSmallIcon(android.R.drawable.stat_notify_error)
                    .setLargeIcon(BitmapFactory.decodeResource(res, R.drawable.icon))
                    .setTicker(res.getString(R.string.token_notification_ticker))
                    .setWhen(System.currentTimeMillis())
                    .setAutoCancel(true)
                    .setContentTitle(res.getString(R.string.token_notification_title))
                    .setContentText(res.getString(R.string.token_notification_description));
        Notification n = builder.getNotification();

        nm.notify(0, n);
    }

    private static boolean checkToken() throws IOException {
			String response = facebook.request("me");
			//Log.i("response", response);
			return (!response.contains("\"type\":\"OAuthException\"") && facebook.isSessionValid());
	}


	public static void refreshPermissions(Context c){

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);

		int permissionLevel = prefs.getInt("permission_level", 0);
		Log.i("Permission Level", String.valueOf(permissionLevel));

		if (permissionLevel < FacebookUtil.PERMISSION_LEVEL){
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setClassName("org.mots.haxsync", "org.mots.haxsync.activities.AuthorizationActivity");
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			c.startActivity(intent);
			SharedPreferences.Editor editor = prefs.edit();
			editor.putInt("permission_level", FacebookUtil.PERMISSION_LEVEL);
			editor.commit();
		}
	}


    public static List<FacebookGraphFriend> getFriends(int maxsize, boolean addMeToFriends){
        Bundle params = new Bundle();
        params.putString("fields", "picture.width("+maxsize+").height("+maxsize+"), name, first_name, last_name, username, birthday, location, updated_time");
        List<FacebookGraphFriend> friends = new ArrayList<FacebookGraphFriend>();
        List<String> ids = new ArrayList<String>();


        try {
            Bundle idParam = new Bundle();
            idParam.putString("fields", "id");
            if(addMeToFriends){
                JSONObject resp =  new JSONObject(facebook.request("me",idParam));
                Log.i(TAG, "id response " + resp);
                Log.i(TAG, "add Me to friends:" + resp.getString("id"));
                ids.add(resp.getString("id"));


            }

            JSONObject resp =  new JSONObject(facebook.request("me/friends", idParam));
            Log.i(TAG, "id response:" + resp);
            JSONArray friendsjson = resp.getJSONArray("data");
            for(int i = 0; i < friendsjson.length(); i++){
                ids.add(friendsjson.getJSONObject(i).getString("id"));

            }
            for(String id : ids){
                JSONObject response = new JSONObject(facebook.request(id,params));
                FacebookGraphFriend  friend = new FacebookGraphFriend(response);
                friends.add(friend);

            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, "Error loading friends: " + e.toString());
        }
        return friends;

    }


	public static ArrayList<FacebookFQLFriend> getFriendInfo(boolean status){
        //me/friends?fields=picture.height(720).width(720),name,username,birthday,location



    //old fql way
	JSONArray friendarray = new JSONArray();
	JSONArray timestamps = new JSONArray();
	int fetched = FQL_LIMIT;
	int callNo = 0;
	try{
		while (fetched == FQL_LIMIT){
		JSONObject jsonFQL = new JSONObject();
		String query1;
		if (status){
			query1 = "select name,  username, status, uid, profile_update_time, birthday_date, pic_big, current_location from user where uid in (select uid2 from friend where uid1=me()) order by name limit " + FQL_LIMIT + " offset " + callNo * FQL_LIMIT;
		} else {
			query1 = "select name,  username, uid, profile_update_time, birthday_date, pic_big, current_location from user where uid in (select uid2 from friend where uid1=me()) order by name limit " +  FQL_LIMIT + " offset " + callNo * FQL_LIMIT;
		}
		//Log.i("query1", query1);
		jsonFQL.put("query1", query1);
	    jsonFQL.put("query2", "SELECT modified, src_big, owner FROM photo WHERE pid IN (SELECT cover_pid FROM album WHERE owner IN (SELECT uid FROM #query1) AND type = 'profile')");
	    Bundle params = new Bundle();
	    params.putString("method", "fql.multiquery");
	    params.putString("queries", jsonFQL.toString());
	    String friendstring = facebook.request(params);
		JSONArray friendInfo = new JSONArray(friendstring);
		JSONArray newFriends = friendInfo.getJSONObject(0).getJSONArray("fql_result_set");
		Log.i("friendstring", friendstring);
		fetched = newFriends.length();
		Log.i("no of friends fetched", String.valueOf(fetched));
		friendarray = concatArray(friendarray, newFriends);
		timestamps = concatArray(timestamps, friendInfo.getJSONObject(1).getJSONArray("fql_result_set"));
		callNo++;
		}
	} catch (Exception e) {
		Log.e("ERROR:", e.toString());
	}
	//Log.i("friendarray", friendarray.toString());
	Log.i("total number of friends fetched", String.valueOf(friendarray.length()));
	Log.i("timestamps", timestamps.toString());
	return mergeArrays(friendarray, timestamps);

	}



	public static PicInfo getProfilePicInfo(String uid){
		String query = "SELECT modified, src_big, owner FROM photo WHERE pid IN (SELECT cover_pid FROM album WHERE owner = "+uid+" AND type = 'profile')";

		Bundle params = new Bundle();
		params.putString("method", "fql.query");
		params.putString("query", query);
		PicInfo pic = new PicInfo();
		try {
			JSONArray hires = new JSONArray(facebook.request(params));
			if (hires.length() >= 1){
				pic.url = hires.getJSONObject(0).getString("src_big");
				pic.timestamp = hires.getJSONObject(0).getLong("modified");
			}
			else {
				query = "select pic_big from user where uid =" +uid;
				params.putString("query", query);
				//Log.i("json", facebook.request(params));
				pic.url = new JSONArray(facebook.request(params)).getJSONObject(0).getString("pic_big");

			}

		} catch (Exception e){
			e.printStackTrace();
		}
		return pic;
	}




	private static JSONArray concatArray(JSONArray old, JSONArray add) throws JSONException {
	   for (int i = 0; i < add.length(); i++) {
	     old.put(add.get(i));
    }
	      return old;
	}

	public static ArrayList<Status> getStatuses(String uid, boolean fullTimeline){
		JSONArray statusarray = getStatusJSON(uid, fullTimeline);
		ArrayList<Status> statuses = new ArrayList<Status>();
		if (statusarray != null){
			for (int j = statusarray.length() -1 ; j >= 0 ; j--) {
				JSONObject statusjson = null;
				try {
					statusjson = statusarray.getJSONObject(j);
				} catch (JSONException e) {	}
				if (statusjson != null) {
					statuses.add(new FacebookStatus(statusjson));
				}
			}
		}
		return statuses;

	}

	private static JSONArray getStatusJSON(String uid, boolean fullTimeline){
		String query = "SELECT message, post_id, type, created_time, actor_id, app_data, source_id, likes, comments, permalink FROM stream WHERE source_id="+uid+"  ORDER BY created_time DESC LIMIT 50";
		if (!fullTimeline){
			query = "SELECT message, post_id, type, created_time, actor_id, app_data, source_id, likes, comments, permalink FROM stream WHERE source_id="+uid+" AND actor_id="+uid+" ORDER BY created_time DESC LIMIT 50";
		}
		Bundle params = new Bundle();
		params.putString("method", "fql.query");
		params.putString("query", query);
		JSONArray result = null;
		try {
			String response = facebook.request(params);
		//	Log.i("response", response);
			result = new JSONArray(response);
		} catch (Exception e){}
		//Log.i("response", result.toString());
		return result;
	}


	public static List<Event> getEvents(String status){
		ArrayList<Event> events = new ArrayList<Event>();
	try{
	    Bundle params = new Bundle();
	    params.putString("method", "fql.query");
	    String statusString = "";
	    String[] statusArray = status.split("\\|");
	    for (int i = 0; i < statusArray.length; i++){
	    	statusString += "'"+statusArray[i]+"'";
	    	if (i != statusArray.length-1)
	    		statusString += ", ";
	    }
	    String request = "SELECT name, eid, start_time, end_time, location, description FROM event WHERE eid IN ( SELECT eid FROM event_member WHERE uid =  me() AND rsvp_status in ("+statusString+"))";
	    Log.i("request", request);
	    params.putString("query", request);
	    JSONArray eventarray = new JSONArray(facebook.request(params));
	    for (int i = 0; i < eventarray.length(); i++){
	    	events.add(new Event(eventarray.getJSONObject(i)));
	    }
	} catch (Exception e) {
		Log.e("ERROR:", e.toString());
	}
	return events;
	}

	private static void addBirthday(String name, String date){
		int month = Integer.valueOf(date.split("/")[0]);
		int day = Integer.valueOf(date.split("/")[1]);
		GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		cal.set(Calendar.getInstance().get(Calendar.YEAR), month-1, day, 0, 0 , 0);
		long millis = cal.getTimeInMillis();
		birthdays.put(name, millis);
	}

	public static HashMap<String, Long> getBirthdays(){
		if (birthdays.size() > 0){
			return birthdays;
		}
		String query = "select name, birthday_date from user where uid in (select uid2 from friend where uid1=me()) order by name";
		Bundle params = new Bundle();
		params.putString("method", "fql.query");
		params.putString("query", query);
		JSONArray result = null;
		try {
			result = new JSONArray(facebook.request(params));
		} catch (Exception e) {
			return birthdays;
		}for(int i = 0; i < result.length(); i++){
			try{
				JSONObject friend = result.getJSONObject(i);
				if (friend.getString("birthday_date") != null){
					addBirthday(friend.getString("name"), friend.getString("birthday_date"));
				}} catch (Exception e) {}

		}
		return birthdays;
	}



	private static ArrayList<FacebookFQLFriend> mergeArrays(JSONArray friendarray, JSONArray timestamps){
		ArrayList<FacebookFQLFriend> friends = new ArrayList<FacebookFQLFriend>();
		try{
		for(int i = 0; i < friendarray.length(); i++){
			JSONObject friend = friendarray.getJSONObject(i);
			String uid = friend.getString("uid");

			//write birthday hashmap
			String birthdayDate = friend.getString("birthday_date");
			if (birthdayDate != null && !birthdayDate.equals("") && ! birthdayDate.equals("null")){
				addBirthday(friend.getString("name"), friend.getString("birthday_date"));
			}

			long modified = 0;
			for(int j = 0; j < timestamps.length(); j++){
				JSONObject timestamp = timestamps.getJSONObject(j);
				if (timestamp.getString("owner").equals(uid)){
					modified = timestamp.getLong("modified");
					String src = timestamp.getString("src_big");
					if (!src.equalsIgnoreCase("")){
						friend.put("pic_big", src);
					}
					break;
				}
			}
			friend.put("pic_modified", modified);
			friends.add(new FacebookFQLFriend(friend));
		}
		} catch (Exception e) {
			Log.e("ERROR:", e.toString());
		}
	//	Log.i("JSON2", merged.toString());
		return friends;
	}

    public static JSONObject getPicInfo(long picID){
		String query = "SELECT owner, src_big, pid, aid FROM photo WHERE object_id="+picID;
	    Bundle params = new Bundle();
	    params.putString("method", "fql.query");
	    params.putString("query", query);
	    JSONObject result = null;
		try {
			result = new JSONArray(facebook.request(params)).getJSONObject(0);
			//Log.i("PIC_ID"+picID, result);
		} catch (Exception e) {
			Log.e(TAG, "error getting pic for id: " + picID);
			return null;
		}
		return result;
	}



	public static JSONObject getProfilePic(String uid){
		String query = "SELECT modified, src_big FROM photo WHERE pid IN (SELECT cover_pid FROM album WHERE owner="+uid+" AND type = 'profile')";
		Log.i("query", query);
	    Bundle params = new Bundle();
	    params.putString("method", "fql.query");
	    params.putString("query", query);
	    JSONObject result = null;
		try {
			result = new JSONArray(facebook.request(params)).getJSONObject(0);
		} catch (Exception e) {	}
		//Log.i("result", result.toString());
		return result;
	}



	public static String getSelfID(){
		FacebookGraphFriend selfInfo = getSelfInfo();
        return selfInfo.getUserName();
	}


	public static FacebookGraphFriend getSelfInfo(){
        FacebookGraphFriend friend = null;
        Bundle params = new Bundle();
        params.putString("fields", "picture.width(720).height(720), name, username, birthday, location, updated_time");
        try {
            JSONObject response = new JSONObject(facebook.request("me", params));
            friend = new FacebookGraphFriend(response);
            //Log.i("GRAPH FRIENDS", friendsjson.toString());
        } catch (IOException e) {
            Log.e(TAG, "Error loading friends: " + e.toString());
        }catch (JSONException e) {
            Log.e(TAG, "Error loading friends: " + e.toString());
        }
    	return  friend;
	}

	public static JSONObject getSelfInfoAsync(){
    	Bundle params = new Bundle();
    	params.putString("fields", "name, picture");
    	mAsyncRunner.request("me", params, new UserRequestListener());
    	while (selfInfo == null){
    		try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	return selfInfo;

	}


	public static String getFriendName(String uid){
		String name = null;
		try{
			String query = "select name from user where uid = " + uid;
		    Bundle params = new Bundle();
		    params.putString("method", "fql.query");
		    params.putString("query", query);
		    name = new JSONArray(facebook.request(params)).getJSONObject(0).getString("name");
		} catch (Exception e) {
			Log.e("ERROR:", e.toString());
		}
	   // Log.i("JSON1", friendInfo.toString());
		return name;

	}






	public static JSONArray getFriendNames(){
	JSONArray friendInfo = null;
	try{
		String query = "select name from user where uid in (select uid2 from friend where uid1=me()) order by name";
	    Bundle params = new Bundle();
	    params.putString("method", "fql.query");
	    params.putString("query", query);
	    String friendstring = facebook.request(params);
		friendInfo = new JSONArray(friendstring);
	} catch (Exception e) {
		Log.e("ERROR:", e.toString());
	}
   // Log.i("JSON1", friendInfo.toString());
	return friendInfo;

	}

	public static int convertStatus(String statusString){
		if (statusString.equals("attending")){
			return Attendees.ATTENDEE_STATUS_ACCEPTED;
		} else if (statusString.equals("unsure")){
			return  Attendees.ATTENDEE_STATUS_TENTATIVE;
		} else if (statusString.equals("declined")){
			return Attendees.ATTENDEE_STATUS_DECLINED;
		} else {
			return Attendees.ATTENDEE_STATUS_INVITED;
		}
	}

	public static List<EventAttendee> getEventAttendees(long eid){
	JSONObject jsonFQL = new JSONObject();
	String query1 = "SELECT uid, rsvp_status FROM event_member WHERE eid = " +eid + " AND uid IN (select uid2 from friend where uid1=me())";
	List<EventAttendee> attendees = new ArrayList<EventAttendee>();
	try {
		jsonFQL.put("query1", query1);
	    jsonFQL.put("query2", "SELECT name, username, uid FROM user WHERE uid IN (SELECT uid FROM #query1)");
	    Bundle params = new Bundle();
	    params.putString("method", "fql.multiquery");
	    params.putString("queries", jsonFQL.toString());
	    JSONArray response = new JSONArray(facebook.request(params));
	    attendees = mergeAttendees(response.getJSONObject(0).getJSONArray("fql_result_set"), response.getJSONObject(1).getJSONArray("fql_result_set"));
	   // Log.i("respnse", response.toString());
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	/*try{
		JSONObject self = getSelfInfo();
		String query = "SELECT rsvp_status FROM event_member WHERE eid = " + eid +" AND uid = me()";
	    Bundle params = new Bundle();
	    params.putString("method", "fql.query");
	    params.putString("query", query);
	    int status = convertStatus(new JSONArray(facebook.request(params)).getJSONObject(0).getString("rsvp_status"));
		EventAttendee selfAttendee = new EventAttendee(self.getString("name"), self.getString("email"), status);
		Log.i("selfattendee", selfAttendee.toString());
	    attendees.add(selfAttendee);
	}catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}*/
    return attendees;
	}


	public static String getSelfAttendance(long eid){
	try{
		String query = "SELECT rsvp_status FROM event_member WHERE eid = " + eid +" AND uid = me()";
	    Bundle params = new Bundle();
	    params.putString("method", "fql.query");
	    params.putString("query", query);
	    return new JSONArray(facebook.request(params)).getJSONObject(0).getString("rsvp_status");
	}catch (Exception e) {
		// TODO Auto-generated catch block
		Log.e(TAG, e.getMessage());
		return "";
	}
	}

	private static List<EventAttendee> mergeAttendees(JSONArray statuses, JSONArray attendees){
		ArrayList<EventAttendee> attendeeList = new ArrayList<EventAttendee>();
		try{
		for(int i = 0; i < attendees.length(); i++){
			JSONObject friend = attendees.getJSONObject(i);
			String uid = friend.getString("uid");

			//write birthday hashmap

			long modified = 0;
			for(int j = 0; j < statuses.length(); j++){
				JSONObject status = statuses.getJSONObject(j);
				if (status.getString("uid").equals(uid)){
					friend.put("rsvp_status", status.getString("rsvp_status"));
					break;
				}
			}
			attendeeList.add(new EventAttendee(friend));
		}
		} catch (Exception e) {
			Log.e("ERROR:", e.toString());
		}
	//	Log.i("JSON2", merged.toString());
		return attendeeList;
	}





	public static JSONObject getFriendNamesAsync(){
    	Bundle params = new Bundle();
    	params.putString("fields", "name, picture");
    	mAsyncRunner.request("me", params, new UserRequestListener());
    	while (selfInfo == null){
    		try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	return selfInfo;

	}

	public static class TokenListener implements ServiceListener{

		@Override
		public void onComplete(Bundle values) {
			//Log.i("got values", values.toString());
			accessToken = values.getString(Facebook.TOKEN);
			// TODO Auto-generated method stub

		}

		@Override
		public void onFacebookError(FacebookError e) {
			Log.e(TAG, e.toString());
		}

		@Override
		public void onError(Error e) {
			Log.e(TAG, e.toString());
		}

	}

	public static class UserRequestListener extends BaseRequestListener{
		public void onComplete(final String response, final Object state){
			JSONObject jsonobject;
				try{
					jsonobject = new JSONObject(response);
					selfInfo = jsonobject;
				} catch (JSONException e){
					e.printStackTrace();
				}
		}
	}

}
