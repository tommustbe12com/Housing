package com.tommustbe12.housing.listeners;

import com.tommustbe12.housing.debug.Debug;
import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.tags.OwnerTagService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class PlayerJoinListener implements Listener {
    private final Debug debug;
    private final HouseItemListener houseItemListener;
    private final HouseManager houses;
    private final OwnerTagService ownerTags;

    public PlayerJoinListener(Debug debug, HouseItemListener houseItemListener, HouseManager houses, OwnerTagService ownerTags) {
        this.debug = debug;
        this.houseItemListener = houseItemListener;
        this.houses = houses;
        this.ownerTags = ownerTags;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        houseItemListener.giveMenuItem(event.getPlayer());
        // Safety: ensure no sticky tags when joining into hub/default world
        if (houses.getHouseInfoByWorld(event.getPlayer().getWorld()) == null) {
            ownerTags.clear(event.getPlayer());
        }
        debug.toOps("Gave housing menu item to " + event.getPlayer().getName());
    }
}
