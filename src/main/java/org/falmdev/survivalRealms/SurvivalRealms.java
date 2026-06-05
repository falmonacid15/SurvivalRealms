package org.falmdev.survivalRealms;

import fr.minuskube.inv.InventoryManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.falmdev.survivalRealms.command.LoginCommand;
import org.falmdev.survivalRealms.command.RegisterCommand;
import org.falmdev.survivalRealms.command.SrCommand;
import org.falmdev.survivalRealms.database.*;
import org.falmdev.survivalRealms.database.warp.SQLiteWarpRepository;
import org.falmdev.survivalRealms.gui.admin.UserListGUI;
import org.falmdev.survivalRealms.listener.AuthListener;
import org.falmdev.survivalRealms.manager.AuthManager;
import org.falmdev.survivalRealms.manager.LuckPermsManager;
import org.falmdev.survivalRealms.manager.SpawnManager;
import org.falmdev.survivalRealms.manager.WarpManager;
import org.falmdev.survivalRealms.menu.MenuDefinition;
import org.falmdev.survivalRealms.menu.MenuEngine;
import org.falmdev.survivalRealms.menu.MenuLoader;
import org.falmdev.survivalRealms.menu.MenuRegistry;
import org.falmdev.survivalRealms.menu.action.ActionRegistry;
import org.falmdev.survivalRealms.menu.placeholder.PlaceholderResolver;
import org.falmdev.survivalRealms.util.LoggerUtil;
import org.falmdev.survivalRealms.util.placeholder.PlaceholderManager;

import java.util.Map;
import java.util.logging.Logger;

public final class SurvivalRealms extends JavaPlugin {

    private DatabaseManager  databaseManager;
    private AuthManager      authManager;
    private SpawnManager     spawnManager;
    private LuckPermsManager luckPermsManager;
    private WarpManager      warpManager;
    private InventoryManager invManager;
    private PlaceholderManager placeholderManager;

    private MenuEngine          menuEngine;
    private ActionRegistry actionRegistry;
    private PlaceholderResolver placeholderResolver;
    private MenuRegistry        menuRegistry;

    private UserListGUI userListGUI;

