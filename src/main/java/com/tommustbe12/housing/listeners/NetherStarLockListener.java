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

        // block moving/removing the menu star from slot 8
        if (event.getRawSlot() == 8) {
            event.setCancelled(true);
            HousingItems.ensureMenuStar(plugin, player);
            return;
        }

        // block placing the menu star into any other slot
        if (HousingItems.isMenuStar(plugin, cursor) || HousingItems.isMenuStar(plugin, current)) {
            if (event.getRawSlot() != 8) {
                event.setCancelled(true);
                HousingItems.ensureMenuStar(plugin, player);
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getRawSlots().contains(8)) {
            event.setCancelled(true);
            HousingItems.ensureMenuStar(plugin, player);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (HousingItems.isMenuStar(plugin, event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            HousingItems.ensureMenuStar(plugin, event.getPlayer());
        }
    }
}

