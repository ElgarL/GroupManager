/*
 * 
 */
package org.anjocaido.groupmanager.storage.statements;

import java.io.File;

import org.anjocaido.groupmanager.GroupManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author ElgarL
 *
 */
public class H2Statements extends Statements {

	public H2Statements() {
		
		DRIVER = "org.h2.Driver"; //com.palmergames.libs.org.h2.Driver
		URL = "jdbc:h2:" + JavaPlugin.getPlugin(GroupManager.class).getDataFolder().getAbsolutePath() + File.separator + GroupManager.getGMConfig().getDatabaseName();
		
		CREATE_UPDATE_TABLE = "CREATE TABLE IF NOT EXISTS %s ("
				+ " TABLE_NAME VARCHAR(255) PRIMARY KEY,"
				+ " UPDATED BIGINT NOT NULL"
				+ " );";

		CREATE_USER_TABLE = "CREATE TABLE IF NOT EXISTS %s ("
				+ " UUID VARCHAR(36) PRIMARY KEY,"
				+ " LASTNAME VARCHAR(16) DEFAULT NULL,"
				+ " PRIMARYGROUP VARCHAR(32) NOT NULL,"
				+ " SUBGROUPS TEXT DEFAULT NULL,"
				+ " PERMISSIONS TEXT DEFAULT NULL,"
				+ " INFO TEXT DEFAULT NULL"
				+ " );";

		CREATE_GROUP_TABLE = "CREATE TABLE IF NOT EXISTS %s ("
				+ " NAME VARCHAR(32) PRIMARY KEY,"
				+ " ISDEFAULT INTEGER DEFAULT 0,"
				+ " PERMISSIONS TEXT DEFAULT NULL,"
				+ " INHERITANCE TEXT DEFAULT NULL,"
				+ " INFO TEXT DEFAULT NULL"
				+ " );";
		
		CREATE_GLOBALGROUP_TABLE = "CREATE TABLE IF NOT EXISTS %s ("
				+ " NAME VARCHAR(32) PRIMARY KEY,"
				+ " PERMISSIONS TEXT DEFAULT NULL"
				+ " );";

		INSERT_REPLACE_UPDATE = "MERGE INTO %s ("
				+ " TABLE_NAME,UPDATED"
				+ " ) VALUES(?,?);";

		INSERT_REPLACE_USER = "MERGE INTO %s ("
				+ " UUID,LASTNAME,PRIMARYGROUP,SUBGROUPS,PERMISSIONS,INFO"
				+ " ) VALUES(?,?,?,?,?,?);";
		
		INSERT_REPLACE_GROUP = "MERGE INTO %s ("
				+ " NAME,ISDEFAULT,PERMISSIONS,INHERITANCE,INFO"
				+ " ) VALUES(?,?,?,?,?);";
		
		INSERT_REPLACE_GLOBALGROUP = "MERGE INTO %s ("
				+ " NAME,PERMISSIONS"
				+ " ) VALUES(?,?);";

		SELECT_TIMESTAMP = "SELECT UPDATED FROM %s WHERE TABLE_NAME = '%s';";
		SELECT_ALL = "SELECT * FROM %s;";
		SELECT_IS_EMPTY = "SELECT count(*) FROM (select 1 from %s limit 1);";
	}
}
