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
package org.anjocaido.groupmanager.data;

import java.util.Map;

/**
 * 
 * @author gabrielcouto
 */
public class UserVariables extends Variables {

	private final User owner;

	public UserVariables(User owner) {

		super(owner);
		this.owner = owner;
	}

	public UserVariables(User owner, Map<? extends String, ?> varList) {

		super(owner);
		this.variables.clear();
		this.variables.putAll(varList);
		this.owner = owner;
	}

	/**
	 * A clone of all vars here.
	 * 
	 * @return UserVariables clone
	 */
	protected UserVariables clone(User newOwner) {

		UserVariables clone = new UserVariables(newOwner);
		synchronized(variables) {
			for (String key : variables.keySet()) {
				clone.variables.put(key, variables.get(key));
			}
		}
		newOwner.flagAsChanged();
		return clone;
	}

	/**
	 * @return the owner
	 */
	@Override
	public User getOwner() {

		return owner;
	}
}
