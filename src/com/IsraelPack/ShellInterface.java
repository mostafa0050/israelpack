package com.IsraelPack;

import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ShellInterface extends Thread {
	private static List<String> info, debug, error = new ArrayList<String>();

	public void cleanInfo() {
		info.clear();
	}

	public void cleanDebug() {
		debug.clear();
	}

	public void cleanError() {
		error.clear();
	}

	public List<String> getInfo() {
		return info;
	}

	public List<String> getDebug() {
		return debug;
	}

	public List<String> getError() {
		return error;
	}

	public static boolean doExec(String[] commands) {
		int sleeptime = 200;
		Process process = null;
		DataOutputStream os = null;

		try {
			//debug.add("Starting su");
			process = Runtime.getRuntime().exec("su");
			os = new DataOutputStream(process.getOutputStream());

			for (String single : commands) {
				//debug.add("Executing " + single);
				os.writeBytes(single + "\n");
				os.flush();
				Thread.sleep(sleeptime);
			}
			os.writeBytes("exit\n");
			os.flush();

			process.waitFor();
			return true;

		} catch (Exception e) {
			//error.add("doExec " + e.getMessage());
			e.printStackTrace();
			return false;
		} finally {
			try {
				if (os != null) {
					os.close();
				}
				process.destroy();
			} catch (Exception e) {
				//error.add("doExec " + e.getMessage());
				return false;
			}
		}
	}
}
