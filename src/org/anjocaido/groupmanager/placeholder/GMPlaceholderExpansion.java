package org.anjocaido.groupmanager.placeholder;


import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.permissions.AnjoPermissionsHandler;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.jetbrains.annotations.NotNull;

/**
 * This class will automatically register as a placeholder expansion 
 * when a jar including this class is added to the directory 
 * {@code /plugins/PlaceholderAPI/expansions} on your server.
 * <br>
 * <br>If you create such a class inside your own plugin, you have to
 * register it manually in your plugins {@code onEnable()} by using 
 * {@code new YourExpansionClass().register();}
 */
public class GMPlaceholderExpansion extends PlaceholderExpansion {

	private final GroupManager plugin;

	/**
	 * Since we register the expansion inside our own plugin, we can simply use this
	 * method here to get an instance of our plugin.
	 *
	 * @param plugin The instance of our plugin.
	 */
	public GMPlaceholderExpansion(GroupManager plugin) {
		this.plugin = plugin;
	}
	
	/**
	 * Because this is an internal class, you must override this method to let
	 * PlaceholderAPI know to not unregister your expansion class when
	 * PlaceholderAPI is reloaded
	 *
	 * @return true to persist through reloads
	 */
	@Override
	public boolean persist() {
		return true;
	}
	
    /**
     * This method should always return true unless we
     * have a dependency we need to make sure is on the server
     * for our placeholders to work!
     *
     * @return always true since we do not have any dependencies.
     */
    @Override
    public boolean canRegister(){
        return true;
    }

    /**
     * The name of the person who created this expansion should go here.
     * 
     * @return The name of the author as a String.
     */
    @Override
    public @NotNull String getAuthor(){
    	return plugin.getDescription().getAuthors().toString();
    }

    /**
     * The placeholder identifier should go here.
     * <br>This is what tells PlaceholderAPI to call our onRequest 
     * method to obtain a value if a placeholder starts with our 
     * identifier.
     * <br>The identifier has to be lowercase and can't contain _ or %
     *
     * @return The identifier in {@code %<identifier>_<value>%} as String.
     */
    @Override
    public @NotNull String getIdentifier(){
        return "groupmanager";
    }

    /**
     * This is the version of this expansion.
     * <br>You don't have to use numbers, since it is set as a String.
     *
     * @return The version as a String.
     */
    @Override
    public @NotNull String getVersion(){
        return "1.0.0";
    }
  
    /**
     * This is the method called when a placeholder with our identifier 
     * is found and needs a value.
     * <br>We specify the value identifier in this method.
     * <br>Since version 2.9.1 can you use OfflinePlayers in your requests.
     *
     * @param  player
     *         A {@link org.bukkit.OfflinePlayer OfflinePlayer}.
     * @param  identifier
     *         A String containing the identifier/value.
     *
     * @return Possibly-null String of the requested identifier.
     */
    @Override
    public String onRequest(OfflinePlayer player, @NotNull String identifier){
  
    	if (player == null) {
			return "";
		}
    	
    	Player online = player.getPlayer();
    	/**
    	 * Only return data for online players as offline have no world.
    	 */
    	if (online == null) {
			return "";
		}
    	
    	final AnjoPermissionsHandler handler = plugin.getWorldsHolder().getWorldPermissions(online);
    	if (handler == null) return null;
    	
    	switch (identifier) {
    	
    	case "group":
    		
    		return handler.getPrimaryGroup(online.getName());
    		
    	case "allgroups":	// Includes inheritance.

    		return String.join(", ", handler.getGroups(online.getName()));
    		
    	case "subgroups":	// Includes timed.

    		return String.join(", ", handler.getSubGroups(online.getName()));
    		
    	case "user_prefix":
    		
    		return handler.getUserPrefix(online.getName());
    		
    	case "user_suffix":
    		
    		return handler.getUserSuffix(online.getName());
    		
    	case "group_prefix":
    		
    		return handler.getGroupPrefix(handler.getGroup(online.getName()));
    		
    	case "group_suffix":
    		
    		return handler.getGroupSuffix(handler.getGroup(online.getName()));
    		
    	default:
    		
    		String[] split = identifier.split("_");

			if ("perm".equals(split[0])) {    // Perm check via PAPI.

				if (split.length == 2) {

					return String.valueOf(handler.has(online, split[1]));
				}
			}
    		
    		// We return null if an invalid placeholder (f.e. %groupmanager_placeholder3%) 
            // was provided
    		return null;
    	}
    }
}
