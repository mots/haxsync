package org.mots.haxsync.services;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.mots.haxsync.R;
import org.mots.haxsync.provider.FacebookFQLFriend;
import org.mots.haxsync.provider.FacebookGraphFriend;
import org.mots.haxsync.provider.Status;
import org.mots.haxsync.utilities.BitmapUtil;
import org.mots.haxsync.utilities.ContactUtil;
import org.mots.haxsync.utilities.ContactUtil.Photo;
import org.mots.haxsync.utilities.DeviceUtil;
import org.mots.haxsync.utilities.FacebookUtil;
import org.mots.haxsync.utilities.RootUtil;
import org.mots.haxsync.utilities.WebUtil;

import com.jjnford.android.util.Shell.ShellException;

import android.accounts.Account;
import android.accounts.OperationCanceledException;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.RawContacts.Entity;
import android.util.Log;

public class ContactsSyncAdapterService extends Service {
	private static final String TAG = "ContactsSyncAdapterService";
	private static SyncAdapterImpl sSyncAdapter = null;
	public static ContentResolver mContentResolver = null;
	private static String UsernameColumn = ContactsContract.RawContacts.SYNC1;
	private static String PhotoTimestampColumn = ContactsContract.RawContacts.SYNC2;

	public ContactsSyncAdapterService() {
		super();
	}

	private static class SyncAdapterImpl extends AbstractThreadedSyncAdapter {
		private Context mContext;

		public SyncAdapterImpl(Context context) {
			super(context, true);
			mContext = context;
		}

