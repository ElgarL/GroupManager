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

		if (GroupManager.isLoaded() && !plugin.getWorldsHolder().isInList(worldName)) {
			GroupManager.logger.info("New world detected...");
			GroupManager.logger.info("Creating data for: " + worldName);
			
			if (plugin.getWorldsHolder().isWorldKnown("all_unnamed_worlds")) {
				
				String usersMirror = plugin.getWorldsHolder().getMirrorsUser().get("all_unnamed_worlds");
				String groupsMirror = plugin.getWorldsHolder().getMirrorsGroup().get("all_unnamed_worlds");
				
				if (usersMirror != null)
					plugin.getWorldsHolder().getMirrorsUser().put(worldName.toLowerCase(), usersMirror);
				
				if (groupsMirror != null)
					plugin.getWorldsHolder().getMirrorsGroup().put(worldName.toLowerCase(), groupsMirror);
				
			}
			
			plugin.getWorldsHolder().setupWorldFolder(worldName);
			plugin.getWorldsHolder().loadWorld(worldName);
			
			
			if (plugin.getWorldsHolder().isInList(worldName)) {
				GroupManager.logger.info("Don't forget to configure/mirror this world in config.yml.");
			} else
				GroupManager.logger.severe("Failed to configure this world.");
		}
	}
}