package org.falmdev.survivalRealms.domain.model;

import java.util.UUID;

public class TeamBanner {

    private final UUID   teamId;
    private       String baseColor;
    private       String patternData;

    public TeamBanner(UUID teamId, String baseColor, String patternData) {
        this.teamId      = teamId;
        this.baseColor   = baseColor;
        this.patternData = patternData;
    }

    public UUID   getTeamId()      { return teamId; }
    public String getBaseColor()   { return baseColor; }
    public String getPatternData() { return patternData; }

    public void setBaseColor(String baseColor)     { this.baseColor   = baseColor; }
    public void setPatternData(String patternData) { this.patternData = patternData; }
}