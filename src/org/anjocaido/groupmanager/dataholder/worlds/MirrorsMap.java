/*
 * 
 */
package org.anjocaido.groupmanager.dataholder.worlds;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.localization.Messages;

/**
 * @author ElgarL
 *
 */
public class MirrorsMap extends WorldsHolder {

	HashSet<String> mirroredWorlds = new HashSet<>();

	public MirrorsMap(GroupManager plugin) {

		super(plugin);
	}

	/**
	 * Parse the mirrorsMap and setup data for all worlds.
	 */
	@Override
	public void parseMirrors() {

		Map<String, Object> mirrorsMap = GroupManager.getGMConfig().getMirrorsMap();

		if (mirrorsMap == null) return;
		
		/*
		 * Add the server default world.
		 */
		if (serverDefaultWorldName != null)
			addRootWorld(serverDefaultWorldName);

		/*
		 * All keys under this entry should be world name/mirror maps.
		 */
		for (Object root : mirrorsMap.keySet()) {

			String rootWorld = root.toString().toLowerCase();
			/*
			 * The first root world sets the default
			 * if it's not already set.
			 */
			if (serverDefaultWorldName == null)
				serverDefaultWorldName = rootWorld;

			/*
			 * This is a root world so...
			 * 
			 * Store a key so we know to load data for
			 * this world, if there is no key stored already
			 * and there is no mirror defined.
			 */
			addRootWorld(rootWorld);

			/*
			 * Load all child entries for this node.
			 */
			Object child = mirrorsMap.get(root.toString());

			if (child instanceof ArrayList) {

				/*
				 * This is an array so the following
				 * worlds fully mirror their parent
				 */
				parseSubSection((ArrayList<?>) child, rootWorld);

			} else if (child instanceof Map) {

				/*
				 * This child is a Map entry so it could
				 * fully or partially mirror the parent
				 */
				parseSubSection(((Map<?, ?>) child), rootWorld);
			} else if (child != null) {

				/*
				 * If the child is null, then the world is un-mirrored and nothing needs to be done
				 */
				logger.log(Level.WARNING, String.format(Messages.getString("WorldsHolder.UNKNOWN_MIRRORING_FORMAT"), child.getClass().getName())); //$NON-NLS-1$
			}
		}

		/*
		 * Create a data source for any
		 * worlds we need to load.
		 */
		loadParentWorlds();

		// Create a data-set for any worlds not already loaded.
		for (String world : mirroredWorlds) {
			if (!isParentWorld(world)) {
				GroupManager.logger.log(Level.FINE, String.format(Messages.getString("WorldsHolder.NO_DATA"), world)); //$NON-NLS-1$
			}
			if (!hasGroupsMirror(world) || !hasUsersMirror(world)){
				/*
				 * Partial mirrors need data.
				 */
				addWorldData(world, null);
				getDataSource().init(world);
			}
			getDataSource().loadWorld(world, true);
		}
	}

	private void parseSubSection(ArrayList<?> subSection, String rootWorld) {

		for (Object o : subSection) {
			String world = o.toString().toLowerCase();
			if (!world.equalsIgnoreCase(serverDefaultWorldName)) {

				putGroupsMirror(world, getGroupsMirrorParent(rootWorld));
				logger.log(Level.FINE, String.format(Messages.getString("WorldsHolder.ADDING_GROUPS_MIRROR"), world)); //$NON-NLS-1$

				putUsersMirror(world, getUsersMirrorParent(rootWorld));
				logger.log(Level.FINE, String.format(Messages.getString("WorldsHolder.ADDING_USERS_MIRROR"), world)); //$NON-NLS-1$

				// Track this world so we can create data for it later.
				mirroredWorlds.add(o.toString());

			} else
				logger.log(Level.WARNING, String.format(Messages.getString("WorldsHolder.MIRRORING_ERROR"), world)); //$NON-NLS-1$
		}
	}

	private void parseSubSection(Map<?, ?> subSection, String rootWorld) {

		for (Object element : subSection.keySet()) {

			String world = element.toString();

			if (world.equalsIgnoreCase(serverDefaultWorldName)) {
				// Mirroring ourselves?!
				logger.log(Level.WARNING, String.format(Messages.getString("WorldsHolder.MIRRORING_ERROR"), world)); //$NON-NLS-1$
				continue;
			}

			if (subSection.get(element) instanceof ArrayList) {
				ArrayList<?> mirrorList = (ArrayList<?>) subSection.get(element);

				/*
				 * These worlds have defined mirroring
				 */
				for (Object o : mirrorList) {

					switch (o.toString().toLowerCase()) {

					case "groups":

						putGroupsMirror(world, getGroupsMirrorParent(rootWorld));
						logger.log(Level.FINE, String.format(Messages.getString("WorldsHolder.ADDING_GROUPS_MIRROR"), world)); //$NON-NLS-1$
						break;

					case "users":

						putUsersMirror(world, getUsersMirrorParent(rootWorld));
						logger.log(Level.FINE, String.format(Messages.getString("WorldsHolder.ADDING_USERS_MIRROR"), world)); //$NON-NLS-1$
					}
				}

				// Track this world so we can create data for it later.
				mirroredWorlds.add(world);

			} else {
				/*
				 * We don't support nesting!
				 */
				logger.log(Level.WARNING, String.format(Messages.getString("WorldsHolder.UNKNOWN_MIRRORING_FORMAT"), world)); //$NON-NLS-1$
			}
		}
	}
	
	private void addRootWorld(String rootWorld) {
		
		/*
		 * This is a root world so...
		 * 
		 * Store a key so we know to load data for
		 * this world, if there is no key stored already
		 * and there is no mirror defined.
		 */
		try {

			if (!isParentWorld(rootWorld) && (!hasGroupsMirror(rootWorld) || !hasUsersMirror(rootWorld)))
				addWorldData(rootWorld, null);

		} catch (Exception ignored) {}
	}
}
