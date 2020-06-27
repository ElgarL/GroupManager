/**
 * 
 */
package org.anjocaido.groupmanager.commands;

import java.util.ArrayList;
import java.util.List;

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.data.Group;
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
public class ManGDelP extends BaseCommand implements TabCompleter {

	/**
	 * 
	 */
	public ManGDelP() {}

	@Override
	protected boolean parseCommand(@NotNull String[] args) {

		// Validating state of sender
		if (dataHolder == null || permissionHandler == null) {
			if (!setDefaultWorldHandler(sender))
				return true;
		}
		// Validating arguments
		if (args.length < 2) {
			sender.sendMessage(ChatColor.RED + "Review your arguments count! (/mangdelp <group> <permission> [permission2] [permission3]...)");
			return true;
		}
		
		auxGroup = dataHolder.getGroup(args[0]);
		if (auxGroup == null) {
			sender.sendMessage(ChatColor.RED + "'" + args[0] + "' Group doesnt exist!");
			return true;
		}
		for (int i = 1; i < args.length; i++)
		{
			auxString = args[i].replace("'", "");
		
			// Validating your permissions
			permissionResult = permissionHandler.checkFullUserPermission(senderUser, auxString);
			if (!isConsole && !isOpOverride && (permissionResult.resultType.equals(PermissionCheckResult.Type.NOTFOUND) || permissionResult.resultType.equals(PermissionCheckResult.Type.NEGATION))) {
				sender.sendMessage(ChatColor.RED + "Can't remove a permission you don't have: '" + auxString + "'");
				continue;
			}
			// Validating permissions of user
			permissionResult = permissionHandler.checkGroupOnlyPermission(auxGroup, auxString);
			if (permissionResult.resultType.equals(PermissionCheckResult.Type.NOTFOUND)) {
				sender.sendMessage(ChatColor.YELLOW + "The group doesn't have direct access to that permission: '" + auxString + "'");
				continue;
			}
			if (!auxGroup.hasSamePermissionNode(auxString)) {
				sender.sendMessage(ChatColor.RED + "This permission node doesn't match any node.");
				sender.sendMessage(ChatColor.RED + "But might match node: " + permissionResult.accessLevel);
				continue;
			}
			// Seems OK
			auxGroup.removePermission(auxString);
			sender.sendMessage(ChatColor.YELLOW + "You removed '" + auxString + "' from group '" + auxGroup.getName() + "' permissions.");
		}

		GroupManager.getBukkitPermissions().updateAllPlayers();

		return true;
	}
	
	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {

		parseSender(sender, alias);
		
		List<String> result = new ArrayList<String>();
		/*
		 * Return a TabComplete for base groups.
		 */
		if (args.length == 1) {

			for (Group g : dataHolder.getGroupList()) {
				result.add(g.getName());
			}
		}
		/*
		 * Return a TabComplete for permissions.
		 */
		if (args.length >= 2) {
				
			result = dataHolder.getGroup(args[0]).getPermissionList();

		}
		return result;

	}

}
