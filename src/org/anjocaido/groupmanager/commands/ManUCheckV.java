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
public class ManUCheckV extends BaseCommand {

	/**
	 * 
	 */
	public ManUCheckV() {}

	@Override
	protected boolean parseCommand(@NotNull String[] args) {

		// Validating state of sender
		if (dataHolder == null || permissionHandler == null) {
			if (!setDefaultWorldHandler(sender))
				return true;
		}
		// Validating arguments
		if (args.length != 2) {
			sender.sendMessage(ChatColor.RED + Messages.getString("ERROR_REVIEW_ARGUMENTS") + Messages.getString("MANUCHECKV_SYNTAX")); //$NON-NLS-1$ //$NON-NLS-2$
			return true;
		}
		if ((GroupManager.getGMConfig().isToggleValidate()) && ((match = validatePlayer(args[0], sender)) == null)) {
			return false;
		}
		if (match != null) {
			auxUser = dataHolder.getUser(match.toString());
		} else {
			auxUser = dataHolder.getUser(args[0]);
		}
		// Validating permission
		auxGroup = auxUser.getGroup();
		auxGroup2 = permissionHandler.nextGroupWithVariable(auxGroup, args[1]);

		if (!auxUser.getVariables().hasVar(args[1])) {
			// Check sub groups
			if (!auxUser.isSubGroupsEmpty() && auxGroup2 == null)
				for (Group subGroup : auxUser.subGroupListCopy()) {
					auxGroup2 = permissionHandler.nextGroupWithVariable(subGroup, args[1]);
				}
			if (auxGroup2 == null) {
				sender.sendMessage(ChatColor.YELLOW + Messages.getString("ERROR_USER_NO_ACCESS_VARIABLE")); //$NON-NLS-1$
				return true;
			}
		}
		// Seems OK
		if (auxUser.getVariables().hasVar(auxString)) {
			sender.sendMessage(ChatColor.YELLOW + String.format(Messages.getString("VARIABLE_VALUE"), ChatColor.GOLD + args[1] + ChatColor.YELLOW, ChatColor.GREEN + auxUser.getVariables().getVarObject(args[1]).toString() + ChatColor.WHITE)); //$NON-NLS-1$
			sender.sendMessage(ChatColor.YELLOW + Messages.getString("USER_HAS_VARIABLE_DIRECT"));
		}
		sender.sendMessage(ChatColor.YELLOW + String.format(Messages.getString("VARIABLE_VALUE"), ChatColor.GOLD + args[1] + ChatColor.YELLOW, ChatColor.GREEN + auxGroup2.getVariables().getVarObject(args[1]).toString() + ChatColor.WHITE)); //$NON-NLS-1$
		if (!auxGroup.equals(auxGroup2)) {
			sender.sendMessage(ChatColor.YELLOW + Messages.getString("VARIABLE_INHERITED") + ChatColor.GREEN + auxGroup2.getName()); //$NON-NLS-1$
		}

		return true;
	}

	@Override
	public @Nullable List<String> tabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
		
		List<String> result = new ArrayList<>();
		
		/*
		 * Return a TabComplete for users.
		 */
		if (args.length == 1) {

			result = tabCompleteUsers(args[0]);
		}
		
		return result;
	}
}
