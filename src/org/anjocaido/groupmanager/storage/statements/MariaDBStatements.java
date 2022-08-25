/*
 * 
 */
package org.anjocaido.groupmanager.storage.statements;

import org.anjocaido.groupmanager.GroupManager;

/**
 * @author ElgarL
 *
 */
public class MariaDBStatements extends Statements {

	public MariaDBStatements() {
		
		DRIVER = "com.mariadb.jdbc.Driver"; //com.palmergames.libs.com.mysql.cj.jdbc.Driver
		URL = "jdbc:mysql://" + GroupManager.getGMConfig().getHostname() + ":" + GroupManager.getGMConfig().getPort() + "/" + GroupManager.getGMConfig().getDatabaseName() + "?characterEncoding=utf8";
		
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
				+ " ISDEFAULT BOOLEAN DEFAULT FALSE,"
				+ " PERMISSIONS TEXT DEFAULT NULL,"
				+ " INHERITANCE TEXT DEFAULT NULL,"
				+ " INFO TEXT DEFAULT NULL"
				+ " );";
		
		CREATE_GLOBALGROUP_TABLE = "CREATE TABLE IF NOT EXISTS %s ("
				+ " NAME VARCHAR(32) PRIMARY KEY,"
				+ " PERMISSIONS TEXT DEFAULT NULL"
				+ " );";

		INSERT_REPLACE_UPDATE = "REPLACE INTO %s ("
				+ " TABLE_NAME,UPDATED"
				+ " ) VALUES(?,?);";

		INSERT_REPLACE_USER = "REPLACE INTO %s ("
				+ " UUID,LASTNAME,PRIMARYGROUP,SUBGROUPS,PERMISSIONS,INFO"
				+ " ) VALUES(?,?,?,?,?,?);";
		
		INSERT_REPLACE_GROUP = "REPLACE INTO %s ("
				+ " NAME,ISDEFAULT,PERMISSIONS,INHERITANCE,INFO"
				+ " ) VALUES(?,?,?,?,?);";
		
		INSERT_REPLACE_GLOBALGROUP = "REPLACE INTO %s ("
				+ " NAME,PERMISSIONS"
				+ " ) VALUES(?,?);";

		SELECT_TIMESTAMP = "SELECT UPDATED FROM %s WHERE TABLE_NAME = '%s';";
		SELECT_ALL = "SELECT * FROM %s;";
		SELECT_IS_EMPTY = "SELECT count(*) FROM (select 1 from %s limit 1) AS alias;";
	}
}
