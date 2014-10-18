package org.mots.haxsync.activities;

import org.mots.haxsync.R;
import org.mots.haxsync.utilities.DeviceUtil;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.ContactsContract;

public class ContinueReceiver extends BroadcastReceiver {
	private static final String TAG = "WifiReceiver";


	@Override
	public void onReceive(Context context, Intent intent) {
		SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_MULTI_PROCESS);
		boolean wifiOnly = prefs.getBoolean("wifi_only", false);
		boolean chargingOnly = prefs.getBoolean("charging_only", false);
		String action = intent.getAction();
		if((wifiOnly && DeviceUtil.isWifi(context) && action.equals("android.net.wifi.STATE_CHANGE")) || (chargingOnly && action.equals("android.intent.action.ACTION_POWER_CONNECTED"))){
			AccountManager am = AccountManager.get(context);
			Account[] accs = am.getAccountsByType(context.getString(R.string.ACCOUNT_TYPE));
			if (accs.length > 0){
				Account account = accs[0];
				SharedPreferences.Editor editor = prefs.edit();
				if (prefs.getBoolean("missed_contact_sync", false)){
					ContentResolver.requestSync(account, ContactsContract.AUTHORITY, new Bundle());
					editor.putBoolean("missed_contact_sync", false);
				}
				if (prefs.getBoolean("missed_calendar_sync", false)){
					ContentResolver.requestSync(account, CalendarContract.AUTHORITY, new Bundle());
					editor.putBoolean("missed_calendar_sync", false);
				}
				editor.commit();
			} 
		}
	}

}
