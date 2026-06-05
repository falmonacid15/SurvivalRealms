package org.falmdev.survivalRealms.infrastructure.external;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.falmdev.survivalRealms.domain.port.PlaceholderPort;
import org.falmdev.survivalRealms.infrastructure.external.placeholder.PlaceholderModule;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PlaceholderApiAdapter implements PlaceholderPort {

    private final JavaPlugin              plugin;
    private final List<PlaceholderModule> modules = new ArrayList<>();
    private SrExpansion                   expansion;
    private boolean                       available = false;

    public PlaceholderApiAdapter(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void addModule(PlaceholderModule module) {
        modules.add(module);
    }

    @Override
    public void register() {
        available = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        if (!available) return;
        expansion = new SrExpansion();
        expansion.register();
    }

    @Override
    public void unregister() {
        if (available && expansion != null) expansion.unregister();
    }

    @Override
    public String process(Player player, String text) {
        if (text == null || text.isBlank()) return "";
        text = text.replaceAll("\\{(sr_[^}]+)}", "%$1%");
        if (available) return PlaceholderAPI.setPlaceholders(player, text);
        return resolveInternal(player, text);
    }

    public boolean isAvailable() { return available; }

    private String resolveInternal(Player player, String text) {
        for (PlaceholderModule module : modules) {
            String key = extractKey(text);
            if (key != null && module.handles(key)) {
                return text.replace("{" + key + "}", module.resolve(player, key));
            }
        }
        return text;
    }

    private String extractKey(String text) {
        int start = text.indexOf('{');
        int end   = text.indexOf('}');
        if (start == -1 || end == -1 || end <= start) return null;
        return text.substring(start + 1, end);
    }

    private class SrExpansion extends PlaceholderExpansion {

        @Override
        public @NotNull String getIdentifier() { return "sr"; }

        @Override
        public @NotNull String getAuthor() { return "falmdev"; }

        @Override
        public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }

        @Override
        public boolean persist() { return true; }

        @Override
        public boolean canRegister() { return true; }

        @Override
        public String onRequest(OfflinePlayer player, @NotNull String params) {
            for (PlaceholderModule module : modules) {
                if (module.handles(params)) return module.resolve(player, params);
            }
            return null;
        }
    }
}