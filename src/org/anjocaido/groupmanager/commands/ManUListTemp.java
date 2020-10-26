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
import org.anjocaido.groupmanager.data.User;
import org.anjocaido.groupmanager.localization.Messages;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;


/**
 * @author ElgarL
 *
 */
public class ManUListTemp extends BaseCommand {

	/**
	 * 
	 */
	public ManUListTemp() {}

	@Override
	protected boolean parseCommand(@NotNull String[] args) {

		// Validating state of sender
		if (dataHolder == null || permissionHandler == null) {
			if (!setDefaultWorldHandler(sender))
				return true;
		}
		// WORKING
		auxString = ""; //$NON-NLS-1$
		ArrayList<User> removeList = new ArrayList<User>();
		int count = 0;
		
		if (GroupManager.getOverloadedUsers().size() > 0)
			for (User u : GroupManager.getOverloadedUsers().get(dataHolder.getName().toLowerCase())) {
				if (!dataHolder.isOverloaded(u.getUUID())) {
					removeList.add(u);
				} else {
					auxString += u.getLastName() + ", "; //$NON-NLS-1$
					count++;
				}
			}

		if (count == 0) {
			sender.sendMessage(ChatColor.YELLOW + Messages.getString("NO_OVERLOAD")); //$NON-NLS-1$
			return true;
		}
		auxString = auxString.substring(0, auxString.lastIndexOf(",")); //$NON-NLS-1$
		if (GroupManager.getOverloadedUsers().get(dataHolder.getName().toLowerCase()) == null) {
			GroupManager.getOverloadedUsers().put(dataHolder.getName().toLowerCase(), new ArrayList<User>());
		}
		GroupManager.getOverloadedUsers().get(dataHolder.getName().toLowerCase()).removeAll(removeList);
		sender.sendMessage(ChatColor.YELLOW + String.format(Messages.getString("OVERLOADED_USERS"), count, ChatColor.WHITE + auxString)); //$NON-NLS-1$

		return true;
	}

}
