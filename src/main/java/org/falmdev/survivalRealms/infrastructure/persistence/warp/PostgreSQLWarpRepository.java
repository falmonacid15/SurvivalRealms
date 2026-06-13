package org.falmdev.survivalRealms.infrastructure.persistence.warp;

import org.falmdev.survivalRealms.domain.model.Warp;
import org.falmdev.survivalRealms.domain.repository.WarpRepository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public class PostgreSQLWarpRepository implements WarpRepository {

    private final DataSource ds;
    private final Logger     logger;

    public PostgreSQLWarpRepository(DataSource ds, Logger logger) {
        this.ds     = ds;
        this.logger = logger;
    }

    @Override
    public void initialize() throws Exception {
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            s.execute("""
                CREATE TABLE IF NOT EXISTS sr_warps (
                    id         VARCHAR(36)  PRIMARY KEY,
                    name       VARCHAR(64)  NOT NULL,
                    world      VARCHAR(64)  NOT NULL,
                    x          DOUBLE PRECISION NOT NULL,
                    y          DOUBLE PRECISION NOT NULL,
                    z          DOUBLE PRECISION NOT NULL,
                    yaw        REAL         DEFAULT 0,
                    pitch      REAL         DEFAULT 0,
                    created_by VARCHAR(36)  NOT NULL,
                    icon       VARCHAR(64)  DEFAULT 'GRASS_BLOCK',
                    UNIQUE (name)
                );
                """);
        }
    }

    @Override
    public void save(Warp w) throws Exception {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("""
                INSERT INTO sr_warps (id,name,world,x,y,z,yaw,pitch,created_by,icon)
                VALUES (?,?,?,?,?,?,?,?,?,?)
                ON CONFLICT(id) DO UPDATE SET
                    world=EXCLUDED.world, x=EXCLUDED.x, y=EXCLUDED.y, z=EXCLUDED.z,
                    yaw=EXCLUDED.yaw, pitch=EXCLUDED.pitch, icon=EXCLUDED.icon
                """)) {
            bind(ps, w);
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(String id) throws Exception {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM sr_warps WHERE id=?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public Optional<Warp> findById(String id) throws Exception {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT * FROM sr_warps WHERE id=?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public Optional<Warp> findByName(String name) throws Exception {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT * FROM sr_warps WHERE LOWER(name)=LOWER(?)")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<Warp> findAll() throws Exception {
        List<Warp> list = new ArrayList<>();
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
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