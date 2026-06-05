package org.falmdev.survivalRealms.application.user;

import org.bukkit.plugin.java.JavaPlugin;
import org.falmdev.survivalRealms.domain.model.User;
import org.falmdev.survivalRealms.domain.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public class UserService {

    private final JavaPlugin     plugin;
    private final UserRepository userRepository;

    public UserService(JavaPlugin plugin, UserRepository userRepository) {
        this.plugin         = plugin;
        this.userRepository = userRepository;
    }

    public Optional<User> findByUuid(UUID uuid) {
        try {
            return userRepository.findByUuid(uuid);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error buscando usuario " + uuid, e);
            return Optional.empty();
        }
    }

    public Optional<User> findByUsername(String username) {
        try {
            return userRepository.findByUsername(username);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error buscando usuario " + username, e);
            return Optional.empty();
        }
    }

    public List<User> findAll() {
        try {
            return userRepository.findAll();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error obteniendo usuarios", e);
            return List.of();
        }
    }

    public void delete(UUID uuid) {
        try {
            userRepository.delete(uuid);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error eliminando usuario " + uuid, e);
        }
    }
}