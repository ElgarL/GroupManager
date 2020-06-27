/**
 * 
 */
package org.anjocaido.groupmanager.commands;

import java.util.ArrayList;

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.data.User;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;


/**
 * @author ElgarL
 *
 */
public class TempDelAll extends BaseCommand {

	/**
	 * 
	 */
	public TempDelAll() {}

	@Override
	protected boolean parseCommand(@NotNull String[] args) {

		// Validating state of sender
		if (dataHolder == null || permissionHandler == null) {
			if (!setDefaultWorldHandler(sender))
				return true;
		}
		// WORKING
		int count = 0;
		
		for (User u : GroupManager.getOverloadedUsers().get(dataHolder.getName().toLowerCase())) {
			if (dataHolder.isOverloaded(u.getUUID())) {
				dataHolder.removeOverload(u.getUUID());
				count++;
			}
		}
		if (count == 0) {
			sender.sendMessage(ChatColor.YELLOW + "There are no users in overload mode.");
			return true;
		}
		if (GroupManager.getOverloadedUsers().get(dataHolder.getName().toLowerCase()) == null) {
			GroupManager.getOverloadedUsers().put(dataHolder.getName().toLowerCase(), new ArrayList<User>());
		}
		GroupManager.getOverloadedUsers().get(dataHolder.getName().toLowerCase()).clear();
		sender.sendMessage(ChatColor.YELLOW + " " + count + "All users in overload mode are now normal again.");

		return true;
	}

}
