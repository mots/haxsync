package org.mots.haxsync.activities;

import java.util.ArrayList;

import android.view.ViewStub;

import org.json.JSONException;
import org.json.JSONObject;
import org.mots.haxsync.R;
import org.mots.haxsync.asynctasks.QuickSettings;
import org.mots.haxsync.utilities.DeviceUtil;
import org.mots.haxsync.utilities.FacebookUtil;

import com.facebook.Settings;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.Util;

import android.net.Uri;
import android.opengl.Visibility;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.LightingColorFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.github.espiandev.showcaseview.ShowcaseView;

public class WizardActivity extends AccountAuthenticatorActivity {
	
	private ArrayList<Integer> steps = new ArrayList<Integer>() ;
	
	private String[] permissions = {"offline_access", "read_stream", "user_events", "friends_events", "friends_status", "user_status", 
			"friends_photos", "user_photos", "friends_about_me", "friends_website", "email", "friends_birthday", "friends_location"};
	
	
	private ViewFlipper flipper = null;
	private View next = null;
    private View settingsView = null;
	private TextView stepDisplay = null;
	private Spinner contactSpinner = null;
	
	private Button fbButton = null;
	private Button workaroundButton = null;
	
	private Switch eventSwitch;
	private Switch birthdaySwitch;
	private Switch reminderSwitch;
	
	private CheckBox wizardCheck;

    private ShowcaseView sv;
    private boolean isShowCase = false;
	

	
	private final String TAG = "WizardActivity";
    private View showcaseView;

    private void setupSteps(){
		if (!DeviceUtil.hasAccount(this)){
			steps.add(R.layout.wiz_fb_login);
			next.setEnabled(false);
		}
		if (DeviceUtil.needsWorkaround(this))
			steps.add(R.layout.wiz_workaround);
		if (shouldSkipSettings())
			steps.add(R.layout.wiz_existing_settings);
		steps.add(R.layout.wiz_settings);
		steps.add(R.layout.wiz_success);
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		  FacebookUtil.facebook.authorizeCallback(requestCode, resultCode, data);
	}
	
	private void toggleReminderVisibility(){
		if (birthdaySwitch.isChecked() || eventSwitch.isChecked())
			reminderSwitch.setVisibility(View.VISIBLE);
		else
			reminderSwitch.setVisibility(View.INVISIBLE);
	}
	
	private boolean shouldSkipSettings(){
		SharedPreferences prefs = getSharedPreferences(getPackageName() + "_preferences", MODE_MULTI_PROCESS);
		return ((prefs.getBoolean("sync_birthdays", true) != prefs.getBoolean("sync_contact_birthday", true)) || (prefs.getBoolean("birthday_reminders", true) != prefs.getBoolean("event_reminders", true)));	
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wizard);
        
        flipper = (ViewFlipper) findViewById(R.id.wizardFlipper);
        next = findViewById(R.id.nextView);
        settingsView = findViewById(R.id.settingsView);

        setupSteps();
        
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        for (int step : steps){
        	View stepView = inflater.inflate(step, this.flipper, false);
        	this.flipper.addView(stepView);
        }
        
        //restore active step after orientation change
        if (savedInstanceState != null) {
            int step = savedInstanceState.getInt("step");
            if (steps.contains(step))
            	flipper.setDisplayedChild(steps.indexOf(step));
        }
        
        fbButton = (Button) findViewById(R.id.fbButton);
        workaroundButton = (Button) findViewById(R.id.workaroundButton);
        
        wizardCheck = (CheckBox) findViewById(R.id.checkHide);
        
