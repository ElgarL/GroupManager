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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.dataholder.WorldDataHolder;
import org.anjocaido.groupmanager.events.GMUserEvent.Action;
import org.anjocaido.groupmanager.localization.Messages;
import org.anjocaido.groupmanager.utils.BukkitWrapper;
import org.anjocaido.groupmanager.utils.Tasks;
import org.bukkit.entity.Player;

/**
 * 
 * @author gabrielcouto/ElgarL
 */
public class User extends DataUnit implements Cloneable {

	private String group;
	private final List<String> subGroups = Collections.synchronizedList(new ArrayList<>());
	private Map<String, Long> timedSubGroups = Collections.unmodifiableSortedMap(Collections.synchronizedSortedMap(Collections.emptySortedMap()));
	/**
	 * This one holds the fields in INFO node,
	 * like prefix = 'c' or build = false.
	 */
	private UserVariables variables = new UserVariables(this);

	/**
	 * @param name
	 */
	public User(WorldDataHolder source, String name) {

		super(source, name);
		this.group = source.getDefaultGroup().getName();
	}

	@Override
	public User clone() {

		User clone = new User(getDataSource(), this.getLastName());
		clone.group = this.group;

		// Clone all subgroups.
		clone.subGroups.addAll(this.subGroupListCloneStringCopy());

		// Clone permissions
		for (String perm : this.getPermissionList()) {
			clone.addPermission(perm);
		}
		// Clone timed permissions.
		for (Entry<String, Long> perm : this.getTimedPermissions().entrySet()) {
			clone.addTimedPermission(perm.getKey(), perm.getValue());
		}
		// Clone timed subgroups.
		for (Entry<String, Long> grp : this.getTimedSubGroups().entrySet()) {
			clone.addTimedSubGroup(getDataSource().getGroup(grp.getKey()), grp.getValue());
		}

		return clone;
	}

	/**
	 * Use this to deliver a user from one WorldDataHolder to another
	 *
	 * @param dataSource
	 * @return null if given dataSource already contains the same user
	 */
	public User clone(WorldDataHolder dataSource) {

		if (dataSource.isUserDeclared(this.getUUID())) {
			return null;
		}

		User clone = dataSource.createUser(this.getUUID());

		if (dataSource.getGroup(group) == null) {
			clone.setGroup(dataSource.getDefaultGroup());
		} else {
			clone.setGroup(dataSource.getGroup(this.getGroupName()));
		}

		// Clone all subgroups.
		clone.subGroups.addAll(this.subGroupListCloneStringCopy());

		// Clone permissions
		for (String perm : this.getPermissionList()) {
			clone.addPermission(perm);
		}

		// Clone timed permissions.
		for (Entry<String, Long> perm : this.getTimedPermissions().entrySet()) {
			clone.addTimedPermission(perm.getKey(), perm.getValue());
		}

		// Clone timed subgroups.
		for (Entry<String, Long> grp : this.getTimedSubGroups().entrySet()) {
			clone.addTimedSubGroup(getDataSource().getGroup(grp.getKey()), grp.getValue());
		}

		clone.variables = this.variables.clone(this);
		clone.flagAsChanged();
		return clone;
	}

	public User clone(String uUID, String CurrentName) {

		User clone = this.getDataSource().createUser(uUID);

		clone.setLastName(CurrentName);

		// Set the group silently.
		clone.setGroup(this.getDataSource().getGroup(this.getGroupName()), false);

		// Clone all subgroups.
		clone.subGroups.addAll(this.subGroupListCloneStringCopy());

		// Clone permissions
		for (String perm : this.getPermissionList()) {
			clone.addPermission(perm);
		}

		// Clone timed permissions.
		for (Entry<String, Long> perm : this.getTimedPermissions().entrySet()) {
			clone.addTimedPermission(perm.getKey(), perm.getValue());
		}

		// Clone timed subgroups.
		for (Entry<String, Long> grp : this.getTimedSubGroups().entrySet()) {
			clone.addTimedSubGroup(getDataSource().getGroup(grp.getKey()), grp.getValue());
		}

		clone.variables = this.variables.clone(this);
		clone.flagAsChanged();

		return clone;
	}

