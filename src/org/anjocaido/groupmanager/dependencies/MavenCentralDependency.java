/*
 * 
 */
package org.anjocaido.groupmanager.dependencies;

import java.net.MalformedURLException;
import java.net.URL;

import org.bukkit.plugin.Plugin;

/**
 * Single Thread dependency management.
 * 
 * Thanks to https://www.spigotmc.org/resources/dependencymanager.62275/
 * DotRar for the original idea.
 */
public class MavenCentralDependency extends Dependency {

	private final String rootUrl = "https://repo1.maven.org/maven2";
	private final String group, artifact, version;

	/**
	 * Single Threaded dependency management.
	 * Forces all dependencies to be available before loading.
	 * 
	 * @param plugin	calling Plugin
	 * @param group		groupId
	 * @param artifact	artifactId
	 * @param version	dependency version
	 */
	MavenCentralDependency(Plugin plugin, String group, String artifact, String version) {

		super(plugin);

		this.group = group;
		this.artifact = artifact;
		this.version = version;
	}

	@Override
	protected URL buildUrl() throws MalformedURLException {

		String groupSlashed = String.join("/", group.split("\\."));
		String jarName = String.format("%s-%s.jar", artifact, version);
		return new URL(String.format("%s/%s/%s/%s/%s", rootUrl, groupSlashed, artifact, version, jarName));
	}

	@Override
	protected String getLocalName() {

		return String.format("%s-%s.jar", artifact, version);
	}
}
