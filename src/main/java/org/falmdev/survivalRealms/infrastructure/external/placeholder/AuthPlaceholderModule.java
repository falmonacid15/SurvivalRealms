package org.falmdev.survivalRealms.infrastructure.external.placeholder;

import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.falmdev.survivalRealms.application.auth.AuthService;

import java.util.Set;
import java.util.UUID;

public class AuthPlaceholderModule implements PlaceholderModule {

    private static final Set<String> HANDLES = Set.of(
            "auth_registered",
            "auth_authenticated",
            "auth_timeout",
            "auth_max_attempts",
            "auth_min_pass",
            "auth_max_pass"
    );

    private final JavaPlugin  plugin;
    private final AuthService authService;

    public AuthPlaceholderModule(JavaPlugin plugin, AuthService authService) {
        this.plugin      = plugin;
        this.authService = authService;
    }

    @Override
    public boolean handles(String params) {
        return HANDLES.contains(params.toLowerCase());
    }

    @Override
    public String resolve(OfflinePlayer player, String params) {
        UUID uuid = player.getUniqueId();
        return switch (params.toLowerCase()) {
            case "auth_registered"    -> authService.isRegistered(player.getName()) ? "Sí" : "No";
            case "auth_authenticated" -> authService.isAuthenticated(uuid) ? "Sí" : "No";
            case "auth_timeout"       -> String.valueOf(plugin.getConfig().getInt("auth.login-timeout", 60));
            case "auth_max_attempts"  -> String.valueOf(plugin.getConfig().getInt("auth.max-attempts", 5));
            case "auth_min_pass"      -> String.valueOf(plugin.getConfig().getInt("auth.min-password-length", 6));
            case "auth_max_pass"      -> String.valueOf(plugin.getConfig().getInt("auth.max-password-length", 32));
            default -> "";
        };
    }
}