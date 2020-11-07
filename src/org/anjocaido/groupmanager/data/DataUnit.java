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
import org.anjocaido.groupmanager.utils.StringPermissionComparator;
import org.anjocaido.groupmanager.utils.Tasks;

/**
 * 
 * @author gabrielcouto
 */
public abstract class DataUnit {

	private WorldDataHolder dataSource;
	private final String uUID;
	private String lastName = "";
	private boolean changed, sorted = false;
	private List<String> permissions = Collections.unmodifiableList(Collections.emptyList());
	
	private Map<String, Long> timedPermissions = Collections.unmodifiableSortedMap(Collections.synchronizedSortedMap(Collections.emptySortedMap()));

	public DataUnit(WorldDataHolder dataSource, String name) {

		this.dataSource = dataSource;
		this.uUID = name;
	}

	public DataUnit(String name) {

		this.uUID = name;
	}

	/**
	 * Every group is matched only by their names and DataSources names.
	 * 
	 * @param o
	 * @return true if they are equal. false if not.
	 */
	@Override
	public boolean equals(Object o) {

		if (o instanceof DataUnit) {
			DataUnit go = (DataUnit) o;
			if (this.getUUID().equalsIgnoreCase(go.getUUID())) {
				// Global Group match.
				if (this.dataSource == null && go.getDataSource() == null)
					return true;
				// This is a global group, the object to test isn't.
				if (this.dataSource == null && go.getDataSource() != null)
					return false;
				// This is not a global group, but the object to test is.
				if (this.dataSource != null && go.getDataSource() == null)
					return false;
				// Match on group name and world name.
				return this.dataSource.getName().equalsIgnoreCase(go.getDataSource().getName());
			}
		}
		return false;
	}

	@Override
	public int hashCode() {

		int hash = 5;
		hash = 71 * hash + (this.uUID != null ? this.uUID.toLowerCase().hashCode() : 0);
		return hash;
	}

	/**
	 * Set the data source to point to a different worldDataHolder
	 * 
	 * @param source
	 */
	public void setDataSource(WorldDataHolder source) {

		this.dataSource = source;
	}

	/**
	 * Get the current worldDataHolder this object is pointing to
	 * 
	 * @return the dataSource
	 */
	public WorldDataHolder getDataSource() {

		return dataSource;
	}
	
	/**
	 * Read the UUID for this object, or the name if it has no UUID.
	 * 
	 * @return String of this objects UUID or name.
	 */
	public String getUUID() {

		return uUID;
	}
	
	/**
	 * Fetch the name of this object.
	 * 
	 * @return String of this objects name.
	 */
	public String getLastName() {

		/*
		 * Return the uUID field if it hasn't been updated
		 * for a UUID yet (or is a group name).
		 */
		if (uUID.length() < 36)
			return this.uUID;
		
		return this.lastName;
	}
	
	public void setLastName(String lastName) {

		if (!lastName.equals(this.lastName)) {
			
			this.lastName = lastName;
			dataSource.putUUIDLookup(lastName, uUID);
			
			changed = true;
		}
	}

	public void flagAsChanged() {

		WorldDataHolder testSource = getDataSource();
		String source;

		if (testSource == null)
			source = "GlobalGroups";
		else
			source = testSource.getName();

		GroupManager.logger.finest(String.format("DataSource: %s - DataUnit: %s flagged as ", source, getUUID()) + "changed!");
		// for(StackTraceElement st: Thread.currentThread().getStackTrace()){
		// GroupManager.logger.finest(st.toString());
		// }
		sorted = false;
		changed = true;
	}

	public boolean isChanged() {

		return changed;
	}

	public void flagAsSaved() {

		WorldDataHolder testSource = getDataSource();
		String source;

		if (testSource == null)
			source = "GlobalGroups";
		else
			source = testSource.getName();

		GroupManager.logger.finest(String.format("DataSource: %s - DataUnit: %s flagged as ", source, getUUID()) + "saved!");
		changed = false;
	}

	public boolean hasSamePermissionNode(String permission) {

		synchronized(timedPermissions) {
			return permissions.contains(permission) || timedPermissions.containsKey(permission);
		}
	}

	/**
	 * Add a new permission node
	 * 
	 * @param permission
	 */
	public void addPermission(String permission) {

		if (!hasSamePermissionNode(permission)) {
			List<String> clone = new ArrayList<>(permissions);
			clone.add(permission);
			permissions = Collections.unmodifiableList(clone);
		}
		flagAsChanged();
	}
	
