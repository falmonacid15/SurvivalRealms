package org.falmdev.survivalRealms.presentation.menu;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.InventoryManager;
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
import org.bukkit.plugin.java.JavaPlugin;
import org.falmdev.survivalRealms.application.warp.WarpService;
import org.falmdev.survivalRealms.domain.model.Warp;
import org.falmdev.survivalRealms.presentation.gui.GuiLayout;
import org.falmdev.survivalRealms.presentation.gui.UserListGUI;
import org.falmdev.survivalRealms.presentation.menu.action.ActionRegistry;
import org.falmdev.survivalRealms.presentation.menu.placeholder.MenuPlaceholderResolver;
import org.falmdev.survivalRealms.util.MessageUtil;

import java.util.*;
import java.util.stream.Collectors;

public class MenuEngine {

    private final JavaPlugin                  plugin;
    private final Map<String, MenuDefinition> registry;
    private final ActionRegistry              actions;
    private final MenuPlaceholderResolver     placeholders;
    private final WarpService                 warpService;
    private final InventoryManager            invManager;
    private final Map<UUID, Deque<String>>    history = new HashMap<>();

    private UserListGUI userListGUI;

    public MenuEngine(JavaPlugin plugin, Map<String, MenuDefinition> registry,
                      ActionRegistry actions, MenuPlaceholderResolver placeholders,
                      WarpService warpService, InventoryManager invManager) {
        this.plugin       = plugin;
        this.registry     = registry;
        this.actions      = actions;
        this.placeholders = placeholders;
        this.warpService  = warpService;
        this.invManager   = invManager;
    }

