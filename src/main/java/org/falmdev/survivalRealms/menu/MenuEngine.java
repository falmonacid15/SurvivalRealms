package org.falmdev.survivalRealms.menu;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import fr.minuskube.inv.content.Pagination;
import fr.minuskube.inv.content.SlotIterator;
import fr.minuskube.inv.content.SlotPos;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.falmdev.survivalRealms.SurvivalRealms;
import org.falmdev.survivalRealms.gui.GuiLayout;
import org.falmdev.survivalRealms.menu.action.ActionRegistry;
import org.falmdev.survivalRealms.menu.placeholder.PlaceholderResolver;
import org.falmdev.survivalRealms.model.Warp;

import java.util.*;
import java.util.stream.Collectors;

public class MenuEngine {

    private final SurvivalRealms plugin;
    private final Map<String, MenuDefinition> registry;
    private final ActionRegistry actions;
    private final PlaceholderResolver placeholders;

    private final Map<UUID, Deque<String>> history = new HashMap<>();

    public MenuEngine(SurvivalRealms plugin,
                      Map<String, MenuDefinition> registry,
                      ActionRegistry actions,
                      PlaceholderResolver placeholders) {
        this.plugin       = plugin;
        this.registry     = registry;
        this.actions      = actions;
        this.placeholders = placeholders;
    }

    // ── API pública ───────────────────────────────────────────────────────────

    public void open(Player player, String menuId) {
        open(player, menuId, 0);
    }

    public void open(Player player, String menuId, int page) {
        MenuDefinition def = registry.get(menuId);
        if (def == null) {
            player.sendMessage("§cMenú no encontrado: " + menuId);
            return;
        }
        pushHistory(player, menuId);
        buildInventory(def).open(player, page);
    }

    public void openParent(Player player) {
        Deque<String> stack = history.get(player.getUniqueId());
        if (stack == null || stack.size() < 2) { player.closeInventory(); return; }
        stack.pop();
        String parent = stack.peek();
        if (parent == null) { player.closeInventory(); return; }
        open(player, parent);
    }

    public void reload() {
        Map<String, MenuDefinition> fresh = new MenuLoader(plugin).loadAll();
        registry.clear();
        registry.putAll(fresh);
    }

    public void clearHistory(UUID uuid) { history.remove(uuid); }

    // ── Construcción ──────────────────────────────────────────────────────────

    private SmartInventory buildInventory(MenuDefinition def) {
        int rows = Math.max(1, def.getSize() / 9);
        return SmartInventory.builder()
                .id(def.getId())
                .provider(new DynamicProvider(def))
                .size(rows, 9)
                .title(def.getName())
                .manager(plugin.getInvManager())
                .build();
    }

    // ── Provider ──────────────────────────────────────────────────────────────

    private class DynamicProvider implements InventoryProvider {

        private final MenuDefinition def;

        DynamicProvider(MenuDefinition def) { this.def = def; }

        @Override
        public void init(Player player, InventoryContents contents) {

            // Layout base
            if (def.isUseLayout()) {
                GuiLayout.apply(contents, plugin, new GuiLayout.GuiActions() {
                    public void onHome(Player p)  {
                        // Vuelve al primer menú del historial
                        Deque<String> stack = history.getOrDefault(
                                p.getUniqueId(), new ArrayDeque<>());
                        String first = stack.isEmpty() ? null :
                                ((ArrayDeque<String>) stack).peekLast();
                        if (first != null) open(p, first); else p.closeInventory();
                    }
                    public void onWarps(Player p)    { open(p, "warps"); }
                    public void onBack(Player p)     { openParent(p); }
                    public void onPrevPage(Player p) {}  // se sobrescribe en paginado
                    public void onNextPage(Player p) {}  // se sobrescribe en paginado
                });
            }

            // Items estáticos
            for (MenuDefinition.ItemDefinition item : def.getItems().values()) {
                int row = item.slot / 9;
                int col = item.slot % 9;
                contents.set(row, col, ClickableItem.of(
                        buildItem(item.material, item.name, item.lore, player),
                        e -> actions.execute(item.action, player, item.actionData)
                ));
            }

            // Paginado
            if (def.isPaginated() && def.getPaginatedItem() != null) {
                buildPaginated(player, contents, def.getPaginatedItem());
            }
        }

        @Override
        public void update(Player player, InventoryContents contents) {}

        // ── Paginación ────────────────────────────────────────────────────────

