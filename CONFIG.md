## Config

This page details the default and basic configuration of the Group Manager config files.

Warning: Group manager overwrites its config files on reload, if you make manual changes be sure to /manload them.

Contents|
---
    1 After Install
        1.1 config.yml
        1.2 Mirroring
        1.3 group.yml
        1.4 user.yml
        1.5 globalgroups.yml
    2 Permissions
        2.1 Example Permissions
    3 Minimal
    4 Video Tutorial

## After Install

When you first download GM, you will generally only have a .jar file. The very first time you run GM it will generate some config files which you can then edit and maintain.

The standard config is usually something which is very similar to the layout detailed on the main page, but you will need to edit the configuration at least a little before use, if nothing else than adding yourself as admin.

The directory structure when you first generate the config files will look something like this:

    plugins/
        GroupManager/
            config.yml
            globalgroups.yml
            backups/
            worlds/
                world/
                    groups.yml
                    users.yml

## config.yml
Located at: plugins/GroupManager/config.yml

This config file is used when you have multiple worlds. It is used to copy the permissions from a single world across to others.

The config file will look something like this:
```
settings:
  config:
    # With this enabled anyone set as op has full permissions when managing GroupManager
    # The user will be able to promote players to the same group or even above.
    opOverrides: true
    
    # Default setting for 'mantogglevalidate'
    # true will cause GroupManager to attempt name matching by default.
    validate_toggle: true
    # ************************************************************************************************************************************************************
    # *** NOTE: Having this feature enabled can allow improper use of Command Blocks which may lead to undesireable permission changes. You have been warned! ***
    # ************************************************************************************************************************************************************
    allow_commandblocks: false
    
  data:
    save:
      # How often GroupManager will save it's data back to groups.yml and users.yml
      minutes: 10
      # Number of hours to retain backups (plugins/GroupManager/backup)
      hours: 24
      
  logging:
    # Level of detail GroupManager will use when logging.
    # Acceptable entries are - ALL,CONFIG,FINE,FINER,FINEST,INFO,OFF,SEVERE,WARNING
    level: INFO
    
  mirrors:
        # Worlds listed here have their settings mirrored in their children.
        # The first element 'world' is the main worlds name, and is the parent world.
        # subsequent elements 'world_nether' and 'world_the_end' are worlds which will use
        # the same user/groups files as the parent.
        # the element 'all_unnamed_worlds' specifies all worlds that aren't listed, and automatically mirrors them to it's parent.
        # Each child world can be configured to mirror the 'groups', 'users' or both files from its parent.
        world:
          world_nether:
          - users
          - groups
          world_the_end:
          - users
          - groups
          all_unnamed_worlds:
          - users
          - groups
    #  world2:      (World2 would have it's own set of user and groups files)
    #    world3:
    #    - users    (World3 would use the users.yml from world2, but it's own groups.yml)
    #    world4:
    #    - groups   (World4 would use the groups.yml from world2, but it's own users.yml)
    #  world5:
    #    - world6   (this would cause world6 to mirror both files from world5)
```

## Mirroring

Here is an example of how to use mirroring in GM

# This is an example GroupManager Mirroring system.
# This will take you into some more complex GM Mirroring.
```
  mirrors:
        MainWorld:
          MainWorld_nether:
          - users
          - groups
          Hardcore:
          - groups
        Skylands:
          Skylands2:
          - users
          - groups
        Hardcore:
          Hardcore_nether:
          - users
          - groups
          all_unnamed_worlds:
          - users
          - groups
```  
In this example you would be expected to maintain several sets of config files. Skylands and Skylands2 will share the Skylands folder. Mainworld, and the MainWorld_nether will share a folder. Hardcore will use the groups files from MainWorld but will have its own user file, and all other worlds will use the Hardcore user file and Mainworld groups file.

## group.yml
Located at: plugins/GroupManager/worlds/<worldname>/group.yml

This is the main and most important config file. This is where you declare the groups for which you are going to be using. The example config file is a little too big to post here, but it will be automatically generated the first time you start GM.

