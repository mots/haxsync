package org.mots.haxsync.fragments;

import org.mots.haxsync.activities.FriendPicker;
import org.mots.haxsync.R;

import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SyncStatusObserver;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ListFragment;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.RawContacts;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AlphabetIndexer;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorAdapter;
import android.app.LoaderManager;

public class ContactListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>, SyncStatusObserver{

    private static final String STATE_ACTIVATED_POSITION = "activated_position";

    private Callbacks mCallbacks = sDummyCallbacks;
    private int mActivatedPosition = ListView.INVALID_POSITION;
    private ContactsCursorAdapter mAdapter;
    private MenuItem addContact;

	private Object mContentProviderHandle;

    public interface Callbacks {

		public void onItemSelected(long id);
		
    }

    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onItemSelected(long id) {
        }
        
       
    };

    public ContactListFragment() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        setEmptyText(getActivity().getString(R.string.no_contacts));
        setHasOptionsMenu(true);


		String[] columns = new String[] {RawContacts.DISPLAY_NAME_PRIMARY};
		int[] to = new int[] { android.R.id.text1 };
		mAdapter = new ContactsCursorAdapter(
		        getActivity(),
		        android.R.layout.simple_list_item_activated_1, 
		        null,
		        columns,
		        to,
		        0);
		setListAdapter(mAdapter);
		showSyncIndicator();
	    mContentProviderHandle = ContentResolver.addStatusChangeListener(
	              ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE, this);

    }
    
    @Override
	public void onPause() {
      super.onPause();
      ContentResolver.removeStatusChangeListener(mContentProviderHandle);
    }

    @Override
	public void onResume() {
      super.onResume();
      //hideIfSyncing();
      mContentProviderHandle = ContentResolver.addStatusChangeListener(
          ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE, this);
    }
    
    private void showSyncIndicator(){
    	AccountManager am = AccountManager.get(getActivity());
    	Account account = am.getAccountsByType(getActivity().getString(R.string.ACCOUNT_TYPE))[0];
    	final boolean isSyncing = (ContentResolver.isSyncActive(account, "com.android.contacts") || ContentResolver.isSyncPending(account, "com.android.contacts"));
    	getActivity().runOnUiThread(new Runnable() {
    		@Override
    		public void run() {
    			if (isSyncing) {
    				if (((LinearLayout) getView()).getChildCount() == 1)
    				((LinearLayout) getView()).addView(LayoutInflater.from(getActivity()).inflate(R.layout.contact_loading_indicator, null), 0);
    			} else{
    				if (((LinearLayout) getView()).getChildCount() > 1)
    					((LinearLayout) getView()).removeViewAt(0);
    			}
    		}
    	});
    }
    
    private void hideList(){
        if (addContact != null)
        	addContact.setEnabled(false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null && savedInstanceState
                .containsKey(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
        }
       // getListView().setFastScrollEnabled(true);
   //     getListView().setFastScrollAlwaysVisible(true);
        getLoaderManager().initLoader(0, null, this);

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }
    

    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		SharedPreferences prefs = getActivity().getSharedPreferences(getActivity().getPackageName() + "_preferences", android.content.Context.MODE_MULTI_PROCESS);
		boolean phoneOnly = prefs.getBoolean("phone_only", true);
		if (phoneOnly){
			inflater.inflate(R.menu.contact_list_menu, menu);
	    	addContact = menu.findItem(R.id.add_contact);
		}
    	super.onCreateOptionsMenu(menu, inflater);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.contact_list, null);
        return view;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	case R.id.add_contact:
    		final Intent intent = new Intent(getActivity(), FriendPicker.class);
    		startActivity(intent);
    		return true;
        default:
            return super.onOptionsItemSelected(item);
    	}
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);
        mCallbacks.onItemSelected(id);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActivatedPosition != ListView.INVALID_POSITION) {
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
    }

    public void setActivateOnItemClick(boolean activateOnItemClick) {
        getListView().setChoiceMode(activateOnItemClick
                ? ListView.CHOICE_MODE_SINGLE
                : ListView.CHOICE_MODE_NONE);
    }

    public void setActivatedPosition(int position) {
        if (position == ListView.INVALID_POSITION) {
            getListView().setItemChecked(mActivatedPosition, false);
        } else {
            getListView().setItemChecked(position, true);
        }

        mActivatedPosition = position;
    }
    
    private boolean isTwoPane(){
		 return (getActivity().findViewById(R.id.contact_detail_container) != null);
    }

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		Activity ac = getActivity();
		AccountManager am = AccountManager.get(ac);
		Account account = am.getAccountsByType(ac.getString(R.string.ACCOUNT_TYPE))[0];
		Uri rawContactUri = RawContacts.CONTENT_URI.buildUpon()
					.appendQueryParameter(RawContacts.ACCOUNT_NAME, account.name)
					.appendQueryParameter(RawContacts.ACCOUNT_TYPE, account.type)
					.build();

		return new CursorLoader(ac, rawContactUri,
				new String[] {RawContacts._ID, RawContacts.DISPLAY_NAME_PRIMARY},
				null, null, RawContacts.DISPLAY_NAME_PRIMARY + " ASC");
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		 mAdapter.swapCursor(data);
		 ListView lv = getListView();
		 lv.setFastScrollEnabled(true);
		 lv.setFastScrollAlwaysVisible(true);
		 if (isTwoPane()){
			 lv.setVerticalScrollbarPosition(ListView.SCROLLBAR_POSITION_LEFT);
			 lv.setPadding(15, 0, 0, 0);
		 }
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
        mAdapter.swapCursor(null);
		
	}
	
	private class ContactsCursorAdapter extends SimpleCursorAdapter implements SectionIndexer{

	    private AlphabetIndexer mAlphaIndexer;
	    
	    @SuppressWarnings("deprecation")
		public ContactsCursorAdapter(Context context, int layout, Cursor c,
	            String[] from, int[] to) {
	            super(context, layout, c, from, to);
	    }

		public ContactsCursorAdapter(Context context, int layout, Cursor c,
				String[] from, int[] to, int flags) {
			super(context, layout, c, from, to, flags);
			// TODO Auto-generated constructor stub
		}

		@Override
		public int getPositionForSection(int section) {
			return mAlphaIndexer.getPositionForSection(section);
		}

		@Override
		public int getSectionForPosition(int position) {
			return mAlphaIndexer.getSectionForPosition(position);
		}

		@Override
		public Object[] getSections() {
			return mAlphaIndexer.getSections();
		}
		
	    public Cursor swapCursor(Cursor c) {
	        // Create our indexer
	        if (c != null) {
	            mAlphaIndexer = new AlphabetIndexer(c, c.getColumnIndex(RawContacts.DISPLAY_NAME_PRIMARY),
	                " ABCDEFGHIJKLMNOPQRSTUVWXYZ");
	        }
	        return super.swapCursor(c);
	    }

		
	}

	@Override
	public void onStatusChanged(int which) {
		showSyncIndicator();
	}
}
