package com.tommustbe12.housing.listeners;

import com.tommustbe12.housing.util.HousingItems;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public final class NetherStarLockListener implements Listener {
    private final Plugin plugin;

    public NetherStarLockListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        // Hub items lock (0,1,8)
        if (event.getRawSlot() == 0 && HousingItems.isHubHousesItem(plugin, current)) { event.setCancelled(true); return; }
        if (event.getRawSlot() == 1 && HousingItems.isHubHotItem(plugin, current)) { event.setCancelled(true); return; }
        if (event.getRawSlot() == 8 && HousingItems.isHubCookiesLeftItem(plugin, current)) { event.setCancelled(true); return; }

        // House menu star lock (slot 8)
        if (event.getRawSlot() == 8 && HousingItems.isMenuStar(plugin, current)) {
            event.setCancelled(true);
            HousingItems.ensureMenuStar(plugin, player);
            return;
        }

        // block placing locked items into other slots
        boolean locked = HousingItems.isMenuStar(plugin, cursor)
                || HousingItems.isHubHousesItem(plugin, cursor)
                || HousingItems.isHubHotItem(plugin, cursor)
                || HousingItems.isHubCookiesLeftItem(plugin, cursor)
                || HousingItems.isMenuStar(plugin, current)
                || HousingItems.isHubHousesItem(plugin, current)
                || HousingItems.isHubHotItem(plugin, current)
                || HousingItems.isHubCookiesLeftItem(plugin, current);
        if (locked) {
            int rawSlot = event.getRawSlot();
            boolean ok = (rawSlot == 8 && HousingItems.isMenuStar(plugin, current))
                    || (rawSlot == 0 && HousingItems.isHubHousesItem(plugin, current))
                    || (rawSlot == 1 && HousingItems.isHubHotItem(plugin, current))
                    || (rawSlot == 8 && HousingItems.isHubCookiesLeftItem(plugin, current));
            if (!ok) event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getRawSlots().contains(8) || event.getRawSlots().contains(0) || event.getRawSlots().contains(1)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack it = event.getItemDrop().getItemStack();
        if (HousingItems.isMenuStar(plugin, it)
                || HousingItems.isHubHousesItem(plugin, it)
                || HousingItems.isHubHotItem(plugin, it)
                || HousingItems.isHubCookiesLeftItem(plugin, it)) {
            event.setCancelled(true);
        }
    }
}
