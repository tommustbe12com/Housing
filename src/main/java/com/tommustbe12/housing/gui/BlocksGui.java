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

public final class BlocksGui {
    private static final String TITLE = "Blocks";

    private final HouseManager houses;
    private final HouseGroupsService groups;

    public BlocksGui(HouseManager houses, HouseGroupsService groups) {
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
        if (!owner && (groups == null || !groups.has(info.owner(), info.slot(), player.getUniqueId(), HousePermission.BUILD))) return;

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        fill(inv);
        inv.setItem(10, named(Material.BARRIER, "§cBarrier", List.of("§7Click to get 64.")));
        inv.setItem(12, named(Material.STRUCTURE_VOID, "§7Structure Void", List.of("§7Click to get 64.")));
        inv.setItem(49, named(Material.ARROW, "§7Back", List.of("§7Return.")));
        player.openInventory(inv);
    }

    public void handleClick(Player player, ItemStack clicked, Runnable back) {
        if (clicked == null) return;
        if (clicked.getType() == Material.ARROW) { back.run(); return; }
        if (clicked.getType() == Material.BARRIER || clicked.getType() == Material.STRUCTURE_VOID) {
            ItemStack give = new ItemStack(clicked.getType(), 64);
            player.getInventory().addItem(give);
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