        if (fbButton != null){
        fbButton.getBackground().setColorFilter(new LightingColorFilter(1, 0xFF3B5998));
        fbButton.setOnClickListener(new View.OnClickListener() {
			
				@Override
				public void onClick(View v) {
					FacebookUtil.facebook.authorize(WizardActivity.this, permissions, new DialogListener() {
						@Override
						public void onComplete(Bundle values) {
							Log.i(TAG, FacebookUtil.facebook.getAccessToken());
							FacebookUtil.accessToken = FacebookUtil.facebook.getAccessToken();
							JSONObject user = FacebookUtil.getSelfInfoAsync();
				    		AccountManager am = AccountManager.get(WizardActivity.this);
							Log.i("Expires", String.valueOf(FacebookUtil.facebook.getAccessExpires()));;
				    		if (!DeviceUtil.hasAccount(WizardActivity.this)){
				 				try {
									Bundle result = null;
									Account account = new Account(user.getString("name"), WizardActivity.this.getString((R.string.ACCOUNT_TYPE)));
									if (am.addAccountExplicitly(account, FacebookUtil.facebook.getAccessToken(), null)) {
										result = new Bundle();
										result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
										result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
										setAccountAuthenticatorResult(result);
										SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(WizardActivity.this);
										SharedPreferences.Editor editor = prefs.edit();
										editor.putInt("permission_level", FacebookUtil.PERMISSION_LEVEL);
										editor.putLong("access_expires", FacebookUtil.facebook.getAccessExpires());
										editor.commit();
										fbButton.setText(WizardActivity.this.getText(R.string.logged_in));
										fbButton.setEnabled(false);
										next.setEnabled(true);
										flipper.showNext();
										updateNextView();
										
									}
								} catch (JSONException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
				    		}
						}
	
						@Override
						public void onFacebookError(FacebookError error) {
							Log.e(TAG, (error.getErrorType() == null) ? "no error message" : error.getErrorType());
						//	Log.e(TAG, error.getMessage());
							Log.i(TAG, "fberror");
	//						LoginActivity.this.finish();
						}
	
						@Override
						public void onError(DialogError e) {
							Log.i(TAG, "error");
		//					LoginActivity.this.finish();
						}
	
						@Override
						public void onCancel() {
							Log.i(TAG, "cancel");
			//				LoginActivity.this.finish();
						}
					
				});
			}
	        });
        }
        if (workaroundButton != null){
        workaroundButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse("market://details?id=com.haxsync.facebook.workaround"));
				startActivity(intent);
			}
		});
        }
        
    	eventSwitch = ((Switch) findViewById(R.id.switchEvent));
    	birthdaySwitch = ((Switch) findViewById(R.id.switchBirthdays));
    	
    	// :(
    	if (FacebookUtil.RESPECT_FACEBOOK_POLICY) {
    		birthdaySwitch.setVisibility(View.GONE);
    		findViewById(R.id.seperatorBirthdays).setVisibility(View.GONE);
    	}
    	
    	reminderSwitch = ((Switch) findViewById(R.id.switchReminders));

        
        
        contactSpinner = (Spinner) findViewById(R.id.contactSpinner);
        
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.ContactsChoices, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        contactSpinner.setAdapter(adapter);
        
        readSettings();
        
        eventSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				toggleReminderVisibility();
			}
		});
        birthdaySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				toggleReminderVisibility();
			}
		});
        stepDisplay = (TextView) findViewById(R.id.stepView);
        updateNextView();

        next.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
                Log.i(TAG, "child " + flipper.getDisplayedChild() + " of " + flipper.getChildCount());
                if (shouldSkipNext()){
					flipper.showNext();
				}
				else if (isSettings()){
                    SharedPreferences prefs = getSharedPreferences(getPackageName() + "_preferences", MODE_MULTI_PROCESS);
                    boolean settingsFound = prefs.getBoolean("settings_found", false);

                    //highlight the settings button in case the user hasn't found the settings yet.
                    if (!settingsFound){
                        showCaseSettings();
                    }
					applySettings();
				}
                else if (isShowCase){
                    sv.hide();
                    isShowCase = false;
                    SharedPreferences prefs = getSharedPreferences(getPackageName() + "_preferences", MODE_MULTI_PROCESS);
                    boolean settingsFound = prefs.getBoolean("settings_found", false);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("settings_found" , true);
                    editor.commit();
                    //flipper.removeView(showcaseView);
                    //flipper.removeViewAt(flipper.getDisplayedChild());
                }
				if (isLast()){
					DeviceUtil.toggleWizard(WizardActivity.this, !wizardCheck.isChecked());
					WizardActivity.this.setResult(Activity.RESULT_OK);
					WizardActivity.this.finish();
				} else{
					flipper.showNext();
                    //showcase doesn't count as step, so remove it from flipper so the step counter doesn't get messed up.
                    /*if (isShowCase){
                        flipper.removeViewAt(flipper.getDisplayedChild() - 1);
                        isShowCase = false;
                    }*/

                    updateNextView();



				}
			}
		});

        settingsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(WizardActivity.this, Preferences.class);
                startActivity(i);
            }
        });
    }

    private void showCaseSettings() {
        ShowcaseView.ConfigOptions co = new ShowcaseView.ConfigOptions();
        co.noButton = true;

        //add empty view so showcase isn't fugly
        showcaseView = new View(this);
        flipper.addView(showcaseView, flipper.getDisplayedChild()+1);
        isShowCase = true;
        //add -1 to steps array so things don't get confusing
        steps.add(steps.indexOf(R.layout.wiz_settings) + 1, -1);
        sv = ShowcaseView.insertShowcaseView(settingsView, this, R.string.preferences, R.string.preferences_summary, co);
    }


    private void readSettings(){
    	Account account = DeviceUtil.getAccount(this);
    	boolean contactSync = true;
    	boolean calendarSync = true;
    	if (account != null){
	    	contactSync = ContentResolver.getSyncAutomatically(account, ContactsContract.AUTHORITY);
	    	calendarSync = ContentResolver.getSyncAutomatically(account, CalendarContract.AUTHORITY);
    	}
		SharedPreferences prefs = getSharedPreferences(getPackageName() + "_preferences", MODE_MULTI_PROCESS);

    	if (!contactSync)
    		contactSpinner.setSelection(0);
    	else if (prefs.getBoolean("phone_only", true))
    		contactSpinner.setSelection(1);
    	else
    		contactSpinner.setSelection(2);
    	
		eventSwitch.setChecked(prefs.getBoolean("sync_events", true) && calendarSync);
		birthdaySwitch.setChecked(prefs.getBoolean("sync_birthdays", true) && calendarSync);
		reminderSwitch.setChecked(prefs.getBoolean("event_reminders", true));
		wizardCheck.setChecked(!DeviceUtil.isWizardShown(this));
		toggleReminderVisibility();
    }
    
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        int step = steps.get(flipper.getDisplayedChild());
        savedInstanceState.putInt("step", step);
    }

    
    private void applySettings(){
    	int contactChoice = contactSpinner.getSelectedItemPosition();
    	boolean eventSync = eventSwitch.isChecked();
    	boolean birthdaySync = birthdaySwitch.isChecked();
    	boolean reminders = reminderSwitch.isChecked();
    	
    	new QuickSettings(this, contactChoice, eventSync, birthdaySync, reminders).execute();
    	
    }
    
    private boolean isLast(){
    	return (flipper.getDisplayedChild()+1 == flipper.getChildCount()) && !isShowCase;
    }
    
    private boolean shouldSkipNext(){
    	if (steps.get(flipper.getDisplayedChild()) == R.layout.wiz_existing_settings){
    		return ((RadioButton) findViewById(R.id.radioSkip)).isChecked();
    	}
    	return false;
    }
    
    private boolean isSettings(){
    	return steps.get(flipper.getDisplayedChild()) == R.layout.wiz_settings;
    }
    
    private void updateNextView(){
    	if (stepDisplay != null){
    		stepDisplay.setText(getResources().getString(R.string.step, flipper.getDisplayedChild()+1, flipper.getChildCount()));
    	}
    	if (isLast()){
    	//	((TextView) findViewById(R.id.nextLabel)).setText(getResources().getString(R.string.done));
    	}
    }

    
}
