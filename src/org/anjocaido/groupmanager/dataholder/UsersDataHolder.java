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
import java.util.SortedMap;
import java.util.TreeMap;

import org.anjocaido.groupmanager.data.User;

/**
 * This container holds all Users loaded from the relevant usersFile.
 * 
 * @author ElgarL
 * 
 */
public class UsersDataHolder {

	private WorldDataHolder dataSource;
	private File usersFile;
	private boolean changed = false;
	private long timeStamp = 0;

	/**
	 * The actual groups holder
	 */
	private final SortedMap<String, User> users = Collections.synchronizedSortedMap(new TreeMap<>());

	/**
	 * Constructor
	 */
	protected UsersDataHolder() {}

	public void setDataSource(WorldDataHolder dataSource) {

		this.dataSource = dataSource;
		// Push this data source to the users, so they pull the correct groups data.
		synchronized(users) {
			for (User user : users.values())
				user.setDataSource(this.dataSource);
		}
	}

	/**
	 * Note: Iteration over this object has to be synchronized!
	 * 
	 * @return the users
	 */
	public SortedMap<String, User> getUsers() {

		return users;
	}

	public WorldDataHolder getDataSource() {

		return this.dataSource;
	}

	/**
	 * Resets the Users
	 */
	void resetUsers() {

		synchronized(users) {
			this.users.clear();
		}
	}

	/**
	 * @return the usersFile
	 */
	public File getUsersFile() {

		return usersFile;
	}

	/**
	 * @param usersFile the usersFile to set
	 */
	public void setUsersFile(File usersFile) {

		this.usersFile = usersFile;
	}

	/**
	 * @return true if Users have changed.
	 */
	public boolean isUsersChanged() {

		return changed;
	}
	
	/**
	 * Flag all users as changed so we can force save to SQL.
	 */
	void setAllChanged() {
		
		setUsersChanged(true);
		
		synchronized(users) {
			users.entrySet().forEach(entry -> entry.getValue().flagAsChanged());
		}
	}

	/**
	 * @param changed the state to set for changed.
	 */
	public void setUsersChanged(boolean changed) {

		this.changed = changed;
	}

	/**
	 * @return the timeStamp
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
