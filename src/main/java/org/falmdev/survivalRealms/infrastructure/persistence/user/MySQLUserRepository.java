package org.falmdev.survivalRealms.infrastructure.persistence.user;

import com.zaxxer.hikari.HikariDataSource;
import org.falmdev.survivalRealms.domain.model.User;
import org.falmdev.survivalRealms.domain.repository.UserRepository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public class MySQLUserRepository implements UserRepository {

    private final DataSource ds;
    private final Logger     logger;

    public MySQLUserRepository(DataSource ds, Logger logger) {
        this.ds     = ds;
        this.logger = logger;
    }

    public void initialize() throws Exception {
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS sr_users (
                    uuid          VARCHAR(36)  PRIMARY KEY,
                    username      VARCHAR(16)  NOT NULL,
                    password_hash VARCHAR(72)  NOT NULL,
                    last_ip       VARCHAR(45),
                    last_login    BIGINT       DEFAULT 0,
                    registered_at BIGINT       NOT NULL,
                    last_world    VARCHAR(64),
                    last_x        DOUBLE       DEFAULT 0,
                    last_y        DOUBLE       DEFAULT 64,
                    last_z        DOUBLE       DEFAULT 0,
                    last_yaw      FLOAT        DEFAULT 0,
                    last_pitch    FLOAT        DEFAULT 0,
                    UNIQUE KEY uq_username (username)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                """);
        }
    }

    @Override
    public void save(User u) throws Exception {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("""
                INSERT INTO sr_users(uuid,username,password_hash,last_ip,last_login,registered_at,
                                     last_world,last_x,last_y,last_z,last_yaw,last_pitch)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?)
                ON DUPLICATE KEY UPDATE
                    password_hash=VALUES(password_hash),
                    last_ip=VALUES(last_ip),
                    last_login=VALUES(last_login),
                    last_world=VALUES(last_world),
                    last_x=VALUES(last_x),
                    last_y=VALUES(last_y),
                    last_z=VALUES(last_z),
                    last_yaw=VALUES(last_yaw),
                    last_pitch=VALUES(last_pitch)
                """)) {
            bind(ps, u);
            ps.executeUpdate();
        }
    }

    @Override
    public void update(User u) throws Exception {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("""
                UPDATE sr_users
                SET password_hash=?, last_ip=?, last_login=?
                WHERE uuid=?
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
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("""
                UPDATE sr_users
                SET last_world=?, last_x=?, last_y=?, last_z=?, last_yaw=?, last_pitch=?
                WHERE uuid=?
                """)) {
            ps.setString(1, u.getLastWorld());
            ps.setDouble(2, u.getLastX());
            ps.setDouble(3, u.getLastY());
            ps.setDouble(4, u.getLastZ());
            ps.setFloat(5,  u.getLastYaw());
            ps.setFloat(6,  u.getLastPitch());
            ps.setString(7, u.getUuid().toString());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(UUID uuid) throws Exception {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM sr_users WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        }
    }

    @Override
    public Optional<User> findByUuid(UUID uuid) throws Exception {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT * FROM sr_users WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public Optional<User> findByUsername(String username) throws Exception {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT * FROM sr_users WHERE username=?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public boolean existsByUsername(String username) throws Exception {
        return findByUsername(username).isPresent();
    }

    @Override
    public List<User> findAll() throws Exception {
        List<User> users = new ArrayList<>();
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT * FROM sr_users ORDER BY username ASC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) users.add(map(rs));
        }
        return users;
    }

    public void close() {
        if (ds instanceof HikariDataSource hds && !hds.isClosed()) hds.close();
    }

    private void bind(PreparedStatement ps, User u) throws SQLException {
        ps.setString(1,  u.getUuid().toString());
        ps.setString(2,  u.getUsername());
        ps.setString(3,  u.getPasswordHash());
        ps.setString(4,  u.getLastIp());
        ps.setLong(5,    u.getLastLogin() != null ? u.getLastLogin().getEpochSecond() : 0);
        ps.setLong(6,    u.getRegisteredAt().getEpochSecond());
        ps.setString(7,  u.getLastWorld());
        ps.setDouble(8,  u.getLastX());
        ps.setDouble(9,  u.getLastY());
        ps.setDouble(10, u.getLastZ());
        ps.setFloat(11,  u.getLastYaw());
        ps.setFloat(12,  u.getLastPitch());
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
}