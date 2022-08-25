/*
 * 
 */
package org.anjocaido.groupmanager.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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
import org.anjocaido.groupmanager.storage.hikari.HikariCPDataSource;
import org.anjocaido.groupmanager.storage.statements.Statements;
import org.apache.commons.lang.StringUtils;
import org.bukkit.World;

/**
 * @author ElgarL
 *
 */
public class CoreSQL implements DataSource {

	protected final GroupManager plugin;
	protected HikariCPDataSource hikari;

	protected final String UPDATE_TABLE = (GroupManager.getGMConfig().getDatabaseGroup() + "_TABLES").toUpperCase();
	protected final String GLOBALGROUPS_TABLE = (GroupManager.getGMConfig().getDatabaseGroup() + "_GLOBALGROUPS").toUpperCase();

	private Statements statements;

	/**
	 * Throw any exceptions as we want to prevent
	 * the plugin loading if the database fails.
	 * 
	 * @param plugin
	 * @param statements
	 * @throws Exception
	 */
	public CoreSQL(GroupManager plugin, Statements statements) throws Exception {

		this.plugin = plugin;
		this.statements = statements;

		hikari = new HikariCPDataSource(this.statements.getDriver(), this.statements.getURL());

		// Create the time stamp table if it doesn't exist.
		try (Connection conn = hikari.getConnection();
				PreparedStatement create = conn.prepareStatement(String.format(this.statements.getCreateGlobalGroupTable(), UPDATE_TABLE))) {

			create.execute();
		}

		// Create the GlobalGroups table if it doesn't exist.
		try (Connection conn = hikari.getConnection();
				PreparedStatement create = conn.prepareStatement(String.format(this.statements.getCreateGlobalGroupTable(), GLOBALGROUPS_TABLE))) {

			
			create.execute();
		}
	}

	@Override
	public void init(String worldName) {

		WorldsHolder holder = plugin.getWorldsHolder();
		String worldNameLowered = worldName.toLowerCase();
		boolean isEmpty = true;

		// Create a table for any non-mirrored group data.
		if (!holder.hasGroupsMirror(worldNameLowered)) {

			String tableName = (GroupManager.getGMConfig().getDatabaseGroup() + "_" + worldNameLowered + "_GROUPS").toUpperCase();

			try (Connection conn = hikari.getConnection();
					PreparedStatement create = conn.prepareStatement(String.format(this.statements.getCreateGroupTable(), tableName))) {

				create.execute();

			} catch (SQLException e1) {
				e1.printStackTrace();
			}

			try (Connection conn = hikari.getConnection();
					PreparedStatement empty = conn.prepareStatement(String.format(this.statements.getSelectIsEmpty(), tableName))) {

				ResultSet result = empty.executeQuery();

				if (result.next()){
					isEmpty = (result.getInt(1) == 0);
					GroupManager.logger.info("Total Count for Groups in " + worldNameLowered + ":" + result.getInt(1));
				}

			} catch (SQLException e1) {
				e1.printStackTrace();
			}

			// Populate fresh data if the table is empty.
			if (isEmpty) {
				try (Connection conn = hikari.getConnection();
						PreparedStatement insert = conn.prepareStatement(String.format(this.statements.getInsertReplaceGroup(), tableName))) {

					insert.setString(1, "default");
					insert.setBoolean(2, true);
					insert.setString(3, null);
					insert.setString(4, null);
					insert.setString(5, null);

					insert.executeUpdate();

				} catch (SQLException e) {
					e.printStackTrace();
				}

				updateTableTimeStamp(tableName, Instant.now().toEpochMilli());
			}
		}


		// Create a table for any non-mirrored user data.
		if (!holder.hasUsersMirror(worldNameLowered)) {

			String tableName = (GroupManager.getGMConfig().getDatabaseGroup() + "_" + worldNameLowered + "_USERS").toUpperCase();

			try (Connection conn = hikari.getConnection();
					PreparedStatement create = conn.prepareStatement(String.format(this.statements.getCreateUserTable(), tableName))) {

				create.execute();

			} catch (SQLException e1) {
				e1.printStackTrace();
			}

			// All done. We don't need any default data for users.
		}
	}

