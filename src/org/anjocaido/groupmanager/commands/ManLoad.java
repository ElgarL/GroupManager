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
package org.anjocaido.groupmanager.commands;

import java.util.ArrayList;
import java.util.List;

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.events.GMSystemEvent;
import org.anjocaido.groupmanager.localization.Messages;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * @author ElgarL
 *
 */
public class ManLoad extends BaseCommand {

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
				sender.sendMessage(ChatColor.RED + Messages.getString("COMMAND_ERROR")); //$NON-NLS-1$
				return true;
			}

			auxString = ""; //$NON-NLS-1$
			for (int i = 0; i < args.length; i++) {
				auxString += args[i];
				if ((i + 1) < args.length) {
					auxString += " "; //$NON-NLS-1$
				}
			}

			GroupManager.setLoaded(false); // Disable Bukkit Perms update and event triggers

			GroupManager.getGlobalGroups().load();
			plugin.getWorldsHolder().loadWorld(auxString);

			sender.sendMessage(String.format(Messages.getString("RELOAD_REQUEST_ATTEMPT"), auxString)); //$NON-NLS-1$

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

			sender.sendMessage(Messages.getString("RELOADED")); //$NON-NLS-1$
		}

		/**
		 * Fire an event as none will have been triggered in the reload.
		 */
		if (GroupManager.isLoaded())
			GroupManager.getGMEventHandler().callEvent(GMSystemEvent.Action.RELOADED);

		return true;
	}
	
	@Override
	public @Nullable List<String> tabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
		
		/*
		 * Populate the first argument of TabComplete with a list of valid world roots.
		 */
		if (args.length == 1) {
			return getWorlds();
		}
		
		return new ArrayList<String>();
	}

}
