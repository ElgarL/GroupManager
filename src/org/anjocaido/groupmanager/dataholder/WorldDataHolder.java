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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.data.Group;
import org.anjocaido.groupmanager.data.User;
import org.anjocaido.groupmanager.events.GMGroupEvent;
import org.anjocaido.groupmanager.events.GMSystemEvent;
import org.anjocaido.groupmanager.events.GMUserEvent;
import org.anjocaido.groupmanager.events.GMUserEvent.Action;
import org.anjocaido.groupmanager.permissions.AnjoPermissionsHandler;

/**
 * One instance of this should exist per world/mirror it contains all functions
 * to manage these data sets and points to the relevant users and groups
 * objects.
 *
 * @author gabrielcouto, ElgarL
 */
public class WorldDataHolder {

	/**
	 * World name
	 */
	protected String name;
	/**
	 * The actual groups holder
	 */
	protected GroupsDataHolder groups = new GroupsDataHolder();
	/**
	 * The actual users holder
	 */
	protected UsersDataHolder users = new UsersDataHolder();

	/**
	 * List of UUID's associated with this user name.
	 */
	protected static Map<String, Set<String>> nameToUUIDLookup = new TreeMap<String, Set<String>>();
	/**
	 *
	 */
	protected AnjoPermissionsHandler permissionsHandler;

	/**
	 * Prevent direct instantiation
	 *
	 * @param worldName
	 */
	public WorldDataHolder(String worldName) {

		name = worldName;
	}

	/**
	 * The main constructor for a new WorldDataHolder
	 *
	 * @param worldName
	 * @param groups
	 * @param users
	 */
	public WorldDataHolder(String worldName, GroupsDataHolder groups, UsersDataHolder users) {

		this.name = worldName;
		this.groups = groups;
		this.users = users;
	}

	/**
	 * update the dataSource to point to this object.
	 * <p>
	 * This should be called whenever a set of world data is fetched.
	 */
	public void updateDataSource() {

		this.groups.setDataSource(this);
		this.users.setDataSource(this);
	}

	/**
	 * Search for a user. If it doesn't exist, create a new one with default group.
	 * <p>
	 * If this is called passing a player name with mantogglevalidate off
	 * it can return the wrong user object (offline/online UUID match).
	 *
	 * @param userId the UUID String or name of the user
	 * @return class that manage that user permission
	 */
	public User getUser(String userId) {

		if (getUsers().containsKey(userId.toLowerCase())) {
			return getUsers().get(userId.toLowerCase());
		}

		// Legacy name matching
		if ((userId.length() < 36) && nameToUUIDLookup.containsKey(userId.toLowerCase())) {

			// Search for a name to UUID match
			for (String uid : getUUIDLookup(userId.toLowerCase())) {

				User user = getUsers().get(uid.toLowerCase());

				if ((user != null) && user.getLastName().equalsIgnoreCase(userId)) {
					return user;
				}
			}
		}

		if (!nameToUUIDLookup.containsKey(userId.toLowerCase())) {
			GroupManager.logger.fine("ERROR: No lookup for: " + userId);
		}

		// No user account found so create a new one.
		User newUser = createUser(userId);
		return newUser;
	}

	/**
	 * *** Internal GM use only ***
	 * This is called when a player joins to update/add their UUID.
	 *
	 * @param uUID        the player objects UUID.
	 * @param currentName the name they have just logged in with.
	 * @return the user object for this player.
	 */
	public User getUser(String uUID, String currentName) {

		// Check for a UUID account
		User user = getUsers().get(uUID.toLowerCase());

		if (user != null) {

			GroupManager.logger.fine("User record found for UUID: " + uUID + ":" + currentName);
			user.setLastName(currentName);
			/*
			 * Check for a non UUID name match as
			 * its possible some plugin (worldedit)
			 * performed a command permission lookup
			 * by name, before we got to see a login
			 * event (to grab a UUID). This would
			 * force create a user .
			 */
			if (getUsers().containsKey(currentName.toLowerCase()))
				getUsers().remove(currentName.toLowerCase());

			return user;
		}

		// Search for a LastName match
		user = getUsers().get(currentName.toLowerCase());

		if ((user != null) && user.getLastName().equalsIgnoreCase(currentName) && user.getUUID().equalsIgnoreCase(user.getLastName())) {

			// Clone this user so we can set it's uUID
			User usr = user.clone(uUID, currentName);

			// Delete it and replace with the new clone.
			this.removeUser(user.getUUID().toLowerCase());
			this.addUser(usr);

			GroupManager.logger.fine("Updating User record for UUID: " + uUID + ":" + currentName);

			return getUsers().get(uUID.toLowerCase());
		}

		if (user != null) {
			GroupManager.logger.fine("User record found but UUID mismatch for: " + currentName);
		}

		// No user account found so create a new one.
		User newUser = createUser(uUID.toLowerCase());
		newUser.setLastName(currentName);

		GroupManager.logger.fine("New User record created: " + uUID + ":" + currentName);

		return newUser;
	}

