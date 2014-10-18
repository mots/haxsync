package org.mots.haxsync.utilities.intents;

import android.content.Intent;

public interface IntentBuilder {

	/**
	 * @param postID the id of the post
	 * @param uid the uid of the poster
	 * @param permalink a permalink to the post
	 * @return an intent that displays the post
	 */
	public Intent getPostIntent(String postID, String uid, String permalink);
	
	

	/**
	 * @param objectID the object_id of a photo object
	 * @return an intent that displays the photo
	 */
	public Intent getPhotoIntent(String objectID);
	
	
	/**
	 * @param uid the uid of the user in question
	 * @return an intent that displays the user's profile page
	 */
	public Intent getProfileIntent(String uid);

}
