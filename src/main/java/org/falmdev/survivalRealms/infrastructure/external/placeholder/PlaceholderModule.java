package org.falmdev.survivalRealms.infrastructure.external.placeholder;

import org.bukkit.OfflinePlayer;

public interface PlaceholderModule {
    boolean handles(String params);
    String resolve(OfflinePlayer player, String params);
}