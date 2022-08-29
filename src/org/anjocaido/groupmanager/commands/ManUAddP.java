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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.localization.Messages;
import org.anjocaido.groupmanager.utils.PermissionCheckResult;
import org.anjocaido.groupmanager.utils.Tasks;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author ElgarL
 *
 */
public class ManUAddP extends BaseCommand {

	/**
	 * 
	 */
	public ManUAddP() {}

	@Override
	protected boolean parseCommand(@NotNull String[] args) {

		// Validating state of sender
		if (dataHolder == null || permissionHandler == null) {
			if (!setDefaultWorldHandler(sender))
				return true;
		}
		// Validating arguments
		if (args.length < 2) {
			sender.sendMessage(ChatColor.RED + Messages.getString("ERROR_REVIEW_ARGUMENTS") + Messages.getString("MANUADDP_SYNTAX")); //$NON-NLS-1$ //$NON-NLS-2$
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

		// Validating your permissions
		if (!isConsole && !isOpOverride && (senderGroup != null && permissionHandler.inGroup(auxUser.getUUID(), senderGroup.getName()))) {
			sender.sendMessage(ChatColor.RED + Messages.getString("ERROR_SAME_GROUP_OR_HIGHER")); //$NON-NLS-1$
			return true;
		}

		for (int i = 1; i < args.length; i++)
		{
			auxString = args[i].replace("'", ""); //$NON-NLS-1$ //$NON-NLS-2$
			String[] split;
			Instant timed = null;
			Long period = null;
			/*
			 * check for a timed permission
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
					sender.sendMessage(ChatColor.RED + String.format(Messages.getString("ERROR_INVALID_DURATION"), auxString) + Messages.getString("MANUADDP_SYNTAX")); //$NON-NLS-1$ //$NON-NLS-2$
					continue;
				}
			}

			permissionResult = permissionHandler.checkFullUserPermission(senderUser, auxString);
			if (!isConsole && !isOpOverride && (permissionResult.resultType.equals(PermissionCheckResult.Type.NOTFOUND) || permissionResult.resultType.equals(PermissionCheckResult.Type.NEGATION))) {
				sender.sendMessage(ChatColor.RED + String.format(Messages.getString("ERROR_CANT_ADD_PERMISSION"), auxString)); //$NON-NLS-1$
				continue;
			}
			// Validating permissions of user
			permissionResult = permissionHandler.checkUserOnlyPermission(auxUser, auxString);
			if (checkPermissionExists(sender, auxString, permissionResult, "user")) //$NON-NLS-1$
			{
				continue;
			}
			// Seems Ok

			// Auto saves.
			if (period != null) {
				auxUser.addTimedPermission(auxString, timed.getEpochSecond());
				sender.sendMessage(ChatColor.YELLOW + String.format(Messages.getString("ADDED_PERMISSION_TO_USER_TIMED"), auxString, auxUser.getLastName(), period)); //$NON-NLS-1$

			} else {
				auxUser.addPermission(auxString);
				sender.sendMessage(ChatColor.YELLOW + String.format(Messages.getString("ADDED_PERMISSION_TO_USER"), auxString, auxUser.getLastName())); //$NON-NLS-1$
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
		switch (args.length) {

		case 0:
			break;

		case 1:
			result = tabCompleteUsers(args[0]);
			break;

		default:
			result = getPermissionNodes(args[args.length - 1]);
			break;
		}

		return result;
	}

}
