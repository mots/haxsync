package org.mots.haxsync.activities;

import org.mots.haxsync.R;
import org.mots.haxsync.services.CalendarSyncAdapterService;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.NumberPicker;

public class EventReminder extends Activity {
	private Button OkButton;
	private NumberPicker days;
	private NumberPicker hours;
	private NumberPicker minutes;
	private SharedPreferences prefs;
	
    public final class ReminderUpdater extends AsyncTask<String, Void, Void>{
    	private final Context context;
    	private final Account account;
    	private final long minutes;

    	protected ReminderUpdater(Context c, Account a, long minutes){
    		this.context = c;
    		this.account = a;
    		this.minutes = minutes;
    	}

    	@Override
    	protected Void doInBackground(String... params) {
    		for (String cal : params){
    			CalendarSyncAdapterService.updateReminders(context, account, cal, minutes);
    		}
    		return null;
    	}

    }


	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.event_reminder);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		days = (NumberPicker) findViewById(R.id.Days);
		hours = (NumberPicker) findViewById(R.id.Hours);
		minutes = (NumberPicker) findViewById(R.id.Minutes);
		days.setMaxValue(365);
		hours.setMaxValue(23);
		minutes.setMaxValue(59);
		minutes.setValue(30);
		long reminder_minutes = prefs.getLong("event_reminder_minutes", 30);
		if (reminder_minutes != 30){
			int[] time = minutesToTime(reminder_minutes);
			days.setValue(time[0]);
			hours.setValue(time[1]);
			minutes.setValue(time[2]);
		}
		OkButton = (Button) findViewById(R.id.OK);
		OkButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				SharedPreferences.Editor editor = prefs.edit();
				editor.putLong("event_reminder_minutes", getMinutes());
				editor.commit();
				
				AccountManager am = AccountManager.get(EventReminder.this);
				Account account = am.getAccountsByType("org.mots.haxsync.account")[0];
				new EventReminder.ReminderUpdater(EventReminder.this, account, getMinutes()).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, EventReminder.this.getString(R.string.event_cal));
				EventReminder.this.finish();
			}
		});
	}
	
	private int[] minutesToTime(long minutes){
		int[] time = new int[3];
		time[0] = (int) (minutes / 1440);
		minutes -= time[0] * 1440L;
		time[1] = (int) (minutes / 60);
		minutes -= time[1] *60;
		time[2] = (int) minutes;
		return time;
	}
	
	private long getMinutes() {
		long minutesl = days.getValue() * 1440L
				+ hours.getValue() * 60
				+ minutes.getValue();
		return minutesl;
	}

}
