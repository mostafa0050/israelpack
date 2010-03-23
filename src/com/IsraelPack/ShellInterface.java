package com.IsraelPack;

import java.io.DataOutputStream;

import android.util.Log;

public class ShellInterface extends Thread {

	public static void doExec(String[] commands) {
		int sleeptime = 200;
		Process process = null;
		DataOutputStream os = null;

		try {
			Log.i(IsraelPack.TAG, "Starting exec of su");
			process = Runtime.getRuntime().exec("su");

			os = new DataOutputStream(process.getOutputStream());

			// Doing Stuff ;)
			Log.i(IsraelPack.TAG, "Starting command loop");
			for (String single : commands) {
				Log.i(IsraelPack.TAG, "Executing [" + single + "]");
				os.writeBytes(single + "\n");
				os.flush();
				Thread.sleep(sleeptime);
			}

			os.writeBytes("exit\n");
			os.flush();

			process.waitFor();

		} catch (Exception e) {
			Log.d(IsraelPack.TAG, "Unexpected error - Here is what I know: "
					+ e.getMessage());
			e.printStackTrace();
		} finally {
			try {
				if (os != null) {
					os.close();
				}
				process.destroy();
			} catch (Exception e) {
				// nothing
			}
		}
	}
}
