/**
 * 
 */
package org.anjocaido.groupmanager.commands;

import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;


/**
 * @author ElgarL
 *
 */
public class ManToggleSave extends BaseCommand {

	/**
	 * 
	 */
	public ManToggleSave() {}

	@Override
	protected boolean parseCommand(@NotNull String[] args) {

		if (!plugin.isSchedulerRunning()) {
			plugin.enableScheduler();
			sender.sendMessage(ChatColor.YELLOW + "The auto-saving is enabled!");
		} else {
			plugin.disableScheduler();
			sender.sendMessage(ChatColor.YELLOW + "The auto-saving is disabled!");
		}
		return true;
	}

}
