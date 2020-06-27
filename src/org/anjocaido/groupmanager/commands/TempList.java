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
public class TempList extends BaseCommand {

	/**
	 * 
	 */
	public TempList() {}

	@Override
	protected boolean parseCommand(@NotNull String[] args) {

		// Validating state of sender
		if (dataHolder == null || permissionHandler == null) {
			if (!setDefaultWorldHandler(sender))
				return true;
		}
		// WORKING
		auxString = "";
		ArrayList<User> removeList = new ArrayList<User>();
		int count = 0;
		
		for (User u : GroupManager.getOverloadedUsers().get(dataHolder.getName().toLowerCase())) {
			if (!dataHolder.isOverloaded(u.getUUID())) {
				removeList.add(u);
			} else {
				auxString += u.getLastName() + ", ";
				count++;
			}
		}
		if (count == 0) {
			sender.sendMessage(ChatColor.YELLOW + "There are no users in overload mode.");
			return true;
		}
		auxString = auxString.substring(0, auxString.lastIndexOf(","));
		if (GroupManager.getOverloadedUsers().get(dataHolder.getName().toLowerCase()) == null) {
			GroupManager.getOverloadedUsers().put(dataHolder.getName().toLowerCase(), new ArrayList<User>());
		}
		GroupManager.getOverloadedUsers().get(dataHolder.getName().toLowerCase()).removeAll(removeList);
		sender.sendMessage(ChatColor.YELLOW + " " + count + " Users in overload mode: " + ChatColor.WHITE + auxString);

		return true;
	}

}