	@Override
	public void loadGlobalGroups(GlobalGroups globalGroups) {

		try {
			loadGlobalGroupsAsync(globalGroups).get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}

	private CompletableFuture<Void> loadGlobalGroupsAsync(GlobalGroups globalGroups) {

		return CompletableFuture.supplyAsync(() -> {

			Set<Group> groups = new HashSet<>();

			// Read all groups from SQL.
			try (Connection conn = hikari.getConnection();
					PreparedStatement query = conn.prepareStatement(String.format(this.statements.getSelectAll(), GLOBALGROUPS_TABLE))) {

				ResultSet result = query.executeQuery();

				while (result.next()) {

					// NAME,PERMISSIONS

					String name = result.getString("NAME");
					String permissions = result.getString("PERMISSIONS");

					Group thisGrp = new Group(name.toLowerCase());

					// Add all permissions.
					if (permissions != null ) {
						Arrays.stream(permissions.split(",")).forEach(perm -> {

							thisGrp.addPermission(perm);
						});
					}
					groups.add(thisGrp);
				}

				if (!groups.isEmpty()) {

					GroupManager.setLoaded(false);
					globalGroups.resetGlobalGroups();
					for (Group group : groups) {
						globalGroups.addGroup(group);
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			// Fetch the time stamp for this data.
			return timeStampQuery(GLOBALGROUPS_TABLE);

		}).thenAccept((changed) -> {

			// Some data was updated.
			if (changed != null) {
				globalGroups.setTimeStampGroups(changed);
				globalGroups.removeGroupsChangedFlag();
			}

			GroupManager.setLoaded(true);
		});
	}

	@Override
	public void saveGlobalGroups(boolean overwrite) {

		//TODO: ignoring the overwrite setting for now (not sure we need it)

		GlobalGroups globalGroups = GroupManager.getGlobalGroups();

		CompletableFuture.supplyAsync(() -> {

			Long changed = null;

			if (globalGroups.haveGroupsChanged() && GroupManager.getGMConfig().getAccessType() == ACCESS_LEVEL.READ_WRITE) {

				// Batch push any changed Group data to the database.
				try (Connection conn = hikari.getConnection();
						PreparedStatement insert = conn.prepareStatement(String.format(this.statements.getInsertReplaceGlobalGroup(), GLOBALGROUPS_TABLE));) {

					conn.setAutoCommit(false);

					SortedMap<String, Group> groups = globalGroups.getGroups();
					Set<String> keys = groups.keySet();

					synchronized (groups) {
						Iterator<String> iter = keys.iterator(); // Must be in synchronized block
						while (iter.hasNext()) {
							Group group = groups.get(iter.next());

							if (!group.isChanged()) continue;

							changed = Instant.now().toEpochMilli();

							String name = group.getName();
							List<String> permissions = group.getSavePermissionList();

							insert.setString(1, name);
							insert.setString(2, permissions.isEmpty() ? null : StringUtils.join(permissions, ","));

							insert.addBatch();
						}
					}

					// Execute batch update.
					if (changed != null) {
						int[] result = insert.executeBatch();

						GroupManager.logger.info("Batch save GlobalGroups: " + Arrays.toString(result));
						conn.commit();
					}
					conn.setAutoCommit(true);

					if (changed == null) {
						// No data was saved so it must require deleting
						changed = deleteGlobalGroups(GLOBALGROUPS_TABLE);
					}

					// Update changed timeStamp in SQL table.
					updateTableTimeStamp(GLOBALGROUPS_TABLE, changed);

				} catch (Exception e) {
					e.printStackTrace();
				}

			} else {
				/*
				 * Check for newer data as no local changes
				 * or we are not permitted to write to SQL.
				 */
				if (hasNewGlobalGroupsData()) {
					GroupManager.logger.log(Level.WARNING, Messages.getString("GlobalGroups.WARN_NEWER_GG_FOUND_LOADING")); //$NON-NLS-1$

					loadGlobalGroupsAsync(globalGroups);
					return null;
				}
			}

			return changed;

		}).thenAccept((changed) -> {

			// Some data was updated.
			if (changed != null) {
				globalGroups.setTimeStampGroups(changed);
				globalGroups.removeGroupsChangedFlag();
			}

			if (GroupManager.isLoaded())
				GroupManager.getGMEventHandler().callEvent(GMSystemEvent.Action.SAVED);
		});
	}

	private Long deleteGlobalGroups(String tableName) {

		Set<String> databaseGroups = new HashSet<>();

		try (Connection conn = hikari.getConnection();
				PreparedStatement query = conn.prepareStatement("SELECT NAME FROM " + tableName + ";")) {

			ResultSet result = query.executeQuery();

			while (result.next()) {
				databaseGroups.add(result.getString(1));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Batch delete GlobalGroups in SQL who are no longer valid in our data.

		Long changed = null;

		try (Connection conn = hikari.getConnection();
				PreparedStatement insert = conn.prepareStatement("DELETE FROM " + tableName + " WHERE NAME = '?';")) {

			conn.setAutoCommit(false);

			Set<String> keys = GroupManager.getGlobalGroups().getGroups().keySet();

			for (String name : databaseGroups) {
				if (!keys.contains(name)) {

					changed = Instant.now().toEpochMilli();

					insert.setString(1, name);
					insert.addBatch();
				}
			}

			if (changed != null) {
				int[] result = insert.executeBatch();

				GroupManager.logger.info("Batch delete GlobalGroups: " + Arrays.toString(result));
				conn.commit();
			}
			conn.setAutoCommit(true);

			if (changed == null) {
				// No data was deleted so catch up
				changed = Instant.now().toEpochMilli();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return changed;
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

		WorldDataHolder tempHolder = new WorldDataHolder(worldNameLowered);

		// Map the group object for any mirror
		if (holder.hasGroupsMirror(worldNameLowered)) {
			tempHolder.setGroupsObject(holder.getWorldData(holder.getGroupsMirror(worldNameLowered)).getGroupsObject());

		} else {
			tempHolder.setGroupsFile(null);
			try {
				loadGroups(tempHolder);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// Map the user object for any mirror
		if (holder.hasUsersMirror(worldNameLowered)) {
			tempHolder.setUsersObject(holder.getWorldData(holder.getUsersMirror(worldNameLowered)).getUsersObject());

		} else {
			tempHolder.setUsersFile(null);
			try {
				loadUsers(tempHolder);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		OverloadedWorldHolder thisWorldData = new OverloadedWorldHolder(tempHolder);

		if (thisWorldData != null) {
			// Set the TimeStamps as it will be default from the initial creation.
			thisWorldData.setUsersFile(null);
			thisWorldData.getUsersObject().setTimeStamp(tempHolder.getUsersObject().getTimeStamp());

			thisWorldData.setGroupsFile(null);
			thisWorldData.getGroupsObject().setTimeStamp(tempHolder.getGroupsObject().getTimeStamp());

			GroupManager.logger.finest(String.format(Messages.getString("WorldsHolder.WORLD_LOAD_SUCCESS"), worldName)); //$NON-NLS-1$
			holder.addWorldData(worldNameLowered, thisWorldData);
		}
	}


	@Override
	public void loadAllSearchedWorlds() {

		WorldsHolder holder = plugin.getWorldsHolder();
		/*
		 * Read all known worlds from Bukkit to create the data if it
		 * doesn't already exist, and its not mirrored.
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

				//GroupManager.logger.log(Level.FINE, String.format(Messages.getString("WorldsHolder.CREATING_FOLDERS"), world.getName())); //$NON-NLS-1$
				//init(world.getName());
			}
		}

		holder.allWorldsDataList().forEach(worldHolder -> {

			GroupManager.logger.log(Level.INFO, (String.format(Messages.getString("WorldsHolder.WORLD_FOUND"), worldHolder.getName()))); //$NON-NLS-1$

			/*
			 * don't load any worlds which are already loaded,
			 * or fully mirrored worlds that don't need data.
			 */
			if (!holder.hasOwnData(worldHolder.getName()) && (!holder.hasUsersMirror("all_unnamed_worlds") || !holder.hasGroupsMirror("all_unnamed_worlds"))) {
				/*
				 * Call init() to ensure there is data to load.
				 */
				init(worldHolder.getName());
				loadWorld(worldHolder.getName(), false);
			}
		});
	}

	@Override
	public void loadGroups(WorldDataHolder dataHolder) throws Exception {

		loadGroupsAsync(dataHolder).get();
	}

	public CompletableFuture<Void> loadGroupsAsync(WorldDataHolder dataHolder) {

		return CompletableFuture.supplyAsync(() -> {

			Map<String, List<String>> inheritanceMap = new HashMap<>();

			String tableName = (GroupManager.getGMConfig().getDatabaseGroup() + "_" + dataHolder.getName().toLowerCase() + "_GROUPS").toUpperCase();

			// Read all groups from SQL.
			try (Connection conn = hikari.getConnection();
					PreparedStatement query = conn.prepareStatement(String.format(this.statements.getSelectAll(), tableName))) {

				ResultSet result = query.executeQuery();

				while (result.next()) {

					// NAME,DEFAULT,PERMISSIONS,INHERITANCE,INFO

					String name = result.getString("NAME");
					boolean isDefault = result.getBoolean("ISDEFAULT");
					String permissions = result.getString("PERMISSIONS");
					String inheritance = result.getString("INHERITANCE");
					String infoBlob = result.getString("INFO");

					Group thisGrp = dataHolder.createGroup(name);
					if (thisGrp == null) {
						throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_GROUP_DUPLICATE"), name, dataHolder.getName()));
					}

					if (isDefault) {
						/*
						 * Set this as the default group. Warn if some other group has
						 * already claimed that position.
						 */
						if (dataHolder.getDefaultGroup() != null) {
							GroupManager.logger.warning(String.format(Messages.getString("WorldDatHolder.ERROR_DEFAULT_DUPLICATE"), thisGrp.getName(), dataHolder.getDefaultGroup().getName()));
							GroupManager.logger.warning(String.format(Messages.getString("WorldDatHolder.WARN_OVERIDE_DEFAULT"), dataHolder.getName()));
						}
						dataHolder.setDefaultGroup(thisGrp);
					}

					// Add all permissions.
					if (permissions != null ) {
						Arrays.stream(permissions.split(",")).forEach(perm -> {

							if (perm.contains("|")) {
								String[] split = perm.split("\\|");
								try {
									thisGrp.addTimedPermission(split[0], Long.parseLong(split[1]));
								} catch (Exception e) {
									GroupManager.logger.warning("Timed Permission error: " + perm);
								}
							} else {
								/*
								 * add a standard permission
								 */
								thisGrp.addPermission(perm);
							}
						});
					}


					// Push all inheritances to a Map to process after all groups are loaded.
					if (inheritance != null) {
						Arrays.stream(inheritance.split(",")).forEach(group -> {

							try {
								inheritanceMap.computeIfAbsent(name, k -> new ArrayList<>());
								inheritanceMap.get(name).add((String) group);

							} catch (Exception ex) {
								throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_INVALID_FORMAT"), "inheritance", thisGrp.getName(), dataHolder.getName()), ex);
							}
						});
					}

					// INFO section.
					if (infoBlob != null) {

						Map<String, Object> info = new HashMap<>();

						Arrays.stream(infoBlob.split(",")).forEach(node -> {
							String[] split = node.split("\\|");

							info.put(split[0], (split.length == 2) ? split[1] : new String());
						});
						thisGrp.setVariables(info);
					}


				}

				/*
				 * Build the inheritance map and record any errors
				 */
				for (String group : inheritanceMap.keySet()) {
					List<String> inheritedList = inheritanceMap.get(group);
					Group thisGroup = dataHolder.getGroup(group);
					if (thisGroup != null)
						for (String inheritedKey : inheritedList) {
							if (inheritedKey != null) {
								Group inheritedGroup = dataHolder.getGroup(inheritedKey);
								if (inheritedGroup != null) {
									thisGroup.addInherits(inheritedGroup);
								} else
									GroupManager.logger.warning(String.format(Messages.getString("WorldDatHolder.WARN_INHERITED_NOT_FOUND"), inheritedKey, thisGroup.getName(), dataHolder.getName()));
							}
						}
				}

				if (dataHolder.getDefaultGroup() == null) {
					throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_NO_DEFAULT"), dataHolder.getName()));
				}

			} catch (SQLException e) {
				e.printStackTrace();
			}

			// Fetch the time stamp for this data.
			return timeStampQuery(tableName);

		}).thenAccept((changed) -> {

			// Some data was updated.
			if (changed != null) {
				dataHolder.getGroupsObject().setTimeStamp(changed);
				dataHolder.removeGroupsChangedFlag();
			}

			if (GroupManager.isLoaded())
				GroupManager.getGMEventHandler().callEvent(GMSystemEvent.Action.SAVED);
		});
	}

	@Override
	public void loadUsers(WorldDataHolder dataHolder) throws Exception {

		loadUsersAsync(dataHolder).get();
	}

	public CompletableFuture<Void> loadUsersAsync(WorldDataHolder dataHolder) {

		return CompletableFuture.supplyAsync(() -> {

			String tableName = (GroupManager.getGMConfig().getDatabaseGroup() + "_" + dataHolder.getName() + "_USERS").toUpperCase();

			// Read all groups from SQL.
			try (Connection conn = hikari.getConnection();
					PreparedStatement query = conn.prepareStatement(String.format(this.statements.getSelectAll(), tableName))) {

				ResultSet result = query.executeQuery();

				while (result.next()) {

					// UUID,LASTNAME,PRIMARYGROUP,SUBGROUPS,PERMISSIONS,INFO

					String id = result.getString("UUID");
					String lastName = result.getString("LASTNAME");
					String group = result.getString("PRIMARYGROUP");
					String subGroups = result.getString("SUBGROUPS");
					String permissions = result.getString("PERMISSIONS");
					String infoBlob = result.getString("INFO");

					User user = dataHolder.createUser(id);
					if (user == null) {
						throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_DUPLICATE_USER"), id, dataHolder.getName()));
					}

					if (lastName != null) {
						user.setLastName(lastName);
					}

					// Add all permissions.
					if (permissions != null) {
						Arrays.stream(permissions.split(",")).forEach(perm -> {

							if (perm.contains("|")) {
								String[] split = perm.split("\\|");
								try {
									user.addTimedPermission(split[0], Long.parseLong(split[1]));
								} catch (Exception e) {
									GroupManager.logger.warning("Timed Permission error: " + perm);
								}
							} else {
								/*
								 * add a standard permission
								 */
								user.addPermission(perm);
							}
						});
					}

					if (group != null) {
						Group hisGroup = dataHolder.getGroup(group);
						if (hisGroup == null) {
							GroupManager.logger.log(Level.WARNING, String.format(Messages.getString("WorldDatHolder.WARN_NO_GROUP_STATED"), group, user.getLastName(), dataHolder.getDefaultGroup().getName(), dataHolder.getName()));
							hisGroup = dataHolder.getDefaultGroup();
						}
						user.setGroup(hisGroup);
					} else {
						user.setGroup(dataHolder.getDefaultGroup());
					}

					// Add sub-groups.
					if (subGroups != null) {
						Arrays.stream(subGroups.split(",")).forEach(sub -> {

							if (sub.contains("|")) {
								String[] split = sub.split("\\|");
								try {
									Group subGrp = dataHolder.getGroup(split[0]);
									user.addTimedSubGroup(subGrp, Long.parseLong(split[1]));
								} catch (Exception e) {
									GroupManager.logger.warning("TimedSubGroup error: " + sub);
								}
							} else {
								Group subGrp = dataHolder.getGroup(sub);
								if (subGrp != null)
									user.addSubGroup(subGrp);
							}
						});
					}

					// INFO section.
					if (infoBlob != null) {

						Map<String, Object> info = new HashMap<>();

						Arrays.stream(infoBlob.split(",")).forEach(node -> {
							String[] split = node.split("\\|");

							info.put(split[0], (split.length == 2) ? split[1] : new String());
						});
						user.setVariables(info);
					}


				}

			} catch (SQLException e) {
				e.printStackTrace();
			}

			// Fetch the time stamp for this data.
			return timeStampQuery(tableName);

		}).thenAccept((changed) -> {

			// Some data was updated.
			if (changed != null) {
				dataHolder.getUsersObject().setTimeStamp(changed);
				dataHolder.removeUsersChangedFlag();
			}

			if (GroupManager.isLoaded())
				GroupManager.getGMEventHandler().callEvent(GMSystemEvent.Action.SAVED);
		});
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

		try {
			reloadGroupsAsync(dataHolder).get(); // Wait for result
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}

	public CompletableFuture<Void> reloadGroupsAsync(WorldDataHolder dataHolder) {

		return CompletableFuture.supplyAsync(() -> {

			WorldDataHolder ph = null;

			GroupManager.setLoaded(false);

			try {
				// temporary holder in case the load fails.
				ph = new WorldDataHolder(dataHolder.getName());

				// Wait on the data.
				loadGroupsAsync(ph).get();

			} catch (Exception ex) {
				Logger.getLogger(WorldDataHolder.class.getName()).log(Level.SEVERE, null, ex);
				ph = null;
			}

			return ph;
		}).thenAccept((ph) -> {

			if (ph != null) {
				// transfer new data
				dataHolder.resetGroups();
				
				for (Group tempGroup : ph.getGroupList()) {
					tempGroup.clone(dataHolder);
				}
				
				dataHolder.setDefaultGroup(dataHolder.getGroup(ph.getDefaultGroup().getName()));
				dataHolder.removeGroupsChangedFlag();
				dataHolder.getGroupsObject().setTimeStamp(ph.getGroupsObject().getTimeStamp());
			}
			GroupManager.setLoaded(true);
			GroupManager.getGMEventHandler().callEvent(GMSystemEvent.Action.RELOADED);
		});
	}

	@Override
	public void reloadUsers(WorldDataHolder dataHolder) {

		try {
			reloadUsersAsync(dataHolder).get(); // Wait for result
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}

	public CompletableFuture<Void> reloadUsersAsync(WorldDataHolder dataHolder) {

		return CompletableFuture.supplyAsync(() -> {

			WorldDataHolder ph = null;

			GroupManager.setLoaded(false);

			try {
				// temporary holder in case the load fails.
				ph = new WorldDataHolder(dataHolder.getName());
				
				// copy groups for reference
				for (Group tempGroup : dataHolder.getGroupList()) {
					tempGroup.clone(ph);
				}
				
				// setup the default group before loading user data.
				ph.setDefaultGroup(ph.getGroup(dataHolder.getDefaultGroup().getName()));

				// Wait on the data.
				loadUsersAsync(ph).get();

			} catch (Exception ex) {
				Logger.getLogger(WorldDataHolder.class.getName()).log(Level.SEVERE, null, ex);
				ph = null;
			}

			return ph;
		}).thenAccept((ph) -> {

			if (ph != null) {
				// transfer new data
				dataHolder.resetUsers();
				
				for (User tempUser : ph.getUserList()) {
					tempUser.clone(dataHolder);
				}

				dataHolder.removeUsersChangedFlag();
				dataHolder.getUsersObject().setTimeStamp(ph.getUsersObject().getTimeStamp());
			}
			GroupManager.setLoaded(true);
			GroupManager.getGMEventHandler().callEvent(GMSystemEvent.Action.RELOADED);
		});
	}

	@Override
	public void saveGroups(WorldDataHolder dataHolder) {

		if (GroupManager.getGMConfig().getAccessType() == ACCESS_LEVEL.READ || !dataHolder.haveGroupsChanged()) return;

		CompletableFuture.supplyAsync(() -> {

			Long changed = null;

			String tableName = (GroupManager.getGMConfig().getDatabaseGroup() + "_" + dataHolder.getName() + "_GROUPS").toUpperCase();

			// Batch push any changed Group data to the database.
			try (Connection conn = hikari.getConnection();
					PreparedStatement insert = conn.prepareStatement(String.format(this.statements.getInsertReplaceGroup(), tableName));) {

				conn.setAutoCommit(false);

				SortedMap<String, Group> groups = dataHolder.getGroups();
				Set<String> keys = groups.keySet();

				synchronized (groups) {
					Iterator<String> iter = keys.iterator(); // Must be in synchronized block
					while (iter.hasNext()) {
						Group group = groups.get(iter.next());

						if (!group.isChanged()) continue;

						boolean isDefault = group.equals(dataHolder.getDefaultGroup());
						changed = Instant.now().toEpochMilli();

						String name = group.getName();
						List<String> permissions = group.getSavePermissionList();
						List<String> inheritance = group.getInherits();
						String variables = group.getVariables().getBlob();

						insert.setString(1, name);
						insert.setBoolean(2, isDefault);
						insert.setString(3, permissions.isEmpty() ? null : StringUtils.join(permissions, ","));
						insert.setString(4, inheritance.isEmpty() ? null : StringUtils.join(inheritance, ","));
						insert.setString(5, variables.length() == 0 ? null : variables);

						insert.addBatch();
					}
				}

				// Execute batch update.
				if (changed != null) {
					int[] result = insert.executeBatch();

					GroupManager.logger.info("Batch save Groups: " + Arrays.toString(result));
					conn.commit();
				}
				conn.setAutoCommit(true);

				if (changed == null) {
					// No data was saved so it must require deleting
					changed = deleteGroups(dataHolder, tableName);
				}
				// Update changed timeStamp in SQL table.
				updateTableTimeStamp(tableName, changed);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return changed;

		}).thenAccept((changed) -> {

			// Some data was updated.
			if (changed != null) {
				dataHolder.getGroupsObject().setTimeStamp(changed);
				dataHolder.removeGroupsChangedFlag();
			}

			if (GroupManager.isLoaded())
				GroupManager.getGMEventHandler().callEvent(GMSystemEvent.Action.SAVED);
		});
	}

	private Long deleteGroups(WorldDataHolder dataHolder, String tableName) {

		Set<String> databaseGroups = new HashSet<>();

		try (Connection conn = hikari.getConnection();
				PreparedStatement query = conn.prepareStatement("SELECT NAME FROM " + tableName + ";")) {

			ResultSet result = query.executeQuery();

			while (result.next()) {
				databaseGroups.add(result.getString(1));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Batch delete Groups in SQL which are no longer valid in our data.

		Long changed = null;

		try (Connection conn = hikari.getConnection();
				PreparedStatement insert = conn.prepareStatement("DELETE FROM " + tableName + " WHERE NAME = '?';")) {

			conn.setAutoCommit(false);

			Set<String> keys = dataHolder.getGroups().keySet();

			for (String name : databaseGroups) {
				if (!keys.contains(name)) {

					changed = Instant.now().toEpochMilli();

					insert.setString(1, name);
					insert.addBatch();
				}
			}

			if (changed != null) {
				int[] result = insert.executeBatch();

				GroupManager.logger.info("Batch delete Groups: " + Arrays.toString(result));
				conn.commit();
			}
			conn.setAutoCommit(true);

			if (changed == null) {
				// No data was deleted so catch up
				changed = Instant.now().toEpochMilli();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return changed;
	}

	@Override
	public void saveUsers(WorldDataHolder dataHolder) {

		if (GroupManager.getGMConfig().getAccessType() == ACCESS_LEVEL.READ || !dataHolder.haveUsersChanged()) return;

		CompletableFuture.supplyAsync(() -> {

			Long changed = null;

			String tableName = (GroupManager.getGMConfig().getDatabaseGroup() + "_" + dataHolder.getName() + "_USERS").toUpperCase();

			// Batch push any changed User data to the database.
			try (Connection conn = hikari.getConnection();
					PreparedStatement insert = conn.prepareStatement(String.format(this.statements.getInsertReplaceUser(), tableName))) {

				conn.setAutoCommit(false);

				SortedMap<String, User> users = dataHolder.getUsers();
				Set<String> keys = users.keySet();

				synchronized (users) {
					Iterator<String> iter = keys.iterator(); // Must be in synchronized block
					while (iter.hasNext()) {
						User user = users.get(iter.next());

						if ((user.getGroup() == null || user.getGroup().equals(dataHolder.getDefaultGroup())) && user.getPermissionList().isEmpty() && user.getVariables().isEmpty() && user.isSubGroupsEmpty()) {
							continue;
						}

						if (!user.isChanged()) {
							continue;
						}

						changed = Instant.now().toEpochMilli();

						// UUID,LASTNAME,PRIMARYGROUP,SUBGROUPS,PERMISSIONS,INFO

						String UUID = user.getUUID();
						String name = user.getLastName();
						List<String> subGroups = user.getSaveSubGroupsList();
						List<String> permissions = user.getSavePermissionList();
						String variables = user.getVariables().getBlob();

						insert.setString(1, UUID);
						insert.setString(2, (name.equals(UUID)) ? null : name);
						insert.setString(3, user.getGroupName());
						insert.setString(4, subGroups.isEmpty() ? null : StringUtils.join(subGroups, ","));
						insert.setString(5, permissions.isEmpty() ? null : StringUtils.join(permissions, ","));
						insert.setString(6, variables.length() == 0 ? null : variables);

						insert.addBatch();
					}
				}

				if (changed != null) {
					int[] result = insert.executeBatch();

					GroupManager.logger.info("Batch save Users: " + Arrays.toString(result));
					conn.commit();
				}
				conn.setAutoCommit(true);

				if (changed == null) {
					// No data was saved so it must require deleting
					changed = deleteUsers(dataHolder, tableName);
				}

				// Update changed timeStamp in SQL table.
				updateTableTimeStamp(tableName, changed);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return changed;

		}).thenAccept((changed) -> {

			// Some data was updated.
			if (changed != null) {
				dataHolder.getUsersObject().setTimeStamp(changed);
				dataHolder.removeUsersChangedFlag();
			}

			if (GroupManager.isLoaded())
				GroupManager.getGMEventHandler().callEvent(GMSystemEvent.Action.SAVED);
		});
	}

	private Long deleteUsers(WorldDataHolder dataHolder, String tableName) {

		Set<String> databaseUUIDs = new HashSet<>();

		try (Connection conn = hikari.getConnection();
				PreparedStatement query = conn.prepareStatement("SELECT UUID FROM " + tableName + ";")) {

			ResultSet result = query.executeQuery();

			while (result.next()) {
				databaseUUIDs.add(result.getString(1));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Batch delete users in SQL who are no longer valid in our data.

		Long changed = null;

		try (Connection conn = hikari.getConnection();
				PreparedStatement insert = conn.prepareStatement("DELETE FROM " + tableName + " WHERE UUID = ?;")) {

			conn.setAutoCommit(false);

			Set<String> keys = dataHolder.getUsers().keySet();

			for (String UUID : databaseUUIDs) {
				if (!keys.contains(UUID)) {

					changed = Instant.now().toEpochMilli();

					insert.setString(1, UUID);
					insert.addBatch();
				}
			}

			if (changed != null) {
				int[] result = insert.executeBatch();

				GroupManager.logger.info("Batch delete Users: " + Arrays.toString(result));
				conn.commit();
			}
			conn.setAutoCommit(true);

			if (changed == null) {
				// No data was deleted so catch up
				changed = Instant.now().toEpochMilli();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return changed;
	}

	/**
	 * Store the new time stamp for this table.
	 * 
	 * @param tableName	a table name.
	 * @param timeStamp	a new timeStamp.
	 */
	private void updateTableTimeStamp(String tableName, Long timeStamp) {

		try (Connection conn = hikari.getConnection();
				PreparedStatement insert = conn.prepareStatement(String.format(this.statements.getInsertReplaceUpdate(), UPDATE_TABLE))) {

			insert.setString(1, tableName);
			insert.setLong(2, timeStamp);
			insert.executeUpdate();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public boolean hasNewGlobalGroupsData() {

		return GroupManager.getGlobalGroups().getTimeStampGroups() < timeStampQuery(GLOBALGROUPS_TABLE);
	}

	@Override
	public boolean hasNewGroupsData(WorldDataHolder dataHolder) {

		return dataHolder.getGroupsObject().getTimeStamp() < getDatabaseTimeStampGroups(dataHolder);
	}

	@Override
	public boolean hasNewUsersData(WorldDataHolder dataHolder) {

		return dataHolder.getUsersObject().getTimeStamp() < getDatabaseTimeStampUsers(dataHolder);
	}

	/**
	 * Helper method to retrieve a Groups table time stamp.
	 * 
	 * This method should only be called Async.
	 * 
	 * @param dataHolder	a WorldDataHolder containing groups/users.
	 * @return	a long value millis.
	 */
	private long getDatabaseTimeStampGroups(WorldDataHolder dataHolder) {

		String tableName = (GroupManager.getGMConfig().getDatabaseGroup() + "_" + dataHolder.getName() + "_GROUPS").toUpperCase();

		return timeStampQuery(tableName);
	}

	/**
	 * Helper method to retrieve a Users table time stamp.
	 * 
	 * This method should only be called Async.
	 * 
	 * @param dataHolder	a WorldDataHolder containing groups/users.
	 * @return	a long value millis.
	 */
	private long getDatabaseTimeStampUsers(WorldDataHolder dataHolder) {

		String tableName = (GroupManager.getGMConfig().getDatabaseGroup() + "_" + dataHolder.getName() + "_USERS").toUpperCase();

		return timeStampQuery(tableName);
	}

	/**
	 * Fetch from SQL any time stamp for this table.
	 * 
	 * This method should only be called Async.
	 * 
	 * @param tableName	the SQL table name to check for a time stamp.
	 * @return	a long value millis.
	 */
	private long timeStampQuery(String tableName) {

		long timeStamp = 0L;

		/*if (Bukkit.isPrimaryThread()) {
		    System.out.println("*** Warning: timeStampQuery running on the main Thread ***");
		    Thread.dumpStack();
		  }*/

		// Fetch the time stamp for this table.
		try (Connection conn = hikari.getConnection();
				PreparedStatement query = conn.prepareStatement(String.format(this.statements.getSelectTimeStamp(), UPDATE_TABLE, tableName))) {

			ResultSet result = query.executeQuery();
			if (result.next()) {
				timeStamp = result.getLong("UPDATED");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return timeStamp;
	}

	@Override
	public void backup(OverloadedWorldHolder world, BACKUP_TYPE type) { /* Not our job */ }

	@Override
	public void purgeBackups() { /* Not our job */ }

}
