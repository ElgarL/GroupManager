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

<a href="https://elgarl.github.io/GroupManager/">GroupManager Website</a> || <a href="https://github.com/ElgarL/GroupManager/blob/gh-pages/COMMANDS.md">GroupManager Commands</a> || <a href="https://github.com/ElgarL/GroupManager/blob/gh-pages/CONFIG.md">GroupManager Config</a>
