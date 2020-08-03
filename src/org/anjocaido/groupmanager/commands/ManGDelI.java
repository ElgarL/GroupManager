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
public class ManGDelI extends BaseCommand {

	/**
	 * 
	 */
	public ManGDelI() {

	}

	@Override
	protected boolean parseCommand(@NotNull String[] args) {

		// Validating state of sender
		if (dataHolder == null || permissionHandler == null) {
			if (!setDefaultWorldHandler(sender))
				return true;
		}
		// Validating arguments
		if (args.length != 2) {
			sender.sendMessage(ChatColor.RED + Messages.getString("ERROR_REVIEW_ARGUMENTS") + Messages.getString("MANGDELI_SYNTAX")); //$NON-NLS-1$ //$NON-NLS-2$
			return true;
		}
		auxGroup = dataHolder.getGroup(args[0]);
		if (auxGroup == null) {
			sender.sendMessage(ChatColor.RED + String.format(Messages.getString("ERROR_GROUP_DOES_NOT_EXIST"), args[0])); //$NON-NLS-1$
			return true;
		}
		auxGroup2 = dataHolder.getGroup(args[1]);
		if (auxGroup2 == null) {
			sender.sendMessage(ChatColor.RED + String.format(Messages.getString("ERROR_GROUP_DOES_NOT_EXIST"), args[1])); //$NON-NLS-1$
			return true;
		}
		if (auxGroup.isGlobal()) {
			sender.sendMessage(ChatColor.RED + Messages.getString("ERROR_GG_DO_NOT_SUPPORT_INHERITANCE")); //$NON-NLS-1$
			return true;
		}

		// Validating permission
		if (!permissionHandler.hasGroupInInheritance(auxGroup, auxGroup2.getName())) {
			sender.sendMessage(ChatColor.RED + String.format(Messages.getString("ERROR_GROUP_NOT_INHERIT"), auxGroup.getName(), auxGroup2.getName())); //$NON-NLS-1$
			return true;
		}
		if (!auxGroup.getInherits().contains(auxGroup2.getName())) {
			sender.sendMessage(ChatColor.RED + String.format(Messages.getString("ERROR_GROUP_NOT_INHERIT_DIRECT"), auxGroup.getName(), auxGroup2.getName())); //$NON-NLS-1$
			return true;
		}
		// Seems OK
		auxGroup.removeInherits(auxGroup2.getName());
		sender.sendMessage(ChatColor.YELLOW + String.format(Messages.getString("GROUP_REMOVED_INHERITANCE"), auxGroup2.getName(), auxGroup.getName())); //$NON-NLS-1$

		GroupManager.getBukkitPermissions().updateAllPlayers();

		return true;
	}
	
	@Override
	public @Nullable List<String> tabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
		
		List<String> result = new ArrayList<String>();
		/*
		 * Return a TabComplete for base groups.
		 */
		if (args.length == 1) {

			result = tabCompleteGroups(args[0]);
		}
		/*
		 * Return a TabComplete for inherited groups.
		 */
		if (args.length == 2) {
			try {
				result = dataHolder.getGroup(args[0]).getInherits();
			} catch (Exception ex) {
				// Failed to match first group!
			}

		}
		return result;

	}

}
