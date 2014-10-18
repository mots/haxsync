package org.mots.haxsync.activities;

import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.mots.haxsync.R;
import org.mots.haxsync.utilities.FacebookUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


public class GoogleBackup extends Activity {
	
	private SharedPreferences prefs;
	//private ProgressBar progressBar;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.google_backup);
		AccountManager am = AccountManager.get(this);
		final Account[] googleAccounts = am.getAccountsByType("com.google");
		if (googleAccounts.length > 1){
			final CharSequence[] items = new CharSequence[googleAccounts.length];
			for (int i = 0; i < googleAccounts.length; i++){
				items[i] = googleAccounts[i].name;
			}

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(this.getString(R.string.google_select));
			builder.setItems(items, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {
			    	googleAccounts[0] = googleAccounts[item];
		//	        Toast.makeText(getApplicationContext(), items[item], Toast.LENGTH_SHORT).show();
			    }
			});
			AlertDialog alert = builder.create();
			alert.show();
		}
		final Account googleAcc = googleAccounts[0];

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getString(R.string.google_backup_warning))
				.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
		                GoogleBackup.this.finish();						
					}
				})
		       .setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		       		new FriendWorker(GoogleBackup.this, googleAcc).execute();
		           }
		       })
		       .setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                GoogleBackup.this.finish();
		           }
		       });
		AlertDialog alert = builder.create();
		alert.show();
		//progressBar = (ProgressBar) findViewById(R.id.progressBar);
	//	new FriendWorker(this).execute();
		//ListView friendList = (ListView) findViewById(R.id.friendListView);
		//friendList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		//friendList.set
	}
	





	
	
	final class FriendWorker extends AsyncTask<Void, Integer, Boolean>{
		private final GoogleBackup parent;
		private String contactName = "";
		private final Account googleAcc;
		
		protected FriendWorker(final GoogleBackup parent, final Account googleAcc){
			this.parent = parent;
			this.googleAcc = googleAcc;
			
		}
		
		private void writeHTCData(long rawContactID, String fbID, String friendID){
			String note = "";
			
			ContentResolver mContentResolver = parent.getContentResolver();
			
	        String noteWhere = ContactsContract.Data.MIMETYPE + " = ? AND " + ContactsContract.Data.RAW_CONTACT_ID + "= ?";
	        String[] noteWhereParams = new String[]{ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE, String.valueOf(rawContactID)};
	        Cursor cursor = mContentResolver.query(ContactsContract.Data.CONTENT_URI, new String[] {ContactsContract.CommonDataKinds.Note.NOTE}, noteWhere, noteWhereParams, null);
			if (cursor.getCount() > 0){
				cursor.moveToFirst();
				note = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Note.NOTE));
			}	
			cursor.close();
			
			Document doc = null;
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = null;
			try {
				docBuilder = docFactory.newDocumentBuilder();
			} catch (ParserConfigurationException e1) {
				e1.printStackTrace();
			}
			
			if (docBuilder == null){
				return;
			}

			if (note.startsWith("<HTCData>")){
				try {
					StringReader reader = new StringReader( note );
					InputSource inputSource = new InputSource( reader );
					doc = docBuilder.parse(inputSource);
					reader.close();

				} catch (Exception e) {
					Log.e("ERROR:", e.toString());
				}
			}
			if (doc == null){
				doc = docBuilder.newDocument();
				Node htcdata = doc.createElement("HTCData");
				doc.appendChild(htcdata);
			}
			
			
			Node fb = doc.createElement("Facebook");
			fb.setTextContent("id:"+friendID+"/friendof:"+fbID);
			Node htc = doc.getFirstChild();
			NodeList oldfb = doc.getElementsByTagName("Facebook");
			if (oldfb.getLength() > 0){
				htc.replaceChild(fb, oldfb.item(0));
			} else{
				htc.appendChild(fb);
			}
			String xmlString = null;
			try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

			StreamResult result = new StreamResult(new StringWriter());
			DOMSource source = new DOMSource(doc);
			transformer.transform(source, result);
			xmlString = result.getWriter().toString();
		//	Log.i("xml", xmlString);

			} catch (TransformerException e) {
				// TODO Auto-generated catch block
				return;
			}
			
			if (xmlString != null){
			mContentResolver.delete(ContactsContract.Data.CONTENT_URI, noteWhere, noteWhereParams);
			ContentValues contentValues = new ContentValues();
			contentValues.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE);
			contentValues.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactID);
			contentValues.put(ContactsContract.CommonDataKinds.Note.NOTE, xmlString);
			mContentResolver.insert(ContactsContract.Data.CONTENT_URI, contentValues);
			}




		}
		
		protected Boolean doInBackground(Void... params){
			AccountManager am = AccountManager.get(parent);
			Account account = am.getAccountsByType(parent.getString(R.string.ACCOUNT_TYPE))[0];
			
			if (FacebookUtil.authorize(parent, account)){
				String selfID = FacebookUtil.getSelfID();
				if (selfID == null){
					return false;
				}
				String googleName = googleAcc.name;
				Uri rawContactUri = RawContacts.CONTENT_URI.buildUpon()
						.appendQueryParameter(RawContacts.ACCOUNT_NAME, account.name)
						.appendQueryParameter(RawContacts.ACCOUNT_TYPE, account.type)
						.build();
				Uri googleUri = RawContacts.CONTENT_URI.buildUpon()
						.appendQueryParameter(RawContacts.ACCOUNT_NAME, googleName)
						.appendQueryParameter(RawContacts.ACCOUNT_TYPE, "com.google")
						.build();
				ContentResolver mContentResolver = parent.getContentResolver();
				Cursor c1 = mContentResolver.query(rawContactUri, new String[] { BaseColumns._ID, RawContacts.CONTACT_ID, RawContacts.DISPLAY_NAME_PRIMARY, RawContacts.SYNC1 }, null, null, null);
				while (c1.moveToNext()) {
					long contactID = c1.getLong(c1.getColumnIndex(RawContacts.CONTACT_ID));
					Cursor c2 = mContentResolver.query(googleUri, new String[] { BaseColumns._ID}, RawContacts.CONTACT_ID +" = '" + contactID + "'", null, null);
					if (c2.getCount() > 0){
						c2.moveToFirst();
						contactName = c1.getString(c1.getColumnIndex(RawContacts.DISPLAY_NAME_PRIMARY));
						writeHTCData(c2.getLong(c2.getColumnIndex(BaseColumns._ID)), selfID, c1.getString(c1.getColumnIndex(RawContacts.SYNC1)));
						publishProgress((int) ((c1.getPosition() / (float) c1.getCount()) * 100));
	
					}
					c2.close();
					//Log.i("backup", );
					
				}
				c1.close();
				ContentResolver.requestSync(googleAcc, ContactsContract.AUTHORITY, new Bundle());
				return true;
			}
			else{
				return false;
			}
		}
		
	     protected void onProgressUpdate(Integer... progress) {
	    	 ProgressBar update = (ProgressBar)parent.findViewById(R.id.progressBar);
	    	 TextView  name = (TextView)parent.findViewById(R.id.contactName);
	    	 name.setText(contactName);
	    	 update.setProgress(progress[0]);
	     }
		
		protected void onPostExecute(boolean result){
			if(!result){
				Toast toast = Toast.makeText(parent, "Error connecting to Facebook.", Toast.LENGTH_LONG);
				toast.show();
			}
			parent.finish();
			//Log.i("test", "onpostexecute");
		//	parent.onWorkerFinished(result);
		}
		
	}

	
	
	
}
