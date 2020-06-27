/**
 * 
 */
package org.anjocaido.groupmanager.commands;

import java.util.ArrayList;
import java.util.List;

import org.anjocaido.groupmanager.GroupManager;
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
public class ManGDel extends BaseCommand implements TabCompleter {

	/**
	 * 
	 */
	public ManGDel() {}

	@Override
	protected boolean parseCommand(@NotNull String[] args) {

		// Validating state of sender
		if (dataHolder == null || permissionHandler == null) {
			if (!setDefaultWorldHandler(sender))
				return true;
		}
		// Validating arguments
		if (args.length != 1) {
			sender.sendMessage(ChatColor.RED + "Review your arguments count! (/mangdel <group>)");
			return false;
		}
		auxGroup = dataHolder.getGroup(args[0]);
		if (auxGroup == null) {
			sender.sendMessage(ChatColor.RED + "'" + args[0] + "' Group doesnt exist!");
			return true;
		}
		// Seems OK
		dataHolder.removeGroup(auxGroup.getName());
		sender.sendMessage(ChatColor.YELLOW + "You deleted a group named " + auxGroup.getName() + ", it's users are default group now.");

		GroupManager.getBukkitPermissions().updateAllPlayers();

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
