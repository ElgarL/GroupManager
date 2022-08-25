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
package org.anjocaido.groupmanager.dataholder.worlds;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.data.User;
import org.anjocaido.groupmanager.dataholder.OverloadedWorldHolder;
import org.anjocaido.groupmanager.dataholder.WorldDataHolder;
import org.anjocaido.groupmanager.localization.Messages;
import org.anjocaido.groupmanager.permissions.AnjoPermissionsHandler;
import org.anjocaido.groupmanager.storage.DataSource;
import org.anjocaido.groupmanager.storage.statements.Statements;
import org.anjocaido.groupmanager.utils.BukkitWrapper;
import org.bukkit.entity.Player;

/**
 * 
 * @author gabrielcouto, ElgarL
 */
public abstract class WorldsHolder extends ChildMirrors {

	/**
	 * Map with instances of loaded worlds.
	 */
	private Map<String, OverloadedWorldHolder> worldsData = Collections.synchronizedMap(new LinkedHashMap<>());

	protected String serverDefaultWorldName;

	private final GroupManager plugin;
	protected final Logger logger;
	private DataSource dataSource;

	/**
	 * Throw any exceptions as we want to prevent
	 * the plugin loading if the database fails.
	 * 
	 * @param plugin
	 * @throws SQLException 
	 */
	protected WorldsHolder(GroupManager plugin) throws Exception {

		this.plugin = plugin;
		this.logger = plugin.getLogger();

		dataSource = Statements.getSource(plugin);
	}

	/**
	 * Parse the mirrorsMap and setup data for all worlds.
	 */
	public abstract void parseMirrors();

	public void addWorldData(String key, OverloadedWorldHolder worldData) {

		worldsData.put(key.toLowerCase(), worldData);
	}

	/**
	 * Erase any loaded data and perform a fresh load.
	 */
	public void resetWorldsHolder() {

		worldsData = Collections.synchronizedMap(new LinkedHashMap<>());
		clearGroupsMirror();
		clearUsersMirror();

		// Setup mirrors and load all data.
		initialLoad();

		if (serverDefaultWorldName == null)
			throw new IllegalStateException(Messages.getString("WorldsHolder.ERROR_NO_DEFAULT_GROUP")); //$NON-NLS-1$
	}

	private void initialLoad() {

		// Initialize our DataSource.
		detectDefaultWorldName();
		// Load all worlds and mirrors as defined in config.yml
		parseMirrors();
		// search the worlds folder for any manually created worlds (not listed in config.yml)
		dataSource.loadAllSearchedWorlds();
	}

	private void detectDefaultWorldName() {

		/* Do not use the folder name if this
		 * is a Bukkit Forge server.
		 */
		if (plugin.getServer().getName().equalsIgnoreCase("BukkitForge")) { //$NON-NLS-1$
			serverDefaultWorldName = "overworld"; //$NON-NLS-1$

		} else {
			Properties server = new Properties();
			try {
				server.load(new FileInputStream("server.properties")); //$NON-NLS-1$
				serverDefaultWorldName = server.getProperty("level-name").toLowerCase(); //$NON-NLS-1$
			} catch (IOException ex) {
				GroupManager.logger.log(Level.SEVERE, "Failed to load server.properties", ex);
			}
		}
	}

	/**
	 * Load data for all listed parent worlds.
	 */
	public void loadParentWorlds() {

		for (String world : worldsData.keySet()) {
			dataSource.init(world);
			dataSource.loadWorld(world, false);
		}
	}

	/**
	 *
	 */
	public void reloadAll() {

		// Load global groups
		GroupManager.getGlobalGroups().load();

		ArrayList<WorldDataHolder> alreadyDone = new ArrayList<>();
		for (WorldDataHolder w : worldsData.values()) {
			if (alreadyDone.contains(w)) {
				continue;
			}
			if (!hasGroupsMirror(w.getName()))
				dataSource.reloadGroups(w);

			if (!hasUsersMirror(w.getName()))
				dataSource.reloadUsers(w);

			alreadyDone.add(w);
		}
	}

