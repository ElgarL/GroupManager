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
package org.anjocaido.groupmanager.dataholder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.data.Group;
import org.anjocaido.groupmanager.data.User;
import org.anjocaido.groupmanager.events.GMGroupEvent;
import org.anjocaido.groupmanager.events.GMSystemEvent;
import org.anjocaido.groupmanager.events.GMUserEvent;
import org.anjocaido.groupmanager.events.GMUserEvent.Action;
import org.anjocaido.groupmanager.localization.Messages;
import org.anjocaido.groupmanager.permissions.AnjoPermissionsHandler;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.reader.UnicodeReader;

/**
 * One instance of this should exist per world/mirror it contains all functions
 * to manage these data sets and points to the relevant users and groups
 * objects.
 *
 * @author gabrielcouto, ElgarL
 */
public class WorldDataHolder {

    /**
     * World name
     */
    protected String name;
    /**
     * The actual groups holder
     */
    protected GroupsDataHolder groups = new GroupsDataHolder();
    /**
     * The actual users holder
     */
    protected UsersDataHolder users = new UsersDataHolder();

    /**
     * List of UUID's associated with this user name.
     */
    protected static Map<String, Set<String>> nameToUUIDLookup = new TreeMap<String, Set<String>>();
    /**
     *
     */
    protected AnjoPermissionsHandler permissionsHandler;

    /**
     * Prevent direct instantiation
     *
     * @param worldName
     */
    public WorldDataHolder(String worldName) {

        name = worldName;
    }

    /**
     * The main constructor for a new WorldDataHolder
     *
     * @param worldName
     * @param groups
     * @param users
     */
    public WorldDataHolder(String worldName, GroupsDataHolder groups, UsersDataHolder users) {

        this.name = worldName;
        this.groups = groups;
        this.users = users;

        // this.defaultGroup = defaultGroup;
    }

    /**
     * update the dataSource to point to this object.
     * <p>
     * This should be called whenever a set of world data is fetched.
     */
    public void updateDataSource() {

        this.groups.setDataSource(this);
        this.users.setDataSource(this);
    }

    /**
     * Search for a user. If it doesn't exist, create a new one with default group.
     * <p>
     * If this is called passing a player name with mantogglevalidate off
     * it can return the wrong user object (offline/online UUID match).
     *
     * @param userId the UUID String or name of the user
     * @return class that manage that user permission
     */
    public User getUser(String userId) {

        if (getUsers().containsKey(userId.toLowerCase())) {
            return getUsers().get(userId.toLowerCase());
        }

        // Legacy name matching
        if ((userId.length() < 36) && nameToUUIDLookup.containsKey(userId.toLowerCase())) {

            // Search for a name to UUID match
            for (String uid : getUUIDLookup(userId.toLowerCase())) {

                User user = getUsers().get(uid.toLowerCase());

                if ((user != null) && user.getLastName().equalsIgnoreCase(userId)) {
                    return user;
                }
            }

        }

        if (!nameToUUIDLookup.containsKey(userId.toLowerCase())) {
            GroupManager.logger.fine("ERROR: No lookup for: " + userId);
        }

        // No user account found so create a new one.
        User newUser = createUser(userId);
        return newUser;
    }

    /**
     * *** Internal GM use only ***
     * This is called when a player joins to update/add their UUID.
     *
     * @param uUID        the player objects UUID.
     * @param currentName the name they have just logged in with.
     * @return the user object for this player.
     */
    public User getUser(String uUID, String currentName) {

        // Check for a UUID account
        User user = getUsers().get(uUID.toLowerCase());

        if (user != null) {

            GroupManager.logger.fine("User record found for UUID: " + uUID + ":" + currentName);
            user.setLastName(currentName);
            /*
             * Check for a non UUID name match as
             * its possible some plugin (worldedit)
             * performed a command permission lookup
             * by name, before we got to see a login
             * event (to grab a UUID). This would
             * force create a user .
             */
            if (getUsers().containsKey(currentName.toLowerCase())) getUsers().remove(currentName.toLowerCase());

            return user;

        }

        // Search for a LastName match
        user = getUsers().get(currentName.toLowerCase());

        if ((user != null) && user.getLastName().equalsIgnoreCase(currentName) && user.getUUID().equalsIgnoreCase(user.getLastName())) {

            // Clone this user so we can set it's uUID
            User usr = user.clone(uUID, currentName);

            // Delete it and replace with the new clone.
            this.removeUser(user.getUUID().toLowerCase());
            this.addUser(usr);

            GroupManager.logger.fine("Updating User record for UUID: " + uUID + ":" + currentName);

            return getUsers().get(uUID.toLowerCase());
        }

        if (user != null) {
            GroupManager.logger.fine("User record found but UUID mismatch for: " + currentName);
        }

        // No user account found so create a new one.
        User newUser = createUser(uUID.toLowerCase());
        newUser.setLastName(currentName);

        GroupManager.logger.fine("New User record created: " + uUID + ":" + currentName);

        return newUser;
    }

    /**
     * Add a user to the list. If it already exists, overwrite the old.
     *
     * @param theUser the user you want to add to the permission list
     */
    public void addUser(User theUser) {

        if (theUser.getDataSource() != this) {
            theUser = theUser.clone(this);
        }
        if (theUser == null) {
            return;
        }
        if ((theUser.getGroup() == null)) {
            theUser.setGroup(groups.getDefaultGroup());
        }
        removeUser(theUser.getUUID().toLowerCase());
        getUsers().put(theUser.getUUID().toLowerCase(), theUser);

        // Store for name to UUID lookups.
        //putUUIDLookup(theUser.getLastName(), theUser.getUUID().toLowerCase());

        setUsersChanged(true);
        if (GroupManager.isLoaded())
            GroupManager.getGMEventHandler().callEvent(theUser, Action.USER_ADDED);
    }

