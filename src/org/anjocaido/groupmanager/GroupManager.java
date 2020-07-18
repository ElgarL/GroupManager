/*
 *  GroupManager - A plug-in for Spigot/Bukkit based Minecraft servers.
 *  Copyright (C) 2020  ElgarL
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.anjocaido.groupmanager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.anjocaido.groupmanager.Tasks.BukkitPermsUpdateTask;
import org.anjocaido.groupmanager.commands.ListGroups;
import org.anjocaido.groupmanager.commands.ManCheckW;
import org.anjocaido.groupmanager.commands.ManClear;
import org.anjocaido.groupmanager.commands.ManDemote;
import org.anjocaido.groupmanager.commands.ManGAdd;
import org.anjocaido.groupmanager.commands.ManGAddI;
import org.anjocaido.groupmanager.commands.ManGAddP;
import org.anjocaido.groupmanager.commands.ManGAddV;
import org.anjocaido.groupmanager.commands.ManGCheckP;
import org.anjocaido.groupmanager.commands.ManGCheckV;
import org.anjocaido.groupmanager.commands.ManGClearP;
import org.anjocaido.groupmanager.commands.ManGDel;
import org.anjocaido.groupmanager.commands.ManGDelI;
import org.anjocaido.groupmanager.commands.ManGDelP;
import org.anjocaido.groupmanager.commands.ManGDelV;
import org.anjocaido.groupmanager.commands.ManGListP;
import org.anjocaido.groupmanager.commands.ManGListV;
import org.anjocaido.groupmanager.commands.ManLoad;
import org.anjocaido.groupmanager.commands.ManPromote;
import org.anjocaido.groupmanager.commands.ManSave;
import org.anjocaido.groupmanager.commands.ManSelect;
import org.anjocaido.groupmanager.commands.ManToggleSave;
import org.anjocaido.groupmanager.commands.ManToggleValidate;
import org.anjocaido.groupmanager.commands.ManUAdd;
import org.anjocaido.groupmanager.commands.ManUAddP;
import org.anjocaido.groupmanager.commands.ManUAddSub;
import org.anjocaido.groupmanager.commands.ManUAddV;
import org.anjocaido.groupmanager.commands.ManUCheckP;
import org.anjocaido.groupmanager.commands.ManUCheckV;
import org.anjocaido.groupmanager.commands.ManUClearP;
import org.anjocaido.groupmanager.commands.ManUDel;
import org.anjocaido.groupmanager.commands.ManUDelP;
import org.anjocaido.groupmanager.commands.ManUDelSub;
import org.anjocaido.groupmanager.commands.ManUDelV;
import org.anjocaido.groupmanager.commands.ManUListP;
import org.anjocaido.groupmanager.commands.ManUListV;
import org.anjocaido.groupmanager.commands.ManWhois;
import org.anjocaido.groupmanager.commands.ManWorld;
import org.anjocaido.groupmanager.commands.TempAdd;
import org.anjocaido.groupmanager.commands.TempDel;
import org.anjocaido.groupmanager.commands.TempDelAll;
import org.anjocaido.groupmanager.commands.TempList;
import org.anjocaido.groupmanager.data.User;
import org.anjocaido.groupmanager.dataholder.worlds.WorldsHolder;
import org.anjocaido.groupmanager.events.GMWorldListener;
import org.anjocaido.groupmanager.events.GroupManagerEventHandler;
import org.anjocaido.groupmanager.metrics.Metrics;
import org.anjocaido.groupmanager.permissions.BukkitPermissions;
import org.anjocaido.groupmanager.utils.GMLoggerHandler;
import org.anjocaido.groupmanager.utils.PermissionCheckResult;
import org.anjocaido.groupmanager.utils.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;


/**
 *
 * @author gabrielcouto, ElgarL
 */
public class GroupManager extends JavaPlugin {
	
	public GroupManager() {}
	
	private File backupFolder;
	private Runnable commiter;
	private Runnable cleanup;
	private ScheduledThreadPoolExecutor scheduler;
	private static Map<String, ArrayList<User>> overloadedUsers = new HashMap<String, ArrayList<User>>();
	private static Map<String, String> selectedWorlds = new HashMap<String, String>();
	
	private static WorldsHolder worldsHolder;
	
	private boolean validateOnlinePlayer = true;
	
	private static boolean isLoaded = false;
	private static GMConfiguration config;
	private AtomicBoolean lock = new AtomicBoolean(false);
	
