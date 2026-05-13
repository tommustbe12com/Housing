package com.tommustbe12.housing.gui;

import com.tommustbe12.housing.groups.HouseGroupsService;
import com.tommustbe12.housing.groups.HousePermission;
import com.tommustbe12.housing.houses.HouseManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class HousePlayersGui {
    private static final String TITLE = "Players Here";

    private final HouseManager houses;
    private final HouseGroupsService groups;
    private final PlayerSettingsGui playerSettings;

    public HousePlayersGui(HouseManager houses, HouseGroupsService groups, PlayerSettingsGui playerSettings) {
        this.houses = houses;
        this.groups = groups;
        this.playerSettings = playerSettings;
    }

    public boolean isTitle(String title) {
        return TITLE.equals(title);
    }

    public void open(Player viewer, Runnable back) {
        var info = houses.getHouseInfoByWorld(viewer.getWorld());
        if (info == null) return;
        boolean owner = info.owner().equals(viewer.getUniqueId());
        if (!owner && (groups == null || !groups.has(info.owner(), info.slot(), viewer.getUniqueId(), HousePermission.CHANGE_GROUP))) return;

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        fill(inv);

        int slot = 0;
        for (Player p : viewer.getWorld().getPlayers()) {
            if (slot >= 45) break;
            inv.setItem(slot++, named(Material.PLAYER_HEAD, "§b" + p.getName(), List.of("§7Click to manage.")));
        }

        inv.setItem(49, named(Material.ARROW, "§7Back", List.of("§7Return.")));
        viewer.openInventory(inv);
    }

    public void handleClick(Player viewer, int rawSlot, ItemStack clicked, Runnable back) {
        if (clicked == null) return;
        if (clicked.getType() == Material.ARROW) { back.run(); return; }
        if (rawSlot < 0 || rawSlot >= 45) return;
        String name = org.bukkit.ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        if (name == null || name.isBlank()) return;
        Player target = Bukkit.getPlayerExact(name.trim());
        if (target == null) return;
        playerSettings.open(viewer, target, () -> open(viewer, back));
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

