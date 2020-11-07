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

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.data.User;
import org.anjocaido.groupmanager.dataholder.OverloadedWorldHolder;
import org.anjocaido.groupmanager.dataholder.WorldDataHolder;
import org.anjocaido.groupmanager.localization.Messages;
import org.anjocaido.groupmanager.permissions.AnjoPermissionsHandler;
import org.anjocaido.groupmanager.utils.Tasks;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author gabrielcouto, ElgarL
 */
public class WorldsHolder {

	/**
	 * Map with instances of loaded worlds.
	 */
	private Map<String, OverloadedWorldHolder> worldsData = new HashMap<>();

	/**
	 * Map of mirrors: <nonExistingWorldName, existingAndLoadedWorldName>
	 * The key is the mirror.
	 * The object is the mirrored.
	 * 
	 * Mirror shows the same data of mirrored.
	 */
	private Map<String, String> mirrorsGroup = new HashMap<>();
	private Map<String, String> mirrorsUser = new HashMap<>();

	private String serverDefaultWorldName;
	private final GroupManager plugin;
	private File worldsFolder;

	/**
	 * 
	 * @param plugin
	 */
	public WorldsHolder(GroupManager plugin) {

		this.plugin = plugin;
		resetWorldsHolder();
	}
	
	/**
	 * @return the mirrorsGroup
	 */
	public Map<String, String> getMirrorsGroup() {
	
		return mirrorsGroup;
	}

	
	/**
	 * @return the mirrorsUser
	 */
	public Map<String, String> getMirrorsUser() {
	
		return mirrorsUser;
	}
	
	/**
	 * Has data already been loaded for this world?
	 * 
	 * @param name
	 * @return	true if data is found.
	 */
	public boolean isWorldKnown(String name) {
		
		return worldsData.containsKey(name.toLowerCase());
	}
	
	/**
	 * Erase any loaded data and perform a fresh load from file.
	 */
	public void resetWorldsHolder() {
		
		worldsData = new HashMap<>();
		mirrorsGroup = new HashMap<>();
		mirrorsUser = new HashMap<>();
		
		// Setup folders and check files exist for the primary world
		verifyFirstRun();
		initialLoad();
		if (serverDefaultWorldName == null)
			throw new IllegalStateException(Messages.getString("WorldsHolder.ERROR_NO_DEFAULT_GROUP")); //$NON-NLS-1$
	}

	private void initialLoad() {

		// load the initial world
		initialWorldLoading();
		// Configure and load any mirrors and additional worlds as defined in config.yml
		mirrorSetUp();
		// search the worlds folder for any manually created worlds (not listed in config.yml)
		loadAllSearchedWorlds();
	}

	private void initialWorldLoading() {

		//Load the default world
		loadWorld(serverDefaultWorldName);
		//defaultWorld = getUpdatedWorldData(serverDefaultWorldName);
	}

	private void loadAllSearchedWorlds() {

		/*
		 * Read all known worlds from Bukkit Create the data files if they don't
		 * already exist, and they are not mirrored.
		 */
		for (World world : plugin.getServer().getWorlds()) {
			GroupManager.logger.log(Level.FINE, String.format(Messages.getString("WorldsHolder.CHECKING_DATA"), world.getName())); //$NON-NLS-1$
			if ((!worldsData.containsKey(world.getName().toLowerCase())) && ((!mirrorsGroup.containsKey(world.getName().toLowerCase())) || (!mirrorsUser.containsKey(world.getName().toLowerCase())))) {

				if (worldsData.containsKey("all_unnamed_worlds")) { //$NON-NLS-1$
					
					String usersMirror = mirrorsUser.get("all_unnamed_worlds"); //$NON-NLS-1$
					String groupsMirror = mirrorsGroup.get("all_unnamed_worlds"); //$NON-NLS-1$
					
					if (usersMirror != null)
						mirrorsUser.put(world.getName().toLowerCase(), usersMirror);
					
					if (groupsMirror != null)
						mirrorsGroup.put(world.getName().toLowerCase(), groupsMirror);
					
				}
				
				GroupManager.logger.log(Level.FINE, String.format(Messages.getString("WorldsHolder.CREATING_FOLDERS"), world.getName())); //$NON-NLS-1$
				setupWorldFolder(world.getName());
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
				 * don't load any worlds which are already loaded or fully
				 * mirrored worlds that don't need data.
				 */
				if (!worldsData.containsKey(folder.getName().toLowerCase()) && ((!mirrorsGroup.containsKey(folder.getName().toLowerCase())) || (!mirrorsUser.containsKey(folder.getName().toLowerCase())))) {
					/*
					 * Call setupWorldFolder to check case sensitivity and
					 * convert to lower case, before we attempt to load this
					 * world.
					 */
					setupWorldFolder(folder.getName());
					loadWorld(folder.getName().toLowerCase());
				}

			}
		}
	}

