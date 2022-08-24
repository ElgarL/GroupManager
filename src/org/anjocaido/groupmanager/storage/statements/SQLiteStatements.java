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
public class SQLiteStatements extends Statements {

	public SQLiteStatements () {
		
		DRIVER = "org.sqlite.JDBC";
		URL = "jdbc:sqlite:" + JavaPlugin.getPlugin(GroupManager.class).getDataFolder().getAbsolutePath() + File.separator + GroupManager.getGMConfig().getDatabaseName();
		
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

		INSERT_REPLACE_UPDATE = "INSERT INTO %s ("
				+ " TABLE_NAME,UPDATED"
				+ " ) VALUES(?,?)"
				+ " ON CONFLICT(TABLE_NAME) DO UPDATE SET"
				+ " UPDATED = excluded.UPDATED;";

		INSERT_REPLACE_USER = "INSERT INTO %s ("
				+ " UUID,LASTNAME,PRIMARYGROUP,SUBGROUPS,PERMISSIONS,INFO"
				+ " ) VALUES(?,?,?,?,?,?)"
				+ " ON CONFLICT(UUID) DO UPDATE SET"
				+ " LASTNAME = excluded.LASTNAME,"
				+ " PRIMARYGROUP = excluded.PRIMARYGROUP,"
				+ " SUBGROUPS = excluded.SUBGROUPS,"
				+ " PERMISSIONS = excluded.PERMISSIONS,"
				+ " INFO = excluded.INFO;";
		
		INSERT_REPLACE_GROUP = "INSERT INTO %s ("
				+ " NAME,ISDEFAULT,PERMISSIONS,INHERITANCE,INFO"
				+ " ) VALUES(?,?,?,?,?)"
				+ " ON CONFLICT(NAME) DO UPDATE SET"
				+ " ISDEFAULT = excluded.ISDEFAULT,"
				+ " PERMISSIONS = excluded.PERMISSIONS,"
				+ " INHERITANCE = excluded.INHERITANCE,"
				+ " INFO = excluded.INFO;";
		
		INSERT_REPLACE_GLOBALGROUP = "INSERT INTO %s ("
				+ " NAME,PERMISSIONS"
				+ " ) VALUES(?,?)"
				+ " ON CONFLICT(NAME) DO UPDATE SET"
				+ " PERMISSIONS = excluded.PERMISSIONS;";

		SELECT_TIMESTAMP = "SELECT UPDATED FROM %s WHERE TABLE_NAME = '%s';";
		SELECT_ALL = "SELECT * FROM %s;";
		SELECT_IS_EMPTY = "SELECT count(*) FROM (select 1 from %s limit 1);";
	}
}