	/**
	 * Gets the main group this user is a member of.
	 *
	 * @return the group.
	 */
	public Group getGroup() {

		Group result = getDataSource().getGroup(group);
		if (result == null) {
			this.setGroup(getDataSource().getDefaultGroup());
			result = getDataSource().getDefaultGroup();
		}
		return result;
	}

	/**
	 * Gets the main group name this user is a member of.
	 *
	 * @return the group name.
	 */
	public String getGroupName() {

		Group result = getDataSource().getGroup(group);
		if (result == null) {
			group = getDataSource().getDefaultGroup().getName();
		}
		return group;
	}

	/**
	 * Place holder to let people know to stop using this method.
	 *
	 * @return a string containing the players last known name.
	 * @deprecated use {@link #getLastName()} and {@link #getUUID()}.
	 */
	@Deprecated
	public String getName() {

		return this.getLastName();

	}

	/**
	 * Silently set teh Users Group.
	 * 
	 * @param group the group to set
	 */
	public void setGroup(Group group) {

		setGroup(group, true);
	}

	/**
	 * Sets the Group this User belongs to.
	 * 
	 * @param group       the group to set
	 * @param updatePerms if we are to trigger a superperms update.
	 */
	public void setGroup(Group group, Boolean updatePerms) {

		if (!this.getDataSource().groupExists(group.getName())) {
			getDataSource().addGroup(group);
		}
		group = getDataSource().getGroup(group.getName());
		String oldGroup = this.group;
		this.group = group.getName();
		flagAsChanged();
		if (GroupManager.isLoaded()) {
			if (!GroupManager.getBukkitPermissions().isPlayer_join() && (updatePerms))
				GroupManager.getBukkitPermissions().updatePlayer(getBukkitPlayer());

			// Do we notify of the group change?
			String defaultGroupName = getDataSource().getDefaultGroup().getName();
			// if we were not in the default group
			// or we were in the default group and the move is to a different
			// group.
			boolean notify = (!oldGroup.equalsIgnoreCase(defaultGroupName)) || ((oldGroup.equalsIgnoreCase(defaultGroupName)) && (!this.group.equalsIgnoreCase(defaultGroupName)));

			if (notify)
				GroupManager.notify(this.getLastName(), String.format(Messages.getString("MOVED_TO_GROUP"), group.getName(), this.getDataSource().getName()));

			if (updatePerms)
				GroupManager.getGMEventHandler().callEvent(this, Action.USER_GROUP_CHANGED);
		}
	}

	/**
	 * Adds a static sub-group.
	 * 
	 * @param subGroup	the group to add.
	 * @return			true if the group was added.
	 */
	public boolean addSubGroup(Group subGroup) {

		// Don't allow adding a subgroup if it's already set as the primary.
		if (this.group.equalsIgnoreCase(subGroup.getName())) {
			return false;
		}
		// User already has this subgroup
		if (containsSubGroup(subGroup))
			return false;

		// If the group doesn't exists add it
		if (!this.getDataSource().groupExists(subGroup.getName())) {
			getDataSource().addGroup(subGroup);
		}

		subGroups.add(subGroup.getName());
		flagAsChanged();
		if (GroupManager.isLoaded()) {
			if (!GroupManager.getBukkitPermissions().isPlayer_join())
				GroupManager.getBukkitPermissions().updatePlayer(getBukkitPlayer());
			GroupManager.getGMEventHandler().callEvent(this, Action.USER_SUBGROUP_CHANGED);
		}
		return true;

		//subGroup = getDataSource().getGroup(subGroup.getName());
		//removeSubGroup(subGroup);
		//subGroups.add(subGroup.getName());
	}

