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
package org.anjocaido.groupmanager.commands;

import org.anjocaido.groupmanager.localization.Messages;
import org.bukkit.ChatColor;
import org.bukkit.plugin.PluginDescriptionFile;
import org.jetbrains.annotations.NotNull;


/**
 * @author ElgarL, l1ttleO
 *
 */
public class ManVer extends BaseCommand {

    /**
     *
     */
    public ManVer() {}

    @Override
    protected boolean parseCommand(@NotNull String[] args) {

        PluginDescriptionFile pdfFile = plugin.getDescription();
        sender.sendMessage(ChatColor.WHITE + String.format(Messages.getString("VERSION"), pdfFile.getVersion()));
        return true;
    }
}
