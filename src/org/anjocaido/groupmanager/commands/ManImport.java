/*
 * 
 */
package org.anjocaido.groupmanager.commands;

import java.util.concurrent.CompletableFuture;

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.localization.Messages;
import org.anjocaido.groupmanager.storage.CoreYaml;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

/**
 * Allows importing of data from Yaml to SQL.
 * 
 * @author ElgarL
 *
 */
public class ManImport extends BaseCommand {

	@Override
	protected boolean parseCommand(@NotNull String[] args) {

		if (GroupManager.getGMConfig().getDatabaseType().equals("YAML")) {
			
			sender.sendMessage(ChatColor.RED + "You can only import when using SQL.");
			return true;
		}
		CompletableFuture.runAsync(() -> {
			try {
				/*
				 * Obtain a lock so we can save.
				 */
				plugin.getSaveLock().lock();
				
				CoreYaml yamlData = new CoreYaml(plugin);

				yamlData.loadGlobalGroups(GroupManager.getGlobalGroups());
				GroupManager.setLoaded(false);
				
				plugin.getWorldsHolder().allWorldsDataList().forEach(world -> {
					
					yamlData.init(world.getName());
					yamlData.loadWorld(world.getName(), false);
					plugin.getWorldsHolder().getWorldData(world.getName()).setAllChanged();
				});
				
				GroupManager.getGlobalGroups().setAllGroupsChangedFlag();
				plugin.getWorldsHolder().saveChanges(true);
				
				sender.sendMessage(ChatColor.YELLOW + Messages.getString("GroupManager.REFRESHED")); //$NON-NLS-1$

			} catch (IllegalStateException ex) {
				sender.sendMessage(ChatColor.RED + ex.getMessage());

			} finally {
				// Release lock.
				if(plugin.getSaveLock().isHeldByCurrentThread()) {
					GroupManager.setLoaded(true);
					plugin.getSaveLock().unlock();
				}
			}
		});
		
		return true;
	}

	
}
