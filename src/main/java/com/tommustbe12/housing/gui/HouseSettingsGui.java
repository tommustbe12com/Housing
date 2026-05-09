package com.tommustbe12.housing.gui;

import com.tommustbe12.housing.chat.ChatPrompts;
import com.tommustbe12.housing.groups.HouseGroupsService;
import com.tommustbe12.housing.groups.HousePermission;
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
    private final GroupsGui groupsGui;
    private final HouseGroupsService groups;
    private final BannedPlayersGui bannedGui;

    public HouseSettingsGui(Plugin plugin, ChatPrompts prompts, HouseManager houses, GroupsGui groupsGui, HouseGroupsService groups) {
        this.plugin = plugin;
        this.prompts = prompts;
        this.houses = houses;
        this.groupsGui = groupsGui;
        this.groups = groups;
        this.bannedGui = new BannedPlayersGui(plugin, houses, groups);
    }

    public boolean isTitle(String title) {
        return TITLE.equals(title);
    }

    public void open(Player player) {
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null) return;
        boolean isOwner = info.owner().equals(player.getUniqueId());
        if (!isOwner && (groups == null || !groups.has(info.owner(), info.slot(), player.getUniqueId(), HousePermission.CHANGE_SETTINGS))) {
            player.sendMessage("§cYou don't have permission to change settings in this house.");
            return;
        }
        HouseData data = houses.getHouse(info.owner(), info.slot());

        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        fill(inv);
        inv.setItem(10, named(Material.FILLED_MAP, "§bGroups", List.of("§7Edit groups and permissions.")));
        inv.setItem(12, named(Material.NAME_TAG, "§aHouse Name", List.of("§7Current:", "§f" + ChatColor.translateAlternateColorCodes('&', data.name()), "§7Click to edit")));
        inv.setItem(14, named(Material.COMPASS, "§bSet Spawn Here", List.of("§7Sets house spawn to your location.")));
        inv.setItem(16, named(Material.CLOCK, "§eTime", List.of("§7Current: §f" + data.timeOfDay(), "§7Click to +1000 (wraps at 24000).")));
        inv.setItem(18, named(Material.BARRIER, "§cBans", List.of("§7View and unban players.")));
        inv.setItem(26, named(Material.ARROW, "§7Back", List.of("§7Return.")));
        player.openInventory(inv);
    }

    public void handleClick(Player player, int rawSlot, ItemStack clicked, Runnable back) {
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null) return;
        boolean isOwner = info.owner().equals(player.getUniqueId());
        if (!isOwner && (groups == null || !groups.has(info.owner(), info.slot(), player.getUniqueId(), HousePermission.CHANGE_SETTINGS))) return;
        HouseData data = houses.getHouse(info.owner(), info.slot());

        if (clicked.getType() == Material.ARROW) {
            back.run();
            return;
        }
        if (clicked.getType() == Material.FILLED_MAP && groupsGui != null) {
            groupsGui.open(player, () -> open(player));
            return;
        }
        if (clicked.getType() == Material.BARRIER) {
            bannedGui.open(player, () -> open(player));
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
            long next = (data.timeOfDay() + 1000L) % 24000L;
            setTime(player, data, next);
            return;
        }
    }

    private void setTime(Player player, HouseData data, long time) {
        data.setTimeOfDay(time);
        houses.saveHouse(data);
        player.getWorld().setTime(time);
        player.sendMessage("§aTime updated to §f" + time + "§a.");
        open(player);
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
