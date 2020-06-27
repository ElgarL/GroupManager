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
package org.anjocaido.groupmanager.dataholder;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.anjocaido.groupmanager.data.Group;

/**
 * This container holds all Groups loaded from the relevant groupsFile.
 * 
 * @author ElgarL
 * 
 */
public class GroupsDataHolder {

	private WorldDataHolder dataSource;
	private Group defaultGroup = null;
	private File groupsFile;
	private boolean haveGroupsChanged = false;
	private long timeStampGroups = 0;

	/**
	 * The actual groups holder
	 */
	private final Map<String, Group> groups = Collections.synchronizedMap(new HashMap<String, Group>());

	/**
	 * Constructor
	 */
	protected GroupsDataHolder() {

	}

	public void setDataSource(WorldDataHolder dataSource) {

		this.dataSource = dataSource;
		//push this data source to the users, so they pull the correct groups data.
		synchronized(groups) {
		for (Group group : groups.values())
			group.setDataSource(this.dataSource);
		}
	}
	
	public WorldDataHolder getDataSource() {

		return this.dataSource;
	}

	/**
	 * @return the defaultGroup
	 */
	public Group getDefaultGroup() {

		return defaultGroup;
	}

	/**
	 * @param defaultGroup the defaultGroup to set
	 */
	public void setDefaultGroup(Group defaultGroup) {

		this.defaultGroup = defaultGroup;
	}

	/**
	 * Note: Iteration over this object has to be synchronized!
	 * @return the groups
	 */
	public Map<String, Group> getGroups() {

		return groups;
	}

	/**
	 * Resets the Groups
	 */
	public void resetGroups() {
		this.groups.clear();
	}

	/**
	 * @return the groupsFile
	 */
	public File getGroupsFile() {

		return groupsFile;
	}

	/**
	 * @param groupsFile the groupsFile to set
	 */
	public void setGroupsFile(File groupsFile) {

		this.groupsFile = groupsFile;
	}

	/**
	 * @return the haveGroupsChanged
	 */
	public boolean HaveGroupsChanged() {

		return haveGroupsChanged;
	}

	/**
	 * @param haveGroupsChanged the haveGroupsChanged to set
	 */
	public void setGroupsChanged(boolean haveGroupsChanged) {

		this.haveGroupsChanged = haveGroupsChanged;
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

}
