package com.IsraelPack;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class jobThread implements Runnable {
	private final jobsAPI jobs = new jobsAPI();
	private JSONArray jsCommands;
	private Handler mHandler;

	private boolean status = false;

	public void setmHandler(Handler mHandler) {
		this.mHandler = mHandler;
	}

	public void setJsCommands(JSONArray jsCommands) {
		this.jsCommands = jsCommands;
	}

	public boolean isStatus() {
		return status;
	}

	@Override
	public void run() {
		try {
			for (int i = 0; i < jsCommands.length(); i++) {
				Message msg1 = new Message();
				msg1.arg1 = 1;				
				mHandler.sendMessage(msg1);
				JSONObject item = jsCommands.getJSONObject(i);
				String type = item.getString("type");
				if (type.equals("mkdir")) {
					if (jobs.makeDir(item.getString("path"))) {
						status = true;
					} else {
						status = false;
					}
				} else if (type.equals("download")) {
					if (jobs.Download(item.getString("url"), item
							.getString("to"), item.getString("md5"))) {
						status = true;
					} else {
						status = false;
					}
				} else if (type.equals("unzip")) {
					if (jobs
							.unzip(item.getString("from"), item.getString("to"))) {
					} else {
						status = true;
					}
				} else if (type.equals("mount")) {
					if (jobs.mount(item.getString("partition"))) {
						status = true;
					} else {
						status = false;
					}
				} else if (type.equals("replaceFiles")) {
					List<String> l = new ArrayList<String>();
					for (int j = 0; j < item.getJSONArray("list").length(); j++) {
						l.add(item.getJSONArray("list").getString(j));
					}
					String[] list = l.toArray(new String[l.size()]);
					if (jobs.replaceFiles(item.getString("from"), item
							.getString("to"), list)) {
						status = true;
					} else {
						status = false;
					}
				} else if (type.equals("chmodFiles")) {
					List<String> l = new ArrayList<String>();
					for (int j = 0; j < item.getJSONArray("list").length(); j++) {
						l.add(item.getJSONArray("list").getString(j));
					}
					String[] list = l.toArray(new String[l.size()]);
					if (jobs.chmodFiles(item.getString("path"), item
							.getString("permissions"), list)) {
						status = true;
					} else {
						status = false;
					}
				} else {
					status = false;
				}
				if (isStatus()) {
					Log.d("IsraelPack", "job clean - " + item.getString("msg"));
				} else {
					Log.d("IsraelPack", "job dirty - " + item.getString("msg"));
				}
				Message msg2 = new Message();
				msg2.arg1 = 2;				
				mHandler.sendMessage(msg2);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		} finally {
			Log.d("IsraelPack", "Ending thread");
		}
	}
}