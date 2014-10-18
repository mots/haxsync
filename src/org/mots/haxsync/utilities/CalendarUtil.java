package org.mots.haxsync.utilities;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.mots.haxsync.provider.EventAttendee;

import android.content.ContentValues;
import android.content.Context;
import android.provider.CalendarContract.Attendees;

public class CalendarUtil {
	private static SimpleDateFormat ISO8601DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
	
	public static void addAttendee(Context c, long eventID, EventAttendee attendee){
		ContentValues cv = new ContentValues();
		cv.put(Attendees.ATTENDEE_NAME, attendee.getName());
		cv.put(Attendees.ATTENDEE_EMAIL, attendee.getEmail());
		cv.put(Attendees.EVENT_ID, eventID);
		cv.put(Attendees.ATTENDEE_STATUS, attendee.getAttendeeStatus());

		c.getContentResolver().insert(Attendees.CONTENT_URI, cv);
	}
	
	public static void removeAttendees(Context c, long eventID){
		String where = Attendees.EVENT_ID + " = '" + eventID + "'";
		c.getContentResolver().delete(Attendees.CONTENT_URI, where, null);
	}
	
	public static long ISOtoEpoch(String time){
		try {
			Date d = ISO8601DATEFORMAT.parse(time);
			return d.getTime();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -2;
		}

	}

	public static long convertTime(long time){
	    GregorianCalendar t1 = new GregorianCalendar(TimeZone.getTimeZone("America/Los_Angeles"));
	    t1.setTimeInMillis(time);
	    GregorianCalendar t2 = new GregorianCalendar();
	    t2.set(t1.get(GregorianCalendar.YEAR), t1.get(GregorianCalendar.MONTH), t1.get(GregorianCalendar.DAY_OF_MONTH), t1.get(GregorianCalendar.HOUR_OF_DAY), t1.get(GregorianCalendar.MINUTE), t1.get(GregorianCalendar.SECOND));
	    return t2.getTimeInMillis();
	}

}
