package com.IsraelPack;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.IsraelPack.IsraelPack.Global;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class jobThread implements Runnable {
	private final jobsAPI jobs = new jobsAPI();
	private JSONArray jsCommands;
	private Handler mHandler;
	private appLogger appLog;
	private boolean status = false;

	public void setAppLog(appLogger appLog) {
		this.appLog = appLog;
	}

	public void setmHandler(Handler mHandler) {
		this.mHandler = mHandler;
	}

	public void setJsCommands(JSONArray jsCommands) {
		this.jsCommands = jsCommands;
	}

	public boolean isStatus() {
		return status;
	}

	@SuppressWarnings("null")
	@Override
	public synchronized void run() {
		try {
			for (int i = 0; i < jsCommands.length(); i++) {
				appLog.addLogData(Log.INFO, "Starting package commands");
				Message.obtain(mHandler, i, Global.STATUS_PACKAGE_RUNNING, i)
						.sendToTarget();
				JSONObject item = jsCommands.getJSONObject(i);
				String type = item.getString("type");
				appLog.addLogData(Log.INFO, "running " + type + " command");
				if (type.equals("mkdir")) {
					if (jobs.makeDir(item.getString("path"))) {
						appLog.addLogData(Log.INFO, type + " finished cleanly");
						status = true;
						Message.obtain(mHandler, i,
								Global.STATUS_PACKAGE_RUNNING, i)
								.sendToTarget();
					} else {
						appLog.addLogData(Log.INFO, type + " failed");
						status = false;
						Message.obtain(mHandler, i,
								Global.STATUS_PACKAGE_FINISH, i).sendToTarget();
						return;
					}
				} else if (type.equals("download")) {
					if (jobs.Download(item.getString("url"), item
							.getString("to"), item.getString("md5"))) {
						appLog.addLogData(Log.INFO, type + " finished cleanly");
						status = true;
						Message.obtain(mHandler, i,
								Global.STATUS_PACKAGE_RUNNING, i)
								.sendToTarget();
					} else {
						appLog.addLogData(Log.INFO, type + " failed");
						status = false;
						Message.obtain(mHandler, i,
								Global.STATUS_PACKAGE_FINISH, i).sendToTarget();
						return;
					}
				} else if (type.equals("unzip")) {
					if (jobs
							.unzip(item.getString("from"), item.getString("to"))) {
						appLog.addLogData(Log.INFO, type + " finished cleanly");
						status = true;
						Message.obtain(mHandler, i,
								Global.STATUS_PACKAGE_RUNNING, i)
								.sendToTarget();
					} else {
						appLog.addLogData(Log.INFO, type + " failed");
						status = false;
						Message.obtain(mHandler, i,
								Global.STATUS_PACKAGE_FINISH, i).sendToTarget();
						return;
					}
				} else if (type.equals("cmd")) {
					String[] commands = null;
					for (int cmdIndex = 0; cmdIndex < item.getJSONArray("cmd")
							.length(); cmdIndex++) {
						commands[cmdIndex] = item.getJSONArray("cmd")
								.getString(cmdIndex);
					}
					if (jobs.cmd(commands)) {
						appLog.addLogData(Log.INFO, type + " finished cleanly");
						status = true;
						Message.obtain(mHandler, i,
								Global.STATUS_PACKAGE_RUNNING, i)
								.sendToTarget();
					} else {
						appLog.addLogData(Log.INFO, type + " failed");
						status = false;
						Message.obtain(mHandler, i,
								Global.STATUS_PACKAGE_FINISH, i).sendToTarget();
						return;
					}
				} else if (type.equals("mount")) {
					if (jobs.mount(item.getString("partition"))) {
						appLog.addLogData(Log.INFO, type + " finished cleanly");
						status = true;
						Message.obtain(mHandler, i,
								Global.STATUS_PACKAGE_RUNNING, i)
								.sendToTarget();
					} else {
						appLog.addLogData(Log.INFO, type + " failed");
						status = false;
						Message.obtain(mHandler, i,
								Global.STATUS_PACKAGE_FINISH, i).sendToTarget();
						return;
					}
				} else if (type.equals("replaceFiles")) {
					List<String> l = new ArrayList<String>();
					for (int j = 0; j < item.getJSONArray("list").length(); j++) {
						l.add(item.getJSONArray("list").getString(j));
					}
					String[] list = l.toArray(new String[l.size()]);
					if (jobs.replaceFiles(item.getString("from"), item
							.getString("to"), list)) {
						appLog.addLogData(Log.INFO, type + " finished cleanly");
						status = true;
						Message.obtain(mHandler, i,
								Global.STATUS_PACKAGE_RUNNING, i)
								.sendToTarget();
					} else {
						appLog.addLogData(Log.INFO, type + " failed");
						status = false;
						Message.obtain(mHandler, i,
								Global.STATUS_PACKAGE_FINISH, i).sendToTarget();
						return;
					}
				} else if (type.equals("chmodFiles")) {
					List<String> l = new ArrayList<String>();
					for (int j = 0; j < item.getJSONArray("list").length(); j++) {
						l.add(item.getJSONArray("list").getString(j));
					}
					String[] list = l.toArray(new String[l.size()]);
					if (jobs.chmodFiles(item.getString("path"), item
							.getString("permissions"), list)) {
						appLog.addLogData(Log.INFO, type + " finished cleanly");
						status = true;
						Message.obtain(mHandler, i,
								Global.STATUS_PACKAGE_RUNNING, i)
								.sendToTarget();
					} else {
						appLog.addLogData(Log.INFO, type + " failed");
						status = false;
						Message.obtain(mHandler, i,
								Global.STATUS_PACKAGE_FINISH, i).sendToTarget();
						return;
					}
				} else {
					appLog.addLogData(Log.INFO, "Unknown command - " + type);
					status = false;
					Message
							.obtain(mHandler, i, Global.STATUS_PACKAGE_FINISH,
									i).sendToTarget();
					return;
				}
			}
			appLog.addLogData(Log.INFO, "All Commands finished");
		} catch (JSONException e) {
			appLog.addLogData(Log.ERROR, e.getMessage());
			e.printStackTrace();
		}
		Message.obtain(mHandler, 0, Global.STATUS_PACKAGE_FINISH, 0)
				.sendToTarget();
	}
}