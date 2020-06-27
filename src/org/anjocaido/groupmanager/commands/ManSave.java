/**
 * 
 */
package org.anjocaido.groupmanager.commands;

import java.util.Arrays;
import java.util.List;

import org.anjocaido.groupmanager.GroupManager;
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
public class ManSave extends BaseCommand implements TabCompleter {

	/**
	 * 
	 */
	public ManSave() {}

	@Override
	protected boolean parseCommand(@NotNull String[] args) {

		boolean forced = false;

		if ((args.length == 1) && (args[0].equalsIgnoreCase("force")))
			forced = true;

		try {
			GroupManager.getWorldsHolder().saveChanges(forced);
			sender.sendMessage(ChatColor.YELLOW + "All changes were saved.");
		} catch (IllegalStateException ex) {
			sender.sendMessage(ChatColor.RED + ex.getMessage());
		}
		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {

		if (args.length == 1) {
			
			return Arrays.asList("Forced");
		}
		return null;
	}

}
