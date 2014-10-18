package org.mots.haxsync.provider;

import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import org.mots.haxsync.utilities.FacebookUtil;

import java.util.ArrayList;

/**
 * Created by mots on 6/23/13.
 */
public class FacebookGraphFriend implements  Friend{

    private  JSONObject json;
    private static final String TAG = "FacebookGraphFriend";

    @Override
    public String toString(){
        return "Name: "+this.getName(false) +", FB-ID: "+this.getUserName();
    }

    public FacebookGraphFriend(JSONObject json){
        this.json = json;
    }

    @Override
    public String getName(boolean ignoreMiddleNames) {
        String name = null;
        try{
            if(!ignoreMiddleNames){
                name = json.getString("name");
                Log.i(TAG, "name without middlename" + name);
            }

            else{
                name = json.getString("first_name") + " " + json.getString("last_name");
             }
        }catch (JSONException e){
            Log.e(TAG, e.toString());
         }
        return 	name;

    }
    //returns lame @facebook.com email because the API doesn't allow anything else
    public String getEmail(){
        try {
            return json.getString("username") + "@facebook.com";
        } catch (JSONException e) {
            return null;
        }
    }

    public String getLocation(){
        String location = null;
        try {
            JSONObject loc = json.getJSONObject("location");
            location = loc.getString("name");
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
        }
        return location;
    }

    @Override
    public String getUserName() {
        String username = null;
        try {
            username = json.getString("id");
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
        }
        return 	username;
    }

    @Override
    public String getPicURL() {
        String picUrl = null;
        try {
            JSONObject pic = json.getJSONObject("picture").getJSONObject("data");
            if (pic.getBoolean("is_silhouette"))
                return picUrl;
            picUrl = pic.getString("url");
        } catch (JSONException e){
            Log.e(TAG, e.toString());
        }
        return picUrl;
    }

    public String getBirthday(){
        try {
            String birthday = json.getString("birthday");
            String[] birthdayArray = birthday.split("/");
            if (birthdayArray.length == 3){
                return birthdayArray[2] + "-" + birthdayArray[0] + "-" + birthdayArray[1];
            } else if (birthdayArray.length == 2){
                return "--" + birthdayArray[0]  + "-" + birthdayArray[1];
            }

        } catch (JSONException e) {}
        return 	null;
    }

    //graph api doesn't seem to support this :/
    @Override
    public long getPicTimestamp() {
        return 0;
    }

    @Override
    public ArrayList<Status> getStatuses() {
        return FacebookUtil.getStatuses(getUserName(), false);
    }
}