	/**
	 *
	 * @param worldName
	 */
	public void reloadWorld(String worldName) {

		if (!hasGroupsMirror(worldName))
			dataSource.reloadGroups(getWorldData(worldName));

		if (!hasUsersMirror(worldName))
			dataSource.reloadUsers(getWorldData(worldName));
	}

	/*
	 * Never call this. The Only access is via GM's clean up thread.
	 */
	public boolean purgeExpiredPerms() {

		ArrayList<WorldDataHolder> alreadyDone = new ArrayList<>();
		boolean result = false;

		for (WorldDataHolder world : worldsData.values()) {

			if (alreadyDone.contains(world)) {
				continue;
			}

			/*
			* Update individual player permissions if changed.
			*/
			if (world.purgeTimedPermissions()) {
				result = true;

				for (User user : world.getUserList()) {
					// If the player is online, this will create new data for the user.
					Player targetPlayer = BukkitWrapper.getInstance().getPlayer(user.getLastName());
					if (targetPlayer != null)
						GroupManager.getBukkitPermissions().updatePermissions(targetPlayer);
				}
			}
			alreadyDone.add(world);
		}
		return result;
	}

	/**
	 * Wrapper to retain backwards compatibility
	 * (call this function to auto overwrite files)
	 */
	public void saveChanges() {

		saveChanges(true);
	}

	/**
	 *
	 */
	public boolean saveChanges(boolean overwrite) {

		boolean changed = false;
		ArrayList<WorldDataHolder> alreadyDone = new ArrayList<>();

		// Remove old backups.
		dataSource.purgeBackups();

		/*
		 * Save Global Groups
		 */
		if (GroupManager.getGlobalGroups().haveGroupsChanged()) {
			dataSource.backup(null, DataSource.BACKUP_TYPE.GLOBALGROUPS);
			plugin.getWorldsHolder().getDataSource().saveGlobalGroups(overwrite);

		} else {
			if (overwrite || (!overwrite && dataSource.hasNewGlobalGroupsData())) {
				GroupManager.logger.log(Level.WARNING, Messages.getString("GlobalGroups.WARN_NEWER_GG_FOUND_LOADING")); //$NON-NLS-1$
				GroupManager.getGlobalGroups().load();
			}
		}

		/*
		 * Save each world.
		 */
		for (OverloadedWorldHolder w : worldsData.values()) {
			if (alreadyDone.contains(w)) {
				continue;
			}
			if (w == null) {
				GroupManager.logger.log(Level.SEVERE, Messages.getString("WorldsHolder.WHAT_HAPPENED")); //$NON-NLS-1$
				continue;
			}

			if (!hasGroupsMirror(w.getName())) {
				if (w.haveGroupsChanged()) {
					if (overwrite || (!overwrite && !dataSource.hasNewGroupsData(w))) {
						// Backup Groups file
						dataSource.backup(w, DataSource.BACKUP_TYPE.GROUPS);
						dataSource.saveGroups(w);
						changed = true;

					} else {
						// Newer file found.
						GroupManager.logger.log(Level.WARNING, String.format(Messages.getString("WorldsHolder.WARN_NEWER_GROUPS_FILE_UNABLE"), w.getName())); //$NON-NLS-1$
						throw new IllegalStateException(Messages.getString("ERROR_UNABLE_TO_SAVE")); //$NON-NLS-1$
					}
				} else {
					//Check for newer file as no local changes.
					if (dataSource.hasNewGroupsData(w)) {
						GroupManager.logger.log(Level.INFO, Messages.getString("WorldsHolder.NEWER_GROUPS_FILE_LOADING")); //$NON-NLS-1$

						dataSource.reloadGroups(w);
						changed = true;
					}
				}
			}

			if (!hasUsersMirror(w.getName())) {
				if (w.haveUsersChanged()) {
					if (overwrite || (!overwrite && !dataSource.hasNewUsersData(w))) {
						// Backup Users file
						dataSource.backup(w, DataSource.BACKUP_TYPE.USERS);
						dataSource.saveUsers(w);
						changed = true;

					} else {
						// Newer file found.
						GroupManager.logger.log(Level.WARNING, Messages.getString("WorldsHolder.WARN_NEWER_USERS_FILE_UNABLE") + w.getName()); //$NON-NLS-1$
						throw new IllegalStateException(Messages.getString("ERROR_UNABLE_TO_SAVE")); //$NON-NLS-1$
						}
					} else {
					// Check for newer file as no local changes.
					if (dataSource.hasNewUsersData(w)) {
						GroupManager.logger.log(Level.INFO, Messages.getString("WorldsHolder.NEWER_USERS_FILE_LOADING")); //$NON-NLS-1$

						dataSource.reloadUsers(w);
						changed = true;
					}
				}
			}
			alreadyDone.add(w);
		}
		return changed;
	}

