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
package org.anjocaido.groupmanager;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.anjocaido.groupmanager.data.Group;
import org.anjocaido.groupmanager.events.GMGroupEvent;
import org.anjocaido.groupmanager.utils.PermissionCheckResult;

/**
 * @author ElgarL
 * 
 */
public class GlobalGroups {

	private final Map<String, Group> groups = Collections.synchronizedMap(new HashMap<>());

	private long timeStampGroups = 0;
	private boolean haveGroupsChanged = false;
	private File GlobalGroupsFile;

	private final GroupManager plugin;

	public GlobalGroups(GroupManager plugin) {

		this.plugin = plugin;
	}

	public void load() {

		plugin.getWorldsHolder().getDataSource().loadGlobalGroups(this);
	}

	/**
	 * @return the timeStampGroups
	 */
	public long getTimeStampGroups() {

		return timeStampGroups;
	}

	/**
	 * @param timeStampGroups the timeStampGroups to set
	 */
	public void setTimeStampGroups(long timeStampGroups) {

		this.timeStampGroups = timeStampGroups;
	}

	/**
	 * @param haveGroupsChanged
	 *            the haveGroupsChanged to set
	 */
	public void setGroupsChanged(boolean haveGroupsChanged) {

		this.haveGroupsChanged = haveGroupsChanged;
	}

	/**
	 * Adds a group, or replaces an existing one.
	 * 
	 * @param groupToAdd
	 */
	public void addGroup(Group groupToAdd) {

		// Create a new group if it already exists
		if (hasGroup(groupToAdd.getName())) {
			groupToAdd = groupToAdd.clone();
			removeGroup(groupToAdd.getName());
		}

		newGroup(groupToAdd);
		haveGroupsChanged = true;
		if (GroupManager.isLoaded())
			GroupManager.getGMEventHandler().callEvent(groupToAdd, GMGroupEvent.Action.GROUP_ADDED);
	}

	/**
	 * Creates a new group if it doesn't already exist.
	 * 
	 * @param newGroup
	 * @return group created, or null if a group already exists
	 */
	public Group newGroup(Group newGroup) {

		// Push a new group
		if (!groups.containsKey(newGroup.getName().toLowerCase())) {
			groups.put(newGroup.getName().toLowerCase(), newGroup);
			this.setGroupsChanged(true);
			return newGroup;
		}
		return null;
	}

	/**
	 * Delete a group if it exist.
	 * 
	 * @param groupName
	 */
	public boolean removeGroup(String groupName) {

		// Push a new group
		if (groups.containsKey(groupName.toLowerCase())) {
			groups.remove(groupName.toLowerCase());
			this.setGroupsChanged(true);
			if (GroupManager.isLoaded())
				GroupManager.getGMEventHandler().callEvent(groupName.toLowerCase(), GMGroupEvent.Action.GROUP_REMOVED);
			return true;
		}
		return false;
	}

	/**
	 * Returns true if the Global Group exists in the globalgroups.yml
	 * 
	 * @param groupName
	 * @return true if the group exists
	 */
	public boolean hasGroup(String groupName) {

		return groups.containsKey(groupName.toLowerCase());
	}

	/**
	 * Returns true if the group has the correct permission node.
	 * 
	 * @param groupName
	 * @param permissionNode
	 * @return true if node exists
	 */
	public boolean hasPermission(String groupName, String permissionNode) {
		return hasGroup(groupName) && groups.get(groupName.toLowerCase()).hasSamePermissionNode(permissionNode);

	}

	/**
	 * Returns a PermissionCheckResult of the permission node for the group to
	 * be tested against.
	 * 
	 * @param groupName
	 * @param permissionNode
	 * @return PermissionCheckResult object
	 */
	public PermissionCheckResult checkPermission(String groupName, String permissionNode) {

		PermissionCheckResult result = new PermissionCheckResult();
		result.askedPermission = permissionNode;
		result.resultType = PermissionCheckResult.Type.NOTFOUND;

		if (!hasGroup(groupName))
			return result;

		Group tempGroup = groups.get(groupName.toLowerCase());

		if (tempGroup.hasSamePermissionNode(permissionNode))
			result.resultType = PermissionCheckResult.Type.FOUND;
		if (tempGroup.hasSamePermissionNode("-" + permissionNode)) //$NON-NLS-1$
			result.resultType = PermissionCheckResult.Type.NEGATION;
		if (tempGroup.hasSamePermissionNode("+" + permissionNode)) //$NON-NLS-1$
			result.resultType = PermissionCheckResult.Type.EXCEPTION;

		return result;
	}

	/**
	 * Returns a List of all permission nodes for this group, null if none
	 * 
	 * @param groupName	the group name to list all permissions from.
	 * @return			List of all group names or null.
	 */
	public List<String> getGroupsPermissions(String groupName) {

		if (!hasGroup(groupName))
			return null;

		return groups.get(groupName.toLowerCase()).getPermissionList();
	}

	/**
	 * Resets GlobalGroups.
	 */
	public void resetGlobalGroups() {
		this.groups.clear();
	}


	/**
	 * Get the Map containing all global groups.
	 * You must synchronize on this map for all iterations.
	 * 
	 * @return the groups
	 */
	public Map<String, Group> getGroups() {

		return groups;
	}

	/**
	 * Get all Groups in an array.
	 * 
	 * @return a collection of the groups
	 */
	public Group[] getGroupList() {
		synchronized(groups) {
			return groups.values().toArray(new Group[0]);
		}
	}

	/**
	 * Returns the Global Group or null if it doesn't exist.
	 * 
	 * @param groupName
	 * @return Group object
	 */
	public Group getGroup(String groupName) {
		return hasGroup(groupName) ? groups.get(groupName.toLowerCase()) : null;

	}

	/**
	 * The file that contains data for this Object
	 * or null if not using flat file.
	 * 
	 * @return the globalGroupsFile
	 */
	public File getGlobalGroupsFile() {

		return GlobalGroupsFile;
	}

	public void setGlobalGroupsFile(File file) {

		GlobalGroupsFile = file;
	}

	/**
	 * @return the haveGroupsChanged
	 */
	public boolean haveGroupsChanged() {

		if (this.haveGroupsChanged) {
			return true;
		}
		synchronized(groups) {
			for (Group g : groups.values()) {
				if (g.isChanged()) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Clear any changed flags on the groups.
	 */
	public void removeGroupsChangedFlag() {

		setGroupsChanged(false);
		synchronized(groups) {
			for (Group g : groups.values()) {
				g.flagAsSaved();
			}
		}
	}
}
