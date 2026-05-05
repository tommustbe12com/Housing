package com.tommustbe12.housing.listeners;

import com.tommustbe12.housing.actions.HouseActionsService;
import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.inventory.InventoryService;
import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public final class HouseRespawnListener implements Listener {
    private final Plugin plugin;
    private final HouseManager houses;
    private final HouseActionsService actions;
    private final InventoryService inventories;

    public HouseRespawnListener(Plugin plugin, HouseManager houses, HouseActionsService actions, InventoryService inventories) {
        this.plugin = plugin;
        this.houses = houses;
        this.actions = actions;
        this.inventories = inventories;
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        var info = houses.getHouseInfoByWorld(event.getPlayer().getWorld());
        if (info == null) return;

        Location spawn = houses.getSpawn(info.owner(), info.slot());
        if (spawn != null) {
            event.setRespawnLocation(spawn);
        }
        actions.runEvent(info.owner(), info.slot(), event.getPlayer().getWorld(), event.getPlayer(), "player_respawn");

        // Re-apply house inventory defaults (ensures nether star is present)
        Bukkit.getScheduler().runTaskLater(plugin, () -> inventories.applyHouseInventoryOrDefault(event.getPlayer(), info.owner(), info.slot()), 1L);
    }
}