	private String lastError = "";

	private static GlobalGroups globalGroups;

	private GMLoggerHandler ch;
	
	private static GroupManagerEventHandler GMEventHandler;
	private static BukkitPermissions BukkitPermissions;
	private GMWorldListener WorldEvents;
	public static final Logger logger = Logger.getLogger(GroupManager.class.getName());


	@Override
	public void onDisable() {
		
		onDisable(false);
	}
	
	@Override
	public void onEnable() {
		
		/*
		 * Register Metrics
		 */
		try {
		    @SuppressWarnings("unused")
			Metrics metrics = new Metrics(this, 7982);
		    
		    //metrics.addCustomChart(new Metrics.SimplePie("chart_id", () -> "My value"));
		} catch (Exception e) {
			System.err.println("[GroupManager] Error setting up metrics");
		}
		
		/*
		 * Initialize the event handler
		 */
		setGMEventHandler(new GroupManagerEventHandler(this));
		onEnable(false);
	}
	
	public void onDisable(boolean restarting) {

		setLoaded(false);

		if (!restarting) {
			// Unregister this service if we are shutting down.
			this.getServer().getServicesManager().unregister(getWorldsHolder());
		}

		disableScheduler(); // Shutdown before we save, so it doesn't interfere.
		if (worldsHolder != null) {
			try {
				worldsHolder.saveChanges(false);
			} catch (IllegalStateException ex) {
				GroupManager.logger.warning(ex.getMessage());
			}
		}

		// Remove all attachments before clearing
		if (BukkitPermissions != null) {
			BukkitPermissions.removeAllAttachments();
		}
		
		if (!restarting) {
			
			if (WorldEvents != null)
				WorldEvents = null;

			BukkitPermissions = null;
			
		}

		// log that we are disabled.
		PluginDescriptionFile pdfFile = this.getDescription();
		GroupManager.logger.warning("version " + pdfFile.getVersion() + " is disabled!");

		
		if (!restarting)
			GroupManager.logger.removeHandler(ch);
	}
	
	public void onEnable(boolean restarting) {

		try {
			/*
			 * reset local variables.
			 */
			overloadedUsers = new HashMap<String, ArrayList<User>>();
			selectedWorlds = new HashMap<String, String>();
			lastError = "";
			
			/*
			 * Setup our logger if we are not restarting.
			 */
			if (!restarting) {
				GroupManager.logger.setUseParentHandlers(false);
				ch = new GMLoggerHandler();
				GroupManager.logger.addHandler(ch);
			}
			GroupManager.logger.setLevel(Level.ALL);

			// Create the backup folder, if it doesn't exist.
			prepareBackupFolder();
			// Load the config.yml
			prepareConfig();
			// Load the global groups
			globalGroups = new GlobalGroups(this);
			
			/*
			 * Configure the worlds holder.
			 */
			if (!restarting)
				worldsHolder = new WorldsHolder(this);
			else
				worldsHolder.resetWorldsHolder();

			/*
			 * This should NEVER happen. No idea why it's still here.
			 */
			PluginDescriptionFile pdfFile = this.getDescription();
			if (worldsHolder == null) {
				GroupManager.logger.severe("Can't enable " + pdfFile.getName() + " version " + pdfFile.getVersion() + ", bad loading!");
				this.getServer().getPluginManager().disablePlugin(this);
				throw new IllegalStateException("An error ocurred while loading GroupManager");
			}

			/*
			 *  Prevent our registered events from triggering
			 *  updates as we are not fully loaded.
			 */
			setLoaded(false);

			/*
			 *  Initialize the world listener and Bukkit permissions
			 *  to handle events and initialize our command handlers
			 *  if this is a fresh start
			 *  
			 *  else
			 *  
			 *  Reset bukkit permissions.
			 */
			if (!restarting) {
				WorldEvents = new GMWorldListener(this);
				BukkitPermissions = new BukkitPermissions(this);
				
				initCommands();
				
			} else {
				BukkitPermissions.reset();
			}

			/*
			 * Start the scheduler for data saving.
			 */
			enableScheduler();

			/*
			 * Schedule a Bukkit Permissions update for 1 tick later.
			 * All plug-ins will be loaded by then
			 */

			if (getServer().getScheduler().scheduleSyncDelayedTask(this, new BukkitPermsUpdateTask(), 1) == -1) {
				GroupManager.logger.severe("Could not schedule superperms Update.");
				/*
				 * Flag that we are now loaded and should start processing events.
				 */
				setLoaded(true);
			}

			GroupManager.logger.info("version " + pdfFile.getVersion() + " is enabled!");

			// Register as a service
			if (!restarting)
				this.getServer().getServicesManager().register(WorldsHolder.class, getWorldsHolder(), this, ServicePriority.Lowest);
			
		} catch (Exception ex) {

			/*
			 * Store the error and write to the log.
			 */
			saveErrorLog(ex);

			/*
			 * Throw an error so Bukkit knows about it.
			 */
			throw new IllegalArgumentException(ex.getMessage(), ex);

		}
	}
	
