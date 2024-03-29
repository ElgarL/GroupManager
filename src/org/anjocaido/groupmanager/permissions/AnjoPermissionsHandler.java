/*
 *  GroupManager - A plug-in for Spigot/Bukkit based Minecraft servers.
 *  Copyright (C) 2020  ElgarL
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.anjocaido.groupmanager.permissions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.data.Group;
import org.anjocaido.groupmanager.data.User;
import org.anjocaido.groupmanager.dataholder.WorldDataHolder;
import org.anjocaido.groupmanager.utils.PermissionCheckResult;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Everything here maintains the model created by Nijikokun
 * 
 * But implemented to use GroupManager system. Which provides instant changes,
 * without file access.
 * 
 * It holds permissions only for one single world.
 * 
 * @author gabrielcouto, ElgarL
 */
public class AnjoPermissionsHandler extends PermissionsReaderInterface {

	private WorldDataHolder ph;

	/**
	 * It needs a WorldDataHolder to work with.
	 * 
	 * @param holder
	 */
	public AnjoPermissionsHandler(WorldDataHolder holder) {

		ph = holder;
	}

	/**
	 * A short name method, for permission method.
	 * 
	 * @param player
	 * @param permission
	 * @return true if the player has the permission
	 */
	@Override
	public boolean has(Player player, String permission) {

		return permission(player, permission);
	}

	/**
	 * Checks if a Player has that permission node in their current world.
	 * Includes Bukkit permissions assigned by other plug-ins and defaults.
	 * Supports wildcards.
	 * 
	 * @param player
	 * @param permission
	 * @return true if the player has the permission
	 */
	@Override
	public boolean permission(Player player, String permission) {

		PermissionCheckResult result = checkFullUserPermission(ph.getUser(player.getUniqueId().toString()), permission);
		return result.resultType == PermissionCheckResult.Type.EXCEPTION || result.resultType == PermissionCheckResult.Type.FOUND;
	}

	/**
	 * Checks if a User has that permission node.
	 * Will NOT include Bukkit assigned permissions.
	 * Supports wildcards.
	 * 
	 * @param playerName
	 * @param permission
	 * @return true if the player has the permission
	 */
	public boolean permission(String playerName, String permission) {

		return checkUserPermission(ph.getUser(playerName), permission);
	}

	/**
	 * Returns the name of the group of that player name.
	 * 
	 * @param userName
	 * @return String of players group name.
	 */
	@Override
	public String getGroup(String userName) {

		return ph.getUser(userName).getGroup().getName();
	}

	/**
	 * Returns All permissions (including inheritance and sub groups) for the
	 * player, including child nodes from Bukkit.
	 * 
	 * @param userName
	 * @return List<String> of all players permissions.
	 */
	@Override
	public List<String> getAllPlayersPermissions(String userName) {

		return new ArrayList<>(getAllPlayersPermissions(userName, true));
	}

	/**
	 * Returns All permissions (including inheritance and sub groups) for the
	 * player. With or without Bukkit child nodes.
	 * 
	 * @param userName
	 * @return Set<String> of all players permissions.
	 */
	@Override
	public Set<String> getAllPlayersPermissions(String userName, Boolean includeChildren) {

		Set<String> overrides = new LinkedHashSet<>();

		// Add the players own permissions.
		Set<String> playerPermArray = new LinkedHashSet<>(populatePerms(ph.getUser(userName).getPermissionList(), includeChildren));

		ArrayList<String> alreadyProcessed = new ArrayList<>();

		// fetch all group permissions
		for (String group : getGroups(userName)) {
			// Don't process a group more than once.
			if (!alreadyProcessed.contains(group)) {
				alreadyProcessed.add(group);

				Set<String> groupPermArray;

				if (group.startsWith("g:") && GroupManager.getGlobalGroups().hasGroup(group)) {
					// GlobalGroups
					groupPermArray = populatePerms(GroupManager.getGlobalGroups().getGroupsPermissions(group), includeChildren);

				} else {
					// World Groups
					groupPermArray = populatePerms(ph.getGroup(group).getPermissionList(), includeChildren);
				}

				// Add all group permissions, unless negated by earlier permissions.
				for (String perm : groupPermArray) {
					boolean negated = (perm.startsWith("-"));

					// Overridden (Exception) permission defeats negation.
					if (perm.startsWith("+")) {
						overrides.add(perm.substring(1));
						continue;
					}

					// Perm doesn't already exists and there is no negation for it
					// or It's a negated perm where a normal perm doesn't exists (don't allow inheritance to negate higher perms)
					if ((!negated && !playerPermArray.contains(perm) && !wildcardNegation(playerPermArray, perm)) || (negated && !playerPermArray.contains(perm.substring(1)) && !wildcardNegation(playerPermArray, perm.substring(1))))
						playerPermArray.add(perm);

				}
			}

		}

		// Process overridden permissions

		for (String node : overrides) {

			playerPermArray.remove("-" + node);

			playerPermArray.add(node);

		}

		// Collections.sort(playerPermArray, StringPermissionComparator.getInstance());

		return playerPermArray;
	}

