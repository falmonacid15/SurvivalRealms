package org.falmdev.survivalRealms;

import fr.minuskube.inv.InventoryManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.falmdev.survivalRealms.application.auth.AuthService;
import org.falmdev.survivalRealms.application.spawn.SpawnService;
import org.falmdev.survivalRealms.application.user.UserService;
import org.falmdev.survivalRealms.application.warp.WarpService;
import org.falmdev.survivalRealms.domain.port.PermissionPort;
import org.falmdev.survivalRealms.domain.port.PlaceholderPort;
import org.falmdev.survivalRealms.domain.repository.UserRepository;
import org.falmdev.survivalRealms.domain.repository.WarpRepository;
import org.falmdev.survivalRealms.infrastructure.external.LuckPermsAdapter;
import org.falmdev.survivalRealms.infrastructure.external.PlaceholderApiAdapter;
import org.falmdev.survivalRealms.infrastructure.external.placeholder.AuthPlaceholderModule;
import org.falmdev.survivalRealms.infrastructure.external.placeholder.SpawnPlaceholderModule;
import org.falmdev.survivalRealms.infrastructure.external.placeholder.WarpPlaceholderModule;
import org.falmdev.survivalRealms.infrastructure.persistence.DatabaseFactory;
import org.falmdev.survivalRealms.presentation.command.LoginCommand;
import org.falmdev.survivalRealms.presentation.command.RegisterCommand;
import org.falmdev.survivalRealms.presentation.command.SrCommand;
import org.falmdev.survivalRealms.presentation.gui.UserListGUI;
import org.falmdev.survivalRealms.presentation.gui.WorldSelectorGUI;
import org.falmdev.survivalRealms.presentation.listener.AuthListener;
import org.falmdev.survivalRealms.presentation.menu.MenuEngine;
import org.falmdev.survivalRealms.presentation.menu.MenuLoader;
import org.falmdev.survivalRealms.presentation.menu.MenuRegistry;
import org.falmdev.survivalRealms.presentation.menu.action.ActionRegistry;
import org.falmdev.survivalRealms.presentation.menu.placeholder.MenuPlaceholderResolver;
import org.falmdev.survivalRealms.util.LoggerUtil;

import java.util.logging.Logger;

public final class SurvivalRealms extends JavaPlugin {

    private DatabaseFactory.RepositoryBundle repositories;
    private InventoryManager                 invManager;
    private PlaceholderApiAdapter            placeholderAdapter;

    private SpawnService   spawnService;
    private AuthService    authService;
    private WarpService    warpService;
    private UserService    userService;

    private MenuEngine     menuEngine;
    private UserListGUI    userListGUI;
    private WorldSelectorGUI worldSelectorGUI;


