package org.falmdev.survivalRealms.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.falmdev.survivalRealms.SurvivalRealms;
import org.falmdev.survivalRealms.manager.AuthManager;
import org.falmdev.survivalRealms.util.MessageUtil;
import org.jetbrains.annotations.NotNull;

public class RegisterCommand implements CommandExecutor {

    private final SurvivalRealms plugin;
    private final AuthManager    auth;

    public RegisterCommand(SurvivalRealms plugin, AuthManager auth) {
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

        if (auth.isRegistered(player.getName())) {
            player.sendMessage(MessageUtil.color(prefix + plugin.getMsg("messages.already-registered")));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(MessageUtil.color(prefix + plugin.getMsg("messages.usage-register")));
            return true;
        }

        String password = args[0];
        String confirm  = args[1];
        int minLen = plugin.getConfig().getInt("auth.min-password-length", 6);
        int maxLen = plugin.getConfig().getInt("auth.max-password-length", 32);

        if (password.length() < minLen) {
            player.sendMessage(MessageUtil.color(prefix +
                    MessageUtil.replace(plugin.getMsg("messages.register-too-short"), "min", String.valueOf(minLen))));
            return true;
        }
        if (password.length() > maxLen) {
            player.sendMessage(MessageUtil.color(prefix +
                    MessageUtil.replace(plugin.getMsg("messages.register-too-long"), "max", String.valueOf(maxLen))));
            return true;
        }
        if (!password.equals(confirm)) {
            player.sendMessage(MessageUtil.color(prefix + plugin.getMsg("messages.register-mismatch")));
            return true;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean ok = auth.register(player, password);
            Bukkit.getScheduler().runTask(plugin, () ->
                    player.sendMessage(MessageUtil.color(prefix + plugin.getMsg(
                            ok ? "messages.register-success" : "messages.already-registered")))
            );
        });

        return true;
    }
}