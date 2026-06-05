package org.falmdev.survivalRealms.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class MessageUtil {

    private MessageUtil() {}

    public static String color(String msg) {
        return msg == null ? "" : msg.replace("&", "§");
    }

    public static Component component(String msg) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(msg);
    }

    public static String replace(String msg, String key, String value) {
        return msg.replace("{" + key + "}", value);
    }
}