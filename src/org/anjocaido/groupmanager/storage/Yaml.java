/*
 * 
 */
package org.anjocaido.groupmanager.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.anjocaido.groupmanager.GlobalGroups;
import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.data.Group;
import org.anjocaido.groupmanager.data.User;
import org.anjocaido.groupmanager.dataholder.OverloadedWorldHolder;
import org.anjocaido.groupmanager.dataholder.WorldDataHolder;
import org.anjocaido.groupmanager.dataholder.worlds.WorldsHolder;
import org.anjocaido.groupmanager.events.GMSystemEvent;
import org.anjocaido.groupmanager.localization.Messages;
import org.anjocaido.groupmanager.utils.Tasks;
import org.bukkit.World;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.reader.UnicodeReader;

/**
 * @author ElgarL
 *
 */
public class Yaml implements DataSource {

	private GroupManager plugin;
	private File worldsFolder;

	/**
	 * 
	 * @param plugin
	 */
	public Yaml(GroupManager plugin) {

		this.plugin = plugin;
		this.worldsFolder = new File(this.plugin.getDataFolder(), "worlds"); //$NON-NLS-1$
	}

	@Override
	public void init(String worldName) {

		WorldsHolder holder = plugin.getWorldsHolder();
		String worldNameLowered = worldName.toLowerCase();

		if (!worldsFolder.exists()) {
			worldsFolder.mkdirs();
		}

		File defaultWorldFolder = new File(worldsFolder, worldNameLowered);
		if ((!defaultWorldFolder.exists()) && ((!holder.hasGroupsMirror(worldNameLowered))) || (!holder.hasUsersMirror(worldNameLowered))) {

			/*
			 * check and convert all old case sensitive folders to lower case
			 */
			File casedWorldFolder = new File(worldsFolder, worldName);
			if ((casedWorldFolder.exists()) && (casedWorldFolder.getName().toLowerCase().equals(worldNameLowered))) {
				/*
				 * Rename the old folder to the new lower cased format
				 */
				casedWorldFolder.renameTo(new File(worldsFolder, worldNameLowered));
			} else {
				/*
				 * Else we just create the folder
				 */
				defaultWorldFolder.mkdirs();
			}
		}

		if (defaultWorldFolder.exists()) {
			if (!holder.hasGroupsMirror(worldNameLowered)) {
				File groupsFile = new File(defaultWorldFolder, "groups.yml"); //$NON-NLS-1$
				if (!groupsFile.exists() || groupsFile.length() == 0) {

					InputStream template = plugin.getResource("groups.yml"); //$NON-NLS-1$
					try {
						Tasks.copy(template, groupsFile);
					} catch (IOException ex) {
						GroupManager.logger.log(Level.SEVERE, null, ex);
					}
				}
			}

			if (!holder.hasUsersMirror(worldNameLowered)) {
				File usersFile = new File(defaultWorldFolder, "users.yml"); //$NON-NLS-1$
				if (!usersFile.exists() || usersFile.length() == 0) {

					InputStream template = plugin.getResource("users.yml"); //$NON-NLS-1$
					try {
						Tasks.copy(template, usersFile);
					} catch (IOException ex) {
						GroupManager.logger.log(Level.SEVERE, null, ex);
					}
				}
			}
		}
	}

