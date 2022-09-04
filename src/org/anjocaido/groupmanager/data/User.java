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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

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
	private final Map<String, Long> subGroups = new LinkedHashMap<>();

	/**
	 * This holds the fields in INFO node,
	 * like prefix = 'c' or build = false.
	 */
	private Variables variables = new Variables(this);

	/**
	 * @param name
	 */
	public User(WorldDataHolder source, String name) {

		super(source, name);
		this.group = source.getDefaultGroup().getName();
	}

	@Override
	public User clone() {

		boolean loaded = GroupManager.isLoaded();
		GroupManager.setLoaded(false); // Disable so we can push all data without triggering a save.
		
		User clone = new User(getDataSource(), this.getLastName());
		clone.group = this.group;

		// Clone permissions.
		for (Entry<String, Long> perm : this.getPermissions().entrySet()) {
			clone.addTimedPermission(perm.getKey(), perm.getValue());
		}
		// Clone subgroups.
		clone.subGroups.putAll(this.subGroups);

		GroupManager.setLoaded(loaded);	// Restore original state.
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
		
		boolean loaded = GroupManager.isLoaded();
		GroupManager.setLoaded(false); // Disable so we can push all data without triggering a save.

		User clone = dataSource.createUser(this.getUUID());

		clone.setLastName(this.getLastName());

		if (dataSource.getGroup(group) == null) {
			clone.setGroup(dataSource.getDefaultGroup());
		} else {
			clone.setGroup(dataSource.getGroup(this.getGroupName()));
		}

		// Clone permissions.
		for (Entry<String, Long> perm : this.getPermissions().entrySet()) {
			clone.addTimedPermission(perm.getKey(), perm.getValue());
		}

		// Clone subgroups.
		clone.subGroups.putAll(this.subGroups);

		clone.variables = this.variables.clone(this);
		// No need to flagAsChanged as its done in createUser
		
		GroupManager.setLoaded(loaded);	// Restore original state.
		return clone;
	}

	public User clone(String uUID, String CurrentName) {

		boolean loaded = GroupManager.isLoaded();
		GroupManager.setLoaded(false); // Disable so we can push all data without triggering a save.
		
		User clone = this.getDataSource().createUser(uUID);

		clone.setLastName(CurrentName);

		// Set the group silently.
		clone.setGroup(this.getDataSource().getGroup(this.getGroupName()), false);

		// Clone permissions.
		for (Entry<String, Long> perm : this.getPermissions().entrySet()) {
			clone.addTimedPermission(perm.getKey(), perm.getValue());
		}

		// Clone timed subgroups.
		clone.subGroups.putAll(this.subGroups);

		clone.variables = this.variables.clone(this);
		clone.flagAsChanged();

		GroupManager.setLoaded(loaded);	// Restore original state.
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
	 * Silently set the Users Group.
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

			Runnable notify = new Runnable() {

				// Do we notify of the group change?
				String defaultGroupName = getDataSource().getDefaultGroup().getName();
				// if we were not in the default group
				// or we were in the default group and the move is to a different
				// group.
				boolean notify = (!oldGroup.equalsIgnoreCase(defaultGroupName)) || ((oldGroup.equalsIgnoreCase(defaultGroupName)) && (!User.this.group.equalsIgnoreCase(defaultGroupName)));

				@Override
				public void run() {

					if (notify)
						GroupManager.notify(User.this.getLastName(), String.format(Messages.getString("MOVED_TO_GROUP"), User.this.group, User.this.getDataSource().getName()));

					if (updatePerms)
						GroupManager.getGMEventHandler().callEvent(User.this, Action.USER_GROUP_CHANGED);
				}
			};

			if (!GroupManager.getBukkitPermissions().isPlayer_join() && (updatePerms)) {
				GroupManager.getPlugin(GroupManager.class).getWorldsHolder().refreshData(notify);
			}
		}
	}

	/**
	 * Adds a static sub-group.
	 * 
	 * @param subGroup	the group to add.
	 * @return			true if the group was added.
	 */
	public boolean addSubGroup(Group subGroup) {

		return addTimedSubGroup(subGroup, 0L);
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

		// If the group doesn't exists add it (Mirroring)
		if (!this.getDataSource().groupExists(subGroup.getName())) {
			getDataSource().addGroup(subGroup);
		}

		Long duration = subGroups.get(subGroup.getName());

		// User already has this subgroup?
		if (!containsSubGroup(subGroup) || (containsSubGroup(subGroup) && (duration < expires) && (duration != 0))) {

			subGroups.put(subGroup.getName(), expires);

			GroupManager.logger.finest(String.format("Timed: %s - expires: %o", subGroup.getName(), expires));

			flagAsChanged();
			if (GroupManager.isLoaded()) {

				Runnable notify = new Runnable() {

					@Override
					public void run() {

						GroupManager.getGMEventHandler().callEvent(User.this, Action.USER_SUBGROUP_CHANGED);
					}
				};

				if (!GroupManager.getBukkitPermissions().isPlayer_join())
					GroupManager.getPlugin(GroupManager.class).getWorldsHolder().refreshData(notify);
			}
			return true;
		}

		return false;
	}

	/**
	 * Total sub-groups.
	 *
	 * @return	amount of sub-groups on this user.
	 */
	public int subGroupsSize() {

		return subGroups.size();
	}

	/**
	 * Does this User have ANY sub-groups
	 * 
	 * @return	true if any sub-groups are present.
	 */
	public boolean isSubGroupsEmpty() {

		return subGroups.isEmpty();
	}

	/**
	 * Does the user have this as a SubGroup.
	 * 
	 * @param subGroup	the Group to test.
	 * @return			true if group is present.
	 */
	public boolean containsSubGroup(Group subGroup) {

		return subGroups.containsKey(subGroup.getName());
	}

	/**
	 * Remove a sub-group.
	 * 
	 * @param subGroup	the Group to remove.
	 * @return			true if a group was removed.
	 */
	public boolean removeSubGroup(Group subGroup) {

		if (subGroups.remove(subGroup.getName()) != null) {
			flagAsChanged();

			if (GroupManager.isLoaded()) {

				Runnable notify = new Runnable() {

					@Override
					public void run() {

						GroupManager.getGMEventHandler().callEvent(User.this, Action.USER_SUBGROUP_CHANGED);
					}
				};

				if (!GroupManager.getBukkitPermissions().isPlayer_join())
					GroupManager.getPlugin(GroupManager.class).getWorldsHolder().refreshData(notify);
			}
			return true;
		}
		return false;
	}

	/**
	 * Returns a new array of the Sub-Groups attached to this user.
	 *
	 * @return List of sub-groups.
	 */
	public ArrayList<Group> subGroupListCopy() {

		ArrayList<Group> groupList = new ArrayList<>();

		subGroups.keySet().forEach(name -> {
			Group g = getDataSource().getGroup(name);
			if (g != null)
				groupList.add(g);
		});
		return groupList;
	}

	/**
	 * Fetch a SubGroup list formatted for saving.
	 * 
	 * @return	List of sub-group names.
	 */
	public List<String> getSaveSubGroupsList() {

		ArrayList<String> val = new ArrayList<>();

		subGroups.forEach((group, timer) -> val.add(group + ((timer != 0) ? "|" + timer : "")));

		return val;
	}

	/**
	 * Compiles a list of Sub-Group Names attached to this user.
	 *
	 * @return List of sub-group names.
	 */
	public ArrayList<String> subGroupListStringCopy() {

		ArrayList<String> val = new ArrayList<>();

		subGroups.forEach((group, timer) -> val.add(group));

		return val;
	}

	/**
	 * @return the variables
	 */
	public Variables getVariables() {

		return variables;
	}

	/**
	 *
	 * @param nodeData
	 */
	public void setVariables(Map<?, ?> nodeData) {

		boolean loaded = GroupManager.isLoaded();
		GroupManager.setLoaded(false); // Disable so we can push all data without triggering a save.
		
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

					GroupManager.getGMEventHandler().callEvent(User.this, Action.USER_INFO_CHANGED);
				}
			};

			if (!GroupManager.getBukkitPermissions().isPlayer_join())
				GroupManager.getPlugin(GroupManager.class).getWorldsHolder().refreshData(notify);
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
	public Player getBukkitPlayer() {

		return BukkitWrapper.getInstance().getPlayer(getLastName());
	}

	/**
	 * Is this player currently Online.
	 *
	 * @return true if the player is online, false if not.
	 */
	public boolean isOnline() {

		return getBukkitPlayer() != null;
	}

	/**
	 * Remove any expired subGroups.
	 *
	 * @return true if any Groups were removed.
	 */
	public boolean removeExpired() {

		boolean expired = false;

		for (Entry<String, Long> entry : subGroups.entrySet()) {
			if ((entry.getValue() != 0) && Tasks.isExpired(entry.getValue())) {
				if (subGroups.remove(entry.getKey()) != null) {

					expired = true;
					GroupManager.logger.log(Level.INFO, (String.format("Timed Subgroup removed from : %s : %s", getLastName(), entry.getKey())));
				}
			}
		}
		return expired || super.removeExpired();
	}
}
