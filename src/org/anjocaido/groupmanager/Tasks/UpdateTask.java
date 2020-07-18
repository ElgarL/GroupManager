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
package org.anjocaido.groupmanager.Tasks;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.anjocaido.groupmanager.GroupManager;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

/*
 * 
 * Created by ElgarL
 */

public class UpdateTask implements Runnable {

	private double newVersion;
	private double currentVersion;
	private String newVersionTitle;
	private String currentVersionTitle;

	public UpdateTask(String version) {

		super();
		currentVersionTitle = version;
		currentVersion = parseVersion(currentVersionTitle);
	}

	@Override
	public void run() {

		try {
			GroupManager.logger.info("Checking for Updates ... ");
			newVersion = updateCheck();
			
			/*
			 * A newer version is available or
			 * we are on a snapshot of the new build.
			 */
			if ((newVersion > currentVersion) || ((newVersion == currentVersion) && (currentVersionTitle.contains("SNAPSHOT")))) {
				
				GroupManager.logger.warning("Stable Version: " + newVersionTitle + " is out!" + " You are still running version: " + currentVersionTitle);
				GroupManager.logger.warning("Update at: https://www.spigotmc.org/resources/groupmanager.80743/");
				
			} else if (currentVersion > newVersion) {
				
				GroupManager.logger.info("Stable Version: " + newVersionTitle + " | Current Version: " + currentVersionTitle);

			} else {
				
				GroupManager.logger.info("No new version available");
			}
		} catch (Exception e) {
			// ignore exceptions
		}

	}

	public double updateCheck() {
		
        try {
            URL url = new URL("https://api.github.com/repos/ElgarL/GroupManager/releases/latest");
            URLConnection conn = url.openConnection();
            conn.setReadTimeout(5000);
            conn.addRequestProperty("User-Agent", "GroupManager Update Check");
            conn.setDoOutput(true);
            
            final JsonReader reader = new JsonReader(new InputStreamReader(conn.getInputStream()));
            
            reader.setLenient(true);
            
            try
            {
                while (reader.hasNext()) 
                {
                    JsonToken nextToken = reader.peek();
                    
                    switch (nextToken) {
                    
                    case BEGIN_OBJECT:
                    
                    	reader.beginObject();
                    	break;
                    	
					case BEGIN_ARRAY:
						
						reader.beginArray();
						break;
						
					case BOOLEAN:
						
						reader.nextBoolean();
						break;
						
					case END_ARRAY:
						
						reader.endArray();
						break;
						
					case END_DOCUMENT:
						
						break;
						
					case END_OBJECT:
						
						reader.endObject();
						break;
						
					case NAME:
						
						String name = reader.nextName();
                        
                        if (name.equalsIgnoreCase("name")) {
                        	newVersionTitle = reader.nextString();
                        }
						break;
						
					case NULL:
						
						reader.nextNull();
						break;
						
					case NUMBER:
						
						reader.nextLong();
						break;
						
					case STRING:
						
						reader.nextString();
						break;
						
					default:
						
						reader.skipValue();
						//System.out.println("Token *** " + JsonToken.valueOf(JsonToken.class, nextToken.name()));
						break;

                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                reader.close();
            }
           
            /*
             * Parse the new version string
             */
            return parseVersion(newVersionTitle);
            
        } catch (Exception e) {
        	GroupManager.logger.info("There was an issue attempting to check for the latest version.");
        	e.printStackTrace();
        }
        /*
         * No version found so report our current version.
         */
        return currentVersion;
    }
	
	/**
	 * Extract the numerical value from the version string.
	 * 
	 * @param version
	 * @return
	 */
	private Double parseVersion(String version) {
		
		try {
			version = version.split(" ")[0].split("-")[0].trim();
			
			if (version.startsWith("v"))
					version = version.replaceFirst("v", "");
			
			return Double.valueOf(version);
		} catch (Exception e) {
			GroupManager.logger.info("There was an issue parsing the version string.");
        	e.printStackTrace();
		}
		
		return 0.0;
	}

}