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

    private static NamespacedKey key(Plugin plugin, String k) {
        return new NamespacedKey(plugin, k);
    }

    public static ItemStack createMenuStar(Plugin plugin) {
        ItemStack star = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = star.getItemMeta();
        meta.setDisplayName("§bHousing");
        meta.setLore(List.of("§7Right-click to open"));
        meta.getPersistentDataContainer().set(key(plugin, "housing_item"), PersistentDataType.BYTE, (byte) 1);
        star.setItemMeta(meta);
        return star;
    }

    public static boolean isMenuStar(Plugin plugin, ItemStack item) {
        if (item == null || item.getType() != Material.NETHER_STAR || !item.hasItemMeta()) return false;
        Byte b = item.getItemMeta().getPersistentDataContainer().get(key(plugin, "housing_item"), PersistentDataType.BYTE);
        return b != null && b == (byte) 1;
    }

    public static void ensureMenuStar(Plugin plugin, Player player) {
        if (player == null) return;
        ItemStack existing = player.getInventory().getItem(8);
        if (!isMenuStar(plugin, existing)) {
            player.getInventory().setItem(8, createMenuStar(plugin));
        }
    }

    public static ItemStack createRegionWand(Plugin plugin) {
        ItemStack wand = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = wand.getItemMeta();
        meta.setDisplayName("§eRegion Wand");
        meta.setLore(List.of("§7Left-click: set pos1", "§7Right-click: set pos2"));
        meta.getPersistentDataContainer().set(key(plugin, "region_wand"), PersistentDataType.BYTE, (byte) 1);
        wand.setItemMeta(meta);
        return wand;
    }

    public static boolean isRegionWand(Plugin plugin, ItemStack item) {
        if (item == null || item.getType() != Material.BLAZE_ROD || !item.hasItemMeta()) return false;
        Byte b = item.getItemMeta().getPersistentDataContainer().get(key(plugin, "region_wand"), PersistentDataType.BYTE);
        return b != null && b == (byte) 1;
    }

    public static ItemStack createHubHousesItem(Plugin plugin) {
        ItemStack item = new ItemStack(Material.OAK_DOOR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§aYour Houses");
        meta.setLore(List.of("§7Click to open your houses."));
        meta.getPersistentDataContainer().set(key(plugin, "hub_houses"), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isHubHousesItem(Plugin plugin, ItemStack item) {
        if (item == null || item.getType() != Material.OAK_DOOR || !item.hasItemMeta()) return false;
        Byte b = item.getItemMeta().getPersistentDataContainer().get(key(plugin, "hub_houses"), PersistentDataType.BYTE);
        return b != null && b == (byte) 1;
    }

    public static ItemStack createHubHotItem(Plugin plugin) {
        ItemStack item = new ItemStack(Material.FIREWORK_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§7Hot Houses");
        meta.setLore(List.of("§7Click to browse houses."));
        meta.getPersistentDataContainer().set(key(plugin, "hub_hot"), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isHubHotItem(Plugin plugin, ItemStack item) {
        if (item == null || item.getType() != Material.FIREWORK_STAR || !item.hasItemMeta()) return false;
        Byte b = item.getItemMeta().getPersistentDataContainer().get(key(plugin, "hub_hot"), PersistentDataType.BYTE);
        return b != null && b == (byte) 1;
    }

    public static ItemStack createHubCookiesLeftItem(Plugin plugin, int remaining) {
        ItemStack item = new ItemStack(Material.COOKIE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6Cookies Left: §f" + remaining);
        meta.setLore(List.of("§7You can give §f" + remaining + "§7 cookie(s) this week."));
        meta.getPersistentDataContainer().set(key(plugin, "hub_cookies_left"), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isHubCookiesLeftItem(Plugin plugin, ItemStack item) {
        if (item == null || item.getType() != Material.COOKIE || !item.hasItemMeta()) return false;
        Byte b = item.getItemMeta().getPersistentDataContainer().get(key(plugin, "hub_cookies_left"), PersistentDataType.BYTE);
        return b != null && b == (byte) 1;
    }

    public static ItemStack createNpcPlacerItem(Plugin plugin) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§bNPC Placer");
        meta.setLore(List.of("§7Right-click a block to place an NPC."));
        meta.getPersistentDataContainer().set(key(plugin, "npc_placer"), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isNpcPlacerItem(Plugin plugin, ItemStack item) {
        if (item == null || item.getType() != Material.PLAYER_HEAD || !item.hasItemMeta()) return false;
        Byte b = item.getItemMeta().getPersistentDataContainer().get(key(plugin, "npc_placer"), PersistentDataType.BYTE);
        return b != null && b == (byte) 1;
    }

    public static ItemStack createHologramPlacerItem(Plugin plugin) {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§dHologram Placer");
        meta.setLore(List.of("§7Right-click a block to place a hologram.", "§7Shift-right-click hologram to edit."));
        meta.getPersistentDataContainer().set(key(plugin, "holo_placer"), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isHologramPlacerItem(Plugin plugin, ItemStack item) {
        if (item == null || item.getType() != Material.NAME_TAG || !item.hasItemMeta()) return false;
        Byte b = item.getItemMeta().getPersistentDataContainer().get(key(plugin, "holo_placer"), PersistentDataType.BYTE);
        return b != null && b == (byte) 1;
    }
}
