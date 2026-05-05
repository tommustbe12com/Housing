package com.tommustbe12.housing.listeners;

import com.tommustbe12.housing.gui.ItemEditGui;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class ItemEditGuiListener implements Listener {
    private final ItemEditGui gui;

    public ItemEditGuiListener(ItemEditGui gui) {
        this.gui = gui;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!gui.isTitle(title) && !"Add Action".equals(title) && !(title != null && title.startsWith("Actions: "))) return;
        // "Add Action" and "Actions:" are handled by HouseItemListener already; only handle item edit windows here.
        if (!gui.isTitle(title)) return;
        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;
        gui.handleClick(player, title, event.getRawSlot(), event.getCurrentItem());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (gui.isTitle(title)) event.setCancelled(true);
    }
}

