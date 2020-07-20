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

import org.anjocaido.groupmanager.utils.Tasks;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.reader.UnicodeReader;

/**
 * 
 * @author gabrielcouto
 */
public class GMConfiguration {
	
	private boolean allowCommandBlocks = false;
	private boolean opOverride = true;
	private boolean toggleValidate = true;
	private Integer saveInterval = 10;
	private Integer backupDuration = 24;
	private String loggerLevel = "OFF";
	private Map<String, Object> mirrorsMap;
	

	private GroupManager plugin;
	private Map<String, Object> GMconfig;

	public GMConfiguration(GroupManager plugin) {

		this.plugin = plugin;
		
		/*
		 * Set defaults
		 */
		allowCommandBlocks = false;
		opOverride = true;
		toggleValidate = true;
		saveInterval = 10;
		backupDuration = 24;
		loggerLevel = "OFF";
				
		load();
	}

	@SuppressWarnings("unchecked")
	public void load() {

		if (!plugin.getDataFolder().exists()) {
			plugin.getDataFolder().mkdirs();
		}

		File configFile = new File(plugin.getDataFolder(), "config.yml");

		if (!configFile.exists()) {
			try {
				Tasks.copy(plugin.getResource("config.yml"), configFile);
			} catch (IOException ex) {
				GroupManager.logger.log(Level.SEVERE, "Error creating a new config.yml", ex);
			}
		}

		Yaml configYAML = new Yaml(new SafeConstructor());

		try {
			FileInputStream configInputStream = new FileInputStream(configFile);
			GMconfig = (Map<String, Object>) configYAML.load(new UnicodeReader(configInputStream));
			configInputStream.close();

		} catch (Exception ex) {
			throw new IllegalArgumentException(String.format("The following file is corrupt: %s", configFile.getPath()), ex);
		}

		/*
		 * Read our config settings and store them for reading later.
		 */
		try {
			Map<String, Object> config = getElement("config", getElement("settings", GMconfig));

			try {
				allowCommandBlocks = (Boolean) config.get("allow_commandblocks");
			} catch (Exception ex) {
				GroupManager.logger.log(Level.SEVERE, nodeError("allow_commandblocks"), ex);
			}
			
			try {
				opOverride = (Boolean) config.get("opOverrides");
			} catch (Exception ex) {
				GroupManager.logger.log(Level.SEVERE, nodeError("opOverrides"), ex);
			}
			
			try {
				toggleValidate = (Boolean) config.get("validate_toggle");
			} catch (Exception ex) {
				GroupManager.logger.log(Level.SEVERE, nodeError("validate_toggle"), ex);
			}

			/*
			 * data node for save/backup timers.
			 */
			try {
				Map<String, Object> save = getElement("save", getElement("data", getElement("settings", GMconfig)));
				
				try {
					saveInterval = (Integer) save.get("minutes");
				} catch (Exception ex) {
					GroupManager.logger.log(Level.SEVERE, nodeError("minutes"), ex);
				}
				
				try {
					backupDuration = (Integer) save.get("hours");
				} catch (Exception ex) {
					GroupManager.logger.log(Level.SEVERE, nodeError("hours"), ex);
				}
				
			} catch (Exception ex) {
				GroupManager.logger.log(Level.SEVERE, nodeError("data"), ex);
			}

			

			Object level = ((Map<String, String>) getElement("settings", GMconfig).get("logging")).get("level");
			if (level instanceof String)
				loggerLevel = (String) level;

			/*
			 * Store our mirrors map for parsing later.
			 */
			mirrorsMap = (Map<String, Object>) ((Map<String, Object>) GMconfig.get("settings")).get("mirrors");
			
			if (mirrorsMap == null)
				throw new Exception();

		} catch (Exception ex) {
			/*
			 * Flag the error and use defaults
			 */
			GroupManager.logger.log(Level.SEVERE, "There are errors in your config.yml. Using default settings", ex);
			
			mirrorsMap = new HashMap<String, Object>();
		}
		// Setup defaults
		adjustLoggerLevel();
		plugin.setValidateOnlinePlayer(isToggleValidate());
	}
	
	private String nodeError(String node) {
		
		return String.format("Missing or corrupt '%s' node. Using default settings", node);
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, Object> getElement(String element, Map<String, Object> map) {
		
		if (!map.containsKey(element)) {
			throw new IllegalArgumentException(String.format("The config.yml has no '%s'.", element));
		}
		
		return (Map<String, Object>) map.get(element);
		
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
