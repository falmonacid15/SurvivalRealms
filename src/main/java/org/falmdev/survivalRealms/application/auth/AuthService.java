package org.falmdev.survivalRealms.application.auth;

import at.favre.lib.crypto.bcrypt.BCrypt;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.falmdev.survivalRealms.application.spawn.SpawnService;
import org.falmdev.survivalRealms.domain.model.User;
import org.falmdev.survivalRealms.domain.port.PermissionPort;
import org.falmdev.survivalRealms.domain.repository.UserRepository;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class AuthService {

    private final JavaPlugin       plugin;
    private final UserRepository   userRepository;
    private final SpawnService     spawnService;
    private final PermissionPort   permissions;
    private final int              maxAttempts;

    private final Set<UUID>          authenticated  = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> failedAttempts = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> timeoutTasks   = new ConcurrentHashMap<>();

    public AuthService(JavaPlugin plugin, UserRepository userRepository,
                       SpawnService spawnService, PermissionPort permissions,
                       int maxAttempts) {
        this.plugin         = plugin;
        this.userRepository = userRepository;
        this.spawnService   = spawnService;
        this.permissions    = permissions;
        this.maxAttempts    = maxAttempts;
    }

    public RegisterResult register(Player player, String password) {
        try {
            if (userRepository.existsByUsername(player.getName())) {
                return RegisterResult.ALREADY_REGISTERED;
            }
            String hash = BCrypt.withDefaults().hashToString(12, password.toCharArray());
            User user = new User(
                    player.getUniqueId(), player.getName(), hash,
                    extractIp(player), Instant.now(), Instant.now(),
                    null, 0, 0, 0, 0, 0
            );
            userRepository.save(user);
            permissions.assignDefaultGroup(player.getUniqueId());
            authenticated.add(player.getUniqueId());
            cancelTimeout(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> spawnService.teleportToMain(player));
            return RegisterResult.SUCCESS;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error registrando a " + player.getName(), e);
            return RegisterResult.ERROR;
        }
    }

    public LoginResult login(Player player, String password) {
        try {
            Optional<User> opt = userRepository.findByUsername(player.getName());
            if (opt.isEmpty()) return LoginResult.NOT_REGISTERED;

            User user = opt.get();
            BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), user.getPasswordHash());

            if (!result.verified) {
                int attempts = failedAttempts.merge(player.getUniqueId(), 1, Integer::sum);
                int remaining = maxAttempts - attempts;
                if (remaining <= 0) {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            player.kickPlayer("§cDemasiados intentos fallidos."));
                    failedAttempts.remove(player.getUniqueId());
                    return LoginResult.MAX_ATTEMPTS;
                }
                return LoginResult.WRONG_PASSWORD.withRemaining(remaining);
            }

            user.setLastLogin(Instant.now());
            user.setLastIp(extractIp(player));
            userRepository.update(user);

            authenticated.add(player.getUniqueId());
            failedAttempts.remove(player.getUniqueId());
            cancelTimeout(player.getUniqueId());

            Bukkit.getScheduler().runTask(plugin, () -> spawnService.teleportToMain(player));
            return LoginResult.SUCCESS;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error en login de " + player.getName(), e);
            return LoginResult.ERROR;
        }
    }

    public void logout(Player player) {
        UUID uuid = player.getUniqueId();
        authenticated.remove(uuid);
        failedAttempts.remove(uuid);
        cancelTimeout(uuid);
        saveLastLocation(player);
    }

    public void startTimeoutTask(Player player, int seconds) {
        int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!authenticated.contains(player.getUniqueId()) && player.isOnline()) {
                player.kickPlayer("§cTiempo de autenticación agotado.");
                timeoutTasks.remove(player.getUniqueId());
            }
        }, seconds * 20L).getTaskId();

        timeoutTasks.put(player.getUniqueId(), taskId);
        spawnService.teleportToAuth(player);
    }

    public boolean isAuthenticated(UUID uuid) {
        return authenticated.contains(uuid);
    }

    public boolean isRegistered(String username) {
        try {
            return userRepository.existsByUsername(username);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error verificando registro de " + username, e);
            return false;
        }
    }

    public Optional<User> findUser(UUID uuid) {
        try {
            return userRepository.findByUuid(uuid);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error buscando usuario " + uuid, e);
            return Optional.empty();
        }
    }

    private void saveLastLocation(Player player) {
        try {
            Optional<User> opt = userRepository.findByUuid(player.getUniqueId());
            if (opt.isEmpty()) return;
            User user = opt.get();
            user.setLastWorld(player.getWorld().getName());
            user.setLastX(player.getLocation().getX());
            user.setLastY(player.getLocation().getY());
            user.setLastZ(player.getLocation().getZ());
            user.setLastYaw(player.getLocation().getYaw());
            user.setLastPitch(player.getLocation().getPitch());
            userRepository.updateLastLocation(user);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error guardando ubicación de " + player.getName(), e);
        }
    }

    private void cancelTimeout(UUID uuid) {
        Integer taskId = timeoutTasks.remove(uuid);
        if (taskId != null) Bukkit.getScheduler().cancelTask(taskId);
    }

    private String extractIp(Player player) {
        if (player.getAddress() == null) return "unknown";
        return player.getAddress().getAddress().getHostAddress();
    }

    public enum RegisterResult {
        SUCCESS, ALREADY_REGISTERED, ERROR
    }

    public enum LoginResult {
        SUCCESS, NOT_REGISTERED, WRONG_PASSWORD, MAX_ATTEMPTS, ERROR;

        private int remainingAttempts;

        public LoginResult withRemaining(int remaining) {
            this.remainingAttempts = remaining;
            return this;
        }

        public int getRemainingAttempts() { return remainingAttempts; }
    }
}