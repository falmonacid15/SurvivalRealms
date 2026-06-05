package org.falmdev.survivalRealms.menu;

import org.bukkit.configuration.file.YamlConfiguration;
import org.falmdev.survivalRealms.SurvivalRealms;
import org.falmdev.survivalRealms.util.LoggerUtil;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

public class MenuLoader {

    private final SurvivalRealms plugin;
    private final File menusFolder;

    public MenuLoader(SurvivalRealms plugin) {
        this.plugin      = plugin;
        this.menusFolder = new File(plugin.getDataFolder(), "menus");
    }

    public Map<String, MenuDefinition> loadAll() {
        Logger log = plugin.getLogger();
        Map<String, MenuDefinition> result = new LinkedHashMap<>();

        if (!menusFolder.exists()) {
            menusFolder.mkdirs();
            saveDefaults();
        }

        loadFolder(menusFolder, menusFolder, result, log);
        LoggerUtil.item(log, "Menús cargados: §f" + result.size());
        return result;
    }

    private void loadFolder(File root, File folder,
                            Map<String, MenuDefinition> result, Logger log) {
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                loadFolder(root, file, result, log);
            } else if (file.getName().endsWith(".yml")) {
                try {
                    String id = resolveId(root, file);
                    MenuDefinition def = parse(id, file);
                    result.put(id, def);
                    LoggerUtil.item(log, "  Menú cargado: §f" + id);
                } catch (Exception e) {
                    log.warning("[MenuLoader] Error cargando " + file.getName()
                            + ": " + e.getMessage());
                }
            }
        }
    }

    private String resolveId(File root, File file) {
        String relative = root.toURI().relativize(file.toURI()).getPath();
        return relative.replace(".yml", "");
    }

    private MenuDefinition parse(String id, File file) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        String  name       = cfg.getString("name", id);
        int     size       = cfg.getInt("size", 45);
        boolean useLayout  = cfg.getString("layout", "none").equalsIgnoreCase("default");
        boolean paginated  = cfg.getBoolean("paginated", false);
        String  parent     = cfg.getString("parent", null);

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
            String pMat    = cfg.getString("paginated-item.material", "STONE");
            String pName   = cfg.getString("paginated-item.name", "");
            List<String> pLore = cfg.getStringList("paginated-item.lore");
            String pAction = cfg.getString("paginated-item.action", "NONE");
            String pData   = cfg.getString("paginated-item.action-data", "");
            paginatedItem  = new MenuDefinition.PaginatedItemDefinition(
                    pMat, pName, pLore, pAction, pData);
        }

        return new MenuDefinition(id, name, size, useLayout,
                paginated, parent, items, paginatedItem);
    }


    private void saveDefaults() {
        String[] defaults = {
                "menus/admin/main.yml",
                "menus/admin/auth.yml",
                "menus/warps.yml"
        };
        for (String path : defaults) {
            File target = new File(plugin.getDataFolder(), path);
            if (!target.exists()) {
                target.getParentFile().mkdirs();
                try {
                    plugin.saveResource(path, false);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("[MenuLoader] Default no encontrado en JAR: "
                            + path + " — créalo manualmente en plugins/SurvivalRealms/menus/");
                }
            }
        }
    }
}