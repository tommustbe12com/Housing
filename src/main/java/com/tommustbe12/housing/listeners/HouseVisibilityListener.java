package com.tommustbe12.housing.listeners;

import com.tommustbe12.housing.houses.HouseManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

/**
 * Scopes the tab list and player visibility:
 * - Hub players only see hub players.
 * - House players only see players in the same house world.
 */
public final class HouseVisibilityListener implements Listener {
    private final Plugin plugin;
    private final HouseManager houses;

    public HouseVisibilityListener(Plugin plugin, HouseManager houses) {
        this.plugin = plugin;
        this.houses = houses;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, this::refreshAll, 1L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, this::refreshAll, 1L);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, this::refreshAll, 1L);
    }

    private void refreshAll() {
        World hub = resolveHubWorld();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (viewer == target) continue;
                boolean shouldSee = shouldSee(viewer, target, hub);
                if (shouldSee) {
                    viewer.showPlayer(plugin, target);
                } else {
                    viewer.hidePlayer(plugin, target);
                }
            }
        }
    }

    private boolean shouldSee(Player viewer, Player target, World hub) {
        var viewerInfo = houses.getHouseInfoByWorld(viewer.getWorld());
        var targetInfo = houses.getHouseInfoByWorld(target.getWorld());

        if (viewerInfo != null || targetInfo != null) {
            // House visibility: only same world (same house instance).
            return viewer.getWorld() == target.getWorld();
        }

        // Hub visibility: only the hub world.
        if (hub == null) return false;
        return viewer.getWorld() == hub && target.getWorld() == hub;
    }

    private World resolveHubWorld() {
        String hubWorldName = plugin.getConfig().getString("hub.world", "");
        if (hubWorldName == null || hubWorldName.isBlank()) return Bukkit.getWorlds().getFirst();
        World hub = Bukkit.getWorld(hubWorldName);
        return hub != null ? hub : Bukkit.getWorlds().getFirst();
    }
}

