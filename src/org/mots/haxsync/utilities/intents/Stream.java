package org.mots.haxsync.utilities.intents;

import android.content.Intent;

public class Stream implements IntentBuilder {
	private String prefix;
	
	public Stream(String prefix){
		this.prefix = prefix;
	}

	@Override
	public Intent getPostIntent(String postID, String uid, String permalink) {
		Intent intent = new Intent(prefix+".action.SHOW_POST");
		intent.putExtra(prefix+"id", postID);
		intent.putExtra(prefix + "from", "");
		intent.putExtra(prefix + "to", "");
		intent.putExtra(prefix + "fromid", "");
		intent.putExtra(prefix + "message", "");
		intent.putExtra(prefix + "haallegato", false);
		intent.putExtra(prefix + "picture", "");
		intent.putExtra(prefix + "link", "");
		intent.putExtra(prefix + "object_id", "");
		intent.putExtra(prefix + "source", "");
		intent.putExtra(prefix + "name", "");
		intent.putExtra(prefix + "caption", "");
		intent.putExtra(prefix + "description", "");
		intent.putExtra(prefix + "data", "");
		intent.putExtra(prefix + "like", "");
		intent.putExtra(prefix + "comm", "");
		intent.putExtra(prefix + "type", "");
		intent.putExtra(prefix + "icon", "");
		intent.putExtra(prefix + "story", "");
		return intent;
	}

	@Override
	public Intent getPhotoIntent(String pid) {
		Intent intent = new Intent(prefix+".action.SHOW_FOTO");
		intent.putExtra(prefix+"id", pid);
		return intent;
	}

	@Override
	public Intent getProfileIntent(String uid) {
		Intent intent = new Intent(prefix+".action.SHOW_PROFILE");
		intent.putExtra(prefix+"id", uid);
		return intent;
	}

}
