package org.mots.haxsync.utilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mots.haxsync.services.ContactsSyncAdapterService;
import org.mots.haxsync.services.ContactsSyncAdapterService.SyncEntry;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

public class ContactUtil {
    public static class Contact{
    	public long ID;
    	public String name;
    	public String accountType;
    }
    
    public static class Photo{
    	public byte[] data;
    	public String url;
    	public long timestamp;
    }
    
    public static final String TAG = "ContactUtil";
    
    private static HashMap<String, Drawable> accountIcons = new HashMap<String, Drawable>();
    
    public static Contact getMergedContact(Context c, long contactID, Account account){
    	
		Uri ContactUri = RawContacts.CONTENT_URI.buildUpon()
				.appendQueryParameter(RawContacts.ACCOUNT_NAME, account.name)
				.appendQueryParameter(RawContacts.ACCOUNT_TYPE, account.type)
				.build();
    	
		Cursor cursor = c.getContentResolver().query(ContactUri, new String[] { BaseColumns._ID, RawContacts.DISPLAY_NAME_PRIMARY}, RawContacts.CONTACT_ID +" = '" + contactID + "'", null, null);
		if (cursor.getCount() > 0){
			cursor.moveToFirst();
			Contact contact = new Contact();
			contact.ID = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
			contact.name = cursor.getString(cursor.getColumnIndex(RawContacts.DISPLAY_NAME_PRIMARY));
			cursor.close();
			return contact;
		}
		cursor.close();
		return null;

    }
    
    private static void fillIcons(Context c){
        AccountManager am = AccountManager.get(c);
        AuthenticatorDescription[] auths = am.getAuthenticatorTypes();
        PackageManager pm = c.getPackageManager();
        for (AuthenticatorDescription auth : auths){
        	accountIcons.put(auth.type, pm.getDrawable(auth.packageName,  auth.iconId, null));
        /*	Log.i("Account:", auth.type);
        	Log.i("pkg:", auth.packageName);
        	Log.i("icon:", String.valueOf(auth.smallIconId));*/

        }
    }
    
    
    
    public static List<HashMap<String, Object>> getMergedContacts(Context c, long rawContactID){
    	
    	
		Cursor cursor = c.getContentResolver().query(RawContacts.CONTENT_URI, new String[] { RawContacts.CONTACT_ID}, RawContacts._ID +" = '" + rawContactID + "'", null, null);
		ArrayList<HashMap<String, Object>> contacts = new ArrayList<HashMap<String, Object>>();
		
		if (cursor.getCount() > 0){
			cursor.moveToFirst();
			long contactID = cursor.getLong(cursor.getColumnIndex(RawContacts.CONTACT_ID));
			Cursor c2 = c.getContentResolver().query(RawContacts.CONTENT_URI, new String[] { BaseColumns._ID, RawContacts.DISPLAY_NAME_PRIMARY, RawContacts.ACCOUNT_TYPE}, RawContacts.CONTACT_ID +" = '" + contactID + "'" + " AND " + BaseColumns._ID + " != " +rawContactID , null, null);
			while(c2.moveToNext()){
				HashMap<String, Object> contact = new HashMap<String, Object>();
				contact.put("name", c2.getString(c2.getColumnIndex(RawContacts.DISPLAY_NAME_PRIMARY)));
				contact.put("id", c2.getString(c2.getColumnIndex(BaseColumns._ID)));
				Drawable icon;
				//todo: cache this shit
				fillIcons(c);
				try{
					icon = accountIcons.get(c2.getString(c2.getColumnIndex(RawContacts.ACCOUNT_TYPE)));
				} catch (Exception e){
					fillIcons(c);
					icon = accountIcons.get(c2.getString(c2.getColumnIndex(RawContacts.ACCOUNT_TYPE)));
				}
				contact.put("icon", icon);
				contacts.add(contact);
			}
			c2.close();

		}
		cursor.close();
		return contacts;

    }

	public static void merge(Context c, long id1, long id2){
		 Log.i("MERGING", id1 +","+id2);
		 ContentValues values = new ContentValues();
	     values.put(ContactsContract.AggregationExceptions.RAW_CONTACT_ID1, id1);
	     values.put(ContactsContract.AggregationExceptions.RAW_CONTACT_ID2, id2);
	     values.put(ContactsContract.AggregationExceptions.TYPE,ContactsContract.AggregationExceptions.TYPE_KEEP_TOGETHER);
	     c.getContentResolver().update(ContactsContract.AggregationExceptions.CONTENT_URI, values, null, null);
	}
	
