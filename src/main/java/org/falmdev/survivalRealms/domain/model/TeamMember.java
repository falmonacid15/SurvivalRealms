package org.falmdev.survivalRealms.domain.model;

import java.util.UUID;

public class TeamMember {

    private final UUID     playerUuid;
    private final String   playerName;
    private final UUID     teamId;
    private       TeamRole role;

    public TeamMember(UUID playerUuid, String playerName, UUID teamId, TeamRole role) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.teamId     = teamId;
        this.role       = role;
    }

    public UUID     getPlayerUuid() { return playerUuid; }
    public String   getPlayerName() { return playerName; }
    public UUID     getTeamId()     { return teamId; }
    public TeamRole getRole()       { return role; }
    public void     setRole(TeamRole role) { this.role = role; }

    public boolean isAdmin() { return role == TeamRole.ADMIN; }
}