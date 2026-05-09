package com.tommustbe12.housing.listeners;

import com.tommustbe12.housing.houses.HouseManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;

/**
 * WorldBorder only limits X/Z. This adds a simple Y cap for house worlds
 * so each house has a 256 (X) x 256 (Z) x 256 (Y) usable volume.
 */
public final class HouseVerticalBorderListener implements Listener {
    private final Plugin plugin;
    private final HouseManager houses;
    private final int minY;
    private final int maxY;

    public HouseVerticalBorderListener(Plugin plugin, HouseManager houses) {
        this.plugin = plugin;
        this.houses = houses;
        this.minY = plugin.getConfig().getInt("houses.world-border.min-y", 0);
        this.maxY = plugin.getConfig().getInt("houses.world-border.max-y", 255);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        if (houses.getHouseInfoByWorld(event.getPlayer().getWorld()) == null) return;

        int y = event.getTo().getBlockY();
        if (y < minY || y > maxY) {
            event.setTo(event.getFrom());
        }
    }
}

