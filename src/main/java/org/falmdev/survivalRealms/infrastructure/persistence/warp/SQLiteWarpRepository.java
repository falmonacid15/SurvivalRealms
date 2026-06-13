package org.falmdev.survivalRealms.infrastructure.persistence.warp;

import org.falmdev.survivalRealms.domain.model.Warp;
import org.falmdev.survivalRealms.domain.repository.WarpRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public class SQLiteWarpRepository implements WarpRepository {

    private final Connection connection;
    private final Logger     logger;

    public SQLiteWarpRepository(Connection connection, Logger logger) {
        this.connection = connection;
        this.logger     = logger;
    }

    @Override
    public void initialize() throws Exception {
        try (Statement s = connection.createStatement()) {
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS sr_warps (
                    id         TEXT PRIMARY KEY,
                    name       TEXT NOT NULL COLLATE NOCASE,
                    world      TEXT NOT NULL,
                    x          REAL NOT NULL,
                    y          REAL NOT NULL,
                    z          REAL NOT NULL,
                    yaw        REAL DEFAULT 0,
                    pitch      REAL DEFAULT 0,
                    created_by TEXT NOT NULL,
                    icon       TEXT DEFAULT 'GRASS_BLOCK'
                );
                """);
            s.executeUpdate("""
                CREATE UNIQUE INDEX IF NOT EXISTS idx_warp_name
                ON sr_warps(name COLLATE NOCASE);
                """);
        }
    }

    @Override
    public void save(Warp w) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement("""
            INSERT OR REPLACE INTO sr_warps
                (id, name, world, x, y, z, yaw, pitch, created_by, icon)
            VALUES (?,?,?,?,?,?,?,?,?,?)
            """)) {
            bind(ps, w);
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(String id) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM sr_warps WHERE id=?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public Optional<Warp> findById(String id) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM sr_warps WHERE id=?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public Optional<Warp> findByName(String name) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM sr_warps WHERE name=? COLLATE NOCASE")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<Warp> findAll() throws Exception {
        List<Warp> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM sr_warps ORDER BY name ASC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    private void bind(PreparedStatement ps, Warp w) throws SQLException {
        ps.setString(1,  w.getId());
        ps.setString(2,  w.getName());
        ps.setString(3,  w.getWorld());
        ps.setDouble(4,  w.getX());
        ps.setDouble(5,  w.getY());
        ps.setDouble(6,  w.getZ());
        ps.setFloat(7,   w.getYaw());
        ps.setFloat(8,   w.getPitch());
        ps.setString(9,  w.getCreatedBy().toString());
        ps.setString(10, w.getIcon());
    }

    private Warp map(ResultSet rs) throws SQLException {
        return new Warp(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("world"),
                rs.getDouble("x"),
                rs.getDouble("y"),
                rs.getDouble("z"),
                rs.getFloat("yaw"),
                rs.getFloat("pitch"),
                UUID.fromString(rs.getString("created_by")),
                rs.getString("icon")
        );
    }
}