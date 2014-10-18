package org.mots.haxsync.utilities;

import android.util.Log;

import com.jjnford.android.util.Shell;
import com.jjnford.android.util.Shell.ShellException;

public class RootUtil {
	private static final String PICTURE_DIR = "/data/data/com.android.providers.contacts/files/photos/";
	
	public static boolean isRoot(){
		return Shell.su();
	}
	
	public static String movePic(String path, String file) throws ShellException{
		String newpath = PICTURE_DIR + file;
		String command = "mv " + path + " " + PICTURE_DIR + file;
		Log.i("COMMAND", command);
		Shell.sudo(command);
		return newpath;
	}
	
	public static void changeOwner(String file) throws ShellException{
		String command = "chown app_1.app_1 " + file;
		Log.i("COMMAND", command);
		Shell.sudo(command);
	}
	
	public static void refreshContacts() throws ShellException{
		Shell.sudo("pm disable com.android.providers.contacts");
		Shell.sudo("pm enable com.android.providers.contacts");
	}
	
	public static String listPics() throws ShellException{
		return Shell.sudo("ls " + PICTURE_DIR);
	}
	
	

}
