package org.falmdev.survivalRealms.menu.action;

import org.bukkit.entity.Player;
import org.falmdev.survivalRealms.SurvivalRealms;
import org.falmdev.survivalRealms.menu.MenuAction;
import org.falmdev.survivalRealms.util.MessageUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class ActionRegistry {

    private final SurvivalRealms plugin;
    private final Map<String, MenuAction> actions = new HashMap<>();

    public ActionRegistry(SurvivalRealms plugin) {
        this.plugin = plugin;
        registerDefaults();
    }

    private void registerDefaults() {

        // ── Navegación ────────────────────────────────────────────────────────
        register("NONE", (player, data) -> {});

        register("CLOSE", (player, data) -> player.closeInventory());

        register("OPEN_MENU", (player, data) -> {
            if (data == null || data.isBlank()) return;
            plugin.getMenuEngine().open(player, data);
        });

        register("OPEN_PARENT", (player, data) -> {
            plugin.getMenuEngine().openParent(player);
        });

        // ── Auth ──────────────────────────────────────────────────────────────
        register("SET_AUTH_SPAWN", (player, data) -> {
            plugin.getSpawnManager().setAuthSpawn(player.getLocation());
            player.sendMessage(MessageUtil.color(
                    plugin.getMsg("messages.prefix")
                            + plugin.getMsg("messages.auth-spawn-set")));
            player.closeInventory();
        });

        register("SET_MAIN_SPAWN", (player, data) -> {
            plugin.getSpawnManager().setMainSpawn(player.getLocation());
            player.sendMessage(MessageUtil.color(
                    plugin.getMsg("messages.prefix")
                            + plugin.getMsg("messages.main-spawn-set")));
            player.closeInventory();
        });

        register("OPEN_USER_LIST", (player, data) ->
                plugin.getUserListGUI().open(player, 0));

        register("RELOAD_CONFIG", (player, data) -> {
            plugin.reloadConfig();
            player.sendMessage(MessageUtil.color(
                    plugin.getMsg("messages.prefix")
                            + plugin.getMsg("messages.reload-success")));
            player.closeInventory();
        });

        // ── Warps ─────────────────────────────────────────────────────────────
        register("WARP_TELEPORT", (player, data) -> {
            if (data == null || data.isBlank()) return;
            plugin.getWarpManager().getById(data).ifPresentOrElse(
                    warp -> {
                        player.closeInventory();
                        plugin.getWarpManager().teleport(player, warp);
                        player.sendMessage(MessageUtil.color(
                                plugin.getMsg("messages.prefix")
                                        + "§7Teletransportado a §f" + warp.getName()));
                    },
                    () -> player.sendMessage(MessageUtil.color(
                            plugin.getMsg("messages.prefix") + "§cWarp no encontrado."))
            );
        });
    }

    public void register(String name, MenuAction action) {
        actions.put(name.toUpperCase(), action);
    }

    public void execute(String name, Player player, String data) {
        MenuAction action = actions.get(name == null ? "NONE" : name.toUpperCase());
        if (action == null) {
            plugin.getLogger().warning("[ActionRegistry] Acción desconocida: " + name);
            return;
        }
        try {
            action.execute(player, data);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "[ActionRegistry] Error ejecutando acción " + name, e);
        }
    }
}