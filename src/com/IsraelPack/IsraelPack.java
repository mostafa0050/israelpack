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
	final jobsAPI jobs = new jobsAPI();

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

		if (jobs.sdcardAvailable()) {
			jobs.makeDir("/sdcard/IsraelPack");
		} else {
			printStatus(Global.ERROR, "Missing sdcard");
			return;
		}

		startButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Utils
						.DownloadFromUrl(
								"http://israelpack.googlecode.com/files/serverPackages.json",
								"/sdcard/IsraelPack/serverPackages.json");
				showPackages("/sdcard/IsraelPack/serverPackages.json");
			}
		});
		exitButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
	}

	private void showPackages(String jsonFile) {
		try {
			JSONObject json = new JSONObject(Utils.readFileAsString(jsonFile));
			if (json.has("packageVersion")) {
				if (!(json.getString("packageVersion").equals("1.0"))) {
					printStatus(Global.ERROR,
							"This format is not supported, update the applications maybe?");
					return;
				}
			}
			if (json.has("packages")) {
				JSONArray jsPackages = json.getJSONArray("packages");
				for (int i = 0; i < jsPackages.length(); i++) {
					JSONObject jsPackage = jsPackages.getJSONObject(i);
					printStatus(Global.INFO, "Package found - "
							+ jsPackage.getString("name"));
					Utils.DownloadFromUrl(jsPackage.getString("url"), jsPackage
							.getString("file"));
					runJson(jsPackage.getString("file"));
				}
			}

		} catch (JSONException e) {
			printStatus(Global.ERROR, e.getMessage());
			e.printStackTrace();
		}
	}

	private boolean runJson(String jsonFile) {
		try {
			JSONObject json = new JSONObject(Utils.readFileAsString(jsonFile));
			if (json.has("packageVersion")) {
				if (!(json.getString("packageVersion").equals("1.0"))) {
					printStatus(Global.ERROR,
							"This format is not supported, update the applications maybe?");
					return false;
				}
			} else {
				printStatus(Global.ERROR, "Unrecognize package file");
				return false;
			}
			if (json.has("name")) {
				printStatus(Global.INFO, "Running package - "
						+ json.getString("name"));
			} else {
				printStatus(Global.ERROR, "Missing entries in package file");
				return false;
			}
			if (json.has("check")) {
				JSONArray jsCheck = json.getJSONArray("check");
				for (int i = 0; i < jsCheck.length(); i++) {
					String check = jsCheck.getString(i);

					if (check.equals("sdcardMounted")) {
						if (!(jobs.sdcardAvailable())) {
							printStatus(Global.ERROR, "Missing sdcard");
							return false;
						}
					} else if (check.equals("sdFound")) {
						if (!(jobs.suAvailable())) {
							printStatus(Global.ERROR, "Missing su (root)");
							return false;
						}
					}
				}
			}
			if (json.has("commands")) {
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
						if (jobs.unzip(item.getString("from"), item
								.getString("to"))) {
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