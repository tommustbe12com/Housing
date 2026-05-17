package com.tommustbe12.housing.holograms;

import com.tommustbe12.housing.actions.ActionContext;
import com.tommustbe12.housing.actions.placeholders.Placeholders;
import com.tommustbe12.housing.actions.placeholders.VariablesStore;
import com.tommustbe12.housing.debug.Debug;
import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.houses.HouseSlot;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class HologramsRuntime {
    private final Plugin plugin;
    private final Debug debug;
    private final HouseManager houses;
    private final HologramsService holograms;
    private final NamespacedKey holoIdKey;
    private final NamespacedKey holoLineKey;
    private final Map<UUID, List<UUID>> spawnedEntityIdsByHologram = new ConcurrentHashMap<>();
    private final Placeholders placeholders;

    public HologramsRuntime(Plugin plugin, Debug debug, HouseManager houses, HologramsService holograms) {
        this.plugin = plugin;
        this.debug = debug;
        this.houses = houses;
        this.holograms = holograms;
        this.holoIdKey = new NamespacedKey(plugin, "holo_id");
        this.holoLineKey = new NamespacedKey(plugin, "holo_line");
        VariablesStore vars = new VariablesStore(plugin);
        this.placeholders = new Placeholders(vars);
    }

    public NamespacedKey holoIdKey() { return holoIdKey; }
    public NamespacedKey holoLineKey() { return holoLineKey; }

    public void spawnAll(UUID owner, HouseSlot slot, World world) {
        for (HologramData h : holograms.get(owner, slot, world)) spawnOrUpdate(owner, slot, world, h);
    }

    public void despawnAll(World world) {
        if (world == null) return;
        // If the server restarts/crashes, armor stands are persisted in the world but our runtime tracking map
        // is empty. Always do a best-effort cleanup pass by scanning for hologram-tagged entities.
        for (ArmorStand stand : world.getEntitiesByClass(ArmorStand.class)) {
            try {
                var pdc = stand.getPersistentDataContainer();
                if (pdc.has(holoIdKey, PersistentDataType.STRING)) stand.remove();
            } catch (Exception ignored) {}
        }
        for (List<UUID> ids : new ArrayList<>(spawnedEntityIdsByHologram.values())) {
            for (UUID eid : ids) {
                Entity e = world.getEntity(eid);
                if (e != null) e.remove();
            }
        }
        spawnedEntityIdsByHologram.clear();
    }

    public void despawn(World world, UUID hologramId) {
        if (world == null || hologramId == null) return;
        List<UUID> ids = spawnedEntityIdsByHologram.remove(hologramId);
        if (ids != null) {
            for (UUID eid : ids) {
                Entity e = world.getEntity(eid);
                if (e != null) e.remove();
            }
        }
        // Best-effort removal for persisted holograms after restart (when we don't have entity ids tracked).
        String target = hologramId.toString();
        for (ArmorStand stand : world.getEntitiesByClass(ArmorStand.class)) {
            try {
                var pdc = stand.getPersistentDataContainer();
                String id = pdc.get(holoIdKey, PersistentDataType.STRING);
                if (target.equals(id)) stand.remove();
            } catch (Exception ignored) {}
        }
    }

    public void spawnOrUpdate(UUID owner, HouseSlot slot, World world, HologramData h) {
        if (world == null || h == null || h.location() == null) return;

        // remove old entities
        List<UUID> old = spawnedEntityIdsByHologram.remove(h.id());
        if (old != null) {
            for (UUID eid : old) {
                Entity e = world.getEntity(eid);
                if (e != null) e.remove();
            }
        }

        Location base = h.location().clone();
        base.setWorld(world);
        base = ensureAboveGround(base);

        List<String> rawLines = h.lines().isEmpty() ? List.of("§fHologram") : h.lines();
        if (rawLines.size() > 10) rawLines = rawLines.subList(0, 10);

        List<UUID> spawned = new ArrayList<>();
        double dy = 0.25;
        for (int i = 0; i < rawLines.size(); i++) {
            Location loc = base.clone().add(0, (rawLines.size() - 1 - i) * dy, 0);
            ArmorStand stand = (ArmorStand) world.spawnEntity(loc, EntityType.ARMOR_STAND);
            stand.setVisible(false);
            stand.setMarker(true);
            stand.setSmall(true);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setPersistent(true);
            stand.setCustomNameVisible(true);

            String line = ChatColor.translateAlternateColorCodes('&', rawLines.get(i));
            // Resolve non-player-specific placeholders.
            ActionContext ctx = new ActionContext(plugin, debug, owner, slot, world, null, null, loc);
            stand.setCustomName(placeholders.resolve(ctx, line));

            var pdc = stand.getPersistentDataContainer();
            pdc.set(holoIdKey, PersistentDataType.STRING, h.id().toString());
            pdc.set(holoLineKey, PersistentDataType.INTEGER, i);
            spawned.add(stand.getUniqueId());
        }

        spawnedEntityIdsByHologram.put(h.id(), spawned);
    }

    public Location ensureAboveGround(Location loc) {
        if (loc == null || loc.getWorld() == null) return loc;
        Location out = loc.clone();
        for (int i = 0; i < 6; i++) {
            if (out.getBlock().getType().isAir() && out.clone().add(0, 1, 0).getBlock().getType().isAir()) return out;
            out.add(0, 1, 0);
        }
        return out;
    }
}
