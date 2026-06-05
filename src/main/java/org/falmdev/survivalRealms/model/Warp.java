package org.falmdev.survivalRealms.model;

import java.util.UUID;

public class Warp {

    private final String id;
    private final String name;
    private final String world;
    private final double x;
    private final double y;
    private final double z;
    private final float  yaw;
    private final float  pitch;
    private final UUID   createdBy;
    private String icon;

    public Warp(String id, String name, String world,
                double x, double y, double z,
                float yaw, float pitch,
                UUID createdBy, String icon) {
        this.id        = id;
        this.name      = name;
        this.world     = world;
        this.x         = x;
        this.y         = y;
        this.z         = z;
        this.yaw       = yaw;
        this.pitch     = pitch;
        this.createdBy = createdBy;
        this.icon      = icon;
    }

    public String getId()        { return id; }
    public String getName()      { return name; }
    public String getWorld()     { return world; }
    public double getX()         { return x; }
    public double getY()         { return y; }
    public double getZ()         { return z; }
    public float  getYaw()       { return yaw; }
    public float  getPitch()     { return pitch; }
    public UUID   getCreatedBy() { return createdBy; }
    public String getIcon()      { return icon; }

    public void setIcon(String icon) { this.icon = icon; }
}