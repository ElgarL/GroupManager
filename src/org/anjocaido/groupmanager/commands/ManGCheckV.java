/**
 * 
 */
package org.anjocaido.groupmanager.commands;

import java.util.ArrayList;
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
public class ManGCheckV extends BaseCommand implements TabCompleter {

	/**
	 * 
	 */
	public ManGCheckV() {}

	@Override
	protected boolean parseCommand(@NotNull String[] args) {

		// Validating state of sender
		if (dataHolder == null || permissionHandler == null) {
			if (!setDefaultWorldHandler(sender))
				return true;
		}
		// Validating arguments
		if (args.length != 2) {
			sender.sendMessage(ChatColor.RED + "Review your arguments count! (/mangcheckv <group> <variable>)");
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
		auxGroup2 = permissionHandler.nextGroupWithVariable(auxGroup, args[1]);
		if (auxGroup2 == null) {
			sender.sendMessage(ChatColor.RED + "The group doesn't have access to that variable!");
		}
		// Seems OK
		sender.sendMessage(ChatColor.YELLOW + "The value of variable '" + ChatColor.GOLD + args[1] + ChatColor.YELLOW + "' is: '" + ChatColor.GREEN + auxGroup2.getVariables().getVarObject(args[1]).toString() + ChatColor.WHITE + "'");
		if (!auxGroup.equals(auxGroup2)) {
			sender.sendMessage(ChatColor.YELLOW + "And the value was inherited from group: " + ChatColor.GREEN + auxGroup2.getName());
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