        private void buildPaginated(Player player, InventoryContents contents,
                                    MenuDefinition.PaginatedItemDefinition template) {

            List<Warp> warps = plugin.getWarpManager().getWarpsForPlayer(player);
            Pagination pagination = contents.pagination();

            ClickableItem[] items = warps.stream().map(warp -> {
                String mat  = template.material.replace("{warp_icon}", warp.getIcon());
                String name = resolveWarp(template.name, warp);
                List<String> lore = template.lore.stream()
                        .map(l -> resolveWarp(l, warp))
                        .collect(Collectors.toList());
                return ClickableItem.of(
                        buildItem(mat, name, lore, player),
                        e -> actions.execute(template.action, player, warp.getId())
                );
            }).toArray(ClickableItem[]::new);

            pagination.setItems(items);
            pagination.setItemsPerPage(GuiLayout.CONTENT_SLOTS.length); // 21

            // API correcta de SmartInvs 1.2.7 — solo fila y columna de inicio
            SlotIterator iterator = contents.newIterator(SlotIterator.Type.HORIZONTAL, 1, 1);

            // blacklist recibe int (índice de slot = row * 9 + col)
            // Bloqueamos col 0 y col 8 de las filas 1, 2, 3
            for (int row = 1; row <= 3; row++) {
                iterator.blacklist(row, 0); // columna 0
                iterator.blacklist(row, 8); // columna 8
            }

            pagination.addToIterator(iterator);

            // Número de página
            contents.set(4, 7, ClickableItem.empty(
                    makeRaw(Material.PAPER, "§7Página §f" + (pagination.getPage() + 1))
            ));

            if (!pagination.isFirst()) {
                contents.set(GuiLayout.SLOT_PREV_PAGE, ClickableItem.of(
                        GuiLayout.makeSkull(GuiLayout.getTexturePrev(), "§7§l« Anterior"),
                        e -> open(player, def.getId(), pagination.previous().getPage())
                ));
            }

            if (!pagination.isLast()) {
                contents.set(GuiLayout.SLOT_NEXT_PAGE, ClickableItem.of(
                        GuiLayout.makeSkull(GuiLayout.getTextureNext(), "§7§lSiguiente »"),
                        e -> open(player, def.getId(), pagination.next().getPage())
                ));
            }
        }

        // ── Builders de items ─────────────────────────────────────────────────

        private ItemStack buildItem(String matStr, String name,
                                    List<String> lore, Player player) {
            Material mat;
            try {
                mat = Material.valueOf(
                        placeholders.resolve(matStr, player).toUpperCase().trim());
            } catch (IllegalArgumentException e) {
                mat = Material.STONE;
            }

            ItemStack item = new ItemStack(mat);
            ItemMeta  meta = item.getItemMeta();
            meta.setDisplayName(color(placeholders.resolve(name, player)));
            meta.setLore(lore.stream()
                    .map(l -> color(placeholders.resolve(l, player)))
                    .collect(Collectors.toList()));
            item.setItemMeta(meta);
            return item;
        }

        private ItemStack makeRaw(Material mat, String name) {
            ItemStack item = new ItemStack(mat);
            ItemMeta  meta = item.getItemMeta();
            meta.setDisplayName(name);
            item.setItemMeta(meta);
            return item;
        }

        private String resolveWarp(String text, Warp warp) {
            return text
                    .replace("{warp_id}",    warp.getId())
                    .replace("{warp_name}",  warp.getName())
                    .replace("{warp_world}", warp.getWorld())
                    .replace("{warp_x}",     fmt(warp.getX()))
                    .replace("{warp_y}",     fmt(warp.getY()))
                    .replace("{warp_z}",     fmt(warp.getZ()))
                    .replace("{warp_icon}",  warp.getIcon());
        }

        private String color(String s) {
            return s == null ? "" : s.replace("&", "§");
        }

        private String fmt(double v) { return String.format("%.1f", v); }
    }

    // ── Historial ─────────────────────────────────────────────────────────────

    private void pushHistory(Player player, String menuId) {
        Deque<String> stack = history.computeIfAbsent(
                player.getUniqueId(), k -> new ArrayDeque<>());
        if (!stack.isEmpty() && stack.peek().equals(menuId)) return;
        stack.push(menuId);
        while (stack.size() > 10)
            ((ArrayDeque<String>) stack).removeLast();
    }
}