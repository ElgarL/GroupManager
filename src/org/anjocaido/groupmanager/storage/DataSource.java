/*
 * 
 */
package org.anjocaido.groupmanager.storage;

import java.io.IOException;

import org.anjocaido.groupmanager.GlobalGroups;
import org.anjocaido.groupmanager.dataholder.OverloadedWorldHolder;
import org.anjocaido.groupmanager.dataholder.WorldDataHolder;

/**
 * All data load/saving classes should implement this Interface.
 * 
 * @author ElgarL
 *
 */
public interface DataSource {

	/**
	 * Perform folder creation and default data setups.
	 * 
	 * @param worldName	the world we are to setup.
	 */
	void init(String worldName);

	/**
	 * Load data for our GlobalGroups.
	 */
	void loadGlobalGroups(GlobalGroups globalGroups);

	/**
	 * Save our GlobalGroups data.
	 */
	void saveGlobalGroups(boolean overwrite);

	/**
	 * Load data for this world.
	 * 
	 * @param worldName	the world to load data for.
	 * @param isMirror	if this world is a full or partial mirror.
	 */
	void loadWorld(String worldName, Boolean isMirror);

	/**
	 * Search the servers world folders for any unknown
	 * world folders and load data for them.
	 */
	void loadAllSearchedWorlds();

	/**
	 * Load Groups data into this world holder.
	 * 
	 * @param dataHolder	the world container to load data into.
	 * @throws IOException
	 */
	void loadGroups(WorldDataHolder dataHolder) throws IOException;

	/**
	 * Load Users data into this world holder.
	 * 
	 * @param dataHolder	the world container to load data into.
	 * @throws IOException
	 */
	void loadUsers(WorldDataHolder dataHolder) throws IOException;

	/**
	 * Reload data for this world.
	 * Do not overwrite current data unless the load is successful.
	 * 
	 * @param dataHolder	the world container to load data into.
	 */
	void reload(WorldDataHolder dataHolder);

	/**
	 * Reload Groups data for this world.
	 * Do not overwrite current data unless the load is successful.
	 * 
	 * @param dataHolder	the world container to load data into.
	 */
	void reloadGroups(WorldDataHolder dataHolder);

	/**
	 * Reload Users data for this world.
	 * Do not overwrite current data unless the load is successful.
	 * 
	 * @param dataHolder	the world container to load data into.
	 */
	void reloadUsers(WorldDataHolder dataHolder);

	/**
	 * Save this worlds Groups data.
	 * 
	 * @param dataHolder	the world container to save data from.
	 */
	void saveGroups(WorldDataHolder dataHolder);

	/**
	 * Save this worlds Users data.
	 * 
	 * @param dataHolder	the world container to save data from.
	 */
	void saveUsers(WorldDataHolder dataHolder);

	/**
	 * Does the dataSource have newer Global Groups data?
	 * 
	 * @return				true if the saved data is newer.
	 */
	boolean hasNewGlobalGroupsData();

	/**
	 * Does the dataSource have newer Groups data?
	 * 
	 * @param dataHolder	the data object to test.
	 * @return				true if the saved data is newer.
	 */
	boolean hasNewGroupsData(WorldDataHolder dataHolder);

	/**
	 * Does the dataSource have newer Users data?
	 * 
	 * @param dataHolder	the data object to test.
	 * @return				true if the saved data is newer.
	 */
	boolean hasNewUsersData(WorldDataHolder dataHolder);

	/**
	 * Backup old data.
	 * 
	 * @param world	the world to backup (null if GlobalGroups Type).
	 * @param type	Type GROUS, USERS, GLOBALGROUPS.
	 */
	void backup(OverloadedWorldHolder world, TYPE type);

	/**
	 * Remove old backups as per the settings in the config.
	 */
	void purgeBackups();

	/**
	 * Type specifier for backups.
	 */
	enum TYPE { GROUPS, USERS, GLOBALGROUPS };
}
