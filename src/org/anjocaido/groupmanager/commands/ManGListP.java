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
public class ManGListP extends BaseCommand implements TabCompleter {

	/**
	 * 
	 */
	public ManGListP() {}

	@Override
	protected boolean parseCommand(@NotNull String[] args) {

		// Validating state of sender
		if (dataHolder == null || permissionHandler == null) {
			if (!setDefaultWorldHandler(sender))
				return true;
		}
		// Validating arguments
		if (args.length != 1) {
			sender.sendMessage(ChatColor.RED + "Review your arguments count! (/manglistp <group>)");
			return true;
		}
		auxGroup = dataHolder.getGroup(args[0]);
		if (auxGroup == null) {
			sender.sendMessage(ChatColor.RED + "'" + args[0] + "' Group doesnt exist!");
			return true;
		}
		// Validating permission

		// Seems OK
		auxString = "";
		for (String perm : auxGroup.getPermissionList()) {
			auxString += perm + ", ";
		}
		if (auxString.lastIndexOf(",") > 0) {
			auxString = auxString.substring(0, auxString.lastIndexOf(","));
			sender.sendMessage(ChatColor.YELLOW + "The group '" + auxGroup.getName() + "' has following permissions: " + ChatColor.WHITE + auxString);
			auxString = "";
			for (String grp : auxGroup.getInherits()) {
				auxString += grp + ", ";
			}
			if (auxString.lastIndexOf(",") > 0) {
				auxString = auxString.substring(0, auxString.lastIndexOf(","));
				sender.sendMessage(ChatColor.YELLOW + "And all permissions from groups: " + auxString);
			}

		} else {
			sender.sendMessage(ChatColor.YELLOW + "The group '" + auxGroup.getName() + "' has no specific permissions.");
			auxString = "";
			for (String grp : auxGroup.getInherits()) {
				auxString += grp + ", ";
			}
			if (auxString.lastIndexOf(",") > 0) {
				auxString = auxString.substring(0, auxString.lastIndexOf(","));
				sender.sendMessage(ChatColor.YELLOW + "Only all permissions from groups: " + auxString);
			}

		}
		return true;
	}
	
	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {

		parseSender(sender, alias);
		
		List<String> result = new ArrayList<String>();
		/*
		 * Return a TabComplete for groups.
		 */
		if (args.length == 1) {

			for (Group g : dataHolder.getGroupList()) {
				result.add(g.getName());
			}
			
			/*
			 * Include global groups.
			 */
			for (Group g : GroupManager.getGlobalGroups().getGroupList()) {
				result.add(g.getName());
			}
		}
		return result;
	}

}
