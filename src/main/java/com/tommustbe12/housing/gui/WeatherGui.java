package com.tommustbe12.housing.gui;

import com.tommustbe12.housing.groups.HouseGroupsService;
import com.tommustbe12.housing.groups.HousePermission;
import com.tommustbe12.housing.houses.HouseData;
import com.tommustbe12.housing.houses.HouseManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.List;

public final class WeatherGui {
    private static final String TITLE = "Weather";

    private final Plugin plugin;
    private final HouseManager houses;
    private final HouseGroupsService groups;

    public WeatherGui(Plugin plugin, HouseManager houses, HouseGroupsService groups) {
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

        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        fill(inv);
        inv.setItem(10, named(Material.SUNFLOWER, "§eSunny", List.of("§7Set clear weather.")));
        inv.setItem(12, named(Material.WATER_BUCKET, "§bRain", List.of("§7Set rain.")));
        inv.setItem(14, named(Material.TRIDENT, "§9Thunder", List.of("§7Set thunderstorm.")));
        inv.setItem(22, named(Material.ARROW, "§7Back", List.of("§7Return.")));
        player.openInventory(inv);
    }

    public void handleClick(Player player, ItemStack clicked, Runnable back) {
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null) return;
        boolean owner = info.owner().equals(player.getUniqueId());
        if (!owner && (groups == null || !groups.has(info.owner(), info.slot(), player.getUniqueId(), HousePermission.CHANGE_SETTINGS))) return;

        if (clicked == null) return;
        if (clicked.getType() == Material.ARROW) { back.run(); return; }

        HouseData data = houses.getHouse(info.owner(), info.slot());
        if (clicked.getType() == Material.SUNFLOWER) data.setWeather("SUNNY");
        if (clicked.getType() == Material.WATER_BUCKET) data.setWeather("RAIN");
        if (clicked.getType() == Material.TRIDENT) data.setWeather("THUNDER");
        houses.saveHouse(data);
        houses.applyWeather(player.getWorld(), data.weather());
        open(player, back);
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
