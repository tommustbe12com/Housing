package com.tommustbe12.housing.listeners;

import com.tommustbe12.housing.debug.Debug;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerQuitListener implements Listener {
    private final Debug debug;

    public PlayerQuitListener(Debug debug) {
        this.debug = debug;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        debug.toOps(event.getPlayer().getName() + " quit.");
    }
}