    /**
     * Removes the user from the list. (he might become a default user)
     *
     * @param userId the UUID or username for the user to remove
     * @return true if it had something to remove
     */
    public boolean removeUser(String userId) {

        if (getUsers().containsKey(userId.toLowerCase())) {

            User user = getUser(userId.toLowerCase());

            // Remove the name to UUID lookup for this user object.
            removeUUIDLookup(user.getLastName().toLowerCase(), user.getUUID());

            getUsers().remove(userId.toLowerCase());

            setUsersChanged(true);

            if (GroupManager.isLoaded())
                GroupManager.getGMEventHandler().callEvent(userId, GMUserEvent.Action.USER_REMOVED);

            return true;
        }
        return false;
    }

    /**
     * @param userId
     * @return true if we have data for this player.
     */
    public boolean isUserDeclared(String userId) {

        return getUsers().containsKey(userId.toLowerCase());
    }

    /**
     * Change the default group of the file.
     *
     * @param group the group you want make default.
     */
    public void setDefaultGroup(Group group) {

        if (!getGroups().containsKey(group.getName().toLowerCase()) || (group.getDataSource() != this)) {
            addGroup(group);
        }
        groups.setDefaultGroup(getGroup(group.getName()));
        setGroupsChanged(true);
        if (GroupManager.isLoaded())
            GroupManager.getGMEventHandler().callEvent(GMSystemEvent.Action.DEFAULT_GROUP_CHANGED);
    }

    /**
     * Returns the default group of the file
     *
     * @return the default group
     */
    public Group getDefaultGroup() {

        return groups.getDefaultGroup();
    }

    /**
     * Returns a group of the given name
     *
     * @param groupName the name of the group
     * @return a group if it is found. null if not found.
     */
    public Group getGroup(String groupName) {

        if (groupName.toLowerCase().startsWith("g:"))
            return GroupManager.getGlobalGroups().getGroup(groupName);
        else
            return getGroups().get(groupName.toLowerCase());
    }

    /**
     * Check if a group exists. Its the same of getGroup, but check if it is
     * null.
     *
     * @param groupName the name of the group
     * @return true if exists. false if not.
     */
    public boolean groupExists(String groupName) {

        if (groupName.toLowerCase().startsWith("g:"))
            return GroupManager.getGlobalGroups().hasGroup(groupName);
        else
            return getGroups().containsKey(groupName.toLowerCase());
    }

    /**
     * Add a group to the list
     *
     * @param groupToAdd
     */
    public void addGroup(Group groupToAdd) {

        if (groupToAdd.getName().toLowerCase().startsWith("g:")) {
            GroupManager.getGlobalGroups().addGroup(groupToAdd);
            GroupManager.getGMEventHandler().callEvent(groupToAdd, GMGroupEvent.Action.GROUP_ADDED);
            return;
        }

        if (groupToAdd.getDataSource() != this) {
            groupToAdd = groupToAdd.clone(this);
        }
        removeGroup(groupToAdd.getName());
        getGroups().put(groupToAdd.getName().toLowerCase(), groupToAdd);
        setGroupsChanged(true);
        if (GroupManager.isLoaded())
            GroupManager.getGMEventHandler().callEvent(groupToAdd, GMGroupEvent.Action.GROUP_ADDED);
    }

    /**
     * Remove the group from the list
     *
     * @param groupName
     * @return true if something was removed. false the group was default or
     * non-existent
     */
    public boolean removeGroup(String groupName) {

        if (groupName.toLowerCase().startsWith("g:")) {
            return GroupManager.getGlobalGroups().removeGroup(groupName);
        }

        if (getDefaultGroup() != null && groupName.equalsIgnoreCase(getDefaultGroup().getName())) {
            return false;
        }
        if (getGroups().containsKey(groupName.toLowerCase())) {
            getGroups().remove(groupName.toLowerCase());
            setGroupsChanged(true);
            if (GroupManager.isLoaded())
                GroupManager.getGMEventHandler().callEvent(groupName.toLowerCase(), GMGroupEvent.Action.GROUP_REMOVED);
            return true;
        }
        return false;

    }

    /**
     * Creates a new User with the given name and adds it to this holder.
     *
     * @param userId the UUID or username you want
     * @return null if user already exists. or new User
     */
    public User createUser(String userId) {

        if (getUsers().containsKey(userId.toLowerCase())) {
            return null;
        }
        User newUser = new User(this, userId);
        newUser.setGroup(groups.getDefaultGroup(), false);
        addUser(newUser);
        setUsersChanged(true);
        return newUser;
    }

    /**
     * Creates a new Group with the given name and adds it to this holder
     *
     * @param groupName the groupname you want
     * @return null if group already exists. or new Group
     */
    public Group createGroup(String groupName) {

        if (groupName.toLowerCase().startsWith("g:")) {
            Group newGroup = new Group(groupName);
            return GroupManager.getGlobalGroups().newGroup(newGroup);
        }

        if (getGroups().containsKey(groupName.toLowerCase())) {
            return null;
        }

        Group newGroup = new Group(this, groupName);
        addGroup(newGroup);
        setGroupsChanged(true);
        return newGroup;
    }

    /**
     * @return a collection of the groups
     */
    public Collection<Group> getGroupList() {

        synchronized (getGroups()) {
            return new ArrayList<Group>(getGroups().values());
        }
    }

    /**
     * @return a collection of the users
     */
    public Collection<User> getUserList() {

        synchronized (getUsers()) {
            return new ArrayList<User>(getUsers().values());
        }
    }

