package org.falmdev.survivalRealms.manager;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.falmdev.survivalRealms.SurvivalRealms;
import org.falmdev.survivalRealms.database.warp.WarpRepository;
import org.falmdev.survivalRealms.model.Warp;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public class WarpManager {

    private final SurvivalRealms plugin;
    private final WarpRepository  repo;

    public WarpManager(SurvivalRealms plugin, WarpRepository repo) {
        this.plugin = plugin;
        this.repo   = repo;
    }

    // ── Crear ─────────────────────────────────────────────────────────────────

    public boolean createWarp(String name, Location loc, UUID createdBy, String icon) {
        try {
            if (repo.findByName(name).isPresent()) return false;
            Warp warp = new Warp(
                    UUID.randomUUID().toString(), name,
                    loc.getWorld().getName(),
                    loc.getX(), loc.getY(), loc.getZ(),
                    loc.getYaw(), loc.getPitch(),
                    createdBy, icon
            );
            repo.saveWarp(warp);
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error creando warp '" + name + "'", e);
            return false;
        }
    }

    // ── Eliminar ──────────────────────────────────────────────────────────────

    public boolean deleteWarp(String name) {
        try {
            Optional<Warp> opt = repo.findByName(name);
            if (opt.isEmpty()) return false;
            repo.deleteWarp(opt.get().getId());
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error eliminando warp '" + name + "'", e);
            return false;
        }
    }

    // ── Obtener ───────────────────────────────────────────────────────────────

    public Optional<Warp> getByName(String name) {
        try { return repo.findByName(name); }
        catch (Exception e) { return Optional.empty(); }
    }

    public Optional<Warp> getById(String id) {
        try { return repo.findById(id); }
        catch (Exception e) { return Optional.empty(); }
    }

    public List<Warp> getAllWarps() {
        try { return repo.findAll(); }
        catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error obteniendo warps", e);
            return List.of();
        }
    }

    /**
     * Retorna los warps que el jugador puede ver/usar,
     * limitados por su grupo de LuckPerms.
     */
    public List<Warp> getWarpsForPlayer(Player player) {
        List<Warp> all = getAllWarps();
        int limit = getLimitForPlayer(player);
        if (limit < 0) return all; // sin límite
        return all.stream()
                .sorted(Comparator.comparing(Warp::getName))
                .limit(limit)
                .toList();
    }

    // ── Límites por LuckPerms ─────────────────────────────────────────────────

    /**
     * Obtiene el límite de warps para un jugador según su grupo primario en LP.
     * Si LP no está disponible, usa el límite por defecto del config.
     * Si el grupo no está en el config, usa default-limit.
     * -1 = sin límite.
     */
    public int getLimitForPlayer(Player player) {
        int defaultLimit = plugin.getConfig().getInt("warps.default-limit", 3);

        LuckPermsManager lp = plugin.getLuckPermsManager();
        if (!lp.isAvailable()) return defaultLimit;

        try {
            LuckPerms api = Bukkit.getServicesManager().load(LuckPerms.class);
            if (api == null) return defaultLimit;

            User user = api.getUserManager().getUser(player.getUniqueId());
            if (user == null) return defaultLimit;

            String primaryGroup = user.getPrimaryGroup();

            // Buscar el límite en config para el grupo primario
            if (plugin.getConfig().contains("warps.limits." + primaryGroup)) {
                return plugin.getConfig().getInt("warps.limits." + primaryGroup, defaultLimit);
            }

            // Buscar también en grupos heredados (orden de peso descendente)
            return user.getInheritedGroups(user.getQueryOptions())
                    .stream()
                    .map(g -> plugin.getConfig().getInt("warps.limits." + g.getName(), Integer.MIN_VALUE))
                    .filter(v -> v != Integer.MIN_VALUE)
                    .findFirst()
                    .orElse(defaultLimit);

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error obteniendo límite de warps para "
                    + player.getName(), e);
            return defaultLimit;
        }
    }

    public boolean exists(String name) {
        return getByName(name).isPresent();
    }

    // ── Teleport ──────────────────────────────────────────────────────────────

    public void teleport(Player player, Warp warp) {
        World world = Bukkit.getWorld(warp.getWorld());
        if (world == null) {
            player.sendMessage("§cEl mundo §f" + warp.getWorld() + " §cno existe.");
            return;
        }
        player.teleport(new Location(world,
                warp.getX(), warp.getY(), warp.getZ(),
                warp.getYaw(), warp.getPitch()));
    }

    public void teleportByName(Player player, String name) {
        getByName(name).ifPresentOrElse(
                warp -> teleport(player, warp),
                () -> player.sendMessage("§cEl warp §f" + name + " §cno existe.")
        );
    }

    public Material parseMaterial(String icon) {
        if (icon == null || icon.isBlank()) return Material.GRASS_BLOCK;
        try { return Material.valueOf(icon.toUpperCase().trim()); }
        catch (IllegalArgumentException e) { return Material.GRASS_BLOCK; }
    }
}