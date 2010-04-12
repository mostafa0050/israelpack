package com.IsraelPack;

import java.util.ArrayList;
import java.util.HashMap;

import android.util.Log;

public class appLogger {
	private HashMap<Integer, ArrayList<String>> log = null;
	private final int INFO = 1;
	private final int DEBUG = 2;
	private final int ERROR = 3;

	public void create() {
		log = new HashMap<Integer, ArrayList<String>>();
		log.put(INFO, new ArrayList<String>());
		log.put(DEBUG, new ArrayList<String>());
		log.put(ERROR, new ArrayList<String>());
		Log.d("IsraelPack", log.keySet().toString());
	}

	public void clearAll() {
		log.clear();
	}

	public void clearInfo() {
		log.get(INFO).clear();
	}

	public void clearDebug() {
		log.get(DEBUG).clear();
	}

	public void clearError() {
		log.get(ERROR).clear();
	}

	public ArrayList<String> getInfo() {
		return log.get(INFO);
	}

	public ArrayList<String> getDebug() {
		return log.get(DEBUG);
	}

	public ArrayList<String> getError() {
		return log.get(ERROR);
	}

	public void addInfo(String msg) {
		Log.d("IsraelPack", log.keySet().toString());
		log.get(INFO).add(msg);
	}

	public void addDebug(String msg) {
		Log.d("IsraelPack", log.keySet().toString());
		log.get(DEBUG).add(msg);
	}

	public void addError(String msg) {
		Log.d("IsraelPack", log.keySet().toString());
		log.get(ERROR).add(msg);
	}

	private String getString(int type) {
		String output = null;
		for (String msg : log.get(type)) {
			output += "-I- " + msg + "\n";
		}
		return output;
	}

	public String getInfoString() {
		return getString(INFO);
	}

	public String getDebugString() {
		return getString(DEBUG);
	}

	public String getErrorString() {
		return getString(ERROR);
	}
}