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

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;


/**
 * @author ElgarL
 *
 */
public class BukkitWrapper {

	private static BukkitWrapper instance;

	private BukkitWrapper() {}

	public static BukkitWrapper getInstance(){
		if (instance == null) {
			synchronized (BukkitWrapper.class) {
				if (instance == null) {
					instance = new BukkitWrapper();
				}
			}
		}
		return instance;
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

		return Bukkit.getOfflinePlayer(name);
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

		return Bukkit.getOfflinePlayer(uid);
	}
	
	/**
	 * Fetch all Offline Players known to the server.
	 * 
	 * @return an Array of {@OfflinePlayer} objects.
	 */
	public OfflinePlayer[] getOfflinePlayers() {
		
		return Bukkit.getOfflinePlayers();
	}

	/**
	 * Attempts to match any players with the given name, and returns a list of all possible matches.
	 * This list is not sorted in any particular order. If an exact match is found, the returned list will only contain a single result.
	 * 
	 * @param name (partial) to match
	 * @return List of matched Players
	 */
	public List<Player> matchPlayer(String name) {

		return Bukkit.matchPlayer(name);
	}

	/**
	 * Gets a Player object if the player is Online.
	 * returns null if offline.
	 * 
	 * @param name
	 * @return {@Player} object for this name.
	 */
	public Player getPlayer(String name) {

		return Bukkit.getPlayer(name);
	}

	public Long getLastOnline(UUID uid) {

		// Search all known players (to this server) for a matching UUID.
		return getOfflinePlayer(uid).getLastPlayed();
	}

	public Long getFirstPlayed(UUID uid) {

		return getOfflinePlayer(uid).getFirstPlayed();
	}

}

