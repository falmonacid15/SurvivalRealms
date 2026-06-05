package org.falmdev.survivalRealms.manager;

import at.favre.lib.crypto.bcrypt.BCrypt;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.falmdev.survivalRealms.SurvivalRealms;
import org.falmdev.survivalRealms.database.DatabaseManager;
import org.falmdev.survivalRealms.model.User;
import org.falmdev.survivalRealms.util.MessageUtil;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class AuthManager {

    private final SurvivalRealms plugin;
    private final DatabaseManager db;
    private final SpawnManager spawnManager;
    private final LuckPermsManager luckPerms;

    private final Set<UUID>          authenticated  = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> failedAttempts = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> timeoutTasks   = new ConcurrentHashMap<>();

    public AuthManager(SurvivalRealms plugin, DatabaseManager db,
                       SpawnManager spawnManager, LuckPermsManager luckPerms) {
        this.plugin       = plugin;
        this.db           = db;
        this.spawnManager = spawnManager;
        this.luckPerms    = luckPerms;
    }

    public boolean register(Player player, String password) {
        try {
            if (db.isRegistered(player.getName())) return false;

            String hash = BCrypt.withDefaults().hashToString(12, password.toCharArray());
            User user = new User(
                    player.getUniqueId(), player.getName(), hash,
                    getIp(player), Instant.now(), Instant.now(),
                    null, 0, 0, 0, 0, 0
            );
            db.saveUser(user);

            luckPerms.assignDefaultGroup(player.getUniqueId());

            authenticated.add(player.getUniqueId());
            cancelTimeout(player.getUniqueId());

            Bukkit.getScheduler().runTask(plugin, () ->
                    teleportToMainSpawn(player));

            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error al registrar: " + player.getName(), e);
            return false;
        }
    }

    public LoginResult login(Player player, String password) {
        try {
            Optional<User> opt = db.findByUsername(player.getName());
            if (opt.isEmpty()) return LoginResult.NOT_REGISTERED;

            User user = opt.get();
            BCrypt.Result result = BCrypt.verifyer()
                    .verify(password.toCharArray(), user.getPasswordHash());

            if (!result.verified) {
                int attempts  = failedAttempts.merge(player.getUniqueId(), 1, Integer::sum);
                int max       = plugin.getConfig().getInt("auth.max-attempts", 5);
                int remaining = max - attempts;

                if (remaining <= 0) {
                    failedAttempts.remove(player.getUniqueId());
                    cancelTimeout(player.getUniqueId());
                    Bukkit.getScheduler().runTask(plugin, () ->
                            player.kickPlayer(MessageUtil.color(
                                    plugin.getMsg("messages.login-kicked-attempts"))));
                    return LoginResult.TOO_MANY_ATTEMPTS;
                }
                return LoginResult.wrong(remaining);
            }

            failedAttempts.remove(player.getUniqueId());
            authenticated.add(player.getUniqueId());
            cancelTimeout(player.getUniqueId());

            user.setLastIp(getIp(player));
            user.setLastLogin(Instant.now());

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (user.hasLastLocation()) {
                    teleportToLastLocation(player, user);
                } else {
                    teleportToMainSpawn(player);
                }
            });

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try { db.updateUser(user); }
                catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error actualizando usuario", e);
                }
            });

            return LoginResult.SUCCESS;

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error en login: " + player.getName(), e);
            return LoginResult.ERROR;
        }
    }

    public void saveLastLocation(Player player) {
        if (!isAuthenticated(player.getUniqueId())) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Optional<User> opt = db.findByUuid(player.getUniqueId());
                if (opt.isEmpty()) return;

                User user = opt.get();
                Location loc = player.getLocation();
                user.setLastWorld(loc.getWorld().getName());
                user.setLastX(loc.getX());
                user.setLastY(loc.getY());
                user.setLastZ(loc.getZ());
                user.setLastYaw(loc.getYaw());
                user.setLastPitch(loc.getPitch());
                db.updateLastLocation(user);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error guardando última posición", e);
            }
        });
    }

    public boolean isAuthenticated(UUID uuid) { return authenticated.contains(uuid); }

    public boolean isRegistered(String username) {
        try { return db.isRegistered(username); }
        catch (Exception e) { return false; }
    }

    public void logout(Player player) {
        saveLastLocation(player);
        authenticated.remove(player.getUniqueId());
        failedAttempts.remove(player.getUniqueId());
        cancelTimeout(player.getUniqueId());
    }


    public void startTimeoutTask(Player player) {
        spawnManager.getAuthSpawn().ifPresent(player::teleport);

        int seconds = plugin.getConfig().getInt("auth.login-timeout", 60);
        int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isAuthenticated(player.getUniqueId()) && player.isOnline()) {
                player.kickPlayer(MessageUtil.color(
                        plugin.getMsg("messages.login-timeout")));
            }
        }, seconds * 20L).getTaskId();

        timeoutTasks.put(player.getUniqueId(), taskId);
    }

    private void cancelTimeout(UUID uuid) {
        Integer taskId = timeoutTasks.remove(uuid);
        if (taskId != null) Bukkit.getScheduler().cancelTask(taskId);
    }

    private void teleportToMainSpawn(Player player) {
        spawnManager.getMainSpawn().ifPresentOrElse(
                player::teleport,
                () -> plugin.getLogger().warning(
                        "Main spawn no configurado. Usa /sr setmainspawn")
        );
    }

    private void teleportToLastLocation(Player player, User user) {
        World world = Bukkit.getWorld(user.getLastWorld());
        if (world == null) {
            teleportToMainSpawn(player);
            return;
        }
        Location loc = new Location(world,
                user.getLastX(), user.getLastY(), user.getLastZ(),
                user.getLastYaw(), user.getLastPitch());
        player.teleport(loc);
    }

    private String getIp(Player player) {
        return player.getAddress() != null
                ? player.getAddress().getAddress().getHostAddress()
                : "unknown";
    }

    public enum LoginResult {
        SUCCESS, NOT_REGISTERED, WRONG_PASSWORD, TOO_MANY_ATTEMPTS, ERROR;

        private int remaining;

        public static LoginResult wrong(int remaining) {
            WRONG_PASSWORD.remaining = remaining;
            return WRONG_PASSWORD;
        }

        public int getRemainingAttempts() { return remaining; }
    }
}