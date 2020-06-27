/**
 * 
 */
package org.anjocaido.groupmanager.commands;

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.data.Group;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;


/**
 * @author ElgarL
 *
 */
public class ListGroups extends BaseCommand {

	/**
	 * 
	 */
	public ListGroups() {}

	@Override
	protected boolean parseCommand(@NotNull String[] args) {

		// Validating state of sender
		if (dataHolder == null || permissionHandler == null) {
			if (!setDefaultWorldHandler(sender))
				return true;
		}
		// WORKING
		auxString = "";
		String auxString2 = "";
		for (Group g : dataHolder.getGroupList()) {
			auxString += g.getName() + ", ";
		}
		for (Group g : GroupManager.getGlobalGroups().getGroupList()) {
			auxString2 += g.getName() + ", ";
		}
		if (auxString.lastIndexOf(",") > 0) {
			auxString = auxString.substring(0, auxString.lastIndexOf(","));
		}
		if (auxString2.lastIndexOf(",") > 0) {
			auxString2 = auxString2.substring(0, auxString2.lastIndexOf(","));
		}
		sender.sendMessage(ChatColor.YELLOW + "Groups Available: " + ChatColor.WHITE + auxString);
		sender.sendMessage(ChatColor.YELLOW + "GlobalGroups Available: " + ChatColor.WHITE + auxString2);

		return true;
	}

}
