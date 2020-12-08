/*
 * 
 */
package org.anjocaido.groupmanager.storage;

import java.io.IOException;

import org.anjocaido.groupmanager.dataholder.OverloadedWorldHolder;
import org.anjocaido.groupmanager.dataholder.WorldDataHolder;

/**
 * @author ElgarL
 *
 */
public interface DataSource {

	public void init(String worldName);

	public void loadWorld(String worldName, Boolean isMirror);
	public void loadAllSearchedWorlds();

	public void loadGroups(WorldDataHolder dataHolder) throws IOException;
	public void loadUsers(WorldDataHolder dataHolder) throws IOException;

	public void reload(WorldDataHolder dataHolder);
	public void reloadGroups(WorldDataHolder dataHolder);
	public void reloadUsers(WorldDataHolder dataHolder);

	public void saveGroups(WorldDataHolder dataHolder);
	public void saveUsers(WorldDataHolder dataHolder);

	public void backup(OverloadedWorldHolder world, Boolean groupsOrUsers);
}
