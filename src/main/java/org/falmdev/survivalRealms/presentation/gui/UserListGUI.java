package org.falmdev.survivalRealms.presentation.gui;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.InventoryManager;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import fr.minuskube.inv.content.Pagination;
import fr.minuskube.inv.content.SlotIterator;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.falmdev.survivalRealms.application.auth.AuthService;
import org.falmdev.survivalRealms.application.user.UserService;
import org.falmdev.survivalRealms.domain.model.User;
import org.falmdev.survivalRealms.presentation.menu.MenuEngine;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class UserListGUI implements InventoryProvider {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final JavaPlugin       plugin;
    private final UserService      userService;
    private final AuthService      authService;
    private final InventoryManager invManager;
    private final MenuEngine       menuEngine;

    public UserListGUI(JavaPlugin plugin, UserService userService,
                       AuthService authService, InventoryManager invManager,
                       MenuEngine menuEngine) {
        this.plugin      = plugin;
        this.userService = userService;
        this.authService = authService;
        this.invManager  = invManager;
        this.menuEngine  = menuEngine;
    }

    public void open(Player player) {
        SmartInventory.builder()
                .id("admin_user_list")
                .provider(this)
                .size(6, 9)
                .title("§8§lAdmin §7— §dUsuarios")
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

        List<User> users = userService.findAll();

        Pagination pagination = contents.pagination();
        pagination.setItemsPerPage(21);

        ClickableItem[] items = users.stream().map(user -> {
            ItemStack skull = buildSkull(user);
            return ClickableItem.of(skull, e -> {
                if (e.isLeftClick()) {
                    new UserDetailGUI(plugin, user, authService, invManager, menuEngine, this)
                            .build().open(player);
                }
            });
        }).toArray(ClickableItem[]::new);

        pagination.setItems(items);

        SlotIterator iterator = contents.newIterator(SlotIterator.Type.HORIZONTAL, 1, 1);
        for (int row = 1; row <= 3; row++) {
            iterator.blacklist(row, 0);
            iterator.blacklist(row, 8);
        }
        iterator.blacklist(4, 0);
        iterator.blacklist(4, 1);
        iterator.blacklist(4, 2);
        iterator.blacklist(4, 3);
        iterator.blacklist(4, 4);
        iterator.blacklist(4, 5);
        iterator.blacklist(4, 6);
        iterator.blacklist(4, 7);
        iterator.blacklist(4, 8);
        pagination.addToIterator(iterator);

        contents.set(4, 4, ClickableItem.empty(
                GuiLayout.makeItem(Material.PAPER,
                        "§7Página §f" + (pagination.getPage() + 1),
                        "§7Total: §f" + users.size() + " §7usuarios")));

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

    private ItemStack buildSkull(User user) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta  = (SkullMeta) skull.getItemMeta();

        Player online = Bukkit.getPlayer(user.getUuid());
        if (online != null) meta.setOwningPlayer(online);
        else meta.setOwnerProfile(
                Bukkit.createPlayerProfile(user.getUuid(), user.getUsername()));

        meta.setDisplayName("§e§l" + user.getUsername());

        boolean isLogged  = authService.isAuthenticated(user.getUuid());
        String lastLogin  = user.getLastLogin() != null && user.getLastLogin().getEpochSecond() > 0
                ? FMT.format(user.getLastLogin()) : "§8Nunca";
        String registered = FMT.format(user.getRegisteredAt());
        String lastIp     = user.getLastIp() != null ? user.getLastIp() : "§8Desconocida";
        String lastPos    = user.getLastWorld() != null
                ? user.getLastWorld() + " §8(" + fmt(user.getLastX()) + ", "
                  + fmt(user.getLastY()) + ", " + fmt(user.getLastZ()) + "§8)"
                : "§8Sin posición";

        meta.setLore(Arrays.asList(
                "§8━━━━━━━━━━━━━━━━━━━━━",
                "§7Estado:       " + (isLogged ? "§aEn línea" : "§cDesconectado"),
                "§7Registrado:   §f" + registered,
                "§7Último login: §f" + lastLogin,
                "§7Última IP:    §f" + lastIp,
                "§7Última pos:   §f" + lastPos,
                "§8━━━━━━━━━━━━━━━━━━━━━",
                "§eClick izq §7para ver detalle"
        ));

        skull.setItemMeta(meta);
        return skull;
    }

    private String fmt(double v) { return String.format("%.1f", v); }
}