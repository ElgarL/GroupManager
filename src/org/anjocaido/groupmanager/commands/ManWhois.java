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

import org.anjocaido.groupmanager.localization.Messages;
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
public class ManWhois extends BaseCommand implements TabCompleter {

	/**
	 * 
	 */
	public ManWhois() {}

	@Override
	protected boolean parseCommand(@NotNull String[] args) {

		// Validating state of sender
		if (dataHolder == null || permissionHandler == null) {
			if (!setDefaultWorldHandler(sender))
				return true;
		}
		// Validating arguments
		if (args.length != 1) {
			sender.sendMessage(ChatColor.RED + Messages.getString("ERROR_REVIEW_ARGUMENTS") + Messages.getString("MANWHOIS_SYNTAX")); //$NON-NLS-1$ //$NON-NLS-2$
			return true;
		}
		if ((plugin.isValidateOnlinePlayer()) && ((match = validatePlayer(args[0], sender)) == null)) {
			return false;
		}
		if (match != null) {
			auxUser = dataHolder.getUser(match.toString());
		} else {
			auxUser = dataHolder.getUser(args[0]);
		}
		// Seems OK
		sender.sendMessage(ChatColor.YELLOW + Messages.getString("NAME") + ChatColor.GREEN + auxUser.getLastName()); //$NON-NLS-1$
		sender.sendMessage(ChatColor.YELLOW + Messages.getString("GROUP") + ChatColor.GREEN + auxUser.getGroup().getName()); //$NON-NLS-1$
		// Compile a list of subgroups
		auxString = "";
		for (String subGroup : auxUser.subGroupListStringCopy()) {
			auxString += subGroup + ", "; //$NON-NLS-1$
		}
		if (auxString.lastIndexOf(",") > 0) { //$NON-NLS-1$
			auxString = auxString.substring(0, auxString.lastIndexOf(",")); //$NON-NLS-1$
			sender.sendMessage(ChatColor.YELLOW + Messages.getString("SUBGROUPS") + auxString); //$NON-NLS-1$
		}

		sender.sendMessage(ChatColor.YELLOW + Messages.getString("OVERLOADED") + ChatColor.GREEN + dataHolder.isOverloaded(auxUser.getUUID())); //$NON-NLS-1$
		auxGroup = dataHolder.surpassOverload(auxUser.getUUID()).getGroup();
		if (!auxGroup.equals(auxUser.getGroup())) {
			sender.sendMessage(ChatColor.YELLOW + Messages.getString("ORIGINAL_GROUP") + ChatColor.GREEN + auxGroup.getName()); //$NON-NLS-1$
		}
		
		return true;
	}
	
	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {

		parseSender(sender, alias);
		
		List<String> result = new ArrayList<String>();
		
		/*
		 * Return a TabComplete for users.
		 */
		if (args.length == 1) {

			result = tabCompleteUsers(args[0]);
		}
		
		return result;
	}

}
