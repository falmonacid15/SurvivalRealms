package org.falmdev.survivalRealms.presentation.menu.placeholder;

import org.bukkit.entity.Player;
import org.falmdev.survivalRealms.domain.port.PlaceholderPort;

public class MenuPlaceholderResolver {

    private final PlaceholderPort placeholderPort;

    public MenuPlaceholderResolver(PlaceholderPort placeholderPort) {
        this.placeholderPort = placeholderPort;
    }

    public String resolve(String text, Player player) {
        if (text == null || text.isBlank()) return "";
        text = text.replaceAll("\\{(sr_[^}]+)}", "%$1%");
        return placeholderPort.process(player, text);
    }

    public String resolveStatic(String text) {
        if (text == null || text.isBlank()) return "";
        return text.replace("&", "§");
    }
}