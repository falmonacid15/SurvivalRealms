package org.falmdev.survivalRealms.util.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.falmdev.survivalRealms.SurvivalRealms;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SrPlaceholderExpansion extends PlaceholderExpansion {

    private final SurvivalRealms plugin;
    private final List<ModulePlaceholders> modules = new ArrayList<>();

    public SrPlaceholderExpansion(SurvivalRealms plugin) {
        this.plugin = plugin;
    }

    // ── Registro de módulos ───────────────────────────────────────────────────

    public void registerModule(ModulePlaceholders module) {
        modules.add(module);
    }

    // ── Info de la expansión (requerido por PAPI) ─────────────────────────────

    @Override
    public @NotNull String getIdentifier() { return "sr"; }

    @Override
    public @NotNull String getAuthor() { return "falmdev"; }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() { return true; }

    @Override
    public boolean canRegister() { return true; }

    // ── Resolución de placeholders ────────────────────────────────────────────

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        for (ModulePlaceholders module : modules) {
            if (module.handles(params)) {
                return module.resolve(player, params);
            }
        }
        return null; // PAPI interpreta null como "placeholder no encontrado"
    }
}