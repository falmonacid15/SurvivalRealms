package org.falmdev.survivalRealms.infrastructure.external.placeholder;

import org.bukkit.OfflinePlayer;
import org.falmdev.survivalRealms.application.warp.WarpService;

import java.util.Set;

public class WarpPlaceholderModule implements PlaceholderModule {

    private static final Set<String> HANDLES = Set.of(
            "warps_total",
            "warps_limit"
    );

    private final WarpService warpService;

    public WarpPlaceholderModule(WarpService warpService) {
        this.warpService = warpService;
    }

    @Override
    public boolean handles(String params) {
        return HANDLES.contains(params.toLowerCase());
    }

    @Override
    public String resolve(OfflinePlayer player, String params) {
        return switch (params.toLowerCase()) {
            case "warps_total" -> String.valueOf(warpService.getAllWarps().size());
            case "warps_limit" -> {
                int limit = warpService.getLimitForPlayer(player.getUniqueId());
                yield limit < 0 ? "∞" : String.valueOf(limit);
            }
            default -> "";
        };
    }
}