	/**
	 * Is there a direct or wildcard negation in the list which covers this permission node.
	 * 
	 * @param playerPermArray
	 * @param node
	 * @return
	 */
	private boolean wildcardNegation(Set<String> playerPermArray, String node) {

		/*
		 * Check for a negated parent with a wildcard or negated permission
		 */

		if (playerPermArray.contains("-" + node))
			return true;

		final String[] parts = node.split("\\.");
		final StringBuilder builder = new StringBuilder(node.length());
		for (String part : parts) {
			builder.append('*');
			if (playerPermArray.contains("-" + builder)) {
				GroupManager.logger.log(Level.FINE, "Wildcard Negation found for " + node);
				return true;
			}

			builder.deleteCharAt(builder.length() - 1);
			builder.append(part).append('.');
		}

		/*
		 * No negated parent found so return false.
		 */
		GroupManager.logger.log(Level.FINE, "No Negation found for " + node);
		return false;

	}

	private Set<String> populatePerms(List<String> permsList, boolean includeChildren) {

		// Create a new array so it's modifiable.
		List<String> perms = new ArrayList<>(permsList);
		Set<String> permArray = new LinkedHashSet<>();
		boolean allPerms = false;

		// Allow * node to populate ALL permissions to Bukkit.
		if (perms.contains("*")) {
			permArray.addAll(GroupManager.getBukkitPermissions().getAllRegisteredPermissions(includeChildren));
			allPerms = true;
			perms.remove("*");
			// Remove the no offline perms node as this should not be given.
			perms.remove("groupmanager.noofflineperms");
		}

		for (String perm : perms) {
			/*
			  all permission sets are passed here pre-sorted, alphabetically.
			  This means negated nodes will be processed before all permissions
			  other than *.
			 */
			boolean negated = perm.startsWith("-");

			if (!permArray.contains(perm)) {
				permArray.add(perm);

				if ((negated))
					permArray.remove(perm.substring(1));

				/*
				  Process child nodes if required,
				  or this is a negated node AND we used * to include all
				  permissions,
				  in which case we need to remove all children of that node.
				 */
				if ((includeChildren) || (negated && allPerms)) {

					Map<String, Boolean> children = GroupManager.getBukkitPermissions().getAllChildren((negated ? perm.substring(1) : perm), new LinkedHashSet<>());

					if (children != null) {
						if (negated)
							if (allPerms) {

								// Remove children of negated nodes
								for (String child : children.keySet())
									if (children.get(child))
										permArray.remove(child);

							} else {

								// Add child nodes
								for (String child : children.keySet())
									if (children.get(child))
										if ((!permArray.contains(child)) && (!permArray.contains("-" + child)))
											permArray.add(child);
							}
					}
				}
			}
		}

		return permArray;
	}

