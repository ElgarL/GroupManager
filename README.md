# GroupManager

GroupManager is a plug-in for Sigot/Bukkit based Minecraft servers. It allows you to group permissions together and add players to each group. Each group can have a full inheritance tree of other sub-groups, enabling players to have differing levels of authority and control with a promotion and demotion hierachy.

The most traditional system (shown below) uses group inheritance, starting with new players at the top, and owners on the bottom. On smaller servers you might end up merging the moderator and admin rolls, but most people will have different opinions when it comes to which powers the admin should have access to, and which powers moderators should receive as well.
___
- **Newbie**  
The newbie group contains the very basic permission nodes, and should be safe to give all newly joined players upon login.
- **Player**  
The player group covers all the commands that established players can use. This usually includes	commands which involve the economy, basic teleportation, and other basic commands.
- **Moderator**  
The Moderator and Admin groups can be merged or split even further based on your preference. Typically the lowest moderator rank would have access to kick and jail a user, while higher ranks could have access to IP ban players, turn invisible, and spawn creative mode items.
- **Admin**
- **Owner**  
The owner group usually has access to all permissions/commands, and is generally the only one with commands that control plugins, define groups, and ability to shut down/restart the server.
___

## Initial Install

The install process for Group Manager is rather simple:

    Safely stop the server.
    Place the GroupManager plug-in into your plugins folder. (GroupManager.jar)
    Start and stop the server.
    Set up/modify the initial config.yml file.
    Restart the server

## Permissions

At the very core of GroupManager's configuration are the permission nodes. These are the entire point of GroupManager, and control exactly what a player can and can't do. Each plugin has its own set of permission nodes, directly relating to the commands from that specific plugin. For each plugin you will need to check their related documentation.

**Permissions Nodes**  
Every plugin which supports a permissions plugin, will have their own permission nodes.