	@SuppressWarnings("rawtypes")
	public void mirrorSetUp() {

		mirrorsGroup.clear();
		mirrorsUser.clear();
		Map<String, Object> mirrorsMap = GroupManager.getGMConfig().getMirrorsMap();

		HashSet<String> mirroredWorlds = new HashSet<>();

		if (mirrorsMap != null) {
			for (String source : mirrorsMap.keySet()) {
				// Make sure all non mirrored worlds have a set of data files.
				setupWorldFolder(source);
				// Load the world data
				if (!worldsData.containsKey(source.toLowerCase()))
					loadWorld(source);

				if (mirrorsMap.get(source) instanceof ArrayList) {
					ArrayList mirrorList = (ArrayList) mirrorsMap.get(source);

					// These worlds fully mirror their parent
					for (Object o : mirrorList) {
						String world = o.toString().toLowerCase();
						if (!world.equalsIgnoreCase(serverDefaultWorldName)) {
							try {
								mirrorsGroup.remove(world);
								mirrorsUser.remove(world);
							} catch (Exception ignored) {
							}
							mirrorsGroup.put(world, getWorldData(source).getName());
							mirrorsUser.put(world, getWorldData(source).getName());

							// Track this world so we can create a datasource for it later
							mirroredWorlds.add(o.toString());

						} else
							GroupManager.logger.log(Level.WARNING, String.format(Messages.getString("WorldsHolder.MIRRORING_ERROR"), o.toString())); //$NON-NLS-1$
					}
				} else if (mirrorsMap.get(source) instanceof Map) {
					Map subSection = (Map) mirrorsMap.get(source);

					for (Object key : subSection.keySet()) {

						if (!((String)key).equalsIgnoreCase(serverDefaultWorldName)) {

							if (subSection.get(key) instanceof ArrayList) {
								ArrayList mirrorList = (ArrayList) subSection.get(key);

								// These worlds have defined mirroring
								for (Object o : mirrorList) {
									String type = o.toString().toLowerCase();
									try {
										if (type.equals("groups")) //$NON-NLS-1$
											mirrorsGroup.remove(((String)key).toLowerCase());

										if (type.equals("users")) //$NON-NLS-1$
											mirrorsUser.remove(((String)key).toLowerCase());

									} catch (Exception ignored) {
									}
									if (type.equals("groups")) { //$NON-NLS-1$
										mirrorsGroup.put(((String)key).toLowerCase(), getWorldData(source).getName());
										GroupManager.logger.log(Level.FINE, String.format(Messages.getString("WorldsHolder.ADDING_GROUPS_MIRROR"), key.toString())); //$NON-NLS-1$
									}

									if (type.equals("users")) { //$NON-NLS-1$
										mirrorsUser.put(((String)key).toLowerCase(), getWorldData(source).getName());
										GroupManager.logger.log(Level.FINE, String.format(Messages.getString("WorldsHolder.ADDING_USERS_MIRROR"), key.toString())); //$NON-NLS-1$
									}
								}

								// Track this world so we can create a datasource for it later
								mirroredWorlds.add((String)key);

							} else
								throw new IllegalStateException(String.format(Messages.getString("WorldsHolder.UNKNOWN_MIRRORING_FORMAT"), key)); //$NON-NLS-1$

						} else {
							GroupManager.logger.log(Level.WARNING, String.format(Messages.getString("WorldsHolder.MIRRORING_ERROR"), key)); //$NON-NLS-1$
						}

					}
				}
			}

			// Create a datasource for any worlds not already loaded
			for (String world : mirroredWorlds) {
				if (!worldsData.containsKey(world.toLowerCase())) {
					GroupManager.logger.log(Level.FINE, String.format(Messages.getString("WorldsHolder.NO_DATA"), world)); //$NON-NLS-1$
					setupWorldFolder(world);
					loadWorld(world, true);
				}
			}
		}
	}