	/**
	 * Adds a timed sub-group.
	 * 
	 * @param subGroup	the group to add.
	 * @return			true if the group was added.
	 */
	public boolean addTimedSubGroup(Group subGroup, long expires) {

		// Don't allow adding a subgroup if it's already set as the primary.
		if (this.group.equalsIgnoreCase(subGroup.getName())) {
			return false;
		}
		// User already has this subgroup
		if (containsSubGroup(subGroup))
			return false;

		// If the group doesn't exists add it
		if (!this.getDataSource().groupExists(subGroup.getName())) {
			getDataSource().addGroup(subGroup);
		}

		synchronized(timedSubGroups) {
			
			if ((timedSubGroups.containsKey(subGroup.getName()) && timedSubGroups.get(subGroup.getName()) < expires)
					|| !timedSubGroups.containsKey(subGroup.getName())) {
	
				Map<String, Long> clone = new HashMap<>(timedSubGroups);
				clone.put(subGroup.getName(), expires);
				timedSubGroups = Collections.unmodifiableMap(clone);
				
				GroupManager.logger.info(String.format("Timed: %s - expires: %o", subGroup.getName(), expires));
			}
			flagAsChanged();
			if (GroupManager.isLoaded()) {
				if (!GroupManager.getBukkitPermissions().isPlayer_join())
					GroupManager.getBukkitPermissions().updatePlayer(getBukkitPlayer());
				GroupManager.getGMEventHandler().callEvent(this, Action.USER_SUBGROUP_CHANGED);
			}
		}
		return true;
	}

	/**
	 * Total sub-groups, times and static.
	 *
	 * @return	amount of sub-groups on this user.
	 */
	public int subGroupsSize () {

		return subGroups.size() + timedSubGroups.size();
	}

	/**
	 * Does this User have ANY sub-groups
	 * static or timed.
	 * 
	 * @return	true if any sub-groups are present.
	 */
	public boolean isSubGroupsEmpty () {

		return subGroups.isEmpty() && timedSubGroups.isEmpty();
	}

	/**
	 * Does the user have this as a SubGroup (not timed).
	 * 
	 * @param subGroup	the Group to test.
	 * @return			true if group is present.
	 */
	public boolean containsSubGroup (Group subGroup){

		return subGroups.contains(subGroup.getName());
	}

	/**
	 * Remove a sub-group.
	 * 
	 * @param subGroup	the Group to remove.
	 * @return			true if a group was removed.
	 */
	public boolean removeSubGroup (Group subGroup){
		
		if (timedSubGroups.containsKey(subGroup.getName()))
			return removeTimedSubGroup(subGroup);
		
		try {
			if (subGroups.remove(subGroup.getName())) {
				flagAsChanged();
				if (GroupManager.isLoaded())
					if (!GroupManager.getBukkitPermissions().isPlayer_join())
						GroupManager.getBukkitPermissions().updatePlayer(getBukkitPlayer());
				GroupManager.getGMEventHandler().callEvent(this, Action.USER_SUBGROUP_CHANGED);
				return true;
			}
		} catch (Exception ignored) {}
		
		return false;
	}

	/**
	 * Remove a timed sub-group.
	 * 
	 * @param subGroup	the Group to remove.
	 * @return			true if a group was removed.
	 */
	private boolean removeTimedSubGroup(Group subGroup) {

		synchronized(timedSubGroups) {
			
			Map<String, Long> clone = new HashMap<String, Long>(timedSubGroups);
			if (clone.remove(subGroup.getName()) != null) {
				flagAsChanged();
				timedSubGroups = Collections.unmodifiableMap(clone);
				if (GroupManager.isLoaded())
					if (!GroupManager.getBukkitPermissions().isPlayer_join())
						GroupManager.getBukkitPermissions().updatePlayer(getBukkitPlayer());
				GroupManager.getGMEventHandler().callEvent(this, Action.USER_SUBGROUP_CHANGED);
				return true;
			}
			
			return false;
		}
	}

