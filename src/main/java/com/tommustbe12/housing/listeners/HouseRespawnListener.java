package com.tommustbe12.housing.listeners;

import com.tommustbe12.housing.actions.HouseActionsService;
import com.tommustbe12.housing.houses.HouseManager;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public final class HouseRespawnListener implements Listener {
    private final HouseManager houses;
    private final HouseActionsService actions;

    public HouseRespawnListener(HouseManager houses, HouseActionsService actions) {
        this.houses = houses;
        this.actions = actions;
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
    }
}
