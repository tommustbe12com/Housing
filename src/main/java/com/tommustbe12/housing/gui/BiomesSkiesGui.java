package com.tommustbe12.housing.gui;

import com.tommustbe12.housing.groups.HouseGroupsService;
import com.tommustbe12.housing.groups.HousePermission;
import com.tommustbe12.housing.houses.HouseData;
import com.tommustbe12.housing.houses.HouseManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class BiomesSkiesGui {
    private static final String TITLE = "Biomes & Skies";

    private final Plugin plugin;
    private final HouseManager houses;
    private final HouseGroupsService groups;

    public BiomesSkiesGui(Plugin plugin, HouseManager houses, HouseGroupsService groups) {
        this.plugin = plugin;
        this.houses = houses;
        this.groups = groups;
    }

    public boolean isTitle(String title) {
        return TITLE.equals(title);
    }

    public void open(Player player, Runnable back) {
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null) return;
        boolean owner = info.owner().equals(player.getUniqueId());
        if (!owner && (groups == null || !groups.has(info.owner(), info.slot(), player.getUniqueId(), HousePermission.CHANGE_SETTINGS))) return;

        HouseData data = houses.getHouse(info.owner(), info.slot());
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        fill(inv);

        inv.setItem(10, named(Material.GRASS_BLOCK, "§aSky: Overworld", List.of("§7(Current: §f" + data.sky() + "§7)")));
        inv.setItem(12, named(Material.NETHERRACK, "§cSky: Nether", List.of("§7(Current: §f" + data.sky() + "§7)")));
        inv.setItem(14, named(Material.END_STONE, "§dSky: End", List.of("§7(Current: §f" + data.sky() + "§7)")));

        // Biome picker (simple curated list)
        List<Biome> biomes = List.of(
                Biome.PLAINS, Biome.FOREST, Biome.DESERT, Biome.SAVANNA,
                Biome.SNOWY_PLAINS, Biome.JUNGLE, Biome.SWAMP, Biome.MUSHROOM_FIELDS
        );
        int slot = 27;
        for (Biome b : biomes) {
            inv.setItem(slot++, named(Material.OAK_SAPLING, "§eBiome: §f" + b.name(), List.of("§7Click to apply to your house.")));
            if (slot >= 45) break;
        }

        inv.setItem(49, named(Material.ARROW, "§7Back", List.of("§7Return.")));
        player.openInventory(inv);
    }

    public void handleClick(Player player, int rawSlot, ItemStack clicked, Runnable back) {
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null) return;
        boolean owner = info.owner().equals(player.getUniqueId());
        if (!owner && (groups == null || !groups.has(info.owner(), info.slot(), player.getUniqueId(), HousePermission.CHANGE_SETTINGS))) return;
        if (clicked == null) return;

        if (clicked.getType() == Material.ARROW) { back.run(); return; }

        HouseData data = houses.getHouse(info.owner(), info.slot());
        if (clicked.getType() == Material.GRASS_BLOCK) data.setSky("OVERWORLD");
        if (clicked.getType() == Material.NETHERRACK) data.setSky("NETHER");
        if (clicked.getType() == Material.END_STONE) data.setSky("END");

        // Biome apply items start at row 4
        if (rawSlot >= 27 && rawSlot < 45) {
            String name = strip(clicked);
            if (name != null && name.toUpperCase(Locale.ROOT).startsWith("BIOME:")) {
                String biomeName = name.substring("BIOME:".length()).trim();
                try {
                    Biome b = Biome.valueOf(biomeName.toUpperCase(Locale.ROOT));
                    data.setBiome(b.name());
                    houses.saveHouse(data);
                    houses.applyBiome(player.getWorld(), data.biome());
                    open(player, back);
                    return;
                } catch (Exception ignored) {
                }
            }
        }

        houses.saveHouse(data);
        open(player, back);
    }

    private static String strip(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String dn = item.getItemMeta().getDisplayName();
        if (dn == null) return null;
        return org.bukkit.ChatColor.stripColor(dn);
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

