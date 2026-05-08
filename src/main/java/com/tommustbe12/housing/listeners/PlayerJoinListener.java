package com.tommustbe12.housing.listeners;

import com.tommustbe12.housing.debug.Debug;
import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.npcs.NpcManager;
import com.tommustbe12.housing.tags.OwnerTagService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class PlayerJoinListener implements Listener {
    private final Debug debug;
    private final HouseItemListener houseItemListener;
    private final HouseManager houses;
    private final OwnerTagService ownerTags;
    private final NpcManager npcs;

    public PlayerJoinListener(Debug debug, HouseItemListener houseItemListener, HouseManager houses, OwnerTagService ownerTags, NpcManager npcs) {
        this.debug = debug;
        this.houseItemListener = houseItemListener;
        this.houses = houses;
        this.ownerTags = ownerTags;
        this.npcs = npcs;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        houseItemListener.giveMenuItem(event.getPlayer());
        // Safety: ensure no sticky tags when joining into hub/default world
        var info = houses.getHouseInfoByWorld(event.getPlayer().getWorld());
        if (info == null) {
            ownerTags.clear(event.getPlayer());
        } else {
            npcs.spawnAll(info.owner(), info.slot(), event.getPlayer().getWorld());
        }
        debug.toOps("Gave housing menu item to " + event.getPlayer().getName());
    }
}
