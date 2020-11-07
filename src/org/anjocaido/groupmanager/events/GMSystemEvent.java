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
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * @author ElgarL
 * 
 */
public class GMSystemEvent extends Event {

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

	protected Action action;

	public GMSystemEvent(Action action) {

		super();
		
		this.action = action;
	}

	public Action getAction() {

		return this.action;
	}

	public enum Action {
		RELOADED, SAVED, DEFAULT_GROUP_CHANGED, VALIDATE_TOGGLE,
	}

	public void schedule(final GMSystemEvent event) {

		synchronized (GroupManager.getGMEventHandler().getServer()) {
			if (GroupManager.getGMEventHandler().getServer().getScheduler().scheduleSyncDelayedTask(GroupManager.getGMEventHandler().getPlugin(), () -> GroupManager.getGMEventHandler().getServer().getPluginManager().callEvent(event), 1) == -1)
				GroupManager.logger.warning("Could not schedule GM Event.");
		}
	}
}