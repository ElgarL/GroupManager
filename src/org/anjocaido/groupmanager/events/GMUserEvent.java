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

import java.util.logging.Level;

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.data.User;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * @author ElgarL
 * 
 */
public class GMUserEvent extends Event {

	/**
	 * 
	 */
	private static final HandlerList handlers = new HandlerList();

	@Override
	public @NotNull HandlerList getHandlers() {

		return handlers;
	}

	public static HandlerList getHandlerList() {

		return handlers;
	}

	//////////////////////////////

	protected User user;

	protected String userName;

	protected Action action;

	public GMUserEvent(User user, Action action) {

		super();

		this.user = user;
		this.action = action;
		this.userName = user.getLastName();
	}

	public GMUserEvent(String userName, Action action) {

		super();

		this.userName = userName;
		this.action = action;
	}

	public Action getAction() {

		return this.action;
	}

	public User getUser() {

		return user;
	}

	public String getUserName() {

		return userName;
	}

	public enum Action {
		USER_PERMISSIONS_CHANGED, USER_INHERITANCE_CHANGED, USER_INFO_CHANGED, USER_GROUP_CHANGED, USER_SUBGROUP_CHANGED, USER_ADDED, USER_REMOVED,
	}

	public void schedule(final GMUserEvent event) {

		synchronized (GroupManager.getGMEventHandler().getServer()) {
			if (GroupManager.getGMEventHandler().getServer().getScheduler().scheduleSyncDelayedTask(GroupManager.getGMEventHandler().getPlugin(), () -> GroupManager.getGMEventHandler().getServer().getPluginManager().callEvent(event), 1) == -1)
				GroupManager.logger.log(Level.WARNING, "Could not schedule GM Event.");
		}
	}
}