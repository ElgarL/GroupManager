package org.anjocaido.groupmanager.storage.hikari;

import java.sql.Connection;
import java.sql.SQLException;

import org.anjocaido.groupmanager.GroupManager;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class HikariCPDataSource {

	private HikariConfig config = new HikariConfig();
	private HikariDataSource ds;

	public Connection getConnection() throws SQLException {

		return ds.getConnection();
	}

	/**
	 * Throw any exceptions as we want to prevent
	 * the plugin loading if the database fails.
	 * 
	 * @throws Exception
	 */
	public HikariCPDataSource(String driver, String url) throws Exception {

		// ensure the driver is on the ClassPath.
		Class.forName(driver);

		config.setJdbcUrl(url);
		config.setUsername(GroupManager.getGMConfig().getDatabaseUsername());
		config.setPassword(GroupManager.getGMConfig().getDatabasePassword());
		config.addDataSourceProperty("cachePrepStmts", "true");
		config.addDataSourceProperty("prepStmtCacheSize", "250");
		config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
		config.addDataSourceProperty("maximum-pool-size", "100");
		ds = new HikariDataSource(config);
	}
}
