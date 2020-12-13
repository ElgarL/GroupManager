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
package org.anjocaido.groupmanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.anjocaido.groupmanager.localization.Messages;
import org.anjocaido.groupmanager.utils.Tasks;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.reader.UnicodeReader;

/**
 * 
 * @author gabrielcouto
 */
public class GMConfiguration {

	private String language;
	private boolean allowCommandBlocks;
	private boolean opOverride;
	private boolean toggleValidate;
	private boolean tabValidate;

	private int saveInterval;
	private int backupDuration;

	private DBTYPE dbType;
	private String dbName;
	private String dbTable;
	private String dbUsername;
	private String dbPassword;
	private String dbHostname;
	private int dbPort;

	private boolean purgeEnabled;
	private long userExpires;

	private String loggerLevel; //$NON-NLS-1$
	private Map<String, Object> mirrorsMap;


	private final GroupManager plugin;

	public GMConfiguration(GroupManager plugin) {

		this.plugin = plugin;

		/*
		 * Set defaults
		 */
		language = "english"; //$NON-NLS-1$
		allowCommandBlocks = false;
		opOverride = true;
		toggleValidate = true;
		tabValidate = true;
		saveInterval = 10;
		backupDuration = 24;

		dbType = DBTYPE.YAML;
		dbName = "minecraft";
		dbTable = "GroupManager";
		dbUsername = "root";
		dbPassword = "pass";
		dbHostname = "localhost";
		dbPort = 3306;

		purgeEnabled = true;
		userExpires = Tasks.parsePeriod("90d"); //$NON-NLS-1$
		loggerLevel = "OFF"; //$NON-NLS-1$
	}

	public static enum DBTYPE { YAML, SQLITE, H2, MYSQL };

