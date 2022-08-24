/*
 * 
 */
package org.anjocaido.groupmanager.utils;

import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;

/**
 * @author ElgarL
 *
 */
public class Supported {

	/**
	 * Does Spigot support library loading (1.16.5+)
	 * 
	 * @return true if supported.
	 */
	public static boolean hasLibraries() {

		try {
			// Doesn't exist before 1.16.5
			PluginDescriptionFile.class.getMethod("getLibraries");
			return true;
		} catch (Exception ex) {
			// Server too old to support getLibraries.
			return false;
		}
	}
	
	/**
	 * Does Spigot support Player.updateCommand() (1.14+?).
	 * 
	 * @return true if supported.
	 */
	public static boolean hasUpdateCommand() {
		
		try {
			// Method only available post 1.14
			Player.class.getMethod("updateCommands");
			return true;
		} catch (Exception ex) {
			// Server too old to support updateCommands.
			return false;
		}
	}
}
