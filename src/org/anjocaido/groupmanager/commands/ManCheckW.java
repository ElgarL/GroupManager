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
import org.anjocaido.groupmanager.dataholder.OverloadedWorldHolder;
import org.anjocaido.groupmanager.localization.Messages;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * @author ElgarL
 *
 */
public class ManCheckW extends BaseCommand implements TabCompleter {

	/**
	 * 
	 */
	public ManCheckW() {}

	@Override
	protected boolean parseCommand(@NotNull String[] args) {

		if (args.length < 1) {
			sender.sendMessage(ChatColor.RED + Messages.getString("ERROR_REVIEW_ARGUMENTS") + Messages.getString("MANCHECKW_SYNTAX")); //$NON-NLS-1$ //$NON-NLS-2$
			sender.sendMessage(ChatColor.YELLOW + Messages.getString("WORLDS_AVAILABLE"));
			
			ArrayList<OverloadedWorldHolder> worlds = plugin.getWorldsHolder().allWorldsDataList();
			auxString = ""; //$NON-NLS-1$
			for (int i = 0; i < worlds.size(); i++) {
				auxString += worlds.get(i).getName();
				if ((i + 1) < worlds.size()) {
					auxString += ", "; //$NON-NLS-1$
				}
			}
			sender.sendMessage(ChatColor.YELLOW + auxString);
			return false;
		}
		
		auxString = ""; //$NON-NLS-1$
		for (int i = 0; i < args.length; i++) {
			if (args[i] == null) {
				GroupManager.logger.warning(String.format(Messages.getString("ERROR_BUKKIT_INVALID_ARGUMENTS"), this.getClass().getSimpleName(), args.length)); //$NON-NLS-1$
				return false;
			}
			auxString += args[i];
			if (i < (args.length - 1)) {
				auxString += " "; //$NON-NLS-1$
			}
		}
		dataHolder = plugin.getWorldsHolder().getWorldData(auxString);
		
		sender.sendMessage(ChatColor.YELLOW + String.format(Messages.getString("ERROR_BUKKIT_INVALID_ARGUMENTS"), dataHolder.getName())); //$NON-NLS-1$
		sender.sendMessage(ChatColor.YELLOW + Messages.getString("WORLD_USING_DATA_FILES")); //$NON-NLS-1$
		sender.sendMessage(ChatColor.YELLOW + Messages.getString("GROUPS") + ChatColor.GREEN + dataHolder.getGroupsFile().getAbsolutePath()); //$NON-NLS-1$
		sender.sendMessage(ChatColor.YELLOW + Messages.getString("USERS") + ChatColor.GREEN + dataHolder.getUsersFile().getAbsolutePath()); //$NON-NLS-1$

		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
		
		parseSender(sender, alias);
		
		/*
		 * Populate the first argument of TabComplete with a list of valid world roots.
		 */
		if (args.length == 1) {
			return getWorlds();
		}
		
		return new ArrayList<String>();
	}

}
