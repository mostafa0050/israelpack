package com.IsraelPack;

import java.util.ArrayList;

import android.util.Log;

public class appLogger {

	private class Entry {
		int Type;
		String Msg;
	}

	private ArrayList<Entry> log = null;

	private String getPrefix(int type) {
		switch (type) {
			case (Log.ERROR) :
				return "-E- ";
			case (Log.WARN) :
				return "-W- ";
			case (Log.INFO) :
				return "-I- ";
			case (Log.DEBUG) :
				return "-D- ";
			case (Log.VERBOSE) :
				return "-V- ";
			default :
				return null;
		}
	}

	public void create() {
		log = new ArrayList<Entry>();
	}

	public void clearAll() {
		log.clear();
	}

	public String getLogDataTypeFormatted(int type) {
		String output = "";

		for (Entry item : log) {
			if (item.Type == type) {
				output += item.Msg;
			}
		}
		return output;
	}

	public ArrayList<String> getLogDataType(int type) {
		ArrayList<String> output = new ArrayList<String>();

		for (Entry item : log) {
			if (item.Type == type) {
				output.add(item.Msg);
			}
		}
		return output;
	}

	public String getLogDataAllFormatted() {
		String output = "";

		for (Entry item : log) {
			output += item.Msg;
		}
		return output;
	}

	public ArrayList<String> getLogDataAll() {
		ArrayList<String> output = new ArrayList<String>();

		for (Entry item : log) {
			output.add(item.Msg);
		}
		return output;
	}

	public void addLogData(int type, String msg) {
		String prefix = getPrefix(type);
		Entry entry = new Entry();
		entry.Type = type;
		entry.Msg = prefix + msg + "\n";
		log.add(entry);
	}
}