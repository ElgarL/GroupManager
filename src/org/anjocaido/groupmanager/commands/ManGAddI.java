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
import org.anjocaido.groupmanager.data.Group;
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
public class ManGAddI extends BaseCommand implements TabCompleter {

	/**
	 * 
	 */
	public ManGAddI() {}

	@Override
	protected boolean parseCommand(@NotNull String[] args) {

		// Validating state of sender
		if (dataHolder == null || permissionHandler == null) {
			if (!setDefaultWorldHandler(sender))
				return true;
		}
		// Validating arguments
		if (args.length != 2) {
			sender.sendMessage(ChatColor.RED + "Review your arguments count! (/mangaddi <group1> <group2>)");
			return true;
		}
		auxGroup = dataHolder.getGroup(args[0]);
		if (auxGroup == null) {
			sender.sendMessage(ChatColor.RED + "'" + args[0] + "' Group doesnt exist!");
			return true;
		}
		auxGroup2 = dataHolder.getGroup(args[1]);
		if (auxGroup2 == null) {
			sender.sendMessage(ChatColor.RED + "'" + args[1] + "' Group doesnt exist!");
			return true;
		}
		if (auxGroup.isGlobal()) {
			sender.sendMessage(ChatColor.RED + "GlobalGroups do NOT support inheritance.");
			return true;
		}

		// Validating permission
		if (permissionHandler.hasGroupInInheritance(auxGroup, auxGroup2.getName())) {
			sender.sendMessage(ChatColor.RED + "Group " + auxGroup.getName() + " already inherits " + auxGroup2.getName() + " (might not be directly)");
			return true;
		}
		// Seems OK
		auxGroup.addInherits(auxGroup2);
		sender.sendMessage(ChatColor.YELLOW + "Group " + auxGroup2.getName() + " is now in " + auxGroup.getName() + " inheritance list.");

		GroupManager.getBukkitPermissions().updateAllPlayers();

		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {

		parseSender(sender, alias);
		
		List<String> result = new ArrayList<String>();
		/*
		 * Return a TabComplete for base groups.
		 */
		if (args.length == 1 || args.length == 2) {

			for (Group g : dataHolder.getGroupList()) {
				result.add(g.getName());
			}
			/*
			 * Include global groups.
			 */
			if (args.length == 2) {
				
				for (Group g : GroupManager.getGlobalGroups().getGroupList()) {
					result.add(g.getName());
				}
			}
			return result;
		}
		
		return null;
	}

}
