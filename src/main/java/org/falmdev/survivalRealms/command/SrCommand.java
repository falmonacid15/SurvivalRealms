package org.falmdev.survivalRealms.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.falmdev.survivalRealms.SurvivalRealms;
import org.falmdev.survivalRealms.manager.SpawnManager;
import org.falmdev.survivalRealms.model.Warp;
import org.falmdev.survivalRealms.util.MessageUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SrCommand implements CommandExecutor, TabCompleter {

    private final SurvivalRealms plugin;
    private final SpawnManager   spawnManager;

    private static final String PERM_ADMIN          = "survivalrealms.admin";
    private static final String PERM_SET_AUTH_SPAWN = "survivalrealms.command.setauthspawn";
    private static final String PERM_SET_MAIN_SPAWN = "survivalrealms.command.setmainspawn";
    private static final String PERM_RELOAD         = "survivalrealms.command.reload";
    private static final String PERM_INFO           = "survivalrealms.command.info";

    public SrCommand(SurvivalRealms plugin, SpawnManager spawnManager) {
        this.plugin       = plugin;
        this.spawnManager = spawnManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        String prefix = plugin.getMsg("messages.prefix");

        if (args.length == 0) {
            sendHelp(sender, prefix);
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "setauthspawn" -> {
                if (!hasAnyPerm(sender, PERM_ADMIN, PERM_SET_AUTH_SPAWN)) {
                    sender.sendMessage(MessageUtil.color(prefix + plugin.getMsg("messages.no-permission")));
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Solo un jugador puede usar este comando.");
                    return true;
                }
                spawnManager.setAuthSpawn(player.getLocation());
                sender.sendMessage(MessageUtil.color(prefix + plugin.getMsg("messages.auth-spawn-set")));
            }

            case "setmainspawn" -> {
                if (!hasAnyPerm(sender, PERM_ADMIN, PERM_SET_MAIN_SPAWN)) {
                    sender.sendMessage(MessageUtil.color(prefix + plugin.getMsg("messages.no-permission")));
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Solo un jugador puede usar este comando.");
                    return true;
                }
                spawnManager.setMainSpawn(player.getLocation());
                sender.sendMessage(MessageUtil.color(prefix + plugin.getMsg("messages.main-spawn-set")));
            }

            case "gui" -> {
                if (!hasAnyPerm(sender, PERM_ADMIN)) {
                    sender.sendMessage(MessageUtil.color(
                            prefix + plugin.getMsg("messages.no-permission")));
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Solo jugadores.");
                    return true;
                }
                plugin.getMenuEngine().open(player, "admin/main");
            }

            case "reload" -> {
                if (!hasAnyPerm(sender, PERM_ADMIN, PERM_RELOAD)) {
                    sender.sendMessage(MessageUtil.color(prefix + plugin.getMsg("messages.no-permission")));
                    return true;
                }
                plugin.reloadConfig();
                plugin.getMenuEngine().reload();
                sender.sendMessage(MessageUtil.color(prefix + plugin.getMsg("messages.reload-success")));
            }

            case "info" -> {
                if (!hasAnyPerm(sender, PERM_ADMIN, PERM_INFO)) {
                    sender.sendMessage(MessageUtil.color(prefix + plugin.getMsg("messages.no-permission")));
                    return true;
                }
                sendInfo(sender, prefix);
            }

            case "warp" -> {
                if (args.length < 2) {
                    sendWarpHelp(sender, prefix);
                    return true;
                }
                switch (args[1].toLowerCase()) {
                    case "create", "add" -> {
                        if (!hasAnyPerm(sender, PERM_ADMIN)) {
                            sender.sendMessage(MessageUtil.color(prefix + plugin.getMsg("messages.no-permission")));
                            return true;
                        }
                        if (!(sender instanceof Player player)) {
                            sender.sendMessage("Solo jugadores.");
                            return true;
                        }
                        if (args.length < 3) {
                            sender.sendMessage(MessageUtil.color(prefix + "&cUso: /sr warp create <nombre> [icono]"));
                            return true;
                        }
                        String warpName = args[2];
                        String icon = args.length >= 4 ? args[3].toUpperCase() : "GRASS_BLOCK";
                        boolean created = plugin.getWarpManager()
                                .createWarp(warpName, player.getLocation(), player.getUniqueId(), icon);
                        if (created) {
                            sender.sendMessage(MessageUtil.color(prefix + "&aWarp &f" + warpName + " &acreado."));
                        } else {
                            sender.sendMessage(MessageUtil.color(prefix + "&cYa existe un warp con ese nombre."));
                        }
                    }
                    case "delete", "remove" -> {
                        if (!hasAnyPerm(sender, PERM_ADMIN)) {
                            sender.sendMessage(MessageUtil.color(prefix + plugin.getMsg("messages.no-permission")));
                            return true;
                        }
                        if (args.length < 3) {
                            sender.sendMessage(MessageUtil.color(prefix + "&cUso: /sr warp delete <nombre>"));
                            return true;
                        }
                        boolean deleted = plugin.getWarpManager().deleteWarp(args[2]);
                        if (deleted) {
                            sender.sendMessage(MessageUtil.color(prefix + "&aWarp &f" + args[2] + " &aeliminado."));
                        } else {
                            sender.sendMessage(MessageUtil.color(prefix + "&cNo existe ese warp."));
                        }
                    }
                    case "list" -> {
                        if (!(sender instanceof Player player)) {
                            plugin.getWarpManager().getAllWarps().forEach(w ->
                                    sender.sendMessage("&7- &f" + w.getName() + " &8(" + w.getWorld() + ")"));
                            return true;
                        }
                        var warps = plugin.getWarpManager().getWarpsForPlayer(player);
                        int limit = plugin.getWarpManager().getLimitForPlayer(player);
                        sender.sendMessage(MessageUtil.color(prefix + "&7Warps disponibles &8(&f"
                                + warps.size() + (limit < 0 ? "" : "/" + limit) + "&8):"));
                        warps.forEach(w -> sender.sendMessage(
                                MessageUtil.color("  &8• &f" + w.getName()
                                        + " &8— &7" + w.getWorld())));
                    }
                    case "tp", "teleport" -> {
                        if (!(sender instanceof Player player)) {
                            sender.sendMessage("Solo jugadores.");
                            return true;
                        }
                        if (args.length < 3) {
                            sender.sendMessage(MessageUtil.color(prefix + "&cUso: /sr warp tp <nombre>"));
                            return true;
                        }
                        plugin.getWarpManager().teleportByName(player, args[2]);
                    }
                    default -> sendWarpHelp(sender, prefix);
                }
            }

            default -> sendHelp(sender, prefix);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("setauthspawn", "setmainspawn", "reload", "info", "gui", "warp")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("warp")) {
            return List.of("create", "delete", "list", "tp")
                    .stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("warp")
                && (args[1].equalsIgnoreCase("delete") || args[1].equalsIgnoreCase("tp"))) {
            return plugin.getWarpManager().getAllWarps()
                    .stream()
                    .map(Warp::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                    .toList();
        }
        return List.of();
    }

    private void sendHelp(CommandSender sender, String prefix) {
        sender.sendMessage(MessageUtil.color(prefix + "&7Comandos disponibles:"));
        sender.sendMessage(MessageUtil.color("&8  /sr setauthspawn &7— Setea el spawn de autenticación"));
        sender.sendMessage(MessageUtil.color("&8  /sr setmainspawn &7— Setea el spawn principal"));
        sender.sendMessage(MessageUtil.color("&8  /sr reload       &7— Recarga la configuración"));
        sender.sendMessage(MessageUtil.color("&8  /sr info         &7— Muestra información del plugin"));
    }

    private void sendInfo(CommandSender sender, String prefix) {
        String version = plugin.getDescription().getVersion();
        String dbType  = plugin.getConfig().getString("database.type", "sqlite").toUpperCase();
        boolean authSpawnSet = spawnManager.hasAuthSpawn();
        boolean mainSpawnSet = spawnManager.hasMainSpawn();

        sender.sendMessage(MessageUtil.color(prefix + "&6SurvivalRealms v" + version));
        sender.sendMessage(MessageUtil.color("&8  Base de datos &7» &f" + dbType));
        sender.sendMessage(MessageUtil.color("&8  Auth spawn    &7» "
                + (authSpawnSet ? "&aConfigurado" : "&cNo configurado")));
        sender.sendMessage(MessageUtil.color("&8  Main spawn    &7» "
                + (mainSpawnSet ? "&aConfigurado" : "&cNo configurado")));
        sender.sendMessage(MessageUtil.color("&8  LuckPerms     &7» "
                + (plugin.getLuckPermsManager().isAvailable() ? "&aConectado" : "&cNo disponible")));
    }

    private void sendWarpHelp(CommandSender sender, String prefix) {
        sender.sendMessage(MessageUtil.color(prefix + "&7Comandos de warps:"));
        sender.sendMessage(MessageUtil.color("  &8/sr warp create &f<nombre> [icono] &7— Crear warp"));
        sender.sendMessage(MessageUtil.color("  &8/sr warp delete &f<nombre>          &7— Eliminar warp"));
        sender.sendMessage(MessageUtil.color("  &8/sr warp list                       &7— Listar warps"));
        sender.sendMessage(MessageUtil.color("  &8/sr warp tp &f<nombre>              &7— Teletransportarse"));
    }

    private boolean hasAnyPerm(CommandSender sender, String... perms) {
        for (String perm : perms) {
            if (sender.hasPermission(perm)) return true;
        }
        return false;
    }
}