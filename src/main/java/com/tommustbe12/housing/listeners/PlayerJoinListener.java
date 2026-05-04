package com.tommustbe12.housing.listeners;

import com.tommustbe12.housing.debug.Debug;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class PlayerJoinListener implements Listener {
    private final Debug debug;
    private final HouseItemListener houseItemListener;

    public PlayerJoinListener(Debug debug, HouseItemListener houseItemListener) {
        this.debug = debug;
        this.houseItemListener = houseItemListener;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        houseItemListener.giveMenuItem(event.getPlayer());
        debug.toOps("Gave housing menu item to " + event.getPlayer().getName());
    }
}

