package org.falmdev.survivalRealms.menu;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface MenuAction {
    void execute(Player player, String data);
}