package org.falmdev.survivalRealms.util;

import java.util.logging.Logger;

public final class LoggerUtil {

    public static final String RESET  = "\u001B[0m";
    public static final String CYAN   = "\u001B[36m";
    public static final String YELLOW = "\u001B[33m";
    public static final String GREEN  = "\u001B[32m";
    public static final String RED    = "\u001B[31m";
    public static final String GRAY   = "\u001B[90m";
    public static final String WHITE  = "\u001B[97m";
    public static final String BOLD   = "\u001B[1m";

    private LoggerUtil() {}

    public static void section(Logger log, String name) {
        log.info(GRAY + "  (" + CYAN + name + GRAY + ") -> Iniciando..." + RESET);
    }

    public static void done(Logger log, String name) {
        log.info(GRAY + "  (" + CYAN + name + GRAY + ") -> " + GREEN + "OK" + RESET);
    }

    public static void done(Logger log, String name, String extra) {
        log.info(GRAY + "  (" + CYAN + name + GRAY + ") -> " + GREEN + "OK"
                + GRAY + "  (" + extra + ")" + RESET);
    }

    public static void fail(Logger log, String name, String reason) {
        log.severe(GRAY + "  (" + RED + name + GRAY + ") -> " + RED + "FALLO: " + reason + RESET);
    }

    public static void warn(Logger log, String name, String reason) {
        log.warning(GRAY + "  (" + YELLOW + name + GRAY + ") -> " + YELLOW + "WARN: " + reason + RESET);
    }

    public static void item(Logger log, String detail) {
        log.info(GRAY + "    \u2022 " + WHITE + detail + RESET);
    }

    public static void item(Logger log, String key, String value) {
        log.info(GRAY + "    \u2022 " + WHITE + key + GRAY + " = " + YELLOW + value + RESET);
    }

    public static String line() {
        return GRAY + "  \u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500"
                + "\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500"
                + "\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500"
                + "\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500"
                + "\u2500\u2500\u2500\u2500\u2500\u2500\u2500" + RESET;
    }

    public static void printLine(Logger log) {
        log.info(line());
    }

    public static void success(Logger log, String message) {
        log.info(GRAY + "  " + GREEN + BOLD + "\u2714 " + WHITE + message + RESET);
    }

    public static void error(Logger log, String message) {
        log.severe(GRAY + "  " + RED + BOLD + "\u2718 " + WHITE + message + RESET);
    }

    public static void printInfoBox(Logger log, String pluginName, String version,
                                    String description, String author) {
        log.info(GRAY + "  \u250c\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500"
                + "\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500"
                + "\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500"
                + "\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500"
                + "\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2510" + RESET);
        log.info(GRAY + "  \u2502  " + WHITE + BOLD + pluginName + RESET
                + GRAY + "  v" + YELLOW + version
                + GRAY + pad(pluginName.length() + version.length()) + "\u2502" + RESET);
        log.info(GRAY + "  \u2502  " + GRAY + description
                + GRAY + pad(description.length() - 2) + "\u2502" + RESET);
        log.info(GRAY + "  \u2502  " + GRAY + "Desarrollado por " + CYAN + author
                + GRAY + pad(author.length() + 15) + "\u2502" + RESET);
        log.info(GRAY + "  \u2514\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500"
                + "\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500"
                + "\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500"
                + "\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500"
                + "\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2518" + RESET);
        log.info(" ");
    }

    private static String pad(int contentLength) {
        int total = 43; //
        int spaces = total - contentLength;
        if (spaces <= 0) return " ";
        return " ".repeat(spaces);
    }
}