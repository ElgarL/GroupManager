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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

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
public class ManSave extends BaseCommand {

	/**
	 * 
	 */
	public ManSave() {}

	@Override
	protected boolean parseCommand(@NotNull String[] args) {

		AtomicBoolean forced = new AtomicBoolean(false);

		if ((args.length == 1) && (args[0].equalsIgnoreCase("force"))) //$NON-NLS-1$
			forced.set(true);

		CompletableFuture.runAsync(() -> {
			try {
				/*
				 * Obtain a lock so we can save.
				 */
				plugin.getSaveLock().lock();
				GroupManager.setLoaded(false);

				plugin.getWorldsHolder().saveChanges(forced.get());
				sender.sendMessage(ChatColor.YELLOW + Messages.getString("GroupManager.REFRESHED")); //$NON-NLS-1$

			} catch (IllegalStateException ex) {
				sender.sendMessage(ChatColor.RED + ex.getMessage());

			} finally {
				// Release lock.
				if(plugin.getSaveLock().isHeldByCurrentThread()) {
					GroupManager.setLoaded(true);
					plugin.getSaveLock().unlock();
				}
			}
		});

		return true;
	}

	@Override
	public @Nullable List<String> tabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {

		if (args.length == 1) {

			return Collections.singletonList("force"); //$NON-NLS-1$
		}
		return null;
	}

}
