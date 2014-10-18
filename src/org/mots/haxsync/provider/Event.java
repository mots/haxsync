package org.mots.haxsync.provider;

import org.json.JSONException;
import org.json.JSONObject;
import org.mots.haxsync.utilities.CalendarUtil;
import org.mots.haxsync.utilities.FacebookUtil;

public class Event {
	private JSONObject json;

	public Event(JSONObject json) {
		this.json = json;
	}
	
	public long getEventID(){
		try {
			return json.getLong("eid");
		} catch (JSONException e) {
			return -2;
		}
	}

	public String getLocation() {
		try {
			return json.getString("location");
		} catch (JSONException e) {
			return null;
		}
	}

	public long getStartTime() {
		try {
			return CalendarUtil.convertTime(json.getLong("start_time") * 1000);
		} catch (JSONException e) {
			try {
				String timeString = json.getString("start_time");
				return CalendarUtil.ISOtoEpoch(timeString);
			} catch (JSONException e1) {
				e.printStackTrace();
				return -2;
			}
		}
	}

	public long getEndTime() {
		try {
			return CalendarUtil.convertTime(json.getLong("end_time") * 1000);
		} catch (JSONException e) {
			try {
				String timeString = json.getString("end_time");
				if (timeString.equals("null"))
					return getStartTime() + 3600000;
				return CalendarUtil.ISOtoEpoch(timeString);
			} catch (JSONException e1) {
				e.printStackTrace();
				return -2;
			}
		}
	}

	public String getDescription() {
		try {
			return json.getString("description");
		} catch (JSONException e) {
			return null;
		}
	}

	public String getName() {
		try {
			return json.getString("name");
		} catch (JSONException e) {
			return null;
		}
	}
	
	public int getRsvp() {
		return FacebookUtil.convertStatus(FacebookUtil.getSelfAttendance(getEventID()));
	}

}