    @Override
    public void onEnable() {
        Logger log = getLogger();
        printBanner(log);

        // ── Configuración ─────────────────────────────────────────────────────
        LoggerUtil.section(log, "Configuración");
        saveDefaultConfig();
        String dbType      = getConfig().getString("database.type", "sqlite").toUpperCase();
        int    timeout     = getConfig().getInt("auth.login-timeout", 60);
        int    maxAttempts = getConfig().getInt("auth.max-attempts", 5);
        int    minPass     = getConfig().getInt("auth.min-password-length", 6);
        int    maxPass     = getConfig().getInt("auth.max-password-length", 32);
        String defGroup    = getConfig().getString("permissions.default-group", "default");
        String adminGroup  = getConfig().getString("permissions.admin-group", "admin");
        LoggerUtil.item(log, "database.type  ", dbType);
        LoggerUtil.item(log, "login-timeout  ", timeout + "s");
        LoggerUtil.item(log, "max-attempts   ", String.valueOf(maxAttempts));
        LoggerUtil.item(log, "password-length", minPass + " - " + maxPass + " chars");
        LoggerUtil.item(log, "default-group  ", defGroup);
        LoggerUtil.item(log, "admin-group    ", adminGroup);
        LoggerUtil.done(log, "Configuración");

        // ── Base de datos ─────────────────────────────────────────────────────
        LoggerUtil.section(log, "DatabaseManager");
        try {
            databaseManager = DatabaseFactory.create(this);
            databaseManager.initialize();
            LoggerUtil.item(log, "Driver         ", dbType);
            switch (dbType) {
                case "SQLITE" -> LoggerUtil.item(log, "Archivo        ",
                        getConfig().getString("database.sqlite.file", "survivalrealms.db"));
                case "MYSQL" -> {
                    LoggerUtil.item(log, "Host           ",
                            getConfig().getString("database.mysql.host", "localhost")
                                    + ":" + getConfig().getInt("database.mysql.port", 3306));
                    LoggerUtil.item(log, "Database       ",
                            getConfig().getString("database.mysql.database", "survivalrealms"));
                }
                case "POSTGRESQL" -> {
                    LoggerUtil.item(log, "Host           ",
                            getConfig().getString("database.postgresql.host", "localhost")
                                    + ":" + getConfig().getInt("database.postgresql.port", 5432));
                    LoggerUtil.item(log, "Database       ",
                            getConfig().getString("database.postgresql.database", "survivalrealms"));
                }
            }
            LoggerUtil.done(log, "DatabaseManager");
        } catch (Exception e) {
            LoggerUtil.fail(log, "DatabaseManager", e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // ── SpawnManager ──────────────────────────────────────────────────────
        LoggerUtil.section(log, "SpawnManager");
        spawnManager = new SpawnManager(this);
        LoggerUtil.item(log, "auth-spawn     ",
                spawnManager.hasAuthSpawn() ? "Configurado" : "No configurado — usa /sr setauthspawn");
        LoggerUtil.item(log, "main-spawn     ",
                spawnManager.hasMainSpawn() ? "Configurado" : "No configurado — usa /sr setmainspawn");
        LoggerUtil.done(log, "SpawnManager");

        // ── LuckPerms ─────────────────────────────────────────────────────────
        LoggerUtil.section(log, "LuckPerms");
        luckPermsManager = new LuckPermsManager(this);
        if (luckPermsManager.isAvailable()) {
            LoggerUtil.item(log, "Estado         ", "Conectado");
            LoggerUtil.item(log, "default-group  ", defGroup);
            LoggerUtil.item(log, "admin-group    ", adminGroup);
            LoggerUtil.done(log, "LuckPerms");
        } else {
            LoggerUtil.warn(log, "LuckPerms", "No disponible — grupos deshabilitados");
        }

        // ── AuthManager ───────────────────────────────────────────────────────
        LoggerUtil.section(log, "AuthManager");
        authManager = new AuthManager(this, databaseManager, spawnManager, luckPermsManager);
        LoggerUtil.item(log, "Hash algorithm ", "BCrypt (cost=12)");
        LoggerUtil.item(log, "Sesiones       ", "ConcurrentHashSet");
        LoggerUtil.item(log, "Intentos       ", "ConcurrentHashMap");
        LoggerUtil.done(log, "AuthManager");

        // ── SmartInvs ─────────────────────────────────────────────────────────
        LoggerUtil.section(log, "SmartInvs");
        invManager = new InventoryManager(this);
        invManager.init();
        LoggerUtil.item(log, "InventoryManager   inicializado");
        LoggerUtil.done(log, "SmartInvs");

        userListGUI = new UserListGUI(this, 0);

        // ── WarpManager ───────────────────────────────────────────────────────
        LoggerUtil.section(log, "WarpManager");
        try {
            org.falmdev.survivalRealms.database.warp.WarpRepository warpRepo =
                    switch (dbType) {
                        case "MYSQL" -> new org.falmdev.survivalRealms.database.warp.MySQLWarpRepository(
                                ((MySQLDatabaseManager) databaseManager).getDataSource(),
                                getLogger()
                        );
                        case "POSTGRESQL" -> new org.falmdev.survivalRealms.database.warp.PostgreSQLWarpRepository(
                                ((PostgreSQLDatabaseManager) databaseManager).getDataSource(),
                                getLogger()
                        );
                        default -> new org.falmdev.survivalRealms.database.warp.SQLiteWarpRepository(
                                ((SQLiteDatabaseManager) databaseManager).getConnection(),
                                getLogger()
                        );
                    };

            warpRepo.initialize();
            warpManager = new WarpManager(this, warpRepo);
            LoggerUtil.item(log, "Repositorio    ", dbType + " listo");
            LoggerUtil.done(log, "WarpManager");
        } catch (Exception e) {
            LoggerUtil.fail(log, "WarpManager", e.getMessage());
        }

        // ── PlaceholderManager ────────────────────────────────────────────────
        placeholderManager = new PlaceholderManager(this);
        placeholderManager.initialize();

        // ── Sistema de Menús ──────────────────────────────────────────────────
        LoggerUtil.section(log, "MenuSystem");
        menuRegistry        = new MenuRegistry();
        actionRegistry      = new ActionRegistry(this);
        placeholderResolver = new PlaceholderResolver(this);

        Map<String, MenuDefinition> loadedMenus = new MenuLoader(this).loadAll();
        menuRegistry.registerAll(loadedMenus);

        menuEngine = new MenuEngine(this, menuRegistry.getAll(),
                actionRegistry, placeholderResolver);

        LoggerUtil.item(log, "ActionRegistry     ", "listo");
        LoggerUtil.item(log, "PlaceholderResolver", "listo");
        LoggerUtil.item(log, "MenuEngine         ", "listo (" + loadedMenus.size() + " menús)");
        LoggerUtil.done(log, "MenuSystem");

        // ── Comandos ──────────────────────────────────────────────────────────
        LoggerUtil.section(log, "Comandos");
        SrCommand srCommand = new SrCommand(this, spawnManager);
        getCommand("sr").setExecutor(srCommand);
        getCommand("sr").setTabCompleter(srCommand);
        getCommand("register").setExecutor(new RegisterCommand(this, authManager));
        getCommand("login").setExecutor(new LoginCommand(this, authManager));
        LoggerUtil.item(log, "/sr            (alias: /survivalrealms)");
        LoggerUtil.item(log, "/register      (alias: /reg)");
        LoggerUtil.item(log, "/login         (alias: /l, /log)");
        LoggerUtil.done(log, "Comandos");

        // ── Listeners ─────────────────────────────────────────────────────────
        LoggerUtil.section(log, "Listeners");
        getServer().getPluginManager().registerEvents(
                new AuthListener(this, authManager), this);
        LoggerUtil.item(log, "AuthListener   (join, quit, move, chat, commands, daño)");
        LoggerUtil.done(log, "Listeners");

        LoggerUtil.printLine(log);
        LoggerUtil.success(log, "SurvivalRealms v" + getDescription().getVersion()
                + " habilitado correctamente.");
        LoggerUtil.printLine(log);
    }

    @Override
    public void onDisable() {
        Logger log = getLogger();
        LoggerUtil.printLine(log);
        if (placeholderManager != null) placeholderManager.unregister();
        if (databaseManager != null) {
            databaseManager.close();
            log.info(LoggerUtil.GRAY + "  \u2022 " + LoggerUtil.YELLOW
                    + "DatabaseManager" + LoggerUtil.GRAY + " cerrado." + LoggerUtil.RESET);
        }
        LoggerUtil.error(log, "SurvivalRealms deshabilitado.");
        LoggerUtil.printLine(log);
    }

    private void printBanner(Logger log) {
        String v = getDescription().getVersion();
        log.info(" ");
        log.info(LoggerUtil.CYAN + LoggerUtil.BOLD + "  ____                  _           _ " + LoggerUtil.RESET);
        log.info(LoggerUtil.CYAN + LoggerUtil.BOLD + " / ___| _   _ _ ____   _(_)_   ____ _| |" + LoggerUtil.RESET);
        log.info(LoggerUtil.CYAN + LoggerUtil.BOLD + " \\___ \\| | | | '__\\ \\ / / \\ \\ / / _` | |" + LoggerUtil.RESET);
        log.info(LoggerUtil.CYAN + LoggerUtil.BOLD + "  ___) | |_| | |   \\ V /| |\\ V / (_| | |" + LoggerUtil.RESET);
        log.info(LoggerUtil.CYAN + LoggerUtil.BOLD + " |____/ \\__,_|_|    \\_/ |_| \\_/ \\__,_|_|" + LoggerUtil.RESET);
        log.info(LoggerUtil.CYAN + LoggerUtil.BOLD + "  ____            _                  " + LoggerUtil.RESET);
        log.info(LoggerUtil.CYAN + LoggerUtil.BOLD + " |  _ \\ ___  __ _| |_ __ ___  ___   " + LoggerUtil.RESET);
        log.info(LoggerUtil.CYAN + LoggerUtil.BOLD + " | |_) / _ \\/ _` | | '_ ` _ \\/ __|  " + LoggerUtil.RESET);
        log.info(LoggerUtil.CYAN + LoggerUtil.BOLD + " |  _ <  __/ (_| | | | | | | \\__ \\  " + LoggerUtil.RESET);
        log.info(LoggerUtil.CYAN + LoggerUtil.BOLD + " |_| \\_\\___|\\__,_|_|_| |_| |_|___/  " + LoggerUtil.RESET);
        log.info(" ");
        LoggerUtil.printInfoBox(log, "SurvivalRealms", v,
                "Auth plugin para servidores offline-mode", "falmdev");
    }

    public String getMsg(String path) { return getConfig().getString(path, path); }

    public DatabaseManager     getDatabaseManager()     { return databaseManager; }
    public AuthManager         getAuthManager()         { return authManager; }
    public SpawnManager        getSpawnManager()        { return spawnManager; }
    public LuckPermsManager    getLuckPermsManager()    { return luckPermsManager; }
    public WarpManager         getWarpManager()         { return warpManager; }
    public InventoryManager    getInvManager()          { return invManager; }
    public PlaceholderManager  getPlaceholderManager()  { return placeholderManager; }
    public MenuEngine          getMenuEngine()          { return menuEngine; }
    public ActionRegistry      getActionRegistry()      { return actionRegistry; }
    public PlaceholderResolver getPlaceholderResolver() { return placeholderResolver; }
    public MenuRegistry        getMenuRegistry()        { return menuRegistry; }

    public UserListGUI getUserListGUI() { return userListGUI; }
}