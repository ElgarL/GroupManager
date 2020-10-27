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

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.localization.Messages;
import org.anjocaido.groupmanager.utils.Tasks;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ElgarL
 *
 */
public class ManUAddSub extends BaseCommand {

	/**
	 * 
	 */
	public ManUAddSub() {}

	@Override
	protected boolean parseCommand(@NotNull String[] args) {

		// Validating state of sender
		if (dataHolder == null || permissionHandler == null) {
			if (!setDefaultWorldHandler(sender))
				return true;
		}
		// Validating arguments
		if (args.length >= 2) {
			sender.sendMessage(ChatColor.RED + Messages.getString("ERROR_REVIEW_ARGUMENTS") + Messages.getString("MANUADDSUB_SYNTAX")); //$NON-NLS-1$ //$NON-NLS-2$
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
		if (!isConsole && !isOpOverride && (senderGroup != null ? permissionHandler.inGroup(auxUser.getUUID(), senderGroup.getName()) : false)) {
			sender.sendMessage(ChatColor.RED + Messages.getString("ERROR_SAME_PERMISSIONS_OR_HIGHER")); //$NON-NLS-1$
			return true;
		}

		for (int i = 1; i < args.length; i++)
		{
			auxString = args[i].replace("'", ""); //$NON-NLS-1$ //$NON-NLS-2$
			String[] split = null;
			Instant timed = null;
			Long period = null;
			/*
			 * check for a timed subgroup
			 */
			if (auxString.contains("|")) { //$NON-NLS-1$
				split = auxString.split("\\|"); //$NON-NLS-1$

				try {
					period = Tasks.parsePeriod(split[1]);
				} catch (Exception e) {
					period = 0L;
				}
				timed = Instant.now().plus(period, ChronoUnit.MINUTES);
				auxString = split[0];

				if (period == 0) {
					sender.sendMessage(ChatColor.RED + String.format(Messages.getString("ERROR_INVALID_DURATION"), auxString) + Messages.getString("MANUADDSUB_SYNTAX")); //$NON-NLS-1$ //$NON-NLS-2$
					continue;
				}

				auxGroup = dataHolder.getGroup(auxString);
				if (auxGroup == null) {
					sender.sendMessage(ChatColor.RED + String.format(Messages.getString("ERROR_GROUP_DOES_NOT_EXIST"),auxString)); //$NON-NLS-1$
					continue;
				}

				if (!isConsole && !isOpOverride && (permissionHandler.hasGroupInInheritance(auxGroup, senderGroup.getName()))) {
					sender.sendMessage(ChatColor.RED + Messages.getString("ERROR_SUBGROUP_HIGHER_THAN_YOURS")); //$NON-NLS-1$
					continue;
				}

				if (!isConsole && !isOpOverride && (!permissionHandler.inGroup(senderUser.getUUID(), auxUser.getGroupName()) || !permissionHandler.inGroup(senderUser.getUUID(), auxGroup.getName()))) {
					sender.sendMessage(ChatColor.RED + Messages.getString("ERROR_YOU_DO_NOT_INHERIT")); //$NON-NLS-1$
					continue;
				}

				// Seems OK
				if (period != null) {
					if (auxUser.addTimedSubGroup(auxGroup, timed.getEpochSecond())) {
						sender.sendMessage(ChatColor.YELLOW + String.format(Messages.getString("SUBGROUP_ADDED_USER_TIMED"), auxGroup.getName(), auxUser.getLastName(), period)); //$NON-NLS-1$
					} else {
						sender.sendMessage(ChatColor.YELLOW + String.format(Messages.getString("ERROR_SUBGROUP_ALREADY_AVAILABLE"), auxGroup.getName(), auxUser.getLastName())); //$NON-NLS-1$
					}
				}
			} else {
				/*
				 * Normal subgroup
				 */
				auxGroup = dataHolder.getGroup(auxString);
				if (auxGroup == null) {
					sender.sendMessage(ChatColor.RED + String.format(Messages.getString("ERROR_GROUP_DOES_NOT_EXIST"),auxString)); //$NON-NLS-1$
					continue;
				}

				if (!isConsole && !isOpOverride && (permissionHandler.hasGroupInInheritance(auxGroup, senderGroup.getName()))) {
					sender.sendMessage(ChatColor.RED + Messages.getString("ERROR_SUBGROUP_HIGHER_THAN_YOURS")); //$NON-NLS-1$
					continue;
				}

				if (!isConsole && !isOpOverride && (!permissionHandler.inGroup(senderUser.getUUID(), auxUser.getGroupName()) || !permissionHandler.inGroup(senderUser.getUUID(), auxGroup.getName()))) {
					sender.sendMessage(ChatColor.RED + Messages.getString("ERROR_YOU_DO_NOT_INHERIT")); //$NON-NLS-1$
					continue;
				}

				// Seems OK
				if (auxUser.addSubGroup(auxGroup)) {
					sender.sendMessage(ChatColor.YELLOW + String.format(Messages.getString("SUBGROUP_ADDED_USER"), auxGroup.getName(), auxUser.getLastName())); //$NON-NLS-1$
				} else {
					sender.sendMessage(ChatColor.YELLOW + String.format(Messages.getString("ERROR_SUBGROUP_ALREADY_AVAILABLE"), auxGroup.getName(), auxUser.getLastName())); //$NON-NLS-1$
				}
			}
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
		
		/*
		 * Populate the second argument of TabComplete with a list of group names.
		 */
		if (args.length >= 2) {

			result = tabCompleteGroups(args[1]);
		}
		
		return result;
	}

}