	/**
	 * Add a timed permission if the duration is longer than an existing one
	 * or this permission does not exist.
	 * 
	 * @param permission
	 * @param expires
	 */
	public void addTimedPermission(String permission, Long expires) {

		/*
		 * Do not add a timed permission if there is already a static one.
		 */
		if (permissions.contains(permission))
			return;
		
		synchronized(timedPermissions) {
			/*
			 * Has the Permission expired?
			 */
			/*if (Tasks.isExpired(expires)) {
				GroupManager.logger.warning("Failed to add expired permission: " + getLastName() + " : " + permission);
				return;
			}*/
			
			if (!timedPermissions.containsKey(permission) || timedPermissions.get(permission) < expires) {
				Map<String, Long> clone = new HashMap<>(timedPermissions);
				clone.put(permission, expires);
				timedPermissions = Collections.unmodifiableMap(clone);
				GroupManager.logger.info(String.format("Timed: %s - expires: %o", permission, expires));
			}
			flagAsChanged();
		}
	}

	/**
	 * Remove a permission.
	 *
	 * @param permission
	 * @return	true if the permission was found and removed.
	 */
	public boolean removePermission(String permission) {

		synchronized(timedPermissions) {
			if (timedPermissions.containsKey(permission))
				return removeTimedPermission(permission);
		}

		flagAsChanged();
		List<String> clone = new ArrayList<String>(permissions);
		boolean ret = clone.remove(permission);
		permissions = Collections.unmodifiableList(clone);
		return ret;
	}

	/**
	 * Remove a timed permission.
	 *
	 * @param permission
	 * @return	true if the permission was found and removed.
	 */
	private boolean removeTimedPermission(String permission) {

		synchronized(timedPermissions) {
			flagAsChanged();
			Map<String, Long> clone = new HashMap<String, Long>(timedPermissions);
			boolean ret = clone.remove(permission) != null;
			timedPermissions = Collections.unmodifiableMap(clone);
			return ret;
		}
	}

	/**
	 * Use this only to list permissions.
	 * You can't edit the permissions using the returned ArrayList instance
	 * 
	 * @return a copy of the permission list
	 */
	public List<String> getPermissionList() {
		sortPermissions();
		return permissions;
	}
	
	/**
	 * This contains static and timed permissions.
	 * 
	 * @return
	 */
	public List<String> getAllPermissionList() {
		
		synchronized(timedPermissions) {
			List<String> perms = new ArrayList<>();
			perms.addAll(permissions);
			perms.addAll(timedPermissions.keySet());
			Collections.sort(perms);
	
			return perms;
		}
	}
	
	/**
	 * Only use this for saving.
	 * 
	 * @return
	 */
	public List<String> getSavePermissionList() {

		/*
		 * include static permissions.
		 */
		List<String> perms = new ArrayList<>(getPermissionList());
		synchronized(timedPermissions) {
			/*
			 * Include the concatenated timed permissions.
			 */
			for (Entry<String, Long> entry : timedPermissions.entrySet()) {
				perms.add(entry.getKey() + "|" + entry.getValue());
			}
			Collections.sort(perms);
		return perms;
		}
	}
	
	/**
	 * Use this only to list timed permissions.
	 * You can't edit the permissions using the returned Map instance
	 * 
	 * @return
	 */
	public Map<String, Long> getTimedPermissions() {
		
		return new TreeMap<>(timedPermissions);
	}

	public boolean isSorted() {

		return this.sorted;
	}

	public void sortPermissions() {

		if (!isSorted()) {
			List<String> clone = new ArrayList<>(permissions);
			clone.sort(StringPermissionComparator.getInstance());
			permissions = Collections.unmodifiableList(clone);
			
			sorted = true;
		}
	}
	
	/**
	 * Remove any expired permissions.
	 * 
	 * @return true if any permissions were removed.
	 */
	public boolean removeExpired() {
		
		boolean expired = false;
		
		synchronized(timedPermissions) {	
			
			SortedMap<String, Long> clone = new TreeMap<>(timedPermissions);
			for (Entry<String, Long> perm : timedPermissions.entrySet()) {
				if (Tasks.isExpired(perm.getValue())) {
					if (clone.remove(perm.getKey()) != null) {
						//changed = true;
						expired = true;
						GroupManager.logger.info(String.format("Timed Permission removed from : %s : %s", getLastName(), perm.getKey()));
					}
				}
			}
			
			if (expired)
				timedPermissions = Collections.unmodifiableSortedMap(clone);
		}
		
		return expired;
	}
}