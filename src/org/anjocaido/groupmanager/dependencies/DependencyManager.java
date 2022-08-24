/*
 * 
 */
package org.anjocaido.groupmanager.dependencies;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.anjocaido.groupmanager.utils.Supported;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.yaml.snakeyaml.reader.UnicodeReader;

/**
 * @author ElgarL
 *
 */
public class DependencyManager {

	/**
	 * If older than Spigot 1.16.5, download
	 * and inject to the ClassPath all libraries
	 * as specified in the plugin.yml.
	 * 
	 * @param plugin	the Plugin instance
	 * @return	true if all dependencies are OK.
	 */
	public static boolean checkDependencies(Plugin plugin) {

		if (Supported.hasLibraries())
			return true;

		AtomicBoolean isLoaded = new AtomicBoolean(true);

		Consumer<Exception> error = ((ex) -> { // This consumer runs if an error occurs while loading the dependency
			plugin.getLogger().info(ex.getMessage());
			ex.printStackTrace();
			isLoaded.set(false);
		});

		Consumer<String> loaded = ((name) -> { // This consumer runs on a successful load.
			plugin.getLogger().info(name + " loaded!");
		});

		// Read all libraries from the plugin.yml and manually load them (pre 1.16.5).
		
		YamlConfiguration description = YamlConfiguration.loadConfiguration(new UnicodeReader(plugin.getResource("plugin.yml")));
		List<?> libraries = description.getList("libraries");

		if (libraries != null) {
			libraries.forEach(lib -> {

				String[] split = lib.toString().split(":");
				if (split.length == 3) {
					new MavenCentralDependency(plugin, split[0], split[1], split[2]).load(loaded, error);
				}
			});
		}

		return isLoaded.get();
	}
}
