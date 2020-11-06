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
public class GroupVariables extends Variables implements Cloneable {

	private final Group owner;

	public GroupVariables(Group owner) {

		super(owner);
		this.owner = owner;
		addVar("prefix", "");
		addVar("suffix", "");
		addVar("build", false);
	}

	public GroupVariables(Group owner, Map<String, Object> varList) {

		super(owner);
		variables.clear();
		variables.putAll(varList);
		if (variables.get("prefix") == null) {
			variables.put("prefix", "");
			owner.flagAsChanged();
		}
		//thisGrp.prefix = infoNode.get("prefix").toString();

		if (variables.get("suffix") == null) {
			variables.put("suffix", "");
			owner.flagAsChanged();
		}
		//thisGrp.suffix = infoNode.get("suffix").toString();

		if (variables.get("build") == null) {
			variables.put("build", false);
			owner.flagAsChanged();
		}
		this.owner = owner;
	}

	/**
	 * A clone of all vars here.
	 * 
	 * @return GroupVariables clone
	 */
	protected GroupVariables clone(Group newOwner) {

		GroupVariables clone = new GroupVariables(newOwner);
		synchronized(variables) {
		for (String key : variables.keySet()) {
			clone.variables.put(key, variables.get(key));
		}
		}
		newOwner.flagAsChanged();
		return clone;
	}

	/**
	 * Remove a var from the list
	 * 
	 * @param name
	 */
	@Override
	public void removeVar(String name) {

		try {
			this.variables.remove(name);
		} catch (Exception ignored) {
		}
		switch (name) {
			case "prefix":
				addVar("prefix", "");
				break;
			case "suffix":
				addVar("suffix", "");
				break;
			case "build":
				addVar("build", false);
				break;
		}
		owner.flagAsChanged();
	}

	/**
	 * @return the owner
	 */
	@Override
	public Group getOwner() {

		return owner;
	}
}
