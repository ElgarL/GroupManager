<a href="https://elgarl.github.io/GroupManager/">GroupManager Website</a> || <a href="https://elgarl.github.io/GroupManager/COMMANDS">GroupManager Commands</a> || <a href="https://elgarl.github.io/GroupManager/CONFIG">GroupManager Config</a>
# Commands

GroupManager provides two ways to perform user and group management. Either define the groups and users using the config files or modify the users and groups using console commands. All these commands can be used in the server console or in-game by someone with OP, or the relevant permissions for each command. It is recommended to define the groups at least initially using the config files and perform user management with the commands. To give all permissions you can use the wildcard permission groupmanager.*.

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

When adding permissions you can now set a duration after which they will expire.
```
eg /manuaddp towny.wild.*|1d2h32m essentials.afk|10m
```

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
- /manglist  
List the groups available.
```
syntax: <command>
permission: groupmanager.manglist
```
## Group Permissions

When adding permissions you can now set a duration after which they will expire.
```
eg /mangaddp default towny.wild.*|1d2h32m essentials.afk|10m
```

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
Save all permissions in a file.
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
- /manuaddtemp  
Add a user in the overloaded users list (changes made to them will not be saved).
```
syntax: <command> <user>
permission: groupmanager.tempadd
```
- /manudeltemp  
Remove a user from the overloaded users list.
```
syntax: <command> <user>
permission: groupmanager.tempdel
```
- /manulisttemp  
List overloaded users.
```
syntax: <command>
permission: groupmanager.templist
```
- /manudelalltemp  
Remove all users from the overloaded users list.
```
syntax: <command>
permission: groupmanager.tempdelall
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
