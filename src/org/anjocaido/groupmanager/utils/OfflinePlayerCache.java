/*
 * 
 */
package org.anjocaido.groupmanager.utils;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * 
 * Keeps an up to date cache of Player names to UUID.
 * 
 * @author ElgarL
 *
 */
public class OfflinePlayerCache {

	private final BiMap<String, UUID> nameUUID = HashBiMap.create();
	private static OfflinePlayerCache instance;
	
	private OfflinePlayerCache() {}
	
	public static OfflinePlayerCache getInstance(){
		if (instance == null) {
			synchronized (OfflinePlayerCache.class) {
				if (instance == null) {
					instance = new OfflinePlayerCache();
					
					// Populate our cache.
					for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers() ) {
						if (offlinePlayer.getUniqueId() != null && offlinePlayer.getName() != null) {
							instance.forcePutMatch(offlinePlayer.getName(), offlinePlayer.getUniqueId());
						}
					}
				}
			}
		}
		return instance;
	}
	
	/**
	 * Called from our PlayerLoginEvent to keep our cache up to date.
	 * Name and UUID are unique. A name will only map to a current known UUID.
	 * 
	 * @param name	Player name.
	 * @param id	UUID of the Player.
	 */
	public void forcePutMatch(String name, UUID id) {
		
		nameUUID.forcePut(name.toLowerCase(), id);
	}
	
	/**
	 * Search the cache for a UUID which pairs with this name.
	 * 
	 * @param name	a Players name to search for a UUID match.
	 * @return  UUID of a known OfflinePlayer or null.
	 */
	public UUID getUUID(String name) {
		
		return nameUUID.get(name.toLowerCase());
	}
	
	/**
	 * Search the cache for a name which pairs with this UUID.
	 * 
	 * @param id	a Players UUID to search for a name match.
	 * @return	Name of a known OfflinePlayer or null.
	 */
	public String getName(UUID id) {
		
		return nameUUID.inverse().get(id);
	}
	
	public int size() {
		
		return nameUUID.size();
	}
}