	/**
	 * Add a user to the list. If it already exists, overwrite the old.
	 *
	 * @param theUser the user you want to add to the permission list
	 */
	public void addUser(User theUser) {

		if (theUser.getDataSource() != this) {
			theUser = theUser.clone(this);
		}
		if (theUser == null) {
			return;
		}
		if ((theUser.getGroup() == null)) {
			theUser.setGroup(groups.getDefaultGroup());
		}
		removeUser(theUser.getUUID().toLowerCase());
		getUsers().put(theUser.getUUID().toLowerCase(), theUser);

		// Store for name to UUID lookups.
		//putUUIDLookup(theUser.getLastName(), theUser.getUUID().toLowerCase());

		setUsersChanged(true);
		if (GroupManager.isLoaded())
			GroupManager.getGMEventHandler().callEvent(theUser, Action.USER_ADDED);
	}

	/**
	 * Removes the user from the list. (he might become a default user)
	 *
	 * @param userId the UUID or username for the user to remove
	 * @return true if it had something to remove
	 */
	public boolean removeUser(String userId) {

		if (getUsers().containsKey(userId.toLowerCase())) {

			User user = getUser(userId.toLowerCase());

			// Remove the name to UUID lookup for this user object.
			removeUUIDLookup(user.getLastName().toLowerCase(), user.getUUID());

			getUsers().remove(userId.toLowerCase());

			setUsersChanged(true);

			if (GroupManager.isLoaded())
				GroupManager.getGMEventHandler().callEvent(userId, GMUserEvent.Action.USER_REMOVED);

			return true;
		}
		return false;
	}

	/**
	 * @param userId
	 * @return true if we have data for this player.
	 */
	public boolean isUserDeclared(String userId) {

		return getUsers().containsKey(userId.toLowerCase());
	}

	/**
	 * Change the default group of the file.
	 *
	 * @param group the group you want make default.
	 */
	public void setDefaultGroup(Group group) {

		if (!getGroups().containsKey(group.getName().toLowerCase()) || (group.getDataSource() != this)) {
			addGroup(group);
		}
		groups.setDefaultGroup(getGroup(group.getName()));
		setGroupsChanged(true);
		if (GroupManager.isLoaded())
			GroupManager.getGMEventHandler().callEvent(GMSystemEvent.Action.DEFAULT_GROUP_CHANGED);
	}

	/**
	 * Returns the default group of the file
	 *
	 * @return the default group
	 */
	public Group getDefaultGroup() {

		return groups.getDefaultGroup();
	}

	/**
	 * Returns a group of the given name
	 *
	 * @param groupName the name of the group
	 * @return a group if it is found. null if not found.
	 */
	public Group getGroup(String groupName) {

		if (groupName.toLowerCase().startsWith("g:"))
			return GroupManager.getGlobalGroups().getGroup(groupName);
		else
			return getGroups().get(groupName.toLowerCase());
	}

	/**
	 * Check if a group exists. Its the same of getGroup, but check if it is
	 * null.
	 *
	 * @param groupName the name of the group
	 * @return true if exists. false if not.
	 */
	public boolean groupExists(String groupName) {

		if (groupName.toLowerCase().startsWith("g:"))
			return GroupManager.getGlobalGroups().hasGroup(groupName);
		else
			return getGroups().containsKey(groupName.toLowerCase());
	}

	/**
	 * Add a group to the list
	 *
	 * @param groupToAdd
	 */
	public void addGroup(Group groupToAdd) {

		if (groupToAdd.getName().toLowerCase().startsWith("g:")) {
			GroupManager.getGlobalGroups().addGroup(groupToAdd);
			GroupManager.getGMEventHandler().callEvent(groupToAdd, GMGroupEvent.Action.GROUP_ADDED);
			return;
		}

		if (groupToAdd.getDataSource() != this) {
			groupToAdd = groupToAdd.clone(this);
		}
		removeGroup(groupToAdd.getName());
		getGroups().put(groupToAdd.getName().toLowerCase(), groupToAdd);
		setGroupsChanged(true);
		if (GroupManager.isLoaded())
			GroupManager.getGMEventHandler().callEvent(groupToAdd, GMGroupEvent.Action.GROUP_ADDED);
	}

