/**
 * 
 */
package org.anjocaido.groupmanager.commands;

import java.util.ArrayList;
import java.util.List;

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.events.GMSystemEvent;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * @author ElgarL
 *
 */
public class ManLoad extends BaseCommand implements TabCompleter {

	/**
	 * 
	 */
	public ManLoad() {}

	@Override
	protected boolean parseCommand(@NotNull String[] args) {

		/**
		 * Attempt to reload a specific world
		 */
		if (args.length > 0) {

			if (!plugin.getLastError().isEmpty()) {
				sender.sendMessage(ChatColor.RED + "All commands are locked due to an error. " + ChatColor.BOLD + "" + ChatColor.UNDERLINE + "Check plugins/groupmanager/error.log or console" + ChatColor.RESET + "" + ChatColor.RED + " and then try a '/manload'.");
				return true;
			}

			auxString = "";
			for (int i = 0; i < args.length; i++) {
				auxString += args[i];
				if ((i + 1) < args.length) {
					auxString += " ";
				}
			}

			GroupManager.setLoaded(false); // Disable Bukkit Perms update and event triggers

			GroupManager.getGlobalGroups().load();
			plugin.getWorldsHolder().loadWorld(auxString);

			sender.sendMessage("The request to reload world '" + auxString + "' was attempted.");

			GroupManager.setLoaded(true);

			GroupManager.getBukkitPermissions().reset();

		} else {

			/**
			 * Reload all settings and data as no world was specified.
			 */

			/*
			 * Attempting a fresh load.
			 */
			plugin.onDisable(true);
			plugin.onEnable(true);

			sender.sendMessage("All settings and worlds were reloaded!");
		}

		/**
		 * Fire an event as none will have been triggered in the reload.
		 */
		if (GroupManager.isLoaded())
			GroupManager.getGMEventHandler().callEvent(GMSystemEvent.Action.RELOADED);

		return true;
	}
	
	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
		
		parseSender(sender, alias);
		
		/*
		 * Populate the first argument of TabComplete with a list of valid world roots.
		 */
		if (args.length == 1) {
			return getWorlds();
		}
		
		return new ArrayList<String>();
	}

}
