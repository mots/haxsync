package org.mots.haxsync.activities;

import java.util.List;

import org.mots.haxsync.utilities.DeviceUtil;
import org.mots.haxsync.utilities.intents.Facebook;
import org.mots.haxsync.utilities.intents.IntentBuilder;
import org.mots.haxsync.utilities.intents.IntentUtil;
import org.mots.haxsync.utilities.intents.Stream;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

public class PostActivity extends Activity {
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		SharedPreferences prefs = this.getSharedPreferences(this.getPackageName() + "_preferences", MODE_MULTI_PROCESS);		
    	boolean syncNew = prefs.getBoolean("status_new", true);

		super.onCreate(savedInstanceState);
		if (Build.VERSION.SDK_INT < 15 || ! syncNew){
			finish();
		}else if (getIntent().getData() != null) {
			Cursor cursor = managedQuery(getIntent().getData(), null, null, null, null);
			if (cursor.moveToNext()) {
			//	Log.i("Cursor", Arrays.deepToString(cursor.getColumnNames()));
				
				String postID = cursor.getString(cursor.getColumnIndex("stream_item_sync1"));
				String uid = cursor.getString(cursor.getColumnIndex("stream_item_sync2"));
				String permalink = cursor.getString(cursor.getColumnIndex("stream_item_sync3"));
				
				IntentBuilder builder = IntentUtil.getIntentBuilder(this);
				Intent intent = builder.getPostIntent(postID, uid, permalink);
				if (!DeviceUtil.isCallable(this, intent)){
					builder = IntentUtil.getFallbackBuilder();
					intent = builder.getPostIntent(postID, uid, permalink);
				}
				this.startActivity(intent);

				finish();
			}
		} else {
			// How did we get here without data?
			finish();
		}
	}
}