The basics of group manager can be summed up by the example shown here:
```
groups:
  Default:
    default: true
    permissions:
    - essentials.help
    - essentials.help.*
    - -essentials.help.factions
    - essentials.helpop
    - essentials.list
    - essentials.motd
    - essentials.rules
    - essentials.spawn
    - essentials.eco
    inheritance: []
    info:
      prefix: '&e'
      build: false
      suffix: ''
  Builder:
    default: false
    permissions:
    - essentials.home
    - essentials.me
    - essentials.msg
    - essentials.sethome
    - essentials.warp
    inheritance:
    - default
    info:
      prefix: '&2'
      build: true
      suffix: ''
```  
This example has a couple of notable characteristics worth pointing out and is quite useful for most servers:

    Look at the indentation closely, YAML is very strict that this is correct and will error if it is wrong.
    The 2nd line gives the group name, this should be '<name>:'.
    The 3rd line stipulates that this will be the group that users join automatically, there can only be one of these per config file.
    The next section lists all the permissions you want to give to a user, in this case it is only the most basic commands.
    The inheritance section would allow you to build on another user, simply list the group you want to inherit from like the permissions node above.
    The prefix can be shown before the players name, in this case it is a colour code and would make the user a different colour.
    The build toggle is used by some plugins, in this example people in the default group would not be able to build.
    The "default" group, has an example of negative permissions, to specifically revoke a permission you simply add a '-' before the beginning of the permission, in this example revoking the 'essentials.help.factions' permission. The line above is an example of wildcard permissions, which means people in the default group will see all plugins command help, except for factions.

The default configuration has many more such groups with an array of permissions, and uses something called global groups. This is used to make multiple world configs simpler, more details below.

## user.yml  
Located at: plugins/GroupManager/worlds/<worldname>/user.yml

This is you define which user goes into which group. You should remember to make sure you add yourself to this config, so you have access to the management commands in game. Most people will rarely edit this file directly, as its simpler to edit it with the ingame commands.

The config file will look something like this:
```
users:
  kimkandor:
    subgroups: []
    permissions:
    - essentials.heal
    group: Default
  zenexer:
    subgroups: []
    permissions: []
    group: Mod
  khobbits:
    subgroups: []
    permissions: []
    group: Admin
```  
This example has a couple of notable characteristics worth pointing out and is quite useful for most servers:

    Look at the indentation closely, YAML is very strict that this is correct and will error if it is wrong.
    The 2nd line gives the user name, this should be '<name>:'.
    The permissions section lists any permissions in addition to group. In this example 'kimkandor' gets a the extra permission to be able to /heal herself.
    The last line of each user stipulates which group the user belongs too.

## globalgroups.yml  
Located at: plugins/GroupManager/globalgroups.yml

This file is used to make predefined permission 'sets'. The groups in this file will never be given directly to a user, and simply be added to a real groups inheritance.

The point of global groups, is to make it easier to manage servers with multiple worlds. Instead of having to copy the permissions from each world, every time you make a change, you can simply make a global 'mod' group, and list all your normal mod permissions there. That way, you simply add 'g:mod' to the inheritance of the mod groups in each world, and that mod group gets all the permissions from the global file.

The use of the global groups file is mostly optional. You can delete most the groups out of this file, and move the permissions to the groups.yml, as long as you leave the top line of the global groups file. Doing so, is down to personal preference, the main thing is to make sure that if you rename or move any groups in global groups, you also update the name in the inheritance of each world.

## Permissions
At the very core of GroupManager configuration are the permission nodes. These are the entire point of GM, and control exactly what a player can do. Each plugin has its own set of permissions nodes. The default config file with GM reflects some examples.

For other plugins you will need to check the related documentation. More detail on the permissions and details such as wildcard nodes can be found on the GroupManager page.

## Example Permissions
There are a number of example (default) permissions files available for GroupManager:  
[Groups](https://github.com/ElgarL/GroupManager/blob/master/resources/groups.yml) :: [GlobalGroups](https://github.com/ElgarL/GroupManager/blob/master/resources/globalgroups.yml) 

## Minimal

It has been asked in the past, what is the most basic config file needed to run a server. The following config is the most basic config we could recommend, and for most servers this will simply not allow enough control, or provide enough commands to the users.

groups:
  Default:
    default: true
    permissions:
    - essentials.help
    - essentials.list
    - essentials.motd
    - essentials.rules
    info:
      prefix: '&5'
      build: true
      suffix: ''
  Admin:
    default: false
    permissions:
    - '*'
    inheritance:
    - default
    info:
      prefix: '&4'
      build: true
      suffix: ''
