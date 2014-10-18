package org.mots.haxsync.fragments;

import java.util.HashMap;
import java.util.List;

import org.mots.haxsync.R;
import org.mots.haxsync.utilities.ContactUtil;
import org.mots.haxsync.utilities.BitmapUtil;
import org.mots.haxsync.utilities.ContactUtil.Photo;
import org.mots.haxsync.utilities.DeviceUtil;
import org.mots.haxsync.utilities.FacebookUtil;
import org.mots.haxsync.utilities.FacebookUtil.PicInfo;
import org.mots.haxsync.utilities.WebUtil;

import android.content.Context;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Fragment;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.ViewSwitcher;

public class ContactDetailFragment extends Fragment {

    public static final String CONTACT_ID = "contact_id";
    
    private String name;
    private Uri imageURI;
    private String uid;
    private long rawID;
    private long contactID;
    private PicInfo pic;
    private byte[] picture;
    private MenuItem cropImage;
    private MenuItem reloadImage;
    
    
    private ListView listView;
    
    private Button joinButton;
    
    private ViewSwitcher picSwitcher;
    
    private List<HashMap<String, Object>> joined;
    
    private ImageView imageView;
    
    private static final int CONTACT_PICKER_RESULT = 1001;  
    private static final int CROP_RESULT = 1002;  
    

