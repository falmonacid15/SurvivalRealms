package org.falmdev.survivalRealms.gui.admin;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import fr.minuskube.inv.content.Pagination;
import fr.minuskube.inv.content.SlotIterator;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.falmdev.survivalRealms.SurvivalRealms;
import org.falmdev.survivalRealms.gui.GuiLayout;
import org.falmdev.survivalRealms.model.User;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

public class UserListGUI implements InventoryProvider {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                    .withZone(ZoneId.systemDefault());

    private final SurvivalRealms plugin;
    private final int page;

    public UserListGUI(SurvivalRealms plugin, int page) {
        this.plugin = plugin;
        this.page   = page;
    }

    public SmartInventory build() {
        return SmartInventory.builder()
                .id("admin_user_list")
                .provider(this)
                .size(6, 9)
                .title("§8§lAdmin §7— §dUsuarios")
                .manager(plugin.getInvManager())
                .build();
    }

    public void open(Player player, int page) {
        new UserListGUI(plugin, page).build().open(player);
    }

    @Override
    public void init(Player player, InventoryContents contents) {
        GuiLayout.apply(contents, plugin, new GuiLayout.GuiActions() {
            public void onHome(Player p)  { plugin.getMenuEngine().open(p, "admin/main"); }
            public void onWarps(Player p) { plugin.getMenuEngine().open(p, "warps"); }
            public void onBack(Player p)  { plugin.getMenuEngine().open(p, "admin/auth"); }
            public void onPrevPage(Player p) {}
            public void onNextPage(Player p) {}
        });

        List<User> users;
        try {
            users = plugin.getDatabaseManager().findAll();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error cargando usuarios", e);
            contents.set(2, 4, ClickableItem.empty(
                    makeItem(Material.BARRIER, "§cError cargando usuarios", List.of())));
            return;
        }

        Pagination pagination = contents.pagination();
        pagination.setItemsPerPage(GuiLayout.CONTENT_SLOTS.length);

        ClickableItem[] items = users.stream().map(user -> {
            ItemStack skull = buildSkull(user);
            return ClickableItem.of(skull, e -> {
                if (e.isLeftClick()) {
                    new UserDetailGUI(plugin, user).build().open(player);
                }
            });
        }).toArray(ClickableItem[]::new);

        pagination.setItems(items);

        SlotIterator iterator = contents.newIterator(SlotIterator.Type.HORIZONTAL, 1, 1);
        for (int row = 1; row <= 3; row++) {
            iterator.blacklist(row, 0);
            iterator.blacklist(row, 8);
        }
        pagination.addToIterator(iterator);

        // Número de página
        contents.set(4, 7, ClickableItem.empty(
                makeItem(Material.PAPER,
                        "§7Página §f" + (pagination.getPage() + 1),
                        List.of("§7Total: §f" + users.size() + " §7usuarios"))));

        // Botones de paginación
        if (!pagination.isFirst()) {
            contents.set(GuiLayout.SLOT_PREV_PAGE, ClickableItem.of(
                    GuiLayout.makeSkull(GuiLayout.getTexturePrev(), "§7§l« Anterior"),
                    e -> open(player, pagination.previous().getPage())
            ));
        }

        if (!pagination.isLast()) {
            contents.set(GuiLayout.SLOT_NEXT_PAGE, ClickableItem.of(
                    GuiLayout.makeSkull(GuiLayout.getTextureNext(), "§7§lSiguiente »"),
                    e -> open(player, pagination.next().getPage())
            ));
        }
    }

    @Override
    public void update(Player player, InventoryContents contents) {}

    // ── Builders ──────────────────────────────────────────────────────────────

    private ItemStack buildSkull(User user) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        // Intentar asignar skin del jugador si está online
        Player online = Bukkit.getPlayer(user.getUuid());
        if (online != null) meta.setOwningPlayer(online);
        else meta.setOwnerProfile(
                Bukkit.createPlayerProfile(user.getUuid(), user.getUsername()));

        meta.setDisplayName("§e§l" + user.getUsername());

        boolean isLogged = plugin.getAuthManager().isAuthenticated(user.getUuid());
        String lastLogin = user.getLastLogin() != null && user.getLastLogin().getEpochSecond() > 0
                ? FMT.format(user.getLastLogin()) : "§8Nunca";
        String registered = FMT.format(user.getRegisteredAt());
        String lastIp = user.getLastIp() != null ? user.getLastIp() : "§8Desconocida";
        String lastPos = user.hasLastLocation()
                ? user.getLastWorld() + " §8(" + fmt(user.getLastX()) + ", "
                  + fmt(user.getLastY()) + ", " + fmt(user.getLastZ()) + "§8)"
                : "§8Sin posición";

        meta.setLore(Arrays.asList(
                "§8━━━━━━━━━━━━━━━━━━━━━",
                "§7Estado:      " + (isLogged ? "§aEn línea" : "§cDesconectado"),
                "§7Registrado:  §f" + registered,
                "§7Último login: §f" + lastLogin,
                "§7Última IP:   §f" + lastIp,
                "§7Última pos:  §f" + lastPos,
                "§8━━━━━━━━━━━━━━━━━━━━━",
                "§eClick izq §7para ver detalle"
        ));

        skull.setItemMeta(meta);
        return skull;
    }

    private ItemStack makeItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String fmt(double v) { return String.format("%.1f", v); }
}