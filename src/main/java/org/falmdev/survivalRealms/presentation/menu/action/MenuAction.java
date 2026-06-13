package org.falmdev.survivalRealms.presentation.menu.action;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface MenuAction {
    void execute(Player player, String data);
}