package org.falmdev.survivalRealms.database;

import org.falmdev.survivalRealms.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DatabaseManager {
    void initialize() throws Exception;
    void close();
    void saveUser(User user) throws Exception;
    void updateUser(User user) throws Exception;
    void updateLastLocation(User user) throws Exception;
    Optional<User> findByUuid(UUID uuid) throws Exception;
    Optional<User> findByUsername(String username) throws Exception;
    boolean isRegistered(String username) throws Exception;

    List<User> findAll() throws Exception;
    void deleteUser(UUID uuid) throws Exception;
}