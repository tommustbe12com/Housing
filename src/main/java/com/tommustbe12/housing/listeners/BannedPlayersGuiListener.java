package com.tommustbe12.housing.listeners;

import com.tommustbe12.housing.gui.BannedPlayersGui;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

public final class BannedPlayersGuiListener implements Listener {
    private final BannedPlayersGui gui;

    public BannedPlayersGuiListener(BannedPlayersGui gui) {
        this.gui = gui;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getView().getType() != InventoryType.CHEST) return;
        String title = event.getView().getTitle();
        if (!gui.isTitle(title)) return;
        int topSize = event.getView().getTopInventory().getSize();
        int raw = event.getRawSlot();
        if (raw >= topSize) return;
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        gui.handleClick(player, raw, clicked, () -> player.closeInventory());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!gui.isTitle(title)) return;
        event.setCancelled(true);
    }
}
