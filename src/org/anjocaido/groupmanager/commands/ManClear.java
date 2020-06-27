/**
 * 
 */
package org.anjocaido.groupmanager.commands;

import org.anjocaido.groupmanager.GroupManager;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;


/**
 * @author ElgarL
 *
 */
public class ManClear extends BaseCommand {

	/**
	 * 
	 */
	public ManClear() {}

	@Override
	protected boolean parseCommand(@NotNull String[] args) {

		if (args.length != 0) {
			sender.sendMessage(ChatColor.RED + "Review your arguments count!");
			return false;
		}
		GroupManager.getSelectedWorlds().remove(sender.getName());
		sender.sendMessage(ChatColor.YELLOW + "You have removed your world selection. Working with current world (if possible).");

		return true;
	}

}
