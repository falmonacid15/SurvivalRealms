package org.falmdev.survivalRealms.util.placeholder.modules;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.falmdev.survivalRealms.SurvivalRealms;
import org.falmdev.survivalRealms.manager.SpawnManager;
import org.falmdev.survivalRealms.util.placeholder.ModulePlaceholders;

import java.util.Optional;
import java.util.Set;

public class SpawnPlaceholders implements ModulePlaceholders {

    private static final Set<String> HANDLES = Set.of(
            "auth_spawn_status",
            "auth_spawn_world",
            "auth_spawn_x",
            "auth_spawn_y",
            "auth_spawn_z",
            "main_spawn_status",
            "main_spawn_world",
            "main_spawn_x",
            "main_spawn_y",
            "main_spawn_z"
    );

    private final SurvivalRealms plugin;

    public SpawnPlaceholders(SurvivalRealms plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean handles(String params) {
        return HANDLES.contains(params.toLowerCase());
    }

    @Override
    public String resolve(OfflinePlayer player, String params) {
        SpawnManager spawns = plugin.getSpawnManager();

        return switch (params.toLowerCase()) {

            case "auth_spawn_status" ->
                    spawns.hasAuthSpawn() ? "Configurado" : "No configurado";

            case "auth_spawn_world" ->
                    spawns.getAuthSpawn()
                            .map(l -> l.getWorld().getName())
                            .orElse("N/A");

            case "auth_spawn_x" ->
                    spawns.getAuthSpawn().map(l -> fmt(l.getX())).orElse("N/A");

            case "auth_spawn_y" ->
                    spawns.getAuthSpawn().map(l -> fmt(l.getY())).orElse("N/A");

            case "auth_spawn_z" ->
                    spawns.getAuthSpawn().map(l -> fmt(l.getZ())).orElse("N/A");

            case "main_spawn_status" ->
                    spawns.hasMainSpawn() ? "Configurado" : "No configurado";

            case "main_spawn_world" ->
                    spawns.getMainSpawn()
                            .map(l -> l.getWorld().getName())
                            .orElse("N/A");

            case "main_spawn_x" ->
                    spawns.getMainSpawn().map(l -> fmt(l.getX())).orElse("N/A");

            case "main_spawn_y" ->
                    spawns.getMainSpawn().map(l -> fmt(l.getY())).orElse("N/A");

            case "main_spawn_z" ->
                    spawns.getMainSpawn().map(l -> fmt(l.getZ())).orElse("N/A");

            default -> "";
        };
    }

    private String fmt(double v) { return String.format("%.1f", v); }
}