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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.dataholder.WorldDataHolder;
import org.anjocaido.groupmanager.events.GMGroupEvent.Action;

/**
 * 
 * @author gabrielcouto/ElgarL
 */
public class Group extends DataUnit implements Cloneable {

	/**
	 * The groups it inherits DIRECTLY!
	 */
	private List<String> inherits = new LinkedList<>();

	/**
	 * This holds the fields in INFO node.
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

		boolean loaded = GroupManager.isLoaded();
		GroupManager.setLoaded(false); // Disable so we can push all data without triggering a save.
		
		if (isGlobal()) {
			clone = new Group(this.getName());
		} else {
			clone = new Group(getDataSource(), this.getName());
			clone.inherits = new LinkedList<>(this.getInherits());
		}

		for (Entry<String, Long> perm : this.getPermissions().entrySet()) {
			clone.addTimedPermission(perm.getKey(), perm.getValue());
		}
		clone.variables = variables.clone(clone);

		GroupManager.setLoaded(loaded);	// Restore original state.
		return clone;
	}

	/**
	 * Use this to deliver a group from one dataSource to another
	 * 
	 * @param dataSource
	 * @return null or Clone
	 */
	public Group clone(WorldDataHolder dataSource) {

		if (dataSource.groupExists(this.getName())) {
			return null;
		}

		boolean loaded = GroupManager.isLoaded();
		GroupManager.setLoaded(false); // Disable so we can push all data without triggering a save.
		
		Group clone = dataSource.createGroup(this.getName());

		// Don't add inheritance for GlobalGroups
		if (!isGlobal()) {
			clone.inherits = new LinkedList<>(this.getInherits());
		}

		for (Entry<String, Long> perm : this.getPermissions().entrySet()) {
			clone.addTimedPermission(perm.getKey(), perm.getValue());
		}
		clone.variables = variables.clone(clone);
		clone.flagAsChanged(); //use this to make the new dataSource save the new group
		
		GroupManager.setLoaded(loaded);	// Restore original state.
		return clone;
	}

	/**
	 * An unmodifiable list of inherits list
	 * 
	 * @return the inherits
	 */
	public List<String> getInherits() {

		return Collections.unmodifiableList(inherits);
	}

	/**
	 * @param inherit the inherits to set
	 */
	public void addInherits(Group inherit) {

		if (!isGlobal()) {
			
			boolean loaded = GroupManager.isLoaded();
			GroupManager.setLoaded(false); // Disable so we can push all data without triggering a save.
			
			if (!this.getDataSource().groupExists(inherit.getName())) {
				getDataSource().addGroup(inherit);
			}
			if (!inherits.contains(inherit.getName().toLowerCase())) {
				inherits.add(inherit.getName().toLowerCase());
			}
			flagAsChanged();
			
			GroupManager.setLoaded(loaded);	// Restore original state.

			if (GroupManager.isLoaded()) {
				Runnable notify = new Runnable() {

					@Override
					public void run() {

						GroupManager.getGMEventHandler().callEvent(Group.this, Action.GROUP_INHERITANCE_CHANGED);
					}
				};

				GroupManager.getPlugin(GroupManager.class).getWorldsHolder().refreshData(notify);
			}
		}
	}

	public boolean removeInherits(String inherit) {

		if (!isGlobal()) {
			if (this.inherits.contains(inherit.toLowerCase())) {
				
				boolean loaded = GroupManager.isLoaded();
				GroupManager.setLoaded(false); // Disable so we can push all data without triggering a save.
				
				inherits.remove(inherit.toLowerCase());

				flagAsChanged();
				
				GroupManager.setLoaded(loaded);	// Restore original state.

				if (GroupManager.isLoaded()) {
					Runnable notify = new Runnable() {

						@Override
						public void run() {

							GroupManager.getGMEventHandler().callEvent(Group.this, Action.GROUP_INHERITANCE_CHANGED);
						}
					};

					GroupManager.getPlugin(GroupManager.class).getWorldsHolder().refreshData(notify);
					return true;
				}
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
	 * @param nodeData
	 */
	public void setVariables(Map<?, ?> nodeData) {

		if (!isGlobal()) {
			
			boolean loaded = GroupManager.isLoaded();
			GroupManager.setLoaded(false); // Disable so we can push all vars without triggering a save.
			
			variables.clearVars();
			for (Object key : nodeData.keySet()) {
				variables.addVar((String) key, nodeData.get(key));
			}
			flagAsChanged();
			
			GroupManager.setLoaded(loaded);	// Restore original state.
			
			if (GroupManager.isLoaded()) {
				Runnable notify = new Runnable() {

					@Override
					public void run() {

						GroupManager.getGMEventHandler().callEvent(Group.this, Action.GROUP_INFO_CHANGED);
					}
				};

				GroupManager.getPlugin(GroupManager.class).getWorldsHolder().refreshData(notify);
			}
		}
	}
}
