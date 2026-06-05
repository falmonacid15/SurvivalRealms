package org.falmdev.survivalRealms.menu.placeholder;

import org.bukkit.entity.Player;
import org.falmdev.survivalRealms.SurvivalRealms;

public class PlaceholderResolver {

    private final SurvivalRealms plugin;

    public PlaceholderResolver(SurvivalRealms plugin) {
        this.plugin = plugin;
    }

    /**
     * Resuelve placeholders en texto de menús.
     * Soporta dos formatos:
     *   {sr_auth_timeout}  → convierte a %sr_auth_timeout% y pasa por PAPI
     *   %sr_auth_timeout%  → pasa directo por PAPI
     */
    public String resolve(String text, Player player) {
        if (text == null || text.isBlank()) return "";

        // Convierte {sr_*} → %sr_*% para PAPI
        text = text.replaceAll("\\{(sr_[^}]+)}", "%$1%");

        // Pasa por PlaceholderManager (PAPI o fallback)
        return plugin.getPlaceholderManager().process(player, text);
    }
}