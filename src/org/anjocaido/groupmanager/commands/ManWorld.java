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
			sender.sendMessage(ChatColor.YELLOW + "You have the world '" + dataHolder.getName() + "' in your selection.");
		} else {
			if (dataHolder == null) {
				sender.sendMessage(ChatColor.YELLOW + "There is no world selected. And no world is available now.");
			} else {
				sender.sendMessage(ChatColor.YELLOW + "You don't have a world in your selection..");
				sender.sendMessage(ChatColor.YELLOW + "Working with the direct world where your player is.");
				sender.sendMessage(ChatColor.YELLOW + "Your world now uses permissions of world name: '" + dataHolder.getName() + "' ");
			}
		}

		return true;
	}

}
