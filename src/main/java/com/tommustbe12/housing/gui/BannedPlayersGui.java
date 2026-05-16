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
    private static final String TITLE_CONFIRM_PREFIX = "Unban: ";

    private final Plugin plugin;
    private final HouseManager houses;
    private final HouseGroupsService groups;
    private final Map<UUID, List<UUID>> orderByViewer = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> pendingUnbanByViewer = new ConcurrentHashMap<>();

    public BannedPlayersGui(Plugin plugin, HouseManager houses, HouseGroupsService groups) {
        this.plugin = plugin;
        this.houses = houses;
        this.groups = groups;
    }

    public boolean isTitle(String title) {
        return TITLE.equals(title) || (title != null && title.startsWith(TITLE_CONFIRM_PREFIX));
    }

    public void open(Player viewer, Runnable back) {
        pendingUnbanByViewer.remove(viewer.getUniqueId());
        var info = houses.getHouseInfoByWorld(viewer.getWorld());
        if (info == null) return;
        boolean isOwner = info.owner().equals(viewer.getUniqueId());
        if (!isOwner && (groups == null || !groups.has(info.owner(), info.slot(), viewer.getUniqueId(), HousePermission.BAN))) {
            viewer.sendMessage("Â§cYou don't have permission to manage bans in this house.");
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
            inv.setItem(i++, skull(off, "Â§c" + (off.getName() == null ? pid.toString() : off.getName()),
                    List.of("Â§7Click to unban")));
        }

        inv.setItem(53, named(Material.ARROW, "Â§7Back", List.of("Â§7Return.")));
        viewer.openInventory(inv);
    }

    public void handleClick(Player viewer, int rawSlot, ItemStack clicked, Runnable back) {
        if (clicked == null || clicked.getType().isAir()) return;
        var info = houses.getHouseInfoByWorld(viewer.getWorld());
        if (info == null) return;
        boolean isOwner = info.owner().equals(viewer.getUniqueId());
        if (!isOwner && (groups == null || !groups.has(info.owner(), info.slot(), viewer.getUniqueId(), HousePermission.BAN))) return;

        String title = viewer.getOpenInventory().getTitle();
        if (title != null && title.startsWith(TITLE_CONFIRM_PREFIX)) {
            if (clicked.getType() == Material.RED_CONCRETE || clicked.getType() == Material.ARROW) {
                open(viewer, back);
                return;
            }
            if (clicked.getType() == Material.LIME_CONCRETE) {
                UUID target = pendingUnbanByViewer.get(viewer.getUniqueId());
                if (target == null) {
                    open(viewer, back);
                    return;
                }
                groups.setBanned(info.owner(), info.slot(), target, false);
                viewer.sendMessage("Â§aUnbanned.");
                open(viewer, back);
                return;
            }
            return;
        }

        if (clicked.getType() == Material.ARROW) {
            back.run();
            return;
        }
        if (rawSlot < 0 || rawSlot >= 45) return;
        List<UUID> order = orderByViewer.get(viewer.getUniqueId());
        if (order == null || rawSlot >= order.size()) return;
        UUID target = order.get(rawSlot);
        openConfirm(viewer, target);
    }

    private void openConfirm(Player viewer, UUID target) {
        pendingUnbanByViewer.put(viewer.getUniqueId(), target);
        String name = Bukkit.getOfflinePlayer(target).getName();
        if (name == null || name.isBlank()) name = target.toString();

        Inventory inv = Bukkit.createInventory(null, 27, TITLE_CONFIRM_PREFIX + name);
        fill(inv);
        inv.setItem(11, named(Material.LIME_CONCRETE, "Â§aConfirm Unban", List.of("Â§7Are you sure you want to unban", "Â§f" + name + "Â§7?")));
        inv.setItem(15, named(Material.RED_CONCRETE, "Â§cCancel", List.of("Â§7Return to banned players list.")));
        inv.setItem(26, named(Material.ARROW, "Â§7Back", List.of("Â§7Return.")));
        viewer.openInventory(inv);
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

