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
import org.anjocaido.groupmanager.localization.Messages;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;


/**
 * @author ElgarL
 *
 */
public class ManWorld extends BaseCommand {

	/**
	 * 
	 */
	public ManWorld() {}

	@Override
	protected boolean parseCommand(@NotNull String[] args) {

		auxString = GroupManager.getSelectedWorlds().get(sender.getName());
		
		if (auxString != null) {
			sender.sendMessage(ChatColor.YELLOW + String.format(Messages.getString("YOU_HAVE_SELECTED_WORLD"), dataHolder.getName()));
		} else {
			if (dataHolder == null) {
				sender.sendMessage(ChatColor.YELLOW + Messages.getString("NO_WORLD_AVAILABLE"));
			} else {
				sender.sendMessage(ChatColor.YELLOW + Messages.getString("NO_WORLD_SELECTED"));
				sender.sendMessage(ChatColor.YELLOW + Messages.getString("USING_WORLD"));
				sender.sendMessage(ChatColor.YELLOW + String.format(Messages.getString("USING_PERMISSIONS_OF_WORLD"), dataHolder.getName()));
			}
		}

		return true;
	}

}