	private void initCommands() {
		
		getCommand("listgroups").setExecutor(new ListGroups());
		getCommand("mancheckw").setExecutor(new ManCheckW());
		getCommand("manclear").setExecutor(new ManClear());
		getCommand("mandemote").setExecutor(new ManDemote());
		getCommand("mangadd").setExecutor(new ManGAdd());
		getCommand("mangaddi").setExecutor(new ManGAddI());
		getCommand("mangaddp").setExecutor(new ManGAddP());
		getCommand("mangaddv").setExecutor(new ManGAddV());
		getCommand("mangcheckp").setExecutor(new ManGCheckP());
		getCommand("mangcheckv").setExecutor(new ManGCheckV());
		getCommand("mangclearp").setExecutor(new ManGClearP());
		getCommand("mangdel").setExecutor(new ManGDel());
		getCommand("mangdeli").setExecutor(new ManGDelI());
		getCommand("mangdelp").setExecutor(new ManGDelP());
		getCommand("mangdelv").setExecutor(new ManGDelV());
		getCommand("manglistp").setExecutor(new ManGListP());
		getCommand("manglistv").setExecutor(new ManGListV());
		getCommand("manload").setExecutor(new ManLoad());
		getCommand("manpromote").setExecutor(new ManPromote());
		getCommand("mansave").setExecutor(new ManSave());
		getCommand("manselect").setExecutor(new ManSelect());
		getCommand("mantogglesave").setExecutor(new ManToggleSave());
		getCommand("mantogglevalidate").setExecutor(new ManToggleValidate());
		getCommand("manuadd").setExecutor(new ManUAdd());
		getCommand("manuaddp").setExecutor(new ManUAddP());
		getCommand("manuaddsub").setExecutor(new ManUAddSub());
		getCommand("manuaddv").setExecutor(new ManUAddV());
		getCommand("manucheckp").setExecutor(new ManUCheckP());
		getCommand("manucheckv").setExecutor(new ManUCheckV());
		getCommand("manuclearp").setExecutor(new ManUClearP());
		getCommand("manudel").setExecutor(new ManUDel());
		getCommand("manudelsub").setExecutor(new ManUDelSub());
		getCommand("manudelp").setExecutor(new ManUDelP());
		getCommand("manudelv").setExecutor(new ManUDelV());
		getCommand("manulistp").setExecutor(new ManUListP());
		getCommand("manulistv").setExecutor(new ManUListV());
		getCommand("manwhois").setExecutor(new ManWhois());
		getCommand("manworld").setExecutor(new ManWorld());
		getCommand("tempadd").setExecutor(new TempAdd());
		getCommand("tempdel").setExecutor(new TempDel());
		getCommand("tempdelall").setExecutor(new TempDelAll());
		getCommand("templist").setExecutor(new TempList());

	}

	/**
	 * Write an error.log
	 * 
	 * @param ex
	 */
	private void saveErrorLog(Exception ex) {

		if (!getDataFolder().exists()) {
			getDataFolder().mkdirs();
		}

		lastError = ex.getMessage();

		GroupManager.logger.severe("====================================================");
		GroupManager.logger.severe("= ERROR REPORT START - " + this.getDescription().getVersion() + " =");
		GroupManager.logger.severe("====================================================");
		GroupManager.logger.severe("=== PLEASE COPY AND PASTE THE ERROR.LOG FROM THE ===");
		GroupManager.logger.severe("= GROUPMANAGER FOLDER TO A GROUPMANAGER  DEVELOPER =");
		GroupManager.logger.severe("====================================================");
		GroupManager.logger.severe(lastError);
		GroupManager.logger.severe("====================================================");
		GroupManager.logger.severe("= ERROR REPORT ENDED =");
		GroupManager.logger.severe("====================================================");

		// Append this error to the error log.
		try {
			String error = "=============================== GM ERROR LOG ===============================\n";
			error += "= ERROR REPORT START - " + this.getDescription().getVersion() + " =\n\n";
			
			error += Tasks.getStackTraceAsString(ex);
			error += "\n============================================================================\n";

			Tasks.appendStringToFile(error, (getDataFolder() + System.getProperty("file.separator") + "ERROR.LOG"));
		} catch (IOException e) {
			// Failed to write file.
			e.printStackTrace();
		}

	}
	