    public ContactDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getArguments().containsKey(CONTACT_ID)) {
        	rawID = getArguments().getLong(CONTACT_ID);
        	if (rawID != -1)
        		getContactDetails(rawID);
        }
    }
    
    
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    	inflater.inflate(R.menu.contact_detail_menu, menu);
    	cropImage = menu.findItem(R.id.crop_image);
    	reloadImage = menu.findItem(R.id.force_image_dl);
    	super.onCreateOptionsMenu(menu, inflater);
    }
    
    private void toggleItems(){
    	cropImage.setEnabled(!cropImage.isEnabled());
    	reloadImage.setEnabled(!reloadImage.isEnabled());
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	case R.id.crop_image:
    		toggleItems();
    		picSwitcher.showNext();
			new ImageCropper().execute(uid);
			return true;
    	case R.id.force_image_dl:
    		toggleItems();
    		picSwitcher.showNext();
			new ImageRefresher().execute(uid);
			return true;
        default:
            return super.onOptionsItemSelected(item);
    	}
    }
    


    private final SimpleAdapter.ViewBinder mViewBinder =
    	    new SimpleAdapter.ViewBinder() {
    	        @Override
    	        public boolean setViewValue(
    	                final View view,
    	                final Object data,
    	                final String textRepresentation) {
    	            if (view instanceof ImageView) {
   // 	            	Log.i("IMAGEVIEW", data.toString());
    	                ((ImageView) view).setImageDrawable((Drawable) data);
    	                return true;
    	            }

    	            return false;
    	        }
    	    };


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_contact_detail, container, false);
        imageView = (ImageView) rootView.findViewById(R.id.contact_image);
        picSwitcher = (ViewSwitcher) rootView.findViewById(R.id.picSwitcher);
        joinButton = (Button) rootView.findViewById(R.id.joinButton);
        Button seperateButton =  (Button) rootView.findViewById(R.id.seperate);
        if (name != null)
        	getActivity().getActionBar().setTitle(name);
        if (imageURI != null)
        	imageView.setImageURI(imageURI);
        if (joined != null){
            String[] from = new String[] {"name", "icon"};
            int[] to = new int[] {R.id.label, R.id.icon};
            SimpleAdapter adapter = new SimpleAdapter(getActivity(), joined, R.layout.list_row, from, to);
        	listView = (ListView) rootView.findViewById(R.id.joinedList);
        	adapter.setViewBinder(mViewBinder);
        	listView.setAdapter(adapter);        	
        	
        }
        if (joinButton != null){
	        joinButton.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Intent intent = new Intent();
				    intent.setAction("com.android.contacts.action.JOIN_CONTACT");
				    intent.putExtra("com.android.contacts.action.CONTACT_ID", contactID);
				    //HTC Devices don't offer this intent, fall back to a normal "pick contact" one.
				    //TouchWiz breaks, so fall back there, too
				    boolean touchwiz = DeviceUtil.isTouchWiz(getActivity());
				    boolean sense = DeviceUtil.isSense(getActivity());
				    Log.i("IS TOUCHWIZ", String.valueOf(touchwiz));
				    Log.i("IS SENSE", String.valueOf(sense));
				    if ( (!DeviceUtil.isCallable(getActivity(), intent)) || touchwiz || sense)
						intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
				    ContactDetailFragment.this.startActivityForResult(intent, CONTACT_PICKER_RESULT);
				    
				}
			});
        }
        
        if (seperateButton != null){
	        seperateButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
	            	ContactUtil.seperate(getActivity(), rawID);
	            	ContactDetailFragment.this.refreshJoinedList();
	            	getContactDetails(rawID);
				}
			});
        }
       return rootView;
    }
    
    private void startCropping(String localUri){
		final Intent intent = new Intent(getActivity(), com.android.camera.CropImage.class);
		intent.setData(Uri.parse("file://" + localUri));
		SharedPreferences prefs = getActivity().getSharedPreferences(getActivity().getPackageName() + "_preferences", Activity.MODE_MULTI_PROCESS);
		boolean cropPhotos = prefs.getBoolean("crop_square", true);
		if (cropPhotos){
			intent.putExtra("aspectX", 1);
			intent.putExtra("aspectY", 1);
		}
		intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.parse("file://"+getActivity().getCacheDir().getAbsolutePath()+"/cropped.png"));
		ContactDetailFragment.this.startActivityForResult(intent, CROP_RESULT);

    }
    
    private void refreshJoinedList(){
        joined = ContactUtil.getMergedContacts(getActivity(), rawID);
        String[] from = new String[] {"name", "icon"};
        int[] to = new int[] {R.id.label, R.id.icon};
        SimpleAdapter adapter = new SimpleAdapter(getActivity(), joined, R.layout.list_row, from, to);
    	adapter.setViewBinder(mViewBinder);
    	listView.setAdapter(adapter);
    }
    
    
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        } if (requestCode == CROP_RESULT){
	        String uriString = "file://"+getActivity().getCacheDir().getAbsolutePath()+"/cropped.png";
	        ContactUtil.Photo photo = new Photo();
	        photo.data = WebUtil.download(uriString);
	        photo.timestamp = pic.timestamp;
	        photo.url = pic.url;
	        ContactUtil.updateContactPhoto(getActivity().getContentResolver(), rawID, photo, true);
	        imageView.setImageURI(null);
	        imageView.setImageURI(imageURI);
        } else if (requestCode == CONTACT_PICKER_RESULT){
        	long contactID = Long.valueOf(data.getData().getLastPathSegment());
        	if (contactID != this.contactID)
        		ContactUtil.mergeWithContact(getActivity(), rawID, contactID);
            refreshJoinedList();

        }
        }

    
    private void getContactDetails(long id){
		Cursor cursor = getActivity().getContentResolver().query(RawContacts.CONTENT_URI, new String[] {RawContacts._ID, RawContacts.DISPLAY_NAME_PRIMARY, RawContacts.CONTACT_ID, RawContacts.SYNC1}, RawContacts._ID + "=" +id, null, null);
		if (cursor.getColumnCount() >= 1){
			cursor.moveToFirst();
			name = cursor.getString(cursor.getColumnIndex(RawContacts.DISPLAY_NAME_PRIMARY));
			uid = cursor.getString(cursor.getColumnIndex(RawContacts.SYNC1));
			joined = ContactUtil.getMergedContacts(getActivity(), id);
			contactID = cursor.getLong(cursor.getColumnIndex(RawContacts.CONTACT_ID));
			
		}
		cursor.close();
		imageURI = Uri.withAppendedPath(
		ContentUris.withAppendedId(RawContacts.CONTENT_URI, id),
        RawContacts.DisplayPhoto.CONTENT_DIRECTORY);
		Log.i("imageuri", imageURI.toString());
    }
    

    
    public abstract class ImageGetter extends AsyncTask<String, Void, Void>{

    	@Override
    	protected Void doInBackground(String... params) {
    		for (String param : params){
    		//	ContactDetailFragment.this.progressBar.setVisibility(View.VISIBLE);
    			AccountManager am = AccountManager.get(getActivity());
    			Account account = am.getAccountsByType("org.mots.haxsync.account")[0];
				FacebookUtil.authorize(getActivity(), account);
    			ContactDetailFragment.this.pic = FacebookUtil.getProfilePicInfo(param);
    			ContactDetailFragment.this.picture = WebUtil.download(ContactDetailFragment.this.pic.url);
    			useImage(picture);
    		}
    		return null;
    	}
    	
    	protected abstract void useImage(byte[] image);
    	
    	protected void onPostExecute (Void result){
            //((BitmapDrawable)ContactDetailFragment.this.imageView.getDrawable()).getBitmap().recycle();
    		Log.i("imageuri", imageURI.toString());
    		ContactDetailFragment.this.imageView.setImageURI(null);
    		ContactDetailFragment.this.imageView.setImageURI(ContactDetailFragment.this.imageURI);
    		ContactDetailFragment.this.imageView.invalidate();
    		ContactDetailFragment.this.picSwitcher.showPrevious();
    		toggleItems();
    	}

    }
    
    public final class ImageRefresher extends ImageGetter{

		@Override
		protected void useImage(byte[] image) {
			SharedPreferences prefs = ContactDetailFragment.this.getActivity().getSharedPreferences(ContactDetailFragment.this.getActivity().getPackageName() + "_preferences", Context.MODE_MULTI_PROCESS);		
			boolean cropPhotos = prefs.getBoolean("crop_square", true);
			boolean faceDetect = prefs.getBoolean("face_detect", true);
			int maxSize = BitmapUtil.getMaxSize(ContactDetailFragment.this.getActivity());
			if(cropPhotos)
				image = BitmapUtil.resize(image, maxSize, faceDetect);
			ContactUtil.Photo photo = new Photo();
			photo.data = image;
			photo.timestamp = ContactDetailFragment.this.pic.timestamp;
			photo.url = ContactDetailFragment.this.pic.url;
			ContactUtil.updateContactPhoto(ContactDetailFragment.this.getActivity().getContentResolver(), ContactDetailFragment.this.rawID, photo, true);
		}
    }
    
    public final class ImageCropper extends ImageGetter{

		@Override
		protected void useImage(byte[] image) {
			String localuri = DeviceUtil.saveBytes(image, getActivity().getCacheDir().getAbsoluteFile());
			ContactDetailFragment.this.startCropping(localuri);
		}
    	
    }

}
