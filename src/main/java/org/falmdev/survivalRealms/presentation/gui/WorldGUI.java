package org.falmdev.survivalRealms.presentation.gui;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.InventoryManager;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.falmdev.survivalRealms.presentation.menu.MenuEngine;

public class WorldGUI implements InventoryProvider {

    private final JavaPlugin       plugin;
    private final InventoryManager invManager;
    private final MenuEngine       menuEngine;
    private final World            world;
    private final WorldSelectorGUI selectorGUI;

    public WorldGUI(JavaPlugin plugin, InventoryManager invManager,
                    MenuEngine menuEngine, World world, WorldSelectorGUI selectorGUI) {
        this.plugin      = plugin;
        this.invManager  = invManager;
        this.menuEngine  = menuEngine;
        this.world       = world;
        this.selectorGUI = selectorGUI;
    }

    public void open(Player player) {
        SmartInventory.builder()
                .id("admin_world_" + world.getName())
                .provider(this)
                .size(6, 9)
                .title("§8§lMundo §7— §a" + world.getName())
                .manager(invManager)
                .build()
                .open(player);
    }

    @Override
    public void init(Player player, InventoryContents contents) {
        GuiLayout.apply(contents, new GuiLayout.GuiActions() {
            public void onHome(Player p)     { menuEngine.open(p, "admin/main"); }
            public void onWarps(Player p)    { menuEngine.open(p, "warps"); }
            public void onBack(Player p)     { selectorGUI.open(p); }
            public void onPrevPage(Player p) {}
            public void onNextPage(Player p) {}
        });

        contents.set(0, 4, ClickableItem.empty(worldInfoItem()));

        contents.set(1, 1, timeItem(player, "§eDía",        0L));
        contents.set(1, 2, timeItem(player, "§6Amanecer",   1000L));
        contents.set(1, 3, timeItem(player, "§cNoche",      13000L));
        contents.set(1, 4, timeItem(player, "§9Medianoche", 18000L));
        contents.set(1, 6, weatherItem(player, WeatherType.CLEAR));
        contents.set(1, 7, weatherItem(player, WeatherType.DOWNFALL));

        contents.set(2, 1, difficultyItem(player, Difficulty.PEACEFUL));
        contents.set(2, 2, difficultyItem(player, Difficulty.EASY));
        contents.set(2, 3, difficultyItem(player, Difficulty.NORMAL));
        contents.set(2, 4, difficultyItem(player, Difficulty.HARD));
        contents.set(2, 6, pvpToggle(player));
        contents.set(2, 7, mobSpawnToggle(player));

        contents.set(3, 4, ClickableItem.of(
                GuiLayout.makeItem(Material.COMMAND_BLOCK,
                        "§6§lGamerules",
                        "§7Ver y modificar todas las",
                        "§7reglas del juego.",
                        "", "§eClick para abrir."),
                e -> new WorldGameruleGUI(plugin, invManager, menuEngine, world, this).open(player)));
    }

    @Override
    public void update(Player player, InventoryContents contents) {}

    private ItemStack worldInfoItem() {
        long time      = world.getTime();
        String timeStr = time < 6000 ? "Mañana" : time < 12000 ? "Tarde"
                                                  : time < 18000 ? "Noche" : "Madrugada";
        boolean storm   = world.hasStorm();
        boolean thunder = world.isThundering();
        String weather  = thunder ? "§8Tormenta" : storm ? "§9Lluvia" : "§eDespejado";

        return GuiLayout.makeItem(Material.GRASS_BLOCK,
                "§a§l" + world.getName(),
                "§7Tipo:        §f" + world.getEnvironment().name(),
                "§7Dificultad:  §f" + world.getDifficulty().name(),
                "§7Tiempo:      §f" + timeStr + " §8(" + time + ")",
                "§7Clima:       " + weather,
                "§7Jugadores:   §f" + world.getPlayers().size(),
                "§7Entidades:   §f" + world.getEntities().size(),
                "§7Chunks:      §f" + world.getLoadedChunks().length,
                "§7Seed:        §f" + world.getSeed()
        );
    }