	private static Set<Long> getRawContacts(Context c, long contactID, long rawContactID){
		HashSet<Long> ids = new HashSet<Long>();
			Cursor c2 = c.getContentResolver().query(RawContacts.CONTENT_URI, new String[] { BaseColumns._ID}, RawContacts.CONTACT_ID +" = '" + contactID + "'" + " AND " + BaseColumns._ID + " != " +rawContactID , null, null);
			while (c2.moveToNext()){
				ids.add(c2.getLong(c2.getColumnIndex(BaseColumns._ID)));
			}
			c2.close();
		return ids;
	}
	
	public static Set<Long> getRawContacts(ContentResolver c, long rawContactID, String accountType){
		HashSet<Long> ids = new HashSet<Long>();
		Cursor cursor = c.query(RawContacts.CONTENT_URI, new String[] { RawContacts.CONTACT_ID}, RawContacts._ID +" = '" + rawContactID + "'", null, null);	
		if (cursor.getCount() > 0){
			cursor.moveToFirst();
			long contactID = cursor.getLong(cursor.getColumnIndex(RawContacts.CONTACT_ID));
			//	Log.i("QUERY", RawContacts.CONTACT_ID +" = '" + contactID + "'" + " AND " + BaseColumns._ID + " != " +rawContactID + " AND " + RawContacts.ACCOUNT_TYPE + " = '" + accountType+"'");
				Cursor c2 = c.query(RawContacts.CONTENT_URI, new String[] { BaseColumns._ID}, RawContacts.CONTACT_ID +" = '" + contactID + "'" + " AND " + BaseColumns._ID + " != " +rawContactID + " AND " + RawContacts.ACCOUNT_TYPE + " = '" + accountType+"'", null, null);
			//	Log.i("CURSOR SIZE", String.valueOf(c2.getCount()));
				while (c2.moveToNext()){
					ids.add(c2.getLong(c2.getColumnIndex(BaseColumns._ID)));
				}
				c2.close();
		}
		cursor.close();
		return ids;
	}
	
