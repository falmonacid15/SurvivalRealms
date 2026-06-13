package org.falmdev.survivalRealms.domain.port;

import java.util.UUID;

public interface PermissionPort {
    void assignDefaultGroup(UUID uuid);
    void assignGroup(UUID uuid, String group);
    void removeGroup(UUID uuid, String group);
    boolean isInGroup(UUID uuid, String group);
    boolean isAvailable();
}