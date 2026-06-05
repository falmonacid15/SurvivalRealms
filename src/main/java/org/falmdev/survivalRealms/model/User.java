package org.falmdev.survivalRealms.model;

import java.time.Instant;
import java.util.UUID;

public class User {

    private final UUID uuid;
    private final String username;
    private String passwordHash;
    private String lastIp;
    private Instant lastLogin;
    private final Instant registeredAt;

    private String lastWorld;
    private double lastX;
    private double lastY;
    private double lastZ;
    private float  lastYaw;
    private float  lastPitch;

    public User(UUID uuid, String username, String passwordHash,
                String lastIp, Instant lastLogin, Instant registeredAt,
                String lastWorld, double lastX, double lastY, double lastZ,
                float lastYaw, float lastPitch) {
        this.uuid         = uuid;
        this.username     = username;
        this.passwordHash = passwordHash;
        this.lastIp       = lastIp;
        this.lastLogin    = lastLogin;
        this.registeredAt = registeredAt;
        this.lastWorld    = lastWorld;
        this.lastX        = lastX;
        this.lastY        = lastY;
        this.lastZ        = lastZ;
        this.lastYaw      = lastYaw;
        this.lastPitch    = lastPitch;
    }

    public UUID    getUuid()         { return uuid; }
    public String  getUsername()     { return username; }
    public String  getPasswordHash() { return passwordHash; }
    public String  getLastIp()       { return lastIp; }
    public Instant getLastLogin()    { return lastLogin; }
    public Instant getRegisteredAt() { return registeredAt; }
    public String  getLastWorld()    { return lastWorld; }
    public double  getLastX()        { return lastX; }
    public double  getLastY()        { return lastY; }
    public double  getLastZ()        { return lastZ; }
    public float   getLastYaw()      { return lastYaw; }
    public float   getLastPitch()    { return lastPitch; }

    public void setPasswordHash(String h)  { this.passwordHash = h; }
    public void setLastIp(String ip)       { this.lastIp = ip; }
    public void setLastLogin(Instant t)    { this.lastLogin = t; }
    public void setLastWorld(String w)     { this.lastWorld = w; }
    public void setLastX(double x)        { this.lastX = x; }
    public void setLastY(double y)        { this.lastY = y; }
    public void setLastZ(double z)        { this.lastZ = z; }
    public void setLastYaw(float yaw)     { this.lastYaw = yaw; }
    public void setLastPitch(float pitch)  { this.lastPitch = pitch; }

    public boolean hasLastLocation() {
        return lastWorld != null && !lastWorld.isBlank();
    }
}