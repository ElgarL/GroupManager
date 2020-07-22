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
import org.anjocaido.groupmanager.data.Group;
import org.anjocaido.groupmanager.localization.Messages;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;


/**
 * @author ElgarL
 *
 */
public class ListGroups extends BaseCommand {

	/**
	 * 
	 */
	public ListGroups() {}

	@Override
	protected boolean parseCommand(@NotNull String[] args) {

		// Validating state of sender
		if (dataHolder == null || permissionHandler == null) {
			if (!setDefaultWorldHandler(sender))
				return true;
		}
		// WORKING
		auxString = ""; //$NON-NLS-1$
		String auxString2 = ""; //$NON-NLS-1$
		for (Group g : dataHolder.getGroupList()) {
			auxString += g.getName() + ", "; //$NON-NLS-1$
		}
		for (Group g : GroupManager.getGlobalGroups().getGroupList()) {
			auxString2 += g.getName() + ", "; //$NON-NLS-1$
		}
		if (auxString.lastIndexOf(",") > 0) { //$NON-NLS-1$
			auxString = auxString.substring(0, auxString.lastIndexOf(",")); //$NON-NLS-1$
		}
		if (auxString2.lastIndexOf(",") > 0) { //$NON-NLS-1$
			auxString2 = auxString2.substring(0, auxString2.lastIndexOf(",")); //$NON-NLS-1$
		}
		sender.sendMessage(ChatColor.YELLOW + Messages.getString("GROUPS_AVAILABLE") + ChatColor.WHITE + auxString); //$NON-NLS-1$
		sender.sendMessage(ChatColor.YELLOW + Messages.getString("GG_AVALIABLE") + ChatColor.WHITE + auxString2); //$NON-NLS-1$

		return true;
	}

}
