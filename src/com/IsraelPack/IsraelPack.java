package com.IsraelPack;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class IsraelPack extends Activity {

	private static TextView statusText;
	private Button startButton, exitButton;
	private static String statusString = "";

	public class Global {
		public final static String TAG = "IsraelPack";
		public final static int INFO = 1;
		public final static int DEBUG = 2;
		public final static int ERROR = 3;
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		startButton = (Button) this.findViewById(R.id.StartButton);
		exitButton = (Button) this.findViewById(R.id.exitButton);
		statusText = (TextView) this.findViewById(R.id.StatusText);

		statusString = "";

		startButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Utils
						.DownloadFromUrl(
								"http://israelpack.googlecode.com/files/hebrewFonts.json",
								"/sdcard/hebrewFonts.json");
				runJson("/sdcard/hebrewFonts.json");

			}
		});
		exitButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
	}

	private boolean runJson(String jsonFile) {
		final jobsAPI jobs = new jobsAPI();
		try {
			JSONObject json = new JSONObject(Utils.readFileAsString(jsonFile));
			if (!(json.getString("format").equals("1.0"))) {
				printStatus(Global.ERROR,
						"This format is not supported, update the applications maybe?");
				return false;
			}
			JSONArray jsCommands = json.getJSONArray("commands");
			for (int i = 0; i < jsCommands.length(); i++) {
				JSONObject item = jsCommands.getJSONObject(i);
				String type = item.getString("type");

				if (type.equals("mkdir")) {
					printStatus(Global.DEBUG, item.getString("msg"));
					if (jobs.makeDir(item.getString("path"))) {
						printStatus(Global.DEBUG, "mkdir done");
					} else {
						printStatus(Global.ERROR, "error in mkdir");
						return false;
					}

				} else if (type.equals("download")) {
					printStatus(Global.DEBUG, item.getString("msg"));
					if (jobs.Download(item.getString("url"), item
							.getString("to"), item.getString("md5"))) {
						printStatus(Global.DEBUG, "download done");
					} else {
						printStatus(Global.ERROR, "error in download");
						return false;
					}

				} else if (type.equals("unzip")) {
					printStatus(Global.DEBUG, item.getString("msg"));
					if (jobs
							.unzip(item.getString("from"), item.getString("to"))) {
						printStatus(Global.DEBUG, "unzip done");
					} else {
						printStatus(Global.ERROR, "error in unzip");
						return false;
					}

				} else if (type.equals("mount")) {
					printStatus(Global.DEBUG, item.getString("msg"));
					if (jobs.mount(item.getString("partition"))) {
						printStatus(Global.DEBUG, "mounting done");
					} else {
						printStatus(Global.ERROR, "error in mounting");
						return false;
					}

				} else if (type.equals("replaceFiles")) {
					printStatus(Global.DEBUG, item.getString("msg"));
					List<String> l = new ArrayList<String>();
					for (int j = 0; j < item.getJSONArray("list").length(); j++) {
						l.add(item.getJSONArray("list").getString(j));
					}
					String[] list = l.toArray(new String[l.size()]);
					if (jobs.replaceFiles(item.getString("from"), item
							.getString("to"), list)) {
						printStatus(Global.DEBUG, "replaceFiles done");
					} else {
						printStatus(Global.ERROR, "error in replaceFiles");
						return false;
					}

				} else if (type.equals("chmodFiles")) {
					printStatus(Global.DEBUG, item.getString("msg"));
					List<String> l = new ArrayList<String>();
					for (int j = 0; j < item.getJSONArray("list").length(); j++) {
						l.add(item.getJSONArray("list").getString(j));
					}
					String[] list = l.toArray(new String[l.size()]);
					if (jobs.chmodFiles(item.getString("path"), item
							.getString("permissions"), list)) {
						printStatus(Global.DEBUG, "chmodFiles done");
					} else {
						printStatus(Global.ERROR, "error in chmodFiles");
						return false;
					}

				} else {
					printStatus(Global.ERROR, "Unknown command! bug?");
					return false;
				}
			}
		} catch (JSONException e) {
			printStatus(Global.ERROR, e.getMessage());
			e.printStackTrace();
		}
		return true;
	}

	private static void printStatus(int type, String msg) {
		switch (type) {
		case Global.INFO:
			Log.i(Global.TAG, msg);
			statusString += "-I-" + msg + "\n";
			break;
		case Global.DEBUG:
			Log.d(Global.TAG, msg);
			statusString += "-D-" + msg + "\n";
			break;
		case Global.ERROR:
			Log.e(Global.TAG, msg);
			statusString += "-E-" + msg + "\n";
			break;
		}
		statusText.setText(statusString);
		statusText.refreshDrawableState();
	}
}