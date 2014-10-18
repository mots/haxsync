package org.mots.haxsync.services;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mots.haxsync.R;
import org.mots.haxsync.provider.Event;
import org.mots.haxsync.provider.EventAttendee;
import org.mots.haxsync.utilities.CalendarUtil;
import org.mots.haxsync.utilities.DeviceUtil;
import org.mots.haxsync.utilities.FacebookUtil;

import android.accounts.Account;
import android.accounts.OperationCanceledException;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.CalendarContract;
import android.provider.ContactsContract.RawContacts;
import android.text.format.Time;
import android.util.Log;

public class CalendarSyncAdapterService extends Service {
	private static final String TAG = "CalendarSyncAdapterService";
	private static SyncAdapterImpl sSyncAdapter = null;
	private static ContentResolver mContentResolver = null;

	public CalendarSyncAdapterService() {
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
				CalendarSyncAdapterService.performSync(mContext, account,
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
	
	
	private static long getCalendarID(Account account, String name){
		String[] projection = new String[] {
		       CalendarContract.Calendars._ID,
		       CalendarContract.Calendars.ACCOUNT_NAME,
		};
		String where = CalendarContract.Calendars.ACCOUNT_NAME + " = ? AND " + CalendarContract.Calendars.ACCOUNT_TYPE + " = '" + account.type
				+ "' AND " + CalendarContract.Calendars.CALENDAR_DISPLAY_NAME + " = '" + name +"'";
		Cursor calendarCursor = mContentResolver.query(CalendarContract.Calendars.CONTENT_URI, projection, where, new String[] {account.name}, null);
		Log.i("CALENDARS FOUND:", String.valueOf(calendarCursor.getCount()));
		if (calendarCursor.getCount() <= 0){
			calendarCursor.close();
			return -2;
		} else{
			calendarCursor.moveToFirst();
			long id = calendarCursor.getLong(calendarCursor.getColumnIndex(CalendarContract.Calendars._ID));
			calendarCursor.close();
			return id;
		}

	}
	
	private static long createCalendar(Account account, String name, int color){
		ContentValues values = new ContentValues();
		values.put(CalendarContract.Calendars.NAME, name);
		values.put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, name);
		values.put(CalendarContract.Calendars.CALENDAR_COLOR, color);
		values.put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER);
		values.put(CalendarContract.Calendars.OWNER_ACCOUNT, account.name);
		values.put(CalendarContract.Calendars.ACCOUNT_NAME, account.name);
		values.put(CalendarContract.Calendars.ACCOUNT_TYPE, account.type);
		values.put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_READ);
		values.put(CalendarContract.Calendars.SYNC_EVENTS, 1);
		values.put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, Time.getCurrentTimezone());
		Uri calSyncUri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
				.appendQueryParameter(android.provider.CalendarContract.CALLER_IS_SYNCADAPTER, "true")
				.appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, account.name)
				.appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, account.type)
				.build();
		Uri calUri = mContentResolver.insert(calSyncUri, values);	
        long calId = ContentUris.parseId(calUri);
		return calId;
	}
	
	private static long addBirthday(long calId, String name, long time){
		String where = CalendarContract.Events.CALENDAR_ID + " = " + calId + " AND " + CalendarContract.Events.TITLE + " = \"" + name +"\"";
		Cursor cursor = mContentResolver.query(CalendarContract.Events.CONTENT_URI, new String[] {CalendarContract.Events._ID}, where, null, null);
		int count = cursor.getCount();
		if (count == 0){
			cursor.close();
			ContentValues values = new ContentValues();
			values.put(CalendarContract.Events.DTSTART, time);
			values.put(CalendarContract.Events.TITLE, name);
			values.put(CalendarContract.Events.ALL_DAY, 1);
			values.put(CalendarContract.Events.RRULE, "FREQ=YEARLY");
			values.put(CalendarContract.Events.CALENDAR_ID, calId);
			values.put(CalendarContract.Events.DURATION, "P1D");
			values.put(CalendarContract.Events.EVENT_TIMEZONE, "utc");
			values.put(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_FREE);
			return Long.valueOf(mContentResolver.insert(CalendarContract.Events.CONTENT_URI, values).getLastPathSegment());
		} else {
			cursor.moveToFirst();
			long id = cursor.getLong(cursor.getColumnIndex(CalendarContract.Events._ID));
			cursor.close();
			return id;
		}

	}
	
	private static void addReminder(long eventID, long minutes){
		//delete old reminder
		String where = CalendarContract.Reminders.EVENT_ID + " = " + eventID;
		mContentResolver.delete(CalendarContract.Reminders.CONTENT_URI, where, null);
		
		ContentValues values = new ContentValues();
		values.put(CalendarContract.Reminders.EVENT_ID, eventID);
		values.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);
		values.put(CalendarContract.Reminders.MINUTES, minutes);
		
		mContentResolver.insert(CalendarContract.Reminders.CONTENT_URI, values);
		
		}
	
	private static long addEvent(Account acc, long calId, Event e){

		String name = e.getName();
		long start = e.getStartTime();
		long end = e.getEndTime();
		String location = e.getLocation();
		String description = e.getDescription();
		int rsvp = e.getRsvp();
		long eid = e.getEventID();
		Uri insertUri = CalendarContract.Events.CONTENT_URI.buildUpon()
		.appendQueryParameter(android.provider.CalendarContract.CALLER_IS_SYNCADAPTER, "true")
		.appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, acc.name)
		.appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, acc.type)
		.build();

		if (eid != -2){
			String where = CalendarContract.Events.CALENDAR_ID + " = " + calId + " AND " + CalendarContract.Events._SYNC_ID + " = " + eid;
			Cursor cursor = mContentResolver.query(CalendarContract.Events.CONTENT_URI,
					new String[] {CalendarContract.Events._ID, CalendarContract.Events.TITLE, CalendarContract.Events.DTSTART, CalendarContract.Events.DTEND,
					CalendarContract.Events.SELF_ATTENDEE_STATUS, CalendarContract.Events.EVENT_LOCATION, CalendarContract.Events.DESCRIPTION}, where, null, null);
			int count = cursor.getCount();
			if (count == 0){
				cursor.close();			
				ContentValues values = new ContentValues();
				values.put(CalendarContract.Events.DTSTART, start);
				values.put(CalendarContract.Events.DTEND, end);
				values.put(CalendarContract.Events.TITLE, name);
				values.put(CalendarContract.Events.HAS_ATTENDEE_DATA, true);
				values.put(CalendarContract.Events.SELF_ATTENDEE_STATUS, rsvp);
				values.put(CalendarContract.Events._SYNC_ID, eid);
				if (location != null){
					values.put(CalendarContract.Events.EVENT_LOCATION, location);
				}
				if (description != null)
					values.put(CalendarContract.Events.DESCRIPTION, description);
				if (rsvp != CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED){
					values.put(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_FREE);
				}else{
					values.put(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY);
				}
				values.put(CalendarContract.Events.CALENDAR_ID, calId);
				
				values.put(CalendarContract.Events.EVENT_TIMEZONE, Time.getCurrentTimezone());
				return Long.valueOf(mContentResolver.insert(insertUri, values).getLastPathSegment());
			} else {
				cursor.moveToFirst();
				long oldstart = cursor.getLong(cursor.getColumnIndex(CalendarContract.Events.DTSTART));
				long id = cursor.getLong(cursor.getColumnIndex(CalendarContract.Events._ID));
				long oldend = cursor.getLong(cursor.getColumnIndex(CalendarContract.Events.DTEND));
				String oldlocation = cursor.getString(cursor.getColumnIndex(CalendarContract.Events.EVENT_LOCATION));
				String oldDescription = cursor.getString(cursor.getColumnIndex(CalendarContract.Events.DESCRIPTION));
				int oldrsvp = cursor.getInt(cursor.getColumnIndex(CalendarContract.Events.SELF_ATTENDEE_STATUS));
				String oldname = cursor.getString(cursor.getColumnIndex(CalendarContract.Events.TITLE));
				cursor.close();
				ContentValues values = new ContentValues();
				if (oldstart != start)
					values.put(CalendarContract.Events.DTSTART, start);
				if (oldend != end)
					values.put(CalendarContract.Events.DTEND, end);
				if (! oldlocation.equals(location))
					values.put(CalendarContract.Events.EVENT_LOCATION, location);
				if (! oldDescription.equals(description))
					values.put(CalendarContract.Events.DESCRIPTION, description);
				if (! oldname.equals(name))
					values.put(CalendarContract.Events.TITLE, name);
				/*if (oldrsvp != rsvp)
					values.put(CalendarContract.Events.SELF_ATTENDEE_STATUS, rsvp);*/
				if (values.size() != 0)
					mContentResolver.update(CalendarContract.Events.CONTENT_URI, values, CalendarContract.Events._ID + " = ?", new String[] {String.valueOf(id)});
				return id;

			}
		}
		return -1;
	}
	
	public static void removeCalendar(Context context, Account account, String name){
		mContentResolver = context.getContentResolver();
		long calID = getCalendarID(account, name);
		if (calID == -2){
			return;
		}
		Uri calcUri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
				.appendQueryParameter(android.provider.CalendarContract.CALLER_IS_SYNCADAPTER, "true")
				.appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, account.name)
				.appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, account.type)
				.build();
		mContentResolver.delete(calcUri, CalendarContract.Calendars._ID + " = " +calID, null);
	}
	
	public static void setCalendarColor(Context context, Account account, String name, int color){
		mContentResolver = context.getContentResolver();
		long calID = getCalendarID(account, name);
		if (calID == -2){
			return;
		}
		ContentValues values = new ContentValues();
		values.put(CalendarContract.Calendars.CALENDAR_COLOR, color);
		Uri calcUri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
				.appendQueryParameter(android.provider.CalendarContract.CALLER_IS_SYNCADAPTER, "true")
				.appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, account.name)
				.appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, account.type)
				.build();

		mContentResolver.update(calcUri, values, CalendarContract.Calendars._ID + " = " + calID, null);

		
	}
	
	private static Set<String> getFriends(Context context, Account account){
		HashSet<String> friends = new HashSet<String>();
		Uri rawContactUri = RawContacts.CONTENT_URI.buildUpon()
				.appendQueryParameter(RawContacts.ACCOUNT_NAME, account.name)
				.appendQueryParameter(RawContacts.ACCOUNT_TYPE, account.type)
				.build();
		Cursor c1 = mContentResolver.query(rawContactUri, new String[] { RawContacts.DISPLAY_NAME_PRIMARY }, null, null, null);
		while (c1.moveToNext()) {
			friends.add(c1.getString(0));
		}
		c1.close();
		return friends;
	}
	
	public static void removeReminders(Context context, Account account, String calendarName){
		mContentResolver = context.getContentResolver();
		long calID = getCalendarID(account, calendarName);
		if (calID == -2){
			return;
		}
		for (long id : getEvents(calID)){
			String where = CalendarContract.Reminders.EVENT_ID + " = " + id;
			mContentResolver.delete(CalendarContract.Reminders.CONTENT_URI, where, null);
		}
	}
	
	public static void updateReminders(Context context, Account account, String calendarName, long minutes){
		mContentResolver = context.getContentResolver();
		long calID = getCalendarID(account, calendarName);
		if (calID == -2){
			return;
		}
		for (long id : getEvents(calID)){
			addReminder(id, minutes);
		}

	}
	
	
	private static Set<Long> getEvents(long calendarID){
		HashSet<Long> events = new HashSet<Long>();
		Cursor c1 = mContentResolver.query(CalendarContract.Events.CONTENT_URI, new String[] {CalendarContract.Events._ID}, CalendarContract.Events.CALENDAR_ID + " = " +calendarID, null, null);
		while (c1.moveToNext()) {
			events.add(c1.getLong(0));
		}
		c1.close();
		return events;
	}
	

	

	private static void performSync(Context context, Account account,
			Bundle extras, String authority, ContentProviderClient provider,
			SyncResult syncResult) throws OperationCanceledException {
		mContentResolver = context.getContentResolver();

		SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", MODE_MULTI_PROCESS);		
		
		boolean wifiOnly = prefs.getBoolean("wifi_only", false);
		boolean chargingOnly = prefs.getBoolean("charging_only", false);
		if (!((wifiOnly && !DeviceUtil.isWifi(context)) || (chargingOnly && ! DeviceUtil.isCharging(context)))){
			
			FacebookUtil.refreshPermissions(context);
			
			boolean eventSync = prefs.getBoolean("sync_events", true);
			boolean birthdaySync = prefs.getBoolean("sync_birthdays", false);
			boolean eventReminders = prefs.getBoolean("event_reminders", false);
			boolean birthdayReminders = prefs.getBoolean("birthday_reminders", false);
			long eventReminderTime = prefs.getLong("event_reminder_minutes", 30);
			long birthdayReminderTime = prefs.getLong("birthday_reminder_minutes", 1440);
			String statuses = prefs.getString("event_status", "attending|unsure");
			Log.i("event sync", String.valueOf(eventSync));
			if (FacebookUtil.authorize(context, account)){
				long birthdayCalendarID = getCalendarID(account, context.getString(R.string.birthday_cal));
				if (birthdaySync){
	
					if (birthdayCalendarID == -2){
						int birthdayColor = prefs.getInt("birthday_color", 0xff1212);
						birthdayCalendarID = createCalendar(account, context.getString(R.string.birthday_cal), birthdayColor);
					}
					
					boolean phoneOnly = prefs.getBoolean("phone_only_cal", false);
					
					HashMap<String, Long> birthdays = FacebookUtil.getBirthdays();
					
					Set<String> friends = getFriends(context, account);
					Log.i("friends", friends.toString());
					if (birthdays != null){
						for (String name : birthdays.keySet()){
							if (!phoneOnly || friends.contains(name)){
								long eventID = addBirthday(birthdayCalendarID, String.format(context.getString(R.string.birthday), name), birthdays.get(name));
								if (birthdayReminders)
									addReminder(eventID, birthdayReminderTime);
							}
						}
					}
				} else if (birthdayCalendarID != -2){
					removeCalendar(context, account, context.getString(R.string.birthday_cal));				
				}
				
				if (eventSync){
					long eventCalendarID = getCalendarID(account, context.getString(R.string.event_cal));
					if (eventCalendarID == -2){
						int color = prefs.getInt("event_color", 0xff2525);
						eventCalendarID = createCalendar(account, context.getString(R.string.event_cal), color);
					}
					List<Event> events = FacebookUtil.getEvents(statuses);
					for (Event e : events){
						long eventID = addEvent(account, eventCalendarID, e);
						CalendarUtil.removeAttendees(context, eventID);
						List<EventAttendee> attendees = FacebookUtil.getEventAttendees(e.getEventID());
						for (EventAttendee a : attendees){
							CalendarUtil.addAttendee(context, eventID, a);
						}
						if (eventReminders && (eventID != -1))
							addReminder(eventID, eventReminderTime);
					}
					
				}
	
			}
		}else {
			SharedPreferences.Editor editor = prefs.edit();
			editor.putBoolean("missed_calendar_sync", true);
			editor.commit();
			}

	}
}