	/**
	 * Returns the dataHolder for the given world.
	 * If the world is not on the worlds list, returns the default world
	 * holder.
	 * 
	 * Mirrors return their parent world data.
	 * If no mirroring data it returns the default world.
	 * 
	 * @param worldName
	 * @return OverloadedWorldHolder
	 */
	public OverloadedWorldHolder getWorldData(String worldName) {

		String worldNameLowered = worldName.toLowerCase();

		// Find this worlds data
		if (worldsData.containsKey(worldNameLowered))
			return getUpdatedWorldData(worldNameLowered);

		// Oddly no data source was found for this world so attempt to return the global mirror.
		if (worldsData.containsKey("all_unnamed_worlds")) { //$NON-NLS-1$
			GroupManager.logger.log(Level.FINEST, (String.format(Messages.getString("WorldsHolder.WORLD_NOT_FOUND_UNNAMED"), worldName))); //$NON-NLS-1$
			return getUpdatedWorldData("all_unnamed_worlds"); //$NON-NLS-1$
		}

		// Oddly no data source or global mirror was found for this world so return the default.
		GroupManager.logger.log(Level.FINEST, (String.format(Messages.getString("WorldsHolder.WORLD_NOT_FOUND_DEFAULT"), worldName))); //$NON-NLS-1$
		return getDefaultWorld();
	}

	/**
	 * Get the requested world data and update it's dataSource to be relevant
	 * for this world
	 * 
	 * @param worldName
	 * @return updated world holder
	 */
	private OverloadedWorldHolder getUpdatedWorldData(String worldName) {

		String worldNameLowered = worldName.toLowerCase();

		if (worldsData.containsKey(worldNameLowered)) {
			OverloadedWorldHolder data = worldsData.get(worldNameLowered);
			if (data != null) data.updateDataSource();
			return data;
		}
		return null;
	}

	/**
	 * Do a matching of playerName, if  one player is found, do
	 * getWorldData(player)
	 *
	 * @param playerName
	 * @return null if matching returned no player, or more than one.
	 */
	public OverloadedWorldHolder getWorldDataByPlayerName(String playerName) {

		List<Player> matchPlayer = plugin.getServer().matchPlayer(playerName);
		if (matchPlayer.size() == 1) {
			return getWorldData(matchPlayer.get(0));
		}
		return null;
	}

	/**
	 * Retrieves the field player.getWorld().getName() and do
	 * getWorld(worldName)
	 * 
	 * @param player
	 * @return OverloadedWorldHolder
	 */
	public OverloadedWorldHolder getWorldData(Player player) {

		return getWorldData(player.getWorld().getName());
	}

	/**
	 * It does getWorld(worldName).getPermissionsHandler()
	 * 
	 * @param worldName
	 * @return AnjoPermissionsHandler
	 */
	public AnjoPermissionsHandler getWorldPermissions(String worldName) {

		return getWorldData(worldName).getPermissionsHandler();
	}

