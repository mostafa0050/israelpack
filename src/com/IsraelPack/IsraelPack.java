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
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class IsraelPack extends Activity {

	private Button connectButton, runButton;
	private TextView serverNameText, statusText;
	private ListView packagesView;
	private ProgressBar appPB;
	private static String status = null;
	private boolean allReady = false;
	private int currentWindow = 1;
	private String serverFile, serverName, workArea, packagesFile = null;
	List<Map<String, String>> packagesList = new ArrayList<Map<String, String>>();
	final jobsAPI jobs = new jobsAPI();
	public final appLogger appLog = new appLogger();
	public jobThread jt = new jobThread();
	public Thread wt = new Thread(jt);

	public final Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.arg1) {
				case 1 :
					Log.d("IsraelPack", "from thread! case 1");
					appPB.setVisibility(View.VISIBLE);
					statusText.setText("Running");
					break;
				case 2:
					Log.d("IsraelPack", "from thread! case 2");
					appPB.setVisibility(View.GONE);
					statusText.setText("Done");
					break;
			}
		}
	};

	public static class Global {
		public final static String TAG = "IsraelPack";
		public final static int INFO = 1;
		public final static int DEBUG = 2;
		public final static int ERROR = 3;

		public final static int MENU_QUIT = 1;
		public final static int MENU_LOG = 2;

		public final static String STATUS_READY = "Ready";
		public final static String STATUS_NOT_READY = "Not ready";
		public final static String STATUS_READY_TO_CONNECT = "Ready to Connect";
		public final static String STATUS_ERROR = "Error";
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
				TextView logText;
				this.setContentView(R.layout.logwindow);
				currentWindow = 2;
				logText = (TextView) this.findViewById(R.id.logText);
				logText.setText(appLog.getDebugString());
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

		appPB = (ProgressBar) this.findViewById(R.id.AppProgressBar);

		appLog.create();
		appLog.clearAll();
		serverNameText.setText(serverName);
		runButton.setClickable(false);
		runButton.setEnabled(false);

		allReady = false;
		status = Global.STATUS_NOT_READY;
		if (!(jobs.suAvailable())) {
			appLog.addError("Missing Root (su)! unable to continue");
			status = Global.STATUS_ERROR;
		} else if (!(jobs.sdcardAvailable())) {
			appLog.addError("Missing sdcard! unable to continue");
			status = Global.STATUS_ERROR;
		} else if (!(jobs.makeDir("/sdcard/IsraelPack"))) {
			appLog.addError("Can't create IsraelPack dir! unable to continue");
			status = Global.STATUS_ERROR;
		} else {
			appLog.addDebug("Ready to start");
			allReady = true;
			status = Global.STATUS_READY_TO_CONNECT;
		}
		updateStatus();

		if (!(allReady)) {
			connectButton.setClickable(false);
			connectButton.setEnabled(false);
			runButton.setClickable(false);
			runButton.setEnabled(false);
		}

		jt.setmHandler(mHandler);

		connectButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (Utils.DownloadFromUrl(serverFile + "/" + packagesFile,
						workArea + "/" + packagesFile)) {
					showPackages(workArea + "/" + packagesFile);
					status = Global.STATUS_READY;
					updateStatus();
					runButton.setClickable(true);
					runButton.setEnabled(true);
				}
			}
		});

		runButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				for (int i = 0; i < packagesView.getChildCount(); i++) {
					CheckBox cbox = (CheckBox) packagesView.getChildAt(i)
							.findViewById(R.id.PackageCheckBox);
					if (cbox.isChecked()) {
						Utils.DownloadFromUrl(packagesList.get(i).get("url"),
								packagesList.get(i).get("file"));
						runJson(packagesList.get(i).get("file"));
					} else {

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
					map.put("file", jsPackage.getString("file"));
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
				jt.setJsCommands(jsCommands);
				wt.start();
			}
		} catch (JSONException e) {
			appLog.addError(e.getMessage());
			e.printStackTrace();
		}
		return true;
	}

	private void updateStatus() {
		if (status.equals(Global.STATUS_ERROR)) {
			statusText.setText(Global.STATUS_ERROR);
			statusText.setTextColor(getResources().getColor(R.color.red));
		} else if (status.equals(Global.STATUS_NOT_READY)) {
			statusText.setText(Global.STATUS_NOT_READY);
			statusText.setTextColor(getResources().getColor(R.color.gray));
		} else if (status.equals(Global.STATUS_READY)) {
			statusText.setText(Global.STATUS_READY);
			statusText.setTextColor(getResources().getColor(R.color.green));
		} else if (status.equals(Global.STATUS_READY_TO_CONNECT)) {
			statusText.setText(Global.STATUS_READY_TO_CONNECT);
			statusText.setTextColor(getResources().getColor(R.color.green));
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
	        if (currentWindow == 2) {
	        	this.setContentView(R.layout.maingui);
	        	currentWindow = 1;
	        	return true;
	        }
	    }
	    return super.onKeyDown(keyCode, event);
	}


	@Override
	public void onDestroy() {
		wt.stop();
		super.onDestroy();
	}
}