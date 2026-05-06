package com.tommustbe12.housing.listeners;

import com.tommustbe12.housing.gui.NpcsGui;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

public final class NpcEquipCloseListener implements Listener {
    private final NpcsGui npcsGui;

    public NpcEquipCloseListener(NpcsGui npcsGui) {
        this.npcsGui = npcsGui;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!"NPC Equipment".equals(event.getView().getTitle())) return;
        if (!(event.getPlayer() instanceof Player player)) return;
        npcsGui.handleEquipClose(player, event.getInventory());
    }
}

