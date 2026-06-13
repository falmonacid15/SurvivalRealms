package org.falmdev.survivalRealms.application.spawn;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.falmdev.survivalRealms.domain.model.Spawn;

import java.util.Optional;

public class SpawnService {

    private final JavaPlugin        plugin;
    private final FileConfiguration config;

    public SpawnService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public void setAuthSpawn(Location location) {
        saveSpawn("auth-spawn", location);
        plugin.saveConfig();
    }

    public void setMainSpawn(Location location) {
        saveSpawn("main-spawn", location);
        plugin.saveConfig();
    }

    public boolean hasAuthSpawn() {
        return config.contains("auth-spawn.world");
    }

    public boolean hasMainSpawn() {
        return config.contains("main-spawn.world");
    }

    public Optional<Spawn> getAuthSpawn() {
        return readSpawn("auth-spawn");
    }

    public Optional<Spawn> getMainSpawn() {
        return readSpawn("main-spawn");
    }

    public void teleportToAuth(Player player) {
        getAuthSpawn().ifPresentOrElse(
                spawn -> teleport(player, spawn),
                () -> plugin.getLogger().warning("auth-spawn no configurado.")
        );
    }

    public void teleportToMain(Player player) {
        getMainSpawn().ifPresentOrElse(
                spawn -> teleport(player, spawn),
                () -> plugin.getLogger().warning("main-spawn no configurado.")
        );
    }

    private void teleport(Player player, Spawn spawn) {
        World world = Bukkit.getWorld(spawn.getWorld());
        if (world == null) {
            plugin.getLogger().warning("Mundo no encontrado: " + spawn.getWorld());
            return;
        }
        Location loc = new Location(world, spawn.getX(), spawn.getY(), spawn.getZ(),
                spawn.getYaw(), spawn.getPitch());
        player.teleport(loc);
    }

    private void saveSpawn(String key, Location loc) {
        config.set(key + ".world", loc.getWorld().getName());
        config.set(key + ".x",     loc.getX());
        config.set(key + ".y",     loc.getY());
        config.set(key + ".z",     loc.getZ());
        config.set(key + ".yaw",   loc.getYaw());
        config.set(key + ".pitch", loc.getPitch());
    }

    private Optional<Spawn> readSpawn(String key) {
        if (!config.contains(key + ".world")) return Optional.empty();
        return Optional.of(new Spawn(
                config.getString(key + ".world"),
                config.getDouble(key + ".x"),
                config.getDouble(key + ".y"),
                config.getDouble(key + ".z"),
                (float) config.getDouble(key + ".yaw"),
                (float) config.getDouble(key + ".pitch")
        ));
    }
}