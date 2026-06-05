package org.falmdev.survivalRealms.presentation.menu;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.content.InventoryContents;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class MenuLayout {

    private MenuLayout() {}

    public static void applyBorder(InventoryContents contents) {
        ItemStack filler = buildFiller();
        int rows = contents.inventory().getRows();
        int cols = 9;
        for (int col = 0; col < cols; col++) {
            contents.set(0, col, ClickableItem.empty(filler));
            contents.set(rows - 1, col, ClickableItem.empty(filler));
        }
        for (int row = 1; row < rows - 1; row++) {
            contents.set(row, 0, ClickableItem.empty(filler));
            contents.set(row, cols - 1, ClickableItem.empty(filler));
        }
    }

    private static ItemStack buildFiller() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta  meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }
}