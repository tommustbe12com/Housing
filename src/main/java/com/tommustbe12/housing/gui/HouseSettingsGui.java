package com.tommustbe12.housing.gui;

import com.tommustbe12.housing.chat.ChatPrompts;
import com.tommustbe12.housing.houses.HouseData;
import com.tommustbe12.housing.houses.HouseManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.List;

public final class HouseSettingsGui {
    private static final String TITLE = "House Settings";

    private final Plugin plugin;
    private final ChatPrompts prompts;
    private final HouseManager houses;

    public HouseSettingsGui(Plugin plugin, ChatPrompts prompts, HouseManager houses) {
        this.plugin = plugin;
        this.prompts = prompts;
        this.houses = houses;
    }

    public boolean isTitle(String title) {
        return TITLE.equals(title);
    }

    public void open(Player player) {
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null || !info.owner().equals(player.getUniqueId())) {
            player.sendMessage("§cSettings is only available in your own house.");
            return;
        }
        HouseData data = houses.getHouse(info.owner(), info.slot());

        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        fill(inv);
        inv.setItem(11, named(Material.NAME_TAG, "§aHouse Name", List.of("§7Current:", "§f" + ChatColor.translateAlternateColorCodes('&', data.name()), "§7Click to edit")));
        inv.setItem(13, named(Material.COMPASS, "§bSet Spawn Here", List.of("§7Sets house spawn to your location.")));
        inv.setItem(15, named(Material.CLOCK, "§eTime Of Day", List.of("§7Current: §f" + data.timeOfDay(), "§7Click to set to current world time")));
        inv.setItem(26, named(Material.ARROW, "§7Back", List.of("§7Return.")));
        player.openInventory(inv);
    }

    public void handleClick(Player player, int rawSlot, ItemStack clicked, Runnable back) {
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null || !info.owner().equals(player.getUniqueId())) return;
        HouseData data = houses.getHouse(info.owner(), info.slot());

        if (clicked.getType() == Material.ARROW) {
            back.run();
            return;
        }
        if (clicked.getType() == Material.NAME_TAG) {
            prompts.prompt(player, "Enter house name (& codes allowed):", msg -> {
                if (msg.equalsIgnoreCase("cancel")) return;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    data.setName(msg);
                    houses.saveHouse(data);
                    open(player);
                });
            });
            return;
        }
        if (clicked.getType() == Material.COMPASS) {
            data.setSpawn(player.getLocation());
            houses.saveHouse(data);
            player.sendMessage("§aHouse spawn updated.");
            open(player);
            return;
        }
        if (clicked.getType() == Material.CLOCK) {
            data.setTimeOfDay(player.getWorld().getTime());
            houses.saveHouse(data);
            player.sendMessage("§aTime updated.");
            open(player);
        }
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

