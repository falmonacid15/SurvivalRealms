package org.falmdev.survivalRealms.presentation.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.falmdev.survivalRealms.application.spawn.SpawnService;
import org.falmdev.survivalRealms.application.warp.WarpService;
import org.falmdev.survivalRealms.domain.model.Warp;
import org.falmdev.survivalRealms.presentation.menu.MenuEngine;
import org.falmdev.survivalRealms.util.MessageUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SrCommand implements CommandExecutor, TabCompleter {

    private static final String PERM_ADMIN          = "survivalrealms.admin";
    private static final String PERM_SET_AUTH_SPAWN = "survivalrealms.command.setauthspawn";
    private static final String PERM_SET_MAIN_SPAWN = "survivalrealms.command.setmainspawn";
    private static final String PERM_RELOAD         = "survivalrealms.command.reload";
    private static final String PERM_INFO           = "survivalrealms.command.info";
    private static final String PERM_GUI            = "survivalrealms.command.gui";

    private final JavaPlugin   plugin;
    private final SpawnService spawnService;
    private final WarpService  warpService;
    private final MenuEngine   menuEngine;

    public SrCommand(JavaPlugin plugin, SpawnService spawnService,
                     WarpService warpService, MenuEngine menuEngine) {
        this.plugin       = plugin;
        this.spawnService = spawnService;
        this.warpService  = warpService;
        this.menuEngine   = menuEngine;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        String prefix = plugin.getConfig().getString("messages.prefix", "");

        if (args.length == 0) {
            sendHelp(sender, prefix);
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "setauthspawn" -> {
                if (!hasAnyPerm(sender, PERM_ADMIN, PERM_SET_AUTH_SPAWN)) {
                    sender.sendMessage(MessageUtil.color(prefix + plugin.getConfig().getString("messages.no-permission")));
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Solo jugadores.");
                    return true;
                }
                spawnService.setAuthSpawn(player.getLocation());
                sender.sendMessage(MessageUtil.color(prefix + plugin.getConfig().getString("messages.auth-spawn-set")));
            }

            case "setmainspawn" -> {
                if (!hasAnyPerm(sender, PERM_ADMIN, PERM_SET_MAIN_SPAWN)) {
                    sender.sendMessage(MessageUtil.color(prefix + plugin.getConfig().getString("messages.no-permission")));
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Solo jugadores.");
                    return true;
                }
                spawnService.setMainSpawn(player.getLocation());
                sender.sendMessage(MessageUtil.color(prefix + plugin.getConfig().getString("messages.main-spawn-set")));
            }

            case "reload" -> {
                if (!hasAnyPerm(sender, PERM_ADMIN, PERM_RELOAD)) {
                    sender.sendMessage(MessageUtil.color(prefix + plugin.getConfig().getString("messages.no-permission")));
                    return true;
                }
                plugin.reloadConfig();
                menuEngine.reload();
                sender.sendMessage(MessageUtil.color(prefix + plugin.getConfig().getString("messages.reload-success")));
            }

            case "gui" -> {
                if (!hasAnyPerm(sender, PERM_ADMIN, PERM_GUI)) {
                    sender.sendMessage(MessageUtil.color(prefix + plugin.getConfig().getString("messages.no-permission")));
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Solo jugadores.");
                    return true;
                }
                menuEngine.open(player, "admin/main");
            }

            case "warp" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Solo jugadores.");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(MessageUtil.color(prefix + "&cUso: /sr warp <create|delete|list|tp> [nombre]"));
                    return true;
                }
                switch (args[1].toLowerCase()) {
                    case "create" -> {
                        if (args.length < 3) {
                            player.sendMessage(MessageUtil.color(prefix + "&cUso: /sr warp create <nombre>"));
                            return true;
                        }
                        boolean ok = warpService.createWarp(args[2], player.getLocation(),
                                player.getUniqueId(), "GRASS_BLOCK");
                        player.sendMessage(MessageUtil.color(prefix + (ok
                                ? "&aWarp &f" + args[2] + " &acreado."
                                : "&cYa existe un warp con ese nombre.")));
                    }
                    case "delete" -> {
                        if (args.length < 3) {
                            player.sendMessage(MessageUtil.color(prefix + "&cUso: /sr warp delete <nombre>"));
                            return true;
                        }
                        boolean ok = warpService.deleteWarp(args[2]);
                        player.sendMessage(MessageUtil.color(prefix + (ok
                                ? "&aWarp &f" + args[2] + " &aeliminado."
                                : "&cWarp no encontrado.")));
                    }
                    case "tp" -> {
                        if (args.length < 3) {
                            player.sendMessage(MessageUtil.color(prefix + "&cUso: /sr warp tp <nombre>"));
                            return true;
                        }
                        boolean ok = warpService.teleportToWarp(player, args[2]);
                        if (!ok) player.sendMessage(MessageUtil.color(prefix + "&cWarp no encontrado."));
                    }
                    case "list" -> {
                        var warps = warpService.getAllWarps();
                        if (warps.isEmpty()) {
                            player.sendMessage(MessageUtil.color(prefix + "&7No hay warps registrados."));
                            return true;
                        }
                        player.sendMessage(MessageUtil.color(prefix + "&6Warps &7(" + warps.size() + "):"));
                        warps.forEach(w -> player.sendMessage(MessageUtil.color("&7- &f" + w.getName())));
                    }
                    default -> player.sendMessage(MessageUtil.color(prefix + "&cUso: /sr warp <create|delete|list|tp> [nombre]"));
                }
            }

            case "info" -> {
                if (!hasAnyPerm(sender, PERM_ADMIN, PERM_INFO)) {
                    sender.sendMessage(MessageUtil.color(prefix + plugin.getConfig().getString("messages.no-permission")));
                    return true;
                }
                sender.sendMessage(MessageUtil.color("&6SurvivalRealms &7v" + plugin.getDescription().getVersion()));
                sender.sendMessage(MessageUtil.color("&7Warps registrados: &f" + warpService.getAllWarps().size()));
                sender.sendMessage(MessageUtil.color("&7Auth-spawn: &f" + (spawnService.hasAuthSpawn() ? "Configurado" : "No configurado")));
                sender.sendMessage(MessageUtil.color("&7Main-spawn: &f" + (spawnService.hasMainSpawn() ? "Configurado" : "No configurado")));
            }

            default -> sendHelp(sender, prefix);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("setauthspawn", "setmainspawn", "reload", "gui", "warp", "info")
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
            return warpService.getAllWarps().stream()
                    .map(Warp::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                    .toList();
        }

        return List.of();
    }

    private void sendHelp(CommandSender sender, String prefix) {
        sender.sendMessage(MessageUtil.color(prefix + "&6SurvivalRealms &7— Comandos:"));
        sender.sendMessage(MessageUtil.color("&7/sr setauthspawn &8— &fEstablece el spawn de autenticación"));
        sender.sendMessage(MessageUtil.color("&7/sr setmainspawn &8— &fEstablece el spawn principal"));
        sender.sendMessage(MessageUtil.color("&7/sr warp <nombre> &8— &fTeletransporta a un warp"));
        sender.sendMessage(MessageUtil.color("&7/sr reload &8— &fRecarga la configuración"));
        sender.sendMessage(MessageUtil.color("&7/sr gui &8— &fAbre el panel de administración"));
        sender.sendMessage(MessageUtil.color("&7/sr info &8— &fMuestra información del plugin"));
    }

    private boolean hasAnyPerm(CommandSender sender, String... perms) {
        for (String perm : perms) {
            if (sender.hasPermission(perm)) return true;
        }
        return false;
    }
}