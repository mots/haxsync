package org.mots.haxsync.activities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.mots.haxsync.R;
import org.mots.haxsync.utilities.FacebookUtil;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;


public class FriendPicker extends Activity {
	
	private ListView friendList;
	private SharedPreferences prefs;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.friend_selector_spinner);
		new FriendWorker(this).execute();
		//ListView friendList = (ListView) findViewById(R.id.friendListView);
		//friendList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		//friendList.set
	}
	
	public void onWorkerFinished(String[] friends){
		if (friends == null){
			friends = new String[] {};
		}
		setContentView(R.layout.friend_selector);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		friendList = (ListView) findViewById(R.id.friendListView);
		friendList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_activated_1, friends);
		friendList.setAdapter(adapter);
		this.setSelection();
		Button doneButton = (Button) findViewById(R.id.DoneButton);
		doneButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				FriendPicker.this.getSelected();
				AccountManager am = AccountManager.get(FriendPicker.this);
				Account account = am.getAccountsByType(FriendPicker.this.getString(R.string.ACCOUNT_TYPE))[0];
				ContentResolver.requestSync(account, ContactsContract.AUTHORITY, new Bundle());
				//setSyncRate(getSeconds());
				//Log.i("SECONDS", Long.toString(getSeconds()));
				FriendPicker.this.finish();
			}
		});
	}
	
	private void setSelection(){
		Set<String> friendSet = prefs.getStringSet("add_friends", new HashSet<String>());
		for (int i = 0; i < friendList.getCount(); i++){
			if (friendSet.contains((String) friendList.getItemAtPosition(i))){
				friendList.setItemChecked(i, true);
			}
		}

		//Log.i("child count" , Integer.toString(friendList.ge;
		//friendList.getChildAt(2).setSelected(true);
		//friendList.g
		//for(int i = 0; i < friendList.getCount(); i++){
			//friendList.setSelection(i);
			//Log.i("item", friendList.getChildAt(i).getTag().toString());
		//}
		//Log.i("selected", friendSet.toString());
	}
	
	protected void getSelected(){
		SparseBooleanArray checked = friendList.getCheckedItemPositions();
		Set<String> friendSet = new HashSet<String>();
		for (int i = 0; i < checked.size(); i++) {
			if (checked.valueAt(i)){
				friendSet.add((String) friendList.getItemAtPosition(checked.keyAt(i)));		
			}
		}
		SharedPreferences.Editor editor = prefs.edit();
		editor.putStringSet("add_friends", friendSet);
		editor.commit();
	}
	

}

final class FriendWorker extends AsyncTask<Void, Void, String[]>{
	private final FriendPicker parent;
	
	protected FriendWorker(final FriendPicker parent){
		this.parent = parent;
	}
	
	protected String[] doInBackground(final Void... params){
		ArrayList<String> friendslist = new ArrayList<String>();
		try {
		AccountManager am = AccountManager.get(parent);
		Account account = am.getAccountsByType(parent.getString(R.string.ACCOUNT_TYPE))[0];
		if (FacebookUtil.authorize(parent, account)){
		JSONArray friends = FacebookUtil.getFriendNames();
		
		//get already synced friends and remove them from the list
		Uri rawContactUri = RawContacts.CONTENT_URI.buildUpon()
				.appendQueryParameter(RawContacts.ACCOUNT_NAME, account.name)
				.appendQueryParameter(RawContacts.ACCOUNT_TYPE, account.type)
				.build();
		Cursor cursor = parent.getContentResolver().query(rawContactUri, new String[] {RawContacts.DISPLAY_NAME_PRIMARY}, null, null, null);
		HashSet<String> existing = new HashSet<String>();
		while (cursor.moveToNext()) {
			existing.add(cursor.getString(cursor.getColumnIndex(RawContacts.DISPLAY_NAME_PRIMARY)));
		}

		if (friends != null) { 
			   for (int i=0;i<friends.length();i++){ 
				   String name = friends.getJSONObject(i).getString("name");
				   if (!existing.contains(name))
					   friendslist.add(name);
			   } 
		} }}
		catch (Exception e){
			Log.e("ERROR", e.toString());
		}
		return friendslist.toArray(new String[friendslist.size()]);
	}
	
	protected void onPostExecute(String[] result){
		parent.onWorkerFinished(result);
	}
	
}
