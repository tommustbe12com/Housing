package com.tommustbe12.housing.listeners;

import com.tommustbe12.housing.gui.CustomMenusGui;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

public final class CustomMenusGuiListener implements Listener {
    private final CustomMenusGui gui;

    public CustomMenusGuiListener(CustomMenusGui gui) {
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

        // Always allow interacting with the player's inventory normally (but block shift-click auto-moving into the editor).
        if (raw >= topSize) {
            if (title != null && title.startsWith("Edit Menu: ") && event.getClick().isShiftClick()) event.setCancelled(true);
            return;
        }

        // In the editor (top inventory), allow placing items, but protect the control row from taking items.
        if (title != null && title.startsWith("Edit Menu: ")) {
            if (raw >= 45 && raw < topSize) event.setCancelled(true);
            if (raw >= 27 && raw < 45) event.setCancelled(true);

            // For action editing: right-clicking an item should never move it, it should open the action editor.
            if (raw >= 0 && raw < 27 && event.getClick().isRightClick()) event.setCancelled(true);
        } else {
            event.setCancelled(true);
        }

        ItemStack clicked = event.getCurrentItem();
        gui.handleClick(player, title, raw, clicked, event.getClick());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!gui.isTitle(title)) return;
        if (title != null && title.startsWith("Edit Menu: ")) {
            for (int slot : event.getRawSlots()) {
                int topSize = event.getView().getTopInventory().getSize();
                if (slot >= topSize) continue;
                if (slot >= 27 && slot < 45) {
                    event.setCancelled(true);
                    return;
                }
                if (slot >= 45 && slot < topSize) {
                    event.setCancelled(true);
                    return;
                }
            }
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!gui.isTitle(title)) return;
        // Don't clear edit state when transitioning from list -> editor (the list inventory closes).
        if (title != null && title.startsWith("Edit Menu: ")) gui.handleClose(player, event.getInventory());
    }
}
