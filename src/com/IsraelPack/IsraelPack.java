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
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
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
	private int running = 0;
	private String serverFile, serverName, workArea, packagesFile,
			emailAddress = null;
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
		public final static String STATUS_ERROR_CONNECT = "Error with the server";
		public final static String STATUS_FATAL = "Error, cannot continue";
		public final static String STATUS_RUNNING = "Running";

		public final static int STATUS_PACKAGE_RUNNING = 1;
		public final static int STATUS_PACKAGE_FINISH = 2;
	}

	final Runnable runUpdateStatus = new Runnable() {
		public void run() {
			updateStatus();
		}
	};

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
				creditsText.setText(getString(R.string.cerdits));
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
			String version = "";
			try {
				version = "(ver. "
						+ getPackageManager().getPackageInfo(getPackageName(),
								0).versionName + ") ";
			} catch (NameNotFoundException e) {
				appLog.addLogData(Log.ERROR, "Error in application version");
				e.printStackTrace();
			}
			final Intent emailIntent = new Intent(
					android.content.Intent.ACTION_SEND);
			emailIntent.setType("plain/text");
			emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
					new String[]{emailAddress});
			emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
					"IsraelPack " + version + "- application log");
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
		emailAddress = this.getString(R.string.emailAddress);

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

		initApplication();

		connectButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				connectButtonClicked();
			}
		});

		runButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				runButtonClicked();
			}
		});
	}

	private void connectButtonClicked() {
		status = Global.STATUS_RUNNING;
		updateStatus();
		final Handler connectHandler = new Handler();
		final Runnable runShowPackages = new Runnable() {
			public void run() {
				showPackages();
			}
		};
		Thread connectThread = new Thread() {
			public void run() {
				appLog.addLogData(Log.INFO, "Connecting to server - "
						+ serverFile);
				if (Utils.DownloadFromUrl(serverFile + "/" + packagesFile,
						workArea + "/" + packagesFile)) {
					appLog.addLogData(Log.INFO, "server file downloaded");
					if (!(getPackages(workArea + "/" + packagesFile))) {
						status = Global.STATUS_ERROR_CONNECT;
					} else {
						status = Global.STATUS_READY;
						connectHandler.post(runShowPackages);
					}
				} else {
					appLog.addLogData(Log.ERROR,
							"downloading server packages file failed");
					status = Global.STATUS_ERROR_CONNECT;
				}
				connectHandler.post(runUpdateStatus);
			}
		};
		connectThread.start();
	}

	private void runButtonClicked() {
		status = Global.STATUS_RUNNING;
		updateStatus();
		for (int i = 0; i < packagesView.getChildCount(); i++) {
			CheckBox cbox = (CheckBox) packagesView.getChildAt(i).findViewById(
					R.id.PackageCheckBox);
			if (cbox.isChecked()) {
				try {
					running++;
					final ProgressBar packagePB = (ProgressBar) packagesView
							.getChildAt(i)
							.findViewById(R.id.PackageProgressBar);
					final TextView packageProgressText = (TextView) packagesView
							.getChildAt(i).findViewById(
									R.id.PackageProgressText);
					final String jsonFile = packagesList.get(i).get("file");
					final String jsonUrl = packagesList.get(i).get("url");
					if (!(Utils.DownloadFromUrl(jsonUrl, jsonFile))) {
						packagePB.setVisibility(View.GONE);
						packageProgressText.setText("Error!");
						packageProgressText.setTextColor(getResources()
								.getColor(R.color.red));
						running--;
						if (running == 0) {
							status = Global.STATUS_READY;
							updateStatus();
						}
					}
					final JSONObject json = new JSONObject(Utils
							.readFileAsString(jsonFile));
					final JSONArray jsCommands = json.getJSONArray("commands");
					packageProgressText.setVisibility(View.VISIBLE);
					packagePB.setVisibility(View.VISIBLE);
					packagePB.setMax(jsCommands.length());
					packagePB.setProgress(0);
					final Handler packageRunHandler = new Handler();
					final Runnable packageRunProgress = new Runnable() {
						public void run() {
							try {
								int progress = packagePB.getProgress();
								packageProgressText.setText(jsCommands
										.getJSONObject(progress).getString(
												"msg"));
								packagePB.setProgress(progress + 1);
							} catch (JSONException e) {
								appLog.addLogData(Log.ERROR,
										"Error in packageRunProgress!"
												+ e.getMessage());
								e.printStackTrace();
							}
						}
					};
					final Runnable packageRunFinish = new Runnable() {
						public void run() {
							packagePB.setVisibility(View.GONE);
							packageProgressText.setVisibility(View.GONE);
							running--;
							if (running == 0) {
								status = Global.STATUS_READY;
								updateStatus();
							}
						}
					};
					final Runnable packageRunError = new Runnable() {
						public void run() {
							packagePB.setVisibility(View.GONE);
							packageProgressText.setText("Error!");
							packageProgressText.setTextColor(getResources()
									.getColor(R.color.red));
							running--;
							if (running == 0) {
								status = Global.STATUS_READY;
								updateStatus();
							}
						}
					};
					Thread packageThread = new Thread() {
						@SuppressWarnings("null")
						public void run() {
							try {
								if (!(json.has("packageVersion"))) {
									appLog.addLogData(Log.ERROR,
											"Unrecognize package file");
									return;
								}
								if (!(json.getString("packageVersion")
										.equals("1.0"))) {
									appLog
											.addLogData(Log.ERROR,
													"This format is not supported, update the applications maybe?");
									return;
								}
								if (!(json.has("name"))) {
									appLog.addLogData(Log.ERROR,
											"Missing entries in package file");
									return;
								}
								appLog.addLogData(Log.INFO,
										"Running package - "
												+ json.getString("name"));
								if (json.has("commands")) {
									JSONArray jsCommands = json
											.getJSONArray("commands");
									for (int i = 0; i < jsCommands.length(); i++) {
										appLog.addLogData(Log.INFO,
												"Starting package commands");

										JSONObject item = jsCommands
												.getJSONObject(i);
										String type = item.getString("type");
										appLog.addLogData(Log.INFO, "running "
												+ type + " command");
										if (type.equals("mkdir")) {
											packageRunHandler
													.post(packageRunProgress);
											if (jobs.makeDir(item
													.getString("path"))) {
												appLog
														.addLogData(
																Log.INFO,
																type
																		+ " finished cleanly");
											} else {
												appLog.addLogData(Log.INFO,
														type + " failed");
												packageRunHandler
														.post(packageRunError);
												return;
											}
										} else if (type.equals("download")) {
											packageRunHandler
													.post(packageRunProgress);
											if (jobs.Download(item
													.getString("url"), item
													.getString("to"), item
													.getString("md5"))) {
												appLog
														.addLogData(
																Log.INFO,
																type
																		+ " finished cleanly");
											} else {
												appLog.addLogData(Log.INFO,
														type + " failed");
												packageRunHandler
														.post(packageRunError);
												return;
											}
										} else if (type.equals("unzip")) {
											packageRunHandler
													.post(packageRunProgress);
											if (jobs.unzip(item
													.getString("from"), item
													.getString("to"))) {
												appLog
														.addLogData(
																Log.INFO,
																type
																		+ " finished cleanly");
											} else {
												appLog.addLogData(Log.INFO,
														type + " failed");
												packageRunHandler
														.post(packageRunError);
												return;
											}
										} else if (type.equals("cmd")) {
											packageRunHandler
													.post(packageRunProgress);
											String[] commands = null;
											for (int cmdIndex = 0; cmdIndex < item
													.getJSONArray("cmd")
													.length(); cmdIndex++) {
												commands[cmdIndex] = item
														.getJSONArray("cmd")
														.getString(cmdIndex);
											}
											if (jobs.cmd(commands)) {
												appLog
														.addLogData(
																Log.INFO,
																type
																		+ " finished cleanly");
											} else {
												appLog.addLogData(Log.INFO,
														type + " failed");
												packageRunHandler
														.post(packageRunError);
												return;
											}
										} else if (type.equals("mount")) {
											packageRunHandler
													.post(packageRunProgress);
											if (jobs.mount(item
													.getString("partition"))) {
												appLog
														.addLogData(
																Log.INFO,
																type
																		+ " finished cleanly");
											} else {
												appLog.addLogData(Log.INFO,
														type + " failed");
												packageRunHandler
														.post(packageRunError);
												return;
											}
										} else if (type.equals("replaceFiles")) {
											packageRunHandler
													.post(packageRunProgress);
											List<String> l = new ArrayList<String>();
											for (int j = 0; j < item
													.getJSONArray("list")
													.length(); j++) {
												l.add(item.getJSONArray("list")
														.getString(j));
											}
											String[] list = l
													.toArray(new String[l
															.size()]);
											if (jobs.replaceFiles(item
													.getString("from"), item
													.getString("to"), list)) {
												appLog
														.addLogData(
																Log.INFO,
																type
																		+ " finished cleanly");
											} else {
												appLog.addLogData(Log.INFO,
														type + " failed");
												packageRunHandler
														.post(packageRunError);
												return;
											}
										} else if (type.equals("chmodFiles")) {
											packageRunHandler
													.post(packageRunProgress);
											List<String> l = new ArrayList<String>();
											for (int j = 0; j < item
													.getJSONArray("list")
													.length(); j++) {
												l.add(item.getJSONArray("list")
														.getString(j));
											}
											String[] list = l
													.toArray(new String[l
															.size()]);
											if (jobs.chmodFiles(item
													.getString("path"), item
													.getString("permissions"),
													list)) {
												appLog
														.addLogData(
																Log.INFO,
																type
																		+ " finished cleanly");
											} else {
												appLog.addLogData(Log.INFO,
														type + " failed");
												packageRunHandler
														.post(packageRunError);
												return;
											}
										} else {
											appLog
													.addLogData(Log.INFO,
															"Unknown command - "
																	+ type);
											packageRunHandler
													.post(packageRunError);
											return;
										}
									}
								}
								packageRunHandler.post(packageRunFinish);
							} catch (JSONException e) {
								appLog.addLogData(Log.ERROR, e.getMessage());
								e.printStackTrace();
							}
						}
					};
					packageThread.start();
				} catch (JSONException e) {
					appLog.addLogData(Log.ERROR, e.getMessage());
					e.printStackTrace();
				}
			}
		}
	}

	private void initApplication() {
		status = Global.STATUS_RUNNING;
		updateStatus();
		final Handler initHandler = new Handler();
		Thread initThread = new Thread() {
			public void run() {
				if (!(jobs.suAvailable())) {
					appLog.addLogData(Log.ERROR,
							"Missing Root (su)! unable to continue");
					status = Global.STATUS_FATAL;
				} else if (!(jobs.sdcardAvailable())) {
					appLog.addLogData(Log.ERROR,
							"Missing sdcard! unable to continue");
					status = Global.STATUS_FATAL;
				} else if (!(jobs.makeDir("/sdcard/IsraelPack"))) {
					appLog.addLogData(Log.ERROR,
							"Can't create IsraelPack dir! unable to continue");
					status = Global.STATUS_FATAL;
				} else {
					appLog.addLogData(Log.INFO, "Ready to start");
					status = Global.STATUS_READY_TO_CONNECT;
				}
				initHandler.post(runUpdateStatus);
			}
		};
		initThread.start();

	}

	private boolean getPackages(String jsonFile) {
		try {
			packagesList.clear();
			JSONObject json = new JSONObject(Utils.readFileAsString(jsonFile));
			if (json.has("packageVersion")) {
				if (!(json.getString("packageVersion").equals("1.0"))) {
					appLog
							.addLogData(Log.ERROR,
									"This format is not supported, update the applications maybe?");
					return false;
				}
			} else {
				appLog
						.addLogData(Log.ERROR,
								"Error in server package file (missing 'packageVersion' field");
				return false;
			}

			if (json.has("packages")) {
				JSONArray jsPackages = json.getJSONArray("packages");
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
			} else {
				appLog.addLogData(Log.INFO,
						"No packages in this server packages file");
			}
			return true;
		} catch (JSONException e) {
			appLog.addLogData(Log.ERROR, e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	private void showPackages() {
		String[] from = {"name", "description", "info"};
		int[] to = {R.id.NameText, R.id.DescText, R.id.InfoText};
		SimpleAdapter sAdapter = new SimpleAdapter(this, packagesList,
				R.layout.mainguilistrow, from, to);
		packagesView.setAdapter(sAdapter);
	}

	private void popupMsg(String text) {
		Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
	}

	private void updateStatus() {
		statusText.setText(status);
		if (status.equals(Global.STATUS_FATAL)) {
			appPB.setVisibility(View.VISIBLE);
			connectButton.setClickable(false);
			connectButton.setEnabled(false);
			runButton.setClickable(false);
			runButton.setEnabled(false);
			statusText.setTextColor(getResources().getColor(R.color.red));
			popupMsg("Error! use 'MENU -> SHOW LOG' option");
		} else if (status.equals(Global.STATUS_ERROR)) {
			appPB.setVisibility(View.VISIBLE);
			connectButton.setClickable(false);
			connectButton.setEnabled(false);
			runButton.setClickable(false);
			runButton.setEnabled(false);
			statusText.setTextColor(getResources().getColor(R.color.red));
			popupMsg("Error! use 'MENU -> SHOW LOG' option");
		} else if (status.equals(Global.STATUS_NOT_READY)) {
			appPB.setVisibility(View.VISIBLE);
			connectButton.setClickable(false);
			connectButton.setEnabled(false);
			runButton.setClickable(false);
			runButton.setEnabled(false);
			statusText.setTextColor(getResources().getColor(R.color.gray));
		} else if (status.equals(Global.STATUS_RUNNING)) {
			appPB.setVisibility(View.VISIBLE);
			connectButton.setClickable(false);
			connectButton.setEnabled(false);
			runButton.setClickable(false);
			runButton.setEnabled(false);
			statusText.setTextColor(getResources().getColor(R.color.gray));
		} else if (status.equals(Global.STATUS_READY_TO_CONNECT)) {
			appPB.setVisibility(View.GONE);
			connectButton.setClickable(true);
			connectButton.setEnabled(true);
			runButton.setClickable(false);
			runButton.setEnabled(false);
			statusText.setTextColor(getResources().getColor(R.color.green));
		} else if (status.equals(Global.STATUS_READY)) {
			appPB.setVisibility(View.GONE);
			connectButton.setClickable(true);
			connectButton.setEnabled(true);
			runButton.setClickable(true);
			runButton.setEnabled(true);
			statusText.setTextColor(getResources().getColor(R.color.green));
		} else {
			appPB.setVisibility(View.GONE);
			connectButton.setClickable(false);
			connectButton.setEnabled(false);
			runButton.setClickable(false);
			runButton.setEnabled(false);
			statusText.setText(Global.STATUS_FATAL);
			statusText.setTextColor(getResources().getColor(R.color.red));
			appLog.addLogData(Log.ERROR, "wrong status - " + status);
			popupMsg("Error! Application bug. use 'MENU -> SHOW LOG' option");
		}
	}
}