General Syntax: <plugin name>.<command name>

    Example Syntax: essentials.tp (Enables a player to teleport using Essentials' /tp command)
    Essentials is the name of the plugin and the command is /tp

The parent node won't always be the name of the plugin, so check before creating your groups. This feature allows plugins to have the same commands, but not conflict with each other when specifying which plugin's command to designate as default.

    Example: essentials.god and worldguard.god

**Wildcard Nodes**  
Group Manager supports wildcard nodes to enable all registered sub-permissions.

    essentials.* : Allows access to every essentials command
    essentials.kits.* : Allows access to use every kit

You can also give the '*' wildcard by itself, without specifying a plugin. This causes the specified group to have access to every command from every plugin. Note: Conflicting commands will become unstable unless negated by a negative node. Use at your own risk. Not all plugins properly register their permissions as well.

**Negative Nodes**  
GroupManager supports negation nodes, allowing you to retract access to commands. This can be useful with inheritance.

    essentials.kits.* : Allows access to every /kit
    -essentials.kits.admin : Removes access to just /kit admin

This can be useful when using the large wildcard permissions, such as giving an admin every command, but then removing access to the GM commands. Combining a wildcard node with a negative node can remove access to numerous sub-commands with ease.

    -essentials.kits.* : Removes access to every /kit

**Exception Nodes**  
GroupManager supports exception nodes, these will override negative nodes, allowing you to forcefully regain access to commands. This can be useful for easily removing access to all commands, then adding each one back individually.

    -essentials.signs.create.* : Removes access to create every Essentials signs
    +essentials.signs.create.trade : Overrides and gives access to create trade signs

## Example Permissions

There are a number of example (default) permissions files available for GroupManager:  
[Groups](https://github.com/ElgarL/GroupManager/blob/master/resources/groups.yml) :: [GlobalGroups](https://github.com/ElgarL/GroupManager/blob/master/resources/globalgroups.yml) 

## Variables
Group Manager allows you to define variables as well as permissions. These are values which are unique to the group. They are typically used in plugins to alter behaviour, based on groups. There are two major examples of this: the 'prefix' and 'suffix' variable and the 'build' toggle.

**Chat Plugins**  
Most chat plugins will read the prefix and suffix variable to allow you to give players custom names and colours based on their current group.

Some examples:

    prefix: '[A]' : Produces a simple <[A]User>
    prefix: '&e' : Produces a coloured <User>

**AntiGrief**  
Some plugins support group based 'antigrief' protection. Like with the chat colours, GroupManager doesn't provide this functionality.
Typically the build status is given as:

    build: false disables building/destroying of blocks.
    build: true enables building/destroying of blocks.

___

## Commands

GroupManager provides two ways to perform user and group management. Either define the groups and users using the config files or modify the users and groups using console commands. All these commands can be used in the server console or in-game by someone with op, or the relevant permissions for each command. It is recommended to define the groups at least initially using the config files and perform user management with the commands. To give all permissions you can use the wildcard permission groupmanager.*.

Contents
----|
1. [Rule of Thumb](#rule-of-thumb)
2. [User Management](#user-management)
3. [User Permissions](#user-permissions)
4. [User Variables](#user-variables)
5. [Group Managament](#group-management)
6. [Group Permissions](#group-permissions)
7. [Group Variables](#group-variables)
8. [Utility Commands](#utility-commands)
9. [Non-Command Permissions](#non-command-permissions)

## Rule of Thumb

GroupManager commands follow a fairly standard syntax:  

    man [u/g] [add/del/list/check] [p/i/v/sub]

man - GroupManager  
[u/g] - user / group  
[p/i/v/sub] - permission / inheritance / variable / subgroup  

## User Management

- /manuadd  
Move a player to a desired group (adds them to the users file if they don't already exist).
```
syntax: <command> <player> <group>  
permission: groupmanager.manuadd
```
- /manudel  
Remove any user specific configuration. Make them the default group.
```
syntax: <command> <player>  
permission: groupmanager.manudel
```
- /manuaddsub  
Add a group to a player's subgroup list.
```
syntax: <command> <player> <group>  
permission: groupmanager.manuaddsub
```
- /manudelsub  
Remove a group from a player's subgroup list.
```
syntax: <command> <player> <group>  
permission: groupmanager.manudelsub
```
- /manpromote  
Promotes a player up an inheritance tree. This command will only allow the user to move the player between groups that inherit.  
```
syntax: <command> <player> <group>  
permission: groupmanager.manpromote
```
- /mandemote  
Demotes a player down an inheritance tree. This command will only allow the user to move the player between groups they inherit.  
```
syntax: <command> <player> <group>  
permission: groupmanager.mandemote
```
- /manwhois  
Display the group that this user belongs to.  
```
syntax: <command> <player>  
permission: groupmanager.manwhois
```
## User Permissions

- /manuaddp  
Add permissions directly to the player. You can add multiple permissions with a single command.
```
syntax: <command> <player> <permission> [permission2] [permission3]...
permission: groupmanager.manuaddp
```
- /manudelp  
Removes permissions directly from the player. You can remove multiple permissions with a single command.
```
syntax: <command> <player> <permission> [permission2] [permission3]...
permission: groupmanager.manudelp
```
- /manulistp  
List all the permissions a player has.
```
syntax: <command> <player>
permission: groupmanager.manulistp
```
- /manucheckp  
Verify if user has a permission, and where it comes from.
```
syntax: <command> <player> <permission>
permission: groupmanager.manucheckp
```
## User Variables

- /manuaddv  
Add, or replaces, a variable on a user (like prefix or suffix).
```
syntax: <command> <user> <variable> <value>
permission: groupmanager.manuaddv
```
- /manudelv  
Remove a variable from a user.
```
syntax: <command> <user> <variable>
permission: groupmanager.manudelv
```
- /manulistv  
List all variables a user has (like prefix or suffix).
```
syntax: <command> <user>
permission: groupmanager.manulistv
```
- /manucheckv  
Verify the value of a variable of user, and where it comes from.
```
syntax: <command> <user> <variable>
permission: groupmanager.manucheckv
```
## Group Management

_Note the availability here for effecting globalgroups with the g: prefix._

- /mangadd  
Add a new group to the system.
```
syntax: <command> <group>
syntax: <command> <g:group>
permission: groupmanager.mangadd
```
- /mangdel  
Removes a group from the system (all its users become default).
```
syntax: <command> <group>
syntax: <command> <g:group>
permission: groupmanager.mangdel
```
- /mangaddi  
Add a group to another groups inheritance list.
```
syntax: <command> <group1> <group2>
permission: groupmanager.mangaddi
```
- /mangdeli  
Remove a group from another groups inheritance list.
```
syntax: <command> <group1> <group2>
permission: groupmanager.mangdeli
```
- /listgroups  
List the groups available.
```
syntax: <command>
permission: groupmanager.listgroups
```
## Group Permissions

_Note the availability here for effecting globalgroups with the g: prefix._

- /mangaddp  
Add permissions to a group. You can add multiple permissions with a single command.
```
syntax: <command> <group> <permission> [permission2] [permission3]...
syntax: <command> <g:group> <permission> [permission2] [permission3]...
permission: groupmanager.mangaddp
```
- /mangdelp  
Removes permission from a group. You can remove multiple permissions with a single command.
```
syntax: <command> <group> <permission> [permission2] [permission3]...
syntax: <command> <g:group> <permission> [permission2] [permission3]...
permission: groupmanager.mangdelp
```
- /manglistp  
Lists all permissions from a group.
```
syntax: <command> <group>
syntax: <command> <g:group>
permission: groupmanager.manglistp
```
- /mangcheckp  
Check if the group has a permission, and where it comes from.
```
syntax: <command> <group> <permission>
syntax: <command> <g:group> <permission>
permission: groupmanager.mangcheckp
```
## Group Variables

- /mangaddv  
Add, or replaces, a variable on a group (like prefix or suffix).
```
syntax: <command> <group> <variable> <value>
permission: groupmanager.mangaddv
```
- /mangdelv  
Remove a variable from a group.
```
syntax: <command> <group> <variable>
permission: groupmanager.mangdelv
```
- /manglistv  
List the variables a group has (like prefix or suffix).
```
syntax: <command> <group>
permission: groupmanager.manglistv
```
- /mangcheckv  
Verify the value of a variable of group, and where it comes from.
```
syntax: <command> <group> <variable>
permission: groupmanager.mangckeckv
```
## Utility Commands

- /mansave  
Save all permissions on file.
```
syntax: <command>
permission: groupmanager.mansave
```
- /manload  
Reload current world and config.yml, or load given world. [world] is an optional parameter.
```
syntax: <command> [world]
permission: groupmanager.manload
```
- /mantogglevalidate  
Toggle on/off the validating if player is online.
```
syntax: <command>
permission: groupmanager.mantogglevalidate
```
- /mantogglesave  
Toggle on/off the autosave. Use with **EXTREME** caution!
```
syntax: <command>
permission: groupmanager.mantogglesave
```
- /manworld  
Prints the selected world name.
```
syntax: <command>
permission: groupmanager.manworld
```
- /manselect  
Select a world to work with next commands.
```
syntax: <command> <world>
permission: groupmanager.manselect
```
- /manclear  
Clear world selection. Next commands will work on your world.
```
syntax: <command>
permission: groupmanager.manclear
```
- /mancheckw  
Obtain the paths to each file a world is storing it's data in (users/groups).
```
syntax: <command> <world>
permission: groupmanager.mancheckw
```

## Non-Command Permissions
    groupmanager.op  
        Overrides all inheritance and permissions when performing Group Manager commands
    groupmanager.notify.self  
        notifies when your rank changes.
    groupmanager.notify.other  
        notifies when someone's rank changes.
    groupmanager.noofflineperms  
        Denies all permissions if server is in offline mode.
___
