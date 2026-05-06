package com.tommustbe12.housing.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;

public final class HousingItems {
    private HousingItems() {}

    public static ItemStack createMenuStar(Plugin plugin) {
        ItemStack star = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = star.getItemMeta();
        meta.setDisplayName("§bHousing");
        meta.setLore(List.of("§7Right-click to open"));
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "housing_item"), PersistentDataType.BYTE, (byte) 1);
        star.setItemMeta(meta);
        return star;
    }

    public static boolean isMenuStar(Plugin plugin, ItemStack item) {
        if (item == null || item.getType() != Material.NETHER_STAR || !item.hasItemMeta()) return false;
        Byte b = item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "housing_item"), PersistentDataType.BYTE);
        return b != null && b == (byte) 1;
    }

    public static void ensureMenuStar(Plugin plugin, Player player) {
        if (player == null) return;
        ItemStack existing = player.getInventory().getItem(8);
        if (!isMenuStar(plugin, existing)) {
            player.getInventory().setItem(8, createMenuStar(plugin));
        }
    }
}

