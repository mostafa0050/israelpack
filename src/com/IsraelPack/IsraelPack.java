package com.IsraelPack;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class IsraelPack extends Activity {
	public static String TAG = "IsraelPack";
	ProgressDialog patience = null;
	private TextView statusText;
	private Button startButton, exitButton;
	private String statusString = "";
	private String[] fileList = { "DroidSans.ttf", "DroidSans-Bold.ttf",
			"DroidSansFallback.ttf", "DroidSansMono.ttf",
			"DroidSerif-Bold.ttf", "DroidSerif-BoldItalic.ttf",
			"DroidSerif-Italic.ttf", "DroidSerif-Regular.ttf" };

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		startButton = (Button) this.findViewById(R.id.StartButton);
		exitButton = (Button) this.findViewById(R.id.exitButton);
		statusText = (TextView) this.findViewById(R.id.StatusText);

		statusString = "";
		printStatus("Starting");

		startButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				prepare();
				downloadFonts();
				replaceFonts();
				printStatus("Done");
			}
		});
		exitButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
	}

	private void printStatus(String msg) {
		statusString += msg + "\n";
		statusText.setText(statusString);
		statusText.refreshDrawableState();
	}

	private void sendShell(final String[] commands, final String msg) {
		Thread t = new Thread() {
			public void run() {
				printStatus(msg);
				ShellInterface.doExec(commands);
			}
		};
		t.start();
		try {
			t.join();
		} catch (InterruptedException e) {
			printStatus("-E- sendShell error - " + e);
		}
	}

	public boolean DownloadFromUrl(String fileName) {
		try {
			BufferedInputStream in = new BufferedInputStream(new URL(
					"http://israelpack.googlecode.com/files/" + fileName)
					.openStream());
			FileOutputStream fos = new FileOutputStream("/sdcard/IsraelPack/"
					+ fileName);
			BufferedOutputStream bout = new BufferedOutputStream(fos, 1024);
			byte[] data = new byte[1024];
			int x = 0;
			while ((x = in.read(data, 0, 1024)) >= 0) {
				bout.write(data, 0, x);
			}
			bout.close();
			in.close();

		} catch (IOException e) {
			printStatus("-E- Download error - " + e);
			return false;
		}
		return true;
	}

	private void prepare() {
		printStatus("Prepare stage");
		String[] mkDir = { "mkdir /sdcard/IsraelPack/" };
		sendShell(mkDir, "Creating fonts directory");
		String[] mountSystem = { "mount -o remount,rw -t yaffs2 /dev/block/mtdblock3 /system" };
		sendShell(mountSystem, "Mounting System as read-write");
	}

	private boolean downloadFonts() {
		printStatus("Download stage");
		for (String fileItem : fileList) {
			File sdFile = new File("/sdcard/IsraelPack/" + fileItem);
			if (!(sdFile.isFile())) {
				if (!(DownloadFromUrl(fileItem))) {
					return false;
				}
				printStatus("-I- Starting " + fileItem + " download");
			} else {
				printStatus("-I- Font " + fileItem + " already exists");
			}
		}
		return false;
	}

	private void replaceFonts() {
		printStatus("Replace stage");
		for (String fileItem : fileList) {
			String sdFile = "/sdcard/IsraelPack/" + fileItem;
			String[] commands = {
					"cat " + sdFile + " > /system/fonts/e" + fileItem,
					"chmod 644 /system/fonts/" + fileItem };
			printStatus("-I- Replacing " + fileItem);
			sendShell(commands, "Replace " + fileItem);
		}
	}
}