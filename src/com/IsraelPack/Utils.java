package com.IsraelPack;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.util.Log;

public class Utils {
	public static boolean DownloadFromUrl(String url, String fileName) {
		try {
			BufferedInputStream in = new BufferedInputStream(new URL(url)
					.openStream());
			FileOutputStream fos = new FileOutputStream(fileName);
			BufferedOutputStream bout = new BufferedOutputStream(fos, 1024);
			byte[] data = new byte[1024];
			int x = 0;
			while ((x = in.read(data, 0, 1024)) >= 0) {
				bout.write(data, 0, x);
			}
			bout.close();
			in.close();
		} catch (IOException e) {
			Log.e("IsraelPack", e.getMessage());
			return false;
		}
		return true;
	}

	public static boolean checkMD5(String md5, String fileName) {
		if (md5 == null || md5 == "" || fileName == null) {
			return false;
		}
		String calculatedDigest = calculateMD5(fileName);
		if (calculatedDigest == null) {
			return false;
		}
		return calculatedDigest.equalsIgnoreCase(md5);
	}

	public static String calculateMD5(String fileName) {
		File updateFile = new File(fileName);
		MessageDigest digest = null;
		try {
			digest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
		InputStream is = null;
		try {
			is = new FileInputStream(updateFile);
		} catch (FileNotFoundException e) {
			return null;
		}
		byte[] buffer = new byte[8192];
		int read = 0;
		try {
			while ((read = is.read(buffer)) > 0) {
				digest.update(buffer, 0, read);
			}
			byte[] md5sum = digest.digest();
			BigInteger bigInt = new BigInteger(1, md5sum);
			String output = bigInt.toString(16);
			// Fill to 32 chars
			output = String.format("%32s", output).replace(' ', '0');
			return output;
		} catch (IOException e) {
			throw new RuntimeException("Unable to process file for MD5", e);
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				throw new RuntimeException(
						"Unable to close input stream for MD5 calculation", e);
			}
		}
	}

	public static String readFileAsString(String filePath) {
		try {
			StringBuffer fileData = new StringBuffer(1000);
			BufferedReader reader;

			reader = new BufferedReader(new FileReader(filePath));
			char[] buf = new char[1024];
			int numRead = 0;
			while ((numRead = reader.read(buf)) != -1) {
				String readData = String.valueOf(buf, 0, numRead);
				fileData.append(readData);
				buf = new char[1024];
			}
			reader.close();
			return fileData.toString();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}