	/**
	 * Remove the group from the list
	 *
	 * @param groupName
	 * @return true if something was removed. false the group was default or
	 * non-existent
	 */
	public boolean removeGroup(String groupName) {

		if (groupName.toLowerCase().startsWith("g:")) {
			return GroupManager.getGlobalGroups().removeGroup(groupName);
		}

		if (getDefaultGroup() != null && groupName.equalsIgnoreCase(getDefaultGroup().getName())) {
			return false;
		}
		if (getGroups().containsKey(groupName.toLowerCase())) {
			getGroups().remove(groupName.toLowerCase());
			setGroupsChanged(true);
			if (GroupManager.isLoaded())
				GroupManager.getGMEventHandler().callEvent(groupName.toLowerCase(), GMGroupEvent.Action.GROUP_REMOVED);
			return true;
		}
		return false;
	}

	/**
	 * Creates a new User with the given name and adds it to this holder.
	 *
	 * @param userId the UUID or username you want
	 * @return null if user already exists. or new User
	 */
	public User createUser(String userId) {

		if (getUsers().containsKey(userId.toLowerCase())) {
			return null;
		}
		User newUser = new User(this, userId);
		newUser.setGroup(groups.getDefaultGroup(), false);
		addUser(newUser);
		setUsersChanged(true);
		return newUser;
	}

	/**
	 * Creates a new Group with the given name and adds it to this holder
	 *
	 * @param groupName the groupname you want
	 * @return null if group already exists. or new Group
	 */
	public Group createGroup(String groupName) {

		if (groupName.toLowerCase().startsWith("g:")) {
			Group newGroup = new Group(groupName);
			return GroupManager.getGlobalGroups().newGroup(newGroup);
		}

		if (getGroups().containsKey(groupName.toLowerCase())) {
			return null;
		}

		Group newGroup = new Group(this, groupName);
		addGroup(newGroup);
		setGroupsChanged(true);
		return newGroup;
	}

	/**
	 * @return a collection of the groups
	 */
	public Collection<Group> getGroupList() {

		synchronized (getGroups()) {
			return new ArrayList<Group>(getGroups().values());
		}
	}

	/**
	 * @return a collection of the users
	 */
	public Collection<User> getUserList() {

		synchronized (getUsers()) {
			return new ArrayList<User>(getUsers().values());
		}
	}

	/**
	 * @return the permissionsHandler
	 */
	public AnjoPermissionsHandler getPermissionsHandler() {

		if (permissionsHandler == null) {
			permissionsHandler = new AnjoPermissionsHandler(this);
		}
		return permissionsHandler;
	}

	/**
	 * @param haveUsersChanged the haveUsersChanged to set
	 */
	public void setUsersChanged(boolean haveUsersChanged) {

		users.setUsersChanged(haveUsersChanged);
	}

