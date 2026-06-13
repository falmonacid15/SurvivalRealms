package org.falmdev.survivalRealms.presentation.menu.action;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.falmdev.survivalRealms.application.spawn.SpawnService;
import org.falmdev.survivalRealms.application.warp.WarpService;
import org.falmdev.survivalRealms.presentation.gui.WorldSelectorGUI;
import org.falmdev.survivalRealms.presentation.menu.MenuEngine;
import org.falmdev.survivalRealms.util.MessageUtil;

import java.util.HashMap;
import java.util.Map;

public class ActionRegistry {

    private final Map<String, MenuAction> actions = new HashMap<>();

    public ActionRegistry(JavaPlugin plugin, SpawnService spawnService,
                          WarpService warpService, MenuEngineProvider menuEngineProvider, WorldSelectorGUI worldSelectorGUI) {
        register("NONE",        (p, d) -> {});
        register("CLOSE",       (p, d) -> p.closeInventory());
        register("OPEN_MENU",   (p, d) -> menuEngineProvider.get().open(p, d));
        register("OPEN_PARENT", (p, d) -> menuEngineProvider.get().openParent(p));
        register("OPEN_USER_LIST", (p, d) -> menuEngineProvider.get().openUserList(p));

        register("SET_AUTH_SPAWN", (p, d) -> {
            spawnService.setAuthSpawn(p.getLocation());
            p.sendMessage(MessageUtil.color(
                    plugin.getConfig().getString("messages.prefix", "")
                            + plugin.getConfig().getString("messages.auth-spawn-set", "")));
            p.closeInventory();
        });

        register("OPEN_WORLD_GUI", (p, d) -> worldSelectorGUI.open(p));

        register("SET_MAIN_SPAWN", (p, d) -> {
            spawnService.setMainSpawn(p.getLocation());
            p.sendMessage(MessageUtil.color(
                    plugin.getConfig().getString("messages.prefix", "")
                            + plugin.getConfig().getString("messages.main-spawn-set", "")));
            p.closeInventory();
        });

        register("WARP_TELEPORT", (p, d) -> {
            p.closeInventory();
            boolean ok = warpService.teleportToWarp(p, d);
            if (!ok) p.sendMessage(MessageUtil.color(
                    plugin.getConfig().getString("messages.prefix", "") + "&cWarp no encontrado."));
        });

        register("RELOAD_CONFIG", (p, d) -> {
            plugin.reloadConfig();
            menuEngineProvider.get().reload();
            p.sendMessage(MessageUtil.color(
                    plugin.getConfig().getString("messages.prefix", "")
                            + plugin.getConfig().getString("messages.reload-success", "")));
        });
    }

    public void register(String key, MenuAction action) {
        actions.put(key.toUpperCase(), action);
    }

    public void execute(Player player, String key, String data) {
        MenuAction action = actions.get(key.toUpperCase());
        if (action != null) action.execute(player, data);
    }

    @FunctionalInterface
    public interface MenuEngineProvider {
        MenuEngine get();
    }
}