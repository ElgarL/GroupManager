/**
 * 
 */
package org.anjocaido.groupmanager.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.data.User;
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
public class ManUDelSub extends BaseCommand implements TabCompleter {

	/**
	 * 
	 */
	public ManUDelSub() {}

	@Override
	protected boolean parseCommand(@NotNull String[] args) {

		// Validating state of sender
		if (dataHolder == null || permissionHandler == null) {
			if (!setDefaultWorldHandler(sender))
				return true;
		}
		// Validating arguments
		if (args.length != 2) {
			sender.sendMessage(ChatColor.RED + "Review your arguments count! (/manudelsub <user> <group>)");
			return true;
		}
		if ((plugin.isValidateOnlinePlayer()) && ((match = validatePlayer(args[0], sender)) == null)) {
			return false;
		}

		if (match != null) {
			auxUser = dataHolder.getUser(match.toString());
		} else {
			auxUser = dataHolder.getUser(args[0]);
		}
		auxGroup = dataHolder.getGroup(args[1]);
		if (auxGroup == null) {
			sender.sendMessage(ChatColor.RED + "'" + args[1] + "' Group doesnt exist!");
			return true;
		}

		// Validating permission
		if (!isConsole && !isOpOverride && (senderGroup != null ? permissionHandler.inGroup(auxUser.getUUID(), senderGroup.getName()) : false)) {
			sender.sendMessage(ChatColor.RED + "You can't modify a player with same permissions as you, or higher.");
			return true;
		}
		// Seems OK
		auxUser.removeSubGroup(auxGroup);
		sender.sendMessage(ChatColor.YELLOW + "You removed subgroup '" + auxGroup.getName() + "' from player '" + auxUser.getLastName() + "' list.");

		// If the player is online, this will create new data for the user.
		if (auxUser.getUUID() != null) {
			targetPlayer = plugin.getServer().getPlayer(UUID.fromString(auxUser.getUUID()));
			if (targetPlayer != null)
				GroupManager.getBukkitPermissions().updatePermissions(targetPlayer);
		}
				
		return true;
	}
	
	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {

		parseSender(sender, alias);
		
		List<String> result = new ArrayList<String>();
		
		/*
		 * Return a TabComplete for users.
		 */
		if (args.length == 1) {

			for (User user : dataHolder.getUserList()) {
				result.add(user.getLastName());
			}
			return result;
		}
		
		if (args.length == 2) {
			if ((plugin.isValidateOnlinePlayer()) && ((match = validatePlayer(args[0], sender)) == null)) {
				return null;
			}

			if (match != null) {
				auxUser = dataHolder.getUser(match.toString());
			} else {
				auxUser = dataHolder.getUser(args[0]);
			}

			return auxUser.subGroupListStringCopy();

		}
		
		return null;
	}

}
