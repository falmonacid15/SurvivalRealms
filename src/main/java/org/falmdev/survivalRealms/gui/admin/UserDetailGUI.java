package org.falmdev.survivalRealms.gui.admin;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.falmdev.survivalRealms.SurvivalRealms;
import org.falmdev.survivalRealms.gui.GuiLayout;
import org.falmdev.survivalRealms.model.User;
import org.falmdev.survivalRealms.util.MessageUtil;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

public class UserDetailGUI implements InventoryProvider {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                    .withZone(ZoneId.systemDefault());

    private final SurvivalRealms plugin;
    private final User user;

    public UserDetailGUI(SurvivalRealms plugin, User user) {
        this.plugin = plugin;
        this.user   = user;
    }

    public SmartInventory build() {
        return SmartInventory.builder()
                .id("admin_user_detail_" + user.getUuid())
                .provider(this)
                .size(6, 9)
                .title("§8§lUsuario §7— §e" + user.getUsername())
                .manager(plugin.getInvManager())
                .build();
    }

    @Override
    public void init(Player player, InventoryContents contents) {
        GuiLayout.apply(contents, plugin, new GuiLayout.GuiActions() {
            public void onHome(Player p)     { plugin.getMenuEngine().open(p, "admin/main"); }
            public void onWarps(Player p)    { plugin.getMenuEngine().open(p, "warps"); }
            public void onBack(Player p)     { plugin.getUserListGUI().open(p, 0); }
            public void onPrevPage(Player p) {}
            public void onNextPage(Player p) {}
        });

        boolean isLogged   = plugin.getAuthManager().isAuthenticated(user.getUuid());
        boolean isOnline   = Bukkit.getPlayer(user.getUuid()) != null;
        String lastLogin   = user.getLastLogin() != null && user.getLastLogin().getEpochSecond() > 0
                ? FMT.format(user.getLastLogin()) : "Nunca";
        String registered  = FMT.format(user.getRegisteredAt());
        String lastIp      = user.getLastIp() != null ? user.getLastIp() : "Desconocida";

        // ── Col 4 — Cabecera: skull del jugador ───────────────────────────────
        contents.set(1, 4, ClickableItem.empty(buildSkull(isLogged)));

        // ── Col 1 — Info general ──────────────────────────────────────────────
        contents.set(1, 1, ClickableItem.empty(makeItem(Material.NAME_TAG,
                "§b§lInformación general",
                Arrays.asList(
                        "§7UUID: §f" + user.getUuid(),
                        "§7Username: §f" + user.getUsername(),
                        "§7Registrado: §f" + registered,
                        "§7Último login: §f" + lastLogin,
                        "§7Última IP: §f" + lastIp
                )
        )));

        // ── Col 2 — Última posición ───────────────────────────────────────────
        contents.set(1, 2, ClickableItem.empty(makeItem(Material.FILLED_MAP,
                "§a§lÚltima posición",
                user.hasLastLocation()
                        ? Arrays.asList(
                        "§7Mundo: §f" + user.getLastWorld(),
                        "§7X: §f" + fmt(user.getLastX()),
                        "§7Y: §f" + fmt(user.getLastY()),
                        "§7Z: §f" + fmt(user.getLastZ()),
                        "§7Yaw: §f" + fmt(user.getLastYaw()),
                        "§7Pitch: §f" + fmt(user.getLastPitch())
                )
                        : List.of("§8Sin posición guardada")
        )));

        // ── Col 3 — Estado de sesión ──────────────────────────────────────────
        contents.set(1, 3, ClickableItem.empty(makeItem(
                isLogged ? Material.LIME_DYE : Material.RED_DYE,
                "§f§lEstado",
                Arrays.asList(
                        "§7Autenticado: " + (isLogged ? "§aSí" : "§cNo"),
                        "§7Online:      " + (isOnline ? "§aSí" : "§cNo")
                )
        )));

        // ── Fila 3 — Acciones ─────────────────────────────────────────────────

        // Teleport al admin hacia la posición del usuario
        contents.set(3, 1, ClickableItem.of(
                makeItem(Material.ENDER_PEARL,
                        "§6§lTeleport a posición",
                        Arrays.asList(
                                "§7Te teleporta a la última",
                                "§7posición conocida del jugador.",
                                "",
                                user.hasLastLocation() ? "§eClick para teleportar" : "§cSin posición guardada"
                        )),
                e -> {
                    if (!user.hasLastLocation()) {
                        player.sendMessage(MessageUtil.color(
                                plugin.getMsg("messages.prefix") + "§cEste usuario no tiene posición guardada."));
                        return;
                    }
                    World world = Bukkit.getWorld(user.getLastWorld());
                    if (world == null) {
                        player.sendMessage(MessageUtil.color(
                                plugin.getMsg("messages.prefix") + "§cEl mundo no existe."));
                        return;
                    }
                    player.closeInventory();
                    player.teleport(new Location(world,
                            user.getLastX(), user.getLastY(), user.getLastZ(),
                            user.getLastYaw(), user.getLastPitch()));
                    player.sendMessage(MessageUtil.color(
                            plugin.getMsg("messages.prefix") + "§7Teleportado a la posición de §f" + user.getUsername()));
                }
        ));

        // Forzar logout (si está logueado)
        contents.set(3, 2, ClickableItem.of(
                makeItem(isLogged ? Material.ORANGE_DYE : Material.GRAY_DYE,
                        "§c§lForce Logout",
                        Arrays.asList(
                                "§7Cierra la sesión del jugador.",
                                "§7Deberá loguearse de nuevo.",
                                "",
                                isLogged ? "§eClick para ejecutar" : "§8El jugador no está logueado"
                        )),
                e -> {
                    if (!isLogged) return;
                    Player target = Bukkit.getPlayer(user.getUuid());
                    if (target != null) {
                        plugin.getAuthManager().logout(target);
                        target.sendMessage(MessageUtil.color(
                                plugin.getMsg("messages.prefix") + "§cTu sesión fue cerrada por un administrador."));
                    }
                    player.sendMessage(MessageUtil.color(
                            plugin.getMsg("messages.prefix") + "§7Sesión de §f" + user.getUsername() + " §7cerrada."));
                    // Refrescar la GUI
                    build().open(player);
                }
        ));

        // Kick si está online
        contents.set(3, 3, ClickableItem.of(
                makeItem(isOnline ? Material.PISTON : Material.GRAY_DYE,
                        "§e§lKick",
                        Arrays.asList(
                                "§7Expulsa al jugador del servidor.",
                                "",
                                isOnline ? "§eClick para ejecutar" : "§8El jugador no está online"
                        )),
                e -> {
                    if (!isOnline) return;
                    Player target = Bukkit.getPlayer(user.getUuid());
                    if (target != null) {
                        target.kickPlayer(MessageUtil.color("§cFuiste expulsado por un administrador."));
                    }
                    player.sendMessage(MessageUtil.color(
                            plugin.getMsg("messages.prefix") + "§f" + user.getUsername() + " §7fue expulsado."));
                    build().open(player);
                }
        ));

        // Eliminar cuenta
        contents.set(3, 5, ClickableItem.of(
                makeItem(Material.TNT,
                        "§4§lEliminar cuenta",
                        Arrays.asList(
                                "§cElimina permanentemente",
                                "§cla cuenta del jugador.",
                                "§4Esta acción no se puede deshacer.",
                                "",
                                "§cClick para ejecutar"
                        )),
                e -> {
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        try {
                            // Forzar logout si está logueado
                            Player target = Bukkit.getPlayer(user.getUuid());
                            if (target != null) {
                                plugin.getAuthManager().logout(target);
                                Bukkit.getScheduler().runTask(plugin, () ->
                                        target.kickPlayer(MessageUtil.color(
                                                "§cTu cuenta fue eliminada por un administrador.")));
                            }
                            plugin.getDatabaseManager().deleteUser(user.getUuid());
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                player.sendMessage(MessageUtil.color(
                                        plugin.getMsg("messages.prefix")
                                                + "§cCuenta de §f" + user.getUsername() + " §celiminada."));
                                plugin.getUserListGUI().open(player, 0);
                            });
                        } catch (Exception ex) {
                            plugin.getLogger().log(Level.WARNING, "Error eliminando usuario", ex);
                        }
                    });
                }
        ));
    }

    @Override
    public void update(Player player, InventoryContents contents) {}

    // ── Builders ──────────────────────────────────────────────────────────────

    private ItemStack buildSkull(boolean isLogged) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta  = (SkullMeta) skull.getItemMeta();
        Player online   = Bukkit.getPlayer(user.getUuid());
        if (online != null) meta.setOwningPlayer(online);
        else meta.setOwnerProfile(
                Bukkit.createPlayerProfile(user.getUuid(), user.getUsername()));
        meta.setDisplayName((isLogged ? "§a" : "§c") + "§l" + user.getUsername());
        meta.setLore(List.of(isLogged ? "§aAutenticado" : "§cNo autenticado"));
        skull.setItemMeta(meta);
        return skull;
    }

    private ItemStack makeItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String fmt(double v) { return String.format("%.1f", v); }
}