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
package org.anjocaido.groupmanager.localization;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;
import java.util.logging.Level;

import org.anjocaido.groupmanager.GroupManager;

public class Messages {

	private static String BUNDLE_NAME = "languages.english"; //$NON-NLS-1$

	private static ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME, new UTF8Control());

	private Messages() {

	}

	public static String getString(String key) {

		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}

	public static void setLanguage() {

		try {
			BUNDLE_NAME = "languages." + GroupManager.getGMConfig().getLanguage();
			RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME, new UTF8Control());
		} catch (Exception ex) {
			// Invalid name, use default.
			GroupManager.logger.log(Level.WARNING, "Missing or corrupt 'language' node. Using default settings");
			BUNDLE_NAME = "languages.english";
			RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME, new UTF8Control());
		}
	}

	static class UTF8Control extends Control {

		public ResourceBundle newBundle (String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
				throws IOException
		{
			// The below is a copy of the default implementation.
			String bundleName = toBundleName(baseName, locale);
			String resourceName = toResourceName(bundleName, "properties");
			ResourceBundle bundle = null;
			InputStream stream = null;
			if (reload) {
				URL url = loader.getResource(resourceName);
				if (url != null) {
					URLConnection connection = url.openConnection();
					if (connection != null) {
						connection.setUseCaches(false);
						stream = connection.getInputStream();
					}
				}
			} else {
				stream = loader.getResourceAsStream(resourceName);
			}
			if (stream != null) {
				try {
					// Only this line is changed to make it read properties files as UTF-8.
					bundle = new PropertyResourceBundle(new InputStreamReader(stream, StandardCharsets.UTF_8));
				} finally {
					stream.close();
				}
			}
			return bundle;
		}
	}
}