	/**
	 * Returns the PermissionsHandler for this player data
	 * 
	 * @param player
	 * @return AnjoPermissionsHandler
	 */
	public AnjoPermissionsHandler getWorldPermissions(Player player) {

		return getWorldData(player).getPermissionsHandler();
	}

	/**
	 * It does getWorldDataByPlayerName(playerName).
	 * If it doesn't return null, it will return result.getPermissionsHandler()
	 *
	 * @param playerName
	 * @return null if the player matching gone wrong.
	 */
	public AnjoPermissionsHandler getWorldPermissionsByPlayerName(String playerName) {

		WorldDataHolder dh = getWorldDataByPlayerName(playerName);
		if (dh != null) {
			return dh.getPermissionsHandler();
		}
		return null;
	}

	/**
	 * Tells if the world has been mapped.
	 * 
	 * Returns true if world is a parent or mirror.
	 * 
	 * @param worldName
	 * @return true if world is loaded or mirrored. false if not listed
	 */
	public boolean isWorldKnown(String worldName) {

		return isParentWorld(worldName) || hasGroupsMirror(worldName) || hasUsersMirror(worldName);
	}

	/**
	 * Verify if world has it's own data (or partially mirrored).
	 *
	 * @param worldName
	 * @return true if it has its own holder. false if not.
	 */
	public boolean hasOwnData(String worldName) {

		String key = worldName.toLowerCase();
		return worldsData.containsKey(key) && worldsData.get(key) != null && (!hasGroupsMirror(key) || !hasUsersMirror(key));
	}

	/**
	 * Has data already been loaded for this world?
	 * Is a parent world.
	 * 
	 * @param name
	 * @return	true if data is found.
	 */
	public boolean isParentWorld(String name) {

		return worldsData.containsKey(name.toLowerCase());
	}

	/**
	 * @return the defaultWorld
	 */
	public OverloadedWorldHolder getDefaultWorld() {

		return getUpdatedWorldData(serverDefaultWorldName);
	}

	/**
	 * @return the dataSource
	 */
	public DataSource getDataSource() {

		return dataSource;
	}

	/**
	 * @param dataSource the dataSource to set
	 */
	public void setDataSource(DataSource dataSource) {

		this.dataSource = dataSource;
	}

	/**
	 * Returns all physically loaded worlds which have at least one of their own
	 * data sets for users or groups which isn't an identical mirror.
	 * 
	 * @return ArrayList<OverloadedWorldHolder> of all loaded worlds
	 */
	public ArrayList<OverloadedWorldHolder> allWorldsDataList() {

		ArrayList<OverloadedWorldHolder> list = new ArrayList<>();

		for (String world : worldsData.keySet()) {

			if (!world.equalsIgnoreCase("all_unnamed_worlds")) { //$NON-NLS-1$

				// Fetch the relevant world object
				OverloadedWorldHolder data = getWorldData(world);

				if (!list.contains(data)) {

					String worldNameLowered = data.getName().toLowerCase();
					String usersMirror = getUsersMirror(worldNameLowered);
					String groupsMirror = getGroupsMirror(worldNameLowered);

					// is users mirrored?
					if (usersMirror != null) {

						// If both are mirrored
						if (groupsMirror != null) {

							// if the data sources are the same, return the parent
							if (usersMirror.equals(groupsMirror)) {
								data = getWorldData(usersMirror.toLowerCase());

								// Only add the parent if it's not already listed.
								if (!list.contains(data))
									list.add(data);

								continue;
							}
							// Both data sources are mirrors, but they are from different parents
							// so fall through to add the actual data object.
						}
						// Groups isn't a mirror so fall through to add this this worlds data source
					}

					// users isn't mirrored so we need to add this worlds data source
					list.add(data);
					}
				}
			}
		return list;
	}
}
