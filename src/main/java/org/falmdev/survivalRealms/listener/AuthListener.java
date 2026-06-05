package org.falmdev.survivalRealms.listener;

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
import org.falmdev.survivalRealms.SurvivalRealms;
import org.falmdev.survivalRealms.manager.AuthManager;
import org.falmdev.survivalRealms.util.MessageUtil;

public class AuthListener implements Listener {

    private final SurvivalRealms plugin;
    private final AuthManager    auth;

    public AuthListener(SurvivalRealms plugin, AuthManager auth) {
        this.plugin = plugin;
        this.auth   = auth;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Inicia temporizador y teleporta al auth spawn
        auth.startTimeoutTask(player);

        // Mensaje de instrucción con pequeño delay para que el cliente cargue
        boolean registered = auth.isRegistered(player.getName());
        String msg = plugin.getMsg(registered
                ? "messages.not-logged-in"
                : "messages.not-registered");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline())
                player.sendMessage(MessageUtil.color(
                        plugin.getMsg("messages.prefix") + msg));
        }, 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Guarda posición y limpia sesión
        auth.logout(event.getPlayer());
        plugin.getMenuEngine().clearHistory(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (auth.isAuthenticated(event.getPlayer().getUniqueId())) return;

        String cmd = event.getMessage().toLowerCase().split(" ")[0];
        if (cmd.equals("/register") || cmd.equals("/reg")
                || cmd.equals("/login")  || cmd.equals("/l") || cmd.equals("/log")) return;

        event.setCancelled(true);
        sendNotLoggedIn(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!auth.isAuthenticated(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask(plugin, () -> sendNotLoggedIn(event.getPlayer()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (auth.isAuthenticated(event.getPlayer().getUniqueId())) return;
        if (event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (!auth.isAuthenticated(event.getPlayer().getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!auth.isAuthenticated(event.getPlayer().getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!auth.isAuthenticated(event.getPlayer().getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player p && !auth.isAuthenticated(p.getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (!auth.isAuthenticated(event.getPlayer().getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player p && !auth.isAuthenticated(p.getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player p && !auth.isAuthenticated(p.getUniqueId()))
            event.setCancelled(true);
    }

    private void sendNotLoggedIn(Player player) {
        player.sendMessage(MessageUtil.color(
                plugin.getMsg("messages.prefix") + plugin.getMsg("messages.not-logged-in")));
    }
}