	/**
     *
     */
	public void reloadAll() {

		// Load global groups
		GroupManager.getGlobalGroups().load();

		ArrayList<WorldDataHolder> alreadyDone = new ArrayList<WorldDataHolder>();
		for (WorldDataHolder w : worldsData.values()) {
			if (alreadyDone.contains(w)) {
				continue;
			}
			if (!mirrorsGroup.containsKey(w.getName().toLowerCase()))
				w.reloadGroups();
			if (!mirrorsUser.containsKey(w.getName().toLowerCase()))
				w.reloadUsers();

			alreadyDone.add(w);
		}

	}

	/**
	 *
	 * @param worldName
	 */
	public void reloadWorld(String worldName) {

		if (!mirrorsGroup.containsKey(worldName.toLowerCase()))
			getWorldData(worldName).reloadGroups();
		if (!mirrorsUser.containsKey(worldName.toLowerCase()))
			getWorldData(worldName).reloadUsers();
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
				
				for (User user: world.getUserList()) {
					// If the player is online, this will create new data for the user.
					Player targetPlayer = plugin.getServer().getPlayer(user.getLastName());
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
		Tasks.removeOldFiles(plugin.getBackupFolder());

		// Write Global Groups
		if (GroupManager.getGlobalGroups().haveGroupsChanged()) {
			GroupManager.getGlobalGroups().writeGroups(overwrite);
		} else {
			if (GroupManager.getGlobalGroups().getTimeStampGroups() < GroupManager.getGlobalGroups().getGlobalGroupsFile().lastModified()) {
				GroupManager.logger.log(Level.WARNING,Messages.getString("GlobalGroups.WARN_NEWER_GG_FOUND_LOADING")); //$NON-NLS-1$
				GroupManager.getGlobalGroups().load();
			}
		}

		for (OverloadedWorldHolder w : worldsData.values()) {
			if (alreadyDone.contains(w)) {
				continue;
			}
			if (w == null) {
				GroupManager.logger.severe(Messages.getString("WorldsHolder.WHAT_HAPPENED")); //$NON-NLS-1$
				continue;
			}
			if (!mirrorsGroup.containsKey(w.getName().toLowerCase()))
				if (w.haveGroupsChanged()) {
					if (overwrite || (!overwrite && (w.getTimeStampGroups() >= w.getGroupsFile().lastModified()))) {
						// Backup Groups file
						backupFile(w, true);

						WorldDataHolder.writeGroups(w, w.getGroupsFile());
						changed = true;
						//w.removeGroupsChangedFlag();
					} else {
						// Newer file found.
						GroupManager.logger.log(Level.WARNING, String.format(Messages.getString("WorldsHolder.WARN_NEWER_GROUPS_FILE_UNABLE"), w.getName())); //$NON-NLS-1$
						throw new IllegalStateException(Messages.getString("ERROR_UNABLE_TO_SAVE")); //$NON-NLS-1$
					}
				} else {
					//Check for newer file as no local changes.
					if (w.getTimeStampGroups() < w.getGroupsFile().lastModified()) {
						System.out.print(Messages.getString("WorldsHolder.NEWER_GROUPS_FILE_LOADING")); //$NON-NLS-1$
						// Backup Groups file
						backupFile(w, true);
						w.reloadGroups();
						changed = true;
					}
				}
			if (!mirrorsUser.containsKey(w.getName().toLowerCase()))
				if (w.haveUsersChanged()) {
					if (overwrite || (!overwrite && (w.getTimeStampUsers() >= w.getUsersFile().lastModified()))) {
						// Backup Users file
						backupFile(w, false);

						WorldDataHolder.writeUsers(w, w.getUsersFile());
						changed = true;
						//w.removeUsersChangedFlag();
					} else {
						// Newer file found.
						GroupManager.logger.log(Level.WARNING, Messages.getString("WorldsHolder.WARN_NEWER_USERS_FILE_UNABLE") + w.getName()); //$NON-NLS-1$
						throw new IllegalStateException(Messages.getString("ERROR_UNABLE_TO_SAVE")); //$NON-NLS-1$
					}
				} else {
					//Check for newer file as no local changes.
					if (w.getTimeStampUsers() < w.getUsersFile().lastModified()) {
						System.out.print(Messages.getString("WorldsHolder.NEWER_USERS_FILE_LOADING")); //$NON-NLS-1$
						// Backup Users file
						backupFile(w, false);
						w.reloadUsers();
						changed = true;
					}
				}
			alreadyDone.add(w);
		}
		return changed;
	}

	/**
	 * Backup the Groups/Users file
	 * 
	 * @param w
	 * @param groups
	 */
	private void backupFile(OverloadedWorldHolder w, Boolean groups) {

		File backupFile = new File(plugin.getBackupFolder(), "bkp_" + w.getName() + (groups ? "_g_" : "_u_") + Tasks.getDateString() + ".yml"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		try {
			Tasks.copy((groups ? w.getGroupsFile() : w.getUsersFile()), backupFile);
		} catch (IOException ex) {
			GroupManager.logger.log(Level.SEVERE, null, ex);
		}
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
			GroupManager.logger.finest(String.format(Messages.getString("WorldsHolder.WORLD_NOT_FOUND_UNNAMED"), worldName)); //$NON-NLS-1$
			return getUpdatedWorldData("all_unnamed_worlds"); //$NON-NLS-1$
		}
		
		// Oddly no data source or global mirror was found for this world so return the default.
		GroupManager.logger.finest(String.format(Messages.getString("WorldsHolder.WORLD_NOT_FOUND_DEFAULT"), worldName)); //$NON-NLS-1$
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
			data.updateDataSource();
			return data;
		}
		return null;

	}

