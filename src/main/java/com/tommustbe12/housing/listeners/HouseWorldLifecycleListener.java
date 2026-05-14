package com.tommustbe12.housing.listeners;

import com.tommustbe12.housing.debug.Debug;
import com.tommustbe12.housing.actions.HouseActionsService;
import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.inventory.InventoryService;
import com.tommustbe12.housing.tags.OwnerTagService;
import com.tommustbe12.housing.groups.HouseGroupsService;
import com.tommustbe12.housing.teams.TeamsService;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public final class HouseWorldLifecycleListener implements Listener {
    private final Plugin plugin;
    private final Debug debug;
    private final HouseManager houses;
    private final OwnerTagService ownerTags;
    private final InventoryService inventories;
    private final HouseActionsService actions;
    private final com.tommustbe12.housing.scoreboard.HouseScoreboardService scoreboards;
    private final HouseGroupsService groups;
    private final TeamsService teams;

    public HouseWorldLifecycleListener(Plugin plugin, Debug debug, HouseManager houses, OwnerTagService ownerTags, InventoryService inventories, HouseActionsService actions, com.tommustbe12.housing.scoreboard.HouseScoreboardService scoreboards, HouseGroupsService groups, TeamsService teams) {
        this.plugin = plugin;
        this.debug = debug;
        this.houses = houses;
        this.ownerTags = ownerTags;
        this.inventories = inventories;
        this.actions = actions;
        this.scoreboards = scoreboards;
        this.groups = groups;
        this.teams = teams;
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        var player = event.getPlayer();

        var fromInfo = houses.getHouseInfoByWorld(event.getFrom());
        var toInfo = houses.getHouseInfoByWorld(player.getWorld());

        // Leaving a house world -> schedule deactivate if empty
        if (fromInfo != null) {
            actions.runEvent(fromInfo.owner(), fromInfo.slot(), event.getFrom(), player, "player_quit");
            houses.scheduleDeactivateIfEmpty(event.getFrom());
        }

        // Entering a house world -> snapshot hub inv, apply house inv, apply tags
        if (toInfo != null) {
            // Enable/disable flight based on group permission (owner always allowed by groups.has()).
            boolean canFly = groups == null || groups.has(toInfo.owner(), toInfo.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.FLY);
            player.setAllowFlight(canFly);
            if (groups != null && groups.isBanned(toInfo.owner(), toInfo.slot(), player.getUniqueId())) {
                houses.sendToHub(player);
                player.sendMessage("§cYou are banned from that house.");
                return;
            }
            inventories.applyHouseInventoryOrDefault(player, toInfo.owner(), toInfo.slot());
            houses.applyOwnerState(player, toInfo.owner());
            if (groups != null) {
                boolean canSwitch = groups.has(toInfo.owner(), toInfo.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.SWITCH_GAMEMODE);
                boolean allowCreative = player.getUniqueId().equals(toInfo.owner())
                        || groups.has(toInfo.owner(), toInfo.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.BUILD);
                houses.applyHouseCommandPerms(player, canSwitch, allowCreative);
                groups.applyDefaultModeIfNeeded(player);
            }
            scoreboards.start(player, toInfo.owner(), toInfo.slot());
            // Apply all tags after scoreboard start so the joining player's viewer scoreboard is populated too.
            refreshWorldTags(player.getWorld(), toInfo.owner(), toInfo.slot());
            actions.runEvent(toInfo.owner(), toInfo.slot(), player.getWorld(), player, "player_join");
        } else {
            scoreboards.stop(player);
            ownerTags.clear(player);
        }
    }

    private void refreshWorldTags(World world, UUID owner, com.tommustbe12.housing.houses.HouseSlot slot) {
        if (world == null) return;
        for (var p : world.getPlayers()) {
            String groupTag = groups == null ? "" : groups.tagForDisplay(owner, slot, p.getUniqueId());
            String teamTag = teams == null ? "" : teams.tagForDisplay(owner, slot, p.getUniqueId());
            String combined = (teamTag == null ? "" : teamTag.trim()) + (groupTag == null ? "" : (" " + groupTag.trim()));
            combined = combined.trim();
            ownerTags.applyTag(p, combined);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        World world = event.getPlayer().getWorld();
        var info = houses.getHouseInfoByWorld(world);
        if (info != null) actions.runEvent(info.owner(), info.slot(), world, event.getPlayer(), "player_quit");
        // Delay one tick so the quitter is removed from the world's player list
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            houses.scheduleDeactivateIfEmpty(world);
        }, 1L);
    }
}
