package org.falmdev.survivalRealms.infrastructure.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import org.falmdev.survivalRealms.domain.repository.UserRepository;
import org.falmdev.survivalRealms.domain.repository.WarpRepository;
import org.falmdev.survivalRealms.infrastructure.persistence.user.MySQLUserRepository;
import org.falmdev.survivalRealms.infrastructure.persistence.user.PostgreSQLUserRepository;
import org.falmdev.survivalRealms.infrastructure.persistence.user.SQLiteUserRepository;
import org.falmdev.survivalRealms.infrastructure.persistence.warp.MySQLWarpRepository;
import org.falmdev.survivalRealms.infrastructure.persistence.warp.PostgreSQLWarpRepository;
import org.falmdev.survivalRealms.infrastructure.persistence.warp.SQLiteWarpRepository;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.logging.Logger;

public final class DatabaseFactory {

    private DatabaseFactory() {}

    public record RepositoryBundle(
            UserRepository userRepository,
            WarpRepository warpRepository,
            Runnable       closer
    ) {}

    public static RepositoryBundle create(FileConfiguration cfg, File dataFolder, Logger logger) throws Exception {
        String type = cfg.getString("database.type", "sqlite").toLowerCase();

        return switch (type) {
            case "mysql"      -> createMySQL(cfg, logger);
            case "postgresql" -> createPostgreSQL(cfg, logger);
            default           -> createSQLite(cfg, dataFolder, logger);
        };
    }

    private static RepositoryBundle createSQLite(FileConfiguration cfg, File dataFolder, Logger logger) throws Exception {
        File dbFile = new File(dataFolder, cfg.getString("database.sqlite.file", "survivalrealms.db"));
        dbFile.getParentFile().mkdirs();
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

        SQLiteUserRepository userRepo = new SQLiteUserRepository(conn, dbFile.getAbsolutePath(), logger);
        SQLiteWarpRepository warpRepo = new SQLiteWarpRepository(conn, logger);

        userRepo.initialize();
        warpRepo.initialize();

        return new RepositoryBundle(userRepo, warpRepo, userRepo::close);
    }

    private static RepositoryBundle createMySQL(FileConfiguration cfg, Logger logger) throws Exception {
        HikariDataSource ds = buildPool(cfg, "mysql", 3306, "SR-MySQL",
                "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8");

        MySQLUserRepository userRepo = new MySQLUserRepository(ds, logger);
        MySQLWarpRepository warpRepo = new MySQLWarpRepository(ds, logger);

        userRepo.initialize();
        warpRepo.initialize();

        return new RepositoryBundle(userRepo, warpRepo, ds::close);
    }

    private static RepositoryBundle createPostgreSQL(FileConfiguration cfg, Logger logger) throws Exception {
        HikariDataSource ds = buildPool(cfg, "postgresql", 5432, "SR-PostgreSQL",
                "jdbc:postgresql://%s:%d/%s");

        PostgreSQLUserRepository userRepo = new PostgreSQLUserRepository(ds, logger);
        PostgreSQLWarpRepository warpRepo = new PostgreSQLWarpRepository(ds, logger);

        userRepo.initialize();
        warpRepo.initialize();

        return new RepositoryBundle(userRepo, warpRepo, ds::close);
    }

    private static HikariDataSource buildPool(FileConfiguration cfg, String key,
                                              int defaultPort, String poolName,
                                              String urlTemplate) {
        String host     = cfg.getString("database." + key + ".host", "localhost");
        int    port     = cfg.getInt("database." + key + ".port", defaultPort);
        String database = cfg.getString("database." + key + ".database", "survivalrealms");
        String username = cfg.getString("database." + key + ".username", "root");
        String password = cfg.getString("database." + key + ".password", "");
        int    poolSize = cfg.getInt("database." + key + ".pool-size", 10);

        HikariConfig hCfg = new HikariConfig();
        hCfg.setJdbcUrl(String.format(urlTemplate, host, port, database));
        hCfg.setUsername(username);
        hCfg.setPassword(password);
        hCfg.setMaximumPoolSize(poolSize);
        hCfg.setMinimumIdle(2);
        hCfg.setConnectionTimeout(10_000);
        hCfg.setIdleTimeout(600_000);
        hCfg.setMaxLifetime(1_800_000);
        hCfg.setPoolName(poolName);

        return new HikariDataSource(hCfg);
    }
}