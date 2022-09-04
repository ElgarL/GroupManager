/*
 * 
 */
package org.anjocaido.groupmanager.storage.statements;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.storage.CoreSQL;
import org.anjocaido.groupmanager.storage.CoreYaml;
import org.anjocaido.groupmanager.storage.DataSource;
import org.anjocaido.groupmanager.utils.Tasks;

/**
 * @author ElgarL
 *
 */
public class Statements {

	private Properties prop = new Properties();
	private GroupManager plugin;

	public Statements(GroupManager plugin) throws Exception {

		this.plugin = plugin;
		saveSupportedSQL(this.plugin);
		loadData();
	}

	private void loadData() throws IOException {

		File sqlFolder = new File(plugin.getDataFolder(), "SQL");
		File source = new File(sqlFolder, GroupManager.getGMConfig().getDatabaseType() + ".properties");

		if (!source.isFile())
			throw new IOException(source.getName() + " is not a File.");

		try (InputStream input = new FileInputStream(source)) {

			// load a properties file
			prop.load(new InputStreamReader(input, StandardCharsets.UTF_8));
		}

	}

	public static DataSource getSource(GroupManager plugin) throws Exception {

		switch (GroupManager.getGMConfig().getDatabaseType()) {

		case "YAML":
			return new CoreYaml(plugin);

		default:
			return new CoreSQL(plugin, new Statements(plugin));
		}
	}

	private void saveSupportedSQL(GroupManager plugin) {

		File sqlFolder = new File(plugin.getDataFolder(), "SQL");

		if (!sqlFolder.exists()) {
			sqlFolder.mkdirs();
		}

		Tasks.saveResource(plugin, new File(sqlFolder, "H2.properties"));
		Tasks.saveResource(plugin, new File(sqlFolder, "MARIADB.properties"));
		Tasks.saveResource(plugin, new File(sqlFolder, "MYSQL.properties"));
		Tasks.saveResource(plugin, new File(sqlFolder, "POSTGRESQL.properties"));
		Tasks.saveResource(plugin, new File(sqlFolder, "SQLITE.properties"));

	}

	/**
	 * @return the DRIVER
	 */
	public String getDriver() {

		return prop.getProperty("DRIVER");
	}

	/**
	 * @return the URL
	 */
	public String getURL() {

		String URL = prop.getProperty("URL");

		URL = URL.replace("%DataFolderPATH%", this.plugin.getDataFolder().getAbsolutePath());
		URL = URL.replace("%FileSeperator%", File.separator);
		URL = URL.replace("%DatabaseName%", GroupManager.getGMConfig().getDatabaseName());
		URL = URL.replace("%HostName%", GroupManager.getGMConfig().getHostname());
		URL = URL.replace("%Port%", String.valueOf(GroupManager.getGMConfig().getPort()));

		return URL;
	}

	/**
	 * @return the CREATE_UPDATE_TABLE
	 */
	public String getCreateUpdateTable() {

		return prop.getProperty("CREATE_UPDATE_TABLE");
	}

	/**
	 * @return the CREATE_USER_TABLE
	 */
	public String getCreateUserTable() {

		return prop.getProperty("CREATE_USER_TABLE");
	}

	/**
	 * @return the CREATE_GROUP_TABLE
	 */
	public String getCreateGroupTable() {

		return prop.getProperty("CREATE_GROUP_TABLE");
	}

	/**
	 * @return the CREATE_GLOBALGROUP_TABLE
	 */
	public String getCreateGlobalGroupTable() {

		return prop.getProperty("CREATE_GLOBALGROUP_TABLE");
	}

	/**
	 * @return the INSERT_REPLACE_UPDATE
	 */
	public String getInsertReplaceUpdate() {

		return prop.getProperty("INSERT_REPLACE_UPDATE");
	}

	/**
	 * @return the INSERT_REPLACE_USER
	 */
	public String getInsertReplaceUser() {

		return prop.getProperty("INSERT_REPLACE_USER");
	}

	/**
	 * @return the INSERT_REPLACE_GROUP
	 */
	public String getInsertReplaceGroup() {

		return prop.getProperty("INSERT_REPLACE_GROUP");
	}

	/**
	 * @return the INSERT_REPLACE_GLOBALGROUP
	 */
	public String getInsertReplaceGlobalGroup() {

		return prop.getProperty("INSERT_REPLACE_GLOBALGROUP");
	}

	/**
	 * @return the SELECT_TIMESTAMP
	 */
	public String getSelectTimeStamp() {

		return prop.getProperty("SELECT_TIMESTAMP");
	}

	/**
	 * @return the SELECT_ALL
	 */
	public String getSelectAll() {

		return prop.getProperty("SELECT_ALL");
	}

	/**
	 * @return the SELECT_IS_EMPTY
	 */
	public String getSelectIsEmpty() {

		return prop.getProperty("SELECT_IS_EMPTY");
	}
}
