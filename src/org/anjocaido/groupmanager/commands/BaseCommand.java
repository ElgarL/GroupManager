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
package org.anjocaido.groupmanager.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.data.Group;
import org.anjocaido.groupmanager.data.User;
import org.anjocaido.groupmanager.dataholder.OverloadedWorldHolder;
import org.anjocaido.groupmanager.localization.Messages;
import org.anjocaido.groupmanager.permissions.AnjoPermissionsHandler;
import org.anjocaido.groupmanager.utils.BukkitWrapper;
import org.anjocaido.groupmanager.utils.PermissionCheckResult;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author ElgarL
 *
 */
public abstract class BaseCommand implements CommandExecutor, TabCompleter {

	protected GroupManager plugin;
	
	protected boolean isConsole = false;
	protected boolean playerCanDo = false;
	protected boolean isOpOverride = false;
	protected boolean isAllowCommandBlocks = false;
	
	protected Player senderPlayer = null, targetPlayer = null;
	protected CommandSender sender = null;
	protected Group senderGroup = null;
	protected User senderUser = null;
	
	protected UUID match = null;
	protected User auxUser = null;
	protected Group auxGroup = null;
	protected Group auxGroup2 = null;
	protected String auxString = null;
	protected PermissionCheckResult permissionResult = null;
	
