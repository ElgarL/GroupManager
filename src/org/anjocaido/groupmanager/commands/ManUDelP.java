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
import java.util.UUID;

import org.anjocaido.groupmanager.GroupManager;
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
public class ManUDelP extends BaseCommand {

	/**
	 * 
	 */
	public ManUDelP() {}

	@Override
	protected boolean parseCommand(@NotNull String[] args) {

		// Validating state of sender
		if (dataHolder == null || permissionHandler == null) {
			if (!setDefaultWorldHandler(sender))
				return true;
		}
		// Validating arguments
		if (args.length < 2) {
			sender.sendMessage(ChatColor.RED + Messages.getString("ERROR_REVIEW_ARGUMENTS") + Messages.getString("MANUDELP_SYNTAX")); //$NON-NLS-1$ //$NON-NLS-2$
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
		
		for (int i = 1; i < args.length; i++)
		{
			auxString = args[i].replace("'", ""); //$NON-NLS-1$ //$NON-NLS-2$
		
			if (!isConsole && !isOpOverride && (senderGroup != null && permissionHandler.inGroup(auxUser.getUUID(), senderGroup.getName()))) {
				sender.sendMessage(ChatColor.RED + Messages.getString("ERROR_SAME_GROUP_OR_HIGHER")); //$NON-NLS-1$
				continue;
			}
			// Validating your permissions
			permissionResult = permissionHandler.checkFullUserPermission(senderUser, auxString);
			if (!isConsole && !isOpOverride && (permissionResult.resultType.equals(PermissionCheckResult.Type.NOTFOUND) || permissionResult.resultType.equals(PermissionCheckResult.Type.NEGATION))) {
				sender.sendMessage(ChatColor.RED + String.format(Messages.getString("ERROR_CANT_REMOVE_PERMISSION"), auxString)); //$NON-NLS-1$
				continue;
			}
			// Validating permissions of user
			permissionResult = permissionHandler.checkUserOnlyPermission(auxUser, auxString);
			if (permissionResult.resultType.equals(PermissionCheckResult.Type.NOTFOUND)) {
				sender.sendMessage(ChatColor.RED + String.format(Messages.getString("ERROR_USER_NO_ACCESS_PERMISSION_DIRECT"), auxString)); //$NON-NLS-1$
				continue;
			}
			if (!auxUser.hasSamePermissionNode(auxString)) {
				sender.sendMessage(ChatColor.RED + Messages.getString("ERROR_NO_MATCHING_PERMISSION_NODE")); //$NON-NLS-1$
				sender.sendMessage(ChatColor.RED + String.format(Messages.getString("POSSIBLE_MATCH"), permissionResult.accessLevel)); //$NON-NLS-1$
				continue;
			}
			auxUser.removePermission(auxString);
			sender.sendMessage(ChatColor.YELLOW + String.format(Messages.getString("REMOVED_PERMISSION_FROM_PLAYER"), auxString, auxUser.getLastName())); //$NON-NLS-1$
		}
		// Seems OK

		// If the player is online, this will create new data for the user.
		if (auxUser.getUUID() != null) {
			targetPlayer = plugin.getServer().getPlayer(UUID.fromString(auxUser.getUUID()));
			if (targetPlayer != null)
				GroupManager.getBukkitPermissions().updatePermissions(targetPlayer);
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
		
		if (args.length >= 2) {
			if ((GroupManager.getGMConfig().isToggleValidate()) && ((match = validatePlayer(args[0], sender)) == null)) {
				return null;
			}

			if (match != null) {
				auxUser = dataHolder.getUser(match.toString());
			} else {
				auxUser = dataHolder.getUser(args[0]);
			}

			return auxUser.getPermissionList();

		}
		
		return result;
	}

}
