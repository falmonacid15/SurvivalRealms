package org.falmdev.survivalRealms.presentation.menu;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MenuLoader {

    private final JavaPlugin plugin;

    public MenuLoader(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public Map<String, MenuDefinition> loadAll() {
        saveDefaults();
        Map<String, MenuDefinition> result = new LinkedHashMap<>();
        File menusDir = new File(plugin.getDataFolder(), "menus");
        if (!menusDir.exists()) menusDir.mkdirs();
        loadDir(menusDir, menusDir, result);
        return result;
    }

    private void loadDir(File root, File dir, Map<String, MenuDefinition> result) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                loadDir(root, file, result);
            } else if (file.getName().endsWith(".yml")) {
                String id = root.toURI().relativize(file.toURI())
                        .getPath().replace(".yml", "");
                try {
                    result.put(id, parse(id, YamlConfiguration.loadConfiguration(file)));
                } catch (Exception e) {
                    plugin.getLogger().warning("[MenuLoader] Error cargando " + id + ": " + e.getMessage());
                }
            }
        }
    }

    private MenuDefinition parse(String id, YamlConfiguration cfg) {
        String  name      = cfg.getString("name", id);
        int     size      = cfg.getInt("size", 54);
        boolean useLayout = cfg.getString("layout", "none").equalsIgnoreCase("default")
                || cfg.getBoolean("use-layout", false);
        boolean paginated = cfg.getBoolean("paginated", false);
        String  parent    = cfg.getString("parent", null);

        Map<String, MenuDefinition.ItemDefinition> items = new LinkedHashMap<>();
        if (cfg.isConfigurationSection("items")) {
            for (String key : cfg.getConfigurationSection("items").getKeys(false)) {
                String base       = "items." + key;
                int    slot       = cfg.getInt(base + ".slot", 0);
                String material   = cfg.getString(base + ".material", "STONE");
                String itemName   = cfg.getString(base + ".name", key);
                List<String> lore = cfg.getStringList(base + ".lore");
                String action     = cfg.getString(base + ".action", "NONE");
                String actionData = cfg.getString(base + ".action-data", "");
                items.put(key, new MenuDefinition.ItemDefinition(
                        slot, material, itemName, lore, action, actionData));
            }
        }

        MenuDefinition.PaginatedItemDefinition paginatedItem = null;
        if (paginated && cfg.isConfigurationSection("paginated-item")) {
            paginatedItem = new MenuDefinition.PaginatedItemDefinition(
                    cfg.getString("paginated-item.material", "STONE"),
                    cfg.getString("paginated-item.name", ""),
                    cfg.getStringList("paginated-item.lore"),
                    cfg.getString("paginated-item.action", "NONE"),
                    cfg.getString("paginated-item.action-data", "")
            );
        }

        return new MenuDefinition(id, name, size, useLayout, paginated, parent, items, paginatedItem);
    }

    private void saveDefaults() {
        String[] defaults = { "menus/admin/main.yml", "menus/admin/auth.yml", "menus/warps.yml" };
        for (String path : defaults) {
            File target = new File(plugin.getDataFolder(), path);
            if (!target.exists()) {
                target.getParentFile().mkdirs();
                try {
                    plugin.saveResource(path, false);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("[MenuLoader] Recurso no encontrado en JAR: " + path);
                }
            }
        }
    }
}