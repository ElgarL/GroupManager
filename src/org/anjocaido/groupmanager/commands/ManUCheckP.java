/**
 * 
 */
package org.anjocaido.groupmanager.commands;

import java.util.ArrayList;
import java.util.List;

import org.anjocaido.groupmanager.data.Group;
import org.anjocaido.groupmanager.data.User;
import org.anjocaido.groupmanager.utils.PermissionCheckResult;
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
public class ManUCheckP extends BaseCommand implements TabCompleter {

	/**
	 * 
	 */
	public ManUCheckP() {}

	@Override
	protected boolean parseCommand(@NotNull String[] args) {

		// Validating state of sender
		if (dataHolder == null || permissionHandler == null) {
			if (!setDefaultWorldHandler(sender))
				return true;
		}
		// Validating arguments
		if (args.length != 2) {
			sender.sendMessage(ChatColor.RED + "Review your arguments count! (/manucheckp <player> <permission>)");
			return true;
		}
		
		auxString = args[1].replace("'", "");

		if ((plugin.isValidateOnlinePlayer()) && ((match = validatePlayer(args[0], sender)) == null)) {
			return false;
		}

		if (match != null) {
			auxUser = dataHolder.getUser(match.toString());
		} else {
			auxUser = dataHolder.getUser(args[0]);
		}
		targetPlayer = plugin.getServer().getPlayer(auxUser.getLastName());
		// Validating permission
		permissionResult = permissionHandler.checkFullGMPermission(auxUser, auxString, false);

		if (permissionResult.resultType.equals(PermissionCheckResult.Type.NOTFOUND)) {
			// No permissions found in GM so fall through and check Bukkit.
			sender.sendMessage(ChatColor.YELLOW + "The player doesn't have access to that permission");

		} else {
			// This permission was found in groupmanager.
			if (permissionResult.owner instanceof User) {
				if (permissionResult.resultType.equals(PermissionCheckResult.Type.NEGATION)) {
					sender.sendMessage(ChatColor.YELLOW + "The user has directly a negation node for that permission.");
				} else if (permissionResult.resultType.equals(PermissionCheckResult.Type.EXCEPTION)) {
					sender.sendMessage(ChatColor.YELLOW + "The user has directly an Exception node for that permission.");
				} else {
					sender.sendMessage(ChatColor.YELLOW + "The user has directly this permission.");
				}
				sender.sendMessage(ChatColor.YELLOW + "Permission Node: " + permissionResult.accessLevel);
			} else if (permissionResult.owner instanceof Group) {
				if (permissionResult.resultType.equals(PermissionCheckResult.Type.NEGATION)) {
					sender.sendMessage(ChatColor.YELLOW + "The user inherits a negation permission from group: " + permissionResult.owner.getLastName());
				} else if (permissionResult.resultType.equals(PermissionCheckResult.Type.EXCEPTION)) {
					sender.sendMessage(ChatColor.YELLOW + "The user inherits an Exception permission from group: " + permissionResult.owner.getLastName());
				} else {
					sender.sendMessage(ChatColor.YELLOW + "The user inherits the permission from group: " + permissionResult.owner.getLastName());
				}
				sender.sendMessage(ChatColor.YELLOW + "Permission Node: " + permissionResult.accessLevel);
			}
		}

		// superperms
		if (targetPlayer != null) {
			sender.sendMessage(ChatColor.YELLOW + "SuperPerms reports Node: " + targetPlayer.hasPermission(args[1]) + ((!targetPlayer.hasPermission(args[1]) && targetPlayer.isPermissionSet(args[1])) ? " (Negated)": ""));
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