	/**
	 * Returns a new array of the Sub-Groups attached to this user.
	 * Includes timed Groups.
	 *
	 * @return List of sub-groups.
	 */
	public ArrayList<Group> subGroupListCopy() {

		ArrayList<Group> groupList = new ArrayList<>();

		synchronized(subGroups) {

			subGroups.forEach(name -> {
				Group g = getDataSource().getGroup(name);

				if (g == null) {
					removeTimedSubGroup(g);
				} else {
					groupList.add(g);
				}
			});

			timedSubGroups.keySet().forEach(name -> {
				Group g = getDataSource().getGroup(name);

				if (g == null) {
					removeTimedSubGroup(g);
				} else {
					groupList.add(g);
				}
			});
		}
		return groupList;
	}

	/**
	 * Returns an unmodifiable map of any timed subGroups.
	 * 
	 * @return	Map of times sub-groups.
	 */
	public Map<String, Long> getTimedSubGroups() {

		return timedSubGroups;
	}

	/**
	 * Fetch a SubGroup list formatted for saving.
	 * 
	 * @return	List of sub-group names.
	 */
	public List<String> getSaveSubGroupsList() {

		synchronized(subGroups) {
			ArrayList<String> val = new ArrayList<>(subGroups);

			timedSubGroups.forEach((group, timer) -> val.add(group + "|" + timer));

			return val;
		}
	}

	/**
	 * Compiles a list of Sub-Group Names attached to this user.
	 * Excludes timed Groups.
	 *
	 * @return List of sub-group names.
	 */
	public ArrayList<String> subGroupListCloneStringCopy() {

		synchronized(subGroups) {

			return new ArrayList<>(subGroups);
		}
	}

	/**
	 * Compiles a list of Sub-Group Names attached to this user.
	 * Includes timed Groups.
	 *
	 * @return List of sub-group names.
	 */
	public ArrayList<String> subGroupListStringCopy() {

		synchronized(subGroups) {
			ArrayList<String> val = new ArrayList<>(subGroups);
			// TODO separate list for clone methods.
			timedSubGroups.forEach((group, timer) -> val.add(group));

			return val;
		}
	}

	/**
	 * @return the variables
	 */
	public UserVariables getVariables () {

		return variables;
	}

	/**
	 *
	 * @param varList
	 */
	public void setVariables (Map < String, Object > varList){

		variables.clearVars();
		for (String key : varList.keySet()) {
			variables.addVar(key, varList.get(key));
		}
		flagAsChanged();
		if (GroupManager.isLoaded()) {
			GroupManager.getGMEventHandler().callEvent(this, Action.USER_INFO_CHANGED);
		}
	}

	@Deprecated
	public User updatePlayer() {

		return this;
	}

	/**
	 * Returns a Player object (if online), or null.
	 *
	 * @return Player object or null.
	 */
	public Player getBukkitPlayer () {

		return BukkitWrapper.getInstance().getPlayer(getLastName());
	}

	/**
	 * Is this player currently Online.
	 *
	 * @return
	 */
	public boolean isOnline () {

		return getBukkitPlayer() != null;
	}
	/**
	 * Remove any expired subGroups.
	 *
	 * @return true if any Groups were removed.
	 */
	public boolean removeExpired() {

		boolean expired = false;

		synchronized (timedSubGroups) {

			SortedMap<String, Long> clone = new TreeMap<>(timedSubGroups);
			for (Entry<String, Long> entry : timedSubGroups.entrySet()) {
				if (Tasks.isExpired(entry.getValue())) {
					if (clone.remove(entry.getKey()) != null) {
						//changed = true;
						expired = true;
						GroupManager.logger.info(String.format("Timed Subgroup removed from : %s : %s", getLastName(), entry.getKey()));
					}
				}
			}

			if (expired)
				timedSubGroups = Collections.synchronizedSortedMap(clone);
		}

		return expired || super.removeExpired();
	}
}