	/**
	 *
	 * @return true if any user data has changed
	 */
	public boolean haveUsersChanged() {

		if (users.HaveUsersChanged()) {
			return true;
		}
		synchronized (users) {
			for (User u : users.getUsers().values()) {
				if (u.isChanged()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * @param setGroupsChanged the haveGroupsChanged to set
	 */
	public void setGroupsChanged(boolean setGroupsChanged) {

		groups.setGroupsChanged(setGroupsChanged);
	}

	/**
	 *
	 * @return true if any group data has changed.
	 */
	public boolean haveGroupsChanged() {

		if (groups.HaveGroupsChanged()) {
			return true;
		}
		synchronized (groups) {
			for (Group g : groups.getGroups().values()) {
				if (g.isChanged()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 *
	 */
	public void removeUsersChangedFlag() {

		setUsersChanged(false);
		synchronized (users) {
			for (User u : getUsers().values()) {
				u.flagAsSaved();
			}
		}
	}

	/**
	 *
	 */
	public void removeGroupsChangedFlag() {

		setGroupsChanged(false);
		synchronized (groups) {
			for (Group g : getGroups().values()) {
				g.flagAsSaved();
			}
		}
	}

	/**
	 * Searches all groups and users for expired permissions
	 * Removes those perms and flags the object for saving.
	 */
	public boolean purgeTimedPermissions() {

		boolean expired = false;

		synchronized (groups) {

			for (Group group : getGroups().values()) {
				if (group.removeExpired()) {
					setGroupsChanged(true);
					expired = true;
				}
			}
		}

		synchronized (users) {

			for (User user : getUsers().values()) {
				if (user.removeExpired()) {
					setUsersChanged(true);
					expired = true;
				}
			}
		}

		return expired;
	}

	/**
	 * @return the usersFile
	 */
	public File getUsersFile() {

		return users.getUsersFile();
	}

	/**
	 * @param file the usersFile to set
	 */
	public void setUsersFile(File file) {

		users.setUsersFile(file);
	}

	/**
	 * @return the groupsFile
	 */
	public File getGroupsFile() {

		return groups.getGroupsFile();
	}

	/**
	 * @param file the groupsFile to set
	 */
	public void setGroupsFile(File file) {

		groups.setGroupsFile(file);
	}

	/**
	 * @return the name
	 */
	public String getName() {

		return name;
	}

	/**
	 * Resets Groups.
	 */
	public void resetGroups() {

		// setDefaultGroup(null);
		groups.resetGroups();
	}

	/**
	 * Resets Users
	 */
	public void resetUsers() {

		users.resetUsers();
		this.clearUUIDLookup();
	}

	/**
	 * Note: Iteration over this object has to be synchronized!
	 *
	 * @return the groups
	 */
	public Map<String, Group> getGroups() {

		return groups.getGroups();
	}

	/**
	 * Note: Iteration over this object has to be synchronized!
	 *
	 * @return the users
	 */
	public Map<String, User> getUsers() {

		return users.getUsers();
	}

	/**
	 * @return the groups
	 */
	public GroupsDataHolder getGroupsObject() {

		return groups;
	}

	/**
	 * @param groupsDataHolder the GroupsDataHolder to set
	 */
	public void setGroupsObject(GroupsDataHolder groupsDataHolder) {

		groups = groupsDataHolder;
	}

	/**
	 * @return the users
	 */
	public UsersDataHolder getUsersObject() {

		return users;
	}

	/**
	 * @param usersDataHolder the UsersDataHolder to set
	 */
	public void setUsersObject(UsersDataHolder usersDataHolder) {

		users = usersDataHolder;
	}

	/**
	 * @return the timeStampGroups
	 */
	public long getTimeStampGroups() {

		return groups.getTimeStampGroups();
	}

	/**
	 * @return the timeStampUsers
	 */
	public long getTimeStampUsers() {

		return users.getTimeStampUsers();
	}

	/**
	 * @param timeStampGroups the timeStampGroups to set
	 */
	public void setTimeStampGroups(long timeStampGroups) {

		groups.setTimeStampGroups(timeStampGroups);
	}

	/**
	 * @param timeStampUsers the timeStampUsers to set
	 */
	public void setTimeStampUsers(long timeStampUsers) {

		users.setTimeStampUsers(timeStampUsers);
	}

	public void setTimeStamps() {

		if (getGroupsFile() != null)
			setTimeStampGroups(getGroupsFile().lastModified());
		if (getUsersFile() != null)
			setTimeStampUsers(getUsersFile().lastModified());
	}

	/** Name to UUID lookups **/

	/**
	 * Add a new name to UUID lookup.
	 *
	 * @param name the User name key to index on.
	 * @param UUID the User object UUID (same as name if there is no UUID).
	 */
	public void putUUIDLookup(String name, String UUID) {

		Set<String> lookup = getUUIDLookup(name.toLowerCase());

		if (lookup == null)
			lookup = new TreeSet<String>();

		lookup.add(UUID);

		nameToUUIDLookup.put(name.toLowerCase(), lookup);
	}

	/**
	 * Delete a name lookup.
	 * Allows for multiple UUID's assigned to a single name (offline/online)
	 *
	 * @param name
	 * @param UUID
	 */
	public void removeUUIDLookup(String name, String UUID) {

		if (nameToUUIDLookup.containsKey(name.toLowerCase())) {

			Set<String> lookup = getUUIDLookup(name.toLowerCase());

			lookup.remove(UUID);

			if (lookup.isEmpty()) {
				nameToUUIDLookup.remove(name.toLowerCase());
				return;
			}

			nameToUUIDLookup.put(name.toLowerCase(), lookup);
		}
	}

	/**
	 *
	 * @param name
	 * @return a Set of strings containing the User objects UUID (or name if they don't have a UUID)
	 */
	public Set<String> getUUIDLookup(String name) {

		return nameToUUIDLookup.get(name.toLowerCase());
	}

	/**
	 * Reset the UUID Lookup cache
	 */
	protected void clearUUIDLookup() {

		nameToUUIDLookup.clear();
	}
}
