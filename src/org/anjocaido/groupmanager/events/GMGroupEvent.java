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
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * @author ElgarL
 * 
 */
public class GMGroupEvent extends Event {

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

	protected Group group;

	protected String groupName;

	protected Action action;

	public GMGroupEvent(Group group, Action action) {

		super();

		this.group = group;
		this.action = action;
		this.groupName = group.getName();
	}

	public GMGroupEvent(String groupName, Action action) {

		super();

		this.groupName = groupName;
		this.action = action;
	}

	public Action getAction() {

		return this.action;
	}

	public Group getGroup() {

		return group;
	}

	public String getGroupName() {

		return groupName;
	}

	public enum Action {
		GROUP_PERMISSIONS_CHANGED, GROUP_INHERITANCE_CHANGED, GROUP_INFO_CHANGED, GROUP_ADDED, GROUP_REMOVED,
	}

	public void schedule(final GMGroupEvent event) {

		synchronized (GroupManager.getGMEventHandler().getServer()) {
			if (GroupManager.getGMEventHandler().getServer().getScheduler().scheduleSyncDelayedTask(GroupManager.getGMEventHandler().getPlugin(), () -> GroupManager.getGMEventHandler().getServer().getPluginManager().callEvent(event), 1) == -1)
				GroupManager.logger.warning("Could not schedule GM Event.");
		}
	}
}