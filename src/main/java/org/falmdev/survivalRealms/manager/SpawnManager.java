package org.falmdev.survivalRealms.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.falmdev.survivalRealms.SurvivalRealms;

import java.util.Optional;

public class SpawnManager {

    private final SurvivalRealms plugin;

    public SpawnManager(SurvivalRealms plugin) {
        this.plugin = plugin;
    }

    // ── Auth spawn ────────────────────────────────────────────────────────────

    public Optional<Location> getAuthSpawn() {
        return readLocation("auth-spawn");
    }

    public void setAuthSpawn(Location loc) {
        writeLocation("auth-spawn", loc);
    }

    public boolean hasAuthSpawn() {
        return plugin.getConfig().contains("auth-spawn.world");
    }

    // ── Main spawn ────────────────────────────────────────────────────────────

    public Optional<Location> getMainSpawn() {
        return readLocation("main-spawn");
    }

    public void setMainSpawn(Location loc) {
        writeLocation("main-spawn", loc);
    }

    public boolean hasMainSpawn() {
        return plugin.getConfig().contains("main-spawn.world");
    }

    // ── Utilidades ────────────────────────────────────────────────────────────

    private Optional<Location> readLocation(String path) {
        FileConfiguration cfg = plugin.getConfig();
        String worldName = cfg.getString(path + ".world");
        if (worldName == null || worldName.isBlank()) return Optional.empty();

        World world = Bukkit.getWorld(worldName);
        if (world == null) return Optional.empty();

        double x     = cfg.getDouble(path + ".x", 0);
        double y     = cfg.getDouble(path + ".y", 64);
        double z     = cfg.getDouble(path + ".z", 0);
        float  yaw   = (float) cfg.getDouble(path + ".yaw", 0);
        float  pitch = (float) cfg.getDouble(path + ".pitch", 0);

        return Optional.of(new Location(world, x, y, z, yaw, pitch));
    }

    private void writeLocation(String path, Location loc) {
        FileConfiguration cfg = plugin.getConfig();
        cfg.set(path + ".world", loc.getWorld().getName());
        cfg.set(path + ".x",     loc.getX());
        cfg.set(path + ".y",     loc.getY());
        cfg.set(path + ".z",     loc.getZ());
        cfg.set(path + ".yaw",   (double) loc.getYaw());
        cfg.set(path + ".pitch", (double) loc.getPitch());
        plugin.saveConfig();
    }
}