package org.mots.haxsync.utilities;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import android.os.Build;
import org.mots.haxsync.R;
import org.mots.haxsync.activities.WelcomeActivity;
import org.mots.haxsync.activities.WorkaroundConfirmation;


import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.BatteryManager;
import android.util.Log;

public class DeviceUtil {

	public static boolean isOnline(Context c) {
		ConnectivityManager connectionManager = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectionManager.getActiveNetworkInfo() != null && connectionManager.getActiveNetworkInfo().isConnected()){
			try {
				InetAddress addr = InetAddress.getByName("graph.facebook.com");
				addr = InetAddress.getByName("api.facebook.com");
			} catch (UnknownHostException e) {
				return false;
			}
			return true;
		}
		return false;
	}

	public static boolean isWifi(Context c){
	    ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
	        NetworkInfo networkInfo = null;
	        if (cm != null) {
	            networkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
	        }
	        return networkInfo != null && networkInfo.isConnected();
	}

	public static boolean isCharging(Context context){
		IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent batteryStatus = context.registerReceiver(null, ifilter);
		// Are we charging / charged?
		int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
		return (status == BatteryManager.BATTERY_STATUS_CHARGING) || (status == BatteryManager.BATTERY_STATUS_FULL);
	}
	
	public static boolean hasAccount(Context context){
		AccountManager am = AccountManager.get(context);
		return (am.getAccountsByType("org.mots.haxsync.account").length > 0);
	}
	
	public static boolean needsWorkaround(Context context){
        //workaround is required on 4.1 and all samsung devices <4.3
        if(android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1){
          return false;
        }


        if ((android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN)
                && (android.os.Build.VERSION.SDK_INT < 15 || Build.MANUFACTURER.toLowerCase().contains("samsung"))
                && (android.os.Build.VERSION.SDK_INT < 15 || Build.MODEL.toLowerCase().contains("maxx"))){
			SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Activity.MODE_MULTI_PROCESS);
			if (!prefs.getBoolean("workaround_ignore", false)){
				try
				{
					context.getPackageManager().getApplicationInfo("com.haxsync.facebook.workaround", 0);
				}catch (PackageManager.NameNotFoundException e){
					return true;
				}
			}
		}
		return false;
	}
	
	public static Account getAccount(Context c){
		AccountManager am = AccountManager.get(c);
		Account[] accounts = am.getAccountsByType(c.getString(R.string.ACCOUNT_TYPE));
		if (accounts.length == 0)
			return null;
		return accounts[0];
	}
	
	public static void toggleWizard(Context c, boolean display){
		PackageManager pm = c.getPackageManager();
		pm.setComponentEnabledSetting(new ComponentName(c, WelcomeActivity.class), display ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
				PackageManager.DONT_KILL_APP);
	}
	
	public static boolean isWizardShown(Context c){
		PackageManager pm = c.getPackageManager();
		return (pm.getComponentEnabledSetting(new ComponentName(c, WelcomeActivity.class)) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
	}

	
	@SuppressLint("NewApi")
	public static void showJellyBeanNotification(Context context){
		if (needsWorkaround(context)){
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse("market://details?id=com.haxsync.facebook.workaround"));


			Intent cancelIntent = new Intent(context, WorkaroundConfirmation.class);

			PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);


			PendingIntent cancelContentIntent = PendingIntent.getActivity(context, 1, cancelIntent, PendingIntent.FLAG_CANCEL_CURRENT);

			NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);


			Resources res = context.getResources();
			Notification.Builder builder = new Notification.Builder(context);


			builder.setContentIntent(contentIntent)
			.setSmallIcon(android.R.drawable.stat_notify_more)
			.setLargeIcon(BitmapFactory.decodeResource(res, R.drawable.icon))
			.setTicker(res.getString(R.string.jb_warning_ticker))
			.setWhen(System.currentTimeMillis())
			.setAutoCancel(true)
			.setContentTitle(res.getString(R.string.jb_warning_title))
			.setContentText(res.getString(R.string.jb_warning_description))
			.addAction(android.R.drawable.ic_menu_info_details, context.getString(android.R.string.yes), contentIntent)
			.addAction(android.R.drawable.ic_menu_close_clear_cancel, context.getString(android.R.string.no), cancelContentIntent);
			Notification n = new Notification.BigTextStyle(builder).bigText(context.getString(R.string.workaround_description)).build();


			nm.notify(1, n);
				
			}
		}
	

	public static String saveBytes(byte[] file, File dir){
		try {
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(dir, "photo.png")));
			bos.write(file);
			bos.flush();
			bos.close();
		} catch (Exception e) {
			return null;
		}
		return dir.getAbsolutePath()+"/photo.png";
	}

	public static boolean isCallable(Context c, Intent intent1) {    
		List<ResolveInfo> list = c.getPackageManager().queryIntentActivities(intent1,     
				PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
	
	}
	
	public static boolean isTouchWiz(Context c){
		PackageManager pm = c.getPackageManager();
		try{
			pm.getPackageInfo("com.sec.android.app.twlauncher", 0);
		} catch (PackageManager.NameNotFoundException e){
			try {
				pm.getPackageInfo("com.sec.android.app.launcher", 0);
			}  catch (PackageManager.NameNotFoundException y){
				try {
					pm.getPackageInfo("com.sec.android.app.samsungapps", 0);
				} catch (PackageManager.NameNotFoundException x){
					return false;
				}
			}
		}
		return true;
	}
	
	public static boolean isSense(Context c){
		PackageManager pm = c.getPackageManager();
		try{
			pm.getPackageInfo("com.htc", 0);
		} catch (PackageManager.NameNotFoundException e){
			return false;
		}
		return true;
	}
	
	public static void log(Context c, String tag, String message){
		SharedPreferences prefs = c.getSharedPreferences(c.getPackageName() + "_preferences", Context.MODE_MULTI_PROCESS);
		if (prefs.getBoolean("debug_logging", false))
			Log.i(tag, message);
	}
	


}
