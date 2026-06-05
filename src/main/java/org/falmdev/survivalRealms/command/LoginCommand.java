package org.falmdev.survivalRealms.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.falmdev.survivalRealms.SurvivalRealms;
import org.falmdev.survivalRealms.manager.AuthManager;
import org.falmdev.survivalRealms.manager.AuthManager.LoginResult;
import org.falmdev.survivalRealms.util.MessageUtil;
import org.jetbrains.annotations.NotNull;

public class LoginCommand implements CommandExecutor {

    private final SurvivalRealms plugin;
    private final AuthManager    auth;

    public LoginCommand(SurvivalRealms plugin, AuthManager auth) {
        this.plugin = plugin;
        this.auth   = auth;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo jugadores.");
            return true;
        }

        String prefix = plugin.getMsg("messages.prefix");

        if (auth.isAuthenticated(player.getUniqueId())) {
            player.sendMessage(MessageUtil.color(prefix + plugin.getMsg("messages.already-logged-in")));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(MessageUtil.color(prefix + plugin.getMsg("messages.usage-login")));
            return true;
        }

        String password = args[0];

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            LoginResult result = auth.login(player, password);
            Bukkit.getScheduler().runTask(plugin, () -> {
                switch (result) {
                    case SUCCESS -> player.sendMessage(MessageUtil.color(prefix +
                            MessageUtil.replace(plugin.getMsg("messages.login-success"),
                                    "player", player.getName())));

                    case NOT_REGISTERED -> player.sendMessage(
                            MessageUtil.color(prefix + plugin.getMsg("messages.not-registered")));

                    case WRONG_PASSWORD -> player.sendMessage(MessageUtil.color(prefix +
                            MessageUtil.replace(plugin.getMsg("messages.login-wrong"),
                                    "attempts", String.valueOf(result.getRemainingAttempts()))));

                    case ERROR -> player.sendMessage(
                            MessageUtil.color(prefix + "&cError interno. Contacta un administrador."));

                    default -> {}
                }
            });
        });

        return true;
    }
}