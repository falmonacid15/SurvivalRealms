package org.falmdev.survivalRealms.util.placeholder;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.falmdev.survivalRealms.SurvivalRealms;
import org.falmdev.survivalRealms.util.LoggerUtil;
import org.falmdev.survivalRealms.util.placeholder.modules.AuthPlaceholders;
import org.falmdev.survivalRealms.util.placeholder.modules.SpawnPlaceholders;
import org.falmdev.survivalRealms.util.placeholder.modules.WarpPlaceholders;

import java.util.logging.Logger;

public class PlaceholderManager {

    private final SurvivalRealms plugin;
    private SrPlaceholderExpansion expansion;
    private boolean papiAvailable = false;

    public PlaceholderManager(SurvivalRealms plugin) {
        this.plugin = plugin;
    }

    // ── Inicialización ────────────────────────────────────────────────────────

    public void initialize() {
        Logger log = plugin.getLogger();
        LoggerUtil.section(log, "PlaceholderManager");

        papiAvailable = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");

        if (papiAvailable) {
            expansion = new SrPlaceholderExpansion(plugin);

            // Registrar módulos en orden
            expansion.registerModule(new AuthPlaceholders(plugin));
            expansion.registerModule(new SpawnPlaceholders(plugin));
            expansion.registerModule(new WarpPlaceholders(plugin));

            expansion.register();

            LoggerUtil.item(log, "PlaceholderAPI     conectado");
            LoggerUtil.item(log, "Expansión          %sr_<placeholder>%");
            LoggerUtil.item(log, "Módulo Auth        registrado");
            LoggerUtil.item(log, "Módulo Spawn       registrado");
            LoggerUtil.item(log, "Módulo Warps       registrado");
        } else {
            LoggerUtil.warn(log, "PlaceholderManager",
                    "PlaceholderAPI no encontrado — placeholders internos activos");
        }

        LoggerUtil.done(log, "PlaceholderManager");
    }

    public void unregister() {
        if (papiAvailable && expansion != null) {
            expansion.unregister();
        }
    }

    // ── API pública ───────────────────────────────────────────────────────────

    /**
     * Procesa placeholders %sr_*% y los de cualquier otra expansión PAPI.
     * Si PAPI no está disponible, usa el resolver interno.
     */
    public String process(Player player, String text) {
        if (text == null) return "";
        if (papiAvailable) {
            return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
        }
        return resolveInternal(player, text);
    }

    public boolean isPapiAvailable() { return papiAvailable; }

    // ── Fallback interno sin PAPI ─────────────────────────────────────────────

    private String resolveInternal(Player player, String text) {
        if (expansion == null) return text;
        // Buscar %sr_algo% manualmente
        StringBuilder sb = new StringBuilder(text);
        String prefix = "%sr_";
        int start;
        while ((start = sb.indexOf(prefix)) != -1) {
            int end = sb.indexOf("%", start + 1);
            if (end == -1) break;
            String params = sb.substring(start + prefix.length(), end);
            String value  = expansion.onRequest(player, params);
            sb.replace(start, end + 1, value != null ? value : "");
        }
        return sb.toString();
    }
}