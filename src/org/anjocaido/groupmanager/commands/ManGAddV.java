/**
 * 
 */
package org.anjocaido.groupmanager.commands;

import java.util.ArrayList;
import java.util.List;

import org.anjocaido.groupmanager.data.Group;
import org.anjocaido.groupmanager.data.Variables;
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
public class ManGAddV extends BaseCommand implements TabCompleter {

	/**
	 * 
	 */
	public ManGAddV() {}

	@Override
	protected boolean parseCommand(@NotNull String[] args) {

		// Validating state of sender
		if (dataHolder == null || permissionHandler == null) {
			if (!setDefaultWorldHandler(sender))
				return true;
		}
		// Validating arguments
		if (args.length < 3) {
			sender.sendMessage(ChatColor.RED + "Review your arguments count! (/mangaddv <group> <variable> <value>)");
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
		// Seems OK
		auxString = "";
		for (int i = 2; i < args.length; i++) {
			auxString += args[i];
			if ((i + 1) < args.length) {
				auxString += " ";
			}
		}
		
		auxString = auxString.replace("'", "");
		auxGroup.getVariables().addVar(args[1], Variables.parseVariableValue(auxString));
		sender.sendMessage(ChatColor.YELLOW + "Variable " + ChatColor.GOLD + args[1] + ChatColor.YELLOW + ":'" + ChatColor.GREEN + auxString + ChatColor.YELLOW + "' added to the group " + auxGroup.getName());

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
