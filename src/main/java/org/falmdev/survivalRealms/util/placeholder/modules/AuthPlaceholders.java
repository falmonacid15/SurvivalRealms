package org.falmdev.survivalRealms.util.placeholder.modules;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.falmdev.survivalRealms.SurvivalRealms;
import org.falmdev.survivalRealms.util.placeholder.ModulePlaceholders;

import java.util.Set;

public class AuthPlaceholders implements ModulePlaceholders {

    private static final Set<String> HANDLES = Set.of(
            "auth_is_logged",
            "auth_is_registered",
            "auth_attempts_left",
            "auth_timeout",
            "auth_max_attempts",
            "auth_min_pass",
            "auth_max_pass"
    );

    private final SurvivalRealms plugin;

    public AuthPlaceholders(SurvivalRealms plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean handles(String params) {
        return HANDLES.contains(params.toLowerCase());
    }

    @Override
    public String resolve(OfflinePlayer offlinePlayer, String params) {
        return switch (params.toLowerCase()) {

            case "auth_is_logged" -> {
                Player p = offlinePlayer.getPlayer();
                yield p != null && plugin.getAuthManager()
                        .isAuthenticated(p.getUniqueId()) ? "true" : "false";
            }

            case "auth_is_registered" ->
                    plugin.getAuthManager()
                            .isRegistered(offlinePlayer.getName()) ? "true" : "false";

            case "auth_timeout" ->
                    String.valueOf(plugin.getConfig().getInt("auth.login-timeout", 60));

            case "auth_max_attempts" ->
                    String.valueOf(plugin.getConfig().getInt("auth.max-attempts", 5));

            case "auth_min_pass" ->
                    String.valueOf(plugin.getConfig().getInt("auth.min-password-length", 6));

            case "auth_max_pass" ->
                    String.valueOf(plugin.getConfig().getInt("auth.max-password-length", 32));

            default -> "";
        };
    }
}