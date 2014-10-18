package org.mots.haxsync.activities;


import org.mots.haxsync.R;
import org.mots.haxsync.utilities.FacebookUtil;

import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.facebook.android.Facebook.DialogListener;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.util.Log;

public class AuthorizationActivity extends Activity {
	public final String TAG = "AuthorizationActivity";
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		  FacebookUtil.facebook.authorizeCallback(requestCode, resultCode, data);
		}
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.main);
		String[] permissions = {"offline_access", "read_stream", "user_events", "friends_events", "friends_status", "user_status", "friends_photos", "user_photos", "friends_about_me", "friends_website", "email", "friends_birthday", "friends_location"};
		FacebookUtil.facebook.authorize(AuthorizationActivity.this, permissions, Facebook.FORCE_DIALOG_AUTH, new DialogListener() {
			@Override
			public void onComplete(Bundle values) {
				FacebookUtil.accessToken = FacebookUtil.facebook.getAccessToken();
				Log.i("Expires", String.valueOf(FacebookUtil.facebook.getAccessExpires()));;
				AccountManager am = AccountManager.get(AuthorizationActivity.this);
				Account account = am.getAccountsByType(AuthorizationActivity.this.getString(R.string.ACCOUNT_TYPE))[0];
				am.setPassword(account, FacebookUtil.facebook.getAccessToken());
				SharedPreferences prefs = AuthorizationActivity.this.getSharedPreferences(AuthorizationActivity.this.getPackageName() + "_preferences", MODE_MULTI_PROCESS);
				SharedPreferences.Editor editor = prefs.edit();
				editor.putInt("permission_level", FacebookUtil.PERMISSION_LEVEL);
				editor.putLong("access_expires", FacebookUtil.facebook.getAccessExpires());
				editor.commit();
				ContentResolver.requestSync(account, ContactsContract.AUTHORITY, new Bundle());
				ContentResolver.requestSync(account, CalendarContract.AUTHORITY, new Bundle());
			//	Log.i(TAG, ContentResolver.);
				//Log.i(TAG, "Calendar :" + ContentResolver.isSyncActive(account, CalendarContract.AUTHORITY));
			//	Log.i(TAG, "Contacts :" + ContentResolver.isSyncActive(account, ContactsContract.AUTHORITY));
				FacebookUtil.isExtendingToken = false;
				AuthorizationActivity.this.finish();
			}

			@Override
			public void onFacebookError(FacebookError error) {
				Log.i(TAG, "fberror");
				AuthorizationActivity.this.finish();
			}

			@Override
			public void onError(DialogError e) {
				Log.i(TAG, "error");
				AuthorizationActivity.this.finish();
			}

			@Override
			public void onCancel() {
				Log.i(TAG, "cancel");
				AuthorizationActivity.this.finish();
			}
		});
	}


}
