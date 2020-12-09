/*
 * 
 */
package org.anjocaido.groupmanager.dataholder.worlds;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author ElgarL
 *
 */
public class ChildMirrors {

	/**
	 * Map of mirrors: <nonExistingWorldName, existingAndLoadedWorldName>
	 * The key is the mirror.
	 * The object is the parent.
	 * 
	 * Mirror shows the same data as parent.
	 */
	private Map<String, String> groupsMirror = new TreeMap<>();
	private Map<String, String> usersMirror = new TreeMap<>();
	
	/**
	 * Find the top level parent world or the
	 * same world name if no mirror exists.
	 * 
	 * @param worldName
	 * @return the highest parent mirror that will have data.
	 */
	public String getGroupsMirrorParent(String worldName) {

		String result = worldName.toLowerCase();

		while (groupsMirror.containsKey(result)) {
			result = groupsMirror.get(result);
		}
		return result;
	}

	/**
	 * Find the top level parent world or the
	 * same world name if no mirror exists.
	 * 
	 * @param worldName
	 * @return the highest parent mirror that will have data.
	 */
	public String getUsersMirrorParent(String worldName) {

		String result = worldName.toLowerCase();

		while (usersMirror.containsKey(result)) {
			result = usersMirror.get(result).toLowerCase();
		}
		return result;
	}
	
	/**
	 * Is this world using mirrored Groups.
	 * 
	 * @param worldName
	 * @return			true if mirrored.
	 */
	public boolean hasGroupsMirror(String worldName) {
		
		return groupsMirror.containsKey(worldName.toLowerCase());
	}
	
	/**
	 * Is this world using mirrored Users.
	 * 
	 * @param worldName
	 * @return			true if mirrored.
	 */
	public boolean hasUsersMirror(String worldName) {
		
		return usersMirror.containsKey(worldName.toLowerCase());
	}
	
	protected void clearGroupsMirror() {

		synchronized (groupsMirror) {
			groupsMirror.clear();
		}
	}
	
	protected void clearUsersMirror() {

		synchronized (usersMirror) {
			usersMirror.clear();
		}
	}
	
	public String getGroupsMirror(String worldName) {

		synchronized (groupsMirror) {
			return groupsMirror.get(worldName.toLowerCase());
		}
	}

	public void putGroupsMirror(String worldName, String parent) {

		synchronized (groupsMirror) {
			groupsMirror.put(worldName.toLowerCase(), parent.toLowerCase());
		}
	}
	
	public String getUsersMirror(String worldName) {

		synchronized (usersMirror) {
			return usersMirror.get(worldName.toLowerCase());
		}
	}

	public void putUsersMirror(String worldName, String parent) {

		synchronized (usersMirror) {
			usersMirror.put(worldName.toLowerCase(), parent.toLowerCase());
		}
	}
}
