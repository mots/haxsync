package org.mots.haxsync.utilities.intents;

import android.content.Intent;
import android.net.Uri;

public class Facebook implements IntentBuilder {

	@Override
	public Intent getPostIntent(String postID, String uid, String permalink) {
		return new Intent(Intent.ACTION_VIEW, Uri.parse("fb://post/"+postID+"?owner="+uid));
	}

	@Override
	public Intent getPhotoIntent(String pid) {
		return new Intent(Intent.ACTION_VIEW, Uri.parse("fb://photo/" + pid));
	}

	@Override
	public Intent getProfileIntent(String uid) {
		return new Intent(Intent.ACTION_VIEW, Uri.parse("fb://profile/" + uid));
	}

	public String getPackageName() {
		return "com.facebook.katana";
	}

}
