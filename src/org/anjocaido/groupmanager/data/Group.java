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

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.dataholder.WorldDataHolder;
import org.anjocaido.groupmanager.events.GMGroupEvent.Action;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 
 * @author gabrielcouto/ElgarL
 */
public class Group extends DataUnit implements Cloneable, Comparable<Group> {

	@Override
	public int compareTo(Group o) {

		return this.getName().compareTo( o.getName() );
	}

	/**
	 * The group it inherits DIRECTLY!
	 */
	private List<String> inherits = Collections.unmodifiableList(Collections.<String>emptyList());
	/**
	 * This one holds the fields in INFO node.
	 * like prefix = 'c'
	 * or build = false
	 */
	private GroupVariables variables = new GroupVariables(this);

	/**
	 * Constructor for individual World Groups.
	 * 
	 * @param name
	 */
	public Group(WorldDataHolder source, String name) {

		super(source, name);
	}

	/**
	 * Constructor for Global Groups.
	 * 
	 * @param name
	 */
	public Group(String name) {

		super(name);
	}
	
	/**
	 * @return the name
	 */
	public String getName() {

		return this.getUUID();
	}

	/**
	 * Is this a GlobalGroup
	 * 
	 * @return true if this is a global group
	 */
	public boolean isGlobal() {

		return (getDataSource() == null);
	}

	/**
	 * Clone this group
	 * 
	 * @return a clone of this group
	 */
	@Override
	public Group clone() {

		Group clone;

		if (isGlobal()) {
			clone = new Group(this.getName());
		} else {
			clone = new Group(getDataSource(), this.getName());
			clone.inherits = this.getInherits().isEmpty() ?
					Collections.unmodifiableList(Collections.<String>emptyList())
					: Collections.unmodifiableList(new ArrayList<String>(this.getInherits()));
		}

		for (String perm : this.getPermissionList()) {
			clone.addPermission(perm);
		}
		for (Entry<String, Long> perm : this.getTimedPermissions().entrySet()) {
			clone.addTimedPermission(perm.getKey(), perm.getValue());
		}
		clone.variables = ((GroupVariables) variables).clone(clone);
		//clone.flagAsChanged();
		return clone;
	}

	/**
	 * Use this to deliver a group from a different dataSource to another
	 * 
	 * @param dataSource
	 * @return Null or Clone
	 */
	public Group clone(WorldDataHolder dataSource) {

		if (dataSource.groupExists(this.getName())) {
			return null;
		}

		Group clone = dataSource.createGroup(this.getName());

		// Don't add inheritance for GlobalGroups
		if (!isGlobal()) {
			clone.inherits = this.getInherits().isEmpty() ?
					Collections.unmodifiableList(Collections.<String>emptyList())
					: Collections.unmodifiableList(new ArrayList<String>(this.getInherits()));
		}
		for (String perm : this.getPermissionList()) {
			clone.addPermission(perm);
		}
		for (Entry<String, Long> perm : this.getTimedPermissions().entrySet()) {
			clone.addTimedPermission(perm.getKey(), perm.getValue());
		}
		clone.variables = variables.clone(clone);
		clone.flagAsChanged(); //use this to make the new dataSource save the new group
		return clone;
	}

	/**
	 * an unmodifiable list of inherits list
	 * You can't manage the list by here
	 * Lol... version 0.6 had a problem because this.
	 * 
	 * @return the inherits
	 */
	public List<String> getInherits() {
		return inherits;
	}

	/**
	 * @param inherit the inherits to set
	 */
	public void addInherits(Group inherit) {

		if (!isGlobal()) {
			if (!this.getDataSource().groupExists(inherit.getName())) {
				getDataSource().addGroup(inherit);
			}
			if (!inherits.contains(inherit.getName().toLowerCase())) {
				List<String> clone = new ArrayList<String>(inherits);
				clone.add(inherit.getName().toLowerCase());
				inherits = Collections.unmodifiableList(clone);
			}
			flagAsChanged();
			if (GroupManager.isLoaded()) {
				GroupManager.getBukkitPermissions().updateAllPlayers();
				GroupManager.getGMEventHandler().callEvent(this, Action.GROUP_INHERITANCE_CHANGED);
			}
		}
	}

	public boolean removeInherits(String inherit) {

		if (!isGlobal()) {
			if (this.inherits.contains(inherit.toLowerCase())) {
				List<String> clone = new ArrayList<String>(inherits);
				clone.remove(inherit.toLowerCase());
				inherits = Collections.unmodifiableList(clone);
				flagAsChanged();
				GroupManager.getGMEventHandler().callEvent(this, Action.GROUP_INHERITANCE_CHANGED);
				return true;
			}
		}
		return false;
	}

	/**
	 * @return the variables
	 */
	public GroupVariables getVariables() {

		return variables;
	}

	/**
	 * 
	 * @param varList
	 */
	public void setVariables(Map<String, Object> varList) {

		if (!isGlobal()) {
			GroupVariables temp = new GroupVariables(this, varList);
			variables.clearVars();
			for (String key : temp.getVarKeyList()) {
				variables.addVar(key, temp.getVarObject(key));
			}
			flagAsChanged();
			if (GroupManager.isLoaded()) {
				GroupManager.getBukkitPermissions().updateAllPlayers();
				GroupManager.getGMEventHandler().callEvent(this, Action.GROUP_INFO_CHANGED);
			}
		}
	}
}
