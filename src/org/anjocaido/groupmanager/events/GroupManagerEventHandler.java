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
import org.anjocaido.groupmanager.data.Group;
import org.anjocaido.groupmanager.data.User;
import org.bukkit.Server;

/**
 * @author ElgarL
 * 
 *         Handles all Event generation.
 * 
 */
public class GroupManagerEventHandler {

	private final Server server;
	private final GroupManager plugin;


	public GroupManagerEventHandler(GroupManager plugin) {

		this.plugin = plugin;
		this.server = plugin.getServer();

	}

	private void callEvent(GMGroupEvent event) {

		try { event.schedule(event); } catch (Exception ignored) {}
	}

	private void callEvent(GMUserEvent event) {

		try { event.schedule(event); } catch (Exception ignored) {}
	}

	private void callEvent(GMSystemEvent event) {

		try { event.schedule(event); } catch (Exception ignored) {}
	}

	public void callEvent(Group group, GMGroupEvent.Action action) {

		callEvent(new GMGroupEvent(group, action));
	}

	public void callEvent(String groupName, GMGroupEvent.Action action) {

		callEvent(new GMGroupEvent(groupName, action));
	}

	public void callEvent(User user, GMUserEvent.Action action) {

		callEvent(new GMUserEvent(user, action));
	}

	public void callEvent(String userName, GMUserEvent.Action action) {

		callEvent(new GMUserEvent(userName, action));
	}

	public void callEvent(GMSystemEvent.Action action) {

		callEvent(new GMSystemEvent(action));
	}

	/**
	 * @return the plugin
	 */
	public GroupManager getPlugin() {

		return plugin;
	}

	/**
	 * @return the server
	 */
	public Server getServer() {

		return server;
	}
}
