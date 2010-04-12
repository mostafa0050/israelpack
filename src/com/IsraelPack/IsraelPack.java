package com.IsraelPack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class IsraelPack extends Activity {

	private Button connectButton, runButton;
	private TextView serverNameText, statusText;
	private ListView packagesView;
	private static String statusString = "";
	private boolean allReady = false;
	private String serverFile, serverName, workArea, packagesFile = null;
	List<Map<String, String>> packagesList = new ArrayList<Map<String, String>>();
	final jobsAPI jobs = new jobsAPI();
	public final appLogger appLog = new appLogger();

	public static class Global {
		public final static String TAG = "IsraelPack";
		public final static int INFO = 1;
		public final static int DEBUG = 2;
		public final static int ERROR = 3;

		public final static int MENU_QUIT = 1;
		public final static int MENU_LOG = 2;
		
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, Global.MENU_LOG, 0, "Show Log");
		menu.add(0, Global.MENU_QUIT, 0, "Quit");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case Global.MENU_QUIT :
				this.finish();
			case Global.MENU_LOG :
				return true;
		}
		return false;
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.maingui);

		serverFile = this.getString(R.string.deafultServer);
		serverName = this.getString(R.string.deafultServerName);
		workArea = this.getString(R.string.deafultWorkArea);
		packagesFile = this.getString(R.string.packagesFile);

		connectButton = (Button) this.findViewById(R.id.ConnectButton);
		runButton = (Button) this.findViewById(R.id.RunButton);
		packagesView = (ListView) this.findViewById(R.id.PackagesView);
		serverNameText = (TextView) this.findViewById(R.id.ServerNameText);
		statusText = (TextView) this.findViewById(R.id.StatusText);

		appLog.create();
		appLog.clearAll();
		serverNameText.setText(serverName);

		allReady = false;
		if (!(jobs.suAvailable())) {
			appLog.addError("Missing Root (su)! unable to continue");
		} else if (!(jobs.sdcardAvailable())) {
			appLog.addError("Missing sdcard! unable to continue");
		} else if (!(jobs.makeDir("/sdcard/IsraelPack"))) {
			appLog
					.addError("Can't create IsraelPack dir! unable to continue");
		} else {
			appLog.addDebug("Ready to start");
			allReady = true;
		}
		updateStatus();

		if (!(allReady)) {
			connectButton.setClickable(false);
			connectButton.setEnabled(false);
			runButton.setClickable(false);
			runButton.setEnabled(false);
		}

		connectButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (Utils.DownloadFromUrl(serverFile + "/" + packagesFile,
						workArea + "/" + packagesFile)) {
					showPackages(workArea + "/" + packagesFile);
				}
			}
		});

		runButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				for (int i = 0; i < packagesView.getChildCount(); i++) {
					CheckBox cbox = (CheckBox) packagesView.getChildAt(i)
							.findViewById(R.id.PackageCheckBox);
					if (cbox.isChecked()) {
						cbox.setChecked(false);
					} else {
						cbox.setChecked(true);
					}
				}
			}
		});
	}

	private void showPackages(String jsonFile) {
		try {
			JSONObject json = new JSONObject(Utils.readFileAsString(jsonFile));
			if (json.has("packageVersion")) {
				if (!(json.getString("packageVersion").equals("1.0"))) {
					appLog
							.addError("This format is not supported, update the applications maybe?");
					return;
				}
			}

			if (json.has("packages")) {
				JSONArray jsPackages = json.getJSONArray("packages");
				packagesList.clear();
				for (int i = 0; i < jsPackages.length(); i++) {
					JSONObject jsPackage = jsPackages.getJSONObject(i);
					appLog.addInfo("Package found - "
							+ jsPackage.getString("name"));
					Map<String, String> map = new HashMap<String, String>();
					map.put("name", jsPackage.getString("name"));
					map.put("description", jsPackage.getString("desc"));
					map.put("info", "Version: "
							+ jsPackage.getString("version") + ", Size: "
							+ jsPackage.getString("size"));
					map.put("url", jsPackage.getString("url"));
					packagesList.add(map);
				}
				String[] from = {"name", "description", "info"};
				int[] to = {R.id.NameText, R.id.DescText, R.id.InfoText};
				SimpleAdapter sAdapter = new SimpleAdapter(this, packagesList,
						R.layout.mainguilistrow, from, to);
				packagesView.setAdapter(sAdapter);
			}

		} catch (JSONException e) {
			appLog.addError(e.getMessage());
			e.printStackTrace();
		}
	}

	private boolean runJson(String jsonFile) {
		try {
			JSONObject json = new JSONObject(Utils.readFileAsString(jsonFile));
			if (!(json.has("packageVersion"))) {
				appLog.addError("Unrecognize package file");
				return false;
			}
			if (!(json.getString("packageVersion").equals("1.0"))) {
				appLog
						.addError("This format is not supported, update the applications maybe?");
				return false;
			}
			if (!(json.has("name"))) {
				appLog.addError("Missing entries in package file");
				return false;
			}
			appLog.addInfo("Running package - " + json.getString("name"));
			if (json.has("commands")) {
				JSONArray jsCommands = json.getJSONArray("commands");
				for (int i = 0; i < jsCommands.length(); i++) {
					JSONObject item = jsCommands.getJSONObject(i);
					String type = item.getString("type");

					if (type.equals("mkdir")) {
						appLog.addDebug(item.getString("msg"));
						if (jobs.makeDir(item.getString("path"))) {
							appLog.addDebug("mkdir done");
						} else {
							appLog.addError("error in mkdir");
							return false;
						}

					} else if (type.equals("download")) {
						appLog.addDebug(item.getString("msg"));
						if (jobs.Download(item.getString("url"), item
								.getString("to"), item.getString("md5"))) {
							appLog.addDebug("download done");
						} else {
							appLog.addError("error in download");
							return false;
						}

					} else if (type.equals("unzip")) {
						appLog.addDebug(item.getString("msg"));
						if (jobs.unzip(item.getString("from"), item
								.getString("to"))) {
							appLog.addDebug("unzip done");
						} else {
							appLog.addError("error in unzip");
							return false;
						}

					} else if (type.equals("mount")) {
						appLog.addDebug(item.getString("msg"));
						if (jobs.mount(item.getString("partition"))) {
							appLog.addDebug("mounting done");
						} else {
							appLog.addError("error in mounting");
							return false;
						}

					} else if (type.equals("replaceFiles")) {
						appLog.addDebug(item.getString("msg"));
						List<String> l = new ArrayList<String>();
						for (int j = 0; j < item.getJSONArray("list").length(); j++) {
							l.add(item.getJSONArray("list").getString(j));
						}
						String[] list = l.toArray(new String[l.size()]);
						if (jobs.replaceFiles(item.getString("from"), item
								.getString("to"), list)) {
							appLog.addDebug("replaceFiles done");
						} else {
							appLog.addError("error in replaceFiles");
							return false;
						}

					} else if (type.equals("chmodFiles")) {
						appLog.addDebug(item.getString("msg"));
						List<String> l = new ArrayList<String>();
						for (int j = 0; j < item.getJSONArray("list").length(); j++) {
							l.add(item.getJSONArray("list").getString(j));
						}
						String[] list = l.toArray(new String[l.size()]);
						if (jobs.chmodFiles(item.getString("path"), item
								.getString("permissions"), list)) {
							appLog.addDebug("chmodFiles done");
						} else {
							appLog.addError("error in chmodFiles");
							return false;
						}

					} else {
						appLog.addError("Unknown command! bug?");
						return false;
					}
				}
			}
		} catch (JSONException e) {
			appLog.addError(e.getMessage());
			e.printStackTrace();
		}
		return true;
	}

	private void updateStatus() {
		if (allReady) {
			statusText.setText("Ready");
			statusText.setBackgroundColor(R.color.green);
			statusText.setTextColor(R.color.white);
		} else {
			statusText.setText("Error");
			statusText.setBackgroundColor(R.color.gray);
			statusText.setTextColor(R.color.white);
		}
	}
}