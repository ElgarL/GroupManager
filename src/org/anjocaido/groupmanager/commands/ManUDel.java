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
public class ManUDel extends BaseCommand {

	/**
	 * 
	 */
	public ManUDel() {}

	@Override
	protected boolean parseCommand(@NotNull String[] args) {

		// Validating state of sender
		if (dataHolder == null || permissionHandler == null) {
			if (!setDefaultWorldHandler(sender))
				return true;
		}
		// Validating arguments
		if (args.length != 1) {
			sender.sendMessage(ChatColor.RED + Messages.getString("ERROR_REVIEW_ARGUMENTS") + Messages.getString("MANUDEL_SYNTAX")); //$NON-NLS-1$ //$NON-NLS-2$
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
		if (!isConsole && !isOpOverride && (senderGroup != null && permissionHandler.inGroup(auxUser.getUUID(), senderGroup.getName()))) {
			sender.sendMessage(ChatColor.RED + Messages.getString("ERROR_SAME_PERMISSIONS_OR_HIGHER")); //$NON-NLS-1$
			return true;
		}
		// Seems OK
		
		boolean loaded = GroupManager.isLoaded();
		GroupManager.setLoaded(false);
		
		dataHolder.removeUser(auxUser.getUUID());
		
		// Restore setting.
		GroupManager.setLoaded(loaded);
		sender.sendMessage(ChatColor.YELLOW + String.format(Messages.getString("USER_CHANGED_TO_DEFAULT"), auxUser.getLastName())); //$NON-NLS-1$

		plugin.getWorldsHolder().refreshData(null);

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
