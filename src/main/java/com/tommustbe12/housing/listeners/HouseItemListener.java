package com.tommustbe12.housing.listeners;

import com.tommustbe12.housing.debug.Debug;
import com.tommustbe12.housing.houses.HouseManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.List;

public final class HouseItemListener implements Listener {
    private final Plugin plugin;
    private final Debug debug;
    private final HouseManager houses;

    public HouseItemListener(Plugin plugin, Debug debug, HouseManager houses) {
        this.plugin = plugin;
        this.debug = debug;
        this.houses = houses;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getAction().isRightClick()) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.NETHER_STAR) return;
        if (!item.hasItemMeta()) return;
        if (!"Housing Menu".equals(item.getItemMeta().getDisplayName())) return;

        event.setCancelled(true);
        openMainMenu(player);
        debug.to(player, "Opened main nether-star menu.");
    }

    public void giveMenuItem(Player player) {
        ItemStack star = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = star.getItemMeta();
        meta.setDisplayName("Housing Menu");
        meta.setLore(List.of("§7Right-click to open"));
        star.setItemMeta(meta);
        player.getInventory().setItem(8, star);
    }

    private void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "Housing");
        inv.setItem(11, named(Material.REPEATER, "§bSystems", List.of("§7Coming soon: regions, actions, commands...")));
        inv.setItem(13, named(Material.COOKIE, "§6Cookies", List.of("§7Use §f/house cookie give§7 to rate houses.")));
        inv.setItem(15, named(Material.NETHER_STAR, "§eMy House", List.of("§7Use §f/house join <you> <slot>§7.")));
        player.openInventory(inv);
    }

    private static ItemStack named(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}