	// PERMISSIONS FOR COMMAND BEING LOADED
	protected OverloadedWorldHolder dataHolder = null;
	protected AnjoPermissionsHandler permissionHandler = null;
	
	
	/**
	 * 
	 */
	public BaseCommand() {
		
		plugin = GroupManager.getPlugin(GroupManager.class);
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		
		// If parsSender fails exit.
		if (!parseSender(sender, label)) return true;
		
		try {
			return parseCommand(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
		
		// If parsSender fails return empty.
		if (!parseSender(sender, alias)) return new ArrayList<String>();
		
		return tabComplete(sender, command, alias, args);
	}
	
	/**
	 * Attempt to setup the data-sources for this command.
	 * 
	 * @param sender
	 * @param command
	 * @param alias
	 * @return true if successful.
	 */
	public boolean parseSender(CommandSender sender, String alias) {

		playerCanDo = false;
		isConsole = false;
		
		isOpOverride = GroupManager.getGMConfig().isOpOverride();
		isAllowCommandBlocks = GroupManager.getGMConfig().isAllowCommandBlocks();
		
		// Prevent all commands other than /manload if we are in an error state.
		if (!plugin.getLastError().isEmpty() && !alias.equalsIgnoreCase("manload")) { //$NON-NLS-1$
			sender.sendMessage(ChatColor.RED + Messages.getString("COMMAND_ERROR")); //$NON-NLS-1$
			return false;
		}
		
		// PREVENT GM COMMANDS BEING USED ON COMMANDBLOCKS
		if (sender instanceof BlockCommandSender && !isAllowCommandBlocks) {
			Block block = ((BlockCommandSender)sender).getBlock();
			GroupManager.logger.warning(ChatColor.RED + Messages.getString("COMMAND_BLOCKS")); //$NON-NLS-1$
			GroupManager.logger.warning(ChatColor.RED + Messages.getString("LOCATION") + ChatColor.GREEN + block.getWorld().getName() + ", " + block.getX() + ", " + block.getY() + ", " + block.getZ()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		  	return false;
		}

		// DETERMINING PLAYER INFORMATION
		if (sender instanceof Player) {
			senderPlayer = (Player) sender;

			senderUser = plugin.getWorldsHolder().getWorldData(senderPlayer).getUser(senderPlayer.getUniqueId().toString());
			senderGroup = senderUser.getGroup();
			isOpOverride = (isOpOverride && (senderPlayer.isOp() || plugin.getWorldsHolder().getWorldPermissions(senderPlayer).has(senderPlayer, "groupmanager.op"))); //$NON-NLS-1$

			if (isOpOverride || plugin.getWorldsHolder().getWorldPermissions(senderPlayer).has(senderPlayer, "groupmanager." + alias)) { //$NON-NLS-1$
				playerCanDo = true;
			}
		} else {

			isConsole = true;
		}
		
		// PERMISSIONS FOR COMMAND BEING LOADED
		dataHolder = null;
		permissionHandler = null;

		if (senderPlayer != null) {
			dataHolder = plugin.getWorldsHolder().getWorldData(senderPlayer);
		}

		String selectedWorld = GroupManager.getSelectedWorlds().get(sender.getName());
		if (selectedWorld != null) {
			dataHolder = plugin.getWorldsHolder().getWorldData(selectedWorld);
		}

		if (dataHolder != null) {
			permissionHandler = dataHolder.getPermissionsHandler();
		}
		
		this.sender = sender;
		
		if (!isConsole && !playerCanDo) {
			sender.sendMessage(ChatColor.RED + Messages.getString("COMMAND_NOT_PERMITTED"));
			return false;
		}
		
		return true;
	}
	
	/**
	 * Sets up the default world for use.
	 */
	protected boolean setDefaultWorldHandler(CommandSender sender) {

		dataHolder = plugin.getWorldsHolder().getWorldData(plugin.getWorldsHolder().getDefaultWorld().getName());
		permissionHandler = dataHolder.getPermissionsHandler();

		if ((dataHolder != null) && (permissionHandler != null)) {
			GroupManager.getSelectedWorlds().put(sender.getName(), dataHolder.getName());
			sender.sendMessage(ChatColor.RED + String.format(Messages.getString("DEFAULT_WORLD_SELECTED"), plugin.getWorldsHolder().getDefaultWorld().getName()));
			return true;
		}

		sender.sendMessage(ChatColor.RED + Messages.getString("WORLD_SELECTION_NEEDED"));
		sender.sendMessage(ChatColor.RED + Messages.getString("USE_MANSELECT"));
		return false;

	}
	
	/**
	 * Load a List of players matching the name given. If none online, check
	 * Offline.
	 * 
	 * @param playerName, sender
	 * @return UUID if a single match is found
	 */
	protected UUID validatePlayer(String playerName, CommandSender sender) {

		List<Player> players = new ArrayList<Player>();
		UUID match = null;

		players = BukkitWrapper.getInstance().matchPlayer(playerName);
		
		if (players.isEmpty()) {
			// Check for an offline player (exact match, ignoring case).
			match = BukkitWrapper.getInstance().getPlayerUUID(playerName);
			
			if (match == null) {
				sender.sendMessage(ChatColor.RED + Messages.getString("PLAYER_NOT_FOUND"));
				return null;
			}
			
			return match;
				
		} else if (players.size() > 1) {
			sender.sendMessage(ChatColor.RED + Messages.getString("TOO_MANY_MATCHES"));
			return null;
		}

		return players.get(0).getUniqueId();

	}
	
	/**
	 * Return any users with partial name matches.
	 * 
	 * @param arg partial name to match
	 * @return
	 */
	protected List<String> tabCompleteUsers(String arg) {
		
		List<String> result = new ArrayList<String>();
		arg = arg.toLowerCase();
		
		/*
		 * Return a TabComplete for users.
		 */
		for (User user : dataHolder.getUserList()) {
			// Possible matching player
			if((user != null) && (user.getLastName() != null) && (user.getLastName().toLowerCase().contains(arg))) {
				
				// If validating check for online state.
				if (GroupManager.getGMConfig().isTabValidate() && GroupManager.getGMConfig().isToggleValidate()) {
					if (user.isOnline())
						result.add(user.getLastName());
				} else {
					// Not validating so add as a possible match
					result.add(user.getLastName());
				}
			}
		}
		return result;
	}
	
	/**
	 * Return a List of groups with partial name matches.
	 * 
	 * @param arg partial name to match
	 * @return
	 */
	protected List<String> tabCompleteGroups(String arg) {
		
		List<String> result = new ArrayList<String>();
		arg = arg.toLowerCase();
		
		for (Group g : dataHolder.getGroupList()) {
			if ((g != null) && (g.getName() != null) && (g.getName().toLowerCase().contains(arg.toLowerCase())))
				result.add(g.getName());
		}
		return result;
	}
	
	/**
	 * Fetch a list of available worlds for tab Complete.
	 * 
	 * @return	a List of all root world names.
	 */
	protected List<String> getWorlds() {
		
		List<String> worlds = new ArrayList<String>();
		
		for (OverloadedWorldHolder world : plugin.getWorldsHolder().allWorldsDataList())
			if ((world != null) && (world.getName() != null))
				worlds.add(world.getName());

		return worlds;
	}

	protected abstract boolean parseCommand(@NotNull String[] args);
	
	protected @Nullable List<String> tabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
		
		/*
		 * Return an empty list so there is no TabComplete on this.
		 */
		return new ArrayList<String>();
	}
}
