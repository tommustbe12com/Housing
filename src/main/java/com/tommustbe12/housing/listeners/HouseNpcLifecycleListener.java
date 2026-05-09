package com.tommustbe12.housing.listeners;

import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.npcs.NpcManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public final class HouseNpcLifecycleListener implements Listener {
    private final HouseManager houses;
    private final NpcManager npcs;

    public HouseNpcLifecycleListener(HouseManager houses, NpcManager npcs) {
        this.houses = houses;
        this.npcs = npcs;
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        var to = event.getPlayer().getWorld();
        var info = houses.getHouseInfoByWorld(to);
        if (info != null) {
            npcs.spawnAll(info.owner(), info.slot(), to);
        }
    }
}
