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
public class ManUAdd extends BaseCommand {

	/**
	 * 
	 */
	public ManUAdd() {}

	protected boolean parseCommand(@NotNull String[] args) {

		// Validating arguments
		if ((args.length != 2) && (args.length != 3)) {
			sender.sendMessage(ChatColor.RED + Messages.getString("ERROR_REVIEW_ARGUMENTS") + Messages.getString("MANUADD_SYNTAX")); //$NON-NLS-1$ //$NON-NLS-2$
			return true;
		}

		// Select the relevant world (if specified)
		if (args.length == 3) {
			dataHolder = plugin.getWorldsHolder().getWorldData(args[2]);
			permissionHandler = dataHolder.getPermissionsHandler();
		}

		// Validating state of sender
		if (dataHolder == null || permissionHandler == null) {
			if (!setDefaultWorldHandler(sender))
				return true;
		}
		
		// If validating players and no player was found by that name.
		if ((GroupManager.getGMConfig().isToggleValidate()) && ((match = validatePlayer(args[0], sender)) == null)) {
			return false;
		}
		
		// If we have a valid match get the account for the target player
		// else attempt a name search for the account.
		if (match != null) {
			auxUser = dataHolder.getUser(match.toString());
		} else {
			auxUser = dataHolder.getUser(args[0]);
		}
		
		// Find a matching group.
		auxGroup = dataHolder.getGroup(args[1]);
		if (auxGroup == null) {
			sender.sendMessage(ChatColor.RED + String.format(Messages.getString("ERROR_GROUP_DOES_NOT_EXIST"),args[1])); //$NON-NLS-1$
			return false;
		}
		if (auxGroup.isGlobal()) {
			sender.sendMessage(ChatColor.RED + Messages.getString("ERROR_PLAYERS_NOT_MEMBERS_OF_GG")); //$NON-NLS-1$
			return false;
		}

		// Validating permissions
		if (!isConsole && !isOpOverride && (senderGroup != null ? permissionHandler.inGroup(auxUser.getUUID(), senderGroup.getName()) : false)) {
			sender.sendMessage(ChatColor.RED + Messages.getString("ERROR_SAME_PERMISSIONS_OR_HIGHER")); //$NON-NLS-1$
			return true;
		}
		if (!isConsole && !isOpOverride && (permissionHandler.hasGroupInInheritance(auxGroup, senderGroup.getName()))) {
			sender.sendMessage(ChatColor.RED + Messages.getString("ERROR_DESTINATION_HIGHER_THAN_YOURS")); //$NON-NLS-1$
			return true;
		}
		if (!isConsole && !isOpOverride && (!permissionHandler.inGroup(senderUser.getUUID(), auxUser.getGroupName()) || !permissionHandler.inGroup(senderUser.getUUID(), auxGroup.getName()))) {
			sender.sendMessage(ChatColor.RED + Messages.getString("ERROR_YOU_DO_NOT_INHERIT")); //$NON-NLS-1$
			return true;
		}

		// Seems OK
		auxUser.setGroup(auxGroup);
		if (!sender.hasPermission("groupmanager.notify.other") || (isConsole))
			sender.sendMessage(ChatColor.YELLOW + String.format(Messages.getString("USER_CHANGED_TO_GROUP"), auxUser.getLastName(), auxGroup.getName(), dataHolder.getName())); //$NON-NLS-1$
		
		return true;
	}
	
	@Override
	public @Nullable List<String> tabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
		
		List<String> result = new ArrayList<String>();
		
		/*
		 * Return a TabComplete for users.
		 */
		if (args.length == 1) {

			result = tabCompleteUsers(args[0]);
		}
		
		/*
		 * Populate the second argument of TabComplete with a list of group names.
		 */
		if (args.length == 2) {

			result = tabCompleteGroups(args[1]);
		}
		
		/*
		 * Populate the third argument of TabComplete with a list of valid world roots.
		 */
		if (args.length == 3) {
			result = getWorlds(); 
		}
		
		return result;
	}

}
