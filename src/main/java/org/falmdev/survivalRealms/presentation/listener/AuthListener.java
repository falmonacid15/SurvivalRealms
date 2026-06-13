package org.falmdev.survivalRealms.presentation.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.falmdev.survivalRealms.application.auth.AuthService;
import org.falmdev.survivalRealms.presentation.menu.MenuEngine;
import org.falmdev.survivalRealms.util.MessageUtil;

public class AuthListener implements Listener {

    private final JavaPlugin  plugin;
    private final AuthService authService;
    private final MenuEngine  menuEngine;

    public AuthListener(JavaPlugin plugin, AuthService authService, MenuEngine menuEngine) {
        this.plugin      = plugin;
        this.authService = authService;
        this.menuEngine  = menuEngine;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player    = event.getPlayer();
        int    timeout   = plugin.getConfig().getInt("auth.login-timeout", 60);
        boolean registered = authService.isRegistered(player.getName());
        String  msgKey   = registered ? "messages.not-logged-in" : "messages.not-registered";

        authService.startTimeoutTask(player, timeout);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.sendMessage(MessageUtil.color(
                        plugin.getConfig().getString("messages.prefix", "")
                                + plugin.getConfig().getString(msgKey, "")));
            }
        }, 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        authService.logout(event.getPlayer());
        menuEngine.clearHistory(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (authService.isAuthenticated(event.getPlayer().getUniqueId())) return;
        String cmd = event.getMessage().toLowerCase().split(" ")[0];
        if (cmd.equals("/register") || cmd.equals("/reg")
                || cmd.equals("/login") || cmd.equals("/l") || cmd.equals("/log")) return;
        event.setCancelled(true);
        sendNotAuth(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!authService.isAuthenticated(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (authService.isAuthenticated(event.getPlayer().getUniqueId())) return;
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!authService.isAuthenticated(player.getUniqueId())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!authService.isAuthenticated(player.getUniqueId())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!authService.isAuthenticated(event.getPlayer().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!authService.isAuthenticated(event.getPlayer().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!authService.isAuthenticated(player.getUniqueId())) event.setCancelled(true);
    }

    private void sendNotAuth(Player player) {
        boolean registered = authService.isRegistered(player.getName());
        String  msgKey     = registered ? "messages.not-logged-in" : "messages.not-registered";
        player.sendMessage(MessageUtil.color(
                plugin.getConfig().getString("messages.prefix", "")
                        + plugin.getConfig().getString(msgKey, "")));
    }
}