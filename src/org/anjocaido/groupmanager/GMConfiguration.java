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
	
	private boolean checkForUpdates = true;
	private String language = "english";
	private boolean allowCommandBlocks = false;
	private boolean opOverride = true;
	private boolean toggleValidate = true;
	private boolean tabValidate = true;
	private Integer saveInterval = 10;
	private Integer backupDuration = 24;
	private String loggerLevel = "OFF"; //$NON-NLS-1$
	private Map<String, Object> mirrorsMap;
	

	private GroupManager plugin;
	private Map<String, Object> GMconfig;

	public GMConfiguration(GroupManager plugin) {

		this.plugin = plugin;
		
		/*
		 * Set defaults
		 */
		checkForUpdates = true;
		language = "english"; //$NON-NLS-1$
		allowCommandBlocks = false;
		opOverride = true;
		toggleValidate = true;
		tabValidate = true;
		saveInterval = 10;
		backupDuration = 24;
		loggerLevel = "OFF"; //$NON-NLS-1$
				
		//load();
	}

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

		try {
			FileInputStream configInputStream = new FileInputStream(configFile);
			GMconfig = (Map<String, Object>) configYAML.load(new UnicodeReader(configInputStream));
			configInputStream.close();

		} catch (Exception ex) {
			throw new IllegalArgumentException(String.format(Messages.getString("GroupManager.FILE_CORRUPT"), configFile.getPath()), ex); //$NON-NLS-1$
		}

		/*
		 * Read our config settings and store them for reading later.
		 */
		try {
			Map<String, Object> config = getElement("config", getElement("settings", GMconfig)); //$NON-NLS-1$ //$NON-NLS-2$
			
			try {
				checkForUpdates = (Boolean) config.get("check_for_updates"); //$NON-NLS-1$
			} catch (Exception ex) {
				GroupManager.logger.log(Level.SEVERE, nodeError("check_for_updates"), ex); //$NON-NLS-1$
				checkForUpdates = true;
			}			
			
			try {
				language = (String) config.get("language"); //$NON-NLS-1$
			} catch (Exception ex) {
				GroupManager.logger.log(Level.SEVERE, nodeError("language"), ex); //$NON-NLS-1$
			}
			if (language == null || language.isEmpty()) language = "english";
			Messages.setLanguage();
			
			try {
				allowCommandBlocks = (Boolean) config.get("allow_commandblocks"); //$NON-NLS-1$
			} catch (Exception ex) {
				GroupManager.logger.log(Level.SEVERE, nodeError("allow_commandblocks"), ex); //$NON-NLS-1$
				allowCommandBlocks = false;
			}
			
			try {
				opOverride = (Boolean) config.get("opOverrides"); //$NON-NLS-1$
			} catch (Exception ex) {
				GroupManager.logger.log(Level.SEVERE, nodeError("opOverrides"), ex); //$NON-NLS-1$
				opOverride = true;
			}
			
			try {
				toggleValidate = (Boolean) config.get("validate_toggle"); //$NON-NLS-1$
			} catch (Exception ex) {
				GroupManager.logger.log(Level.SEVERE, nodeError("validate_toggle"), ex); //$NON-NLS-1$
				toggleValidate = true;
			}
			
			try {
				tabValidate = (Boolean) config.get("tab_validate"); //$NON-NLS-1$
			} catch (Exception ex) {
				GroupManager.logger.log(Level.SEVERE, nodeError("tab_validate"), ex); //$NON-NLS-1$
				tabValidate = true;
			}

			/*
			 * data node for save/backup timers.
			 */
			try {
				Map<String, Object> save = getElement("save", getElement("data", getElement("settings", GMconfig))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				
				try {
					saveInterval = (Integer) save.get("minutes"); //$NON-NLS-1$
				} catch (Exception ex) {
					GroupManager.logger.log(Level.SEVERE, nodeError("minutes"), ex); //$NON-NLS-1$
					saveInterval = 10;
				}
				
				try {
					backupDuration = (Integer) save.get("hours"); //$NON-NLS-1$
				} catch (Exception ex) {
					GroupManager.logger.log(Level.SEVERE, nodeError("hours"), ex); //$NON-NLS-1$
					backupDuration = 24;
				}
				
			} catch (Exception ex) {
				GroupManager.logger.log(Level.SEVERE, nodeError("data"), ex); //$NON-NLS-1$
			}

			

			Object level = ((Map<String, String>) getElement("settings", GMconfig).get("logging")).get("level"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (level instanceof String)
				loggerLevel = (String) level;

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
			
			mirrorsMap = new HashMap<String, Object>();
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

	public Integer getSaveInterval() {

		return saveInterval;
	}

	public Integer getBackupDuration() {

		return backupDuration;
	}

	public void adjustLoggerLevel() {

		try {
			GroupManager.logger.setLevel(Level.parse(loggerLevel));
			return;
		} catch (Exception e) {
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
