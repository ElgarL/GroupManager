/*
 *  GroupManager - A plug-in for Spigot/Bukkit based Minecraft servers.
 *  Copyright (C) 2020  ElgarL
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.anjocaido.groupmanager.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.data.Group;
import org.apache.commons.lang.StringUtils;
import org.bukkit.plugin.Plugin;

/**
 * 
 * @author gabrielcouto
 */
public abstract class Tasks {

	/**
	 * Gets the exception stack trace as a string.
	 * 
	 * @param exception
	 * @return stack trace as a string
	 */
	public static String getStackTraceAsString(Exception exception) {

		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		exception.printStackTrace(pw);
		return sw.toString();
	}
	
	public static void printStackTrace() {
		
		StringUtils.join(new Throwable().getStackTrace(), "\n");
	}

	public static void copy(InputStream src, File dst) throws IOException {

		OutputStream out = new FileOutputStream(dst);

		// Transfer bytes from in to out
		byte[] buf = new byte[1024];
		int len;
		while ((len = src.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		out.close();
		try {
			src.close();
		} catch (Exception ignored) {}
	}

	public static void copy(File src, File dst) throws IOException {

		InputStream in = new FileInputStream(src);
		copy(in, dst);
	}
	
	public static void saveResource(Plugin plugin, File file) {
		
		if (!file.exists() || file.length() == 0) {

			InputStream template = plugin.getResource(file.getName()); //$NON-NLS-1$
			try {
				Tasks.copy(template, file);
			} catch (IOException ex) {
				GroupManager.logger.log(Level.SEVERE, null, ex);
			}
		}
	}

	/**
	 * Appends a string to a file
	 * 
	 * @param data
	 * @param file
	 */
	public static void appendStringToFile(String data, String file) throws IOException {

		FileWriter outStream = new FileWriter("." + System.getProperty("file.separator") + file, true);

		BufferedWriter out = new BufferedWriter(outStream);

		data.replaceAll("\n", System.getProperty("line.separator"));

		out.append(new SimpleDateFormat("yyyy-MM-dd HH-mm").format(System.currentTimeMillis()));
		out.append(System.getProperty("line.separator"));
		out.append(data);
		out.append(System.getProperty("line.separator"));

		out.close();
	}

	public static void removeOldFiles(File folder) {

		if (folder.isDirectory()) {
			long oldTime = System.currentTimeMillis() - (((long) GroupManager.getGMConfig().getBackupDuration() * 60 * 60) * 1000);
			for (File olds : folder.listFiles()) {
				if (olds.isFile()) {
					if (olds.lastModified() < oldTime) {
						try {
							olds.delete();
						} catch (Exception ignored) {
						}
					}
				}
			}
		}
	}

	public static String getDateString() {

		GregorianCalendar now = new GregorianCalendar();
		String date = "";
		date += now.get(Calendar.DAY_OF_MONTH);
		date += "-";
		date += now.get(Calendar.HOUR);
		date += "-";
		date += now.get(Calendar.MINUTE);
		return date;
	}

	private static final Pattern periodPattern = Pattern.compile("([0-9]+)([mhd])");

	public static Long parsePeriod(String period){

		if(period == null) return null;
		period = period.toLowerCase(Locale.ENGLISH);
		Matcher matcher = periodPattern.matcher(period);
		Instant instant=Instant.EPOCH;

		while(matcher.find()){
			int num = Integer.parseInt(matcher.group(1));
			String typ = matcher.group(2);
			switch (typ) {
			case "m":
				instant=instant.plus(Duration.ofMinutes(num));
				break;
			case "h":
				instant=instant.plus(Duration.ofHours(num));
				break;
			case "d":
				instant=instant.plus(Duration.ofDays(num));
				break;
			}
		}
		return TimeUnit.MILLISECONDS.toMinutes(instant.toEpochMilli());
	}

	/**
	 * Is this time in the past?
	 * 
	 * @param expires	long of seconds since Epoch.
	 * @return	true if it has expired.
	 */
	public static boolean isExpired(Long expires) {

		if (expires == 0) return false;
		/*
		 * Time has expired?
		 */
		return Instant.now().isAfter(Instant.ofEpochSecond(expires));
	}

	public static String getStringListInString(List<String> list) {

		if (list == null) {
			return "";
		}
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < list.size(); i++) {
			result.append(list.get(i));
			if (i < list.size() - 1) {
				result.append(", ");
			}
		}
		return result.toString();
	}

	public static String getStringArrayInString(String[] list) {

		if (list == null) {
			return "";
		}
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < list.length; i++) {
			result.append(list[i]);
			if (i < ((list.length) - 1)) {
				result.append(", ");
			}
		}
		return result.toString();
	}

	public static String getGroupListInString(List<Group> list) {

		if (list == null) {
			return "";
		}
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < list.size(); i++) {
			result.append(list.get(i).getName());
			if (i < list.size() - 1) {
				result.append(", ");
			}
		}
		return result.toString();
	}

	public static String join(String[] arr, String separator) {

		if (arr.length == 0)
			return "";
		StringBuilder out = new StringBuilder(arr[0]);
		for (int i = 1; i < arr.length; i++)
			out.append(separator).append(arr[i]);
		return out.toString();
	}

}
