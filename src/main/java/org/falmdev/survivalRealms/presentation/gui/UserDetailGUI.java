package org.falmdev.survivalRealms.presentation.gui;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.InventoryManager;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.falmdev.survivalRealms.application.auth.AuthService;
import org.falmdev.survivalRealms.domain.model.User;
import org.falmdev.survivalRealms.presentation.menu.MenuEngine;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class UserDetailGUI implements InventoryProvider {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final JavaPlugin       plugin;
    private final User             user;
    private final AuthService      authService;
    private final InventoryManager invManager;
    private final MenuEngine       menuEngine;
    private final UserListGUI      userListGUI;

    public UserDetailGUI(JavaPlugin plugin, User user, AuthService authService,
                         InventoryManager invManager, MenuEngine menuEngine,
                         UserListGUI userListGUI) {
        this.plugin      = plugin;
        this.user        = user;
        this.authService = authService;
        this.invManager  = invManager;
        this.menuEngine  = menuEngine;
        this.userListGUI = userListGUI;
    }

    public SmartInventory build() {
        return SmartInventory.builder()
                .id("admin_user_detail_" + user.getUuid())
                .provider(this)
                .size(6, 9)
                .title("§8§lUsuario §7— §e" + user.getUsername())
                .manager(invManager)
                .build();
    }

    @Override
    public void init(Player player, InventoryContents contents) {
        GuiLayout.apply(contents, new GuiLayout.GuiActions() {
            public void onHome(Player p)     { menuEngine.open(p, "admin/main"); }
            public void onWarps(Player p)    { menuEngine.open(p, "warps"); }
            public void onBack(Player p)     { userListGUI.open(p); }
            public void onPrevPage(Player p) {}
            public void onNextPage(Player p) {}
        });

        OfflinePlayer target = Bukkit.getOfflinePlayer(user.getUuid());

        boolean isLogged  = authService.isAuthenticated(user.getUuid());
        boolean isOnline  = Bukkit.getPlayer(user.getUuid()) != null;
        boolean isBanned  = Bukkit.getBanList(BanList.Type.NAME).isBanned(user.getUsername());
        boolean isOp      = target.isOp();
        boolean isWl      = target.isWhitelisted();

        String lastLogin  = user.getLastLogin() != null && user.getLastLogin().getEpochSecond() > 0
                ? FMT.format(user.getLastLogin()) : "Nunca";
        String registered = FMT.format(user.getRegisteredAt());
        String lastIp     = user.getLastIp() != null ? user.getLastIp() : "Desconocida";
        String lastPos    = user.getLastWorld() != null
                ? user.getLastWorld() + " (" + fmt(user.getLastX()) + ", "
                  + fmt(user.getLastY()) + ", " + fmt(user.getLastZ()) + ")"
                : "Sin posición";

        String kills        = PlaceholderAPI.setPlaceholders(target, "%statistic_player_kills%");
        String deaths       = PlaceholderAPI.setPlaceholders(target, "%statistic_deaths%");
        String playtime     = PlaceholderAPI.setPlaceholders(target, "%statistic_play_one_minute%");
        String blocksBroken = PlaceholderAPI.setPlaceholders(target, "%statistic_mine_block%");
        String jumps        = PlaceholderAPI.setPlaceholders(target, "%statistic_jump%");
        String firstJoin    = PlaceholderAPI.setPlaceholders(target, "%player_first_join_date%");

        // ── Slot 4 — Cabeza ───────────────────────────────────────────────────
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta  = (SkullMeta) skull.getItemMeta();
        Player online   = Bukkit.getPlayer(user.getUuid());
        if (online != null) meta.setOwningPlayer(online);
        else meta.setOwnerProfile(Bukkit.createPlayerProfile(user.getUuid(), user.getUsername()));
        meta.setDisplayName("§e§l" + user.getUsername());
        meta.setLore(Arrays.asList(
                "§8━━━━━━━━━━━━━━━━━━━━━",
                "§7UUID:         §f" + user.getUuid(),
                "§7Estado:       " + (isLogged  ? "§aAutenticado"   : "§cNo autenticado"),
                "§7Online:       " + (isOnline   ? "§aEn línea"     : "§cDesconectado"),
                "§7Baneado:      " + (isBanned   ? "§aSí"           : "§cNo"),
                "§7Whitelist:    " + (isWl        ? "§aSí"          : "§cNo"),
                "§7OP:           " + (isOp        ? "§aSí"          : "§cNo"),
                "§7Registrado:   §f" + registered,
                "§7Primer join:  §f" + firstJoin,
                "§7Último login: §f" + lastLogin,
                "§7Última IP:    §f" + lastIp,
                "§7Última pos:   §f" + lastPos,
                "§8━━━━━━━━━━━━━━━━━━━━━"
        ));
        skull.setItemMeta(meta);
        contents.set(0, 4, ClickableItem.empty(skull));

        // ── Fila 1 — Estadísticas ─────────────────────────────────────────────
        contents.set(1, 2, statItem(Material.DIAMOND_SWORD,   "§cKills",           kills,                    "§7Jugadores eliminados."));
        contents.set(1, 3, statItem(Material.SKELETON_SKULL,  "§7Muertes",         deaths,                   "§7Veces que ha muerto."));
        contents.set(1, 4, statItem(Material.CLOCK,           "§bTiempo de juego", formatPlaytime(playtime), "§7Tiempo total en el servidor."));
        contents.set(1, 5, statItem(Material.DIAMOND_PICKAXE, "§6Bloques rotos",   blocksBroken,             "§7Total de bloques minados."));
        contents.set(1, 6, statItem(Material.SLIME_BALL,      "§aSaltos",          jumps,                    "§7Veces que ha saltado."));

        // ── Fila 2 — Acciones de sesión y teleport ────────────────────────────
        contents.set(2, 1, tpButton(player));
        contents.set(2, 2, gamemodeButton(player));
        contents.set(2, 3, kickButton(player));
        contents.set(2, 4, logoutButton(player));
        contents.set(2, 5, healButton(player));
        contents.set(2, 6, feedButton(player));
        contents.set(2, 7, clearInventoryButton(player));

        // ── Fila 3 — Toggles de ban, whitelist ───────────────────────────────
        contents.set(3, 2, banToggle(player, isBanned));
        contents.set(3, 4, whitelistToggle(player, isWl));
        contents.set(3, 6, opToggle(player, isOp));

        // ── Fila 4 — Otros ────────────────────────────────────────────────────
        contents.set(4, 3, viewInventoryButton(player));
        contents.set(4, 5, flightButton(player));
    }

    @Override
    public void update(Player player, InventoryContents contents) {}

    // ── Estadísticas ──────────────────────────────────────────────────────────

    private ClickableItem statItem(Material material, String title, String value, String description) {
        return ClickableItem.empty(
                GuiLayout.makeItem(material, title, "§f" + value, "", description));
    }

    private String formatPlaytime(String rawTicks) {
        try {
            long ticks   = Long.parseLong(rawTicks.trim());
            long seconds = ticks / 20;
            long hours   = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            return hours + "h " + minutes + "m";
        } catch (NumberFormatException e) {
            return rawTicks;
        }
    }

    // ── Toggles ───────────────────────────────────────────────────────────────

    private ClickableItem banToggle(Player admin, boolean isBanned) {
        Material mat  = isBanned ? Material.LIME_DYE   : Material.IRON_BARS;
        String   name = isBanned ? "§aDesbanear jugador" : "§4Banear jugador";
        String   desc = isBanned
                ? "§7El jugador está §4baneado§7.\n§eClick para desbanear."
                : "§7El jugador §ano está baneado§7.\n§eClick para banear.";
        String estado = isBanned ? "§4Baneado" : "§aLibre";

        return ClickableItem.of(
                GuiLayout.makeItem(mat, name,
                        "§7Estado actual: " + estado, "", desc,
                        "", isBanned ? "" : "§4¡Esta acción es permanente!"),
                e -> {
                    if (isBanned) {
                        Bukkit.getBanList(BanList.Type.NAME).pardon(user.getUsername());
                        admin.sendMessage("§aJugador §f" + user.getUsername() + " §adesbaneado.");
                    } else {
                        Bukkit.getBanList(BanList.Type.NAME).addBan(
                                user.getUsername(), "Baneado por un administrador.",
                                (java.util.Date) null, admin.getName());
                        Player t = Bukkit.getPlayer(user.getUuid());
                        if (t != null) t.kickPlayer("§4Has sido baneado del servidor.");
                        admin.sendMessage("§aJugador §f" + user.getUsername() + " §abaneado.");
                    }
                    refresh(admin);
                });
    }

    private ClickableItem whitelistToggle(Player admin, boolean isWl) {
        Material mat  = isWl ? Material.WRITABLE_BOOK : Material.BOOK;
        String   name = isWl ? "§cQuitar de whitelist" : "§aAgregar a whitelist";
        String estado = isWl ? "§aEn whitelist" : "§cFuera de whitelist";

        return ClickableItem.of(
                GuiLayout.makeItem(mat, name,
                        "§7Estado actual: " + estado,
                        "", "§eClick para " + (isWl ? "quitar." : "agregar.")),
                e -> {
                    Bukkit.getOfflinePlayer(user.getUuid()).setWhitelisted(!isWl);
                    admin.sendMessage("§f" + user.getUsername()
                            + (isWl ? " §cquitado de la whitelist." : " §aagregado a la whitelist."));
                    refresh(admin);
                });
    }

    private ClickableItem opToggle(Player admin, boolean isOp) {
        Material mat  = isOp ? Material.COAL : Material.NETHER_STAR;
        String   name = isOp ? "§cQuitar OP" : "§6Dar OP";
        String estado = isOp ? "§aEs OP" : "§cNo es OP";

        return ClickableItem.of(
                GuiLayout.makeItem(mat, name,
                        "§7Estado actual: " + estado,
                        "", "§eClick para " + (isOp ? "quitar OP." : "dar OP."),
                        isOp ? "" : "§4¡Úsalo con precaución!"),
                e -> {
                    Bukkit.getOfflinePlayer(user.getUuid()).setOp(!isOp);
                    admin.sendMessage(isOp
                            ? "§aOP revocado a §f" + user.getUsername()
                            : "§aOP otorgado a §f" + user.getUsername());
                    refresh(admin);
                });
    }

    // ── Refresh — reabre la GUI para reflejar el nuevo estado ─────────────────

    private void refresh(Player admin) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> build().open(admin), 1L);
    }

    // ── Botones de acción ─────────────────────────────────────────────────────

    private ClickableItem tpButton(Player admin) {
        return ClickableItem.of(
                GuiLayout.makeItem(Material.ENDER_PEARL, "§bTeletransportar",
                        "§7Ir a la última posición", "§7conocida del jugador.",
                        "", "§eClick para teletransportar."),
                e -> {
                    if (user.getLastWorld() == null) {
                        admin.sendMessage("§cEste jugador no tiene posición guardada.");
                        return;
                    }
                    World world = Bukkit.getWorld(user.getLastWorld());
                    if (world == null) { admin.sendMessage("§cEl mundo no existe."); return; }
                    admin.teleport(new Location(world,
                            user.getLastX(), user.getLastY(), user.getLastZ(),
                            user.getLastYaw(), user.getLastPitch()));
                    admin.closeInventory();
                    admin.sendMessage("§aTeletransportado a la última posición de §f" + user.getUsername());
                });
    }

    private ClickableItem gamemodeButton(Player admin) {
        return ClickableItem.of(
                GuiLayout.makeItem(Material.COMMAND_BLOCK, "§6Cambiar gamemode",
                        "§7Click izq:  §fSurvival",
                        "§7Click der:  §fCreative",
                        "§7Shift+izq:  §fAdventure",
                        "§7Shift+der:  §fSpectator",
                        "", "§eRequiere que esté en línea."),
                e -> {
                    Player t = Bukkit.getPlayer(user.getUuid());
                    if (t == null) { admin.sendMessage("§cJugador no está en línea."); return; }
                    GameMode gm;
                    if      (e.isShiftClick() && e.isLeftClick())  gm = GameMode.ADVENTURE;
                    else if (e.isShiftClick() && e.isRightClick()) gm = GameMode.SPECTATOR;
                    else if (e.isRightClick())                     gm = GameMode.CREATIVE;
                    else                                           gm = GameMode.SURVIVAL;
                    t.setGameMode(gm);
                    admin.sendMessage("§aGamemode de §f" + user.getUsername() + " §acambiado a §f" + gm.name());
                });
    }

    private ClickableItem kickButton(Player admin) {
        return ClickableItem.of(
                GuiLayout.makeItem(Material.LEATHER_BOOTS, "§eKickear",
                        "§7Desconectar al jugador del servidor.",
                        "", "§eRequiere que esté en línea."),
                e -> {
                    Player t = Bukkit.getPlayer(user.getUuid());
                    if (t == null) { admin.sendMessage("§cJugador no está en línea."); return; }
                    t.kickPlayer("§cExpulsado por un administrador.");
                    admin.sendMessage("§aJugador §f" + user.getUsername() + " §akickeado.");
                    admin.closeInventory();
                });
    }

    private ClickableItem logoutButton(Player admin) {
        return ClickableItem.of(
                GuiLayout.makeItem(Material.IRON_DOOR, "§cCerrar sesión",
                        "§7Fuerza el cierre de sesión", "§7del jugador.",
                        "", "§eRequiere que esté en línea."),
                e -> {
                    Player t = Bukkit.getPlayer(user.getUuid());
                    if (t == null) { admin.sendMessage("§cJugador no está en línea."); return; }
                    authService.logout(t);
                    admin.sendMessage("§aSesión cerrada para §f" + user.getUsername());
                });
    }

    private ClickableItem healButton(Player admin) {
        return ClickableItem.of(
                GuiLayout.makeItem(Material.GOLDEN_APPLE, "§cCurar jugador",
                        "§7Restaura vida y satura al jugador.",
                        "", "§eRequiere que esté en línea."),
                e -> {
                    Player t = Bukkit.getPlayer(user.getUuid());
                    if (t == null) { admin.sendMessage("§cJugador no está en línea."); return; }
                    t.setHealth(t.getMaxHealth());
                    t.setFoodLevel(20);
                    t.setSaturation(20f);
                    admin.sendMessage("§aJugador §f" + user.getUsername() + " §acurado.");
                });
    }

    private ClickableItem feedButton(Player admin) {
        return ClickableItem.of(
                GuiLayout.makeItem(Material.COOKED_BEEF, "§6Alimentar jugador",
                        "§7Restaura el hambre del jugador.",
                        "", "§eRequiere que esté en línea."),
                e -> {
                    Player t = Bukkit.getPlayer(user.getUuid());
                    if (t == null) { admin.sendMessage("§cJugador no está en línea."); return; }
                    t.setFoodLevel(20);
                    t.setSaturation(20f);
                    admin.sendMessage("§aJugador §f" + user.getUsername() + " §aalimentado.");
                });
    }

    private ClickableItem clearInventoryButton(Player admin) {
        return ClickableItem.of(
                GuiLayout.makeItem(Material.TNT, "§cLimpiar inventario",
                        "§7Elimina todos los ítems", "§7del inventario del jugador.",
                        "", "§4¡Esta acción no se puede deshacer!"),
                e -> {
                    Player t = Bukkit.getPlayer(user.getUuid());
                    if (t == null) { admin.sendMessage("§cJugador no está en línea."); return; }
                    t.getInventory().clear();
                    admin.sendMessage("§aInventario de §f" + user.getUsername() + " §alimpiado.");
                });
    }

    private ClickableItem viewInventoryButton(Player admin) {
        return ClickableItem.of(
                GuiLayout.makeItem(Material.CHEST, "§bVer inventario",
                        "§7Abre el inventario del jugador.",
                        "", "§eRequiere que esté en línea."),
                e -> {
                    Player t = Bukkit.getPlayer(user.getUuid());
                    if (t == null) { admin.sendMessage("§cJugador no está en línea."); return; }
                    admin.closeInventory();
                    Bukkit.getScheduler().runTaskLater(plugin, () ->
                            admin.openInventory(t.getInventory()), 1L);
                });
    }

    private ClickableItem flightButton(Player admin) {
        return ClickableItem.of(
                GuiLayout.makeItem(Material.FEATHER, "§bToggle vuelo",
                        "§7Activa o desactiva el vuelo del jugador.",
                        "", "§eRequiere que esté en línea."),
                e -> {
                    Player t = Bukkit.getPlayer(user.getUuid());
                    if (t == null) { admin.sendMessage("§cJugador no está en línea."); return; }
                    t.setAllowFlight(!t.getAllowFlight());
                    t.setFlying(t.getAllowFlight());
                    String estado = t.getAllowFlight() ? "§aactivado" : "§cdesactivado";
                    admin.sendMessage("§aVuelo " + estado + " §apara §f" + user.getUsername());
                });
    }

    private String fmt(double v) { return String.format("%.1f", v); }
}