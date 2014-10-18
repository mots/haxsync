package org.mots.haxsync.activities;

import org.mots.haxsync.R;
import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class Help extends Activity {
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.help);
		WebView myWebView = (WebView) findViewById(R.id.webview);
		WebSettings webSettings = myWebView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		myWebView.setWebViewClient(new WebViewClient() {  
			  @Override  
			  public boolean shouldOverrideUrlLoading(WebView view, String url)  
			  {  
			    view.loadUrl(url);
			    return true;
			  }  
			}); 
		myWebView.loadUrl("file:///android_asset/help/index.html");

		/*AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setMessage("Copyright (c) 2011 Mathias Roth. \n" +
				"Uses Code by Sam Steele (www.c99.org) licensed under the Apache Public license.");
		dialog.show();*/
	}

}
