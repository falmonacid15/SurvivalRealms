package org.falmdev.survivalRealms.presentation.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.falmdev.survivalRealms.application.auth.AuthService;
import org.falmdev.survivalRealms.application.auth.AuthService.RegisterResult;
import org.falmdev.survivalRealms.util.MessageUtil;
import org.jetbrains.annotations.NotNull;

public class RegisterCommand implements CommandExecutor {

    private final JavaPlugin  plugin;
    private final AuthService authService;

    public RegisterCommand(JavaPlugin plugin, AuthService authService) {
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
        int minLen = plugin.getConfig().getInt("auth.min-password-length", 6);
        int maxLen = plugin.getConfig().getInt("auth.max-password-length", 32);

        if (authService.isAuthenticated(player.getUniqueId())) {
            player.sendMessage(MessageUtil.color(prefix + plugin.getConfig().getString("messages.already-logged-in")));
            return true;
        }

        if (authService.isRegistered(player.getName())) {
            player.sendMessage(MessageUtil.color(prefix + plugin.getConfig().getString("messages.already-registered")));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(MessageUtil.color(prefix + plugin.getConfig().getString("messages.usage-register")));
            return true;
        }

        String password = args[0];
        String confirm  = args[1];

        if (password.length() < minLen) {
            player.sendMessage(MessageUtil.color(prefix +
                    MessageUtil.replace(plugin.getConfig().getString("messages.register-too-short"),
                            "min", String.valueOf(minLen))));
            return true;
        }

        if (password.length() > maxLen) {
            player.sendMessage(MessageUtil.color(prefix +
                    MessageUtil.replace(plugin.getConfig().getString("messages.register-too-long"),
                            "max", String.valueOf(maxLen))));
            return true;
        }

        if (!password.equals(confirm)) {
            player.sendMessage(MessageUtil.color(prefix + plugin.getConfig().getString("messages.register-mismatch")));
            return true;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            RegisterResult result = authService.register(player, password);
            Bukkit.getScheduler().runTask(plugin, () -> {
                switch (result) {
                    case SUCCESS -> player.sendMessage(MessageUtil.color(
                            prefix + plugin.getConfig().getString("messages.register-success")));
                    case ALREADY_REGISTERED -> player.sendMessage(MessageUtil.color(
                            prefix + plugin.getConfig().getString("messages.already-registered")));
                    case ERROR -> player.sendMessage(MessageUtil.color(
                            prefix + "&cError interno. Contacta un administrador."));
                }
            });
        });

        return true;
    }
}