/**
 * 
 */
package org.anjocaido.groupmanager.commands;

import java.util.ArrayList;
import java.util.List;

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
public class ManGCheckP extends BaseCommand implements TabCompleter {

	/**
	 * 
	 */
	public ManGCheckP() {}

	@Override
	protected boolean parseCommand(@NotNull String[] args) {

		// Validating state of sender
		if (dataHolder == null || permissionHandler == null) {
			if (!setDefaultWorldHandler(sender))
				return true;
		}
		// Validating arguments
		if (args.length != 2) {
			sender.sendMessage(ChatColor.RED + "Review your arguments count! (/mangcheckp <group> <permission>)");
			return true;
		}
		
		auxString = args[1];
		if (auxString.startsWith("'") && auxString.endsWith("'"))
		{
			auxString = auxString.substring(1, auxString.length() - 1);
		}
		
		auxGroup = dataHolder.getGroup(args[0]);
		if (auxGroup == null) {
			sender.sendMessage(ChatColor.RED + "'" + args[0] + "' Group doesnt exist!");
			return true;
		}
		// Validating permission
		permissionResult = permissionHandler.checkGroupPermissionWithInheritance(auxGroup, auxString);
		if (permissionResult.resultType.equals(PermissionCheckResult.Type.NOTFOUND)) {
			sender.sendMessage(ChatColor.YELLOW + "The group doesn't have access to that permission");
			return true;
		}
		// Seems OK
		// auxString = permissionHandler.checkUserOnlyPermission(auxUser, args[1]);
		if (permissionResult.owner instanceof Group) {
			if (permissionResult.resultType.equals(PermissionCheckResult.Type.NEGATION)) {
				sender.sendMessage(ChatColor.YELLOW + "The group inherits the negation permission from group: " + permissionResult.owner.getLastName());
			} else if (permissionResult.resultType.equals(PermissionCheckResult.Type.EXCEPTION)) {
				sender.sendMessage(ChatColor.YELLOW + "The group inherits an Exception permission from group: " + permissionResult.owner.getLastName());
			} else {
				sender.sendMessage(ChatColor.YELLOW + "The group inherits the permission from group: " + permissionResult.owner.getLastName());
			}
			sender.sendMessage(ChatColor.YELLOW + "Permission Node: " + permissionResult.accessLevel);

		}
		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {

		parseSender(sender, alias);
		
		List<String> result = new ArrayList<String>();
		/*
		 * Return a TabComplete for groups.
		 */
		if (args.length == 1) {

			for (Group g : dataHolder.getGroupList()) {
				result.add(g.getName());
			}
		}
		return result;
	}

}