    public void setUserListGUI(UserListGUI userListGUI) {
        this.userListGUI = userListGUI;
    }

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
        buildInventory(def, page).open(player);
    }

    public void openParent(Player player) {
        Deque<String> stack = history.get(player.getUniqueId());
        if (stack == null || stack.size() < 2) { player.closeInventory(); return; }
        stack.pop();
        String parent = stack.peek();
        if (parent == null) { player.closeInventory(); return; }
        open(player, parent);
    }

    public void openUserList(Player player) {
        if (userListGUI != null) userListGUI.open(player);
    }

    public void reload() {
        Map<String, MenuDefinition> fresh = new MenuLoader(plugin).loadAll();
        registry.clear();
        registry.putAll(fresh);
    }

    public void clearHistory(UUID uuid) { history.remove(uuid); }

    private SmartInventory buildInventory(MenuDefinition def, int page) {
        int rows = Math.max(1, def.getSize() / 9);
        return SmartInventory.builder()
                .id(def.getId())
                .provider(new MenuProvider(def, page))
                .size(rows, 9)
                .title(placeholders.resolveStatic(def.getName()))
                .manager(invManager)
                .build();
    }

    private void pushHistory(Player player, String menuId) {
        history.computeIfAbsent(player.getUniqueId(), k -> new ArrayDeque<>());
        Deque<String> stack = history.get(player.getUniqueId());
        if (stack.isEmpty() || !stack.peek().equals(menuId)) stack.push(menuId);
    }

    private class MenuProvider implements InventoryProvider {

        private final MenuDefinition def;
        private final int            page;

        MenuProvider(MenuDefinition def, int page) {
            this.def  = def;
            this.page = page;
        }

        @Override
        public void init(Player player, InventoryContents contents) {
            if (def.isUseLayout()) {
                GuiLayout.apply(contents, new GuiLayout.GuiActions() {
                    public void onHome(Player p) { open(p, "admin/main"); }
                    public void onWarps(Player p) { open(p, "warps"); }
                    public void onBack(Player p) {
                        String parent = def.getParent();
                        if (parent != null && !parent.isBlank()) open(p, parent);
                        else openParent(p);
                    }
                    public void onPrevPage(Player p) {}
                    public void onNextPage(Player p) {}
                });
            }

            if (def.isPaginated()) {
                List<Warp> warps = warpService.getWarpsForPlayer(player.getUniqueId());
                Pagination pagination = contents.pagination();
                ClickableItem[] items = warps.stream()
                        .map(warp -> buildWarpItem(player, warp))
                        .toArray(ClickableItem[]::new);
                pagination.setItems(items);
                pagination.setItemsPerPage(28);
                SlotIterator iterator = contents.newIterator(SlotIterator.Type.HORIZONTAL, 1, 1);
                iterator.blacklist(SlotPos.of(0, 0)).blacklist(SlotPos.of(0, 8));
                pagination.addToIterator(iterator);
                applyStaticItems(player, contents);
                applyPaginationControls(player, contents, pagination);
            } else {
                applyStaticItems(player, contents);
            }
        }

        @Override
        public void update(Player player, InventoryContents contents) {}

        private void applyStaticItems(Player player, InventoryContents contents) {
            for (MenuDefinition.ItemDefinition item : def.getItems().values()) {
                ItemStack stack = buildStack(player, item.material, item.name, item.lore);
                contents.set(item.slot / 9, item.slot % 9,
                        ClickableItem.of(stack, e ->
                                actions.execute(player, item.action, item.actionData)));
            }
        }

        private void applyPaginationControls(Player player, InventoryContents contents,
                                             Pagination pagination) {
            if (!pagination.isFirst()) {
                ItemStack prev = buildStack(player, "ARROW", "&aPágina anterior", List.of());
                contents.set(GuiLayout.SLOT_PREV_PAGE, ClickableItem.of(prev, e ->
                        open(player, def.getId(), pagination.getPage() - 1)));
            }
            if (!pagination.isLast()) {
                ItemStack next = buildStack(player, "ARROW", "&aPágina siguiente", List.of());
                contents.set(GuiLayout.SLOT_NEXT_PAGE, ClickableItem.of(next, e ->
                        open(player, def.getId(), pagination.getPage() + 1)));
            }
        }

        private ClickableItem buildWarpItem(Player player, Warp warp) {
            MenuDefinition.PaginatedItemDefinition template = def.getPaginatedItem();
            String name = template != null
                    ? placeholders.resolve(template.name
                                           .replace("{warp_name}", warp.getName()), player)
                    : "&f" + warp.getName();
            List<String> lore = template != null
                    ? template.lore.stream()
                      .map(l -> placeholders.resolve(l
                                                     .replace("{warp_name}", warp.getName())
                                                     .replace("{warp_world}", warp.getWorld())
                                                     .replace("{warp_x}", String.format("%.1f", warp.getX()))
                                                     .replace("{warp_y}", String.format("%.1f", warp.getY()))
                                                     .replace("{warp_z}", String.format("%.1f", warp.getZ()))
                                                     .replace("{warp_id}", warp.getId()), player))
                      .collect(Collectors.toList())
                    : List.of();
            String material = template != null
                    ? template.material.replace("{warp_icon}", warp.getIcon())
                    : warp.getIcon();
            String action = template != null ? template.action : "WARP_TELEPORT";
            String data   = template != null
                    ? template.actionData.replace("{warp_id}", warp.getId())
                      .replace("{warp_name}", warp.getName())
                    : warp.getName();
            return ClickableItem.of(buildStack(player, material, name, lore),
                    e -> actions.execute(player, action, data));
        }

        private ItemStack buildStack(Player player, String materialName,
                                     String name, List<String> lore) {
            Material mat;
            try {
                mat = Material.valueOf(materialName.toUpperCase());
            } catch (IllegalArgumentException e) {
                mat = Material.STONE;
            }
            ItemStack stack = new ItemStack(mat);
            ItemMeta  meta  = stack.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(MessageUtil.color(placeholders.resolve(name, player)));
                meta.setLore(lore.stream()
                        .map(l -> MessageUtil.color(placeholders.resolve(l, player)))
                        .collect(Collectors.toList()));
                stack.setItemMeta(meta);
            }
            return stack;
        }
    }
}