package org.falmdev.survivalRealms.presentation.gui;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.InventoryManager;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import fr.minuskube.inv.content.Pagination;
import fr.minuskube.inv.content.SlotIterator;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.GameRules;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.falmdev.survivalRealms.presentation.menu.MenuEngine;

import java.util.List;

public class WorldGameruleGUI implements InventoryProvider {

    private final JavaPlugin       plugin;
    private final InventoryManager invManager;
    private final MenuEngine       menuEngine;
    private final World            world;
    private final WorldGUI         worldGUI;

    private static final List<GameRule<?>> RULES = List.of(
            GameRules.ADVANCE_TIME,
            GameRules.ADVANCE_WEATHER,
            GameRules.ALLOW_ENTERING_NETHER_USING_PORTALS,
            GameRules.BLOCK_DROPS,
            GameRules.BLOCK_EXPLOSION_DROP_DECAY,
            GameRules.COMMAND_BLOCK_OUTPUT,
            GameRules.COMMAND_BLOCKS_WORK,
            GameRules.DROWNING_DAMAGE,
            GameRules.ELYTRA_MOVEMENT_CHECK,
            GameRules.ENDER_PEARLS_VANISH_ON_DEATH,
            GameRules.ENTITY_DROPS,
            GameRules.FALL_DAMAGE,
            GameRules.FIRE_DAMAGE,
            GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER,
            GameRules.FORGIVE_DEAD_PLAYERS,
            GameRules.FREEZE_DAMAGE,
            GameRules.GLOBAL_SOUND_EVENTS,
            GameRules.IMMEDIATE_RESPAWN,
            GameRules.KEEP_INVENTORY,
            GameRules.LAVA_SOURCE_CONVERSION,
            GameRules.MOB_EXPLOSION_DROP_DECAY,
            GameRules.MOB_GRIEFING,
            GameRules.NATURAL_HEALTH_REGENERATION,
            GameRules.PLAYER_MOVEMENT_CHECK,
            GameRules.PLAYERS_NETHER_PORTAL_CREATIVE_DELAY,
            GameRules.PLAYERS_NETHER_PORTAL_DEFAULT_DELAY,
            GameRules.PLAYERS_SLEEPING_PERCENTAGE,
            GameRules.PROJECTILES_CAN_BREAK_BLOCKS,
            GameRules.PVP,
            GameRules.RAIDS,
            GameRules.RANDOM_TICK_SPEED,
            GameRules.REDUCED_DEBUG_INFO,
            GameRules.RESPAWN_RADIUS,
            GameRules.SEND_COMMAND_FEEDBACK,
            GameRules.SHOW_ADVANCEMENT_MESSAGES,
            GameRules.SHOW_DEATH_MESSAGES,
            GameRules.SPECTATORS_GENERATE_CHUNKS,
            GameRules.TNT_EXPLOSION_DROP_DECAY,
            GameRules.UNIVERSAL_ANGER,
            GameRules.WATER_SOURCE_CONVERSION
    );

    public WorldGameruleGUI(JavaPlugin plugin, InventoryManager invManager,
                            MenuEngine menuEngine, World world, WorldGUI worldGUI) {
        this.plugin     = plugin;
        this.invManager = invManager;
        this.menuEngine = menuEngine;
        this.world      = world;
        this.worldGUI   = worldGUI;
    }

    public void open(Player player) {
        SmartInventory.builder()
                .id("admin_world_gamerules_" + world.getName())
                .provider(this)
                .size(6, 9)
                .title("§8§lGamerules §7— §a" + world.getName())
                .manager(invManager)
                .build()
                .open(player);
    }

    @Override
    public void init(Player player, InventoryContents contents) {
        GuiLayout.apply(contents, new GuiLayout.GuiActions() {
            public void onHome(Player p)     { menuEngine.open(p, "admin/main"); }
            public void onWarps(Player p)    { menuEngine.open(p, "warps"); }
            public void onBack(Player p)     { worldGUI.open(p); }
            public void onPrevPage(Player p) {}
            public void onNextPage(Player p) {}
        });

        Pagination pagination = contents.pagination();

        ClickableItem[] items = RULES.stream()
                .map(rule -> gameruleItem(player, rule))
                .toArray(ClickableItem[]::new);

        pagination.setItems(items);
        pagination.setItemsPerPage(21);

        SlotIterator iterator = contents.newIterator(SlotIterator.Type.HORIZONTAL, 1, 1);
        for (int row = 1; row <= 3; row++) {
            iterator.blacklist(row, 0);
            iterator.blacklist(row, 8);
        }
        for (int col = 0; col < 9; col++) {
            iterator.blacklist(4, col);
        }
        pagination.addToIterator(iterator);

        if (!pagination.isFirst()) {
            contents.set(GuiLayout.SLOT_PREV_PAGE, ClickableItem.of(
                    GuiLayout.makeSkull(GuiLayout.getTexturePrev(), "§7§l« Anterior"),
                    e -> open(player)));
        }
        if (!pagination.isLast()) {
            contents.set(GuiLayout.SLOT_NEXT_PAGE, ClickableItem.of(
                    GuiLayout.makeSkull(GuiLayout.getTextureNext(), "§7§lSiguiente »"),
                    e -> open(player)));
        }
    }

    @Override
    public void update(Player player, InventoryContents contents) {}

    @SuppressWarnings("unchecked")
    private <T> ClickableItem gameruleItem(Player admin, GameRule<T> rule) {
        T value        = world.getGameRuleValue(rule);
        String valStr  = value != null ? value.toString() : "N/A";
        boolean isBool = value instanceof Boolean;
        boolean isTrue = Boolean.TRUE.equals(value);

        Material mat = isBool
                ? (isTrue ? Material.LIME_DYE : Material.GRAY_DYE)
                : Material.PAPER;
        String color = isBool ? (isTrue ? "§a" : "§c") : "§e";
        String ruleName = rule.getKey().getKey();

        return ClickableItem.of(
                GuiLayout.makeItem(mat,
                        color + ruleName,
                        "§7Valor actual: §f" + valStr,
                        "",
                        isBool
                                ? "§eClick para " + (isTrue ? "desactivar." : "activar.")
                                : "§eClick izq: §f+1  §eClick der: §f-1"),
                e -> {
                    if (isBool) {
                        world.setGameRule((GameRule<Boolean>) rule, !isTrue);
                        admin.sendMessage("§aGamerule §f" + ruleName + " §acambiada a §f" + !isTrue);
                    } else if (value instanceof Integer intVal) {
                        int next = e.isRightClick()
                                ? Math.max(0, intVal - 1)
                                : intVal + 1;
                        world.setGameRule((GameRule<Integer>) rule, next);
                        admin.sendMessage("§aGamerule §f" + ruleName + " §acambiada a §f" + next);
                    } else {
                        admin.sendMessage("§cNo se puede modificar esta gamerule desde la GUI.");
                    }
                    Bukkit.getScheduler().runTaskLater(plugin, () -> open(admin), 1L);
                });
    }
}