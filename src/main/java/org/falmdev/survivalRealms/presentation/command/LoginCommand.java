package org.falmdev.survivalRealms.presentation.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.falmdev.survivalRealms.application.auth.AuthService;
import org.falmdev.survivalRealms.application.auth.AuthService.LoginResult;
import org.falmdev.survivalRealms.util.MessageUtil;
import org.jetbrains.annotations.NotNull;

public class LoginCommand implements CommandExecutor {

    private final JavaPlugin  plugin;
    private final AuthService authService;

    public LoginCommand(JavaPlugin plugin, AuthService authService) {
        this.plugin      = plugin;
        this.authService = authService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo jugadores.");
            return true;
        }

        String prefix = plugin.getConfig().getString("messages.prefix", "");

        if (authService.isAuthenticated(player.getUniqueId())) {
            player.sendMessage(MessageUtil.color(prefix + plugin.getConfig().getString("messages.already-logged-in")));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(MessageUtil.color(prefix + plugin.getConfig().getString("messages.usage-login")));
            return true;
        }

        String password = args[0];

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            LoginResult result = authService.login(player, password);
            Bukkit.getScheduler().runTask(plugin, () -> {
                switch (result) {
                    case SUCCESS -> player.sendMessage(MessageUtil.color(prefix +
                            MessageUtil.replace(plugin.getConfig().getString("messages.login-success"),
                                    "player", player.getName())));
                    case NOT_REGISTERED -> player.sendMessage(MessageUtil.color(
                            prefix + plugin.getConfig().getString("messages.not-registered")));
                    case WRONG_PASSWORD -> player.sendMessage(MessageUtil.color(prefix +
                            MessageUtil.replace(plugin.getConfig().getString("messages.login-wrong"),
                                    "attempts", String.valueOf(result.getRemainingAttempts()))));
                    case MAX_ATTEMPTS -> {}
                    case ERROR -> player.sendMessage(MessageUtil.color(
                            prefix + "&cError interno. Contacta un administrador."));
                }
            });
        });

        return true;
    }
}