	/**
	 * Verify if player is in such group. It will check it's groups inheritance.
	 * 
	 * So if you have a group Admin > Moderator
	 * 
	 * And verify the player 'MyAdmin', which is Admin, it will return true for
	 * both Admin or Moderator groups.
	 * 
	 * If you have a player 'MyModerator', which is Moderator, it will give
	 * false if you pass Admin in group parameter.
	 * 
	 * @param name
	 * @param group
	 * @return true if in group (with inheritance)
	 */
	@Override
	public boolean inGroup(String name, String group) {

		if (hasGroupInInheritance(ph.getUser(name).getGroup(), group)) {
			return true;
		}
		for (Group subGroup : ph.getUser(name).subGroupListCopy()) {
			if (hasGroupInInheritance(subGroup, group)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Gets the appropriate prefix for the user. This method is a utility method
	 * for chat plugins to get the user's prefix without having to look at every
	 * one of the user's ancestors. Returns an empty string if user has no
	 * parent groups.
	 * 
	 * @param user
	 *            Player's name
	 * @return Player's prefix
	 */
	@Override
	public String getUserPrefix(String user) {

		// Check for a direct prefix
		String prefix = ph.getUser(user).getVariables().getVarString("prefix");
		if (!prefix.isEmpty())
			return prefix;

		// Check for a main group prefix
		prefix = getGroupPrefix(getGroup(user));
		if (!prefix.isEmpty())
			return prefix;

		// Check for a subgroup prefix
		for (String group : ph.getUser(user).subGroupListStringCopy()) {
			prefix = getGroupPrefix(group);
			if (!prefix.isEmpty())
				break;
		}

		return prefix;
	}

	/**
	 * Gets the appropriate suffix for the user. This method is a utility method
	 * for chat plugins to get the user's suffix without having to look at every
	 * one of the user's ancestors. Returns an empty string if user has no
	 * parent groups.
	 * 
	 * @param user
	 *            Player's name
	 * @return Player's suffix
	 */
	@Override
	public String getUserSuffix(String user) {

		// Check for a direct suffix
		String suffix = ph.getUser(user).getVariables().getVarString("suffix");
		if (!suffix.isEmpty())
			return suffix;

		// Check for a main group suffix
		suffix = getGroupSuffix(getGroup(user));
		if (!suffix.isEmpty())
			return suffix;

		// Check for a subgroup suffix
		for (String group : ph.getUser(user).subGroupListStringCopy()) {
			suffix = getGroupSuffix(group);
			if (!suffix.isEmpty())
				break;
		}

		return suffix;
	}

	/**
	 * Gets name of the primary group of the user. Returns the name of the
	 * default group if user has no parent groups, or "Default" if there is no
	 * default group for that world.
	 * 
	 * @param user
	 *            Player's name
	 * @return Name of player's primary group
	 */
	public String getPrimaryGroup(String user) {

		return getGroup(user);

	}

	/**
	 * Check if user can build. Checks inheritance and subgroups.
	 * 
	 * @param userName
	 *            Player's name
	 * @return true if the user can build
	 */
	public boolean canUserBuild(String userName) {

		return getPermissionBoolean(userName, "build");
	}

	/**
	 * Returns the String prefix for the given group
	 * 
	 * @param groupName
	 * @return empty string if found none.
	 */
	@Override
	public String getGroupPrefix(String groupName) {

		Group g = ph.getGroup(groupName);
		return g == null ? "" :  g.getVariables().getVarString("prefix");
	}

	/**
	 * Return the suffix for the given group name
	 * 
	 * @param groupName
	 * @return empty string if not found.
	 */
	@Override
	public String getGroupSuffix(String groupName) {

		Group g = ph.getGroup(groupName);
		if (g == null) {
			return "";
		}
		return g.getVariables().getVarString("suffix");
	}

	/**
	 * Checks the specified group for the Info Build node. Does NOT check
	 * inheritance
	 * 
	 * @param groupName
	 * @return true if can build
	 */
	@Override
	public boolean canGroupBuild(String groupName) {

		Group g = ph.getGroup(groupName);
		return g != null && g.getVariables().getVarBoolean("build");
	}

	/**
	 * It returns a string variable value, set in the INFO node of the group. It
	 * will harvest inheritance for value.
	 * 
	 * @param groupName
	 * @param variable
	 * @return null if no group with that variable is found.
	 */
	@Override
	public String getGroupPermissionString(String groupName, String variable) {

		Group start = ph.getGroup(groupName);
		if (start == null) {
			return null;
		}
		Group result = nextGroupWithVariable(start, variable);
		return result == null ? null : result.getVariables().getVarString(variable);
	}

	/**
	 * It returns a Integer variable value It will harvest inheritance for
	 * value.
	 * 
	 * @param groupName
	 * @param variable
	 * @return -1 if none found or not parseable.
	 */
	@Override
	public int getGroupPermissionInteger(String groupName, String variable) {

		Group start = ph.getGroup(groupName);
		if (start == null) {
			return -1;
		}
		Group result = nextGroupWithVariable(start, variable);
		return result == null ? -1 : result.getVariables().getVarInteger(variable);
	}

	/**
	 * Returns a boolean for given variable in INFO node. It will harvest
	 * inheritance for value.
	 * 
	 * @param group
	 * @param variable
	 * @return false if not found/not parseable.
	 */
	@Override
	public boolean getGroupPermissionBoolean(String group, String variable) {

		Group start = ph.getGroup(group);
		if (start == null) {
			return false;
		}
		Group result = nextGroupWithVariable(start, variable);
		return result != null && result.getVariables().getVarBoolean(variable);
	}

	/**
	 * Returns a double value for the given variable name in INFO node. It will
	 * harvest inheritance for value.
	 * 
	 * @param group
	 * @param variable
	 * @return -1 if not found / not parseable.
	 */
	@Override
	public double getGroupPermissionDouble(String group, String variable) {

		Group start = ph.getGroup(group);
		if (start == null) {
			return -1;
		}
		Group result = nextGroupWithVariable(start, variable);
		return result == null ? -1 : result.getVariables().getVarDouble(variable);
	}

	/**
	 * Returns the variable value of the user, in INFO node.
	 * 
	 * @param user
	 * @param variable
	 * @return empty string if not found
	 */
	@Override
	public String getUserPermissionString(String user, String variable) {

		User auser = ph.getUser(user);
		return auser == null ? "" : auser.getVariables().getVarString(variable);
	}

	/**
	 * Returns the variable value of the user, in INFO node.
	 * 
	 * @param user
	 * @param variable
	 * @return -1 if not found
	 */
	@Override
	public int getUserPermissionInteger(String user, String variable) {

		User auser = ph.getUser(user);
		return auser == null ? -1 : auser.getVariables().getVarInteger(variable);
	}

	/**
	 * Returns the variable value of the user, in INFO node.
	 * 
	 * @param user
	 * @param variable
	 * @return boolean value
	 */
	@Override
	public boolean getUserPermissionBoolean(String user, String variable) {

		User auser = ph.getUser(user);
		return auser != null && auser.getVariables().getVarBoolean(variable);
	}

	/**
	 * Returns the variable value of the user, in INFO node.
	 * 
	 * @param user
	 * @param variable
	 * @return -1 if not found
	 */
	@Override
	public double getUserPermissionDouble(String user, String variable) {

		User auser = ph.getUser(user);
		return auser == null ? -1 : auser.getVariables().getVarDouble(variable);
	}

	/**
	 * Returns the variable value of the user, in INFO node. If not found, it
	 * will search for his Group variables. It will harvest the inheritance and
	 * subgroups.
	 * 
	 * @param user
	 * @param variable
	 * @return empty string if not found
	 */
	@Override
	public String getPermissionString(String user, String variable) {

		User auser = ph.getUser(user);
		if (auser == null) {
			return "";
		}
		if (auser.getVariables().hasVar(variable)) {
			return auser.getVariables().getVarString(variable);
		}
		Group start = auser.getGroup();
		if (start == null) {
			return "";
		}
		Group result = nextGroupWithVariable(start, variable);
		if (result == null) {
			// Check sub groups
			if (!auser.isSubGroupsEmpty())
				for (Group subGroup : auser.subGroupListCopy()) {
					result = nextGroupWithVariable(subGroup, variable);
					// Found value?
				}
		}
		return result == null ? "" : result.getVariables().getVarString(variable);
	}

	/**
	 * Returns the variable value of the user, in INFO node. If not found, it
	 * will search for his Group variables. It will harvest the inheritance and
	 * subgroups.
	 * 
	 * @param user
	 * @param variable
	 * @return -1 if not found
	 */
	@Override
	public int getPermissionInteger(String user, String variable) {

		User auser = ph.getUser(user);
		if (auser == null) {
			return -1;
		}
		if (auser.getVariables().hasVar(variable)) {
			return auser.getVariables().getVarInteger(variable);
		}
		Group start = auser.getGroup();
		if (start == null) {
			return -1;
		}
		Group result = nextGroupWithVariable(start, variable);
		if (result == null) {
			// Check sub groups
			if (!auser.isSubGroupsEmpty())
				for (Group subGroup : auser.subGroupListCopy()) {
					result = nextGroupWithVariable(subGroup, variable);
					// Found value?
				}
		}
		return result == null ? -1 : result.getVariables().getVarInteger(variable);
	}

	/**
	 * Returns the variable value of the user, in INFO node. If not found, it
	 * will search for his Group variables. It will harvest the inheritance and
	 * subgroups.
	 * 
	 * @param user
	 * @param variable
	 * @return false if not found or not parseable to true.
	 */
	@Override
	public boolean getPermissionBoolean(String user, String variable) {

		User auser = ph.getUser(user);
		if (auser == null) {
			return false;
		}
		if (auser.getVariables().hasVar(variable)) {
			return auser.getVariables().getVarBoolean(variable);
		}
		Group start = auser.getGroup();
		if (start == null) {
			return false;
		}
		Group result = nextGroupWithVariable(start, variable);
		if (result == null) {
			// Check sub groups
			if (!auser.isSubGroupsEmpty())
				for (Group subGroup : auser.subGroupListCopy()) {
					result = nextGroupWithVariable(subGroup, variable);
					// Found value?
				}
		}
		return result != null && result.getVariables().getVarBoolean(variable);
	}

	/**
	 * Returns the variable value of the user, in INFO node. If not found, it
	 * will search for his Group variables. It will harvest the inheritance and
	 * subgroups.
	 * 
	 * @param user
	 * @param variable
	 * @return -1 if not found.
	 */
	@Override
	public double getPermissionDouble(String user, String variable) {

		User auser = ph.getUser(user);
		if (auser == null) {
			return -1.0D;
		}
		if (auser.getVariables().hasVar(variable)) {
			return auser.getVariables().getVarDouble(variable);
		}
		Group start = auser.getGroup();
		if (start == null) {
			return -1.0D;
		}
		Group result = nextGroupWithVariable(start, variable);
		if (result == null) {
			// Check sub groups
			if (!auser.isSubGroupsEmpty())
				for (Group subGroup : auser.subGroupListCopy()) {
					result = nextGroupWithVariable(subGroup, variable);
					// Found value?
				}
		}
		return result == null ? -1.0D : result.getVariables().getVarDouble(variable);
	}

	/**
	 * Does not include User's group permission
	 * 
	 * @param user
	 * @param permission
	 * @return PermissionCheckResult
	 */
	public PermissionCheckResult checkUserOnlyPermission(User user, String permission) {

		PermissionCheckResult result = new PermissionCheckResult();
		result.askedPermission = permission;
		result.owner = user;
		for (String access : user.getPermissionList()) {
			result.resultType = comparePermissionString(access, permission);
			if (result.resultType != PermissionCheckResult.Type.NOTFOUND) {
				GroupManager.logger.fine("Found an " + result.resultType + " for " + permission + " on player " + result.owner.getLastName());
				result.accessLevel = access;
				return result;
			}
		}
		result.resultType = PermissionCheckResult.Type.NOTFOUND;
		return result;
	}

	/**
	 * Returns the node responsible for that permission. Does not include User's
	 * group permission.
	 * 
	 * @param group
	 * @param permission
	 * @return the node if permission is found. if not found, return null
	 */
	public PermissionCheckResult checkGroupOnlyPermission(Group group, String permission) {

		PermissionCheckResult result = new PermissionCheckResult();
		result.owner = group;
		result.askedPermission = permission;
		for (String access : group.getPermissionList()) {
			result.resultType = comparePermissionString(access, permission);
			if (result.resultType != PermissionCheckResult.Type.NOTFOUND) {
				GroupManager.logger.fine("Found an " + result.resultType + " for " + permission + " on group " + group.getName());
				result.accessLevel = access;
				return result;
			}
		}
		result.resultType = PermissionCheckResult.Type.NOTFOUND;
		return result;
	}

	/**
	 * Check permissions, including it's group and inheritance.
	 * 
	 * @param user
	 * @param permission
	 * @return true if permission was found. false if not, or was negated.
	 */
	public boolean checkUserPermission(User user, String permission) {

		PermissionCheckResult result = checkFullGMPermission(user, permission, false);
		return result.resultType == PermissionCheckResult.Type.EXCEPTION || result.resultType == PermissionCheckResult.Type.FOUND;
	}

	/**
	 * Do what checkUserPermission did before. But now returning a
	 * PermissionCheckResult.
	 * 
	 * @param user
	 * @param targetPermission
	 * @return PermissionCheckResult
	 */
	public PermissionCheckResult checkFullUserPermission(User user, String targetPermission) {

		return checkFullGMPermission(user, targetPermission, true);
	}

	/**
	 * Wrapper for offline server checks.
	 * Looks for the 'groupmanager.noofflineperms' permissions and reports no permissions on servers set to offline.
	 * 
	 * Check user and groups with inheritance and Bukkit if bukkit = true return
	 * a PermissionCheckResult.
	 * 
	 * @param user
	 * @param targetPermission
	 * @param checkBukkit
	 * @return PermissionCheckResult
	 */
	public PermissionCheckResult checkFullGMPermission(User user, String targetPermission, Boolean checkBukkit) {

		/*
		 * Report no permissions under the following conditions.
		 * 
		 * We are in offline mode
		 * and the player has the 'groupmanager.noofflineperms' permission.
		 */
		if (user == null || targetPermission == null || targetPermission.isEmpty()
				|| (!Bukkit.getServer().getOnlineMode()
						&& (checkPermission(user, "groupmanager.noofflineperms", false).resultType == PermissionCheckResult.Type.FOUND))) {

			PermissionCheckResult result = new PermissionCheckResult();
			result.accessLevel = targetPermission;
			result.resultType = PermissionCheckResult.Type.NOTFOUND;

			return result;
		}

		return checkPermission(user, targetPermission, checkBukkit);
	}

	/**
	 * 
	 * Check user and groups with inheritance and Bukkit if bukkit = true return
	 * a PermissionCheckResult.
	 * 
	 * @param user
	 * @param targetPermission
	 * @param checkBukkit
	 * @return PermissionCheckResult
	 */
	private PermissionCheckResult checkPermission(User user, String targetPermission, Boolean checkBukkit) {

		PermissionCheckResult result = new PermissionCheckResult();
		result.accessLevel = targetPermission;
		result.resultType = PermissionCheckResult.Type.NOTFOUND;
		
		PermissionCheckResult resultUser = checkUserOnlyPermission(user, targetPermission);
		if (resultUser.resultType != PermissionCheckResult.Type.NOTFOUND) {

			resultUser.accessLevel = targetPermission;

			if (resultUser.resultType == PermissionCheckResult.Type.EXCEPTION) {
				return resultUser;
			}

			result = resultUser;

		}

		// IT ONLY CHECKS GROUPS PERMISSIONS IF RESULT FOR USER IS NOT AN EXCEPTION
		PermissionCheckResult resultGroup = checkGroupPermissionWithInheritance(user.getGroup(), targetPermission);
		if (resultGroup.resultType != PermissionCheckResult.Type.NOTFOUND) {

			resultGroup.accessLevel = targetPermission;

			if (resultGroup.resultType == PermissionCheckResult.Type.EXCEPTION) {
				return resultGroup;
			}

			// Do not override higher level permissions with negations.
			if (result.resultType == PermissionCheckResult.Type.NOTFOUND) {
				result = resultGroup;
			}

		}

		// Do we have a high level negation?
		boolean negated = (result.resultType == PermissionCheckResult.Type.NEGATION);

		// SUBGROUPS CHECK
		for (Group subGroup : user.subGroupListCopy()) {

			PermissionCheckResult resultSubGroup = checkGroupPermissionWithInheritance(subGroup, targetPermission);
			if (resultSubGroup.resultType != PermissionCheckResult.Type.NOTFOUND) {

				resultSubGroup.accessLevel = targetPermission;

				// Allow exceptions to override higher level negations
				// but low level negations can not remove higher level permissions.
				if (resultSubGroup.resultType == PermissionCheckResult.Type.EXCEPTION) {

					return resultSubGroup;

				} else if ((resultSubGroup.resultType == PermissionCheckResult.Type.FOUND) && (result.resultType != PermissionCheckResult.Type.NEGATION) && !negated) {

					result = resultSubGroup;

				} else if ((resultSubGroup.resultType == PermissionCheckResult.Type.NEGATION) && !negated) {

					result = resultSubGroup;
				}

			}
		}
		
		// If set to check Bukkit and we found no permission in GroupManager
		if (checkBukkit && result.resultType == PermissionCheckResult.Type.NOTFOUND) {
			// Check Bukkit perms to support plugins which add perms via code
			// (Heroes).
			final Player player = user.getBukkitPlayer();

			if ((player != null) && player.hasPermission(targetPermission)) {
				result.resultType = PermissionCheckResult.Type.FOUND;
				result.owner = user;
			}
		}

		// THEN IT RETURNS A NOT FOUND
		// OR THE RESULT OF THE SUBGROUP SEARCH.
		return result;
	}

	/**
	 * Returns the next group, including inheritance, which contains that
	 * variable name.
	 * 
	 * It does Breadth-first search
	 * 
	 * @param start the starting group to look for
	 * @param targetVariable the variable name
	 * @return The group if found. Null if not.
	 */
	public Group nextGroupWithVariable(Group start, String targetVariable) {

		if (start == null || targetVariable == null) {
			return null;
		}
		LinkedList<Group> stack = new LinkedList<>();
		ArrayList<Group> alreadyVisited = new ArrayList<>();
		stack.push(start);
		alreadyVisited.add(start);
		while (!stack.isEmpty()) {
			Group now = stack.pop();
			if (now.getVariables().hasVar(targetVariable)) {
				return now;
			}
			for (String sonName : now.getInherits()) {
				Group son = ph.getGroup(sonName);
				if (son != null && !alreadyVisited.contains(son)) {
					stack.push(son);
					alreadyVisited.add(son);
				}
			}
		}
		return null;
	}


	/**
	 * Check if given group inherits another group.
	 * 
	 * It does Breadth-first search
	 * 
	 * @param start The group to start the search.
	 * @param askedGroup Name of the group you're looking for
	 * @return true if it inherits the group.
	 */
	public boolean hasGroupInInheritance(Group start, String askedGroup) {

		if (start == null || askedGroup == null) {
			return false;
		}
		LinkedList<Group> stack = new LinkedList<>();
		ArrayList<Group> alreadyVisited = new ArrayList<>();
		stack.push(start);
		alreadyVisited.add(start);
		while (!stack.isEmpty()) {
			Group now = stack.pop();
			if (now.getName().equalsIgnoreCase(askedGroup)) {
				return true;
			}
			for (String sonName : now.getInherits()) {
				Group son = ph.getGroup(sonName);
				if (son != null && !alreadyVisited.contains(son)) {
					stack.push(son);
					alreadyVisited.add(son);
				}
			}
		}
		return false;
	}

	/**
	 * Returns the result of permission check. Including inheritance. If found
	 * anything, the PermissionCheckResult that retuns will include the Group
	 * name, and the result type. Result types will be EXCEPTION, NEGATION,
	 * FOUND.
	 * 
	 * If returned type NOTFOUND, the owner will be null, and ownerType too.
	 * 
	 * It does Breadth-first search
	 * 
	 * @param start
	 * @param targetPermission
	 * @return PermissionCheckResult
	 */
	public PermissionCheckResult checkGroupPermissionWithInheritance(Group start, String targetPermission) {

		if (start == null || targetPermission == null) {
			return null;
		}

		LinkedList<Group> stack = new LinkedList<>();
		List<Group> alreadyVisited = new ArrayList<>();
		PermissionCheckResult result = new PermissionCheckResult();

		stack.push(start);
		alreadyVisited.add(start);

		// Set defaults.
		result.askedPermission = targetPermission;
		result.resultType = PermissionCheckResult.Type.NOTFOUND;

		while (!stack.isEmpty()) {
			Group now = stack.pop();
			PermissionCheckResult resultNow = checkGroupOnlyPermission(now, targetPermission);

			if (!resultNow.resultType.equals(PermissionCheckResult.Type.NOTFOUND)) {

				if (resultNow.resultType.equals(PermissionCheckResult.Type.EXCEPTION)) {
					resultNow.accessLevel = targetPermission;
					GroupManager.logger.fine("Found an " + resultNow.resultType + " for " + targetPermission + " in group " + resultNow.owner.getLastName());
					return resultNow;
				}

				/*
				 * Store the first found permission only.
				 * This will prevent inherited permission negations overriding higher level perms.
				 */
				if (result.resultType.equals(PermissionCheckResult.Type.NOTFOUND)) {
					// No Negation found so store for later
					// as we need to continue looking for an Exception.
					GroupManager.logger.fine("Found an " + resultNow.resultType + " for " + targetPermission + " in group " + resultNow.owner.getLastName());
					result = resultNow;
				}
			}

			for (String sonName : now.getInherits()) {
				Group son = ph.getGroup(sonName);
				if (son != null && !alreadyVisited.contains(son)) {
					// Add rather than push to retain inheritance order.
					stack.add(son);
					alreadyVisited.add(son);
				}
			}
		}

		return result;
	}

	/**
	 * Return whole list of names of groups in a inheritance chain. Including a
	 * starting group.
	 * 
	 * It does Breadth-first search. So closer groups will appear first in list.
	 * 
	 * @param start
	 * @return the group that passed on test. null if no group passed.
	 */
	public ArrayList<String> listAllGroupsInherited(Group start) {

		if (start == null) {
			return null;
		}
		LinkedList<Group> stack = new LinkedList<>();
		ArrayList<String> alreadyVisited = new ArrayList<>();
		stack.push(start);
		alreadyVisited.add(start.getName());
		while (!stack.isEmpty()) {
			Group now = stack.pop();
			for (String sonName : now.getInherits()) {
				Group son = ph.getGroup(sonName);
				if (son != null && !alreadyVisited.contains(son.getName())) {
					stack.push(son);
					alreadyVisited.add(son.getName());
				}
			}
		}
		return alreadyVisited;
	}

	/**
	 * Compare a user permission like 'myplugin.*' against a full plugin
	 * permission name, like 'myplugin.dosomething'. As the example above, will
	 * return true.
	 * 
	 * Please sort permissions before sending them here. So negative tokens get
	 * priority.
	 * 
	 * You must test if it start with negative outside this method. It will only
	 * tell if the nodes are matching or not.
	 * 
	 * Every '-' or '+' in the beginning is ignored. It will match only node
	 * names.
	 * 
	 * @param userAccessLevel
	 * @param fullPermissionName
	 * @return PermissionCheckResult.Type
	 */
	public PermissionCheckResult.Type comparePermissionString(String userAccessLevel, String fullPermissionName) {

		int userAccessLevelLength;
		if (userAccessLevel == null || fullPermissionName == null || fullPermissionName.length() == 0 || (userAccessLevelLength = userAccessLevel.length()) == 0) {
			return PermissionCheckResult.Type.NOTFOUND;
		}

		PermissionCheckResult.Type result = PermissionCheckResult.Type.FOUND;
		int userAccessLevelOffset = 0;
		if (userAccessLevel.charAt(0) == '+') {
			userAccessLevelOffset = 1;
			result = PermissionCheckResult.Type.EXCEPTION;
		} else if (userAccessLevel.charAt(0) == '-') {
			userAccessLevelOffset = 1;
			result = PermissionCheckResult.Type.NEGATION;
		}

		if (fullPermissionName.equals(userAccessLevel)) {
			return result;
		}

		if ("groupmanager.noofflineperms".equals(fullPermissionName)) {
			result = PermissionCheckResult.Type.NOTFOUND;
		}

		if ("*".regionMatches(0, userAccessLevel, userAccessLevelOffset, userAccessLevelLength - userAccessLevelOffset)) {
			return result;
		}
		int fullPermissionNameOffset;
		if (fullPermissionName.charAt(0) == '+' || fullPermissionName.charAt(0) == '-') {
			fullPermissionNameOffset = 1;
		} else {
			fullPermissionNameOffset = 0;
		}

		if (userAccessLevel.charAt(userAccessLevel.length() - 1) == '*') {
			return userAccessLevel.regionMatches(true, userAccessLevelOffset, fullPermissionName, fullPermissionNameOffset, userAccessLevelLength - userAccessLevelOffset - 1) ? result : PermissionCheckResult.Type.NOTFOUND;
		} else {
			return userAccessLevel.regionMatches(true, userAccessLevelOffset, fullPermissionName, fullPermissionNameOffset, Math.max(userAccessLevelLength - userAccessLevelOffset, fullPermissionName.length() - fullPermissionNameOffset)) ? result : PermissionCheckResult.Type.NOTFOUND;
		}
	}

	/**
	 * Returns a list of all groups.
	 * 
	 * Including subgroups.
	 * 
	 * @param userName
	 * @return String[] of all group names.
	 */
	@Override
	public String[] getGroups(String userName) {

		ArrayList<String> allGroups = listAllGroupsInherited(ph.getUser(userName).getGroup());
		for (Group subg : ph.getUser(userName).subGroupListCopy()) {
			allGroups.addAll(listAllGroupsInherited(subg));
		}

		String[] arr = new String[allGroups.size()];
		return allGroups.toArray(arr);
	}

	/**
	 * Returns a list of all subgroups.
	 * 
	 * @param userName
	 * @return String[] of all group names.
	 */
	public String[] getSubGroups(String userName) {

		Set<String> allGroups = new HashSet<>();
		for (Group subg : ph.getUser(userName).subGroupListCopy()) {
			allGroups.addAll(listAllGroupsInherited(subg));
		}

		String[] arr = new String[allGroups.size()];
		return allGroups.toArray(arr);
	}

	/**
	 * A Breadth-first search thru inheritance model.
	 * 
	 * Just a model to copy and paste. This will guarantee the closer groups
	 * will be checked first.
	 * 
	 * @param start
	 * @param targerPermission
	 * @return
	 */
	@SuppressWarnings("unused")
	private Group breadthFirstSearch(Group start, String targerPermission) {

		if (start == null || targerPermission == null) {
			return null;
		}
		LinkedList<Group> stack = new LinkedList<>();
		ArrayList<Group> alreadyVisited = new ArrayList<>();
		stack.push(start);
		alreadyVisited.add(start);
		while (!stack.isEmpty()) {
			Group now = stack.pop();
			PermissionCheckResult resultNow = checkGroupOnlyPermission(now, targerPermission);
			if (resultNow.resultType.equals(PermissionCheckResult.Type.EXCEPTION) || resultNow.resultType.equals(PermissionCheckResult.Type.FOUND)) {
				return now;
			}
			if (resultNow.resultType.equals(PermissionCheckResult.Type.NEGATION)) {
				return null;
			}
			for (String sonName : now.getInherits()) {
				Group son = ph.getGroup(sonName);
				if (son != null && !alreadyVisited.contains(son)) {
					stack.push(son);
					alreadyVisited.add(son);
				}
			}
		}
		return null;
	}

	@Override
	public Group getDefaultGroup() {

		return ph.getDefaultGroup();
	}

	@Override
	public String getInfoString(String entryName, String path, boolean isGroup) {

		if (isGroup) {
			Group data = ph.getGroup(entryName);
			if (data == null) {
				return null;
			}
			return data.getVariables().getVarString(path);
		} else {
			User data = ph.getUser(entryName);
			if (data == null) {
				return null;
			}
			return data.getVariables().getVarString(path);
		}
	}

	@Override
	public int getInfoInteger(String entryName, String path, boolean isGroup) {

		if (isGroup) {
			Group data = ph.getGroup(entryName);
			if (data == null) {
				return -1;
			}
			return data.getVariables().getVarInteger(path);
		} else {
			User data = ph.getUser(entryName);
			if (data == null) {
				return -1;
			}
			return data.getVariables().getVarInteger(path);
		}
	}

	@Override
	public double getInfoDouble(String entryName, String path, boolean isGroup) {

		if (isGroup) {
			Group data = ph.getGroup(entryName);
			if (data == null) {
				return -1;
			}
			return data.getVariables().getVarDouble(path);
		} else {
			User data = ph.getUser(entryName);
			if (data == null) {
				return -1;
			}
			return data.getVariables().getVarDouble(path);
		}

	}

	@Override
	public boolean getInfoBoolean(String entryName, String path, boolean isGroup) {

		if (isGroup) {
			Group data = ph.getGroup(entryName);
			if (data == null) {
				return false;
			}
			return data.getVariables().getVarBoolean(path);
		} else {
			User data = ph.getUser(entryName);
			if (data == null) {
				return false;
			}
			return data.getVariables().getVarBoolean(path);
		}
	}

	@Override
	public void addUserInfo(String name, String path, Object data) {

		ph.getUser(name).getVariables().addVar(path, data);
	}

	@Override
	public void removeUserInfo(String name, String path) {

		ph.getUser(name).getVariables().removeVar(path);
	}

	@Override
	public void addGroupInfo(String name, String path, Object data) {

		ph.getGroup(name).getVariables().addVar(path, data);
	}

	@Override
	public void removeGroupInfo(String name, String path) {

		ph.getGroup(name).getVariables().removeVar(path);
	}
}
