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
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.anjocaido.groupmanager.Tasks.BukkitPermsUpdateTask;
import org.anjocaido.groupmanager.Tasks.UpdateTask;
import org.anjocaido.groupmanager.commands.ManGList;
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
import org.anjocaido.groupmanager.localization.Messages;
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
	private ReentrantLock saveLock = new ReentrantLock();
	
	private String lastError = ""; //$NON-NLS-1$

	private static GlobalGroups globalGroups;

	private GMLoggerHandler ch;
	
	private static GroupManagerEventHandler GMEventHandler;
	public static BukkitPermissions BukkitPermissions;
	private GMWorldListener WorldEvents;
	public static final Logger logger = Logger.getLogger(GroupManager.class.getName());


	@Override
	public void onDisable() {
		
		onDisable(false);
	}
	
	@Override
	public void onEnable() {
		
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
		GroupManager.logger.warning(String.format(Messages.getString("GroupManager.DISABLED"), pdfFile.getVersion())); //$NON-NLS-1$

		
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
			lastError = ""; //$NON-NLS-1$
			
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
				GroupManager.logger.severe(String.format(Messages.getString("GroupManager.CANT_ENABLE"), pdfFile.getName(), pdfFile.getVersion())); //$NON-NLS-1$
				this.getServer().getPluginManager().disablePlugin(this);
				throw new IllegalStateException(Messages.getString("GroupManager.ERROR_LOADING")); //$NON-NLS-1$
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
				GroupManager.logger.severe(Messages.getString("GroupManager.ERROR_SCHEDULING_SUPERPERMS")); //$NON-NLS-1$
				/*
				 * Flag that we are now loaded and should start processing events.
				 */
				setLoaded(true);
			}

			GroupManager.logger.info(String.format(Messages.getString("GroupManager.ENABLED"), pdfFile.getVersion())); //$NON-NLS-1$

			// Register as a service
			if (!restarting)
				this.getServer().getServicesManager().register(WorldsHolder.class, getWorldsHolder(), this, ServicePriority.Lowest);
			
			/*
			 * Version check.
			 */
			this.getServer().getScheduler().runTaskLaterAsynchronously(this, new UpdateTask(pdfFile.getVersion()), 1L);
			
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
		
		if (!restarting) {
			
			/*
			 * Register Metrics
			 */
			try {
				Metrics metrics = new Metrics(this, 7982);
			    
			    metrics.addCustomChart(new Metrics.SimplePie("language", () -> GroupManager.getGMConfig().getLanguage()));
			} catch (Exception e) {
				System.err.println("[GroupManager] Error setting up metrics"); //$NON-NLS-1$
			}
		}
	}
	
	private void initCommands() {
		
		getCommand("mancheckw").setExecutor(new ManCheckW()); //$NON-NLS-1$
		getCommand("manclear").setExecutor(new ManClear()); //$NON-NLS-1$
		getCommand("mandemote").setExecutor(new ManDemote()); //$NON-NLS-1$
		getCommand("mangadd").setExecutor(new ManGAdd()); //$NON-NLS-1$
		getCommand("mangaddi").setExecutor(new ManGAddI()); //$NON-NLS-1$
		getCommand("mangaddp").setExecutor(new ManGAddP()); //$NON-NLS-1$
		getCommand("mangaddv").setExecutor(new ManGAddV()); //$NON-NLS-1$
		getCommand("mangcheckp").setExecutor(new ManGCheckP()); //$NON-NLS-1$
		getCommand("mangcheckv").setExecutor(new ManGCheckV()); //$NON-NLS-1$
		getCommand("mangclearp").setExecutor(new ManGClearP()); //$NON-NLS-1$
		getCommand("mangdel").setExecutor(new ManGDel()); //$NON-NLS-1$
		getCommand("mangdeli").setExecutor(new ManGDelI()); //$NON-NLS-1$
		getCommand("mangdelp").setExecutor(new ManGDelP()); //$NON-NLS-1$
		getCommand("mangdelv").setExecutor(new ManGDelV()); //$NON-NLS-1$
		getCommand("manglist").setExecutor(new ManGList()); //$NON-NLS-1$
		getCommand("manglistp").setExecutor(new ManGListP()); //$NON-NLS-1$
		getCommand("manglistv").setExecutor(new ManGListV()); //$NON-NLS-1$
		getCommand("manload").setExecutor(new ManLoad()); //$NON-NLS-1$
		getCommand("manpromote").setExecutor(new ManPromote()); //$NON-NLS-1$
		getCommand("mansave").setExecutor(new ManSave()); //$NON-NLS-1$
		getCommand("manselect").setExecutor(new ManSelect()); //$NON-NLS-1$
		getCommand("mantogglesave").setExecutor(new ManToggleSave()); //$NON-NLS-1$
		getCommand("mantogglevalidate").setExecutor(new ManToggleValidate()); //$NON-NLS-1$
		getCommand("manuadd").setExecutor(new ManUAdd()); //$NON-NLS-1$
		getCommand("manuaddp").setExecutor(new ManUAddP()); //$NON-NLS-1$
		getCommand("manuaddsub").setExecutor(new ManUAddSub()); //$NON-NLS-1$
		getCommand("manuaddv").setExecutor(new ManUAddV()); //$NON-NLS-1$
		getCommand("manucheckp").setExecutor(new ManUCheckP()); //$NON-NLS-1$
		getCommand("manucheckv").setExecutor(new ManUCheckV()); //$NON-NLS-1$
		getCommand("manuclearp").setExecutor(new ManUClearP()); //$NON-NLS-1$
		getCommand("manudel").setExecutor(new ManUDel()); //$NON-NLS-1$
		getCommand("manudelsub").setExecutor(new ManUDelSub()); //$NON-NLS-1$
		getCommand("manudelp").setExecutor(new ManUDelP()); //$NON-NLS-1$
		getCommand("manudelv").setExecutor(new ManUDelV()); //$NON-NLS-1$
		getCommand("manulistp").setExecutor(new ManUListP()); //$NON-NLS-1$
		getCommand("manulistv").setExecutor(new ManUListV()); //$NON-NLS-1$
		getCommand("manwhois").setExecutor(new ManWhois()); //$NON-NLS-1$
		getCommand("manworld").setExecutor(new ManWorld()); //$NON-NLS-1$
		getCommand("tempadd").setExecutor(new TempAdd()); //$NON-NLS-1$
		getCommand("tempdel").setExecutor(new TempDel()); //$NON-NLS-1$
		getCommand("tempdelall").setExecutor(new TempDelAll()); //$NON-NLS-1$
		getCommand("templist").setExecutor(new TempList()); //$NON-NLS-1$

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

		GroupManager.logger.severe("===================================================="); //$NON-NLS-1$
		GroupManager.logger.severe(String.format("= ERROR REPORT START - %s =", this.getDescription().getVersion())); //$NON-NLS-1$
		GroupManager.logger.severe("===================================================="); //$NON-NLS-1$
		GroupManager.logger.severe("=== PLEASE COPY AND PASTE THE ERROR.LOG FROM THE ==="); //$NON-NLS-1$
		GroupManager.logger.severe("= GROUPMANAGER FOLDER TO A GROUPMANAGER  DEVELOPER ="); //$NON-NLS-1$
		GroupManager.logger.severe("===================================================="); //$NON-NLS-1$
		GroupManager.logger.severe(lastError);
		GroupManager.logger.severe("===================================================="); //$NON-NLS-1$
		GroupManager.logger.severe("= ERROR REPORT ENDED ="); //$NON-NLS-1$
		GroupManager.logger.severe("===================================================="); //$NON-NLS-1$

		// Append this error to the error log.
		try {
			String error = "=============================== GM ERROR LOG ===============================\n"; //$NON-NLS-1$
			error += String.format("= ERROR REPORT START - %s =\n\n", this.getDescription().getVersion()); //$NON-NLS-1$
			
			error += Tasks.getStackTraceAsString(ex);
			error += "\n============================================================================\n"; //$NON-NLS-1$

			Tasks.appendStringToFile(error, (getDataFolder() + System.getProperty("file.separator") + "ERROR.LOG")); //$NON-NLS-1$ //$NON-NLS-2$
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

		backupFolder = new File(this.getDataFolder(), "backup"); //$NON-NLS-1$
		if (!backupFolder.exists()) {
			getBackupFolder().mkdirs();
		}
	}

	private void prepareConfig() {

		config = new GMConfiguration(this);
		config.load();
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
					
					if (isLoaded())
						
						try {
							// obtain a lock so we are the only thread saving (blocking).
							getSaveLock().lock();
							
							if (worldsHolder.saveChanges(false)) {
								
								GroupManager.logger.info(Messages.getString("GroupManager.REFRESHED")); //$NON-NLS-1$
							}
						} catch (IllegalStateException ex) {
							GroupManager.logger.warning(ex.getMessage());
						} finally {
							/*
							 * Release the lock.
							 */
							getSaveLock().unlock();
						}
				}
			};
			/*
			 * Thread for purging expired permissions.
			 */
			cleanup = new Runnable() {

				@Override
				public void run() {
					
					if (isLoaded())
						try {
							// obtain a lock so we are the only thread saving (blocking).
							getSaveLock().lock();
							
							/*
							 * If we removed any permissions and saving is not disabled
							 * update our data files so we are not updating perms
							 * every 60 seconds.
							 */
							if (worldsHolder.purgeExpiredPerms()) {
								
								if (worldsHolder.saveChanges(false))
										GroupManager.logger.info(Messages.getString("GroupManager.REFRESHED")); //$NON-NLS-1$
								
							}
						} catch (IllegalStateException ex) {
							GroupManager.logger.warning(ex.getMessage());
						} finally {
							/*
							 * Release the lock.
							 */
							getSaveLock().unlock();
						}
				}
			};
			
			scheduler = new ScheduledThreadPoolExecutor(2);
			long minutes = (long) getGMConfig().getSaveInterval();
			
			if (minutes > 0) {
				scheduler.scheduleAtFixedRate(commiter, minutes, minutes, TimeUnit.MINUTES);
				scheduler.scheduleAtFixedRate(cleanup, 0, 1, TimeUnit.MINUTES);
				GroupManager.logger.info(String.format(Messages.getString("GroupManager.SCHEDULED_DATA_SAVING_SET"), minutes)); //$NON-NLS-1$
			} else
				GroupManager.logger.warning(Messages.getString("GroupManager.SCHEDULED_DATA_SAVING_DISABLED")); //$NON-NLS-1$
			
			GroupManager.logger.info(String.format(Messages.getString("GroupManager.BACKUPS_RETAINED_MSG"), getGMConfig().getBackupDuration())); //$NON-NLS-1$
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
			GroupManager.logger.warning(Messages.getString("GroupManager.SCHEDULED_DATA_SAVING_DISABLED")); //$NON-NLS-1$
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
		
		
		if (newPerm.startsWith("+")) //$NON-NLS-1$
		{
			if (oldPerm.resultType.equals(PermissionCheckResult.Type.EXCEPTION))
			{
				sender.sendMessage(ChatColor.RED + String.format(Messages.getString("GroupManager.PERMISSION_DIRECT_ACCESS"), type)); //$NON-NLS-1$
				sender.sendMessage(ChatColor.RED + Messages.getString("GroupManager.PERMISSION_NODE") + oldPerm.accessLevel); //$NON-NLS-1$
				return true;
			}
		}
		else if (newPerm.startsWith("-")) //$NON-NLS-1$
		{
			if (oldPerm.resultType.equals(PermissionCheckResult.Type.EXCEPTION))
			{
				sender.sendMessage(ChatColor.RED + String.format(Messages.getString("GroupManager.PERMISSION_EXCEPTION"), type)); //$NON-NLS-1$
				sender.sendMessage(ChatColor.RED + Messages.getString("GroupManager.PERMISSION_NODE") + oldPerm.accessLevel); //$NON-NLS-1$
				return true;
			}
			else if (oldPerm.resultType.equals(PermissionCheckResult.Type.NEGATION))
			{
				sender.sendMessage(ChatColor.RED + String.format(Messages.getString("GroupManager.PERMISSION_MATCHING_NEGATION"), type)); //$NON-NLS-1$
				sender.sendMessage(ChatColor.RED + Messages.getString("GroupManager.PERMISSION_NODE") + oldPerm.accessLevel); //$NON-NLS-1$
				return true;
			}
		}
		else
		{
			if (oldPerm.resultType.equals(PermissionCheckResult.Type.EXCEPTION))
			{
				sender.sendMessage(ChatColor.RED + String.format(Messages.getString("GroupManager.PERMISSION_EXCEPTION_ALREADY"), type)); //$NON-NLS-1$
				sender.sendMessage(ChatColor.RED + Messages.getString("GroupManager.PERMISSION_NODE") + oldPerm.accessLevel); //$NON-NLS-1$
			}
			else if (oldPerm.resultType.equals(PermissionCheckResult.Type.NEGATION))
			{
				sender.sendMessage(ChatColor.RED + String.format(Messages.getString("GroupManager.PERMISSION_MATCHING_NEGATION_ALREADY"), type)); //$NON-NLS-1$
				sender.sendMessage(ChatColor.RED + Messages.getString("GroupManager.PERMISSION_NODE") + oldPerm.accessLevel); //$NON-NLS-1$
			}
			else if (oldPerm.resultType.equals(PermissionCheckResult.Type.FOUND))
			{
				sender.sendMessage(ChatColor.RED + String.format(Messages.getString("GroupManager.PERMISSION_DIRECT_ACCESS_ALREADY"), type)); //$NON-NLS-1$
				sender.sendMessage(ChatColor.RED + Messages.getString("GroupManager.PERMISSION_NODE") + oldPerm.accessLevel); //$NON-NLS-1$
				
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
				if (test.hasPermission("groupmanager.notify.other")) { //$NON-NLS-1$
					/*
					 * Notify others (with the permission node
					 * 'groupmanager.notify.other') of a group change.
					 * 
					 * %s was moved to the group %s in world %s.
					 */
					test.sendMessage(ChatColor.YELLOW + name + Messages.getString("PLAYER_WAS") + msg); //$NON-NLS-1$
				}
			} else if ((player != null) && ((player.hasPermission("groupmanager.notify.self")) || (player.hasPermission("groupmanager.notify.other")))) { //$NON-NLS-1$ //$NON-NLS-2$
				/*
				 * Notify the player (if they have the permission node
				 * 'groupmanager.notify.self') of their group change.
				 * 
				 * You were moved to the group %s in world %s.
				 */
				player.sendMessage(ChatColor.YELLOW + Messages.getString("YOU_WERE") + msg); //$NON-NLS-1$
			}
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
	 * @return the saveLock
	 */
	public ReentrantLock getSaveLock() {
	
		return saveLock;
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
