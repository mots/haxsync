package org.mots.haxsync.activities;

import org.mots.haxsync.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;

public class WorkaroundConfirmation extends Activity {
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getString(R.string.workaround_warning))
				.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
		                WorkaroundConfirmation.this.finish();						
					}
				})
		       .setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		       			SharedPreferences prefs = WorkaroundConfirmation.this.getSharedPreferences(WorkaroundConfirmation.this.getPackageName() + "_preferences", MODE_MULTI_PROCESS);
						SharedPreferences.Editor editor = prefs.edit();
						editor.putBoolean("workaround_ignore", true);
						editor.commit();
						WorkaroundConfirmation.this.finish();

		           }
		       })
		       .setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                WorkaroundConfirmation.this.finish();
		           }
		       });
		AlertDialog alert = builder.create();
		alert.show();

	}
}
