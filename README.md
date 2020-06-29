## GroupManager

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
[GroupManager Groups](https://github.com/ElgarL/GroupManager/blob/master/resources/groups.yml) :: [GroupManager GlobalGroups](https://github.com/ElgarL/GroupManager/blob/master/resources/globalgroups.yml) 

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

Group Manager commands follow a fairly standard syntax:  

    man [u/g] [add/del/list/check] [p/i/v/sub]

man - GroupManager  
[u/g] - user / group  
[p/i/v/sub] - permission / inheritance / variable / subgroup  

## User Management

- /manuadd: Move a player to desired group (adds them to the file if it doesn't exist).
```
syntax: <command> <player> <group>  
permission: groupmanager.manuadd
```
- /manudel: Remove any user specific configuration. Make him default group.
```
syntax: <command> <player>  
permission: groupmanager.manudel
```
- /manuaddsub: Add a group to a player's subgroup list.
```
syntax: <command> <player> <group>  
permission: groupmanager.manuaddsub
```
- /manudelsub: Remove a group from a player's subgroup list.
```
syntax: <command> <player> <group>  
permission: groupmanager.manudelsub
```
- /manpromote: Allows promoting a player up the inheritance tree. This command will only allow the user to move the player between groups they inherit.  
```
syntax: <command> <player> <group>  
permission: groupmanager.manpromote
```
- /mandemote: Allows demoting a player down the inheritance tree. This command will only allow the user to move the player between groups they inherit.  
```
syntax: <command> <player> <group>  
permission: groupmanager.mandemote
```
- /manwhois: Tell the group that this user belongs to.  
```
syntax: <command> <player>  
permission: groupmanager.manwhois
```
## User Permissions

- /manuaddp: Add permission directly to the player.
```
syntax: <command> <player> <permission>
permission: groupmanager.manuaddp
```
- /manudelp: Removes permission directly from the player.
```
syntax: <command> <player> <permission>
permission: groupmanager.manudelp
```
- /manulistp: List all permissions from a player.
```
syntax: <command> <player>
permission: groupmanager.manulistp
```
- /manucheckp: Verify if user has a permission, and where it comes from.
```
syntax: <command> <player> <permission>
permission: groupmanager.manucheckp
```
## User Variables

- /manuaddv: Add, or replaces, a variable to a user (like prefix or suffix).
```
syntax: <command> <user> <variable> <value>
permission: groupmanager.manuaddv
```
- /manudelv: Remove a variable from a user.
```
syntax: <command> <user> <variable>
permission: groupmanager.manudelv
```
- /manulistv: List variables a user has (like prefix or suffix).
```
syntax: <command> <user>
permission: groupmanager.manulistv
```
- /manucheckv: Verify a value of a variable of user, and where it comes from.
```
syntax: <command> <user> <variable>
permission: groupmanager.manucheckv
```
## Group Management

Note the availability here for effecting globalgroups with the g: prefix.

- /mangadd: Add group to the system.
```
syntax: <command> <group>
syntax: <command> <g:group>
permission: groupmanager.mangadd
```
- /mangdel: Removes a group from the system (all its users become default).
```
syntax: <command> <group>
syntax: <command> <g:group>
permission: groupmanager.mangdel
```
- /mangaddi: Add a group to another group inheritance list.
```
syntax: <command> <group1> <group2>
permission: groupmanager.mangaddi
```
- /mangdeli: Remove a group from another group inheritance list.
```
syntax: <command> <group1> <group2>
permission: groupmanager.mangdeli
```
- /listgroups: List the groups available.
```
syntax: <command>
permission: groupmanager.listgroups
```
## Group Permissions

Note the availability here for effecting globalgroups with the g: prefix.

- /mangaddp: Add permission to a group.
```
syntax: <command> <group> <permission>
syntax: <command> <g:group> <permission>
permission: groupmanager.mangaddp
```
- /mangdelp: Removes permission from a group.
```
syntax: <command> <group> <permission>
syntax: <command> <g:group> <permission>
permission: groupmanager.mangdelp
```
- /manglistp: Lists all permissions from a group.
```
syntax: <command> <group>
syntax: <command> <g:group>
permission: groupmanager.manglistp
```
- /mangcheckp: Check if group has a permission, and where it comes from.
```
syntax: <command> <group> <permission>
syntax: <command> <g:group> <permission>
permission: groupmanager.mangcheckp
```
## Group Variables

- /mangaddv: Add, or replaces, a variable to a group (like prefix or suffix).
```
syntax: <command> <group> <variable> <value>
permission: groupmanager.mangaddv
```
- /mangdelv: Remove a variable from a group.
```
syntax: <command> <group> <variable>
permission: groupmanager.mangdelv
```
- /manglistv: List variables a group has (like prefix or suffix).
```
syntax: <command> <group>
permission: groupmanager.manglistv
```
- /mangcheckv: Verify a value of a variable of group, and where it comes from.
```
syntax: <command> <group> <variable>
permission: groupmanager.mangckeckv
```
## Utility Commands

- /mansave: Save all permissions on file.
```
syntax: <command>
permission: groupmanager.mansave
```
- /manload: Reload current world and config.yml, or load given world.
```
syntax: <command> [world]
permission: groupmanager.manload
```
- /mantogglevalidate: Toggle on/off the validating if player is online.
```
syntax: <command>
permission: groupmanager.mantogglevalidate
```
- /mantogglesave: Toggle on/off the autosave.
```
syntax: <command>
permission: groupmanager.mantogglesave
```
- /manworld: Prints the selected world name.
```
syntax: <command>
permission: groupmanager.manworld
```
- /manselect: Select a world to work with next commands.
```
syntax: <command> <world>
permission: groupmanager.manselect
```
- /manclear: Clear world selection. Next commands will work on your world.
```
syntax: <command>
permission: groupmanager.manclear
```
- /mancheckw: Obtain the paths to each file a world is storing it's data in (users/groups).
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
## Welcome to GitHub Pages
 
You can use the [editor on GitHub](https://github.com/ElgarL/GroupManager/edit/gh-pages/README.md) to maintain and preview the content for your website in Markdown files.

Whenever you commit to this repository, GitHub Pages will run [Jekyll](https://jekyllrb.com/) to rebuild the pages in your site, from the content in your Markdown files.

### Markdown

Markdown is a lightweight and easy-to-use syntax for styling your writing. It includes conventions for

```markdown
Syntax highlighted code block

# Header 1
## Header 2
### Header 3

- Bulleted
- List

1. Numbered
2. List

**Bold** and _Italic_ and `Code` text

[Link](url) and ![Image](src)
```

For more details see [GitHub Flavored Markdown](https://guides.github.com/features/mastering-markdown/).

### Jekyll Themes

Your Pages site will use the layout and styles from the Jekyll theme you have selected in your [repository settings](https://github.com/ElgarL/GroupManager/settings). The name of this theme is saved in the Jekyll `_config.yml` configuration file.

### Support or Contact

Having trouble with Pages? Check out our [documentation](https://help.github.com/categories/github-pages-basics/) or [contact support](https://github.com/contact) and we’ll help you sort it out.
