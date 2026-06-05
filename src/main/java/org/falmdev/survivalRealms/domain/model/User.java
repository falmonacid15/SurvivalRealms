package org.falmdev.survivalRealms.domain.model;

import java.time.Instant;
import java.util.UUID;

public class User {

    private final UUID    uuid;
    private final String  username;
    private String        passwordHash;
    private String        lastIp;
    private Instant       lastLogin;
    private final Instant registeredAt;
    private String        lastWorld;
    private double        lastX;
    private double        lastY;
    private double        lastZ;
    private float         lastYaw;
    private float         lastPitch;

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

    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setLastIp(String lastIp)             { this.lastIp = lastIp; }
    public void setLastLogin(Instant lastLogin)      { this.lastLogin = lastLogin; }
    public void setLastWorld(String lastWorld)        { this.lastWorld = lastWorld; }
    public void setLastX(double lastX)               { this.lastX = lastX; }
    public void setLastY(double lastY)               { this.lastY = lastY; }
    public void setLastZ(double lastZ)               { this.lastZ = lastZ; }
    public void setLastYaw(float lastYaw)            { this.lastYaw = lastYaw; }
    public void setLastPitch(float lastPitch)        { this.lastPitch = lastPitch; }
}