	@Override
	public void loadWorld(String worldName, Boolean isMirror) {

		WorldsHolder holder = plugin.getWorldsHolder();
		String worldNameLowered = worldName.toLowerCase();

		if (holder.hasOwnData(worldNameLowered)) {
			holder.getDataSource().reload(holder.getWorldData(worldNameLowered));
			return;
		}
		GroupManager.logger.finest(String.format(Messages.getString("WorldsHolder.ATTEMPT_TO_LOAD"), worldName)); //$NON-NLS-1$

		File thisWorldFolder = new File(worldsFolder, worldNameLowered);
		if ((isMirror) || (thisWorldFolder.exists() && thisWorldFolder.isDirectory())) {

			// Setup file handles, if not mirrored
			File groupsFile = (holder.hasGroupsMirror(worldNameLowered)) ? null : new File(thisWorldFolder, "groups.yml"); //$NON-NLS-1$
			File usersFile = (holder.hasUsersMirror(worldNameLowered)) ? null : new File(thisWorldFolder, "users.yml"); //$NON-NLS-1$

			if ((groupsFile != null) && (!groupsFile.exists())) {
				throw new IllegalArgumentException(String.format(Messages.getString("WorldsHolder.ERROR_NO_GROUPS_FILE"), worldName, groupsFile.getPath())); //$NON-NLS-1$
			}
			if ((usersFile != null) && (!usersFile.exists())) {
				throw new IllegalArgumentException(String.format(Messages.getString("WorldsHolder.ERROR_NO_USERS_FILE"), worldName, usersFile.getPath())); //$NON-NLS-1$
			}

			WorldDataHolder tempHolder = new WorldDataHolder(worldNameLowered);

			// Map the group object for any mirror
			if (holder.hasGroupsMirror(worldNameLowered)) {
				tempHolder.setGroupsObject(holder.getWorldData(holder.getGroupsMirror(worldNameLowered)).getGroupsObject());

			} else {
				tempHolder.setGroupsFile(groupsFile);
				try {
					loadGroups(tempHolder);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			// Map the user object for any mirror
			if (holder.hasUsersMirror(worldNameLowered)) {
				tempHolder.setUsersObject(holder.getWorldData(holder.getUsersMirror(worldNameLowered)).getUsersObject());

			} else {
				tempHolder.setUsersFile(usersFile);
				try {
					loadUsers(tempHolder);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			OverloadedWorldHolder thisWorldData = new OverloadedWorldHolder(tempHolder);

			if (thisWorldData != null) {
				// Set the file TimeStamps as it will be default from the initial creation.
				if (usersFile != null) {
					thisWorldData.setUsersFile(tempHolder.getUsersFile());
					thisWorldData.setTimeStampUsers(tempHolder.getUsersFile().lastModified());
				}
				if (groupsFile != null) {
					thisWorldData.setGroupsFile(tempHolder.getGroupsFile());
					thisWorldData.setTimeStampGroups(tempHolder.getGroupsFile().lastModified());
				}
				GroupManager.logger.finest(String.format(Messages.getString("WorldsHolder.WORLD_LOAD_SUCCESS"), worldName)); //$NON-NLS-1$
				holder.addWorldData(worldNameLowered, thisWorldData);
			}
		}
	}

	@Override
	public void loadAllSearchedWorlds() {

		WorldsHolder holder = plugin.getWorldsHolder();
		/*
		 * Read all known worlds from Bukkit Create the data files if they don't
		 * already exist, and they are not mirrored.
		 */
		for (World world : plugin.getServer().getWorlds()) {
			GroupManager.logger.log(Level.FINE, String.format(Messages.getString("WorldsHolder.CHECKING_DATA"), world.getName())); //$NON-NLS-1$
			if (!holder.hasOwnData(world.getName().toLowerCase())) {

				String usersMirror = holder.getUsersMirror("all_unnamed_worlds"); //$NON-NLS-1$
				String groupsMirror = holder.getGroupsMirror("all_unnamed_worlds"); //$NON-NLS-1$

				if (usersMirror != null)
					holder.putUsersMirror(world.getName().toLowerCase(), usersMirror);

				if (groupsMirror != null)
					holder.putGroupsMirror(world.getName().toLowerCase(), groupsMirror);

				GroupManager.logger.log(Level.FINE, String.format(Messages.getString("WorldsHolder.CREATING_FOLDERS"), world.getName())); //$NON-NLS-1$
				init(world.getName());
			}
		}
		/*
		 * Loop over all folders within the worlds folder and attempt to load
		 * the world data
		 */
		for (File folder : worldsFolder.listFiles()) {
			if (folder.isDirectory() && !folder.getName().startsWith(".")) { //$NON-NLS-1$
				GroupManager.logger.info(String.format(Messages.getString("WorldsHolder.WORLD_FOUND"), folder.getName())); //$NON-NLS-1$

				/*
				 * don't load any worlds which are already loaded,
				 * or fully mirrored worlds that don't need data.
				 */
				if (!holder.hasOwnData(folder.getName().toLowerCase()) && (!holder.hasUsersMirror("all_unnamed_worlds") || !holder.hasGroupsMirror("all_unnamed_worlds"))) {
					/*
					 * Call init() to check case sensitivity and
					 * convert to lower case, before we attempt to load this
					 * world.
					 */
					init(folder.getName());
					loadWorld(folder.getName().toLowerCase(), false);
				}
			}
		}
	}

	@Override
	public void loadGroups(WorldDataHolder dataHolder) throws IOException {

		// READ GROUPS FILE
		File groupsFile = dataHolder.getGroupsFile();

		org.yaml.snakeyaml.Yaml yamlGroups = new org.yaml.snakeyaml.Yaml(new SafeConstructor());
		Map<String, Object> groupsRootDataNode;

		if (!groupsFile.exists()) {
			throw new IllegalArgumentException(Messages.getString("WorldDatHolder.ERROR_NO_GROUPS_FILE") + System.lineSeparator() + groupsFile.getPath());
		}
		try (FileInputStream groupsInputStream = new FileInputStream(groupsFile)) {
			groupsRootDataNode = yamlGroups.load(new UnicodeReader(groupsInputStream));
			if (groupsRootDataNode == null) {
				throw new NullPointerException();
			}
		} catch (Exception ex) {
			throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_INVALID_FILE"), groupsFile.getPath(), ex));
		}

		// PROCESS GROUPS FILE

		Map<String, List<String>> inheritance = new HashMap<>();
		Map<?, ?> allGroupsNode;

		/*
		 * Fetch all groups under the 'groups' entry.
		 */
		try {
			allGroupsNode = (Map<?, ?>) groupsRootDataNode.get("groups");
		} catch (Exception ex) {
			throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_INVALID_FILE"), groupsFile.getPath()), ex);
		}

		if (allGroupsNode == null) {
			throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_NO_GROUPS"), groupsFile.getPath()));
		}

		Iterator<?> groupItr = allGroupsNode.keySet().iterator();
		String groupKey;
		Integer groupCount = 0;

		/*
		 * loop each group entry and process it's data.
		 */
		while (groupItr.hasNext()) {

			try {
				groupCount++;
				// Attempt to fetch the next group name.
				groupKey = (String) groupItr.next();
			} catch (Exception ex) {
				throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_INVALID_GROUP_NAME"), groupCount, groupsFile.getPath()), ex);
			}

			/*
			 * Fetch this groups child nodes
			 */
			Map<?, ?> thisGroupNode;

			try {
				thisGroupNode = (Map<?, ?>) allGroupsNode.get(groupKey);
			} catch (Exception ex) {
				throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_INVALID_CHILD_NODE"), groupKey, groupsFile.getPath()), ex);
			}

			/*
			 * Create a new group with this name in the assigned data source.
			 */
			Group thisGrp = dataHolder.createGroup(groupKey);

			if (thisGrp == null) {
				throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_GROUP_DUPLICATE"), groupKey, groupsFile.getPath()));
			}

			/*
			 * DEFAULT NODE
			 */
			Object nodeData;
			try {
				nodeData = thisGroupNode.get("default");
			} catch (Exception ex) {
				throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_INVALID_FORMAT"), "default", groupKey, groupsFile.getPath()));
			}

			/*
			 * If no 'default' node is found do nothing.
			 */
			if ((nodeData != null && Boolean.parseBoolean(nodeData.toString()))) {
				/*
				 * Set this as the default group. Warn if some other group has
				 * already claimed that position.
				 */
				if (dataHolder.getDefaultGroup() != null) {
					GroupManager.logger.warning(String.format(Messages.getString("WorldDatHolder.ERROR_DEFAULT_DUPLICATE"), thisGrp.getName(), dataHolder.getDefaultGroup().getName()));
					GroupManager.logger.warning(String.format(Messages.getString("WorldDatHolder.WARN_OVERIDE_DEFAULT"), groupsFile.getPath()));
				}
				dataHolder.setDefaultGroup(thisGrp);
			}

			/*
			 * PERMISSIONS NODE
			 */
			try {
				nodeData = thisGroupNode.get("permissions");
			} catch (Exception ex) {
				throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_INVALID_FORMAT"), "permissions", groupKey, groupsFile.getPath()));
			}

			/*
			 * If no permissions node is found, or it's empty do nothing.
			 */
			if (nodeData != null) {
				/*
				 * There is a permission list Which seems to hold data
				 */
				if (nodeData instanceof List) {
					/*
					 * Check each entry and add it as a new permission.
					 */
					try {
						for (Object o : ((List<?>) nodeData)) {
							try {
								/*
								 * Only add this permission if it's not empty.
								 */
								if (!o.toString().isEmpty())
									/*
									 * check for a timed permission
									 */
									if (o.toString().contains("|")) {
										String[] split = o.toString().split("\\|");
										try {

											thisGrp.addTimedPermission(split[0], Long.parseLong(split[1]));
										} catch (Exception e) {
											GroupManager.logger.warning("Timed Permission error: " + o.toString());
										}
									} else {
										/*
										 * add a standard permission
										 */
										thisGrp.addPermission(o.toString());
									}

							} catch (NullPointerException ignored) {} // Safe to ignore.

						}
					} catch (Exception ex) {
						throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_INVALID_FORMAT"), "permissions", thisGrp.getName(), groupsFile.getPath()), ex);
					}

				} else if (nodeData instanceof String) {
					/*
					 * Only add this permission if it's not empty.
					 */
					if (!nodeData.toString().isEmpty()) {
						/*
						 * check for a timed permission
						 */
						if (nodeData.toString().contains("|")) {
							String[] split = nodeData.toString().split("\\|");
							try {

								thisGrp.addTimedPermission(split[0], Long.parseLong(split[1]));
							} catch (Exception e) {
								GroupManager.logger.warning("TimedPermission error: " + nodeData.toString());
							}
						} else {
							/*
							 * add a standard permission
							 */
							thisGrp.addPermission((String) nodeData);
						}
					}

				} else {
					throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_UNKNOWN_TYPE"), "permissions", thisGrp.getName(), groupsFile.getPath()));
				}
			}

			/*
			 * INFO NODE
			 */
			try {
				nodeData = thisGroupNode.get("info");
			} catch (Exception ex) {
				throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_INVALID_FORMAT"), "info", groupKey, groupsFile.getPath()));
			}

			if (nodeData == null) {
				/*
				 * No info section was found, so leave all variables as
				 * defaults.
				 */
				GroupManager.logger.warning(String.format(Messages.getString("WorldDatHolder.WARN_GROUP_NO_INFO"), thisGrp.getName()));
				GroupManager.logger.warning(Messages.getString("WorldDatHolder.WARN_USING_DEFAULT") + groupsFile.getPath());

			} else if (nodeData != null && nodeData instanceof Map) {
				try {
					thisGrp.setVariables((Map<?, ?>) nodeData);
				} catch (Exception ex) {
					throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_INVALID_FORMAT"), "info", thisGrp.getName(), groupsFile.getPath()), ex);
				}

			} else
				throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_UNKNOWN_ENTRY"), "info", thisGrp.getName(), groupsFile.getPath()));

			/*
			 * INHERITANCE NODE
			 */
			try {
				nodeData = thisGroupNode.get("inheritance");
			} catch (Exception ex) {
				throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_INVALID_FORMAT"), "inheritance", groupKey, groupsFile.getPath()));
			}
			/*
			 * If no inheritance node is found, or it's empty do
			 * nothing.
			 */
			if (nodeData instanceof List) {
				try {
					for (Object grp : (List<?>) nodeData) {
						inheritance.computeIfAbsent(groupKey, k -> new ArrayList<>());
						inheritance.get(groupKey).add((String) grp);
					}

				} catch (Exception ex) {
					throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_INVALID_FORMAT"), "inheritance", thisGrp.getName(), groupsFile.getPath()), ex);
				}
			} else
				throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_UNKNOWN_ENTRY"), "inheritance", thisGrp.getName(), groupsFile.getPath()));

			// END GROUP

		}

		if (dataHolder.getDefaultGroup() == null) {
			throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_NO_DEFAULT"), groupsFile.getPath()));
		}

		/*
		 * Build the inheritance map and record any errors
		 */
		for (String group : inheritance.keySet()) {
			List<String> inheritedList = inheritance.get(group);
			Group thisGroup = dataHolder.getGroup(group);
			if (thisGroup != null)
				for (String inheritedKey : inheritedList) {
					if (inheritedKey != null) {
						Group inheritedGroup = dataHolder.getGroup(inheritedKey);
						if (inheritedGroup != null) {
							thisGroup.addInherits(inheritedGroup);
						} else
							GroupManager.logger.warning(String.format(Messages.getString("WorldDatHolder.WARN_INHERITED_NOT_FOUND"), inheritedKey, thisGroup.getName(), groupsFile.getPath()));
					}
				}
		}

		dataHolder.removeGroupsChangedFlag();

		// Update the LastModified time.
		dataHolder.setGroupsFile(groupsFile);
		dataHolder.setTimeStampGroups(groupsFile.lastModified());
	}

	@Override
	public void loadUsers(WorldDataHolder dataHolder) throws IOException {

		// READ USERS FILE
		File usersFile = dataHolder.getUsersFile();

		org.yaml.snakeyaml.Yaml yamlUsers = new org.yaml.snakeyaml.Yaml(new SafeConstructor());
		Map<String, Object> usersRootDataNode;

		if (!dataHolder.getUsersFile().exists()) {
			throw new IllegalArgumentException(Messages.getString("WorldDatHolder.ERROR_NO_USERS_FILE") + System.lineSeparator() + usersFile.getPath());
		}
		try (FileInputStream usersInputStream = new FileInputStream(usersFile)) {
			usersRootDataNode = yamlUsers.load(new UnicodeReader(usersInputStream));
			if (usersRootDataNode == null) {
				throw new NullPointerException();
			}
		} catch (Exception ex) {
			throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_INVALID_FILE"), usersFile.getPath()), ex);
		}

		// PROCESS USERS FILE

		Map<?, ?> allUsersNode;

		/*
		 * Fetch all child nodes under the 'users' entry.
		 */
		try {
			allUsersNode = (Map<?, ?>) usersRootDataNode.get("users");
		} catch (Exception ex) {
			throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_INVALID_FILE"), usersFile.getPath()), ex);
		}

		// Load users if the file is NOT empty

		if (allUsersNode != null) {

			Iterator<?> usersItr = allUsersNode.keySet().iterator();
			String usersKey;
			Object node;
			Integer userCount = 0;

			while (usersItr.hasNext()) {
				try {
					userCount++;
					// Attempt to fetch the next user name.
					node = usersItr.next();
					if (node instanceof Integer)
						usersKey = Integer.toString((Integer) node);
					else
						usersKey = node.toString();

				} catch (Exception ex) {
					throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_INVALID_NODE_USER"), userCount, usersFile.getPath()), ex);
				}

				Map<?, ?> thisUserNode;
				try {
					thisUserNode = (Map<?, ?>) allUsersNode.get(node);
				} catch (Exception ex) {
					throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_INVALID_FORMAT_FOR_USER"), usersKey, usersFile.getPath()));
				}

				User thisUser = dataHolder.createUser(usersKey);
				if (thisUser == null) {
					throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_DUPLICATE_USER"), usersKey, usersFile.getPath()));
				}

				/*
				 * LASTNAME NODES
				 */
				Object nodeData;
				try {
					nodeData = thisUserNode.get("lastname");

				} catch (Exception ex) {
					throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_INVALID_FORMAT_IN_USER"), "lastname", usersKey, usersFile.getPath()));
				}

				if ((nodeData instanceof String)) {
					thisUser.setLastName((String) nodeData);
				}

				/*
				 * USER PERMISSIONS NODES
				 */
				try {
					nodeData = thisUserNode.get("permissions");
				} catch (Exception ex) {
					throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_INVALID_FORMAT_FOR_USER"), "permissions", usersKey, usersFile.getPath()));
				}

				if (nodeData != null) {
					try {
						if (nodeData instanceof List) {
							for (Object o : ((List<?>) nodeData)) {
								/*
								 * Only add this permission if it's not empty
								 */
								if (!o.toString().isEmpty()) {
									/*
									 * check for a timed permission
									 */
									if (o.toString().contains("|")) {
										String[] split = o.toString().split("\\|");
										try {
											thisUser.addTimedPermission(split[0], Long.parseLong(split[1]));
										} catch (Exception e) {
											GroupManager.logger.warning("TimedPermission error: " + o.toString());
										}
									} else {
										thisUser.addPermission(o.toString());
									}
								}
							}
						} else if (nodeData instanceof String) {

							/*
							 * Only add this permission if it's not empty
							 */
							if (!nodeData.toString().isEmpty()) {
								/*
								 * check for a timed permission
								 */
								if (nodeData.toString().contains("|")) {
									String[] split = nodeData.toString().split("\\|");
									try {
										thisUser.addTimedPermission(split[0], Long.parseLong(split[1]));
									} catch (Exception e) {
										GroupManager.logger.warning("TimedPermission error: " + nodeData.toString());
									}
								} else {
									thisUser.addPermission(nodeData.toString());
								}
							}
						}
					} catch (NullPointerException ignored) {} // Safe to ignore null.
				}

				/*
				 * USER INFO NODE
				 */
				try {
					nodeData = thisUserNode.get("info");
				} catch (Exception ex) {
					throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_INVALID_FORMAT_IN_USER"), "info", usersKey, usersFile.getPath()));
				}

				if (nodeData != null) {
					if (nodeData instanceof Map) {
						thisUser.setVariables((Map<?, ?>) nodeData);

					} else
						throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_UNKNOWN_ENTRY_USER"), "info", thisUser.getLastName(), usersFile.getPath()));
				}
				// END INFO NODE

				/*
				 * PRIMARY GROUP
				 */
				try {
					nodeData = thisUserNode.get("group");
				} catch (Exception ex) {
					throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_INVALID_FORMAT_IN_USER"), "group", usersKey, usersFile.getPath()));
				}

				if (nodeData != null) {
					Group hisGroup = dataHolder.getGroup(nodeData.toString());
					if (hisGroup == null) {
						GroupManager.logger.warning(String.format(Messages.getString("WorldDatHolder.WARN_NO_GROUP_STATED"), thisUserNode.get("group").toString(), thisUser.getLastName(), dataHolder.getDefaultGroup().getName(), usersFile.getPath()));
						hisGroup = dataHolder.getDefaultGroup();
					}
					thisUser.setGroup(hisGroup);
				} else {
					thisUser.setGroup(dataHolder.getDefaultGroup());
				}

				/*
				 * SUBGROUPS NODES
				 */
				try {
					nodeData = thisUserNode.get("subgroups");
				} catch (Exception ex) {
					throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_INVALID_FORMAT_IN_USER"), "subgroups", usersKey, usersFile.getPath()));
				}

				/*
				 * If no subgroups node is found, or it's empty do nothing.
				 */
				if (nodeData != null) {

					if (nodeData instanceof List) {
						for (Object o : ((List<?>) nodeData)) {
							/*
							 * Only add this subgroup if it's not empty
							 */
							if (!o.toString().isEmpty()) {
								/*
								 * check for a timed subgroup
								 */
								if (o.toString().contains("|")) {
									String[] split = o.toString().split("\\|");
									try {
										Group subGrp = dataHolder.getGroup(split[0]);
										thisUser.addTimedSubGroup(subGrp, Long.parseLong(split[1]));
									} catch (Exception e) {
										GroupManager.logger.warning("TimedSubGroup error: " + o.toString());
									}
								} else {
									Group subGrp = dataHolder.getGroup(o.toString());
									if (subGrp != null)
										thisUser.addSubGroup(subGrp);
								}
							}
						}
					} else if (nodeData instanceof String) {

						/*
						 * Only add this subgroup if it's not empty
						 */
						if (!nodeData.toString().isEmpty()) {
							/*
							 * check for a timed subgroup
							 */
							if (nodeData.toString().contains("|")) {
								String[] split = nodeData.toString().split("\\|");
								Group subGrp = dataHolder.getGroup(split[0]);
								try {
									thisUser.addTimedSubGroup(subGrp, Long.parseLong(split[1]));
								} catch (Exception e) {
									GroupManager.logger.warning("TimedSubGroup error: " + nodeData.toString());
								}
							} else {
								Group subGrp = dataHolder.getGroup(nodeData.toString());
								if (subGrp != null)
									thisUser.addSubGroup(subGrp);
							}
						}
					}
				}
			}
		}
		dataHolder.removeUsersChangedFlag();
		// Update the LastModified time.
		dataHolder.setUsersFile(usersFile);
		dataHolder.setTimeStampUsers(usersFile.lastModified());
	}

	@Override
	public void reload(WorldDataHolder dataHolder) {

		try {
			reloadGroups(dataHolder);
			reloadUsers(dataHolder);
		} catch (Exception ex) {
			Logger.getLogger(WorldDataHolder.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	@Override
	public void reloadGroups(WorldDataHolder dataHolder) {

		GroupManager.setLoaded(false);

		try {
			// temporary holder in case the load fails.
			WorldDataHolder ph = new WorldDataHolder(dataHolder.getName());

			ph.setGroupsFile(dataHolder.getGroupsFile());
			loadGroups(ph);

			// transfer new data
			dataHolder.resetGroups();
			for (Group tempGroup : ph.getGroupList()) {
				tempGroup.clone(dataHolder);
			}

			dataHolder.setDefaultGroup(dataHolder.getGroup(ph.getDefaultGroup().getName()));
			dataHolder.removeGroupsChangedFlag();
			dataHolder.setTimeStampGroups(dataHolder.getGroupsFile().lastModified());

		} catch (Exception ex) {
			Logger.getLogger(WorldDataHolder.class.getName()).log(Level.WARNING, null, ex);
		}
		GroupManager.setLoaded(true);
		GroupManager.getGMEventHandler().callEvent(GMSystemEvent.Action.RELOADED);
	}

	@Override
	public void reloadUsers(WorldDataHolder dataHolder) {

		GroupManager.setLoaded(false);

		try {
			// temporary holder in case the load fails.
			WorldDataHolder ph = new WorldDataHolder(dataHolder.getName());

			// copy groups for reference
			for (Group tempGroup : dataHolder.getGroupList()) {
				tempGroup.clone(ph);
			}

			// setup the default group before loading user data.
			ph.setDefaultGroup(ph.getGroup(dataHolder.getDefaultGroup().getName()));

			ph.setUsersFile(dataHolder.getUsersFile());
			loadUsers(ph);

			// transfer new data
			dataHolder.resetUsers();
			for (User tempUser : ph.getUserList()) {
				tempUser.clone(dataHolder);
			}
			dataHolder.removeUsersChangedFlag();
			dataHolder.setTimeStampUsers(dataHolder.getUsersFile().lastModified());

		} catch (Exception ex) {
			Logger.getLogger(WorldDataHolder.class.getName()).log(Level.WARNING, null, ex);
		}
		GroupManager.setLoaded(true);
		GroupManager.getGMEventHandler().callEvent(GMSystemEvent.Action.RELOADED);
	}

	@Override
	public void saveGroups(WorldDataHolder dataHolder) {

		Map<String, Object> root = new HashMap<>();

		LinkedHashMap<String, Object> groupsMap = new LinkedHashMap<>();

		root.put("groups", groupsMap);

		for (String groupKey : dataHolder.getGroups().keySet()) {
			Group group = dataHolder.getGroups().get(groupKey);

			Map<String, Object> aGroupMap = new HashMap<>();
			groupsMap.put(group.getName(), aGroupMap);

			if (dataHolder.getDefaultGroup() == null) {
				GroupManager.logger.severe(Messages.getString("WorldDatHolder.WARN_NO_DEFAULT_GROUP") + dataHolder.getName());
			}
			aGroupMap.put("default", group.equals(dataHolder.getDefaultGroup()));

			Map<String, Object> infoMap = new HashMap<>();
			aGroupMap.put("info", infoMap);

			for (String infoKey : group.getVariables().getVarKeyList()) {
				infoMap.put(infoKey, group.getVariables().getVarObject(infoKey));
			}

			aGroupMap.put("inheritance", group.getInherits());

			aGroupMap.put("permissions", group.getSavePermissionList());
		}

		if (!root.isEmpty()) {
			DumperOptions opt = new DumperOptions();
			opt.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
			final org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml(opt);
			try {
				OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(dataHolder.getGroupsFile()), StandardCharsets.UTF_8);

				String newLine = System.getProperty("line.separator");

				out.write("# Group inheritance" + newLine);
				out.write("#" + newLine);
				out.write("# Any inherited groups prefixed with a g: are global groups" + newLine);
				out.write("# and are inherited from the GlobalGroups.yml." + newLine);
				out.write("#" + newLine);
				out.write("# Groups without the g: prefix are groups local to this world" + newLine);
				out.write("# and are defined in the this groups.yml file." + newLine);
				out.write("#" + newLine);
				out.write("# Local group inheritances define your promotion tree when using 'manpromote/mandemote'" + newLine);
				out.write(newLine);

				yaml.dump(root, out);
				out.close();
			} catch (Exception ignored) {}
		}

		// Update the LastModified time.
		dataHolder.setGroupsFile(dataHolder.getGroupsFile());
		dataHolder.setTimeStampGroups(dataHolder.getGroupsFile().lastModified());
		dataHolder.removeGroupsChangedFlag();

		if (GroupManager.isLoaded())
			GroupManager.getGMEventHandler().callEvent(GMSystemEvent.Action.SAVED);
	}

	@Override
	public void saveUsers(WorldDataHolder dataHolder) {

		Map<String, Object> root = new HashMap<>();
		LinkedHashMap<String, Object> usersMap = new LinkedHashMap<>();

		root.put("users", usersMap);

		// A sorted list of users.
		for (String userKey : new TreeSet<>(dataHolder.getUsers().keySet())) {
			User user = dataHolder.getUsers().get(userKey);
			if ((user.getGroup() == null || user.getGroup().equals(dataHolder.getDefaultGroup())) && user.getPermissionList().isEmpty() && user.getVariables().isEmpty() && user.isSubGroupsEmpty()) {
				continue;
			}

			LinkedHashMap<String, Object> aUserMap = new LinkedHashMap<>();
			usersMap.put(user.getUUID(), aUserMap);

			if (!user.getUUID().equalsIgnoreCase(user.getLastName())) {
				aUserMap.put("lastname", user.getLastName());
			}

			// GROUP NODE
			if (user.getGroup() == null) {
				aUserMap.put("group", dataHolder.getDefaultGroup().getName());
			} else {
				aUserMap.put("group", user.getGroup().getName());
			}

			// SUBGROUPS NODE
			aUserMap.put("subgroups", user.getSaveSubGroupsList());

			// PERMISSIONS NODE
			aUserMap.put("permissions", user.getSavePermissionList());

			// USER INFO NODE - BETA
			if (user.getVariables().getSize() > 0) {
				Map<String, Object> infoMap = new HashMap<>();
				aUserMap.put("info", infoMap);
				for (String infoKey : user.getVariables().getVarKeyList()) {
					infoMap.put(infoKey, user.getVariables().getVarObject(infoKey));
				}
			}
			// END USER INFO NODE - BETA
		}

		if (!root.isEmpty()) {
			DumperOptions opt = new DumperOptions();
			opt.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
			final org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml(opt);
			try {
				OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(dataHolder.getUsersFile()), StandardCharsets.UTF_8);
				yaml.dump(root, out);
				out.close();
			} catch (Exception ignored) {
			}
		}

		// Update the LastModified time.
		dataHolder.setUsersFile(dataHolder.getUsersFile());
		dataHolder.setTimeStampUsers(dataHolder.getUsersFile().lastModified());
		dataHolder.removeUsersChangedFlag();

		if (GroupManager.isLoaded())
			GroupManager.getGMEventHandler().callEvent(GMSystemEvent.Action.SAVED);
	}

	@Override
	public boolean hasNewGlobalGroupsData() {

		return GroupManager.getGlobalGroups().getTimeStampGroups() < GroupManager.getGlobalGroups().getGlobalGroupsFile().lastModified();
	}

	@Override
	public boolean hasNewGroupsData(WorldDataHolder dataHolder) {

		return dataHolder.getTimeStampGroups() < dataHolder.getGroupsFile().lastModified();
	}

	@Override
	public boolean hasNewUsersData(WorldDataHolder dataHolder) {

		return dataHolder.getTimeStampUsers() < dataHolder.getUsersFile().lastModified();
	}

	@Override
	public void backup(OverloadedWorldHolder world, BACKUP_TYPE type) {

		String prefix = "bkp_"; //$NON-NLS-1$

		switch (type) {
		case GLOBALGROUPS:
			prefix += "ggroups_"; //$NON-NLS-1$
			break;

		case GROUPS:
			prefix += world.getName() + "_g_"; //$NON-NLS-1$
			break;

		case USERS:
			prefix += world.getName() + "_u_"; //$NON-NLS-1$
			break;
		}

		File backupFile = new File(plugin.getBackupFolder(), prefix + Tasks.getDateString() + ".yml"); //$NON-NLS-1$

		try {

			switch (type) {
			case GLOBALGROUPS:
				Tasks.copy(GroupManager.getGlobalGroups().getGlobalGroupsFile(), backupFile);
				break;

			case GROUPS:
				Tasks.copy(world.getGroupsFile(), backupFile);
				break;

			case USERS:
				Tasks.copy(world.getUsersFile(), backupFile);
				break;
			}

		} catch (IOException ex) {
			GroupManager.logger.log(Level.SEVERE, null, ex);
		}
	}

	@Override
	public void purgeBackups() {

		Tasks.removeOldFiles(plugin.getBackupFolder());
	}

	@Override
	public void loadGlobalGroups(GlobalGroups globalGroups) {

		File globalGroupsFile = globalGroups.getGlobalGroupsFile();
		org.yaml.snakeyaml.Yaml groupYAML = new org.yaml.snakeyaml.Yaml(new SafeConstructor());

		Map<String, Object> groups;

		GroupManager.setLoaded(false);

		// Read globalGroups File.
		if (globalGroupsFile == null)
			globalGroupsFile = new File(plugin.getDataFolder(), "globalgroups.yml"); //$NON-NLS-1$

		if (!globalGroupsFile.exists()) {
			try {
				// Create a new file if it doesn't exist.
				Tasks.copy(plugin.getResource("globalgroups.yml"), globalGroupsFile); //$NON-NLS-1$
			} catch (IOException ex) {
				GroupManager.logger.log(Level.SEVERE, null, ex);
			}
		}

		/*
		 * Load the YAML file.
		 */
		try {
			FileInputStream groupsInputStream = new FileInputStream(globalGroupsFile);
			groups = groupYAML.load(new UnicodeReader(groupsInputStream));
			groupsInputStream.close();
		} catch (Exception ex) {
			throw new IllegalArgumentException(String.format(Messages.getString("GroupManager.FILE_CORRUPT"), globalGroupsFile.getPath()), ex); //$NON-NLS-1$
		}

		/*
		 * Clear out old groups
		 */
		globalGroups.resetGlobalGroups();

		if (!groups.keySet().isEmpty()) {
			// Read all global groups
			Map<?, ?> allGroups;

			try {
				allGroups = (Map<?, ?>) groups.get("groups"); //$NON-NLS-1$
			} catch (Exception ex) {
				throw new IllegalArgumentException(String.format(Messages.getString("GroupManager.FILE_CORRUPT"), globalGroupsFile.getPath()), ex); //$NON-NLS-1$
			}

			// Load each groups permissions list.
			if (allGroups != null) {

				Iterator<?> groupItr = ((Set<?>) allGroups.keySet()).iterator();
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
						groupName = (String) groupItr.next();
					} catch (Exception ex) {
						throw new IllegalArgumentException(String.format(Messages.getString("GlobalGroups.INVALID_GROUP_NAME"), groupCount, globalGroupsFile.getPath()), ex); //$NON-NLS-1$
					}

					/*
					 * Create a new group with this name.
					 */
					Group newGroup = new Group(groupName.toLowerCase());
					Object nodeData;

					// Permission nodes
					try {
						nodeData = ((Map<?, ?>) allGroups.get(groupName)).get("permissions"); //$NON-NLS-1$
					} catch ( Exception ex) {
						throw new IllegalArgumentException(String.format(Messages.getString("GlobalGroups.BAD_FORMATTED"), groupName), ex); //$NON-NLS-1$
					}

					if (nodeData != null)

						if (nodeData instanceof List) {
							try {
								for (Object node : (List<?>) nodeData) {
									if ((node != null) && !node.toString().isEmpty())
										newGroup.addPermission((String) node);
								}
							} catch (ClassCastException ex) {
								throw new IllegalArgumentException(String.format(Messages.getString("GlobalGroups.INVALID_PERMISSION_NODE"), groupName), ex); //$NON-NLS-1$
							}
						} else if (nodeData instanceof String) {
							if ((nodeData != null) && !((String) nodeData).isEmpty())
								newGroup.addPermission((String) nodeData);
						} else
							throw new IllegalArgumentException(String.format(Messages.getString("GlobalGroups.UNKNOWN_PERMISSION_TYPE"), groupName)); //$NON-NLS-1$

					// Push a new group
					globalGroups.addGroup(newGroup);
				}
			}
			globalGroups.removeGroupsChangedFlag();
		}

		globalGroups.setGlobalGroupsFile(globalGroupsFile);
		globalGroups.setTimeStampGroups(globalGroupsFile.lastModified());
		GroupManager.setLoaded(true);
	}

	@Override
	public void saveGlobalGroups(boolean overwrite) {

		GlobalGroups gg = GroupManager.getGlobalGroups();

		if (gg.haveGroupsChanged()) {
			if (overwrite || (!overwrite && !hasNewGlobalGroupsData())) {

				Map<String, Object> root = new HashMap<>();

				Map<String, Object> groupsMap = new HashMap<>();
				root.put("groups", groupsMap); //$NON-NLS-1$

				for (Group group : gg.getGroupList()) {

					// Group header
					Map<String, Object> aGroupMap = new HashMap<>();
					groupsMap.put(group.getName(), aGroupMap);

					// Permission nodes
					aGroupMap.put("permissions", group.getPermissionList()); //$NON-NLS-1$
				}

				if (!root.isEmpty()) {
					DumperOptions opt = new DumperOptions();
					opt.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
					final org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml(opt);

					try {
						yaml.dump(root, new OutputStreamWriter(new FileOutputStream(gg.getGlobalGroupsFile()), StandardCharsets.UTF_8)); //$NON-NLS-1$
					} catch (FileNotFoundException ignored) {}
				}

				/*
				 * Backup GlobalGroups file
				 */
				backup(null, DataSource.BACKUP_TYPE.GLOBALGROUPS);
				gg.setTimeStampGroups(gg.getGlobalGroupsFile().lastModified());

			} else {
				/*
				 * Newer file found?
				 */
				if (hasNewGlobalGroupsData()) {
					GroupManager.logger.log(Level.WARNING, Messages.getString("GlobalGroups.ERROR_NEWER_GG_FOUND")); //$NON-NLS-1$
					throw new IllegalStateException(Messages.getString("ERROR_UNABLE_TO_SAVE")); //$NON-NLS-1$
				}
			}
			gg.removeGroupsChangedFlag();

		} else {
			/*
			 * Check for newer file as no local changes.
			 */
			if (hasNewGlobalGroupsData()) {
				GroupManager.logger.log(Level.WARNING, Messages.getString("GlobalGroups.WARN_NEWER_GG_FOUND_LOADING")); //$NON-NLS-1$

				loadGlobalGroups(gg);
			}
		}
	}
}
