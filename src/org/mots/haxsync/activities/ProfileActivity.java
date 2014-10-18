package org.mots.haxsync.activities;

import org.mots.haxsync.utilities.DeviceUtil;
import org.mots.haxsync.utilities.intents.IntentBuilder;
import org.mots.haxsync.utilities.intents.IntentUtil;
import org.mots.haxsync.utilities.intents.Stream;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

public class ProfileActivity extends Activity {
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getIntent().getData() != null) {
			Cursor cursor = managedQuery(getIntent().getData(), null, null, null, null);
			if (cursor.moveToNext()) {
				String username = cursor.getString(cursor.getColumnIndex("DATA1"));
								
				IntentBuilder builder = IntentUtil.getIntentBuilder(this);
				Intent intent = builder.getProfileIntent(username);
				if (!DeviceUtil.isCallable(this, intent)){
					builder = IntentUtil.getFallbackBuilder();
					intent = builder.getProfileIntent(username);
				}
				this.startActivity(intent);

				finish();

				if (DeviceUtil.isCallable(this, intent)){
					this.startActivity(intent);
				//fall back to browser if user doesn't have FB App installed.
				} else {
					Intent browserIntent = new Intent(Intent.ACTION_VIEW, 	Uri.parse("http://m.facebook.com/profile.php?id="+username)); 
					this.startActivity(browserIntent); 
				}
				finish();
			}
		} else {
			// How did we get here without data?
			finish();
		}
	}
	
}
