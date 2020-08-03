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

import org.anjocaido.groupmanager.localization.Messages;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

/**
 * @author ElgarL
 *
 */
public class ManGAdd extends BaseCommand {

	/**
	 * 
	 */
	public ManGAdd() {}

	@Override
	protected boolean parseCommand(@NotNull String[] args) {

		// Validating state of sender
		if (dataHolder == null || permissionHandler == null) {
			if (!setDefaultWorldHandler(sender))
				return true;
		}
		// Validating arguments
		if (args.length != 1) {
			sender.sendMessage(ChatColor.RED + Messages.getString("ERROR_REVIEW_ARGUMENTS") + Messages.getString("MANGADD_SYNTAX")); //$NON-NLS-1$ //$NON-NLS-2$
			return true;
		}
		auxGroup = dataHolder.getGroup(args[0]);
		if (auxGroup != null) {
			sender.sendMessage(ChatColor.RED + String.format(Messages.getString("ERROR_GROUP_ALREADY_EXISTS"), args[0])); //$NON-NLS-1$
			return true;
		}
		// Seems OK
		auxGroup = dataHolder.createGroup(args[0]);
		sender.sendMessage(ChatColor.YELLOW + String.format(Messages.getString("CREATED_GROUP"), auxGroup.getName())); //$NON-NLS-1$

		return true;
	}

}