	/**
	 * @return the validateOnlinePlayer
	 */
	public boolean isValidateOnlinePlayer() {

		return validateOnlinePlayer;
	}

	/**
	 * @param validateOnlinePlayer the validateOnlinePlayer to set
	 */
	public void setValidateOnlinePlayer(boolean validateOnlinePlayer) {

		this.validateOnlinePlayer = validateOnlinePlayer;
	}

	private void prepareBackupFolder() {

		backupFolder = new File(this.getDataFolder(), "backup");
		if (!backupFolder.exists()) {
			getBackupFolder().mkdirs();
		}
	}

	private void prepareConfig() {

		config = new GMConfiguration(this);
	}

	public void enableScheduler() {

		if (worldsHolder != null) {
			disableScheduler();
			/*
			 * Thread to handle saving data.
			 */
			commiter = new Runnable() {

				@Override
				public void run() {

					boolean active = false;
					
					if (isLoaded())
						try {
							if (lock.compareAndSet(false, true) && worldsHolder.saveChanges(false)) {
								active = true;
								GroupManager.logger.info("Data files refreshed.");
							}
						} catch (IllegalStateException ex) {
							GroupManager.logger.warning(ex.getMessage());
						} finally {
							if (active == true)
								lock.lazySet(false);
						}
				}
			};
			/*
			 * Thread for purging expired permissions.
			 */
			cleanup = new Runnable() {

				@Override
				public void run() {

					boolean active = false;
					
					if (isLoaded())
						try {
							/*
							 * If we removed any permissions and saving is not disabled
							 * update our data files so we are not updating perms
							 * every 60 seconds.
							 */
							if (lock.compareAndSet(false, true) && worldsHolder.purgeExpiredPerms()) {
								
								active = true;
								try {
									if (worldsHolder.saveChanges(false))
										GroupManager.logger.info("Data files refreshed.");
								} catch (IllegalStateException ex) {
									GroupManager.logger.warning(ex.getMessage());
								}
								
							}
						} catch (IllegalStateException ex) {
							GroupManager.logger.warning(ex.getMessage());
						} finally {
							/*
							 * Reset the lock if we set it.
							 */
							if (active == true)
								lock.lazySet(false);
						}
				}
			};
			
			scheduler = new ScheduledThreadPoolExecutor(2);
			long minutes = (long) getGMConfig().getSaveInterval();
			
			if (minutes > 0) {
				scheduler.scheduleAtFixedRate(commiter, minutes, minutes, TimeUnit.MINUTES);
				scheduler.scheduleAtFixedRate(cleanup, 0, 1, TimeUnit.MINUTES);
				GroupManager.logger.info("Scheduled Data Saving is set for every " + minutes + " minutes!");
			} else
				GroupManager.logger.warning("Scheduled Data Saving is Disabled!");
			
			GroupManager.logger.info("Backups will be retained for " + getGMConfig().getBackupDuration() + " hours!");
		}
	}

	public void disableScheduler() {

		if (scheduler != null) {
			try {
				scheduler.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
				scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
				scheduler.shutdown();
			} catch (Exception e) {
			}
			scheduler = null;
			GroupManager.logger.warning("Scheduled Data Saving is disabled!");
		}
	}

	public WorldsHolder getWorldsHolder() {

		return worldsHolder;
	}
	
	/**
	 * @return the overloadedUsers
	 */
	public static Map<String, ArrayList<User>> getOverloadedUsers() {
	
		return overloadedUsers;
	}
	
