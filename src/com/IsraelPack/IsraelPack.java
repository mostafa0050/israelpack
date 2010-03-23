package com.IsraelPack;

import java.io.File;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class IsraelPack extends Activity {
	public static String TAG = "IsraelPack";
	ProgressDialog patience = null;
	private TextView statusText;
	private Button startButton;
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
		statusText = (TextView) this.findViewById(R.id.StatusText);

		startButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				statusString = "";
				printStatus("Starting");
				prepare();
				downloadFonts();
				replaceFonts();
				printStatus("Done");
			}
		});
	}

	private void printStatus(String msg) {
		statusString += msg + "\n";
		statusText.setText(statusString);
	}

	private void sendShell(final String[] commands, String msg) {
		Thread t = new Thread() {
			public void run() {
				// perform expensive tasks in a background thread
				ShellInterface.doExec(commands);
			}
		};
		t.start();
	}

	private void prepare() {
		printStatus("Prepare stage");
		String[] mkDir = { "mkdir /sdcard/.fonts/" };
		sendShell(mkDir, "Creating fonts directory");
		String[] mountSystem = {
				"export path=`grep \"/system\" /system/etc/fstab | awk '{print $1}'`",
				"export fstype=`grep \"/system\" /system/etc/fstab | awk '{print $3}'`",
				"mount -o remount,rw -t $fstype $path"
		};
		sendShell(mountSystem, "Mounting System as read-write");
	}

	private void downloadFonts() {
		printStatus("Download stage");
		for (String fileItem : fileList) {
			File sdFile = new File(Environment.getExternalStorageDirectory()
					+ "/.fonts/" + fileItem);
			if (!(sdFile.isFile())) {
				String[] commands = {
						"cd /sdcard/.fonts/",
						"wget http://israelpack.googlecode.com/files/"
								+ fileItem };
				printStatus("-I- Starting " + fileItem + " download");
				sendShell(commands, "Download " + fileItem);
			} else {
				printStatus("-I- Font " + fileItem + " already exists");
			}
		}
	}

	private void replaceFonts() {
		printStatus("Replace stage");
		for (String fileItem : fileList) {
			String sdFile = "/sdcard/.fonts/" + fileItem;
			String[] commands = { "cp " + sdFile + " /system/fonts/"
					+ fileItem, "chmod 644 /system/fonts/" + fileItem };
			printStatus("-I- Replacing " + fileItem);
			sendShell(commands, "Replace " + fileItem);
		}
	}
}