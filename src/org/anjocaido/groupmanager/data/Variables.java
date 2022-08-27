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
package org.anjocaido.groupmanager.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A class that holds variables of a user/group.
 * In groups, it holds the contents of INFO node.
 * Like:
 * prefix
 * suffix
 * build
 * 
 * @author gabrielcouto, ElgarL
 */
public class Variables implements Cloneable {

	private final DataUnit owner;
	protected final Map<String, Object> variables = Collections.synchronizedMap(new HashMap<>());

	Variables(DataUnit owner) {

		this.owner = owner;
	}
	
	public Variables(DataUnit owner, Map<? extends String, ?> varList) {

		this.owner = owner;
		this.variables.clear();
		this.variables.putAll(varList);
	}

	/**
	 * A clone of all vars here.
	 * 
	 * @return Variables clone
	 */
	protected Variables clone(DataUnit newOwner) {

		Variables clone = new Variables(newOwner);
		synchronized(variables) {
			for (String key : variables.keySet()) {
				clone.variables.put(key, variables.get(key));
			}
		}
		newOwner.flagAsChanged();
		return clone;
	}
	
	/**
	 * Get data as a Blob (csv String) for storing.
	 * 
	 * @return
	 */
	public String getBlob() {
		
		StringBuilder builder = new StringBuilder();
		
		for (Entry<String, Object> entry : variables.entrySet()) {
			builder.append(entry.getKey()).append("|").append(entry.getValue());
			builder.append(",");
		}
		
		return builder.length() == 0 ? new String() : builder.substring(0, builder.lastIndexOf(","));
	}

	/**
	 * Add var to the the INFO node.
	 * examples:
	 * addVar("build",true);
	 * addVar("prefix","c");
	 * 
	 * @param name key name of the var
	 * @param o the object value of the var
	 */
	public void addVar(String name, Object o) {

		if (o == null) return;
		variables.remove(name);
		variables.put(name, o);
		owner.flagAsChanged();
	}

	/**
	 * Returns the object inside the var
	 * 
	 * @param name
	 * @return a Object if exists. null if doesn't exists
	 */
	public Object getVarObject(String name) {

		return variables.get(name);
	}

	/**
	 * Get the String value for the given var name
	 * 
	 * @param name the var key name
	 * @return "" if null. or the toString() value of object
	 */
	public String getVarString(String name) {

		Object o = variables.get(name);
		try {
			return o == null ? "" : o.toString();
		} catch (Exception e) {
			return "";
		}
	}

	/**
	 * 
	 * @param name
	 * @return false if null. or a Boolean.parseBoolean of the string
	 */
	public Boolean getVarBoolean(String name) {

		Object o = variables.get(name);
		try {
			return o != null && Boolean.parseBoolean(o.toString());
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 
	 * @param name
	 * @return -1 if null or if the string cannot be parsed. or a parseInt of the string
	 */
	public Integer getVarInteger(String name) {

		Object o = variables.get(name);
		try {
			return o == null ? -1 : Integer.parseInt(o.toString());
		} catch (Exception e) {
			return -1;
		}
	}

	/**
	 * 
	 * @param name
	 * @return -1 if null or if the string cannot be parsed. or a parseDouble of the string
	 */
	public Double getVarDouble(String name) {

		Object o = variables.get(name);
		try {
			return o == null ? -1.0D : Double.parseDouble(o.toString());
		} catch (Exception e) {
			return -1.0D;
		}
	}

	/**
	 * All variable keys this is holding
	 * 
	 * @return Set of all variable names.
	 */
	public String[] getVarKeyList() {
		synchronized(variables) {
			return variables.keySet().toArray(new String[0]);
		}
	}

	/**
	 * verify is a var exists
	 * 
	 * @param name the key name of the var
	 * @return true if that var exists
	 */
	public boolean hasVar(String name) {

		return variables.containsKey(name);
	}

	/**
	 * Returns the quantity of vars this is holding
	 * 
	 * @return the number of vars
	 */
	public int getSize() {

		return variables.size();
	}

	/**
	 * Remove a var from the list
	 * 
	 * @param name
	 */
	public void removeVar(String name) {

		try {
			variables.remove(name);
		} catch (Exception ignored) {
		}
		owner.flagAsChanged();
	}

	public static Object parseVariableValue(String value) {

		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException ignored) {}
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException ignored) {}
		if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("on")) {
			return true;
		} else if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no") || value.equalsIgnoreCase("off")) {
			return false;
		}
		return value;

	}

	void clearVars() {

		variables.clear();
		owner.flagAsChanged();
	}

	/**
	 * @return the owner
	 */
	public DataUnit getOwner() {

		return owner;
	}

	public boolean isEmpty() {

		return variables.isEmpty();
	}
}
