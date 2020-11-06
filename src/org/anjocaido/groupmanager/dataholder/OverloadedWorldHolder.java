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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.anjocaido.groupmanager.data.User;

/**
 * 
 * @author gabrielcouto
 */
public class OverloadedWorldHolder extends WorldDataHolder {

	/**
     *
     */
	protected final Map<String, User> overloadedUsers = Collections.synchronizedMap(new HashMap<>());

	/**
	 * 
	 * @param ph
	 */
	public OverloadedWorldHolder(WorldDataHolder ph) {

		super(ph.getName());
		this.setGroupsFile(ph.getGroupsFile());
		this.setUsersFile(ph.getUsersFile());
		this.groups = ph.groups;
		this.users = ph.users;
	}

	/**
	 * 
	 * @return user object or a new user if none exists.
	 */
	@Override
	public User getUser(String userId) {

		//OVERLOADED CODE
		String userNameLowered = userId.toLowerCase();
		if (overloadedUsers.containsKey(userNameLowered)) {
			return overloadedUsers.get(userNameLowered);
		}
		//END CODE

		return super.getUser(userId);
	}

	/**
	 * 
	 * @param theUser
	 */
	@Override
	public void addUser(User theUser) {

		if (theUser.getDataSource() != this) {
			theUser = theUser.clone(this);
		}
		if (theUser == null) {
			return;
		}
		if ((theUser.getGroup() == null) || (!getGroups().containsKey(theUser.getGroupName().toLowerCase()))) {
			theUser.setGroup(getDefaultGroup());
		}
		//OVERLOADED CODE
		if (overloadedUsers.containsKey(theUser.getUUID().toLowerCase())) {
			overloadedUsers.remove(theUser.getUUID().toLowerCase());
			overloadedUsers.put(theUser.getUUID().toLowerCase(), theUser);
			return;
		}
		//END CODE
		removeUser(theUser.getUUID());
		getUsers().put(theUser.getUUID().toLowerCase(), theUser);
		setUsersChanged(true);
	}

	/**
	 * 
	 * @param userId
	 * @return true if removed/false if not found.
	 */
	@Override
	public boolean removeUser(String userId) {

		//OVERLOADED CODE
		if (overloadedUsers.containsKey(userId.toLowerCase())) {
			overloadedUsers.remove(userId.toLowerCase());
			return true;
		}
		//END CODE
		if (getUsers().containsKey(userId.toLowerCase())) {
			getUsers().remove(userId.toLowerCase());
			setUsersChanged(true);
			return true;
		}
		return false;
	}

	@Override
	public boolean removeGroup(String groupName) {

		if (groupName.equals(getDefaultGroup().getName())) {
			return false;
		}
		synchronized(getGroups()) {
		for (String key : getGroups().keySet()) {
			if (groupName.equalsIgnoreCase(key)) {
				getGroups().remove(key);
				synchronized(getUsers()) {
				for (String userKey : getUsers().keySet()) {
					User user = getUsers().get(userKey);
					if (user.getGroupName().equalsIgnoreCase(key)) {
						user.setGroup(getDefaultGroup());
					}

				}
				}
				//OVERLOADED CODE
				synchronized(overloadedUsers) {
				for (String userKey : overloadedUsers.keySet()) {
					User user = overloadedUsers.get(userKey);
					if (user.getGroupName().equalsIgnoreCase(key)) {
						user.setGroup(getDefaultGroup());
					}

				}
				}
				//END OVERLOAD
				setGroupsChanged(true);
				return true;
			}
		}
		}
		return false;
	}

	/**
	 * 
	 * @return Collection of all users
	 */
	@Override
	public Collection<User> getUserList() {

		Collection<User> overloadedList = new ArrayList<>();
		synchronized(getUsers()) {
		Collection<User> normalList = getUsers().values();
		for (User u : normalList) {
			overloadedList.add(overloadedUsers.getOrDefault(u.getUUID().toLowerCase(), u));
		}
		}
		return overloadedList;
	}

	/**
	 * 
	 * @param userId
	 * @return true if user is overloaded.
	 */
	public boolean isOverloaded(String userId) {

		return overloadedUsers.containsKey(userId.toLowerCase());
	}

	/**
	 * 
	 * @param userId
	 */
	public void overloadUser(String userId) {

		if (!isOverloaded(userId)) {
			User theUser = getUser(userId);
			theUser = theUser.clone();
			overloadedUsers.remove(theUser.getUUID().toLowerCase());
			overloadedUsers.put(theUser.getUUID().toLowerCase(), theUser);
		}
	}

	/**
	 * 
	 * @param userId
	 */
	public void removeOverload(String userId) {

		User theUser = getUser(userId);
		overloadedUsers.remove(theUser.getUUID().toLowerCase());
	}

	/**
	 * Gets the user in normal state. Surpassing the overload state.
	 * It doesn't affect permissions. But it enables plugins change the
	 * actual user permissions even in overload mode.
	 * 
	 * @param userId
	 * @return user object
	 */
	public User surpassOverload(String userId) {

		if (!isOverloaded(userId)) {
			return getUser(userId);
		}
		if (getUsers().containsKey(userId.toLowerCase())) {
			return getUsers().get(userId.toLowerCase());
		}
		return createUser(userId);
	}
}