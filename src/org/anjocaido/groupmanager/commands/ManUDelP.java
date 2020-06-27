/**
 * 
 */
package org.anjocaido.groupmanager.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.anjocaido.groupmanager.GroupManager;
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
public class ManUDelP extends BaseCommand implements TabCompleter {

	/**
	 * 
	 */
	public ManUDelP() {}

	@Override
	protected boolean parseCommand(@NotNull String[] args) {

		// Validating state of sender
		if (dataHolder == null || permissionHandler == null) {
			if (!setDefaultWorldHandler(sender))
				return true;
		}
		// Validating arguments
		if (args.length < 2) {
			sender.sendMessage(ChatColor.RED + "Review your arguments count! (/manudelp <player> <permission> [permission2] [permission3]...)");
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
		
		for (int i = 1; i < args.length; i++)
		{
			auxString = args[i].replace("'", "");
		
			if (!isConsole && !isOpOverride && (senderGroup != null ? permissionHandler.inGroup(auxUser.getUUID(), senderGroup.getName()) : false)) {
				sender.sendMessage(ChatColor.RED + "You can't modify a player with same group as you, or higher.");
				continue;
			}
			// Validating your permissions
			permissionResult = permissionHandler.checkFullUserPermission(senderUser, auxString);
			if (!isConsole && !isOpOverride && (permissionResult.resultType.equals(PermissionCheckResult.Type.NOTFOUND) || permissionResult.resultType.equals(PermissionCheckResult.Type.NEGATION))) {
				sender.sendMessage(ChatColor.RED + "You can't remove a permission you don't have: '" + auxString + "'");
				continue;
			}
			// Validating permissions of user
			permissionResult = permissionHandler.checkUserOnlyPermission(auxUser, auxString);
			if (permissionResult.resultType.equals(PermissionCheckResult.Type.NOTFOUND)) {
				sender.sendMessage(ChatColor.RED + "The user doesn't have direct access to that permission: '" + auxString + "'");
				continue;
			}
			if (!auxUser.hasSamePermissionNode(auxString)) {
				sender.sendMessage(ChatColor.RED + "This permission node doesn't match any node.");
				sender.sendMessage(ChatColor.RED + "But might match node: " + permissionResult.accessLevel);
				continue;
			}
			auxUser.removePermission(auxString);
			sender.sendMessage(ChatColor.YELLOW + "You removed '" + auxString + "' from player '" + auxUser.getLastName() + "' permissions.");
		}
		// Seems OK

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
		
		if (args.length >= 2) {
			if ((plugin.isValidateOnlinePlayer()) && ((match = validatePlayer(args[0], sender)) == null)) {
				return null;
			}

			if (match != null) {
				auxUser = dataHolder.getUser(match.toString());
			} else {
				auxUser = dataHolder.getUser(args[0]);
			}

			return auxUser.getPermissionList();

		}
		
		return null;
	}

}
