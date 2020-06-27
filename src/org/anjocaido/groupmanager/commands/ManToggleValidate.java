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
public class ManToggleValidate extends BaseCommand {

	/**
	 * 
	 */
	public ManToggleValidate() {}

	@Override
	protected boolean parseCommand(@NotNull String[] args) {

		// Toggle validation of player names.
		plugin.setValidateOnlinePlayer(!plugin.isValidateOnlinePlayer());
		
		sender.sendMessage(ChatColor.YELLOW + "Validate if player is online, now set to: " + Boolean.toString(plugin.isValidateOnlinePlayer()));
		if (!plugin.isValidateOnlinePlayer()) {
			sender.sendMessage(ChatColor.GOLD + "From now on you can edit players that are not connected... BUT:");
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "From now on you should type the whole name of the player, correctly.");
		}
		return true;
	}

}
