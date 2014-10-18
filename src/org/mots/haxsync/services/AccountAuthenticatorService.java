package org.mots.haxsync.services;

import org.mots.haxsync.activities.WizardActivity;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import java.util.Arrays;


public class  AccountAuthenticatorService extends Service {
	private static final String TAG = "AccountAuthenticatorService";
	private static AccountAuthenticatorImpl sAccountAuthenticator = null;
	

	@Override
	public IBinder onBind(Intent intent) {
		IBinder ret = null;
		if (intent.getAction().equals(android.accounts.AccountManager.ACTION_AUTHENTICATOR_INTENT))
			ret = getAuthenticator().getIBinder();
		return ret;
	}
	
	 private AccountAuthenticatorImpl getAuthenticator() {
		  if (sAccountAuthenticator == null)
		   sAccountAuthenticator = new AccountAuthenticatorImpl(this);
		  return sAccountAuthenticator;
		 }
	 
	 private static class AccountAuthenticatorImpl extends AbstractAccountAuthenticator {
			private Context mContext;

			public AccountAuthenticatorImpl(Context context) {
				super(context);
				mContext = context;
			}

			@Override
			public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options)
					throws NetworkErrorException {
				Bundle result = new Bundle();
				Log.i(TAG, "entering login activity");
				Intent i = new Intent(mContext, WizardActivity.class);
				i.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
				result.putParcelable(AccountManager.KEY_INTENT, i);/*
				if (Utility.facebook != null && Utility.facebook.isSessionValid()){
					Log.i(TAG, "account is logged in");
				}*/
				return result;
			}
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * android.accounts.AbstractAccountAuthenticator#confirmCredentials(
			 * android.accounts.AccountAuthenticatorResponse,
			 * android.accounts.Account, android.os.Bundle)
			 */
			@Override
			public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) {
				// TODO Auto-generated method stub
				Log.i(TAG, "confirmCredentials");
				return null;
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * android.accounts.AbstractAccountAuthenticator#editProperties(android
			 * .accounts.AccountAuthenticatorResponse, java.lang.String)
			 */
			@Override
			public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
				// TODO Auto-generated method stub
				Log.i(TAG, "editProperties");
				return null;
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * android.accounts.AbstractAccountAuthenticator#getAuthToken(android
			 * .accounts.AccountAuthenticatorResponse, android.accounts.Account,
			 * java.lang.String, android.os.Bundle)
			 */
			@Override
			public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
				// TODO Auto-generated method stub
				Log.i(TAG, "getAuthToken");
				return null;
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * android.accounts.AbstractAccountAuthenticator#getAuthTokenLabel(java
			 * .lang.String)
			 */
			@Override
			public String getAuthTokenLabel(String authTokenType) {
				// TODO Auto-generated method stub
				Log.i(TAG, "getAuthTokenLabel");
				return null;
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * android.accounts.AbstractAccountAuthenticator#hasFeatures(android
			 * .accounts.AccountAuthenticatorResponse, android.accounts.Account,
			 * java.lang.String[])
			 */
			@Override
			public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
				// TODO Auto-generated method stub
				Log.i(TAG, "hasFeatures: " + Arrays.toString(features));
				return null;
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * android.accounts.AbstractAccountAuthenticator#updateCredentials(android
			 * .accounts.AccountAuthenticatorResponse, android.accounts.Account,
			 * java.lang.String, android.os.Bundle)
			 */
			@Override
			public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) {
				// TODO Auto-generated method stub
				Log.i(TAG, "updateCredentials");
				return null;
			}
		}


	 }



