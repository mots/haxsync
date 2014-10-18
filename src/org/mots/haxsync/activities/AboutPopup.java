package org.mots.haxsync.activities;

import org.mots.haxsync.R;
import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class AboutPopup extends Activity {
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about_popup);
		TextView thanksView = (TextView) findViewById(R.id.thanksView);
		thanksView.setClickable(true);
		thanksView.setMovementMethod(LinkMovementMethod.getInstance());
		thanksView.setText(Html.fromHtml(getString(R.string.thanks)));

		/*AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setMessage("Copyright (c) 2011 Mathias Roth. \n" +
				"Uses Code by Sam Steele (www.c99.org) licensed under the Apache Public license.");
		dialog.show();*/
	}

}