    /**
     * reads the file again
     */
    public void reload() {

        try {
            reloadGroups();
            reloadUsers();
        } catch (Exception ex) {
            Logger.getLogger(WorldDataHolder.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Refresh Group data from file
     */
    public void reloadGroups() {

        GroupManager.setLoaded(false);
        try {
            // temporary holder in case the load fails.
            WorldDataHolder ph = new WorldDataHolder(this.getName());

            loadGroups(ph, getGroupsFile());
            // transfer new data
            resetGroups();
            for (Group tempGroup : ph.getGroupList()) {
                tempGroup.clone(this);
            }
            this.setDefaultGroup(getGroup(ph.getDefaultGroup().getName()));
            this.removeGroupsChangedFlag();
            this.setTimeStampGroups(getGroupsFile().lastModified());

            ph = null;
        } catch (Exception ex) {
            Logger.getLogger(WorldDataHolder.class.getName()).log(Level.WARNING, null, ex);
        }
        GroupManager.setLoaded(true);
        GroupManager.getGMEventHandler().callEvent(GMSystemEvent.Action.RELOADED);
    }

    /**
     * Refresh Users data from file
     */
    public void reloadUsers() {

        GroupManager.setLoaded(false);
        try {
            // temporary holder in case the load fails.
            WorldDataHolder ph = new WorldDataHolder(this.getName());
            // copy groups for reference
            for (Group tempGroup : this.getGroupList()) {
                tempGroup.clone(ph);
            }
            // setup the default group before loading user data.
            ph.setDefaultGroup(ph.getGroup(getDefaultGroup().getName()));
            loadUsers(ph, getUsersFile());
            // transfer new data
            resetUsers();
            for (User tempUser : ph.getUserList()) {
                tempUser.clone(this);
            }
            this.removeUsersChangedFlag();
            this.setTimeStampUsers(getUsersFile().lastModified());

            ph = null;
        } catch (Exception ex) {
            Logger.getLogger(WorldDataHolder.class.getName()).log(Level.WARNING, null, ex);
        }
        GroupManager.setLoaded(true);
        GroupManager.getGMEventHandler().callEvent(GMSystemEvent.Action.RELOADED);
    }

    public void loadGroups(File groupsFile) {

        GroupManager.setLoaded(false);
        try {
            setGroupsFile(groupsFile);
            loadGroups(this, groupsFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new IllegalArgumentException(Messages.getString("WorldDatHolder.ERROR_NO_GROUPS_FILE") + System.lineSeparator() + groupsFile.getPath());
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalArgumentException(Messages.getString("WorldDatHolder.ERROR_ACCESSING_GROUPS_FILE") + System.lineSeparator() + groupsFile.getPath());
        }

        GroupManager.setLoaded(true);
    }

    public void loadUsers(File usersFile) {

        GroupManager.setLoaded(false);
        try {
            setUsersFile(usersFile);
            loadUsers(this, usersFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new IllegalArgumentException(Messages.getString("WorldDatHolder.ERROR_NO_USERS_FILE") + System.lineSeparator() + usersFile.getPath());
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalArgumentException(Messages.getString("WorldDatHolder.ERROR_ACCESSING_USERS_FILE") + System.lineSeparator() + usersFile.getPath());
        }

        GroupManager.setLoaded(true);
    }

    /**
     * Returns a NEW data holder containing data read from the files
     *
     * @param worldName
     * @param groupsFile
     * @param usersFile
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static WorldDataHolder load(String worldName, File groupsFile, File usersFile) throws FileNotFoundException, IOException {

        WorldDataHolder ph = new WorldDataHolder(worldName);

        GroupManager.setLoaded(false);
        if (groupsFile != null)
            loadGroups(ph, groupsFile);
        if (usersFile != null)
            loadUsers(ph, usersFile);
        GroupManager.setLoaded(true);

        return ph;
    }

    /**
     * Updates the WorldDataHolder from the Groups file
     *
     * @param ph
     * @param groupsFile
     * @throws FileNotFoundException
     * @throws IOException
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected static void loadGroups(WorldDataHolder ph, File groupsFile) throws FileNotFoundException, IOException {

        // READ GROUPS FILE

        Yaml yamlGroups = new Yaml(new SafeConstructor());
        Map<String, Object> groupsRootDataNode;

        if (!groupsFile.exists()) {
            throw new IllegalArgumentException(Messages.getString("WorldDatHolder.ERROR_NO_GROUPS_FILE") + System.lineSeparator() + groupsFile.getPath());
        }
        FileInputStream groupsInputStream = new FileInputStream(groupsFile);
        try {
            groupsRootDataNode = (Map<String, Object>) yamlGroups.load(new UnicodeReader(groupsInputStream));
            if (groupsRootDataNode == null) {
                throw new NullPointerException();
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_INVALID_FILE"), groupsFile.getPath(), ex));
        } finally {
            groupsInputStream.close();
        }

        // PROCESS GROUPS FILE

        Map<String, List<String>> inheritance = new HashMap<String, List<String>>();
        Map<String, Object> allGroupsNode = null;

        /*
         * Fetch all groups under the 'groups' entry.
         */
        try {
            allGroupsNode = (Map<String, Object>) groupsRootDataNode.get("groups");
        } catch (Exception ex) {
            throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_INVALID_FILE"), groupsFile.getPath()), ex);
        }

        if (allGroupsNode == null) {
            throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_NO_GROUPS"), groupsFile.getPath()));
        }

        Iterator<String> groupItr = allGroupsNode.keySet().iterator();
        String groupKey;
        Integer groupCount = 0;

        /*
         * loop each group entry and process it's data.
         */
        while (groupItr.hasNext()) {

            try {
                groupCount++;
                // Attempt to fetch the next group name.
                groupKey = groupItr.next();
            } catch (Exception ex) {
                throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_INVALID_GROUP_NAME"), groupCount, groupsFile.getPath()), ex);
            }

            /*
             * Fetch this groups child nodes
             */
            Map<String, Object> thisGroupNode = null;

            try {
                thisGroupNode = (Map<String, Object>) allGroupsNode.get(groupKey);
            } catch (Exception ex) {
                throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_INVALID_CHILD_NODE"), groupKey, groupsFile.getPath()), ex);
            }

            /*
             * Create a new group with this name in the assigned data source.
             */
            Group thisGrp = ph.createGroup(groupKey);

            if (thisGrp == null) {
                throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_GROUP_DUPLICATE"), groupKey, groupsFile.getPath()));
            }

            // DEFAULT NODE

            Object nodeData = null;
            try {
                nodeData = thisGroupNode.get("default");
            } catch (Exception ex) {
                throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_INVALID_FORMAT"), "default", groupKey, groupsFile.getPath()));
            }

            if (nodeData == null) {
                /*
                 * If no 'default' node is found do nothing.
                 */
            } else if ((Boolean.parseBoolean(nodeData.toString()))) {
                /*
                 * Set this as the default group. Warn if some other group has
                 * already claimed that position.
                 */
                if (ph.getDefaultGroup() != null) {
                    GroupManager.logger.warning(String.format(Messages.getString("WorldDatHolder.ERROR_DEFAULT_DUPLICATE"), thisGrp.getName(), ph.getDefaultGroup().getName()));
                    GroupManager.logger.warning(String.format(Messages.getString("WorldDatHolder.WARN_OVERIDE_DEFAULT"), groupsFile.getPath()));
                }
                ph.setDefaultGroup(thisGrp);
            }

            // PERMISSIONS NODE

            nodeData = null;
            try {
                nodeData = thisGroupNode.get("permissions");
            } catch (Exception ex) {
                throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_INVALID_FORMAT"), "permissions", groupKey, groupsFile.getPath()));
            }

            /*
             * If no permissions node is found, or it's empty do nothing.
             */
            if (nodeData != null) {
                /*
                 * There is a permission list Which seems to hold some data
                 */
                if (nodeData instanceof List) {
                    /*
                     * Check each entry and add it as a new permission.
                     */
                    try {
                        for (Object o : ((List) nodeData)) {
                            try {
                                /*
                                 * Only add this permission if it's not empty.
                                 */
                                if (!o.toString().isEmpty())
                                    /*
                                     * check for a timed permission
                                     */
                                    if (o.toString().contains("|")) {
                                        String[] split = o.toString().split("\\|");
                                        try {

                                            thisGrp.addTimedPermission(split[0], Long.parseLong(split[1]));
                                        } catch (Exception e) {
                                            GroupManager.logger.warning("Timed Permission error: " + o.toString());
                                        }
                                    } else {
                                        /*
                                         * add a standard permission
                                         */
                                        thisGrp.addPermission(o.toString());
                                    }

                            } catch (NullPointerException ex) {
                                // Ignore this entry as it's null. It can be
                                // safely dropped
                            }
                        }
                    } catch (Exception ex) {
                        throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_INVALID_FORMAT"), "permissions", thisGrp.getName(), groupsFile.getPath()), ex);
                    }

                } else if (nodeData instanceof String) {
                    /*
                     * Only add this permission if it's not empty.
                     */
                    if (!nodeData.toString().isEmpty()) {
                        /*
                         * check for a timed permission
                         */
                        if (nodeData.toString().contains("|")) {
                            String[] split = nodeData.toString().split("\\|");
                            try {

                                thisGrp.addTimedPermission(split[0], Long.parseLong(split[1]));
                            } catch (Exception e) {
                                GroupManager.logger.warning("TimedPermission error: " + nodeData.toString());
                            }
                        } else {
                            /*
                             * add a standard permission
                             */
                            thisGrp.addPermission((String) nodeData);
                        }
                    }

                } else {
                    throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_UNKNOWN_TYPE"), "permissions", thisGrp.getName(), groupsFile.getPath()));
                }
                /*
                 * Sort all permissions so they are in the correct order for
                 * checking.
                 */
                thisGrp.sortPermissions();
            }

            // INFO NODE

            nodeData = null;
            try {
                nodeData = thisGroupNode.get("info");
            } catch (Exception ex) {
                throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_INVALID_FORMAT"), "info", groupKey, groupsFile.getPath()));
            }

            if (nodeData == null) {
                /*
                 * No info section was found, so leave all variables as
                 * defaults.
                 */
                GroupManager.logger.warning(String.format(Messages.getString("WorldDatHolder.WARN_GROUP_NO_INFO"), thisGrp.getName()));
                GroupManager.logger.warning(Messages.getString("WorldDatHolder.WARN_USING_DEFAULT") + groupsFile.getPath());

            } else if (nodeData instanceof Map) {
                try {
                    if (nodeData != null) {
                        thisGrp.setVariables((Map<String, Object>) nodeData);
                    }
                } catch (Exception ex) {
                    throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_INVALID_FORMAT"), "info", thisGrp.getName(), groupsFile.getPath()), ex);
                }

            } else
                throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_UNKNOWN_ENTRY"), "info", thisGrp.getName(), groupsFile.getPath()));

            // INHERITANCE NODE

            nodeData = null;
            try {
                nodeData = thisGroupNode.get("inheritance");
            } catch (Exception ex) {
                throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_INVALID_FORMAT"), "inheritance", groupKey, groupsFile.getPath()));
            }

            if (nodeData == null || nodeData instanceof List) {
                if (nodeData == null) {
                    /*
                     * If no inheritance node is found, or it's empty do
                     * nothing.
                     */
                } else if (nodeData instanceof List) {

                    try {
                        for (String grp : (List<String>) nodeData) {
                            if (inheritance.get(groupKey) == null) {
                                inheritance.put(groupKey, new ArrayList<String>());
                            }
                            inheritance.get(groupKey).add(grp);
                        }

                    } catch (Exception ex) {
                        throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_INVALID_FORMAT"), "inheritance", thisGrp.getName(), groupsFile.getPath()), ex);
                    }

                }
            } else
                throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_UNKNOWN_ENTRY"), "inheritance", thisGrp.getName(), groupsFile.getPath()));

            // END GROUP

        }

        if (ph.getDefaultGroup() == null) {
            throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_NO_DEFAULT"), groupsFile.getPath()));
        }

        /*
         * Build the inheritance map and recored any errors
         */
        for (String group : inheritance.keySet()) {
            List<String> inheritedList = inheritance.get(group);
            Group thisGroup = ph.getGroup(group);
            if (thisGroup != null)
                for (String inheritedKey : inheritedList) {
                    if (inheritedKey != null) {
                        Group inheritedGroup = ph.getGroup(inheritedKey);
                        if (inheritedGroup != null) {
                            thisGroup.addInherits(inheritedGroup);
                        } else
                            GroupManager.logger.warning(String.format(Messages.getString("WorldDatHolder.WARN_INHERITED_NOT_FOUND"), inheritedKey, thisGroup.getName(), groupsFile.getPath()));
                    }
                }
        }

        ph.removeGroupsChangedFlag();
        // Update the LastModified time.
        ph.setGroupsFile(groupsFile);
        ph.setTimeStampGroups(groupsFile.lastModified());

        // return ph;
    }

    /**
     * Updates the WorldDataHolder from the Users file
     *
     * @param ph
     * @param usersFile
     * @throws FileNotFoundException
     * @throws IOException
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected static void loadUsers(WorldDataHolder ph, File usersFile) throws FileNotFoundException, IOException {

        // READ USERS FILE
        Yaml yamlUsers = new Yaml(new SafeConstructor());
        Map<String, Object> usersRootDataNode;
        if (!usersFile.exists()) {
            throw new IllegalArgumentException(Messages.getString("WorldDatHolder.ERROR_NO_USERS_FILE") + System.lineSeparator() + usersFile.getPath());
        }
        FileInputStream usersInputStream = new FileInputStream(usersFile);
        try {
            usersRootDataNode = (Map<String, Object>) yamlUsers.load(new UnicodeReader(usersInputStream));
            if (usersRootDataNode == null) {
                throw new NullPointerException();
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_INVALID_FILE"), usersFile.getPath()), ex);
        } finally {
            usersInputStream.close();
        }

        // PROCESS USERS FILE

        Map<String, Object> allUsersNode = null;

        /*
         * Fetch all child nodes under the 'users' entry.
         */
        try {
            allUsersNode = (Map<String, Object>) usersRootDataNode.get("users");
        } catch (Exception ex) {
            throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_INVALID_FILE"), usersFile.getPath()), ex);
        }

        // Load users if the file is NOT empty

        if (allUsersNode != null) {

            Iterator<String> usersItr = allUsersNode.keySet().iterator();
            String usersKey;
            Object node;
            Integer userCount = 0;

            while (usersItr.hasNext()) {
                try {
                    userCount++;
                    // Attempt to fetch the next user name.
                    node = usersItr.next();
                    if (node instanceof Integer)
                        usersKey = Integer.toString((Integer) node);
                    else
                        usersKey = node.toString();

                } catch (Exception ex) {
                    throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_INVALID_NODE_USER"), userCount, usersFile.getPath()), ex);
                }

                Map<String, Object> thisUserNode = null;
                try {
                    thisUserNode = (Map<String, Object>) allUsersNode.get(node);
                } catch (Exception ex) {
                    throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_INVALID_FORMAT_FOR_USER"), usersKey, usersFile.getPath()));
                }

                User thisUser = ph.createUser(usersKey);
                if (thisUser == null) {
                    throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_DUPLICATE_USER"), usersKey, usersFile.getPath()));
                }

                // LASTNAME NODES

                Object nodeData = null;
                try {

                    nodeData = thisUserNode.get("lastname");

                } catch (Exception ex) {
                    throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_INVALID_FORMAT_IN_USER"), "lastname", usersKey, usersFile.getPath()));
                }

                if ((nodeData != null) && (nodeData instanceof String)) {

                    thisUser.setLastName((String) nodeData);

                }

                // USER PERMISSIONS NODES

                nodeData = null;
                try {
                    nodeData = thisUserNode.get("permissions");
                } catch (Exception ex) {
                    throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_INVALID_FORMAT_FOR_USER"), "permissions", usersKey, usersFile.getPath()));
                }

                if (nodeData == null) {
                    /*
                     * If no permissions node is found, or it's empty do
                     * nothing.
                     */
                } else {
                    try {
                        if (nodeData instanceof List) {
                            for (Object o : ((List) nodeData)) {
                                /*
                                 * Only add this permission if it's not empty
                                 */
                                if (!o.toString().isEmpty()) {
                                    /*
                                     * check for a timed permission
                                     */
                                    if (o.toString().contains("|")) {
                                        String[] split = o.toString().split("\\|");
                                        try {
                                            thisUser.addTimedPermission(split[0], Long.parseLong(split[1]));
                                        } catch (Exception e) {
                                            GroupManager.logger.warning("TimedPermission error: " + o.toString());
                                        }
                                    } else {
                                        thisUser.addPermission(o.toString());
                                    }
                                }
                            }
                        } else if (nodeData instanceof String) {

                            /*
                             * Only add this permission if it's not empty
                             */
                            if (!nodeData.toString().isEmpty()) {
                                /*
                                 * check for a timed permission
                                 */
                                if (nodeData.toString().contains("|")) {
                                    String[] split = nodeData.toString().split("\\|");
                                    try {
                                        thisUser.addTimedPermission(split[0], Long.parseLong(split[1]));
                                    } catch (Exception e) {
                                        GroupManager.logger.warning("TimedPermission error: " + nodeData.toString());
                                    }
                                } else {
                                    thisUser.addPermission(nodeData.toString());
                                }
                            }

                        }
                    } catch (NullPointerException e) {
                        // Ignore this entry as it's null.
                    }
                    thisUser.sortPermissions();
                }

                // USER INFO NODE

                nodeData = null;
                try {
                    nodeData = thisUserNode.get("info");
                } catch (Exception ex) {
                    throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_INVALID_FORMAT_IN_USER"), "info", usersKey, usersFile.getPath()));
                }

                if (nodeData == null) {
                    /*
                     * If no info node is found, or it's empty do nothing.
                     */
                } else if (nodeData instanceof Map) {
                    thisUser.setVariables((Map<String, Object>) nodeData);

                } else
                    throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_UNKNOWN_ENTRY_USER"), "info", thisUser.getLastName(), usersFile.getPath()));

                // END INFO NODE

                // PRIMARY GROUP

                nodeData = null;
                try {
                    nodeData = thisUserNode.get("group");
                } catch (Exception ex) {
                    throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_INVALID_FORMAT_IN_USER"), "group", usersKey, usersFile.getPath()));
                }

                if (nodeData != null) {
                    Group hisGroup = ph.getGroup(nodeData.toString());
                    if (hisGroup == null) {
                        GroupManager.logger.warning(String.format(Messages.getString("WorldDatHolder.WARN_NO_GROUP_STATED"), thisUserNode.get("group").toString(), thisUser.getLastName(), ph.getDefaultGroup().getName(), usersFile.getPath()));
                        hisGroup = ph.getDefaultGroup();
                    }
                    thisUser.setGroup(hisGroup);
                } else {
                    thisUser.setGroup(ph.getDefaultGroup());
                }

                // SUBGROUPS NODES

                nodeData = null;
                try {
                    nodeData = thisUserNode.get("subgroups");
                } catch (Exception ex) {
                    throw new IllegalArgumentException(String.format(Messages.getString("WorldDatHolder.ERROR_INVALID_FORMAT_IN_USER"), "subgroups", usersKey, usersFile.getPath()));
                }

                if (nodeData == null) {
                    /*
                     * If no subgroups node is found, or it's empty do nothing.
                     */
                    if (nodeData instanceof List) {
                        for (Object o : ((List) nodeData)) {
                            /*
                             * Only add this permission if it's not empty
                             */
                            if (!o.toString().isEmpty()) {
                                /*
                                 * check for a timed permission
                                 */
                                if (o.toString().contains("|")) {
                                    String[] split = o.toString().split("\\|");
                                    try {
                                        Group subGrp = ph.getGroup(split[0]);
                                        thisUser.addTimedSubGroup(subGrp, Long.parseLong(split[1]));
                                    } catch (Exception e) {
                                        GroupManager.logger.warning("TimedSubGroup error: " + o.toString());
                                    }
                                } else {
                                    Group subGrp = ph.getGroup(nodeData.toString());
                                    thisUser.addSubGroup(subGrp);
                                }
                            }
                        }
                    } else if (nodeData instanceof String) {

                        /*
                         * Only add this subgroup if it's not empty
                         */
                        if (!nodeData.toString().isEmpty()) {
                            /*
                             * check for a timed subgroup
                             */
                            if (nodeData.toString().contains("|")) {
                                String[] split = nodeData.toString().split("\\|");
                                Group subGrp = ph.getGroup(split[0]);
                                    try {
                                        thisUser.addTimedSubGroup(subGrp, Long.parseLong(split[1]));
                                    } catch (Exception e) {
                                        GroupManager.logger.warning("TimedSubGroup error: " + nodeData.toString());
                                    }
                                } else {
                                    Group subGrp = ph.getGroup(nodeData.toString());
                                    thisUser.addSubGroup(subGrp);
                                }
                            }
                        }
                    }
                }
            }
		ph.removeUsersChangedFlag();
		// Update the LastModified time.
		ph.setUsersFile(usersFile);
		ph.setTimeStampUsers(usersFile.lastModified());
	}

	/**
	 * Write a dataHolder in a specified file
	 *
	 * @param ph
	 * @param groupsFile
	 */
	public static void writeGroups(WorldDataHolder ph, File groupsFile) {

		Map<String, Object> root = new HashMap<String, Object>();

		Map<String, Object> groupsMap = new HashMap<String, Object>();

		root.put("groups", groupsMap);
		synchronized (ph.getGroups()) {
			for (String groupKey : ph.getGroups().keySet()) {
				Group group = ph.getGroups().get(groupKey);

				Map<String, Object> aGroupMap = new HashMap<String, Object>();
				groupsMap.put(group.getName(), aGroupMap);

				if (ph.getDefaultGroup() == null) {
					GroupManager.logger.severe(Messages.getString("WorldDatHolder.WARN_NO_DEFAULT_GROUP") + ph.getName());
				}
				aGroupMap.put("default", group.equals(ph.getDefaultGroup()));

				Map<String, Object> infoMap = new HashMap<String, Object>();
				aGroupMap.put("info", infoMap);

				for (String infoKey : group.getVariables().getVarKeyList()) {
					infoMap.put(infoKey, group.getVariables().getVarObject(infoKey));
				}

				aGroupMap.put("inheritance", group.getInherits());

				aGroupMap.put("permissions", group.getSavePermissionList());
			}
		}

		if (!root.isEmpty()) {
			DumperOptions opt = new DumperOptions();
			opt.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
			final Yaml yaml = new Yaml(opt);
			try {
				OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(groupsFile), "UTF-8");

				String newLine = System.getProperty("line.separator");

				out.write("# Group inheritance" + newLine);
				out.write("#" + newLine);
				out.write("# Any inherited groups prefixed with a g: are global groups" + newLine);
				out.write("# and are inherited from the GlobalGroups.yml." + newLine);
				out.write("#" + newLine);
				out.write("# Groups without the g: prefix are groups local to this world" + newLine);
				out.write("# and are defined in the this groups.yml file." + newLine);
				out.write("#" + newLine);
				out.write("# Local group inheritances define your promotion tree when using 'manpromote/mandemote'" + newLine);
				out.write(newLine);

				yaml.dump(root, out);
				out.close();
			} catch (UnsupportedEncodingException ex) {
			} catch (FileNotFoundException ex) {
			} catch (IOException e) {
			}
		}

		// Update the LastModified time.
		ph.setGroupsFile(groupsFile);
		ph.setTimeStampGroups(groupsFile.lastModified());
		ph.removeGroupsChangedFlag();

		if (GroupManager.isLoaded())
			GroupManager.getGMEventHandler().callEvent(GMSystemEvent.Action.SAVED);

		/*
		 * FileWriter tx = null; try { tx = new FileWriter(groupsFile, false);
		 * tx.write(yaml.dump(root)); tx.flush(); } catch (Exception e) { }
		 * finally { try { tx.close(); } catch (IOException ex) { } }
		 */
	}

	/**
	 * Write a dataHolder in a specified file
	 *
	 * @param ph
	 * @param usersFile
	 */
	public static void writeUsers(WorldDataHolder ph, File usersFile) {

		Map<String, Object> root = new HashMap<String, Object>();
		LinkedHashMap<String, Object> usersMap = new LinkedHashMap<String, Object>();

		root.put("users", usersMap);
		synchronized (ph.getUsers()) {

			// A sorted list of users.
			for (String userKey : new TreeSet<String>(ph.getUsers().keySet())) {
				User user = ph.getUsers().get(userKey);
				if ((user.getGroup() == null || user.getGroup().equals(ph.getDefaultGroup())) && user.getPermissionList().isEmpty() && user.getVariables().isEmpty() && user.isSubGroupsEmpty()) {
					continue;
				}

				LinkedHashMap<String, Object> aUserMap = new LinkedHashMap<String, Object>();
				usersMap.put(user.getUUID(), aUserMap);

				if (!user.getUUID().equalsIgnoreCase(user.getLastName())) {
					aUserMap.put("lastname", user.getLastName());
				}

				// GROUP NODE
				if (user.getGroup() == null) {
					aUserMap.put("group", ph.getDefaultGroup().getName());
				} else {
					aUserMap.put("group", user.getGroup().getName());
				}

				// SUBGROUPS NODE
				aUserMap.put("subgroups", user.getSaveSubGroupsList());

				// PERMISSIONS NODE
				aUserMap.put("permissions", user.getSavePermissionList());

				// USER INFO NODE - BETA
				if (user.getVariables().getSize() > 0) {
					Map<String, Object> infoMap = new HashMap<String, Object>();
					aUserMap.put("info", infoMap);
					for (String infoKey : user.getVariables().getVarKeyList()) {
						infoMap.put(infoKey, user.getVariables().getVarObject(infoKey));
					}
				}
				// END USER INFO NODE - BETA

			}
		}

		if (!root.isEmpty()) {
			DumperOptions opt = new DumperOptions();
			opt.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
			final Yaml yaml = new Yaml(opt);
			try {
				OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(usersFile), "UTF-8");
				yaml.dump(root, out);
				out.close();
			} catch (UnsupportedEncodingException ex) {
			} catch (FileNotFoundException ex) {
			} catch (IOException e) {
			}
		}

		// Update the LastModified time.
		ph.setUsersFile(usersFile);
		ph.setTimeStampUsers(usersFile.lastModified());
		ph.removeUsersChangedFlag();

		if (GroupManager.isLoaded())
			GroupManager.getGMEventHandler().callEvent(GMSystemEvent.Action.SAVED);

	}

	/**
	 * Don't use this. Unless you want to make this plugin to interact with
	 * original Nijikokun Permissions This method is supposed to make the
	 * original one reload the file, and propagate the changes made here.
	 *
	 * Prefer to use the AnjoCaido's fake version of Nijikokun's Permission
	 * plugin. The AnjoCaido's Permission can propagate the changes made on this
	 * plugin instantly, without need to save the file.
	 *
	 * @param server the server that holds the plugin
	 * @deprecated it is not used anymore... unless if you use original
	 *             Permissions
	 */
	@Deprecated
	public static void reloadOldPlugins(Server server) {

		// Only reload permissions
		PluginManager pm = server.getPluginManager();
		Plugin[] plugins = pm.getPlugins();
		for (int i = 0; i < plugins.length; i++) {
			// plugins[i].getConfiguration().load();
			try {
				plugins[i].getClass().getMethod("setupPermissions").invoke(plugins[i]);
			} catch (Exception ex) {
				continue;
			}
		}
	}

	/**
	 * @return the permissionsHandler
	 */
	public AnjoPermissionsHandler getPermissionsHandler() {

		if (permissionsHandler == null) {
			permissionsHandler = new AnjoPermissionsHandler(this);
		}
		return permissionsHandler;
	}

	/**
	 * @param haveUsersChanged the haveUsersChanged to set
	 */
	public void setUsersChanged(boolean haveUsersChanged) {

		users.setUsersChanged(haveUsersChanged);
	}

	/**
	 *
	 * @return true if any user data has changed
	 */
	public boolean haveUsersChanged() {

		if (users.HaveUsersChanged()) {
			return true;
		}
		synchronized (users) {
			for (User u : users.getUsers().values()) {
				if (u.isChanged()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * @param setGroupsChanged the haveGroupsChanged to set
	 */
	public void setGroupsChanged(boolean setGroupsChanged) {

		groups.setGroupsChanged(setGroupsChanged);
	}

	/**
	 *
	 * @return true if any group data has changed.
	 */
	public boolean haveGroupsChanged() {

		if (groups.HaveGroupsChanged()) {
			return true;
		}
		synchronized (groups) {
			for (Group g : groups.getGroups().values()) {
				if (g.isChanged()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 *
	 */
	public void removeUsersChangedFlag() {

		setUsersChanged(false);
		synchronized (users) {
			for (User u : getUsers().values()) {
				u.flagAsSaved();
			}
		}
	}

	/**
	 *
	 */
	public void removeGroupsChangedFlag() {

		setGroupsChanged(false);
		synchronized (groups) {
			for (Group g : getGroups().values()) {
				g.flagAsSaved();
			}
		}
	}

	/**
	 * Searches all groups and users for expired permissions
	 * Removes those perms and flags the object for saving.
	 */
	public boolean purgeTimedPermissions() {

		boolean expired = false;

		synchronized (groups) {

			for (Group group : getGroups().values()) {
				if (group.removeExpired()) {
					setGroupsChanged(true);
					expired = true;
				}
			}
		}

		synchronized (users) {

			for (User user : getUsers().values()) {
				if (user.removeExpired()) {
					setUsersChanged(true);
					expired = true;
				}
			}
		}

		return expired;
	}

	/**
	 * @return the usersFile
	 */
	public File getUsersFile() {

		return users.getUsersFile();
	}

	/**
	 * @param file the usersFile to set
	 */
	public void setUsersFile(File file) {

		users.setUsersFile(file);
	}

	/**
	 * @return the groupsFile
	 */
	public File getGroupsFile() {

		return groups.getGroupsFile();
	}

	/**
	 * @param file the groupsFile to set
	 */
	public void setGroupsFile(File file) {

		groups.setGroupsFile(file);
	}

	/**
	 * @return the name
	 */
	public String getName() {

		return name;
	}

	/**
	 * Resets Groups.
	 */
	public void resetGroups() {

		// setDefaultGroup(null);
		groups.resetGroups();
	}

	/**
	 * Resets Users
	 */
	public void resetUsers() {

		users.resetUsers();
		this.clearUUIDLookup();
	}

	/**
	 * Note: Iteration over this object has to be synchronized!
	 *
	 * @return the groups
	 */
	public Map<String, Group> getGroups() {

		return groups.getGroups();
	}

	/**
	 * Note: Iteration over this object has to be synchronized!
	 *
	 * @return the users
	 */
	public Map<String, User> getUsers() {

		return users.getUsers();
	}

	/**
	 * @return the groups
	 */
	public GroupsDataHolder getGroupsObject() {

		return groups;
	}

	/**
	 * @param groupsDataHolder the GroupsDataHolder to set
	 */
	public void setGroupsObject(GroupsDataHolder groupsDataHolder) {

		groups = groupsDataHolder;
	}

	/**
	 * @return the users
	 */
	public UsersDataHolder getUsersObject() {

		return users;
	}

	/**
	 * @param usersDataHolder the UsersDataHolder to set
	 */
	public void setUsersObject(UsersDataHolder usersDataHolder) {

		users = usersDataHolder;
	}

	/**
	 * @return the timeStampGroups
	 */
	public long getTimeStampGroups() {

		return groups.getTimeStampGroups();
	}

	/**
	 * @return the timeStampUsers
	 */
	public long getTimeStampUsers() {

		return users.getTimeStampUsers();
	}

	/**
	 * @param timeStampGroups the timeStampGroups to set
	 */
	protected void setTimeStampGroups(long timeStampGroups) {

		groups.setTimeStampGroups(timeStampGroups);
	}

	/**
	 * @param timeStampUsers the timeStampUsers to set
	 */
	protected void setTimeStampUsers(long timeStampUsers) {

		users.setTimeStampUsers(timeStampUsers);
	}

	public void setTimeStamps() {

		if (getGroupsFile() != null)
			setTimeStampGroups(getGroupsFile().lastModified());
		if (getUsersFile() != null)
			setTimeStampUsers(getUsersFile().lastModified());
	}

	/** Name to UUID lookups **/

	/**
	 * Add a new name to UUID lookup.
	 *
	 * @param name the User name key to index on.
	 * @param UUID the User object UUID (same as name if there is no UUID).
	 */
	public void putUUIDLookup(String name, String UUID) {

		Set<String> lookup = getUUIDLookup(name.toLowerCase());

		if (lookup == null)
			lookup = new TreeSet<String>();

		lookup.add(UUID);

		nameToUUIDLookup.put(name.toLowerCase(), lookup);
	}

	/**
	 * Delete a name lookup.
	 * Allows for multiple UUID's assigned to a single name (offline/online)
	 *
	 * @param name
	 * @param UUID
	 */
	public void removeUUIDLookup(String name, String UUID) {

		if (nameToUUIDLookup.containsKey(name.toLowerCase())) {

			Set<String> lookup = getUUIDLookup(name.toLowerCase());

			lookup.remove(UUID);

			if (lookup.isEmpty()) {
				nameToUUIDLookup.remove(name.toLowerCase());
				return;
			}

			nameToUUIDLookup.put(name.toLowerCase(), lookup);

		}

	}

	/**
	 *
	 * @param name
	 * @return a Set of strings containing the User objects UUID (or name if they don't have a UUID)
	 */
	public Set<String> getUUIDLookup(String name) {

		return nameToUUIDLookup.get(name.toLowerCase());
	}

	/**
	 * Reset the UUID Lookup cache
	 */
	protected void clearUUIDLookup() {

		nameToUUIDLookup.clear();
	}

}
