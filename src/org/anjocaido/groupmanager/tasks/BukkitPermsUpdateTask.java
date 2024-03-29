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
package org.anjocaido.groupmanager.tasks;

import java.util.logging.Level;

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.localization.Messages;

/*
 * 
 * Created by ElgarL
 */

public class BukkitPermsUpdateTask implements Runnable {

	public BukkitPermsUpdateTask() {

		super();
	}

	@Override
	public void run() {

		// Signal loaded and update BukkitPermissions.
		GroupManager.setLoaded(true);
		GroupManager.getBukkitPermissions().collectPermissions();
		GroupManager.getBukkitPermissions().updateAllPlayers();

		GroupManager.logger.log(Level.INFO, Messages.getString("BukkitPermsUpdateTask.BUKKIT_PERMISSIONS_UPDATED")); //$NON-NLS-1$

	}

}