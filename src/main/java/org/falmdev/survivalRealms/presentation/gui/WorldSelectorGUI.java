package org.falmdev.survivalRealms.presentation.gui;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.InventoryManager;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.falmdev.survivalRealms.presentation.menu.MenuEngine;

import java.util.List;

public class WorldSelectorGUI implements InventoryProvider {

    private final JavaPlugin       plugin;
    private final InventoryManager invManager;
    private final MenuEngine       menuEngine;

    public WorldSelectorGUI(JavaPlugin plugin, InventoryManager invManager, MenuEngine menuEngine) {
        this.plugin     = plugin;
        this.invManager = invManager;
        this.menuEngine = menuEngine;
    }

    public void open(Player player) {
        SmartInventory.builder()
                .id("admin_world_selector")
                .provider(this)
                .size(6, 9)
                .title("§8§lAdmin §7— §aSeleccionar mundo")
                .manager(invManager)
                .build()
                .open(player);
    }

    @Override
    public void init(Player player, InventoryContents contents) {
        GuiLayout.apply(contents, new GuiLayout.GuiActions() {
            public void onHome(Player p)     { menuEngine.open(p, "admin/main"); }
            public void onWarps(Player p)    { menuEngine.open(p, "warps"); }
            public void onBack(Player p)     { menuEngine.open(p, "admin/main"); }
            public void onPrevPage(Player p) {}
            public void onNextPage(Player p) {}
        });

        List<World> worlds = Bukkit.getWorlds();

        int[] slots = {2, 3, 4, 5, 6};
        for (int i = 0; i < worlds.size() && i < slots.length; i++) {
            World world = worlds.get(i);
            Material mat = switch (world.getEnvironment()) {
                case NETHER  -> Material.NETHERRACK;
                case THE_END -> Material.END_STONE;
                default      -> Material.GRASS_BLOCK;
            };
            contents.set(2, slots[i], ClickableItem.of(
                    GuiLayout.makeItem(mat,
                            "§a§l" + world.getName(),
                            "§7Tipo:      §f" + world.getEnvironment().name(),
                            "§7Dificultad:§f" + world.getDifficulty().name(),
                            "§7Jugadores: §f" + world.getPlayers().size(),
                            "§7Entidades: §f" + world.getEntities().size(),
                            "", "§eClick para gestionar."),
                    e -> new WorldGUI(plugin, invManager, menuEngine, world, this).open(player)));

        }
    }

    @Override
    public void update(Player player, InventoryContents contents) {}
}