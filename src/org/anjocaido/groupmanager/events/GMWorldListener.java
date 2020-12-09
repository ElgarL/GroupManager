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
package org.anjocaido.groupmanager.events;

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.localization.Messages;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;

/**
 * @author ElgarL
 * 
 *         Handle new world creation from other plugins
 * 
 */
public class GMWorldListener implements Listener {

	private final GroupManager plugin;

	public GMWorldListener(GroupManager instance) {

		plugin = instance;
		registerEvents();
	}

	private void registerEvents() {

		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onWorldInit(WorldInitEvent event) {

		String worldName = event.getWorld().getName();

		if (GroupManager.isLoaded() && !plugin.getWorldsHolder().isWorldKnown(worldName)) {
			GroupManager.logger.info(Messages.getString("GMWorldListener.DETECTED_NEW_WORLD")); //$NON-NLS-1$
			GroupManager.logger.info(Messages.getString("GMWorldListener.CREATING_DATA") + worldName); //$NON-NLS-1$

			if (plugin.getWorldsHolder().isParentWorld("all_unnamed_worlds")) { //$NON-NLS-1$

				String usersMirror = plugin.getWorldsHolder().getUsersMirror("all_unnamed_worlds"); //$NON-NLS-1$
				String groupsMirror = plugin.getWorldsHolder().getGroupsMirror("all_unnamed_worlds"); //$NON-NLS-1$

				if (usersMirror != null)
					plugin.getWorldsHolder().putUsersMirror(worldName, usersMirror);

				if (groupsMirror != null)
					plugin.getWorldsHolder().putGroupsMirror(worldName, groupsMirror);

			}

			plugin.getWorldsHolder().getDataSource().init(worldName);
			plugin.getWorldsHolder().getDataSource().loadWorld(worldName, false);


			if (plugin.getWorldsHolder().isWorldKnown(worldName)) {
				GroupManager.logger.info(Messages.getString("GMWorldListener.CONFIGURE_NEW_WORLD")); //$NON-NLS-1$
			} else
				GroupManager.logger.severe(Messages.getString("GMWorldListener.ERROR_UNRECOGNISED_WORLD")); //$NON-NLS-1$
		}
	}
}