package com.tommustbe12.housing.listeners;

import com.tommustbe12.housing.debug.Debug;
import com.tommustbe12.housing.actions.HouseActionsService;
import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.inventory.InventoryService;
import com.tommustbe12.housing.tags.OwnerTagService;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public final class HouseWorldLifecycleListener implements Listener {
    private final Plugin plugin;
    private final Debug debug;
    private final HouseManager houses;
    private final OwnerTagService ownerTags;
    private final InventoryService inventories;
    private final HouseActionsService actions;
    private final com.tommustbe12.housing.scoreboard.HouseScoreboardService scoreboards;

    public HouseWorldLifecycleListener(Plugin plugin, Debug debug, HouseManager houses, OwnerTagService ownerTags, InventoryService inventories, HouseActionsService actions, com.tommustbe12.housing.scoreboard.HouseScoreboardService scoreboards) {
        this.plugin = plugin;
        this.debug = debug;
        this.houses = houses;
        this.ownerTags = ownerTags;
        this.inventories = inventories;
        this.actions = actions;
        this.scoreboards = scoreboards;
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        var player = event.getPlayer();

        var fromInfo = houses.getHouseInfoByWorld(event.getFrom());
        var toInfo = houses.getHouseInfoByWorld(player.getWorld());

        // Leaving a house world -> save that house inventory and schedule deactivate if empty
        if (fromInfo != null) {
            actions.runEvent(fromInfo.owner(), fromInfo.slot(), event.getFrom(), player, "player_quit");
            inventories.saveHouseInventory(player, fromInfo.owner(), fromInfo.slot());
            houses.scheduleDeactivateIfEmpty(event.getFrom());
        }

        // Entering a house world -> snapshot hub inv, apply house inv, apply tags
        if (toInfo != null) {
            if (fromInfo == null) {
                inventories.snapshotHubInventory(player);
            }
            inventories.applyHouseInventoryOrDefault(player, toInfo.owner(), toInfo.slot());
            houses.applyOwnerState(player, toInfo.owner());
            ownerTags.applyOwner(player, toInfo.owner());
            scoreboards.start(player, toInfo.owner(), toInfo.slot());
            actions.runEvent(toInfo.owner(), toInfo.slot(), player.getWorld(), player, "player_join");
        } else {
            // Leaving houses -> restore hub state
            if (fromInfo != null) {
                inventories.restoreHubInventory(player);
            }
            ownerTags.clear(player);
            scoreboards.stop(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        World world = event.getPlayer().getWorld();
        var info = houses.getHouseInfoByWorld(world);
        if (info != null) {
            inventories.saveHouseInventory(event.getPlayer(), info.owner(), info.slot());
            actions.runEvent(info.owner(), info.slot(), world, event.getPlayer(), "player_quit");
        }
        // Delay one tick so the quitter is removed from the world's player list
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            houses.scheduleDeactivateIfEmpty(world);
        }, 1L);
    }
}
