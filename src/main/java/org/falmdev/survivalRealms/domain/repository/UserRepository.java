package org.falmdev.survivalRealms.domain.repository;

import org.falmdev.survivalRealms.domain.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    void save(User user) throws Exception;
    void update(User user) throws Exception;
    void updateLastLocation(User user) throws Exception;
    void delete(UUID uuid) throws Exception;
    Optional<User> findByUuid(UUID uuid) throws Exception;
    Optional<User> findByUsername(String username) throws Exception;
    boolean existsByUsername(String username) throws Exception;
    List<User> findAll() throws Exception;
}