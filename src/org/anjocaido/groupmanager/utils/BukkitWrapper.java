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
package org.anjocaido.groupmanager.utils;

import java.util.List;
import java.util.UUID;

import org.anjocaido.groupmanager.GroupManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;


/**
 * @author ElgarL
 *
 */
public class BukkitWrapper {

	private final Plugin plugin;
	private OfflinePlayer cache = null;
	private static BukkitWrapper instance;

	private BukkitWrapper() {
		plugin = GroupManager.getPlugin(GroupManager.class);
	}

	public static BukkitWrapper getInstance(){
		if(instance == null){
			synchronized (BukkitWrapper.class) {
				if(instance == null){
					instance = new BukkitWrapper();
				}
			}
		}
		return instance;
	}

	/**
	 * Find a players UUID from the servers usercache.
	 * returns null if there is no match.
	 * 
	 * @param name	{@String} containing the players name.
	 * @return	{@UUID} for this player, or null if there is no data.
	 */
	public UUID getPlayerUUID (String name) {

		// Check our cache first
		if ((cache != null) && (cache.getName().equalsIgnoreCase(name)))
			return cache.getUniqueId();

		// Clear our cache as this is a different player
		cache = null;

		// Search all known players (to this server) for a matching name.
		OfflinePlayer offlinePlayer[] = plugin.getServer().getOfflinePlayers();

		for (OfflinePlayer player : offlinePlayer)
			if (player.getName() != null && player.getName().equalsIgnoreCase(name)) {
				cache = player;
				return player.getUniqueId();
			}

		// A player with this name has never been seen on this server.
		return null;
	}

	/**
	 * Find a players name from the servers usercache.
	 * returns null if there is no match.
	 * 
	 * @param uid	{@UUID} to lookup.
	 * @return	{@String} of the players name, or null if there is no data.
	 */
	public String getPlayerName(UUID uid) {

		// Check our cache first
		if ((cache != null) && (cache.getUniqueId().compareTo(uid) == 0))
			return cache.getName();

		// Clear our cache as this is a different player
		cache = null;

		// Search all known players (to this server) for a matching UUID.
		OfflinePlayer offlinePlayer[] = plugin.getServer().getOfflinePlayers();

		for (OfflinePlayer player : offlinePlayer)
			if (player.getName() != null && player.getUniqueId().compareTo(uid) == 0) {
				cache = player;
				return player.getName();
			}

		// A player with this UUID has never been seen on this server.
		return null;
	}

	/**
	 * (Deprecated) Gets an OfflinePlayer object ![NEVER USE]!
	 * adds this data to the servers usercache.
	 * Always returns an object with this name, but the UUID will be generated if there is no account.
	 * (performs a blocking web request if the player is not known to the server)
	 * 
	 * -----------------------------------------------------------------
	 * The reason to never use this is it adding data to the usercache.
	 * This can result in a name being associated with an invalid UUID
	 * possibly confusing future searches.
	 * -----------------------------------------------------------------
	 * 
	 * @param name	a sting of this players name (case insensitive)
	 * @return	{@OfflinePlayer} object for this name.
	 */
	@Deprecated
	public OfflinePlayer getOfflinePlayer(String name) {

		return plugin.getServer().getOfflinePlayer(name);
	}

	/**
	 * Gets an OfflinePlayer object
	 * does NOT add this data to the servers usercache.
	 * Always returns an object but the name will be null if the UUID is not in the usercache.
	 * 
	 * @param uid a {@UUID} for this player.
	 * @return	{@OfflinePlayer} object for this {@UUID}.
	 */
	public OfflinePlayer getOfflinePlayer(UUID uid) {

		return plugin.getServer().getOfflinePlayer(uid);
	}
	
	/**
	 * Fetch all Offline Players known to the server.
	 * 
	 * @return an Array of {@OfflinePlayer} objects.
	 */
	public OfflinePlayer[] getOfflinePlayers() {
		
		return plugin.getServer().getOfflinePlayers();
	}

	/**
	 * Attempts to match any players with the given name, and returns a list of all possible matches.
	 * This list is not sorted in any particular order. If an exact match is found, the returned list will only contain a single result.
	 * 
	 * @param the (partial) name to match
	 * @return
	 */
	public List<Player> matchPlayer(String name) {

		return plugin.getServer().matchPlayer(name);
	}

	/**
	 * Gets a Player object if the player is Online.
	 * returns null if offline.
	 * 
	 * @param name
	 * @return {@Player} object for this name.
	 */
	public Player getPlayer(String name) {

		return plugin.getServer().getPlayer(name);
	}

	public Long getLastOnline(UUID uid) {

		// Check our cache first
		if ((cache != null) && (cache.getUniqueId().compareTo(uid) == 0))
			return cache.getLastPlayed();

		// Clear our cache as this is a different player
		cache = null;

		// Search all known players (to this server) for a matching UUID.
		if (getPlayerName(uid) != null)
			return cache.getLastPlayed();

		// A player with this UUID has never been seen on this server.
		return 0L;
	}

	public Long getFirstPlayed(UUID uid) {

		// Check our cache first
		if ((cache != null) && (cache.getUniqueId().compareTo(uid) == 0))
			return cache.getFirstPlayed();

		// Clear our cache as this is a different player
		cache = null;

		// Search all known players (to this server) for a matching UUID.
		if (getPlayerName(uid) != null)
			return cache.getFirstPlayed();

		// A player with this UUID has never been seen on this server.
		return 0L;
	}

}

