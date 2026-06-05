package org.falmdev.survivalRealms.menu;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class MenuRegistry {

    private final Map<String, MenuDefinition> menus = new LinkedHashMap<>();

    public void register(String id, MenuDefinition def) {
        menus.put(id, def);
    }

    public void registerAll(Map<String, MenuDefinition> all) {
        menus.putAll(all);
    }

    public void clear() {
        menus.clear();
    }

    public Map<String, MenuDefinition> getAll() {
        return menus;
    }

    public boolean exists(String id) {
        return menus.containsKey(id);
    }
}