/**
 * 
 */
package org.anjocaido.groupmanager.commands;

import java.util.ArrayList;
import java.util.List;

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.dataholder.OverloadedWorldHolder;
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
public class ManCheckW extends BaseCommand implements TabCompleter {

	/**
	 * 
	 */
	public ManCheckW() {}

	@Override
	protected boolean parseCommand(@NotNull String[] args) {

		if (args.length < 1) {
			sender.sendMessage(ChatColor.RED + "Review your arguments count! (/mancheckw <world>)");
			sender.sendMessage(ChatColor.YELLOW + "Worlds available: ");
			ArrayList<OverloadedWorldHolder> worlds = GroupManager.getWorldsHolder().allWorldsDataList();
			auxString = "";
			for (int i = 0; i < worlds.size(); i++) {
				auxString += worlds.get(i).getName();
				if ((i + 1) < worlds.size()) {
					auxString += ", ";
				}
			}
			sender.sendMessage(ChatColor.YELLOW + auxString);
			return false;
		}
		
		auxString = "";
		for (int i = 0; i < args.length; i++) {
			if (args[i] == null) {
				GroupManager.logger.warning("Bukkit gave invalid arguments array! Cmd: " + this.getClass().getSimpleName() + " args.length: " + args.length);
				return false;
			}
			auxString += args[i];
			if (i < (args.length - 1)) {
				auxString += " ";
			}
		}
		dataHolder = GroupManager.getWorldsHolder().getWorldData(auxString);
		
		sender.sendMessage(ChatColor.YELLOW + "You have selected world '" + dataHolder.getName() + "'.");
		sender.sendMessage(ChatColor.YELLOW + "This world is using the following data files..");
		sender.sendMessage(ChatColor.YELLOW + "Groups:" + ChatColor.GREEN + " " + dataHolder.getGroupsFile().getAbsolutePath());
		sender.sendMessage(ChatColor.YELLOW + "Users:" + ChatColor.GREEN + " " + dataHolder.getUsersFile().getAbsolutePath());

		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
		
		parseSender(sender, alias);
		
		/*
		 * Populate the first argument of TabComplete with a list of valid world roots.
		 */
		if (args.length == 1) {
			return getWorlds();
		}
		
		return new ArrayList<String>();
	}

}
