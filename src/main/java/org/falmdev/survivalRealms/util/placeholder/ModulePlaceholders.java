package org.falmdev.survivalRealms.util.placeholder;

import org.bukkit.OfflinePlayer;

public interface ModulePlaceholders {

    /**
     * Retorna true si este módulo maneja el placeholder dado.
     * Ej: params = "auth_is_logged" → AuthPlaceholders retorna true.
     */
    boolean handles(String params);

    /**
     * Resuelve el placeholder y retorna el valor como String.
     * Nunca retornar null aquí — usar "" o un valor por defecto.
     */
    String resolve(OfflinePlayer player, String params);
}