	public static void mergeWithContact(Context c, long rawContactID, long contactID){
		ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
		Set<Long> ids = getRawContacts(c, contactID, rawContactID);
		for (long id: ids){
			ContentProviderOperation.Builder builder = ContentProviderOperation
					.newUpdate(ContactsContract.AggregationExceptions.CONTENT_URI);
			builder.withValue(
					ContactsContract.AggregationExceptions.RAW_CONTACT_ID1, rawContactID);
			builder.withValue(
					ContactsContract.AggregationExceptions.RAW_CONTACT_ID2, id);
			builder.withValue(
					ContactsContract.AggregationExceptions.TYPE,ContactsContract.AggregationExceptions.TYPE_KEEP_TOGETHER);
			operationList.add(builder.build());
		}
		if(operationList.size() > 0)
			try {
				c.getContentResolver().applyBatch(ContactsContract.AUTHORITY, operationList);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (OperationApplicationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

	}
	
	public static void seperate(Context c, long rawContactID){
		ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
		Cursor cursor = c.getContentResolver().query(RawContacts.CONTENT_URI, new String[] { RawContacts.CONTACT_ID}, RawContacts._ID +" = '" + rawContactID + "'", null, null);
		if (cursor.moveToFirst()){
			long contactID = cursor.getLong(cursor.getColumnIndex(RawContacts.CONTACT_ID));
			Set<Long> ids = getRawContacts(c, contactID, rawContactID);
			for (long id: ids){
				ContentProviderOperation.Builder builder = ContentProviderOperation
						.newUpdate(ContactsContract.AggregationExceptions.CONTENT_URI);
				builder.withValue(
						ContactsContract.AggregationExceptions.RAW_CONTACT_ID1, rawContactID);
				builder.withValue(
						ContactsContract.AggregationExceptions.RAW_CONTACT_ID2, id);
				builder.withValue(
						ContactsContract.AggregationExceptions.TYPE,ContactsContract.AggregationExceptions.TYPE_KEEP_SEPARATE);
				operationList.add(builder.build());
			}
			
			if(operationList.size() > 0)
				try {
					c.getContentResolver().applyBatch(ContactsContract.AUTHORITY, operationList);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (OperationApplicationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
		cursor.close();
		}
	
	public static Photo getPhoto(ContentResolver c, long rawContactId){
		Photo photo = new Photo();
		String where = ContactsContract.Data.RAW_CONTACT_ID + " = '" + rawContactId 
				+ "' AND " + ContactsContract.Data.MIMETYPE + " = '" + ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE + "'";



		Cursor c1 = c.query(ContactsContract.Data.CONTENT_URI, new String[] {ContactsContract.CommonDataKinds.Photo.PHOTO, ContactsContract.Data.SYNC2, ContactsContract.Data.SYNC3 }, where , null, null);
		if (c1.getCount() > 0){
			c1.moveToLast();
			photo.data = c1.getBlob(c1.getColumnIndex(ContactsContract.CommonDataKinds.Photo.PHOTO));
			photo.timestamp = Long.valueOf(c1.getString(c1.getColumnIndex(ContactsContract.Data.SYNC2)));
			photo.url = c1.getString(c1.getColumnIndex(ContactsContract.Data.SYNC3));
		}
		c1.close();
		return photo;
	}
	
	public static void updateContactPhoto(ContentResolver c, long rawContactId, Photo pic, boolean primary){
		ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
		

		
		//insert new picture
		try {
			if(pic.data != null) {
                //delete old picture
                String where = ContactsContract.Data.RAW_CONTACT_ID + " = '" + rawContactId
                        + "' AND " + ContactsContract.Data.MIMETYPE + " = '" + ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE + "'";
                Log.i(TAG, "Deleting picture: "+where);

                ContentProviderOperation.Builder builder = ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI);
                builder.withSelection(where, null);
                operationList.add(builder.build());
				builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
				builder.withValue(ContactsContract.CommonDataKinds.Photo.RAW_CONTACT_ID, rawContactId);
				builder.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE);
				builder.withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, pic.data);
				builder.withValue(ContactsContract.Data.SYNC2, String.valueOf(pic.timestamp));
				builder.withValue(ContactsContract.Data.SYNC3, pic.url);
                if (primary)
				    builder.withValue(ContactsContract.Data.IS_SUPER_PRIMARY, 1);
				operationList.add(builder.build());

				
			}
			c.applyBatch(ContactsContract.AUTHORITY,	operationList);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			Log.e("ERROR:" , e.toString());
		}


	}
	
	public static void removeBirthdays(Context c, long rawContactId){
		String where = ContactsContract.Data.RAW_CONTACT_ID + " = '" + rawContactId 
				+ "' AND " + ContactsContract.Data.MIMETYPE + " = '" + ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE
				+ "' AND " + ContactsContract.CommonDataKinds.Event.TYPE + " = '" + ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY + "'";
		c.getContentResolver().delete(ContactsContract.Data.CONTENT_URI, where, null);
	}
	
	public static void removeEmails(Context c, long rawContactId){
		String where = ContactsContract.Data.RAW_CONTACT_ID + " = '" + rawContactId 
				+ "' AND " + ContactsContract.Data.MIMETYPE + " = '" + ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE+ "'";
		c.getContentResolver().delete(ContactsContract.Data.CONTENT_URI, where, null);
	}
	
	public static void addEmail(Context c, long rawContactId, String email){
		DeviceUtil.log(c, "adding email", email);
		String where = ContactsContract.Data.RAW_CONTACT_ID + " = '" + rawContactId 
				+ "' AND " + ContactsContract.Data.MIMETYPE + " = '" + ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE+ "'";
		Cursor cursor = c.getContentResolver().query(ContactsContract.Data.CONTENT_URI, new String[] { RawContacts.CONTACT_ID}, where, null, null);
		if (cursor.getCount() == 0){
			ContentValues contentValues = new ContentValues();
			//op.put(ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID, );
			contentValues.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
			contentValues.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
			contentValues.put(ContactsContract.CommonDataKinds.Email.ADDRESS, email);
			c.getContentResolver().insert(ContactsContract.Data.CONTENT_URI, contentValues);
		}
		cursor.close();

	}

	public static void addBirthday(long rawContactId, String birthday){
		String where = ContactsContract.Data.RAW_CONTACT_ID + " = '" + rawContactId 
				+ "' AND " + ContactsContract.Data.MIMETYPE + " = '" + ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE
				+ "' AND " + ContactsContract.CommonDataKinds.Event.TYPE + " = '" + ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY + "'";
		Cursor cursor = ContactsSyncAdapterService.mContentResolver.query(ContactsContract.Data.CONTENT_URI, null, where, null, null);
		int count = cursor.getCount();
		cursor.close();
		if (count <= 0){
			ContentValues contentValues = new ContentValues();
			contentValues.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE);
			contentValues.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
			contentValues.put(ContactsContract.CommonDataKinds.Event.TYPE, ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY);
			contentValues.put(ContactsContract.CommonDataKinds.Event.START_DATE, birthday);
			
			try {
				ContactsSyncAdapterService.mContentResolver.insert(ContactsContract.Data.CONTENT_URI, contentValues);
			//	mContentResolver.applyBatch(ContactsContract.AUTHORITY,	operationList);
			} catch (Exception e) {
				e.printStackTrace();
				//Log.e("ERROR:" , e.^);
			}
		}
	}

	public static void removeContactLocations(Context c, Account account){
		ContactsSyncAdapterService.mContentResolver = c.getContentResolver();
		HashMap<String, ContactsSyncAdapterService.SyncEntry> localContacts = ContactsSyncAdapterService.getLocalContacts(account);
		for (ContactsSyncAdapterService.SyncEntry s : localContacts.values()){
			ContactsSyncAdapterService.mContentResolver.delete(ContactsContract.Data.CONTENT_URI, ContactsContract.Data.MIMETYPE + " = '" + ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE + "' AND " + ContactsContract.Data.RAW_CONTACT_ID + " = " + s.raw_id, null); 
		}			
		
	}

	public static void updateContactLocation(long rawContactId, String country, String region, String city){
		if ((country == null || country.equals("")) && (region == null || region.equals("")) && (city == null || city.equals(""))){
			return;
		}
		String where = ContactsContract.Data.RAW_CONTACT_ID + " = '" + rawContactId 
				+ "' AND " + ContactsContract.Data.MIMETYPE + " = '" + ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE + "'";
		String[] projection = {ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY, ContactsContract.CommonDataKinds.StructuredPostal.REGION, ContactsContract.CommonDataKinds.StructuredPostal.CITY};
		
		Cursor cursor = ContactsSyncAdapterService.mContentResolver.query(ContactsContract.Data.CONTENT_URI, projection, where, null, null);
		boolean insert = false;
		if (cursor.getCount() == 0){
			insert = true; 
		} else{
			cursor.moveToFirst();
			String oldCountry = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY));
			String oldRegion = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.REGION));
			String oldCity = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.CITY));
			if ((oldCountry != null && !oldCountry.equals(country)) || (oldRegion != null && !oldRegion.equals(region)) || (oldCity != null && oldCity.equals(city))){
				ContactsSyncAdapterService.mContentResolver.delete(ContactsContract.Data.CONTENT_URI, where, null);
				insert = true;
			}
		}
		cursor.close();
		if (insert){
			ContentValues contentValues = new ContentValues();
			//op.put(ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID, );
			contentValues.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE);
			contentValues.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
			if (country != null && ! country.equals("")){
				contentValues.put(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY, country);
			} if (region != null && ! region.equals("")){
			contentValues.put(ContactsContract.CommonDataKinds.StructuredPostal.REGION, region);
			} if (city != null && ! city.equals("")){
			contentValues.put(ContactsContract.CommonDataKinds.StructuredPostal.CITY, city);
			}
			try {
				ContactsSyncAdapterService.mContentResolver.insert(ContactsContract.Data.CONTENT_URI, contentValues);
			//	mContentResolver.applyBatch(ContactsContract.AUTHORITY,	operationList);
			} catch (Exception e) {
				e.printStackTrace();
				//Log.e("ERROR:" , e.^);
			}
		}
	
	}

    public static void updateContactLocation(long rawContactId, String location){
        if ((location == null || location.equals(""))){
            return;
        }
        String where = ContactsContract.Data.RAW_CONTACT_ID + " = '" + rawContactId
                + "' AND " + ContactsContract.Data.MIMETYPE + " = '" + StructuredPostal.CONTENT_ITEM_TYPE + "'";
        String[] projection = {StructuredPostal.FORMATTED_ADDRESS};

        Cursor cursor = ContactsSyncAdapterService.mContentResolver.query(ContactsContract.Data.CONTENT_URI, projection, where, null, null);
        boolean insert = false;
        if (cursor.getCount() == 0){
            insert = true;
        } else{
            cursor.moveToFirst();
            String oldloc = cursor.getString(cursor.getColumnIndex(StructuredPostal.FORMATTED_ADDRESS));
            if ((oldloc == null) || (!oldloc.equals(location))){
                ContactsSyncAdapterService.mContentResolver.delete(ContactsContract.Data.CONTENT_URI, where, null);
                insert = true;
            }
        }
        cursor.close();
        if (insert){
            ContentValues contentValues = new ContentValues();
            //op.put(ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID, );
            contentValues.put(ContactsContract.Data.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE);
            contentValues.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
            contentValues.put(StructuredPostal.FORMATTED_ADDRESS, location);
            try {
                ContactsSyncAdapterService.mContentResolver.insert(ContactsContract.Data.CONTENT_URI, contentValues);
                //	mContentResolver.applyBatch(ContactsContract.AUTHORITY,	operationList);
            } catch (Exception e) {
                e.printStackTrace();
                //Log.e("ERROR:" , e.^);
            }
        }

    }
	

}
