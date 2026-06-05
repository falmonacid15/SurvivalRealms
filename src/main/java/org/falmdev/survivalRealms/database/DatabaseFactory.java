package org.falmdev.survivalRealms.database;

import org.bukkit.configuration.file.FileConfiguration;
import org.falmdev.survivalRealms.SurvivalRealms;

import java.io.File;

public final class DatabaseFactory {

    private DatabaseFactory() {}

    public static DatabaseManager create(SurvivalRealms plugin) {
        FileConfiguration cfg = plugin.getConfig();
        String type = cfg.getString("database.type", "sqlite").toLowerCase();

        return switch (type) {
            case "mysql" -> new MySQLDatabaseManager(
                    cfg.getString("database.mysql.host", "localhost"),
                    cfg.getInt("database.mysql.port", 3306),
                    cfg.getString("database.mysql.database", "survivalrealms"),
                    cfg.getString("database.mysql.username", "root"),
                    cfg.getString("database.mysql.password", ""),
                    cfg.getInt("database.mysql.pool-size", 10),
                    plugin.getLogger()
            );
            case "postgresql" -> new PostgreSQLDatabaseManager(
                    cfg.getString("database.postgresql.host", "localhost"),
                    cfg.getInt("database.postgresql.port", 5432),
                    cfg.getString("database.postgresql.database", "survivalrealms"),
                    cfg.getString("database.postgresql.username", "postgres"),
                    cfg.getString("database.postgresql.password", ""),
                    cfg.getInt("database.postgresql.pool-size", 10),
                    plugin.getLogger()
            );
            default -> new SQLiteDatabaseManager(
                    new File(plugin.getDataFolder(),
                            cfg.getString("database.sqlite.file", "survivalrealms.db")),
                    plugin.getLogger()
            );
        };
    }
}