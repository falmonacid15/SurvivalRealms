package org.falmdev.survivalRealms.domain.model;

import java.util.UUID;

public class ClaimedChunk {

    private final UUID   teamId;
    private final String world;
    private final int    chunkX;
    private final int    chunkZ;

    public ClaimedChunk(UUID teamId, String world, int chunkX, int chunkZ) {
        this.teamId = teamId;
        this.world  = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public UUID   getTeamId() { return teamId; }
    public String getWorld()  { return world; }
    public int    getChunkX() { return chunkX; }
    public int    getChunkZ() { return chunkZ; }

    public String toKey() { return world + ":" + chunkX + ":" + chunkZ; }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ClaimedChunk c)) return false;
        return chunkX == c.chunkX && chunkZ == c.chunkZ && world.equals(c.world);
    }

    @Override
    public int hashCode() {
        return toKey().hashCode();
    }
}