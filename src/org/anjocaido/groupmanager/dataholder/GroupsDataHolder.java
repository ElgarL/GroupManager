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
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;

import org.anjocaido.groupmanager.data.Group;

/**
 * This container holds all Groups loaded from the relevant groupsFile.
 * 
 * @author ElgarL
 * 
 */
public class GroupsDataHolder {

	private WorldDataHolder dataSource;
	private Group defaultGroup;
	private File groupsFile;
	private boolean changed = false;
	private long timeStamp = 0;

	/**
	 * The actual groups holder
	 */
	private final ConcurrentSkipListMap<String, Group> groups = new ConcurrentSkipListMap<>();

	/**
	 * Constructor
	 */
	protected GroupsDataHolder() {}

	public void setDataSource(WorldDataHolder dataSource) {

		this.dataSource = dataSource;
		//push this data source to the groups, so they pull the correct groups data.
		for (Iterator<Entry<String, Group>> iterator = groups.entrySet().iterator(); iterator.hasNext();)
			iterator.next().getValue().setDataSource(this.dataSource);
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
	 * 
	 * @return the groups
	 */
	public ConcurrentSkipListMap<String, Group> getGroups() {

		return groups;
	}

	/**
	 * Resets the Groups
	 */
	void resetGroups() {
		
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
	 * @return true if Groups have changed.
	 */
	public boolean isGroupsChanged() {

		return changed;
	}
	
	/**
	 * Flag all users as changed so we can force save to SQL.
	 */
	void setAllChanged() {
		
		setGroupsChanged(true);
		for (Iterator<Entry<String, Group>> iterator = groups.entrySet().iterator(); iterator.hasNext();)
			iterator.next().getValue().flagAsChanged();
	}

	/**
	 * @param changed the state to set for changed.
	 */
	public void setGroupsChanged(boolean changed) {

		this.changed = changed;
	}

	/**
	 * @return the time stamp.
	 */
	public long getTimeStamp() {

		return timeStamp;
	}

	/**
	 * @param timeStamp the time stamp to set.
	 */
	public void setTimeStamp(long timeStamp) {

		this.timeStamp = timeStamp;
	}

}
