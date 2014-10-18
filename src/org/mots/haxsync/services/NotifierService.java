package org.mots.haxsync.services;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;
import org.mots.haxsync.R;
import org.mots.haxsync.provider.FacebookStatus;
import org.mots.haxsync.provider.Status;
import org.mots.haxsync.utilities.DeviceUtil;
import org.mots.haxsync.utilities.FacebookUtil;
import org.mots.haxsync.utilities.WebUtil;
import org.mots.haxsync.utilities.intents.Stream;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.StreamItemPhotos;
import android.provider.ContactsContract.StreamItems;
import android.util.Log;

/**
 * Service to handle view notifications. This allows the sample sync adapter to update the
 * information when the contact is being looked at
 */
@SuppressLint("NewApi")
public class NotifierService extends IntentService {
    private static final String TAG = "NotifierService";
	private static ContentResolver mContentResolver;
    
	private long addContactStreamItem(long rawContactId, String uid, FacebookStatus status,  Account account){
		
		  //get timestamp of latest saved streamItem
		  Cursor c = mContentResolver.query(Uri.withAppendedPath(ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId),
				 RawContacts.StreamItems.CONTENT_DIRECTORY),
		          new String[] {StreamItems.TIMESTAMP}, null, null, StreamItems.TIMESTAMP+" DESC");
		  long oldTimestamp = -2;
		  if (c.getCount() > 0){
				c.moveToFirst();
				oldTimestamp = c.getLong(c.getColumnIndex("timestamp"));
			}
		  c.close();
		  long timestamp = status.getTimestamp();
		  //only add item if newer then latest saved one
		  if(oldTimestamp >= timestamp){
			  return -2;
		  }
		  
		  String message = status.getMessage();
		  
		  String picLink = findPicLink(message);
		  message = message.replace(picLink, "");
		  
		  Youtube yt = findYoutube(message);
		  if (yt != null)
			  message = message.replace(yt.link, "");
		  
		  
		  ContentValues values = new ContentValues();
		  values.put(StreamItems.RAW_CONTACT_ID, rawContactId);
		  values.put(StreamItems.RES_PACKAGE, "org.mots.haxsync");
		  values.put(StreamItems.RES_LABEL, R.string.app_name);
		  values.put(StreamItems.TEXT, message);
		  values.put(StreamItems.TIMESTAMP, timestamp);
		  
		  String commentString = status.getCommentHtml();
		  if (!commentString.equals("")){
			  values.put(StreamItems.COMMENTS, commentString);
		  }
		  
		  values.put(StreamItems.SYNC1, status.getID());
		  values.put(StreamItems.SYNC2, uid);
		  values.put(StreamItems.SYNC3, status.getPermalink());
		  values.put(StreamItems.ACCOUNT_NAME, account.name);
		  values.put(StreamItems.ACCOUNT_TYPE, account.type);
		  Uri streamItemUri = mContentResolver.insert(StreamItems.CONTENT_URI, values);
		  long streamItemId = ContentUris.parseId(streamItemUri);
		  
		  if (status.getType() == 247)
			  addFBPhoto(streamItemId, account, status.getAppData());
		  if (yt != null)
			  addYoutubeThumb(streamItemId, account, yt);
		  if (!picLink.equals(""))
			  addPicThumb(streamItemId, account, picLink);
		  