    @Override
    public void onEnable() {
        Logger log = getLogger();
        printBanner(log);

        saveDefaultConfig();
        LoggerUtil.section(log, "Configuración");
        String dbType      = getConfig().getString("database.type", "sqlite").toUpperCase();
        int    timeout     = getConfig().getInt("auth.login-timeout", 60);
        int    maxAttempts = getConfig().getInt("auth.max-attempts", 5);
        String defGroup    = getConfig().getString("permissions.default-group", "default");
        LoggerUtil.item(log, "database.type  ", dbType);
        LoggerUtil.item(log, "login-timeout  ", timeout + "s");
        LoggerUtil.item(log, "max-attempts   ", String.valueOf(maxAttempts));
        LoggerUtil.item(log, "default-group  ", defGroup);
        LoggerUtil.done(log, "Configuración");

        LoggerUtil.section(log, "DatabaseManager");
        try {
            repositories = DatabaseFactory.create(getConfig(), getDataFolder(), log);
            LoggerUtil.item(log, "Driver         ", dbType);
            LoggerUtil.done(log, "DatabaseManager");
        } catch (Exception e) {
            LoggerUtil.fail(log, "DatabaseManager", e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        UserRepository userRepository = repositories.userRepository();
        WarpRepository warpRepository = repositories.warpRepository();

        LoggerUtil.section(log, "SpawnService");
        spawnService = new SpawnService(this);
        LoggerUtil.item(log, "auth-spawn     ", spawnService.hasAuthSpawn() ? "Configurado" : "No configurado");
        LoggerUtil.item(log, "main-spawn     ", spawnService.hasMainSpawn() ? "Configurado" : "No configurado");
        LoggerUtil.done(log, "SpawnService");

        LoggerUtil.section(log, "LuckPerms");
        PermissionPort permissions = new LuckPermsAdapter(this, defGroup);
        if (permissions.isAvailable()) {
            LoggerUtil.item(log, "Estado         ", "Conectado");
            LoggerUtil.item(log, "default-group  ", defGroup);
        } else {
            LoggerUtil.warn(log, "LuckPerms", "No disponible — grupos deshabilitados");
        }
        LoggerUtil.done(log, "LuckPerms");

        LoggerUtil.section(log, "AuthService");
        authService = new AuthService(this, userRepository, spawnService, permissions, maxAttempts);
        LoggerUtil.item(log, "Hash algorithm ", "BCrypt (cost=12)");
        LoggerUtil.item(log, "max-attempts   ", String.valueOf(maxAttempts));
        LoggerUtil.done(log, "AuthService");

        LoggerUtil.section(log, "WarpService");
        warpService = new WarpService(this, warpRepository, permissions);
        LoggerUtil.item(log, "Warps cargados ", String.valueOf(warpService.getAllWarps().size()));
        LoggerUtil.done(log, "WarpService");

        userService = new UserService(this, userRepository);

        LoggerUtil.section(log, "PlaceholderAPI");
        placeholderAdapter = new PlaceholderApiAdapter(this);
        placeholderAdapter.addModule(new AuthPlaceholderModule(this, authService));
        placeholderAdapter.addModule(new SpawnPlaceholderModule(spawnService));
        placeholderAdapter.addModule(new WarpPlaceholderModule(warpService));
        placeholderAdapter.register();
        if (placeholderAdapter.isAvailable()) {
            LoggerUtil.item(log, "Estado         ", "Conectado");
            LoggerUtil.item(log, "Expansión      ", "%sr_<placeholder>%");
        } else {
            LoggerUtil.warn(log, "PlaceholderAPI", "No encontrado — placeholders internos activos");
        }
        LoggerUtil.done(log, "PlaceholderAPI");

        LoggerUtil.section(log, "SmartInvs");
        invManager = new InventoryManager(this);
        invManager.init();
        LoggerUtil.item(log, "InventoryManager   inicializado");
        LoggerUtil.done(log, "SmartInvs");

        LoggerUtil.section(log, "MenuSystem");
        MenuPlaceholderResolver placeholderResolver = new MenuPlaceholderResolver(placeholderAdapter);
        MenuRegistry            menuRegistry        = new MenuRegistry();
        menuRegistry.registerAll(new MenuLoader(this).loadAll());

        ActionRegistry actionRegistry = new ActionRegistry(
                this, spawnService, warpService, () -> menuEngine, null);

        menuEngine = new MenuEngine(
                this, menuRegistry.getAll(), actionRegistry, placeholderResolver, warpService, invManager);

        worldSelectorGUI = new WorldSelectorGUI(this, invManager, menuEngine);
        actionRegistry.register("OPEN_WORLD_GUI", (p, d) -> worldSelectorGUI.open(p));

        userListGUI = new UserListGUI(this, userService, authService, invManager, menuEngine);
        menuEngine.setUserListGUI(userListGUI);

        LoggerUtil.item(log, "Menús cargados ", String.valueOf(menuRegistry.getAll().size()));
        LoggerUtil.done(log, "MenuSystem");

        LoggerUtil.section(log, "Comandos");
        SrCommand srCommand = new SrCommand(this, spawnService, warpService, menuEngine);
        getCommand("sr").setExecutor(srCommand);
        getCommand("sr").setTabCompleter(srCommand);
        getCommand("register").setExecutor(new RegisterCommand(this, authService));
        getCommand("login").setExecutor(new LoginCommand(this, authService));
        LoggerUtil.item(log, "/sr            (alias: /survivalrealms)");
        LoggerUtil.item(log, "/register      (alias: /reg)");
        LoggerUtil.item(log, "/login         (alias: /l, /log)");
        LoggerUtil.done(log, "Comandos");

        LoggerUtil.section(log, "Listeners");
        getServer().getPluginManager().registerEvents(
                new AuthListener(this, authService, menuEngine), this);
        LoggerUtil.item(log, "AuthListener       registrado");
        LoggerUtil.done(log, "Listeners");

        LoggerUtil.printLine(log);
        LoggerUtil.success(log, "SurvivalRealms v" + getDescription().getVersion() + " habilitado.");
        LoggerUtil.printLine(log);
    }

    @Override
    public void onDisable() {
        Logger log = getLogger();
        LoggerUtil.printLine(log);
        if (placeholderAdapter != null) placeholderAdapter.unregister();
        if (repositories != null) repositories.closer().run();
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

    public AuthService    getAuthService()    { return authService; }
    public SpawnService   getSpawnService()   { return spawnService; }
    public WarpService    getWarpService()    { return warpService; }
    public UserService    getUserService()    { return userService; }
    public MenuEngine     getMenuEngine()     { return menuEngine; }
    public UserListGUI    getUserListGUI()    { return userListGUI; }
    public InventoryManager getInvManager()  { return invManager; }
}