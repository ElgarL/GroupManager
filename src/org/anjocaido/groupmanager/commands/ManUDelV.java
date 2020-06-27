/**
 * 
 */
package org.anjocaido.groupmanager.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
public class ManUDelV extends BaseCommand implements TabCompleter {

	/**
	 * 
	 */
	public ManUDelV() {}

	@Override
	protected boolean parseCommand(@NotNull String[] args) {

		// Validating state of sender
		if (dataHolder == null || permissionHandler == null) {
			if (!setDefaultWorldHandler(sender))
				return true;
		}
		// Validating arguments
		if (args.length != 2) {
			sender.sendMessage(ChatColor.RED + "Review your arguments count! (/manudelv <user> <variable>)");
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
		// Validating permission
		if (!auxUser.getVariables().hasVar(args[1])) {
			sender.sendMessage(ChatColor.RED + "The user doesn't have directly that variable!");
			return true;
		}
		// Seems OK
		auxUser.getVariables().removeVar(args[1]);
		sender.sendMessage(ChatColor.YELLOW + "Variable " + ChatColor.GOLD + args[1] + ChatColor.YELLOW + " removed from the user " + ChatColor.GREEN + auxUser.getLastName());

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
		 * Return a TabComplete for Variables on the user.
		 */
		if (args.length == 2) {
			if ((plugin.isValidateOnlinePlayer()) && ((match = validatePlayer(args[0], sender)) == null)) {
				return null;
			}

			if (match != null) {
				auxUser = dataHolder.getUser(match.toString());
			} else {
				auxUser = dataHolder.getUser(args[0]);
			}

			return Arrays.asList(auxUser.getVariables().getVarKeyList());
		}
		
		return null;
	}

}
