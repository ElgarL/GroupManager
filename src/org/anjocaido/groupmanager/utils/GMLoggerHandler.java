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
package org.anjocaido.groupmanager.utils;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * 
 * @author gabrielcouto
 */
public class GMLoggerHandler extends ConsoleHandler {

	@Deprecated // Use GroupManager.logger.log(Level level, String msg)
	@Override
	public void publish(LogRecord record) {
		String message = "[GroupManager] " + record.getMessage();
		if (record.getLevel().equals(Level.SEVERE) || record.getLevel().equals(Level.WARNING)) {
			System.err.println(message);
		} else {
			System.out.println(message);
		}
	}
}
