package com.IsraelPack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class IsraelPack extends Activity {

	private Button connectButton, runButton;
	private TextView serverNameText, statusText;
	private ListView packagesView;
	private ProgressBar appPB;
	private static String status = null;
	private boolean allReady = false;
	private int running = 0;
	private String serverFile, serverName, workArea, packagesFile = null;
	List<Map<String, String>> packagesList = new ArrayList<Map<String, String>>();
	final jobsAPI jobs = new jobsAPI();
	public final appLogger appLog = new appLogger();

	public static class Global {
		public final static String TAG = "IsraelPack";

		public final static int MENU_QUIT = 1;
		public final static int MENU_LOG = 2;
		public final static int MENU_CREDITS = 3;

		public final static String STATUS_READY = "Ready";
		public final static String STATUS_NOT_READY = "Not ready";
		public final static String STATUS_READY_TO_CONNECT = "Ready to Connect";
		public final static String STATUS_ERROR = "Error";
		public final static String STATUS_RUNNING = "Running";

		public final static int STATUS_PACKAGE_RUNNING = 1;
		public final static int STATUS_PACKAGE_FINISH = 2;
	}

	public static String creditsString = "some code from:\n"
			+ "http://code.google.com/p/market-enabler/\n"
			+ "http://code.google.com/p/android-metamorph/\n"
			+ "and a bit from\n"
			+ "http://code.google.com/p/cyanogen-updater/\n" + "\n"
			+ "and me (dmanbuhnik) :) for my hard work...";

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, Global.MENU_CREDITS, 0, "Credits");
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
				Dialog logDialog = new Dialog(this);
				logDialog.setContentView(R.layout.logwindow);
				logDialog.setTitle(Global.TAG + " Log");
				TextView logText = (TextView) logDialog
						.findViewById(R.id.logText);
				logText.setText(appLog.getLogDataAllFormatted());
				Button closeDialogButton = (Button) logDialog
						.findViewById(R.id.CloseDialogButton);
				Button sendLogDialogButton = (Button) logDialog
						.findViewById(R.id.SendLogDialogButton);
				closeDialogButton.setOnClickListener(new closeListener(
						logDialog));
				sendLogDialogButton.setOnClickListener(new sendLogListener(
						logDialog));
				logDialog.show();

				return true;
			case Global.MENU_CREDITS :
				Dialog creditsDialog = new Dialog(this);
				creditsDialog.setContentView(R.layout.creditswindow);
				creditsDialog.setTitle("Credits");
				TextView creditsText = (TextView) creditsDialog
						.findViewById(R.id.CreditsText);
				creditsText.setText(creditsString);
				creditsDialog.show();
				return true;
		}
		appLog
				.addLogData(Log.ERROR, "wrong menu pressed - "
						+ item.getItemId());
		return false;
	}
	protected class closeListener implements OnClickListener {
		private Dialog dialog;

		public closeListener(Dialog dialog) {
			this.dialog = dialog;
		}

		public void onClick(View v) {
			dialog.dismiss();
		}
	}

	protected class sendLogListener implements OnClickListener {
		private Dialog dialog;

		public sendLogListener(Dialog dialog) {
			this.dialog = dialog;
		}

		public void onClick(View v) {
			final Intent emailIntent = new Intent(
					android.content.Intent.ACTION_SEND);
			emailIntent.setType("plain/text");
			emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
					new String[]{"dmanbuhnik@gmail.com"});
			emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
					"IsraelPack - application log");
			emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, appLog
					.getLogDataAllFormatted());
			startActivity(Intent.createChooser(emailIntent, "Send mail..."));
			dialog.dismiss();
		}
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
		appLog.addLogData(Log.INFO, "Starting " + Global.TAG);
		serverNameText.setText(serverName);
		runButton.setClickable(false);
		runButton.setEnabled(false);

		allReady = false;
		status = Global.STATUS_NOT_READY;
		if (!(jobs.suAvailable())) {
			appLog.addLogData(Log.ERROR,
					"Missing Root (su)! unable to continue");
			status = Global.STATUS_ERROR;
		} else if (!(jobs.sdcardAvailable())) {
			appLog.addLogData(Log.ERROR, "Missing sdcard! unable to continue");
			status = Global.STATUS_ERROR;
		} else if (!(jobs.makeDir("/sdcard/IsraelPack"))) {
			appLog.addLogData(Log.ERROR,
					"Can't create IsraelPack dir! unable to continue");
			status = Global.STATUS_ERROR;
		} else {
			appLog.addLogData(Log.INFO, "Ready to start");
			allReady = true;
			status = Global.STATUS_READY_TO_CONNECT;
		}
		updateStatus();

		connectButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				appLog.addLogData(Log.INFO, "Connecting to server - "
						+ serverFile);
				if (Utils.DownloadFromUrl(serverFile + "/" + packagesFile,
						workArea + "/" + packagesFile)) {
					appLog.addLogData(Log.INFO, "server file downloaded");
					showPackages(workArea + "/" + packagesFile);
					status = Global.STATUS_READY;
					updateStatus();
					runButton.setClickable(true);
					runButton.setEnabled(true);
				} else {
					appLog.addLogData(Log.ERROR, "server file failed");
					allReady = false;
					status = Global.STATUS_ERROR;
					updateStatus();
				}
			}
		});

		runButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				for (int i = 0; i < packagesView.getChildCount(); i++) {
					CheckBox cbox = (CheckBox) packagesView.getChildAt(i)
							.findViewById(R.id.PackageCheckBox);
					if (cbox.isChecked()) {
						ProgressBar packagePB = (ProgressBar) packagesView
								.getChildAt(i).findViewById(
										R.id.PackageProgressBar);
						TextView packageProgressText = (TextView) packagesView
								.getChildAt(i).findViewById(
										R.id.PackageProgressText);
						packageProgressText.setVisibility(View.VISIBLE);
						packagePB.setVisibility(View.VISIBLE);
						packagePB.setProgress(0);
						appPB.setVisibility(View.VISIBLE);
						status = Global.STATUS_RUNNING;
						running++;
						updateStatus();

						Utils.DownloadFromUrl(packagesList.get(i).get("url"),
								packagesList.get(i).get("file"));
						runJson(packagesList.get(i).get("file"), packagePB,
								packageProgressText);
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
							.addLogData(Log.ERROR,
									"This format is not supported, update the applications maybe?");
					status = Global.STATUS_ERROR;
					updateStatus();
					return;
				}
			} else {
				appLog
						.addLogData(Log.ERROR,
								"Error in server package file (missing 'packageVersion' field");
				status = Global.STATUS_ERROR;
				updateStatus();
				return;
			}

			if (json.has("packages")) {
				JSONArray jsPackages = json.getJSONArray("packages");
				packagesList.clear();
				for (int i = 0; i < jsPackages.length(); i++) {
					JSONObject jsPackage = jsPackages.getJSONObject(i);
					appLog.addLogData(Log.INFO, "Package found - "
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
			} else {
				appLog.addLogData(Log.INFO,
						"No packages in this server packages file");
			}

		} catch (JSONException e) {
			appLog.addLogData(Log.ERROR, e.getMessage());
			e.printStackTrace();
		}
	}

	private boolean runJson(String jsonFile, ProgressBar packagePB,
			TextView packageProgressText) {
		try {
			JSONObject json = new JSONObject(Utils.readFileAsString(jsonFile));
			if (!(json.has("packageVersion"))) {
				appLog.addLogData(Log.ERROR, "Unrecognize package file");
				return false;
			}
			if (!(json.getString("packageVersion").equals("1.0"))) {
				appLog
						.addLogData(Log.ERROR,
								"This format is not supported, update the applications maybe?");
				return false;
			}
			if (!(json.has("name"))) {
				appLog.addLogData(Log.ERROR, "Missing entries in package file");
				return false;
			}
			appLog.addLogData(Log.INFO, "Running package - "
					+ json.getString("name"));
			if (json.has("commands")) {
				JSONArray jsCommands = json.getJSONArray("commands");
				dispatchJson(jsCommands, packagePB, packageProgressText);
			}
		} catch (JSONException e) {
			appLog.addLogData(Log.ERROR, e.getMessage());
			e.printStackTrace();
		}
		return true;
	}

	private void dispatchJson(final JSONArray jsCommands,
			final ProgressBar packagePB, final TextView packageProgressText) {
		packagePB.setMax(jsCommands.length());
		Handler commandHandler = new Handler() {
			public void handleMessage(Message msg) {
				if (msg.arg1 == Global.STATUS_PACKAGE_FINISH) {
					running--;
					packagePB.setVisibility(View.GONE);
					packageProgressText.setVisibility(View.GONE);
					if (running == 0) {
						appPB.setVisibility(View.GONE);
						status = Global.STATUS_READY;
						updateStatus();
					}
					return;
				}
				try {
					packageProgressText.setText(jsCommands.getJSONObject(
							msg.what).getString("msg"));
				} catch (JSONException e) {
					packageProgressText.setText("Error");
					e.printStackTrace();
				}
				packagePB.setProgress(msg.arg2);
			}
		};
		jobThread commandJobThread = new jobThread();
		Thread commandThread = new Thread(commandJobThread);
		commandJobThread.setAppLog(appLog);
		commandJobThread.setmHandler(commandHandler);
		commandJobThread.setJsCommands(jsCommands);
		commandThread.start();
	}

	private void popupSendLog() {
		Context context = getApplicationContext();
		CharSequence text = "Error! use 'MENU -> SHOW LOG' option";
		int duration = Toast.LENGTH_LONG;

		Toast toast = Toast.makeText(context, text, duration);
		toast.show();
	}

	private void updateStatus() {
		if (!(allReady)) {
			connectButton.setClickable(false);
			connectButton.setEnabled(false);
			runButton.setClickable(false);
			runButton.setEnabled(false);
		}
		if (status.equals(Global.STATUS_ERROR)) {
			statusText.setText(Global.STATUS_ERROR);
			statusText.setTextColor(getResources().getColor(R.color.red));
			popupSendLog();
		} else if (status.equals(Global.STATUS_NOT_READY)) {
			statusText.setText(Global.STATUS_NOT_READY);
			statusText.setTextColor(getResources().getColor(R.color.gray));
		} else if (status.equals(Global.STATUS_READY)) {
			statusText.setText(Global.STATUS_READY);
			statusText.setTextColor(getResources().getColor(R.color.green));
		} else if (status.equals(Global.STATUS_READY_TO_CONNECT)) {
			statusText.setText(Global.STATUS_READY_TO_CONNECT);
			statusText.setTextColor(getResources().getColor(R.color.green));
		} else if (status.equals(Global.STATUS_RUNNING)) {
			statusText.setText(Global.STATUS_RUNNING);
			statusText.setTextColor(getResources().getColor(R.color.gray));
		}
	}
}