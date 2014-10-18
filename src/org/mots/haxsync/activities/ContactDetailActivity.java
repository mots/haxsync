package org.mots.haxsync.activities;

import org.mots.haxsync.R;
import org.mots.haxsync.fragments.ContactDetailFragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;

public class ContactDetailActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_detail);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            Bundle arguments = new Bundle();
            arguments.putLong(ContactDetailFragment.CONTACT_ID, getIntent().getLongExtra(ContactDetailFragment.CONTACT_ID, -1));
            ContactDetailFragment fragment = new ContactDetailFragment();
            fragment.setArguments(arguments);
            getFragmentManager().beginTransaction()
                    .add(R.id.contact_detail_container, fragment)
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            NavUtils.navigateUpTo(this, new Intent(this, ContactListActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
