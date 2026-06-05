package org.falmdev.survivalRealms.util.placeholder.modules;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.falmdev.survivalRealms.SurvivalRealms;
import org.falmdev.survivalRealms.util.placeholder.ModulePlaceholders;

public class WarpPlaceholders implements ModulePlaceholders {

    private final SurvivalRealms plugin;

    public WarpPlaceholders(SurvivalRealms plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean handles(String params) {
        return params.startsWith("warp_");
    }

    @Override
    public String resolve(OfflinePlayer offlinePlayer, String params) {
        String p = params.toLowerCase();

        if (p.equals("warp_count")) {
            return String.valueOf(plugin.getWarpManager().getAllWarps().size());
        }

        if (p.equals("warp_limit")) {
            Player online = offlinePlayer.getPlayer();
            if (online == null) return "?";
            int limit = plugin.getWarpManager().getLimitForPlayer(online);
            return limit < 0 ? "∞" : String.valueOf(limit);
        }

        if (p.equals("warp_available")) {
            Player online = offlinePlayer.getPlayer();
            if (online == null) return "?";
            return String.valueOf(plugin.getWarpManager().getWarpsForPlayer(online).size());
        }

        if (p.startsWith("warp_name_")) {
            String id = params.substring("warp_name_".length());
            return plugin.getWarpManager().getById(id)
                    .map(Warp -> Warp.getName())
                    .orElse("N/A");
        }

        if (p.startsWith("warp_world_")) {
            String id = params.substring("warp_world_".length());
            return plugin.getWarpManager().getById(id)
                    .map(Warp -> Warp.getWorld())
                    .orElse("N/A");
        }

        if (p.startsWith("warp_x_")) {
            String id = params.substring("warp_x_".length());
            return plugin.getWarpManager().getById(id)
                    .map(w -> String.format("%.1f", w.getX()))
                    .orElse("N/A");
        }

        if (p.startsWith("warp_y_")) {
            String id = params.substring("warp_y_".length());
            return plugin.getWarpManager().getById(id)
                    .map(w -> String.format("%.1f", w.getY()))
                    .orElse("N/A");
        }

        if (p.startsWith("warp_z_")) {
            String id = params.substring("warp_z_".length());
            return plugin.getWarpManager().getById(id)
                    .map(w -> String.format("%.1f", w.getZ()))
                    .orElse("N/A");
        }

        return "";
    }
}