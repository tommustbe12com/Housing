package com.tommustbe12.housing.listeners;

import com.tommustbe12.housing.groups.HouseGroupsService;
import com.tommustbe12.housing.groups.HousePermission;
import com.tommustbe12.housing.houses.HouseManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class HousePermissionEnforcementListener implements Listener {
    private final HouseManager houses;
    private final HouseGroupsService groups;

    public HousePermissionEnforcementListener(HouseManager houses, HouseGroupsService groups) {
        this.houses = houses;
        this.groups = groups;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        var info = houses.getHouseInfoByWorld(event.getBlock().getWorld());
        if (info == null) return;
        Player player = event.getPlayer();
        if (!groups.has(info.owner(), info.slot(), player.getUniqueId(), HousePermission.BUILD)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        var info = houses.getHouseInfoByWorld(event.getBlock().getWorld());
        if (info == null) return;
        Player player = event.getPlayer();
        if (!groups.has(info.owner(), info.slot(), player.getUniqueId(), HousePermission.BUILD)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        var info = houses.getHouseInfoByWorld(event.getBlock().getWorld());
        if (info == null) return;
        Player player = event.getPlayer();
        if (!groups.has(info.owner(), info.slot(), player.getUniqueId(), HousePermission.FLUID)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        var info = houses.getHouseInfoByWorld(event.getBlock().getWorld());
        if (info == null) return;
        Player player = event.getPlayer();
        if (!groups.has(info.owner(), info.slot(), player.getUniqueId(), HousePermission.FLUID)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        var info = houses.getHouseInfoByWorld(block.getWorld());
        if (info == null) return;
        Player player = event.getPlayer();

        Material type = block.getType();
        HousePermission perm = permissionForBlock(type, block);
        if (perm == null) return;

        if (!groups.has(info.owner(), info.slot(), player.getUniqueId(), perm)) {
            event.setCancelled(true);
        }
    }

    private static HousePermission permissionForBlock(Material type, Block block) {
        String name = type.name();

        if (name.endsWith("_DOOR")) {
            if (name.startsWith("IRON_")) return HousePermission.IRON_DOOR;
            return HousePermission.WOOD_DOOR;
        }
        if (name.endsWith("_TRAPDOOR")) {
            if (name.startsWith("IRON_")) return HousePermission.IRON_TRAPDOOR;
            return HousePermission.WOOD_TRAPDOOR;
        }
        if (name.endsWith("_FENCE_GATE")) return HousePermission.FENCE_GATE;
        if (name.endsWith("_BUTTON")) return HousePermission.BUTTON;
        if (name.endsWith("_LEVER")) return HousePermission.LEVER;
        if (type == Material.CHEST || type == Material.TRAPPED_CHEST || type == Material.BARREL || type == Material.SHULKER_BOX) return HousePermission.USE_CHESTS;
        if (type == Material.ENDER_CHEST) return HousePermission.USE_ENDER_CHESTS;
        if (type == Material.JUKEBOX) return HousePermission.JUKEBOX;

        // For safety, only consider things that are known interactables; allow the rest.
        if (block.getBlockData() instanceof Openable) {
            // other openables (ex: copper door) treated like wood doors
            return HousePermission.WOOD_DOOR;
        }
        return null;
    }
}