	@SuppressWarnings("unchecked")
	public void load() {

		if (!plugin.getDataFolder().exists()) {
			plugin.getDataFolder().mkdirs();
		}

		File configFile = new File(plugin.getDataFolder(), "config.yml"); //$NON-NLS-1$

		if (!configFile.exists()) {
			try {
				Tasks.copy(plugin.getResource("config.yml"), configFile); //$NON-NLS-1$
			} catch (IOException ex) {
				GroupManager.logger.log(Level.SEVERE, Messages.getString("GMConfiguration.ERROR_CREATING_CONFIG"), ex); //$NON-NLS-1$
			}
		}

		Yaml configYAML = new Yaml(new SafeConstructor());

		Map<String, Object> GMconfig;
		try {
			FileInputStream configInputStream = new FileInputStream(configFile);
			GMconfig = configYAML.load(new UnicodeReader(configInputStream));
			configInputStream.close();

		} catch (Exception ex) {
			throw new IllegalArgumentException(String.format(Messages.getString("GroupManager.FILE_CORRUPT"), configFile.getPath()), ex); //$NON-NLS-1$
		}

		/*
		 * Read our config settings and store them for reading later.
		 */
		try {
			Map<String, Object> section = getElement("config", getElement("settings", GMconfig)); //$NON-NLS-1$ //$NON-NLS-2$

			try {
				language = (String) section.get("language"); //$NON-NLS-1$
			} catch (Exception ex) {
				GroupManager.logger.log(Level.SEVERE, nodeError("language"), ex); //$NON-NLS-1$
			}
			if (language == null || language.isEmpty()) language = "english";
			Messages.setLanguage();

			try {
				allowCommandBlocks = (boolean) section.get("allow_commandblocks"); //$NON-NLS-1$
			} catch (Exception ex) {
				GroupManager.logger.log(Level.SEVERE, nodeError("allow_commandblocks"), ex); //$NON-NLS-1$
				allowCommandBlocks = false;
			}

			try {
				opOverride = (boolean) section.get("opOverrides"); //$NON-NLS-1$
			} catch (Exception ex) {
				GroupManager.logger.log(Level.SEVERE, nodeError("opOverrides"), ex); //$NON-NLS-1$
				opOverride = true;
			}

			try {
				toggleValidate = (boolean) section.get("validate_toggle"); //$NON-NLS-1$
			} catch (Exception ex) {
				GroupManager.logger.log(Level.SEVERE, nodeError("validate_toggle"), ex); //$NON-NLS-1$
				toggleValidate = true;
			}

			try {
				tabValidate = (boolean) section.get("tab_validate"); //$NON-NLS-1$
			} catch (Exception ex) {
				GroupManager.logger.log(Level.SEVERE, nodeError("tab_validate"), ex); //$NON-NLS-1$
				tabValidate = true;
			}

			/*
			 * data node for save/backup timers.
			 */
			try {
				section = getElement("save", getElement("data", getElement("settings", GMconfig))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

				try {
					saveInterval = Integer.parseInt(section.get("minutes").toString()); //$NON-NLS-1$
				} catch (Exception ex) {
					GroupManager.logger.log(Level.SEVERE, nodeError("minutes"), ex); //$NON-NLS-1$
					saveInterval = 10;
				}

				try {
					backupDuration = Integer.parseInt(section.get("hours").toString()); //$NON-NLS-1$
				} catch (Exception ex) {
					GroupManager.logger.log(Level.SEVERE, nodeError("hours"), ex); //$NON-NLS-1$
					backupDuration = 24;
				}

			} catch (Exception ex) {
				GroupManager.logger.log(Level.SEVERE, nodeError("data"), ex); //$NON-NLS-1$
			}

			/*
			 * data node for database.
			 */
			try {
				section = getElement("database", getElement("data", getElement("settings", GMconfig))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

				try {
					dbType = DBTYPE.valueOf(section.get("type").toString().toUpperCase()); //$NON-NLS-1$
				} catch (Exception ex) {
					GroupManager.logger.log(Level.SEVERE, nodeError("type"), ex); //$NON-NLS-1$
					dbType = DBTYPE.YAML;
				}

				try {
					dbName = (String) section.get("name"); //$NON-NLS-1$
				} catch (Exception ex) {
					GroupManager.logger.log(Level.SEVERE, nodeError("name"), ex); //$NON-NLS-1$
					dbName = "minecraft";
				}

				try {
					dbTable = (String) section.get("table"); //$NON-NLS-1$
				} catch (Exception ex) {
					GroupManager.logger.log(Level.SEVERE, nodeError("table"), ex); //$NON-NLS-1$
					dbTable = "GroupManager";
				}

				/*
				 * MySQL nodes.
				 */
				section = getElement("mysql", getElement("database", getElement("data", getElement("settings", GMconfig)))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

				try {
					dbUsername = (String) section.get("username"); //$NON-NLS-1$
				} catch (Exception ex) {
					GroupManager.logger.log(Level.SEVERE, nodeError("username"), ex); //$NON-NLS-1$
					dbUsername = "root";
				}

				try {
					dbPassword = (String) section.get("password"); //$NON-NLS-1$
				} catch (Exception ex) {
					GroupManager.logger.log(Level.SEVERE, nodeError("password"), ex); //$NON-NLS-1$
					dbPassword = "pass";
				}

				try {
					dbHostname = (String) section.get("hostname"); //$NON-NLS-1$
				} catch (Exception ex) {
					GroupManager.logger.log(Level.SEVERE, nodeError("hostname"), ex); //$NON-NLS-1$
					dbHostname = "localhost";
				}

				try {
					dbPort = Integer.parseInt(section.get("port").toString()); //$NON-NLS-1$
				} catch (Exception ex) {
					GroupManager.logger.log(Level.SEVERE, nodeError("port"), ex); //$NON-NLS-1$
					dbPort = 3306;
				}

			} catch (Exception ex) {
				GroupManager.logger.log(Level.SEVERE, nodeError("database"), ex); //$NON-NLS-1$
			}

			/*
			 * data node for maintenance.
			 */
			try {
				section = getElement("purge", getElement("maintenance", getElement("settings", GMconfig))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

				try {
					purgeEnabled = (boolean) section.get("enabled"); //$NON-NLS-1$
				} catch (Exception ex) {
					GroupManager.logger.log(Level.SEVERE, nodeError("purge-enabled"), ex); //$NON-NLS-1$
					purgeEnabled = true;
				}

				try {
					userExpires = Tasks.parsePeriod((String) section.get("user_expires")); //$NON-NLS-1$
				} catch (Exception ex) {
					GroupManager.logger.log(Level.SEVERE, nodeError("user_expires"), ex); //$NON-NLS-1$
					userExpires = Tasks.parsePeriod("90d"); //$NON-NLS-1$
				}

			} catch (Exception ex) {
				GroupManager.logger.log(Level.SEVERE, nodeError("purge"), ex); //$NON-NLS-1$
			}


			String level = ((Map<String, String>) getElement("settings", GMconfig).get("logging")).get("level"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (level != null)
				loggerLevel = level;

			/*
			 * Store our mirrors map for parsing later.
			 */
			mirrorsMap = (Map<String, Object>) ((Map<String, Object>) GMconfig.get("settings")).get("mirrors"); //$NON-NLS-1$ //$NON-NLS-2$

			if (mirrorsMap == null)
				throw new Exception();

		} catch (Exception ex) {
			/*
			 * Flag the error and use defaults
			 */
			GroupManager.logger.log(Level.SEVERE, Messages.getString("GMConfiguration.ERRORS_IN_CONFIG"), ex); //$NON-NLS-1$

			mirrorsMap = new HashMap<>();
		}
		// Setup defaults
		adjustLoggerLevel();
	}

	private String nodeError(String node) {

		return String.format(Messages.getString("GMConfiguration.CORRUPT_NODE"), node); //$NON-NLS-1$
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getElement(String element, Map<String, Object> map) {

		if (!map.containsKey(element)) {
			throw new IllegalArgumentException(String.format(Messages.getString("GMConfiguration.MISSING_NODE"), element)); //$NON-NLS-1$
		}

		return (Map<String, Object>) map.get(element);

	}

	public String getLanguage() {

		return language;
	}

	public boolean isAllowCommandBlocks() {

		return allowCommandBlocks;
	}

	public boolean isOpOverride() {

		return opOverride;
	}

	public boolean isToggleValidate() {

		return toggleValidate;
	}

	public void setToggleValidate(Boolean value) {

		this.toggleValidate = value;
	}

	public boolean isTabValidate() {

		return tabValidate;
	}

	public boolean isPurgeEnabled() {

		return purgeEnabled;
	}

	public long userExpires() {

		return userExpires;
	}

	public int getSaveInterval() {

		return saveInterval;
	}

	public int getBackupDuration() {

		return backupDuration;
	}

	/**
	 * @return the dbType
	 */
	public DBTYPE getDbType() {

		return dbType;
	}


	/**
	 * @return the dbName
	 */
	public String getDbName() {

		return dbName;
	}


	/**
	 * @return the dbTable
	 */
	public String getDbTable() {

		return dbTable;
	}


	/**
	 * @return the username
	 */
	public String getUsername() {

		return dbUsername;
	}


	/**
	 * @return the password
	 */
	public String getPassword() {

		return dbPassword;
	}


	/**
	 * @return the hostname
	 */
	public String getHostname() {

		return dbHostname;
	}


	/**
	 * @return the port
	 */
	public int getPort() {

		return dbPort;
	}

	public void adjustLoggerLevel() {

		try {
			GroupManager.logger.setLevel(Level.parse(loggerLevel));
			return;
		} catch (Exception ignored) {
		}

		GroupManager.logger.setLevel(Level.INFO);
	}

	public Map<String, Object> getMirrorsMap() {

		if (!mirrorsMap.isEmpty()) {
			return mirrorsMap;
		}
		return null;

	}

}
