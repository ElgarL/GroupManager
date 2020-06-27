/**
 * 
 */
package org.anjocaido.groupmanager.commands;

import java.util.ArrayList;
import java.util.List;

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
public class ManGAdd extends BaseCommand implements TabCompleter {

	/**
	 * 
	 */
	public ManGAdd() {}

	@Override
	protected boolean parseCommand(@NotNull String[] args) {

		// Validating state of sender
		if (dataHolder == null || permissionHandler == null) {
			if (!setDefaultWorldHandler(sender))
				return true;
		}
		// Validating arguments
		if (args.length != 1) {
			sender.sendMessage(ChatColor.RED + "Review your arguments count! (/mangadd <group>)");
			return true;
		}
		auxGroup = dataHolder.getGroup(args[0]);
		if (auxGroup != null) {
			sender.sendMessage(ChatColor.RED + "'" + args[0] + "' Group already exists!");
			return true;
		}
		// Seems OK
		auxGroup = dataHolder.createGroup(args[0]);
		sender.sendMessage(ChatColor.YELLOW + "You created a group named: " + auxGroup.getName());

		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {

		parseSender(sender, alias);
		/*
		 * Return an empty list so there is no TabComplete on this. It should be a new group name.
		 */
		return new ArrayList<String>();
	}

}
