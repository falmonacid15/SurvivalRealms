package org.falmdev.survivalRealms.application.warp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.falmdev.survivalRealms.domain.model.Warp;
import org.falmdev.survivalRealms.domain.port.PermissionPort;
import org.falmdev.survivalRealms.domain.repository.WarpRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public class WarpService {

    private final JavaPlugin      plugin;
    private final WarpRepository  warpRepository;
    private final PermissionPort  permissions;
    private final FileConfiguration config;

    public WarpService(JavaPlugin plugin, WarpRepository warpRepository, PermissionPort permissions) {
        this.plugin         = plugin;
        this.warpRepository = warpRepository;
        this.permissions    = permissions;
        this.config         = plugin.getConfig();
    }

    public boolean createWarp(String name, Location location, UUID createdBy, String icon) {
        try {
            if (warpRepository.findByName(name).isPresent()) return false;
            Warp warp = new Warp(
                    UUID.randomUUID().toString(), name,
                    location.getWorld().getName(),
                    location.getX(), location.getY(), location.getZ(),
                    location.getYaw(), location.getPitch(),
                    createdBy, icon
            );
            warpRepository.save(warp);
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error creando warp '" + name + "'", e);
            return false;
        }
    }

    public boolean deleteWarp(String name) {
        try {
            Optional<Warp> opt = warpRepository.findByName(name);
            if (opt.isEmpty()) return false;
            warpRepository.delete(opt.get().getId());
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error eliminando warp '" + name + "'", e);
            return false;
        }
    }

    public boolean teleportToWarp(Player player, String name) {
        try {
            Optional<Warp> opt = warpRepository.findByName(name);
            if (opt.isEmpty()) return false;
            Warp warp = opt.get();
            World world = Bukkit.getWorld(warp.getWorld());
            if (world == null) return false;
            Location loc = new Location(world, warp.getX(), warp.getY(), warp.getZ(),
                    warp.getYaw(), warp.getPitch());
            player.teleport(loc);
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error teletransportando a warp '" + name + "'", e);
            return false;
        }
    }

    public Optional<Warp> findByName(String name) {
        try {
            return warpRepository.findByName(name);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error buscando warp '" + name + "'", e);
            return Optional.empty();
        }
    }

    public Optional<Warp> findById(String id) {
        try {
            return warpRepository.findById(id);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error buscando warp por id '" + id + "'", e);
            return Optional.empty();
        }
    }

    public List<Warp> getAllWarps() {
        try {
            return warpRepository.findAll();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error obteniendo warps", e);
            return List.of();
        }
    }

    public List<Warp> getWarpsForPlayer(UUID uuid) {
        int limit = getLimitForPlayer(uuid);
        List<Warp> all = getAllWarps().stream()
                .sorted(Comparator.comparing(Warp::getName))
                .toList();
        if (limit < 0) return all;
        return all.stream().limit(limit).toList();
    }

    public int getLimitForPlayer(UUID uuid) {
        if (!permissions.isAvailable()) {
            return config.getInt("warps.default-limit", 3);
        }
        var limits = config.getConfigurationSection("warps.limits");
        if (limits == null) return config.getInt("warps.default-limit", 3);
        for (String group : limits.getKeys(false)) {
            if (permissions.isInGroup(uuid, group)) {
                return limits.getInt(group);
            }
        }
        return config.getInt("warps.default-limit", 3);
    }
}