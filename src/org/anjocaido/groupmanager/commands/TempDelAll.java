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

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.data.User;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;


/**
 * @author ElgarL
 *
 */
public class TempDelAll extends BaseCommand {

	/**
	 * 
	 */
	public TempDelAll() {}

	@Override
	protected boolean parseCommand(@NotNull String[] args) {

		// Validating state of sender
		if (dataHolder == null || permissionHandler == null) {
			if (!setDefaultWorldHandler(sender))
				return true;
		}
		// WORKING
		int count = 0;
		
		for (User u : GroupManager.getOverloadedUsers().get(dataHolder.getName().toLowerCase())) {
			if (dataHolder.isOverloaded(u.getUUID())) {
				dataHolder.removeOverload(u.getUUID());
				count++;
			}
		}
		if (count == 0) {
			sender.sendMessage(ChatColor.YELLOW + "There are no users in overload mode.");
			return true;
		}
		if (GroupManager.getOverloadedUsers().get(dataHolder.getName().toLowerCase()) == null) {
			GroupManager.getOverloadedUsers().put(dataHolder.getName().toLowerCase(), new ArrayList<User>());
		}
		GroupManager.getOverloadedUsers().get(dataHolder.getName().toLowerCase()).clear();
		sender.sendMessage(ChatColor.YELLOW + " " + count + "All users in overload mode are now normal again.");

		return true;
	}

}
