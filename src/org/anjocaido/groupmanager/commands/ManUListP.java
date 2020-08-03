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

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.localization.Messages;
import org.anjocaido.groupmanager.utils.BukkitWrapper;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author ElgarL
 *
 */
public class ManUListP extends BaseCommand {

	/**
	 * 
	 */
	public ManUListP() {}

	@Override
	protected boolean parseCommand(@NotNull String[] args) {

		// Validating state of sender
		if (dataHolder == null || permissionHandler == null) {
			if (!setDefaultWorldHandler(sender))
				return true;
		}
		// Validating arguments
		if ((args.length == 0) || (args.length > 2)) {
			sender.sendMessage(ChatColor.RED + Messages.getString("ERROR_REVIEW_ARGUMENTS") + Messages.getString("MANULISTP_SYNTAX")); //$NON-NLS-1$ //$NON-NLS-2$
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
		// Validating permission
		// Seems OK
		auxString = ""; //$NON-NLS-1$
		for (String perm : auxUser.getAllPermissionList()) {
			auxString += perm + ", "; //$NON-NLS-1$
		}
		if (auxString.lastIndexOf(",") > 0) { //$NON-NLS-1$
			auxString = auxString.substring(0, auxString.lastIndexOf(",")); //$NON-NLS-1$
			sender.sendMessage(ChatColor.YELLOW + String.format(Messages.getString("USER_HAS_PERMISSIONS"), auxUser.getLastName(), ChatColor.WHITE + auxString)); //$NON-NLS-1$
			sender.sendMessage(ChatColor.YELLOW + String.format(Messages.getString("AND_ALL_PERMISSIONS_GROUPS"), auxUser.getGroupName())); //$NON-NLS-1$
			auxString = "";
			for (String subGroup : auxUser.subGroupListStringCopy()) {
				auxString += subGroup + ", ";
			}
			if (auxString.lastIndexOf(",") > 0) {
				auxString = auxString.substring(0, auxString.lastIndexOf(","));
				sender.sendMessage(ChatColor.YELLOW + String.format(Messages.getString("AND_ALL_PERMISSIONS_SUBGROUPS"), auxString)); //$NON-NLS-1$
			}
		} else {
			sender.sendMessage(ChatColor.YELLOW + String.format(Messages.getString("USER_NO_SPECIFIC_PERMISSIONS"), auxUser.getLastName())); //$NON-NLS-1$
			sender.sendMessage(ChatColor.YELLOW + String.format(Messages.getString("AND_ALL_PERMISSIONS_GROUPS"), auxUser.getGroupName())); //$NON-NLS-1$
			
			auxString = ""; //$NON-NLS-1$
			for (String subGroup : auxUser.subGroupListStringCopy()) {
				auxString += subGroup + ", "; //$NON-NLS-1$
			}
			if (auxString.lastIndexOf(",") > 0) { //$NON-NLS-1$
				auxString = auxString.substring(0, auxString.lastIndexOf(","));
				sender.sendMessage(ChatColor.YELLOW + String.format(Messages.getString("AND_ALL_PERMISSIONS_SUBGROUPS"), auxString)); //$NON-NLS-1$
			}
		}

		// bukkit perms
		if ((args.length == 2) && (args[1].equalsIgnoreCase("+"))) {
			targetPlayer = BukkitWrapper.getInstance().getPlayer(auxUser.getLastName());
			if (targetPlayer != null) {
				sender.sendMessage(ChatColor.YELLOW + Messages.getString("SUPER_PERMS_REPORTS")); //$NON-NLS-1$
				for (String line : GroupManager.getBukkitPermissions().listPerms(targetPlayer))
					sender.sendMessage(ChatColor.YELLOW + line);

			}
		}

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
		 * Optional + to display Superperms values for an online player.
		 */
		if (args.length == 2) {
			
			return Arrays.asList("+");
		}
		
		return result;
	}

}
