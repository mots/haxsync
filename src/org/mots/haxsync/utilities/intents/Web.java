package org.mots.haxsync.utilities.intents;

import android.content.Intent;
import android.net.Uri;

public class Web implements IntentBuilder {

	@Override
	public Intent getPostIntent(String postID, String uid, String permalink) {
		return new Intent(Intent.ACTION_VIEW, Uri.parse(permalink.replace("://www.", "://m."))); 
	}

	@Override
	public Intent getPhotoIntent(String pid) {
		return new Intent(Intent.ACTION_VIEW, Uri.parse("https://m.facebook.com/"+pid));
	}

	@Override
	public Intent getProfileIntent(String uid) {
		return new Intent(Intent.ACTION_VIEW, Uri.parse("http://m.facebook.com/profile.php?id="+uid)); 
	}

	public String getPackageName() {
		// TODO Auto-generated method stub
		return null;
	}
	
	

}
