package org.falmdev.survivalRealms.gui;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.SlotPos;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.falmdev.survivalRealms.SurvivalRealms;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import java.util.logging.Level;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public final class GuiLayout {

    private static final String TEXTURE_HOME  = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDA5ODc3OTJjNWFjNDVmMDhlNjkyY2FiZjliOTY2MWYyYzc5ZDBkOGQxNDJmOTk1MWFiMmUwNjQ2YTg1NTgxNiJ9fX0=";
    private static final String TEXTURE_WARPS = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjRjYzEzMWE2NDY5NzUxMzhkNWQ1ZmNiZDlkNGYzYTk3ZGMzN2QxYmI2ZDQyYTdjNTNhZDMwMmFlMTdiNmViYiJ9fX0=";
    private static final String TEXTURE_BACK  = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmIwZjZlOGFmNDZhYzZmYWY4ODkxNDE5MWFiNjZmMjYxZDY3MjZhNzk5OWM2MzdjZjJlNDE1OWZlMWZjNDc3In19fQ==";
    private static final String TEXTURE_PREV  = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWUzZDgyMjVmOGY1Yjk4NTk3ZGYxNWZkOTJiZjY5NTlhZWZkNGM1YmVjOTkxNGRkNjNjYWEwYzMyOWM3YTA2YiJ9fX0=";
    private static final String TEXTURE_NEXT  = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTNlYTY1MTI3MTNiN2JiOGZlZTM3N2E5ODczMjM3ODc1N2YyNmRmODQzNGQwZmFmODk3YWRiNzU2NCJ9fX0=";

    public static final SlotPos SLOT_PREV_PAGE = SlotPos.of(5, 2);
    public static final SlotPos SLOT_BACK      = SlotPos.of(5, 3);
    public static final SlotPos SLOT_WARPS     = SlotPos.of(5, 4);
    public static final SlotPos SLOT_HOME      = SlotPos.of(5, 5);
    public static final SlotPos SLOT_NEXT_PAGE = SlotPos.of(5, 6);

    public static final SlotPos[] CONTENT_SLOTS;

    static {
        int[] excluded = {0, 1, 7, 8, 9, 17, 36, 44};
        java.util.List<SlotPos> slots = new java.util.ArrayList<>();
        for (int row = 0; row <= 4; row++) {
            for (int col = 0; col < 9; col++) {
                int slot = row * 9 + col;
                boolean isExcluded = false;
                for (int ex : excluded) if (ex == slot) { isExcluded = true; break; }
                if (!isExcluded) slots.add(SlotPos.of(row, col));
            }
        }
        CONTENT_SLOTS = slots.toArray(new SlotPos[0]);
    }

    private GuiLayout() {}

    public static void apply(InventoryContents contents,
                             SurvivalRealms plugin,
                             GuiActions actions) {

        ItemStack gray  = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        ItemStack home  = makeSkull(TEXTURE_HOME,  "§6§lInicio");
        ItemStack warps = makeSkull(TEXTURE_WARPS, "§b§lWarps");
        ItemStack back  = makeSkull(TEXTURE_BACK,  "§f§lAtrás");

        // Esquinas grises
        contents.set(0, 0, ClickableItem.empty(gray));
        contents.set(0, 1, ClickableItem.empty(gray));
        contents.set(0, 7, ClickableItem.empty(gray));
        contents.set(0, 8, ClickableItem.empty(gray));
        contents.set(1, 0, ClickableItem.empty(gray));
        contents.set(1, 8, ClickableItem.empty(gray));
        contents.set(4, 0, ClickableItem.empty(gray));
        contents.set(4, 8, ClickableItem.empty(gray));
        contents.set(5, 0, ClickableItem.empty(gray));
        contents.set(5, 1, ClickableItem.empty(gray));
        contents.set(5, 7, ClickableItem.empty(gray));
        contents.set(5, 8, ClickableItem.empty(gray));

        // Botones fijos
        contents.set(SLOT_BACK, ClickableItem.of(back,
                e -> actions.onBack((Player) e.getWhoClicked())));

        contents.set(SLOT_WARPS, ClickableItem.of(warps,
                e -> actions.onWarps((Player) e.getWhoClicked())));

        contents.set(SLOT_HOME, ClickableItem.of(home,
                e -> actions.onHome((Player) e.getWhoClicked())));

        // prev/next no se ponen aquí — cada GUI los agrega solo si hay más páginas
    }

    // ── Skull con textura base64 usando PlayerProfile (Paper 1.21) ────────────

    public static ItemStack makeSkull(String base64, String name, String... lore) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta  = (SkullMeta) skull.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(Arrays.asList(lore));

        try {
            // Decodificar base64 para extraer la URL de la textura
            String decoded  = new String(Base64.getDecoder().decode(base64));
            JSONObject root = (JSONObject) JSONValue.parse(decoded);
            JSONObject textures = (JSONObject) root.get("textures");
            JSONObject skin     = (JSONObject) textures.get("SKIN");
            String url          = (String) skin.get("url");

            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
            PlayerTextures playerTextures = profile.getTextures();
            playerTextures.setSkin(new URL(url));
            profile.setTextures(playerTextures);
            meta.setOwnerProfile(profile);

        } catch (MalformedURLException | NullPointerException | IllegalArgumentException e) {
            // Fallback silencioso — quedará como cabeza genérica
        }

        skull.setItemMeta(meta);
        return skull;
    }

    public static ItemStack makeItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta  = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    public static String getTexturePrev() { return TEXTURE_PREV; }
    public static String getTextureNext() { return TEXTURE_NEXT; }

    public interface GuiActions {
        void onHome(Player player);
        void onWarps(Player player);
        void onBack(Player player);
        void onPrevPage(Player player);
        void onNextPage(Player player);
    }
}