		@Override
		public void onPerformSync(Account account, Bundle extras,
				String authority, ContentProviderClient provider,
				SyncResult syncResult) {
			try {
				ContactsSyncAdapterService.performSync(mContext, account,
						extras, authority, provider, syncResult);
			} catch (OperationCanceledException e) {
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		IBinder ret = null;
		ret = getSyncAdapter().getSyncAdapterBinder();
		return ret;
	}

	private SyncAdapterImpl getSyncAdapter() {
		if (sSyncAdapter == null)
			sSyncAdapter = new SyncAdapterImpl(this);
		return sSyncAdapter;
	}
	
	private static String matches(Set<String> phoneContacts, String fbContact, int maxdistance){
		if (maxdistance == 0){
			if (phoneContacts.contains(fbContact)){
				return fbContact;
			}
			return null;
			//return phoneContacts.contains(fbContact);
		}
		int bestDistance = maxdistance;
		String bestMatch = null;
		for (String contact : phoneContacts){
			int distance = StringUtils.getLevenshteinDistance(contact != null ? contact.toLowerCase() : "", fbContact != null ? fbContact.toLowerCase() : "");
			if( distance <= bestDistance){
				//Log.i("FOUND MATCH", "Phone Contact: " + contact +" FB Contact: " + fbContact +" distance: " + distance + "max distance: " +maxdistance);
				bestMatch = contact;
				bestDistance = distance;
			}
		}
		return bestMatch;
	}

	private static void addContact(Account account, String name, String username) {
		ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();

		ContentProviderOperation.Builder builder = ContentProviderOperation
				.newInsert(RawContacts.CONTENT_URI);
		builder.withValue(RawContacts.ACCOUNT_NAME, account.name);
		builder.withValue(RawContacts.ACCOUNT_TYPE, account.type);
		builder.withValue(RawContacts.SYNC1, username);
		operationList.add(builder.build());

		builder = ContentProviderOperation
				.newInsert(ContactsContract.Data.CONTENT_URI);
		builder.withValueBackReference(
				ContactsContract.CommonDataKinds.StructuredName.RAW_CONTACT_ID,
				0);
		builder.withValue(
				ContactsContract.Data.MIMETYPE,
				ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
		builder.withValue(
				ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
				name);
		operationList.add(builder.build());

		builder = ContentProviderOperation
				.newInsert(ContactsContract.Data.CONTENT_URI);
		builder.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0);
		builder.withValue(ContactsContract.Data.MIMETYPE,
				"vnd.android.cursor.item/vnd.org.mots.haxsync.profile");
		builder.withValue(ContactsContract.Data.DATA1, username);
		builder.withValue(ContactsContract.Data.DATA2, "Facebook Profile");
		builder.withValue(ContactsContract.Data.DATA3, "View profile");
		operationList.add(builder.build());

		try {
			mContentResolver.applyBatch(ContactsContract.AUTHORITY,
					operationList);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void addSelfContact(Account account, int maxSize, boolean square, boolean faceDetect, boolean force, boolean root, int rootsize, File cacheDir, boolean google) {
		
		Uri rawContactUri = ContactsContract.Profile.CONTENT_RAW_CONTACTS_URI.buildUpon()
				.appendQueryParameter(RawContacts.ACCOUNT_NAME, account.name)
				.appendQueryParameter(RawContacts.ACCOUNT_TYPE, account.type)
				.build();
		
		long ID = -2;
		String username = null;
		String email = null;
        FacebookGraphFriend user = FacebookUtil.getSelfInfo();
        if (user == null)
            return;
        Cursor cursor = mContentResolver.query(rawContactUri, new String[] {BaseColumns._ID, UsernameColumn}, null, null, null);
        if (cursor.getCount() > 0){
        	cursor.moveToFirst();
        	ID = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
        	username = cursor.getString(cursor.getColumnIndex(UsernameColumn));
        	cursor.close();
        } else {
        	cursor.close();
            username = user.getUserName();
            email = user.getEmail();

        	
        	ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();

        	ContentProviderOperation.Builder builder = ContentProviderOperation
        			.newInsert(ContactsContract.Profile.CONTENT_RAW_CONTACTS_URI);
        	builder.withValue(RawContacts.ACCOUNT_NAME, account.name);
        	builder.withValue(RawContacts.ACCOUNT_TYPE, account.type);
        	builder.withValue(RawContacts.SYNC1, username);
        	operationList.add(builder.build());

        	builder = ContentProviderOperation
        			.newInsert(ContactsContract.Data.CONTENT_URI);
        	builder.withValueBackReference(
        			ContactsContract.CommonDataKinds.StructuredName.RAW_CONTACT_ID,
        			0);
        	builder.withValue(
        			ContactsContract.Data.MIMETYPE,
        			ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
        	builder.withValue(
        			ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
        			account.name);
        	operationList.add(builder.build());

        	builder = ContentProviderOperation
        			.newInsert(ContactsContract.Data.CONTENT_URI);
        	builder.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0);
        	builder.withValue(ContactsContract.Data.MIMETYPE,
        			"vnd.android.cursor.item/vnd.org.mots.haxsync.profile");
        	builder.withValue(ContactsContract.Data.DATA1, username);
        	builder.withValue(ContactsContract.Data.DATA2, "Facebook Profile");
        	builder.withValue(ContactsContract.Data.DATA3, "View profile");
        	operationList.add(builder.build());
        	
        	if (email != null){
	        	builder = ContentProviderOperation
	        			.newInsert(ContactsContract.Data.CONTENT_URI);
	        	builder.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0);
	        	builder.withValue(ContactsContract.Data.MIMETYPE,
	        			ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
	        	builder.withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email);
	        	operationList.add(builder.build());
        	}

        	try {
        		mContentResolver.applyBatch(ContactsContract.AUTHORITY,
        				operationList);
        	} catch (Exception e) {
        		// TODO Auto-generated catch block
        		e.printStackTrace();
        		return;
        	}
            cursor = mContentResolver.query(rawContactUri, new String[] {BaseColumns._ID}, null, null, null);
            if (cursor.getCount() > 0){
            	cursor.moveToFirst();
            	ID = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
            	cursor.close();
            } else{
            	Log.i(TAG, "NO SELF CONTACT FOUND");
            	return;
            }
        }
        Log.i("self contact", "id: "+ID+" uid: "+ username);
        if (ID != -2 && username != null){

        updateContactPhoto(ID, 0, maxSize, square, user.getPicURL(), faceDetect, true, root, rootsize, cacheDir, google, true);


        }
	}

	
	
	

	

	private static void updateContactStatus(long rawContactId, String status,
			long timeStamp) {
		if (status != null && timeStamp != 0){
		ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
		Uri rawContactUri = ContentUris.withAppendedId(RawContacts.CONTENT_URI,
				rawContactId);
		Uri entityUri = Uri.withAppendedPath(rawContactUri,
				Entity.CONTENT_DIRECTORY);
		Cursor c = mContentResolver.query(entityUri, new String[] {
				RawContacts.SOURCE_ID, Entity.DATA_ID, Entity.MIMETYPE,
				Entity.DATA1 }, null, null, null);
		try {
			while (c.moveToNext()) {
				if (!c.isNull(1)) {
					String mimeType = c.getString(2);

					if (mimeType
							.equals("vnd.android.cursor.item/vnd.org.mots.haxsync.profile")) {
						ContentProviderOperation.Builder builder = ContentProviderOperation
								.newInsert(ContactsContract.StatusUpdates.CONTENT_URI);
						builder.withValue(
								ContactsContract.StatusUpdates.DATA_ID,
								c.getLong(1));
						builder.withValue(
								ContactsContract.StatusUpdates.STATUS, status);
						builder.withValue(
								ContactsContract.StatusUpdates.STATUS_RES_PACKAGE,
								"org.mots.haxsync");
						builder.withValue(
								ContactsContract.StatusUpdates.STATUS_LABEL,
								R.string.app_name);
						builder.withValue(
								ContactsContract.StatusUpdates.STATUS_ICON,
								R.drawable.icon);
						builder.withValue(
								ContactsContract.StatusUpdates.STATUS_TIMESTAMP,
								timeStamp);
						operationList.add(builder.build());
					}
				}
			}
		} finally {
			c.close();
		}
		try {
			mContentResolver.applyBatch(ContactsContract.AUTHORITY,
					operationList);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OperationApplicationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		}
	}
	
	private static void updateContactPhoto(long rawContactId, long timestamp, int maxSize, boolean square, String imgUrl, boolean faceDetect, boolean force, boolean root, int rootsize, File cacheDir, boolean google, boolean primary) {
		if (imgUrl != null){

		String where = ContactsContract.Data.RAW_CONTACT_ID + " = '" + rawContactId 
				+ "' AND " + ContactsContract.Data.MIMETYPE + " = '" + ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE + "'";

		//getting the old timestamp
		String oldurl = "";
		boolean newpic = force;
		
		if (!newpic){
			Cursor c1 = mContentResolver.query(ContactsContract.Data.CONTENT_URI, new String[] { ContactsContract.Data.SYNC3 }, where , null, null);
			if (c1.getCount() > 0){
				c1.moveToLast();

				if (!c1.isNull(c1.getColumnIndex(ContactsContract.Data.SYNC3))){
					oldurl = c1.getString(c1.getColumnIndex(ContactsContract.Data.SYNC3));
					//Log.i(TAG, "read old timestamp: " + oldTimestamp);
				}
			}
			c1.close();
			
			//Log.i(TAG, "Old Timestamp " +String.valueOf(oldTimestamp) + "new timestamp: " + String.valueOf(timestamp));

				if (!oldurl.equals(imgUrl)){
					Log.i(TAG, "OLD URL: " + oldurl);
					Log.i(TAG, "NEW URL: " + imgUrl);
					newpic = true;
				}
				

		}

		if (newpic){
			Log.i(TAG, "getting new image, "+imgUrl);
		//	Log.i(TAG, "Old Timestamp " +String.valueOf(oldTimestamp) + "new timestamp: " + String.valueOf(timestamp));

			byte[] photo = WebUtil.download(imgUrl);
			byte[] origPhoto = photo;
			
			/*if(square)
				photo = BitmapUtil.resize(photo, maxSize, faceDetect);*/
			
			ContactUtil.Photo photoi = new Photo();
			photoi.data = photo;
			photoi.timestamp = timestamp;
			photoi.url = imgUrl;
			
			ContactUtil.updateContactPhoto(mContentResolver, rawContactId, photoi, primary);



		if (root){
			Cursor c1 = mContentResolver.query(ContactsContract.Data.CONTENT_URI, new String[] { ContactsContract.CommonDataKinds.Photo.PHOTO_FILE_ID}, where , null, null);
			if (c1.getCount() > 0){
				c1.moveToLast();
				String photoID = c1.getString(c1.getColumnIndex(ContactsContract.CommonDataKinds.Photo.PHOTO_FILE_ID));
				c1.close();
				if (photoID != null){
					photo = BitmapUtil.resize(origPhoto, rootsize, faceDetect);
					String picpath = DeviceUtil.saveBytes(photo, cacheDir);
					try {
						String newpath = RootUtil.movePic(picpath, photoID);
						RootUtil.changeOwner(newpath);
					} catch (Exception e) {
						Log.e("ROOT EXCEPTION", e.getMessage());
						// TODO: handle exception
					}
				}
			}
			
		}
		Log.i("google photo sync", String.valueOf(google));
		if (google){
			for (long raw : ContactUtil.getRawContacts(mContentResolver, rawContactId, "com.google")){
				Log.i("google rawid", String.valueOf(raw));
				ContactUtil.updateContactPhoto(mContentResolver, raw, photoi, false);
			}
		}
		}
		}
	}

	public static class SyncEntry {
		public Long raw_id = 0L;
	}
	
	private static HashMap<String, Long> loadHTCData(Context c){
		mContentResolver = c.getContentResolver();
		/*ArrayList<Long> contactIDs = new ArrayList<Long>();
		Cursor c1 = mContentResolver.query(ContactsContract.Contacts.CONTENT_URI, new String[] { BaseColumns._ID }, ContactsContract.Contacts.IN_VISIBLE_GROUP +" = 1", null, null);
		while (c1.moveToNext()){
			contactIDs.add(c1.getLong(c1.getColumnIndex(BaseColumns._ID)));
		}
		c1.close();*/
		
		
		HashMap<String, Long> contacts = new HashMap<String, Long>();
		//Cursor cursor = mContentResolver.query(ContactsContract.Data.CONTENT_URI, null, ContactsContract.Data.MIMETYPE +"= ?", ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE, null);
        String noteWhere = ContactsContract.Data.MIMETYPE + " = ?";
        String[] noteWhereParams = new String[]{ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE};
        Cursor cursor = mContentResolver.query(ContactsContract.Data.CONTENT_URI, new String[] {ContactsContract.Data.RAW_CONTACT_ID, ContactsContract.CommonDataKinds.Note.NOTE}, noteWhere, noteWhereParams, null);
        while (cursor.moveToNext()) {
        	try{
            String note = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Note.NOTE));
            if (note != null){
	            if (note.startsWith("<HTCData>")){
					Pattern fbPattern = Pattern.compile("<Facebook>id:(.*)/friendof.*</Facebook>", Pattern.CASE_INSENSITIVE);
					Matcher fbMatcher = fbPattern.matcher(note);
					
					while (fbMatcher.find()){
						String uid = fbMatcher.group(1);
						//Log.i("found HTCDATA", uid);
		            	Long rawID = cursor.getLong(cursor.getColumnIndex(ContactsContract.Data.RAW_CONTACT_ID));
		            	contacts.put(uid, rawID);
					}
	
	            	//String uid = note.split("/friendof")[0].substring(22);
	
	            }
            }
        	} catch (IllegalStateException e){
        		Log.e(TAG, "Error loading HTCDATA");
        		break;
        	}
        }
        cursor.close();
		return contacts;	
	}
	
	private static HashMap<String, Long> loadPhoneContacts(Context c){
		mContentResolver = c.getContentResolver();
		HashMap<String, Long> contacts = new HashMap<String, Long>();
		Cursor cursor = mContentResolver.query(
				Phone.CONTENT_URI,
				   new String[]{Phone.DISPLAY_NAME, Phone.RAW_CONTACT_ID},
				   null,
				   null,
				   null);
		while (cursor.moveToNext()) {
			contacts.put(cursor.getString(cursor.getColumnIndex(Phone.DISPLAY_NAME)), cursor.getLong(cursor.getColumnIndex(Phone.RAW_CONTACT_ID)));
			//names.add(cursor.getString(cursor.getColumnIndex(Phone.DISPLAY_NAME)));
		}
		cursor.close();
		return contacts;	
	}
	
	
	public static HashMap<String, SyncEntry> getLocalContacts(Account account){
		Uri rawContactUri = RawContacts.CONTENT_URI.buildUpon()
				.appendQueryParameter(RawContacts.ACCOUNT_NAME, account.name)
				.appendQueryParameter(RawContacts.ACCOUNT_TYPE, account.type)
				.build();
		HashMap<String, SyncEntry> localContacts = new HashMap<String, SyncEntry>();
		Cursor c1 = mContentResolver.query(rawContactUri, new String[] { BaseColumns._ID, UsernameColumn, PhotoTimestampColumn }, null, null, null);
		while (c1.moveToNext()) {
			SyncEntry entry = new SyncEntry();
			entry.raw_id = c1.getLong(c1.getColumnIndex(BaseColumns._ID));
			localContacts.put(c1.getString(1), entry);
		}
		c1.close();
		return localContacts;
	}

	@SuppressWarnings("unused")
	private static void performSync(Context context, Account account,
			Bundle extras, String authority, ContentProviderClient provider,
			SyncResult syncResult) throws OperationCanceledException {
		SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", MODE_MULTI_PROCESS);
		
		mContentResolver = context.getContentResolver();

		FacebookUtil.refreshPermissions(context);


        //TODO: Clean up stuff that isn't needed anymore since Graph API
		boolean cropPhotos = true;
    	boolean sync = prefs.getBoolean("sync_status", true);
    	boolean syncNew = prefs.getBoolean("status_new", true);
    	boolean syncLocation = prefs.getBoolean("sync_location", true);
    	boolean syncSelf = prefs.getBoolean("sync_self", false);
        boolean imageDefault = prefs.getBoolean("image_primary", true);
    	    	
    	boolean oldStatus = sync && (!syncNew || (Build.VERSION.SDK_INT < 15));
		boolean faceDetect = true;
		
		boolean root = prefs.getBoolean("root_enabled", false);
		int rootSize = 512;
		
		

		if(FacebookUtil.authorize(context, account)){


			HashMap<String, SyncEntry> localContacts = getLocalContacts(account);
			HashMap<String, Long> names = loadPhoneContacts(context);
			HashMap<String, Long> uids = loadHTCData(context);
			//Log.i("CONTACTS", names.toString());
			boolean phoneOnly = prefs.getBoolean("phone_only", true);
			/*if (phoneOnly){
				names = loadPhoneContacts(context);
			}*/
			boolean wifiOnly = prefs.getBoolean("wifi_only", false);
			boolean syncEmail = prefs.getBoolean("sync_facebook_email", false);
			boolean syncBirthday = prefs.getBoolean("sync_contact_birthday", true);
			boolean force = prefs.getBoolean("force_dl", false);
			boolean google = prefs.getBoolean("update_google_photos", false);
            boolean ignoreMiddleaNames = prefs.getBoolean("ignore_middle_names",false);
            boolean addMeToFriends = prefs.getBoolean("add_me_to_friends", false);
			Log.i("google", String.valueOf(google));
			int fuzziness = Integer.parseInt(prefs.getString("fuzziness", "2"));
			Set<String> addFriends = prefs.getStringSet("add_friends", new HashSet<String>());
			Log.i(TAG, "phone_only: " + Boolean.toString(phoneOnly));
			Log.i(TAG, "wifi_only: " + Boolean.toString(wifiOnly));
			Log.i(TAG, "is wifi: " + Boolean.toString(DeviceUtil.isWifi(context)));
			Log.i(TAG, "phone contacts: " + names.toString()); 
			Log.i(TAG, "using old status api: " +String.valueOf(oldStatus));
            Log.i(TAG, "ignoring middle names : " + String.valueOf(ignoreMiddleaNames));
            Log.i(TAG, "add me to friends : "+  String.valueOf(addMeToFriends));
			boolean chargingOnly = prefs.getBoolean("charging_only", false);
			int maxsize = BitmapUtil.getMaxSize(context);
			File cacheDir = context.getCacheDir();
			Log.i("CACHE DIR", cacheDir.getAbsolutePath());
			Log.i("MAX IMAGE SIZE", String.valueOf(maxsize));
			if (!((wifiOnly && !DeviceUtil.isWifi(context)) || (chargingOnly && !DeviceUtil.isCharging(context)))){
			try {
				if (syncSelf){
					addSelfContact(account, maxsize, cropPhotos, faceDetect, force, root, rootSize, cacheDir, google);
				}	
				//ArrayList<FacebookFQLFriend> friends = FacebookUtil.getFriendInfo(oldStatus);
                List<FacebookGraphFriend> friends = FacebookUtil.getFriends(maxsize, addMeToFriends);
				for (FacebookGraphFriend friend : friends) {
					String uid = friend.getUserName();
					String friendName = friend.getName(ignoreMiddleaNames);
					if (friendName != null && uid != null){
					String match = matches(names.keySet(), friendName , fuzziness);
					
					if (!(phoneOnly && (match == null) && !uids.containsKey(uid)) || addFriends.contains(friendName)){
					//add contact
					if (localContacts.get(uid) == null) {
						//String name = friend.getString("name");
						//Log.i(TAG, name + " already on phone: " + Boolean.toString(names.contains(name)));
						
						addContact(account, friendName, uid);
						
						SyncEntry entry = new SyncEntry();
						Uri rawContactUr = RawContacts.CONTENT_URI.buildUpon()
								.appendQueryParameter(RawContacts.ACCOUNT_NAME, account.name)
								.appendQueryParameter(RawContacts.ACCOUNT_TYPE, account.type)
								.appendQueryParameter(RawContacts.Data.DATA1, uid)
								.build();
						Cursor c =mContentResolver.query(rawContactUr, new String[] {
								BaseColumns._ID}, null,
								null, null);
						c.moveToLast();
						long id = c.getLong(c.getColumnIndex(BaseColumns._ID));
						c.close();
					//	Log.i("ID", Long.toString(id));
						entry.raw_id = id;
						localContacts.put(uid, entry);
						if (uids.containsKey(uid)){
							ContactUtil.merge(context, uids.get(uid), id);
						} else if (names.containsKey(match)){
							ContactUtil.merge(context, names.get(match), id);
						}
						//localContacts = loadContacts(accounts, context);
					}
					
	
					// set contact photo
	
					SyncEntry contact = localContacts.get(uid);
					
					updateContactPhoto(contact.raw_id, friend.getPicTimestamp(), maxsize, cropPhotos, friend.getPicURL(), faceDetect, force, root, rootSize, cacheDir, google, imageDefault);
					
					if (syncEmail && !FacebookUtil.RESPECT_FACEBOOK_POLICY)
						ContactUtil.addEmail(context, contact.raw_id, friend.getEmail());


					if (syncLocation  && !FacebookUtil.RESPECT_FACEBOOK_POLICY){
						ContactUtil.updateContactLocation(contact.raw_id, friend.getLocation());
					}
					
					
					if (oldStatus  && !FacebookUtil.RESPECT_FACEBOOK_POLICY){
						ArrayList<Status> statuses = friend.getStatuses();
						if (statuses.size() >= 1){
							updateContactStatus(contact.raw_id,
									statuses.get(0).getMessage(), statuses.get(0).getTimestamp());
						}
						
					}
					if (syncBirthday  && !FacebookUtil.RESPECT_FACEBOOK_POLICY){
						String birthday = friend.getBirthday();
						
						if (birthday != null){
							ContactUtil.addBirthday(contact.raw_id, birthday);
						}
					}
	
				}
				}
			} }catch (Exception e) {
				// TODO: handle exception
				Log.e("ERROR", e.toString());
				e.printStackTrace();
			
			}
			
			if (root){
				try {
					RootUtil.refreshContacts();
				} catch (ShellException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (force){
				SharedPreferences.Editor editor = prefs.edit();
				editor.putBoolean("force_dl", false);
				editor.commit();
	
			}
			
			} else {
				SharedPreferences.Editor editor = prefs.edit();
				editor.putBoolean("missed_contact_sync", true);
				editor.commit();
				}
		}
	}
}
