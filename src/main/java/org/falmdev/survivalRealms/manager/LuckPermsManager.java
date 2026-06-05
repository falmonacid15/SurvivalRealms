package org.falmdev.survivalRealms.manager;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.falmdev.survivalRealms.SurvivalRealms;

import java.util.UUID;
import java.util.logging.Level;

public class LuckPermsManager {

    private final SurvivalRealms plugin;
    private final LuckPerms luckPerms;

    public LuckPermsManager(SurvivalRealms plugin) {
        this.plugin = plugin;
        this.luckPerms = Bukkit.getServicesManager()
                .load(LuckPerms.class);
    }

    public boolean isAvailable() {
        return luckPerms != null;
    }

    public void assignDefaultGroup(UUID uuid) {
        if (!isAvailable()) return;
        String groupName = plugin.getConfig().getString("permissions.default-group", "default");
        assignGroup(uuid, groupName);
    }

    public void assignGroup(UUID uuid, String groupName) {
        if (!isAvailable()) return;

        luckPerms.getUserManager().loadUser(uuid).thenAcceptAsync(user -> {
            if (user == null) return;

            Group group = luckPerms.getGroupManager().getGroup(groupName);
            if (group == null) {
                plugin.getLogger().warning("[LuckPerms] El grupo '" + groupName + "' no existe.");
                return;
            }

            InheritanceNode node = InheritanceNode.builder(group).build();

            if (!user.getNodes().contains(node)) {
                user.data().add(node);
                luckPerms.getUserManager().saveUser(user);
                plugin.getLogger().info("[LuckPerms] Grupo '" + groupName
                        + "' asignado a " + user.getUsername());
            }
        }).exceptionally(e -> {
            plugin.getLogger().log(Level.WARNING, "[LuckPerms] Error asignando grupo", e);
            return null;
        });
    }

    public void removeGroup(UUID uuid, String groupName) {
        if (!isAvailable()) return;

        luckPerms.getUserManager().loadUser(uuid).thenAcceptAsync(user -> {
            if (user == null) return;

            Group group = luckPerms.getGroupManager().getGroup(groupName);
            if (group == null) return;

            InheritanceNode node = InheritanceNode.builder(group).build();
            user.data().remove(node);
            luckPerms.getUserManager().saveUser(user);
        }).exceptionally(e -> {
            plugin.getLogger().log(Level.WARNING, "[LuckPerms] Error removiendo grupo", e);
            return null;
        });
    }

    public boolean isInGroup(UUID uuid, String groupName) {
        if (!isAvailable()) return false;
        User user = luckPerms.getUserManager().getUser(uuid);
        if (user == null) return false;
        return user.getNodes().stream()
                .filter(n -> n instanceof InheritanceNode)
                .map(n -> (InheritanceNode) n)
                .anyMatch(n -> n.getGroupName().equalsIgnoreCase(groupName));
    }
}