		  return streamItemId;
		}
	
	private void addStreamPhoto(long itemID, byte[] photo, Account account, String type, String sync2){
		 ContentValues values = new ContentValues();
		 values.put(StreamItemPhotos.STREAM_ITEM_ID, itemID);
		 values.put(StreamItemPhotos.SORT_INDEX, 1);
		 values.put(StreamItemPhotos.PHOTO, photo);
		 values.put(StreamItems.ACCOUNT_NAME, account.name);
		 values.put(StreamItems.ACCOUNT_TYPE, account.type);
		 values.put(StreamItemPhotos.SYNC1, type);
		 values.put(StreamItemPhotos.SYNC2, sync2);
		 mContentResolver.insert(StreamItems.CONTENT_PHOTO_URI, values);
	}

	private String findPicLink(String message){
		String picLink = "";
		Pattern picPattern = Pattern.compile("https?:\\/\\/[a-z0-9\\-\\.]+\\.[a-z]{2,3}/.+\\.(jpg|png|gif|bmp)", Pattern.CASE_INSENSITIVE);
		Matcher picMatcher = picPattern.matcher(message);
		
		while (picMatcher.find()){
			picLink = picMatcher.group();
			//Log.i("picLink", picLink);
		}
		return picLink;
	}

    public static class Youtube{
    	public String ID;
    	public String link;
    }
	
	private Youtube findYoutube(String message){
		Youtube yt = null;
		Pattern ytPattern = Pattern.compile ("https?:\\/\\/(?:[0-9A-Z-]+\\.)?(?:youtu\\.be\\/|youtube\\.com\\S*[^\\w\\-\\s])([\\w\\-]{11})(?=[^\\w\\-]|$)(?![?=&+%\\w]*(?:['\"][^<>]*>|<\\/a>))[?=&+%\\w]*", Pattern.CASE_INSENSITIVE);
		Matcher ytMatcher = ytPattern.matcher(message);
		while (ytMatcher.find()){
			yt = new Youtube();
			yt.link = ytMatcher.group();
			yt.ID = ytMatcher.group(1);		
		}
		return yt;
	}
	
	private void addFBPhoto(long streamID, Account account, JSONObject appData){
		long picID = 0;
		try {
			picID = appData.getJSONArray("photo_ids").getLong(0);
		} catch (Exception e) {
			Log.e("ERROR", e.toString());
		}
		if (picID != 0){
			JSONObject picinfo = FacebookUtil.getPicInfo(picID);
			if (picinfo != null){
				String src = null;
				try {
					src = picinfo.getString("src_big");
				} catch (Exception e) {
					Log.e("ERROR", e.toString());
				}
				if (src != null){

				byte[] pic = WebUtil.download(src);
				if (pic != null){
					addStreamPhoto(streamID, pic, account, "fbphoto", String.valueOf(picID));
				}
			}
			}
		}

	}
	
	private void addYoutubeThumb(long streamID, Account account, Youtube yt){
		byte[] pic = WebUtil.download("http://img.youtube.com/vi/"+yt.ID+"/0.jpg");
		if (pic != null){
			addStreamPhoto(streamID, pic, account, "youtube", yt.link);
		}
	}
	
	private void addPicThumb(long streamID, Account account, String picLink){
		byte[] pic = WebUtil.download(picLink);
		if (pic != null){
			addStreamPhoto(streamID, pic, account, "link", picLink);
		}
	}
	
    public NotifierService() {
        super(TAG);
    }

    @SuppressWarnings("unused")
	@Override
    protected void onHandleIntent(Intent intent) {
    	if (!FacebookUtil.RESPECT_FACEBOOK_POLICY && DeviceUtil.isOnline(this)){
    		Log.i(TAG, "is online");
    		SharedPreferences prefs = this.getSharedPreferences(this.getPackageName() + "_preferences", MODE_MULTI_PROCESS);		
    		boolean sync = prefs.getBoolean("sync_status", true);
    		boolean syncNew = prefs.getBoolean("status_new", true);
    		boolean timelineAll = prefs.getBoolean("timeline_all", false);


    		if (Build.VERSION.SDK_INT >= 15 && sync && syncNew){
    			mContentResolver = this.getContentResolver();

    			AccountManager am = AccountManager.get(this);
    			Account account = am.getAccountsByType("org.mots.haxsync.account")[0];
    			if (FacebookUtil.authorize(this, account)){

    				String[] projection = new String[] {RawContacts._ID, RawContacts.SYNC1};
    				Cursor c = mContentResolver.query(intent.getData(), projection, null, null, null);
    				c.moveToFirst();
    				long id = c.getLong(c.getColumnIndex(RawContacts._ID));
    				String uid = c.getString(c.getColumnIndex(RawContacts.SYNC1));
    				c.close();

    				ArrayList<Status> statuses = FacebookUtil.getStatuses(uid, timelineAll); 

    				if (statuses != null) {
    					Log.i(TAG, statuses.toString());
    					for (Status status : statuses) {
    						FacebookStatus fbstatus = (FacebookStatus) status;
    						if (timelineAll || fbstatus.getActorID().equals(uid)){
    							if (fbstatus != null && !fbstatus.getMessage().equals(""))					
    								addContactStreamItem(id, uid, fbstatus, account);
    						}
    					}
    				}
    			}
    		}

    	}

    }

}

    