	/**
	 * Checks if a permission exists and of a lower or same priority.
	 */
	public boolean checkPermissionExists(CommandSender sender, String newPerm, PermissionCheckResult oldPerm, String type) {
		
		
		if (newPerm.startsWith("+"))
		{
			if (oldPerm.resultType.equals(PermissionCheckResult.Type.EXCEPTION))
			{
				sender.sendMessage(ChatColor.RED + "The " + type + " already has direct access to that permission.");
				sender.sendMessage(ChatColor.RED + "Node: " + oldPerm.accessLevel);
				return true;
			}
		}
		else if (newPerm.startsWith("-"))
		{
			if (oldPerm.resultType.equals(PermissionCheckResult.Type.EXCEPTION))
			{
				sender.sendMessage(ChatColor.RED + "The " + type + " already has an exception for this node.");
				sender.sendMessage(ChatColor.RED + "Node: " + oldPerm.accessLevel);
				return true;
			}
			else if (oldPerm.resultType.equals(PermissionCheckResult.Type.NEGATION))
			{
				sender.sendMessage(ChatColor.RED + "The " + type + " already has a matching negated node.");
				sender.sendMessage(ChatColor.RED + "Node: " + oldPerm.accessLevel);
				return true;
			}
		}
		else
		{
			if (oldPerm.resultType.equals(PermissionCheckResult.Type.EXCEPTION))
			{
				sender.sendMessage(ChatColor.RED + "The " + type + " already has an exception for this node.");
				sender.sendMessage(ChatColor.RED + "Node: " + oldPerm.accessLevel);
			}
			else if (oldPerm.resultType.equals(PermissionCheckResult.Type.NEGATION))
			{
				sender.sendMessage(ChatColor.RED + "The " + type + " already has a matching negated node.");
				sender.sendMessage(ChatColor.RED + "Node: " + oldPerm.accessLevel);
			}
			else if (oldPerm.resultType.equals(PermissionCheckResult.Type.FOUND))
			{
				sender.sendMessage(ChatColor.RED + "The " + type + " already has direct access to that permission.");
				sender.sendMessage(ChatColor.RED + "Node: " + oldPerm.accessLevel);
				
				// Since not all plugins define wildcard permissions, allow setting the permission anyway if the permissions don't match exactly.
				return (oldPerm.accessLevel.equalsIgnoreCase(newPerm));
			}
		}
		return false;
	}
	
	/**
	 * @return the selectedWorlds
	 */
	public static Map<String, String> getSelectedWorlds() {
	
		return selectedWorlds;
	}

	/**
	 * Send confirmation of a group change. using permission nodes...
	 * 
	 * groupmanager.notify.self groupmanager.notify.other
	 * 
	 * @param name
	 * @param msg
	 */
	public static void notify(String name, String msg) {

		Player player = Bukkit.getServer().getPlayerExact(name);

		for (Player test : Bukkit.getServer().getOnlinePlayers()) {
			if (!test.equals(player)) {
				if (test.hasPermission("groupmanager.notify.other"))
					test.sendMessage(ChatColor.YELLOW + name + " was" + msg);
			} else if ((player != null) && ((player.hasPermission("groupmanager.notify.self")) || (player.hasPermission("groupmanager.notify.other"))))
				player.sendMessage(ChatColor.YELLOW + "You were" + msg);
		}

	}

	/**
	 * @return the config
	 */
	public static GMConfiguration getGMConfig() {

		return config;
	}

	/**
	 * @return the backupFolder
	 */
	public File getBackupFolder() {

		return backupFolder;
	}

	public static GlobalGroups getGlobalGroups() {

		return globalGroups;

	}
	
	/**
	 * Is the plug-in fully loaded?
	 * 
	 * @return	true if we are loaded or false if reloading/loading.
	 */
	public static boolean isLoaded() {

		return isLoaded;
	}
	
	public static void setLoaded(boolean isLoaded) {

		GroupManager.isLoaded = isLoaded;
	}
	
	/**
	 * @return true if the scheduler is running.
	 */
	public boolean isSchedulerRunning() {
	
		return scheduler != null;
	}

	/**
	 * @return the lastError
	 */
	public String getLastError() {
	
		return lastError;
	}
	
	/**
	 * @param lastError the lastError to set
	 */
	public void setLastError(String lastError) {
	
		this.lastError = lastError;
	}
	
	/**
	 * @return the bukkitPermissions
	 */
	public static BukkitPermissions getBukkitPermissions() {
	
		return BukkitPermissions;
	}

	public static GroupManagerEventHandler getGMEventHandler() {

		return GMEventHandler;
	}

	public static void setGMEventHandler(GroupManagerEventHandler gMEventHandler) {

		GMEventHandler = gMEventHandler;
	}

}
