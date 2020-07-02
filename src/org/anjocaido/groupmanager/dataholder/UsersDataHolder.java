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
	private boolean haveUsersChanged = false;
	private long timeStampUsers = 0;

	/**
	 * The actual groups holder
	 */
	private final Map<String, User> users = Collections.synchronizedMap(new HashMap<String, User>());

	/**
	 * Constructor
	 */
	protected UsersDataHolder() {

	}

	public void setDataSource(WorldDataHolder dataSource) {

		this.dataSource = dataSource;
		//push this data source to the users, so they pull the correct groups data.
		synchronized(users) {
		for (User user : users.values())
			user.setDataSource(this.dataSource);
		}
	}

	/**
	 * Note: Iteration over this object has to be synchronised!
	 * @return the users
	 */
	public Map<String, User> getUsers() {

		return users;
	}
	
	public WorldDataHolder getDataSource() {
		
		return this.dataSource;
	}

	/**
	 * Resets the Users
	 */
	public void resetUsers() {
		this.users.clear();
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
	 * @return the haveUsersChanged
	 */
	public boolean HaveUsersChanged() {

		return haveUsersChanged;
	}

	/**
	 * @param haveUsersChanged the haveUsersChanged to set
	 */
	public void setUsersChanged(boolean haveUsersChanged) {

		this.haveUsersChanged = haveUsersChanged;
	}

	/**
	 * @return the timeStampUsers
	 */
	public long getTimeStampUsers() {

		return timeStampUsers;
	}

	/**
	 * @param timeStampUsers the timeStampUsers to set
	 */
	public void setTimeStampUsers(long timeStampUsers) {

		this.timeStampUsers = timeStampUsers;
	}

}
