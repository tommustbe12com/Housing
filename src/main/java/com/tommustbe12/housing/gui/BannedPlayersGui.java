package com.tommustbe12.housing.gui;

import com.tommustbe12.housing.groups.HouseGroupsService;
import com.tommustbe12.housing.groups.HousePermission;
import com.tommustbe12.housing.houses.HouseManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BannedPlayersGui {
    private static final String TITLE = "Banned Players";

    private final Plugin plugin;
    private final HouseManager houses;
    private final HouseGroupsService groups;
    private final Map<UUID, List<UUID>> orderByViewer = new ConcurrentHashMap<>();

    public BannedPlayersGui(Plugin plugin, HouseManager houses, HouseGroupsService groups) {
        this.plugin = plugin;
        this.houses = houses;
        this.groups = groups;
    }

    public boolean isTitle(String title) {
        return TITLE.equals(title);
    }

    public void open(Player viewer, Runnable back) {
        var info = houses.getHouseInfoByWorld(viewer.getWorld());
        if (info == null) return;
        boolean isOwner = info.owner().equals(viewer.getUniqueId());
        if (!isOwner && (groups == null || !groups.has(info.owner(), info.slot(), viewer.getUniqueId(), HousePermission.BAN))) {
            viewer.sendMessage("§cYou don't have permission to manage bans in this house.");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        fill(inv);

        Map<UUID, Boolean> banned = groups.members(info.owner(), info.slot()).banned();
        List<UUID> order = new ArrayList<>();
        for (var e : banned.entrySet()) if (Boolean.TRUE.equals(e.getValue())) order.add(e.getKey());
        orderByViewer.put(viewer.getUniqueId(), order);

        int i = 0;
        for (UUID pid : order) {
            if (i >= 45) break;
            OfflinePlayer off = Bukkit.getOfflinePlayer(pid);
            inv.setItem(i++, skull(off, "§c" + (off.getName() == null ? pid.toString() : off.getName()),
                    List.of("§7Click to unban")));
        }

        inv.setItem(53, named(Material.ARROW, "§7Back", List.of("§7Return.")));
        viewer.openInventory(inv);
    }

    public void handleClick(Player viewer, int rawSlot, ItemStack clicked, Runnable back) {
        if (clicked == null || clicked.getType().isAir()) return;
        var info = houses.getHouseInfoByWorld(viewer.getWorld());
        if (info == null) return;
        boolean isOwner = info.owner().equals(viewer.getUniqueId());
        if (!isOwner && (groups == null || !groups.has(info.owner(), info.slot(), viewer.getUniqueId(), HousePermission.BAN))) return;

        if (clicked.getType() == Material.ARROW) {
            back.run();
            return;
        }
        if (rawSlot < 0 || rawSlot >= 45) return;
        List<UUID> order = orderByViewer.get(viewer.getUniqueId());
        if (order == null || rawSlot >= order.size()) return;
        UUID target = order.get(rawSlot);
        groups.setBanned(info.owner(), info.slot(), target, false);
        viewer.sendMessage("§aUnbanned.");
        open(viewer, back);
    }

    private static ItemStack skull(OfflinePlayer owner, String name, List<String> lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta base = item.getItemMeta();
        if (base instanceof SkullMeta meta) {
            meta.setOwningPlayer(owner);
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
            return item;
        }
        return named(Material.PLAYER_HEAD, name, lore);
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

