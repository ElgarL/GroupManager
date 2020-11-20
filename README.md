# GroupManager
The original permission plug-in for [Bukkit/Spigot](https://www.spigotmc.org/resources/groupmanager.38875/).

**Wiki**  
[https://elgarl.github.io/GroupManager/](https://elgarl.github.io/GroupManager/)

**Bug reports**  
[https://github.com/ElgarL/GroupManager/issues](https://github.com/ElgarL/GroupManager/issues)

**Support**  
Support my Dev work via PayPal - [Donate](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=95VVUXYDUCDH8&source=url).  
Ask for assistance in the GroupManager Discord.
[https://discord.gg/BdGxVmu](https://discord.gg/BdGxVmu)

---
**To include Groupmanager as a dependency in your own plugins.**

Add a repository in your pom.xml
>        <repository>
>            <id>jitpack.io</id>
>            <url>https://jitpack.io</url>
>        </repository>  
Then add as a dependency in your pom.xml
>        <dependency>
>            <groupId>com.github.ElgarL</groupId>
>            <artifactId>groupmanager</artifactId>
>            <version>2.9</version>
>        </dependency>  

---
This sample class can be used in your plugin to access GroupManager.
        
```
import java.util.Arrays;
import java.util.List;

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.dataholder.OverloadedWorldHolder;
import org.anjocaido.groupmanager.permissions.AnjoPermissionsHandler;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

public class GMHook
{
	private GroupManager groupManager;
	private Plugin plugin;

	public GMHook(final Plugin plugin) {
		this.plugin = plugin;
	}

	public boolean hasGroupManager() {
		
		if (groupManager != null) return true;
			
		final PluginManager pluginManager = plugin.getServer().getPluginManager();
		final Plugin GMplugin = pluginManager.getPlugin("GroupManager");

		if (GMplugin != null && GMplugin.isEnabled()) {
			groupManager = (GroupManager)GMplugin;
			return true;
		}
		return false;
	}

	public String getGroup(final Player player) {
		if (!hasGroupManager()) return null;
		
		final AnjoPermissionsHandler handler = groupManager.getWorldsHolder().getWorldPermissions(player);
		if (handler == null) return null;

		return handler.getGroup(player.getName());
	}

	public boolean setGroup(final Player player, final String group) {
		if (!hasGroupManager()) return false;
		
		final OverloadedWorldHolder handler = groupManager.getWorldsHolder().getWorldData(player);
		if (handler == null) return false;
		
		handler.getUser(player.getName()).setGroup(handler.getGroup(group));
		return true;
	}

	public List<String> getGroups(final Player player) {
		if (!hasGroupManager()) return null;
		
		final AnjoPermissionsHandler handler = groupManager.getWorldsHolder().getWorldPermissions(player);
		if (handler == null) return null;
		
		return Arrays.asList(handler.getGroups(player.getName()));
	}

	public String getPrefix(final Player player) {
		if (!hasGroupManager()) return null;
		
		final AnjoPermissionsHandler handler = groupManager.getWorldsHolder().getWorldPermissions(player);
		if (handler == null) return null;
		
		return handler.getUserPrefix(player.getName());
	}

	public String getSuffix(final Player player) {
		if (!hasGroupManager()) return null;
		
		final AnjoPermissionsHandler handler = groupManager.getWorldsHolder().getWorldPermissions(player);
		if (handler == null) return null;
		
		return handler.getUserSuffix(player.getName());
	}

	/**
	 * Use Player.hasPermission
	 * 
	 * @param player
	 * @param node
	 * @return
	 */
	@Deprecated
	public boolean hasPermission(final Player player, final String node) {
		if (!hasGroupManager()) return false;
		
		final AnjoPermissionsHandler handler = groupManager.getWorldsHolder().getWorldPermissions(player);
		if (handler == null) return false;
		
		return handler.has(player, node);
	}
}
