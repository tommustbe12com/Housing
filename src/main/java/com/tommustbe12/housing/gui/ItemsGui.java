package com.tommustbe12.housing.gui;

import com.tommustbe12.housing.util.HousingItems;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.List;

public final class ItemsGui {
    private static final String TITLE = "Items";

    private final Plugin plugin;

    public ItemsGui(Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean isTitle(String title) {
        return TITLE.equals(title);
    }

    public void open(Player player, Runnable backToMain) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        fill(inv);
        inv.setItem(20, HousingItems.createNpcPlacerItem(plugin));
        inv.setItem(22, HousingItems.createHologramPlacerItem(plugin));
        inv.setItem(49, named(Material.ARROW, "§7Back", List.of("§7Return to main menu.")));
        player.openInventory(inv);
    }

    public void handleClick(Player player, ItemStack clicked, Runnable backToMain) {
        if (clicked == null) return;
        if (HousingItems.isNpcPlacerItem(plugin, clicked)) {
            player.getInventory().addItem(HousingItems.createNpcPlacerItem(plugin));
            player.sendMessage("§aGiven NPC placer.");
            return;
        }
        if (HousingItems.isHologramPlacerItem(plugin, clicked)) {
            player.getInventory().addItem(HousingItems.createHologramPlacerItem(plugin));
            player.sendMessage("§aGiven hologram placer.");
            return;
        }
        if (clicked.getType() == Material.ARROW) backToMain.run();
    }

    private static ItemStack named(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static void fill(Inventory inv) {
        ItemStack filler = named(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inv.getSize(); i++) if (inv.getItem(i) == null) inv.setItem(i, filler);
    }
}
