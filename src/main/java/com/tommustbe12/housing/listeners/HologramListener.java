package com.tommustbe12.housing.listeners;

import com.tommustbe12.housing.gui.HologramsGui;
import com.tommustbe12.housing.holograms.HologramData;
import com.tommustbe12.housing.holograms.HologramsRuntime;
import com.tommustbe12.housing.holograms.HologramsService;
import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.util.HousingItems;
import org.bukkit.Location;
import org.bukkit.util.RayTraceResult;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public final class HologramListener implements Listener {
    private final Plugin plugin;
    private final HouseManager houses;
    private final HologramsService holograms;
    private final HologramsRuntime runtime;
    private final HologramsGui gui;

    private static final double EDIT_RAY_DISTANCE = 6.0;
    private static final double EDIT_RAY_RADIUS = 1.5;

    public HologramListener(Plugin plugin, HouseManager houses, HologramsService holograms, HologramsRuntime runtime, HologramsGui gui) {
        this.plugin = plugin;
        this.houses = houses;
        this.holograms = holograms;
        this.runtime = runtime;
        this.gui = gui;
    }

    @EventHandler
    public void onPlace(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getAction().isRightClick()) return;
        Player player = event.getPlayer();

        ItemStack hand = player.getInventory().getItemInMainHand();
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null) return;

        // Easier editing: sneak-right-click anywhere to ray-pick a nearby hologram.
        if (player.isSneaking()) {
            UUID picked = rayPickHologram(player);
            if (picked != null) {
                event.setCancelled(true);
                gui.open(player, picked);
                return;
            }
        }

        if (!HousingItems.isHologramPlacerItem(plugin, hand)) return;
        if (event.getClickedBlock() == null) return;

        event.setCancelled(true);
        // Slightly higher so the nameplate is more visible.
        Location base = event.getClickedBlock().getLocation().add(0.5, 1.8, 0.5);
        base.setYaw(player.getLocation().getYaw());
        base.setPitch(0f);
        base = runtime.ensureAboveGround(base);

        HologramData h = new HologramData(UUID.randomUUID());
        h.setLocation(base);
        h.lines().add("§fNew hologram");
        holograms.get(info.owner(), info.slot(), player.getWorld()).add(h);
        holograms.save(info.owner(), info.slot(), player.getWorld());
        runtime.spawnOrUpdate(info.owner(), info.slot(), player.getWorld(), h);
        player.sendMessage("§aHologram placed. Shift-right-click it to edit.");
    }

    @EventHandler
    public void onClickEntity(PlayerInteractAtEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null) return;

        Entity e = event.getRightClicked();
        if (e == null) return;

        UUID id = readHologramId(e);
        if (id == null) {
            // If they clicked "near" the tiny marker stand, search in a small radius (and slightly above).
            Location at = e.getLocation().add(0, 1.0, 0);
            for (Entity near : e.getWorld().getNearbyEntities(at, 1.5, 2.0, 1.5)) {
                id = readHologramId(near);
                if (id != null) break;
            }
        }
        if (id == null) return;

        event.setCancelled(true);
        if (!player.isSneaking()) return;
        gui.open(player, id);
    }

    private UUID rayPickHologram(Player player) {
        RayTraceResult hit = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                EDIT_RAY_DISTANCE,
                EDIT_RAY_RADIUS,
                e -> readHologramId(e) != null
        );
        if (hit == null) return null;
        Entity hitEntity = hit.getHitEntity();
        if (hitEntity == null) return null;
        UUID primary = readHologramId(hitEntity);
        if (primary == null) return null;

        // If multiple hologram stands are clustered, pick the closest one to the ray hit position.
        Location hp = hit.getHitPosition() == null ? hitEntity.getLocation() : hit.getHitPosition().toLocation(player.getWorld());
        UUID best = primary;
        double bestDist = hp.distanceSquared(hitEntity.getLocation());
        for (Entity near : hitEntity.getWorld().getNearbyEntities(hp, 1.75, 2.5, 1.75)) {
            UUID id = readHologramId(near);
            if (id == null) continue;
            double d = hp.distanceSquared(near.getLocation());
            if (d < bestDist) {
                bestDist = d;
                best = id;
            }
        }
        return best;
    }

    private UUID readHologramId(Entity e) {
        if (e == null) return null;
        String idStr = e.getPersistentDataContainer().get(runtime.holoIdKey(), PersistentDataType.STRING);
        if (idStr == null) return null;
        try { return UUID.fromString(idStr); } catch (Exception ex) { return null; }
    }
}
