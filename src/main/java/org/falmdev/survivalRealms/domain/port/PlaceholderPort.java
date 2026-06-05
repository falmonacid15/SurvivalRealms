package org.falmdev.survivalRealms.domain.port;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public interface PlaceholderPort {
    void register();
    void unregister();
    String process(Player player, String text);
}