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
import org.anjocaido.groupmanager.data.User;
import org.anjocaido.groupmanager.localization.Messages;
import org.anjocaido.groupmanager.utils.PermissionCheckResult;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author ElgarL
 *
 */
public class ManUCheckP extends BaseCommand {

	/**
	 * 
	 */
	public ManUCheckP() {}

	@Override
	protected boolean parseCommand(@NotNull String[] args) {

		// Validating state of sender
		if (dataHolder == null || permissionHandler == null) {
			if (!setDefaultWorldHandler(sender))
				return true;
		}
		// Validating arguments
		if (args.length != 2) {
			sender.sendMessage(ChatColor.RED + Messages.getString("ERROR_REVIEW_ARGUMENTS") + Messages.getString("MANUCHECKP_SYNTAX")); //$NON-NLS-1$ //$NON-NLS-2$
			return true;
		}

		auxString = args[1].replace("'", ""); //$NON-NLS-1$ //$NON-NLS-2$

		if ((GroupManager.getGMConfig().isToggleValidate()) && ((match = validatePlayer(args[0], sender)) == null)) {
			return false;
		}

		if (match != null) {
			auxUser = dataHolder.getUser(match.toString());
		} else {
			auxUser = dataHolder.getUser(args[0]);
		}
		targetPlayer = plugin.getServer().getPlayer(auxUser.getLastName());
		// Validating permission
		permissionResult = permissionHandler.checkFullGMPermission(auxUser, auxString, false);

		if (permissionResult.resultType.equals(PermissionCheckResult.Type.NOTFOUND)) {
			// No permissions found in GM so fall through and check Bukkit.
			sender.sendMessage(ChatColor.YELLOW + Messages.getString("ERROR_USER_NO_ACCESS_PERMISSION")); //$NON-NLS-1$

		} else {
			// This permission was found in groupmanager.
			if (permissionResult.owner instanceof User) {
				if (permissionResult.resultType.equals(PermissionCheckResult.Type.NEGATION)) {
					sender.sendMessage(ChatColor.YELLOW + Messages.getString("USER_HAS_NEGATION_DIRECT")); //$NON-NLS-1$
				} else if (permissionResult.resultType.equals(PermissionCheckResult.Type.EXCEPTION)) {
					sender.sendMessage(ChatColor.YELLOW + Messages.getString("USER_HAS_EXCEPTION_DIRECT")); //$NON-NLS-1$
				} else {
					sender.sendMessage(ChatColor.YELLOW + Messages.getString("USER_HAS_PERMISSION_DIRECT")); //$NON-NLS-1$
				}
				sender.sendMessage(ChatColor.YELLOW + String.format(Messages.getString("PERMISSION_NODE"), permissionResult.accessLevel)); //$NON-NLS-1$
			} else if (permissionResult.owner instanceof Group) {
				if (permissionResult.resultType.equals(PermissionCheckResult.Type.NEGATION)) {
					sender.sendMessage(ChatColor.YELLOW + Messages.getString("USER_INHERITS_NEGATION_FROM_GROUP") + permissionResult.owner.getLastName()); //$NON-NLS-1$
				} else if (permissionResult.resultType.equals(PermissionCheckResult.Type.EXCEPTION)) {
					sender.sendMessage(ChatColor.YELLOW + Messages.getString("USER_INHERITS_EXCEPTION_FROM_GROUP") + permissionResult.owner.getLastName()); //$NON-NLS-1$
				} else {
					sender.sendMessage(ChatColor.YELLOW + Messages.getString("USER_INHERITS_PERMISSION_FROM_GROUP") + permissionResult.owner.getLastName()); //$NON-NLS-1$
				}
				sender.sendMessage(ChatColor.YELLOW + String.format(Messages.getString("PERMISSION_NODE"), permissionResult.accessLevel)); //$NON-NLS-1$
			}
		}

		// superperms
		if (targetPlayer != null) {
			sender.sendMessage(ChatColor.YELLOW + Messages.getString("SUPER_PERMS_REPORTS") + targetPlayer.hasPermission(args[1]) + ((!targetPlayer.hasPermission(args[1]) && targetPlayer.isPermissionSet(args[1])) ? Messages.getString("NEGATED"): "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
