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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.data.User;
import org.anjocaido.groupmanager.utils.PermissionCheckResult;
import org.anjocaido.groupmanager.utils.Tasks;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author ElgarL
 *
 */
public class ManUAddP extends BaseCommand implements TabCompleter {

	/**
	 * 
	 */
	public ManUAddP() {}

	@Override
	protected boolean parseCommand(@NotNull String[] args) {
		
		// Validating state of sender
		if (dataHolder == null || permissionHandler == null) {
			if (!setDefaultWorldHandler(sender))
				return true;
		}
		// Validating arguments
		if (args.length < 2) {
			sender.sendMessage(ChatColor.RED + "Review your arguments count! (/manuaddp <player> <permission> [permission2] [permission3]...)");
			return true;
		}
		
		if ((plugin.isValidateOnlinePlayer()) && ((match = validatePlayer(args[0], sender)) == null)) {
			return false;
		}

		if (match != null) {
			auxUser = dataHolder.getUser(match.toString());
		} else {
			auxUser = dataHolder.getUser(args[0]);
		}
		
		// Validating your permissions
		if (!isConsole && !isOpOverride && (senderGroup != null ? permissionHandler.inGroup(auxUser.getUUID(), senderGroup.getName()) : false)) {
			sender.sendMessage(ChatColor.RED + "Can't modify player with same group than you, or higher.");
			return true;
		}
		
		for (int i = 1; i < args.length; i++)
		{
			auxString = args[i].replace("'", "");
			String[] split = null;
			Instant timed = null;
			Long period = null;
			/*
			 * check for a timed permission
			 */
			if (auxString.contains("|")) {
				split = auxString.split("\\|");

				period = Tasks.parsePeriod(split[1]);
				timed = Instant.now().plus(period, ChronoUnit.MINUTES);
				auxString = split[0];
				
				if (period == 0) {
					sender.sendMessage(ChatColor.RED + "Invalid duration entered with '" + auxString + "' <permission>|1d1h1m");
					continue;
				}
			}
		
			permissionResult = permissionHandler.checkFullUserPermission(senderUser, auxString);
			if (!isConsole && !isOpOverride && (permissionResult.resultType.equals(PermissionCheckResult.Type.NOTFOUND) || permissionResult.resultType.equals(PermissionCheckResult.Type.NEGATION))) {
				sender.sendMessage(ChatColor.RED + "You can't add a permission you don't have: '" + auxString + "'");
				continue;
			}
			// Validating permissions of user
			permissionResult = permissionHandler.checkUserOnlyPermission(auxUser, auxString);
			if (plugin.checkPermissionExists(sender, auxString, permissionResult, "user")) 
			{
				continue;
			}
			// Seems Ok
			
			if (period != null) {
				auxUser.addTimedPermission(auxString, timed.getEpochSecond());
				sender.sendMessage(ChatColor.YELLOW + "You added '" + auxString + "' to player '" + auxUser.getLastName() + "' permissions with a duration of " + period + " minutes.");
				
			} else {
				auxUser.addPermission(auxString);
				sender.sendMessage(ChatColor.YELLOW + "You added '" + auxString + "' to player '" + auxUser.getLastName() + "' permissions.");
			}
		}

		// If the player is online, this will create new data for the user.
		if (auxUser.getUUID() != null) {
			targetPlayer = plugin.getServer().getPlayer(UUID.fromString(auxUser.getUUID()));
			if (targetPlayer != null)
				GroupManager.getBukkitPermissions().updatePermissions(targetPlayer);
		}

		return true;
	}
	
	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {

		parseSender(sender, alias);
		
		List<String> result = new ArrayList<String>();
		
		/*
		 * Return a TabComplete for users.
		 */
		if (args.length == 1) {

			for (User user : dataHolder.getUserList()) {
				result.add(user.getLastName());
			}
			return result;
		}
		
		return null;
	}

}