    private ClickableItem timeItem(Player admin, String label, long ticks) {
        boolean active = Math.abs(world.getTime() - ticks) < 500;
        Material mat   = active ? Material.GLOWSTONE : Material.GRAY_STAINED_GLASS_PANE;
        return ClickableItem.of(
                GuiLayout.makeItem(mat, label,
                        "§7Establecer hora a §f" + ticks + " ticks",
                        active ? "§a▶ Activo" : ""),
                e -> {
                    world.setTime(ticks);
                    admin.sendMessage("§aHora cambiada a §f" + label.replaceAll("§.", "")
                            + " §aen §f" + world.getName());
                    refresh(admin);
                });
    }

    private ClickableItem weatherItem(Player admin, WeatherType type) {
        boolean isClear = type == WeatherType.CLEAR;
        boolean active  = isClear ? !world.hasStorm() : world.hasStorm();
        Material mat    = isClear
                ? (active ? Material.SUNFLOWER          : Material.YELLOW_STAINED_GLASS_PANE)
                : (active ? Material.WATER_BUCKET       : Material.BLUE_STAINED_GLASS_PANE);
        String name = isClear ? "§eDespejado" : "§9Lluvia";
        String desc = isClear ? "§7Limpia el clima." : "§7Activa la lluvia.";

        return ClickableItem.of(
                GuiLayout.makeItem(mat, name, desc, active ? "§a▶ Activo" : ""),
                e -> {
                    world.setStorm(!isClear);
                    world.setThundering(false);
                    admin.sendMessage("§aClima cambiado a §f"
                            + name.replaceAll("§.", "") + " §aen §f" + world.getName());
                    refresh(admin);
                });
    }

    private ClickableItem difficultyItem(Player admin, Difficulty difficulty) {
        boolean active = world.getDifficulty() == difficulty;
        Material mat = active ? Material.GLOWSTONE : switch (difficulty) {
            case PEACEFUL -> Material.POPPY;
            case EASY     -> Material.FLOWERING_AZALEA;
            case NORMAL   -> Material.ORANGE_TULIP;
            default       -> Material.WITHER_ROSE;
        };
        String color = switch (difficulty) {
            case PEACEFUL -> "§a";
            case EASY     -> "§e";
            case NORMAL   -> "§6";
            default       -> "§c";
        };
        return ClickableItem.of(
                GuiLayout.makeItem(mat, color + difficulty.name(),
                        "§7Dificultad: §f" + difficulty.name(),
                        active ? "§a▶ Activo" : ""),
                e -> {
                    world.setDifficulty(difficulty);
                    admin.sendMessage("§aDificultad cambiada a §f" + difficulty.name());
                    refresh(admin);
                });
    }

    private ClickableItem pvpToggle(Player admin) {
        boolean pvp = world.getPVP();
        return ClickableItem.of(
                GuiLayout.makeItem(
                        pvp ? Material.DIAMOND_SWORD : Material.WOODEN_SWORD,
                        pvp ? "§aPVP: §aActivado" : "§cPVP: §cDesactivado",
                        "§7Click para " + (pvp ? "desactivar." : "activar."),
                        "§7Estado: " + (pvp ? "§aON" : "§cOFF")),
                e -> {
                    world.setPVP(!pvp);
                    admin.sendMessage("§aPVP " + (!pvp ? "§aactivado" : "§cdesactivado")
                            + " §aen §f" + world.getName());
                    refresh(admin);
                });
    }

    private ClickableItem mobSpawnToggle(Player admin) {
        boolean spawns = world.getAllowMonsters();
        return ClickableItem.of(
                GuiLayout.makeItem(
                        spawns ? Material.ZOMBIE_HEAD : Material.SKELETON_SKULL,
                        spawns ? "§cMobs: §aActivados" : "§aMobs: §cDesactivados",
                        "§7Click para " + (spawns ? "desactivar." : "activar."),
                        "§7Estado: " + (spawns ? "§aON" : "§cOFF")),
                e -> {
                    world.setSpawnFlags(!spawns, world.getAllowAnimals());
                    admin.sendMessage("§aSpawn de mobs "
                            + (!spawns ? "§aactivado" : "§cdesactivado")
                            + " §aen §f" + world.getName());
                    refresh(admin);
                });
    }

    private void refresh(Player admin) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> open(admin), 1L);
    }
}