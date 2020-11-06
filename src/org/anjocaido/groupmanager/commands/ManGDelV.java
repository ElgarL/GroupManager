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
import java.util.Arrays;
import java.util.List;

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
public class ManGDelV extends BaseCommand {

	/**
	 * 
	 */
	public ManGDelV() {}

	@Override
	protected boolean parseCommand(@NotNull String[] args) {

		// Validating state of sender
		if (dataHolder == null || permissionHandler == null) {
			if (!setDefaultWorldHandler(sender))
				return true;
		}
		// Validating arguments
		if (args.length != 2) {
			sender.sendMessage(ChatColor.RED + Messages.getString("ERROR_REVIEW_ARGUMENTS") + Messages.getString("MANGDELV_SYNTAX")); //$NON-NLS-1$ //$NON-NLS-2$
			return true;
		}
		auxGroup = dataHolder.getGroup(args[0]);
		if (auxGroup == null) {
			sender.sendMessage(ChatColor.RED + String.format(Messages.getString("ERROR_GROUP_DOES_NOT_EXIST"), args[0])); //$NON-NLS-1$
			return true;
		}
		if (auxGroup.isGlobal()) {
			sender.sendMessage(ChatColor.RED + Messages.getString("ERROR_GG_DO_NOT_SUPPORT_INFO_NODES")); //$NON-NLS-1$
			return true;
		}
		// Validating permission
		if (!auxGroup.getVariables().hasVar(args[1])) {
			sender.sendMessage(ChatColor.RED + Messages.getString("ERROR_GROUP_NO_ACCESS_VARIABLE_DIRECT")); //$NON-NLS-1$
			return true;
		}
		// Seems OK
		auxGroup.getVariables().removeVar(args[1]);
		sender.sendMessage(String.format(ChatColor.YELLOW + Messages.getString("VARIABLE_REMOVED_FROM_GROUP"), ChatColor.GOLD + args[1] + ChatColor.YELLOW, ChatColor.GREEN + auxGroup.getLastName()));
		
		return true;
	}
	
	@Override
	public @Nullable List<String> tabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
		
		List<String> result = new ArrayList<>();
		/*
		 * Return a TabComplete for base groups.
		 */
		if (args.length == 1) {

			result = tabCompleteGroups(args[0]);
		}
		/*
		 * Return a TabComplete for Variables.
		 */
		if (args.length == 2) {
			try {
				result = Arrays.asList(dataHolder.getGroup(args[0]).getVariables().getVarKeyList());
			} catch (Exception ex) {
				// Failed to match first group!
			}
		}
		return result;

	}

}
