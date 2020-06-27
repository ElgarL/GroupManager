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
public class ManWorld extends BaseCommand {

	/**
	 * 
	 */
	public ManWorld() {}

	@Override
	protected boolean parseCommand(@NotNull String[] args) {

		auxString = GroupManager.getSelectedWorlds().get(sender.getName());
		
		if (auxString != null) {
			sender.sendMessage(ChatColor.YELLOW + "You have the world '" + dataHolder.getName() + "' in your selection.");
		} else {
			if (dataHolder == null) {
				sender.sendMessage(ChatColor.YELLOW + "There is no world selected. And no world is available now.");
			} else {
				sender.sendMessage(ChatColor.YELLOW + "You don't have a world in your selection..");
				sender.sendMessage(ChatColor.YELLOW + "Working with the direct world where your player is.");
				sender.sendMessage(ChatColor.YELLOW + "Your world now uses permissions of world name: '" + dataHolder.getName() + "' ");
			}
		}

		return true;
	}

}
