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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.anjocaido.groupmanager.data.Group;
import org.anjocaido.groupmanager.events.GMGroupEvent;
import org.anjocaido.groupmanager.localization.Messages;
import org.anjocaido.groupmanager.utils.PermissionCheckResult;
import org.anjocaido.groupmanager.utils.Tasks;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.reader.UnicodeReader;

/**
 * @author ElgarL
 * 
 */
public class GlobalGroups {

	private final GroupManager plugin;

	private final Map<String, Group> groups = Collections.synchronizedMap(new HashMap<>());

	protected long timeStampGroups = 0;
	protected boolean haveGroupsChanged = false;
	protected File GlobalGroupsFile = null;

	public GlobalGroups(GroupManager plugin) {

		this.plugin = plugin;
		load();
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
	 * @return the timeStampGroups
	 */
	public long getTimeStampGroups() {

		return timeStampGroups;
	}

	/**
	 * @param timeStampGroups the timeStampGroups to set
	 */
	protected void setTimeStampGroups(long timeStampGroups) {

		this.timeStampGroups = timeStampGroups;
	}

	/**
	 * @param haveGroupsChanged
	 *            the haveGroupsChanged to set
	 */
	public void setGroupsChanged(boolean haveGroupsChanged) {

		this.haveGroupsChanged = haveGroupsChanged;
	}

	@SuppressWarnings("unchecked")
	public void load() {

		Yaml GGroupYAML = new Yaml(new SafeConstructor());
		Map<String, Object> GGroups;

		GroupManager.setLoaded(false);

		// READ globalGroups FILE
		if (GlobalGroupsFile == null)
			GlobalGroupsFile = new File(plugin.getDataFolder(), "globalgroups.yml"); //$NON-NLS-1$

		if (!GlobalGroupsFile.exists()) {
			try {
				// Create a new file if it doesn't exist.
				Tasks.copy(plugin.getResource("globalgroups.yml"), GlobalGroupsFile); //$NON-NLS-1$
			} catch (IOException ex) {
				GroupManager.logger.log(Level.SEVERE, null, ex);
			}
		}

		/*
		 * Load the YAML file.
		 */
		try {
			FileInputStream groupsInputStream = new FileInputStream(GlobalGroupsFile);
			GGroups = GGroupYAML.load(new UnicodeReader(groupsInputStream));
			groupsInputStream.close();
		} catch (Exception ex) {
			throw new IllegalArgumentException(String.format(Messages.getString("GroupManager.FILE_CORRUPT"), GlobalGroupsFile.getPath()), ex); //$NON-NLS-1$
		}

		// Clear out old groups
		resetGlobalGroups();

		if (!GGroups.keySet().isEmpty()) {
			// Read all global groups
			Map<String, Object> allGroups;

			try {
				allGroups = (Map<String, Object>) GGroups.get("groups"); //$NON-NLS-1$
			} catch (Exception ex) {
				// ex.printStackTrace();
				throw new IllegalArgumentException(String.format(Messages.getString("GroupManager.FILE_CORRUPT"), GlobalGroupsFile.getPath()), ex); //$NON-NLS-1$
			}

			// Load each groups permissions list.
			if (allGroups != null) {

				Iterator<String> groupItr = allGroups.keySet().iterator();
				String groupName;
				Integer groupCount = 0;

				/*
				 * loop each group entry
				 * and read it's data.
				 */
				while (groupItr.hasNext()) {
					try {
						groupCount++;
						// Attempt to fetch the next group name.
						groupName = groupItr.next();
					} catch (Exception ex) {
						throw new IllegalArgumentException(String.format(Messages.getString("GlobalGroups.INVALID_GROUP_NAME"), groupCount, GlobalGroupsFile.getPath()), ex); //$NON-NLS-1$
					}

					/*
					 * Create a new group with this name.
					 */
					Group newGroup = new Group(groupName.toLowerCase());
					Object element;

					// Permission nodes
					try {
						element = ((Map<String, Object>)allGroups.get(groupName)).get("permissions"); //$NON-NLS-1$
					} catch ( Exception ex) {
						throw new IllegalArgumentException(String.format(Messages.getString("GlobalGroups.BAD_FORMATTED"), groupName), ex); //$NON-NLS-1$
					}

					if (element != null)
						if (element instanceof List) {
							try {
								for (String node : (List<String>) element) {
									if ((node != null) && !node.isEmpty())
										newGroup.addPermission(node);
								}
							} catch (ClassCastException ex) {
								throw new IllegalArgumentException(String.format(Messages.getString("GlobalGroups.INVALID_PERMISSION_NODE"), groupName), ex); //$NON-NLS-1$
							}
						} else if (element instanceof String) {
							if ((element != null) && !((String)element).isEmpty())
								newGroup.addPermission((String) element);
						} else
							throw new IllegalArgumentException(String.format(Messages.getString("GlobalGroups.UNKNOWN_PERMISSION_TYPE"), groupName)); //$NON-NLS-1$

					// Push a new group
					addGroup(newGroup);
				}
			}

			removeGroupsChangedFlag();
		}

		setTimeStampGroups(GlobalGroupsFile.lastModified());
		GroupManager.setLoaded(true);
		// GlobalGroupsFile = null;
	}

	/**
	 * Write the globalgroups.yml file
	 */

	public void writeGroups(boolean overwrite) {

		// File GlobalGroupsFile = new File(plugin.getDataFolder(), "globalgroups.yml");

		if (haveGroupsChanged()) {
			if (overwrite || (!overwrite && (getTimeStampGroups() >= GlobalGroupsFile.lastModified()))) {
				Map<String, Object> root = new HashMap<>();

				Map<String, Object> groupsMap = new HashMap<>();
				root.put("groups", groupsMap); //$NON-NLS-1$
				synchronized(groups) {
					for (String groupKey : groups.keySet()) {
						Group group = groups.get(groupKey);

						// Group header
						Map<String, Object> aGroupMap = new HashMap<>();
						groupsMap.put(group.getName(), aGroupMap);

						// Permission nodes
						aGroupMap.put("permissions", group.getPermissionList()); //$NON-NLS-1$
					}
				}

				if (!root.isEmpty()) {
					DumperOptions opt = new DumperOptions();
					opt.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
					final Yaml yaml = new Yaml(opt);
					try {
						yaml.dump(root, new OutputStreamWriter(new FileOutputStream(GlobalGroupsFile), StandardCharsets.UTF_8)); //$NON-NLS-1$
					} catch (FileNotFoundException ignored) {}
				}
				setTimeStampGroups(GlobalGroupsFile.lastModified());
			} else {
				// Newer file found.
				GroupManager.logger.log(Level.WARNING, Messages.getString("GlobalGroups.ERROR_NEWER_GG_FOUND")); //$NON-NLS-1$
				throw new IllegalStateException(Messages.getString("ERROR_UNABLE_TO_SAVE")); //$NON-NLS-1$
			}
			removeGroupsChangedFlag();
		} else {
			// Check for newer file as no local changes.
			if (getTimeStampGroups() < GlobalGroupsFile.lastModified()) {
				GroupManager.logger.log(Level.WARNING, Messages.getString("GlobalGroups.WARN_NEWER_GG_FOUND_LOADING")); //$NON-NLS-1$
				// Backup GlobalGroups file
				backupFile();
				load();
			}
		}

	}

	/**
	 * Backup the GlobalGroups file
	 *
	 */
	private void backupFile() {

		File backupFile = new File(plugin.getBackupFolder(), "bkp_ggroups_" + Tasks.getDateString() + ".yml"); //$NON-NLS-1$ //$NON-NLS-2$
		try {
			Tasks.copy(GlobalGroupsFile, backupFile);
		} catch (IOException ex) {
			GroupManager.logger.log(Level.SEVERE, null, ex);
		}
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

		if (!hasGroup(groupName))
			return false;

		return groups.get(groupName.toLowerCase()).hasSamePermissionNode(permissionNode);

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
	 * @param groupName
	 * @return List of all group names
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

		if (!hasGroup(groupName))
			return null;

		return groups.get(groupName.toLowerCase());

	}

	/**
	 * @return the globalGroupsFile
	 */
	public File getGlobalGroupsFile() {

		return GlobalGroupsFile;
	}

	/**
	 *
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
