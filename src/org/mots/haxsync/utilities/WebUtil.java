package org.mots.haxsync.utilities;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class WebUtil {

	public static byte[] download(String urlString){
	    ByteArrayOutputStream bais = new ByteArrayOutputStream();
	    InputStream is = null;
	    URL url = null;
		try {
			url = new URL(urlString);
	      is = url.openStream ();
	      byte[] byteChunk = new byte[4096];
	      int n;
	
	      while ( (n = is.read(byteChunk)) > 0 ) {
	        bais.write(byteChunk, 0, n);
	      }
	    }
	    catch (Exception e) {
	    	e.printStackTrace();
	    	if (urlString.contains("fbcdn_sphotos_")){
	    		String alt = urlString;
	    		urlString.replace("fbcdn_sphotos_", "fbcdn-sphotos-");
	    		alt.replace("fbcdn_sphotos_a-a.akamaihd.net", "a1.sphotos.ak.fbcdn.net");
	    		byte[] res = download(urlString);
	    		return (res == null) ? download(alt) : res;
	    	}
	    	return null;
	    }
	    finally {
	      if (is != null) { try {
			is.close();
		} catch (IOException e) {
			return null;
		} }
	    }
	
	    return bais.toByteArray();
	}

}
