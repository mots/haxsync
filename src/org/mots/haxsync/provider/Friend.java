package org.mots.haxsync.provider;

import java.util.ArrayList;


public interface Friend {
	public String getName(boolean ignoreMiddleNames);
	public String getUserName();
	public String getPicURL();
	public long getPicTimestamp();
	public ArrayList<Status> getStatuses();
}
