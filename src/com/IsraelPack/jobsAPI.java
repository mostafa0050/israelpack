package com.IsraelPack;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class jobsAPI {
	private static List<String> info, debug, error = new ArrayList<String>();
	private boolean status = false;

	public void cleanAll() {
		cleanInfo();
		cleanDebug();
		cleanError();
	}

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

	public boolean Download(String url, String fileName, String md5) {
		File f = new File(fileName);
		if (f.exists()) {
			if (Utils.checkMD5(md5, fileName)) {
				return true;
			}
		}
		if (Utils.DownloadFromUrl(url, fileName)) {
			if (Utils.checkMD5(md5, fileName)) {
				return true;
			}
		}
		return false;
	}

	public boolean unzip(String fileName, String to) {
		try {
			byte[] buf = new byte[1024];
			ZipInputStream zipinputstream = null;
			ZipEntry zipentry;
			zipinputstream = new ZipInputStream(new FileInputStream(fileName));

			zipentry = zipinputstream.getNextEntry();
			while (zipentry != null) {
				// for each entry to be extracted
				String entryName = zipentry.getName();

				File newFile = new File(entryName);
				String directory = newFile.getParent();

				if (directory == null) {
					if (newFile.isDirectory())
						break;
				}

				FileOutputStream fileoutputstream = new FileOutputStream(to
						+ "/" + entryName);
				int n;
				while ((n = zipinputstream.read(buf, 0, 1024)) > -1)
					fileoutputstream.write(buf, 0, n);

				fileoutputstream.close();
				zipinputstream.closeEntry();
				zipentry = zipinputstream.getNextEntry();

			}// while

			zipinputstream.close();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean makeDir(String dirPath) {
		String[] commands = { "mkdir " + dirPath };
		return sendShell(commands, "Creating directory " + dirPath);
	}

	public boolean mount(String partitionName) {
		if (partitionName.equals("system")) {
			// info.add("Mounting " + partitionName);
			String[] commands = { "mount -o remount,rw -t yaffs2 /dev/block/mtdblock3 /system" };
			return sendShell(commands, "Mounting " + partitionName);
		}
		// error.add("Wrong partition " + partitionName);
		return false;
	}

	public boolean replaceFiles(String fromPath, String toPath, String[] list) {
		List<String> cmd = new ArrayList<String>();
		for (int i = 0; i < list.length; i++) {
			cmd.add("cat " + fromPath + "/" + list[i] + " > " + toPath + "/"
					+ list[i] + "_test");
		}
		String[] commands = cmd.toArray(new String[cmd.size()]);
		return sendShell(commands, "Replacing files");
	}

	public boolean chmodFiles(String path, String permission, String[] list) {
		List<String> cmd = new ArrayList<String>();
		for (int i = 0; i < list.length; i++) {
			cmd.add("chmod " + permission + " " + path + "/" + list[i]
					+ "_test");
		}
		String[] commands = cmd.toArray(new String[cmd.size()]);
		return sendShell(commands, "Replacing files");
	}

	public boolean replaceFile(String newFile, String oldFile) {
		String[] commands = { "cat " + newFile + " > " + oldFile };
		return sendShell(commands, "Replacing file" + oldFile);
	}

	public boolean chmodFile(String newFile, String permissions) {
		String[] commands = { "chmod " + permissions + " " + newFile };
		return sendShell(commands, "Chmod " + permissions + " file" + newFile);
	}

	public boolean sdcardAvailable() {
		return android.os.Environment.getExternalStorageState().equals(
				android.os.Environment.MEDIA_MOUNTED);
	}

	public boolean suAvailable() {
		String[] pathToCheck = { "/system/bin/", "/system/xbin/",
				"/data/local/xbin/", "/data/local/bin/", "/system/sd/xbin/" };
		return findFile("su", pathToCheck);
	}

	public boolean findFile(String fileToFind, String[] pathToCheck) {
		if (pathToCheck == null) {
			File f = new File(fileToFind);
			if (f.exists()) {
				return true;
			} else {
				return false;
			}
		}
		for (String path : pathToCheck) {
			File f = new File(path, fileToFind);
			if (f.exists()) {
				return true;
			}
		}
		return false;
	}

	public boolean sendShell(final String[] commands, final String msg) {
		status = false;
		try {
			// info.add("sendShell " + msg);
			Thread t = new Thread() {
				public void run() {
					status = ShellInterface.doExec(commands);
				}
			};
			t.start();
			t.join();
		} catch (InterruptedException e) {
			// error.add("sendShell " + e);
			return false;
		}
		return status;
	}
}