package org.mots.haxsync.activities;

import org.mots.haxsync.R;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.NumberPicker;

public class SyncPopup extends Activity {
	private Button OkButton;
	private NumberPicker days;
	private NumberPicker hours;
	private NumberPicker minutes;
	private SharedPreferences prefs;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sync_freq_popup);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		days = (NumberPicker) findViewById(R.id.Days);
		hours = (NumberPicker) findViewById(R.id.Hours);
		minutes = (NumberPicker) findViewById(R.id.Minutes);
		days.setMaxValue(365);
		hours.setMaxValue(23);
		minutes.setMaxValue(59);
		days.setValue(1);
		long sync_seconds = prefs.getLong("sync_seconds", 86400);
		if (sync_seconds != 86400){
			int[] time = secondsToTime(sync_seconds);
			days.setValue(time[0]);
			hours.setValue(time[1]);
			minutes.setValue(time[2]);
		}
		OkButton = (Button) findViewById(R.id.OK);
		OkButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				setSyncRate(getSeconds());
				//Log.i("SECONDS", Long.toString(getSeconds()));
				SyncPopup.this.finish();
			}
		});
	}
	
	private int[] secondsToTime(long seconds){
		int[] time = new int[3];
		time[0] = (int) (seconds / 86400);
		seconds -= time[0] * 86400L;
		time[1] = (int) (seconds / 3600);
		seconds -= time[1] *3600;
		time[2] = (int) (seconds / 60);
		return time;
	}
	
	private void setSyncRate(long seconds){
		AccountManager am = AccountManager.get(this);
		Account account = am.getAccountsByType("org.mots.haxsync.account")[0];
		ContentResolver.addPeriodicSync(account, ContactsContract.AUTHORITY, new Bundle(), seconds);
		ContentResolver.addPeriodicSync(account, CalendarContract.AUTHORITY, new Bundle(), seconds);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putLong("sync_seconds", seconds);
		editor.commit();
	}

	private long getSeconds() {
		long seconds = days.getValue() * 86400L
				+ hours.getValue() * 3600
				+ minutes.getValue() * 60;
		return Math.max(60, seconds);
	}

}
