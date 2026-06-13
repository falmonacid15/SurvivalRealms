package org.falmdev.survivalRealms.infrastructure.external.placeholder;

import org.bukkit.OfflinePlayer;
import org.falmdev.survivalRealms.application.spawn.SpawnService;

import java.util.Set;

public class SpawnPlaceholderModule implements PlaceholderModule {

    private static final Set<String> HANDLES = Set.of(
            "auth_spawn_status", "auth_spawn_world",
            "auth_spawn_x", "auth_spawn_y", "auth_spawn_z",
            "main_spawn_status", "main_spawn_world",
            "main_spawn_x", "main_spawn_y", "main_spawn_z"
    );

    private final SpawnService spawnService;

    public SpawnPlaceholderModule(SpawnService spawnService) {
        this.spawnService = spawnService;
    }

    @Override
    public boolean handles(String params) {
        return HANDLES.contains(params.toLowerCase());
    }

    @Override
    public String resolve(OfflinePlayer player, String params) {
        return switch (params.toLowerCase()) {
            case "auth_spawn_status" -> spawnService.hasAuthSpawn() ? "Configurado" : "No configurado";
            case "auth_spawn_world"  -> spawnService.getAuthSpawn().map(s -> s.getWorld()).orElse("N/A");
            case "auth_spawn_x"      -> spawnService.getAuthSpawn().map(s -> fmt(s.getX())).orElse("N/A");
            case "auth_spawn_y"      -> spawnService.getAuthSpawn().map(s -> fmt(s.getY())).orElse("N/A");
            case "auth_spawn_z"      -> spawnService.getAuthSpawn().map(s -> fmt(s.getZ())).orElse("N/A");
            case "main_spawn_status" -> spawnService.hasMainSpawn() ? "Configurado" : "No configurado";
            case "main_spawn_world"  -> spawnService.getMainSpawn().map(s -> s.getWorld()).orElse("N/A");
            case "main_spawn_x"      -> spawnService.getMainSpawn().map(s -> fmt(s.getX())).orElse("N/A");
            case "main_spawn_y"      -> spawnService.getMainSpawn().map(s -> fmt(s.getY())).orElse("N/A");
            case "main_spawn_z"      -> spawnService.getMainSpawn().map(s -> fmt(s.getZ())).orElse("N/A");
            default -> "";
        };
    }

    private String fmt(double v) { return String.format("%.1f", v); }
}