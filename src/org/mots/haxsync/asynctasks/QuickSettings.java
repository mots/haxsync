package org.mots.haxsync.asynctasks;

import org.mots.haxsync.utilities.DeviceUtil;
import org.mots.haxsync.utilities.FacebookUtil;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.ContactsContract;

public class QuickSettings extends AsyncTask<Void, Void, Void> {
	private Context context;
	private int contactChoice;
	private boolean eventSync;
	private boolean birthdaySync;
	private boolean reminders;
	
	public QuickSettings(Context context, int contactChoice, boolean eventSync, boolean birthdaySync, boolean reminders){
		this.context = context;
		this.contactChoice = contactChoice;
		this.eventSync = eventSync;
		this.birthdaySync = birthdaySync && FacebookUtil.RESPECT_FACEBOOK_POLICY;
		this.reminders = reminders;
	}

	@Override
	protected Void doInBackground(Void... params) {
		SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_MULTI_PROCESS);
		SharedPreferences.Editor editor = prefs.edit();
    	
    	editor.putBoolean("sync_events", eventSync);
    	
    	editor.putBoolean("sync_birthdays", birthdaySync);
    	editor.putBoolean("sync_contact_birthday", birthdaySync);

    	editor.putBoolean("birthday_reminders", reminders);
    	editor.putBoolean("event_reminders", reminders);
    	
    	if (contactChoice == 1){
    		editor.putBoolean("phone_only", true);
    	} else if (contactChoice == 2){
    		editor.putBoolean("phone_only", false);
    	}
    	
    	editor.commit();
    	
		Account account = DeviceUtil.getAccount(context);
		
		long seconds = prefs.getLong("sync_seconds", 86400);

    	if (birthdaySync || eventSync){
    		ContentResolver.setSyncAutomatically(account, CalendarContract.AUTHORITY, true);
    		ContentResolver.addPeriodicSync(account, CalendarContract.AUTHORITY, new Bundle(), seconds);
    	}else{
    		ContentResolver.setSyncAutomatically(account, CalendarContract.AUTHORITY, false);
    		ContentResolver.removePeriodicSync(account, CalendarContract.AUTHORITY, new Bundle());
    	}
    	
    	if (contactChoice > 0){
    		ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY, true);
    		ContentResolver.addPeriodicSync(account, ContactsContract.AUTHORITY, new Bundle(), seconds);
    	}else{
    		ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY, false);
    		ContentResolver.removePeriodicSync(account, ContactsContract.AUTHORITY, new Bundle());
    	}
    	return null;
	}
	
}
