package org.falmdev.survivalRealms.infrastructure.external;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.plugin.java.JavaPlugin;
import org.falmdev.survivalRealms.domain.port.PermissionPort;

import java.util.UUID;
import java.util.logging.Level;

public class LuckPermsAdapter implements PermissionPort {

    private final JavaPlugin plugin;
    private final String     defaultGroup;
    private LuckPerms        luckPerms;
    private boolean          available = false;

    public LuckPermsAdapter(JavaPlugin plugin, String defaultGroup) {
        this.plugin       = plugin;
        this.defaultGroup = defaultGroup;
        try {
            this.luckPerms = LuckPermsProvider.get();
            this.available = true;
        } catch (IllegalStateException e) {
            plugin.getLogger().warning("[LuckPerms] No disponible — grupos deshabilitados.");
        }
    }

    @Override
    public boolean isAvailable() { return available; }

    @Override
    public void assignDefaultGroup(UUID uuid) {
        assignGroup(uuid, defaultGroup);
    }

    @Override
    public void assignGroup(UUID uuid, String groupName) {
        if (!available) return;
        luckPerms.getUserManager().loadUser(uuid).thenAcceptAsync(user -> {
            if (user == null) return;
            Group group = luckPerms.getGroupManager().getGroup(groupName);
            if (group == null) {
                plugin.getLogger().warning("[LuckPerms] Grupo no encontrado: " + groupName);
                return;
            }
            InheritanceNode node = InheritanceNode.builder(group).build();
            if (!user.getNodes().contains(node)) {
                user.data().add(node);
                luckPerms.getUserManager().saveUser(user);
            }
        }).exceptionally(e -> {
            plugin.getLogger().log(Level.WARNING, "[LuckPerms] Error asignando grupo", e);
            return null;
        });
    }

    @Override
    public void removeGroup(UUID uuid, String groupName) {
        if (!available) return;
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

    @Override
    public boolean isInGroup(UUID uuid, String groupName) {
        if (!available) return false;
        net.luckperms.api.model.user.User user = luckPerms.getUserManager().getUser(uuid);
        if (user == null) return false;
        return user.getNodes().stream()
                .filter(n -> n instanceof InheritanceNode)
                .map(n -> (InheritanceNode) n)
                .anyMatch(n -> n.getGroupName().equalsIgnoreCase(groupName));
    }

    public String getPrimaryGroup(UUID uuid) {
        if (!available) return defaultGroup;
        net.luckperms.api.model.user.User user = luckPerms.getUserManager().getUser(uuid);
        if (user == null) return defaultGroup;
        return user.getPrimaryGroup();
    }
}