/*
 * 
 */
package org.anjocaido.groupmanager.storage.statements;


/**
 * @author ElgarL
 *
 */
public abstract class Statements {

	public String DRIVER;
	public String URL;
	
	public String CREATE_UPDATE_TABLE;
	public String CREATE_USER_TABLE;
	public String CREATE_GROUP_TABLE;
	public String CREATE_GLOBALGROUP_TABLE;

	public String INSERT_REPLACE_UPDATE;
	public String INSERT_REPLACE_USER;
	public String INSERT_REPLACE_GROUP;
	public String INSERT_REPLACE_GLOBALGROUP;

	public String SELECT_TIMESTAMP;
	public String SELECT_ALL;
	public String SELECT_IS_EMPTY;
}
