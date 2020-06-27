/**
 * 
 */
package org.anjocaido.groupmanager.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.anjocaido.groupmanager.data.Group;
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
public class ManGDelV extends BaseCommand implements TabCompleter {

	/**
	 * 
	 */
	public ManGDelV() {}

	@Override
	protected boolean parseCommand(@NotNull String[] args) {

		// Validating state of sender
		if (dataHolder == null || permissionHandler == null) {
			if (!setDefaultWorldHandler(sender))
				return true;
		}
		// Validating arguments
		if (args.length != 2) {
			sender.sendMessage(ChatColor.RED + "Review your arguments count! (/mangdelv <group> <variable>)");
			return true;
		}
		auxGroup = dataHolder.getGroup(args[0]);
		if (auxGroup == null) {
			sender.sendMessage(ChatColor.RED + "'" + args[0] + "' Group doesnt exist!");
			return true;
		}
		if (auxGroup.isGlobal()) {
			sender.sendMessage(ChatColor.RED + "GlobalGroups do NOT support Info Nodes.");
			return true;
		}
		// Validating permission
		if (!auxGroup.getVariables().hasVar(args[1])) {
			sender.sendMessage(ChatColor.RED + "The group doesn't have directly that variable!");
			return true;
		}
		// Seems OK
		auxGroup.getVariables().removeVar(args[1]);
		sender.sendMessage(ChatColor.YELLOW + "Variable " + ChatColor.GOLD + args[1] + ChatColor.YELLOW + " removed from the group " + ChatColor.GREEN + auxGroup.getName());

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
		 * Return a TabComplete for Variables.
		 */
		if (args.length == 2) {
				
			result = Arrays.asList(dataHolder.getGroup(args[0]).getVariables().getVarKeyList());

		}
		return result;

	}

}
