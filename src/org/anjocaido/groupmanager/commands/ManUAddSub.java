/**
 * 
 */
package org.anjocaido.groupmanager.commands;

import java.util.ArrayList;
import java.util.List;

import org.anjocaido.groupmanager.data.Group;
import org.anjocaido.groupmanager.data.User;
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
public class ManUAddSub extends BaseCommand implements TabCompleter {

	/**
	 * 
	 */
	public ManUAddSub() {}

	@Override
	protected boolean parseCommand(@NotNull String[] args) {

		// Validating state of sender
		if (dataHolder == null || permissionHandler == null) {
			if (!setDefaultWorldHandler(sender)) {
				sender.sendMessage(ChatColor.RED + "Couldn't retrieve your world. World selection is needed.");
				sender.sendMessage(ChatColor.RED + "Use /manselect <world>");
				return true;
			}
		}
		// Validating arguments
		if (args.length != 2) {
			sender.sendMessage(ChatColor.RED + "Review your arguments count! (/manuaddsub <player> <group>)");
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
		auxGroup = dataHolder.getGroup(args[1]);
		if (auxGroup == null) {
			sender.sendMessage(ChatColor.RED + "'" + args[1] + "' Group doesnt exist!");
			return true;
		}
		// Validating permission
		if (!isConsole && !isOpOverride && (senderGroup != null ? permissionHandler.inGroup(auxUser.getUUID(), senderGroup.getName()) : false)) {
			sender.sendMessage(ChatColor.RED + "You can't modify a player with same permissions as you, or higher.");
			return true;
		}
		if (!isConsole && !isOpOverride && (permissionHandler.hasGroupInInheritance(auxGroup, senderGroup.getName()))) {
			sender.sendMessage(ChatColor.RED + "The sub-group can't be the same as yours, or higher.");
			return true;
		}
		if (!isConsole && !isOpOverride && (!permissionHandler.inGroup(senderUser.getUUID(), auxUser.getGroupName()) || !permissionHandler.inGroup(senderUser.getUUID(), auxGroup.getName()))) {
			sender.sendMessage(ChatColor.RED + "You can't modify a player involving a group that you don't inherit.");
			return true;
		}
		// Seems OK
		if (auxUser.addSubGroup(auxGroup))
			sender.sendMessage(ChatColor.YELLOW + "You added subgroup '" + auxGroup.getName() + "' to player '" + auxUser.getLastName() + "'.");
		else
			sender.sendMessage(ChatColor.RED + "The subgroup '" + auxGroup.getName() + "' is already available to '" + auxUser.getLastName() + "'.");
		
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
		
		/*
		 * Populate the second argument of TabComplete with a list of group names.
		 */
		if (args.length == 2) {

			for (Group g : dataHolder.getGroupList()) {
				result.add(g.getName());
			}

			return result;
		}
		
		return null;
	}

}
