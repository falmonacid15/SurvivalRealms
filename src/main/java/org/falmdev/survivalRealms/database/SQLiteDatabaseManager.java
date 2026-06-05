package org.falmdev.survivalRealms.database;

import org.falmdev.survivalRealms.model.User;

import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public class SQLiteDatabaseManager implements DatabaseManager {

    private final File dataFile;
    private final Logger logger;
    private Connection connection;

    public SQLiteDatabaseManager(File dataFile, Logger logger) {
        this.dataFile = dataFile;
        this.logger   = logger;
    }

    @Override
    public void initialize() throws Exception {
        dataFile.getParentFile().mkdirs();
        connection = DriverManager.getConnection("jdbc:sqlite:" + dataFile.getAbsolutePath());

        try (Statement s = connection.createStatement()) {
            s.execute("PRAGMA journal_mode=WAL");
            s.execute("PRAGMA synchronous=NORMAL");
            s.execute("PRAGMA foreign_keys=ON");
        }

        createTables();
        logger.info("[SurvivalRealms] SQLite inicializado: " + dataFile.getName());
    }

    private void createTables() throws SQLException {
        try (Statement s = connection.createStatement()) {
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS sr_users (
                    uuid          TEXT PRIMARY KEY,
                    username      TEXT NOT NULL COLLATE NOCASE,
                    password_hash TEXT NOT NULL,
                    last_ip       TEXT,
                    last_login    INTEGER DEFAULT 0,
                    registered_at INTEGER NOT NULL,
                    last_world    TEXT,
                    last_x        REAL DEFAULT 0,
                    last_y        REAL DEFAULT 64,
                    last_z        REAL DEFAULT 0,
                    last_yaw      REAL DEFAULT 0,
                    last_pitch    REAL DEFAULT 0
                );
                """);
            s.executeUpdate("""
                CREATE UNIQUE INDEX IF NOT EXISTS idx_username
                ON sr_users(username COLLATE NOCASE);
                """);
        }
    }

    private Connection conn() throws SQLException {
        if (connection == null || connection.isClosed())
            connection = DriverManager.getConnection("jdbc:sqlite:" + dataFile.getAbsolutePath());
        return connection;
    }

    public Connection getConnection() throws SQLException {
        return conn();
    }

    @Override
    public void saveUser(User u) throws Exception {
        try (PreparedStatement ps = conn().prepareStatement("""
            INSERT INTO sr_users(uuid,username,password_hash,last_ip,last_login,registered_at,
                                 last_world,last_x,last_y,last_z,last_yaw,last_pitch)
            VALUES(?,?,?,?,?,?,?,?,?,?,?,?)
            """)) {
            ps.setString(1, u.getUuid().toString());
            ps.setString(2, u.getUsername());
            ps.setString(3, u.getPasswordHash());
            ps.setString(4, u.getLastIp());
            ps.setLong(5, u.getLastLogin() != null ? u.getLastLogin().getEpochSecond() : 0);
            ps.setLong(6, u.getRegisteredAt().getEpochSecond());
            ps.setString(7, u.getLastWorld());
            ps.setDouble(8, u.getLastX());
            ps.setDouble(9, u.getLastY());
            ps.setDouble(10, u.getLastZ());
            ps.setFloat(11, u.getLastYaw());
            ps.setFloat(12, u.getLastPitch());
            ps.executeUpdate();
        }
    }

    @Override
    public void updateUser(User u) throws Exception {
        try (PreparedStatement ps = conn().prepareStatement("""
            UPDATE sr_users SET password_hash=?, last_ip=?, last_login=? WHERE uuid=?
            """)) {
            ps.setString(1, u.getPasswordHash());
            ps.setString(2, u.getLastIp());
            ps.setLong(3, u.getLastLogin() != null ? u.getLastLogin().getEpochSecond() : 0);
            ps.setString(4, u.getUuid().toString());
            ps.executeUpdate();
        }
    }

    @Override
    public void updateLastLocation(User u) throws Exception {
        try (PreparedStatement ps = conn().prepareStatement("""
            UPDATE sr_users
            SET last_world=?, last_x=?, last_y=?, last_z=?, last_yaw=?, last_pitch=?
            WHERE uuid=?
            """)) {
            ps.setString(1, u.getLastWorld());
            ps.setDouble(2, u.getLastX());
            ps.setDouble(3, u.getLastY());
            ps.setDouble(4, u.getLastZ());
            ps.setFloat(5, u.getLastYaw());
            ps.setFloat(6, u.getLastPitch());
            ps.setString(7, u.getUuid().toString());
            ps.executeUpdate();
        }
    }

    @Override
    public Optional<User> findByUuid(UUID uuid) throws Exception {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT * FROM sr_users WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public Optional<User> findByUsername(String username) throws Exception {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT * FROM sr_users WHERE username=? COLLATE NOCASE")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public boolean isRegistered(String username) throws Exception {
        return findByUsername(username).isPresent();
    }

    @Override
    public void close() {
        try { if (connection != null && !connection.isClosed()) connection.close(); }
        catch (SQLException e) { logger.warning("Error cerrando SQLite: " + e.getMessage()); }
    }

    private User map(ResultSet rs) throws SQLException {
        return new User(
                UUID.fromString(rs.getString("uuid")),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("last_ip"),
                Instant.ofEpochSecond(rs.getLong("last_login")),
                Instant.ofEpochSecond(rs.getLong("registered_at")),
                rs.getString("last_world"),
                rs.getDouble("last_x"),
                rs.getDouble("last_y"),
                rs.getDouble("last_z"),
                rs.getFloat("last_yaw"),
                rs.getFloat("last_pitch")
        );
    }

    @Override
    public List<User> findAll() throws Exception {
        List<User> users = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT * FROM sr_users ORDER BY username ASC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) users.add(map(rs));
        }
        return users;
    }

    @Override
    public void deleteUser(UUID uuid) throws Exception {
        try (PreparedStatement ps = conn().prepareStatement(
                "DELETE FROM sr_users WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        }
    }
}