	/**
	 * Do a matching of playerName, if its found only one player, do
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

	private void verifyFirstRun() {

		/* Do not use the folder name if this 
		 * is a Bukkit Forge server.
		 */
		if (plugin.getServer().getName().equalsIgnoreCase("BukkitForge")) { //$NON-NLS-1$
			serverDefaultWorldName = "overworld"; //$NON-NLS-1$

		} else {
			Properties server = new Properties();
			try {
				server.load(new FileInputStream(new File("server.properties"))); //$NON-NLS-1$
				serverDefaultWorldName = server.getProperty("level-name").toLowerCase(); //$NON-NLS-1$
			} catch (IOException ex) {
				GroupManager.logger.log(Level.SEVERE, null, ex);
			}
		}
		setupWorldFolder(serverDefaultWorldName);

	}

	public void setupWorldFolder(String worldName) {

		String worldNameLowered = worldName.toLowerCase();

		worldsFolder = new File(plugin.getDataFolder(), "worlds"); //$NON-NLS-1$
		if (!worldsFolder.exists()) {
			worldsFolder.mkdirs();
		}

		File defaultWorldFolder = new File(worldsFolder, worldNameLowered);
		if ((!defaultWorldFolder.exists()) && ((!mirrorsGroup.containsKey(worldNameLowered))) || (!mirrorsUser.containsKey(worldNameLowered))) {

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
			if (!mirrorsGroup.containsKey(worldNameLowered)) {
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

			if (!mirrorsUser.containsKey(worldNameLowered)) {
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

	/**
	 * Copies the specified world data to another world
	 *
	 * @param fromWorld
	 * @param toWorld
	 * @return true if successfully copied.
	 */
	public boolean cloneWorld(String fromWorld, String toWorld) {

		File fromWorldFolder = new File(worldsFolder, fromWorld.toLowerCase());
		File toWorldFolder = new File(worldsFolder, toWorld.toLowerCase());
		if (toWorldFolder.exists() || !fromWorldFolder.exists()) {
			return false;
		}
		File fromWorldGroups = new File(fromWorldFolder, "groups.yml"); //$NON-NLS-1$
		File fromWorldUsers = new File(fromWorldFolder, "users.yml"); //$NON-NLS-1$
		if (!fromWorldGroups.exists() || !fromWorldUsers.exists()) {
			return false;
		}
		File toWorldGroups = new File(toWorldFolder, "groups.yml"); //$NON-NLS-1$
		File toWorldUsers = new File(toWorldFolder, "users.yml"); //$NON-NLS-1$
		toWorldFolder.mkdirs();
		try {
			Tasks.copy(fromWorldGroups, toWorldGroups);
			Tasks.copy(fromWorldUsers, toWorldUsers);
		} catch (IOException ex) {
			Logger.getLogger(WorldsHolder.class.getName()).log(Level.SEVERE, null, ex);
			return false;
		}
		return true;
	}

	/**
	 * Wrapper for LoadWorld(String,Boolean) for backwards compatibility
	 * 
	 * Load a world from file.
	 * If it already been loaded, summon reload method from dataHolder.
	 * 
	 * @param worldName
	 */
	public void loadWorld(String worldName) {

		loadWorld(worldName, false);
	}

	/**
	 * Load a world from file.
	 * If it has already been loaded, use reload() method from the dataHolder.
	 * 
	 * @param worldName
	 */
	public void loadWorld(String worldName, Boolean isMirror) {

		String worldNameLowered = worldName.toLowerCase();

		if (worldsData.containsKey(worldNameLowered)) {
			worldsData.get(worldNameLowered).reload();
			return;
		}
		GroupManager.logger.finest(String.format(Messages.getString("WorldsHolder.ATTEMPT_TO_LOAD"), worldName)); //$NON-NLS-1$ //$NON-NLS-2$
		
		File thisWorldFolder = new File(worldsFolder, worldNameLowered);
		if ((isMirror) || (thisWorldFolder.exists() && thisWorldFolder.isDirectory())) {

			// Setup file handles, if not mirrored
			File groupsFile = (mirrorsGroup.containsKey(worldNameLowered)) ? null : new File(thisWorldFolder, "groups.yml"); //$NON-NLS-1$
			File usersFile = (mirrorsUser.containsKey(worldNameLowered)) ? null : new File(thisWorldFolder, "users.yml"); //$NON-NLS-1$

			if ((groupsFile != null) && (!groupsFile.exists())) {
				throw new IllegalArgumentException(String.format(Messages.getString("WorldsHolder.ERROR_NO_GROUPS_FILE"), worldName, groupsFile.getPath())); //$NON-NLS-1$
			}
			if ((usersFile != null) && (!usersFile.exists())) {
				throw new IllegalArgumentException(String.format(Messages.getString("WorldsHolder.ERROR_NO_USERS_FILE"), worldName, usersFile.getPath())); //$NON-NLS-1$
			}

			WorldDataHolder tempHolder = new WorldDataHolder(worldNameLowered);

			// Map the group object for any mirror
			if (mirrorsGroup.containsKey(worldNameLowered))
				tempHolder.setGroupsObject(this.getWorldData(mirrorsGroup.get(worldNameLowered)).getGroupsObject());
			else
				tempHolder.loadGroups(groupsFile);

			// Map the user object for any mirror
			if (mirrorsUser.containsKey(worldNameLowered))
				tempHolder.setUsersObject(this.getWorldData(mirrorsUser.get(worldNameLowered)).getUsersObject());
			else
				tempHolder.loadUsers(usersFile);

			OverloadedWorldHolder thisWorldData = new OverloadedWorldHolder(tempHolder);

			// null the object so we don't keep file handles open where we shouldn't

			// Set the file TimeStamps as it will be default from the initial load.
			thisWorldData.setTimeStamps();

			if (thisWorldData != null) {
				GroupManager.logger.finest(String.format(Messages.getString("WorldsHolder.WORLD_LOAD_SUCCESS"), worldName)); //$NON-NLS-1$
				worldsData.put(worldNameLowered, thisWorldData);
			}

			//GroupManager.logger.severe("Failed to load world " + worldName + "...");
		}
	}

	/**
	 * Tells if the world has been mapped.
	 * 
	 * It will return true if world is a mirror.
	 * 
	 * @param worldName
	 * @return true if world is loaded or mirrored. false if not listed
	 */
	public boolean isInList(String worldName) {

		return worldsData.containsKey(worldName.toLowerCase()) || mirrorsGroup.containsKey(worldName.toLowerCase()) || mirrorsUser.containsKey(worldName.toLowerCase());
	}

	/**
	 * Verify if world has it's own file permissions.
	 *
	 * @param worldName
	 * @return true if it has its own holder. false if not.
	 */
	public boolean hasOwnData(String worldName) {

		if (worldsData.containsKey(worldName.toLowerCase()) && (!mirrorsGroup.containsKey(worldName.toLowerCase()) || !mirrorsUser.containsKey(worldName.toLowerCase()))) {
			return true;
		}
		return false;
	}

	/**
	 * @return the defaultWorld
	 */
	public OverloadedWorldHolder getDefaultWorld() {

		return getUpdatedWorldData(serverDefaultWorldName);
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
					String usersMirror = mirrorsUser.get(worldNameLowered);
					String groupsMirror = mirrorsGroup.get(worldNameLowered);

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
