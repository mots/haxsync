package org.mots.haxsync.activities;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mots.haxsync.R;
import org.mots.haxsync.services.CalendarSyncAdapterService;
import org.mots.haxsync.utilities.ContactUtil;
import org.mots.haxsync.utilities.FacebookUtil;
import org.mots.haxsync.utilities.intents.IntentUtil;
import org.mots.haxsync.utilities.intents.IntentUtil.NameList;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.BaseColumns;
import android.provider.CalendarContract;
import android.provider.ContactsContract.RawContacts;

public class Preferences extends PreferenceActivity {
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = getSharedPreferences(getPackageName() + "_preferences", MODE_MULTI_PROCESS);
        boolean settingsFound = prefs.getBoolean("settings_found", false);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("settings_found" , true);
        editor.commit();
	}




	@Override
	public void onBuildHeaders(List<Header> target) {
		loadHeadersFromResource(R.xml.preferences, target);
	}

	public static class GeneralFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			// Make sure default values are applied.  In a real app, you would
			// want this in a shared function that is used to retrieve the
			// SharedPreferences wherever they are needed.
			//   PreferenceManager.setDefaultValues(getActivity(),
			//      R.xml.advanced_preferences, false);

			// Load the preferences from an XML resource
			addPreferencesFromResource(R.xml.general_prefs);
	        ListPreference listPref = new ListPreference(getActivity());
	        listPref.setKey("fb_app");
	        listPref.setOrder(0);
	        NameList apps = IntentUtil.getApps(getActivity());
	        listPref.setEntries(apps.namesAvail.toArray(new CharSequence[apps.namesAvail.size()]));
	        listPref.setEntryValues(apps.pkgsAvail.toArray(new String[apps.pkgsAvail.size()]));
	        listPref.setTitle(getActivity().getString(R.string.fb_app));
	        listPref.setSummary(getActivity().getString(R.string.fb_app_description));
			getPreferenceScreen().addPreference(listPref);
		}
	}

	public static class ContactFragment extends PreferenceFragment {

		public final class LocationRemover extends AsyncTask<Void, Void, Void>{
			private final Context context;
			private final Account account;

			protected LocationRemover(Context c, Account a){
				this.context = c;
				this.account = a;
			}

			@Override
			protected Void doInBackground(Void... params) {
				ContactUtil.removeContactLocations(context, account);
				return null;
			}

		}

		public final class BirthdayRemover extends AsyncTask<Void, Void, Void>{
			private final Context context;
			private final Account account;

			protected BirthdayRemover(Context c, Account a){
				this.context = c;
				this.account = a;
			}

			@Override
			protected Void doInBackground(Void... params) {
				Uri rawContactUri = RawContacts.CONTENT_URI.buildUpon()
						.appendQueryParameter(RawContacts.ACCOUNT_NAME, account.name)
						.appendQueryParameter(RawContacts.ACCOUNT_TYPE, account.type)
						.build();
				Cursor c1 = context.getContentResolver().query(rawContactUri, new String[] { BaseColumns._ID}, null, null, null);
				while (c1.moveToNext()){
					ContactUtil.removeBirthdays(context, c1.getLong(c1.getColumnIndex(BaseColumns._ID)));
				}

				return null;
			}

		}

		public final class GoogleCopier extends AsyncTask<Void, Void, Void>{
			private final Context context;
			private final Account account;

			protected GoogleCopier(Context c, Account a){
				this.context = c;
				this.account = a;
			}



			@Override
			protected Void doInBackground(Void... params) {

				HashSet<Long> rawIds = new HashSet<Long>();
				ContentResolver mContentResolver = context.getContentResolver();
				/*				Uri rawContactUri = RawContacts.CONTENT_URI.buildUpon()
						.appendQueryParameter(RawContacts.ACCOUNT_NAME, account.name)
						.appendQueryParameter(RawContacts.ACCOUNT_TYPE, account.type)
						.build();
				 * Cursor c1 = mContentResolver.query(rawContactUri, new String[] { BaseColumns._ID , RawContacts.DISPLAY_NAME_PRIMARY}, null, null, null);
				HashMap<Long, String> names = new HashMap<Long, String>();
				while (c1.moveToNext()) {
					rawIds.add(c1.getLong(c1.getColumnIndex(BaseColumns._ID)));
					names.put(c1.getLong(c1.getColumnIndex(BaseColumns._ID)), c1.getString(c1.getColumnIndex(RawContacts.DISPLAY_NAME_PRIMARY)));
				}
				c1.close();*/

				for (long id : rawIds){
					Set<Long> linked = ContactUtil.getRawContacts(mContentResolver, id, "com.google");
				//	Log.i("haxsync contact", names.get(id));
					//Log.i("number of linked google contacts", String.valueOf(linked.size()));
					if (linked.size() > 0){
						ContactUtil.Photo photo = ContactUtil.getPhoto(mContentResolver, id);
						if (photo != null){
							for (long linkedID : linked){
								ContactUtil.updateContactPhoto(mContentResolver, linkedID, photo, false);
							}
						}
					}
				}

				return null;
			}
		}

		public final class EmailRemover extends AsyncTask<Void, Void, Void>{
			private final Context context;
			private final Account account;

			protected EmailRemover(Context c, Account a){
				this.context = c;
				this.account = a;
			}

			@Override
			protected Void doInBackground(Void... params) {
				Uri rawContactUri = RawContacts.CONTENT_URI.buildUpon()
						.appendQueryParameter(RawContacts.ACCOUNT_NAME, account.name)
						.appendQueryParameter(RawContacts.ACCOUNT_TYPE, account.type)
						.build();
				Cursor c1 = context.getContentResolver().query(rawContactUri, new String[] { BaseColumns._ID}, null, null, null);
				while (c1.moveToNext()){
					ContactUtil.removeEmails(context, c1.getLong(c1.getColumnIndex(BaseColumns._ID)));
				}

				return null;
			}

		}



		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.contact_prefs);
			if (Build.VERSION.SDK_INT < 15){
				Preference newApi = findPreference("status_new");
				newApi.setEnabled(false);
			}

			Preference locSync = findPreference("sync_location");
			
			
			//            Preference rootEnabled = findPreference("root_enabled");
			//            
			//            
			//            rootEnabled.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			//
			//    			@Override
			//    			public boolean onPreferenceChange(Preference preference, Object newValue) {
			//    				return RootUtil.isRoot();
			//    			}
			//
			//            });

			locSync.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					boolean sync = Boolean.valueOf(String.valueOf(newValue));

					if (!sync){
						Context context = preference.getContext();
						AccountManager am = AccountManager.get(context);
						Account account = am.getAccountsByType("org.mots.haxsync.account")[0];
						new ContactFragment.LocationRemover(context, account).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
					}


					return true;
				}
			});

			Preference birthdaySync = findPreference("sync_contact_birthday");

			birthdaySync.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					boolean sync = Boolean.valueOf(String.valueOf(newValue));
					if (!sync){
						Context context = preference.getContext();
						AccountManager am = AccountManager.get(context);
						Account account = am.getAccountsByType("org.mots.haxsync.account")[0];
						new ContactFragment.BirthdayRemover(context, account).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
					}
					return true;

				}
			});

			Preference emails = findPreference("sync_facebook_email");
			emails.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					boolean sync = Boolean.valueOf(String.valueOf(newValue));
					if (!sync){
						Context context = preference.getContext();
						AccountManager am = AccountManager.get(context);
						Account account = am.getAccountsByType("org.mots.haxsync.account")[0];
						new ContactFragment.EmailRemover(context, account).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
					}
					return true;

				}
			});
			
			//remove features Facebook doesn't like :(
			if (FacebookUtil.RESPECT_FACEBOOK_POLICY) {
				Preference statusPrefs = findPreference("status_updates");
				getPreferenceScreen().removePreference(statusPrefs);
				
				PreferenceCategory contactCat = (PreferenceCategory) findPreference("contact_sync");
				contactCat.removePreference(locSync);
				contactCat.removePreference(emails);
				contactCat.removePreference(birthdaySync);
			}

			Preference google = findPreference("update_google_photos");
			google.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					boolean sync = Boolean.valueOf(String.valueOf(newValue));
					if (sync){
						Context context = preference.getContext();
						AccountManager am = AccountManager.get(context);
						Account account = am.getAccountsByType("org.mots.haxsync.account")[0];
						new ContactFragment.GoogleCopier(context, account).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
					}
					return true;
				}

			});
		}
	}

	public static class CalendarFragment extends PreferenceFragment {

		public final class ReminderRemover extends AsyncTask<String, Void, Void>{
			private final Context context;
			private final Account account;

			protected ReminderRemover(Context c, Account a){
				this.context = c;
				this.account = a;
			}

			@Override
			protected Void doInBackground(String... params) {
				for (String cal : params){
					CalendarSyncAdapterService.removeReminders(context, account, cal);
				}
				return null;
			}

		}

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


		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.calendar_prefs);

			Preference eventColor = findPreference("event_color");
			Preference birthdayColor = findPreference("birthday_color");

			Preference eventSync = findPreference("sync_events");
			Preference birthdaySync = findPreference("sync_birthdays");
			Preference phoneOnly = findPreference("phone_only_cal");
			Preference eventStatus = findPreference("event_status");

			Preference eventReminders = findPreference("event_reminders");

			Preference birthdayReminders = findPreference("birthday_reminders");


			eventReminders.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					boolean remind = Boolean.valueOf(String.valueOf(newValue));
					Context context = preference.getContext();
					AccountManager am = AccountManager.get(context);
					Account account = am.getAccountsByType("org.mots.haxsync.account")[0];
					if (!remind){
						new CalendarFragment.ReminderRemover(context, account).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, context.getString(R.string.event_cal));
					} else {
						SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", MODE_MULTI_PROCESS);		
						long minutes = prefs.getLong("event_reminder_minutes", 30);
						new CalendarFragment.ReminderUpdater(context, account, minutes).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, context.getString(R.string.event_cal));
					}
					return true;
				}

			});

			birthdayReminders.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					boolean remind = Boolean.valueOf(String.valueOf(newValue));
					Context context = preference.getContext();
					AccountManager am = AccountManager.get(context);
					Account account = am.getAccountsByType("org.mots.haxsync.account")[0];
					if (!remind){
						new CalendarFragment.ReminderRemover(context, account).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, context.getString(R.string.birthday_cal));
					} else {
						SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", MODE_MULTI_PROCESS);		
						long minutes = prefs.getLong("birthday_reminder_minutes", 1440);
						new CalendarFragment.ReminderUpdater(context, account, minutes).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,context.getString(R.string.birthday_cal));
					}
					return true;
				}

			});


			eventColor.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					int color = Integer.valueOf(String.valueOf(newValue));
					Context context = preference.getContext();
					AccountManager am = AccountManager.get(context);
					Account account = am.getAccountsByType("org.mots.haxsync.account")[0];
					CalendarSyncAdapterService.setCalendarColor(context, account, context.getString(R.string.event_cal), color);
					return true;
				}

			});
			birthdayColor.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					int color = Integer.valueOf(String.valueOf(newValue));
					Context context = preference.getContext();
					AccountManager am = AccountManager.get(context);
					Account account = am.getAccountsByType("org.mots.haxsync.account")[0];
					CalendarSyncAdapterService.setCalendarColor(context, account, context.getString(R.string.birthday_cal), color);
					return true;
				}

			});

			eventSync.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					boolean sync = Boolean.valueOf(String.valueOf(newValue));
					if (!sync){
						Context context = preference.getContext();
						AccountManager am = AccountManager.get(context);
						Account account = am.getAccountsByType("org.mots.haxsync.account")[0];
						CalendarSyncAdapterService.removeCalendar(context, account, context.getString(R.string.event_cal));
					}
					return true;
				}

			});

			birthdaySync.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					boolean sync = Boolean.valueOf(String.valueOf(newValue));
					if (!sync){
						Context context = preference.getContext();
						AccountManager am = AccountManager.get(context);
						Account account = am.getAccountsByType("org.mots.haxsync.account")[0];
						CalendarSyncAdapterService.removeCalendar(context, account, context.getString(R.string.birthday_cal));
					}
					return true;
				}

			});


			phoneOnly.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					boolean phoneOnlyValue= Boolean.valueOf(String.valueOf(newValue));
					if (phoneOnlyValue){
						Context context = preference.getContext();
						AccountManager am = AccountManager.get(context);
						Account account = am.getAccountsByType("org.mots.haxsync.account")[0];
						CalendarSyncAdapterService.removeCalendar(context, account, context.getString(R.string.birthday_cal));
						ContentResolver.requestSync(account, CalendarContract.AUTHORITY, new Bundle());
					}
					return true;
				}

			});

			eventStatus.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					Context context = preference.getContext();
					AccountManager am = AccountManager.get(context);
					Account account = am.getAccountsByType("org.mots.haxsync.account")[0];
					CalendarSyncAdapterService.removeCalendar(context, account, context.getString(R.string.event_cal));
					ContentResolver.requestSync(account, CalendarContract.AUTHORITY, new Bundle());
					return true;
				}

			});


		}
	}

}

