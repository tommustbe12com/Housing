package com.tommustbe12.housing.regions;

import com.tommustbe12.housing.actions.ActionList;
import com.tommustbe12.housing.actions.placeholders.Placeholders;
import com.tommustbe12.housing.actions.placeholders.VariablesStore;
import com.tommustbe12.housing.actions.storage.SimpleActionCodec;
import com.tommustbe12.housing.debug.Debug;
import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.houses.HouseSlot;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class RegionsService {
    private final Plugin plugin;
    private final Debug debug;
    private final HouseManager houses;
    private final RegionsStore store;
    private final SimpleActionCodec codec;

    private final Map<String, Map<String, RegionData>> cache = new ConcurrentHashMap<>();

    public RegionsService(Plugin plugin, Debug debug, HouseManager houses) {
        this.plugin = plugin;
        this.debug = debug;
        this.houses = houses;
        this.store = new RegionsStore(plugin);
        VariablesStore variables = new VariablesStore(plugin);
        Placeholders placeholders = new Placeholders(variables);
        this.codec = new SimpleActionCodec(placeholders, variables, houses, (ctx, fn, global) -> {},
                new com.tommustbe12.housing.inventorylayouts.InventoryLayoutsService(plugin),
                new com.tommustbe12.housing.custommenus.CustomMenusService(plugin, houses),
                new com.tommustbe12.housing.teams.TeamsService(plugin));
    }

    public SimpleActionCodec codec() { return codec; }

    public Map<String, RegionData> regions(UUID owner, HouseSlot slot) {
        return cache.computeIfAbsent(key(owner, slot), k -> store.load(owner, slot, codec));
    }

    public void save(UUID owner, HouseSlot slot) {
        store.save(owner, slot, regions(owner, slot), codec);
    }

    public RegionData get(UUID owner, HouseSlot slot, String nameKey) {
        if (nameKey == null) return null;
        return regions(owner, slot).get(nameKey.toLowerCase());
    }

    public boolean add(UUID owner, HouseSlot slot, RegionData region) {
        if (region == null || region.name() == null || region.name().isBlank()) return false;
        String key = region.name().trim().toLowerCase();
        if (regions(owner, slot).containsKey(key)) return false;
        regions(owner, slot).put(key, region);
        save(owner, slot);
        return true;
    }

    public boolean rename(UUID owner, HouseSlot slot, String oldKey, String newName) {
        if (oldKey == null || newName == null || newName.isBlank()) return false;
        Map<String, RegionData> map = regions(owner, slot);
        RegionData r = map.remove(oldKey.toLowerCase());
        if (r == null) return false;
        String nk = newName.trim().toLowerCase();
        if (map.containsKey(nk)) {
            map.put(oldKey.toLowerCase(), r);
            return false;
        }
        r.setName(newName.trim());
        map.put(nk, r);
        save(owner, slot);
        return true;
    }

    public boolean delete(UUID owner, HouseSlot slot, String key) {
        if (key == null) return false;
        RegionData removed = regions(owner, slot).remove(key.toLowerCase());
        if (removed == null) return false;
        save(owner, slot);
        return true;
    }

    public void moveToSelection(UUID owner, HouseSlot slot, String key, Location sel1, Location sel2) {
        RegionData r = get(owner, slot, key);
        if (r == null) return;
        r.setPos1(sel1);
        r.setPos2(sel2);
        save(owner, slot);
    }

    public RegionData regionAt(UUID owner, HouseSlot slot, Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        for (RegionData r : regions(owner, slot).values()) {
            if (contains(r, loc)) return r;
        }
        return null;
    }

    public boolean contains(RegionData r, Location loc) {
        if (r == null || loc == null) return false;
        World w1 = r.pos1().getWorld();
        World w2 = r.pos2().getWorld();
        if (w1 == null || w2 == null || loc.getWorld() == null) return false;
        if (!w1.getUID().equals(loc.getWorld().getUID())) return false;
        if (!w2.getUID().equals(loc.getWorld().getUID())) return false;

        int minX = Math.min(r.pos1().getBlockX(), r.pos2().getBlockX());
        int minY = Math.min(r.pos1().getBlockY(), r.pos2().getBlockY());
        int minZ = Math.min(r.pos1().getBlockZ(), r.pos2().getBlockZ());
        int maxX = Math.max(r.pos1().getBlockX(), r.pos2().getBlockX());
        int maxY = Math.max(r.pos1().getBlockY(), r.pos2().getBlockY());
        int maxZ = Math.max(r.pos1().getBlockZ(), r.pos2().getBlockZ());

        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public Location randomPointInside(RegionData r) {
        if (r == null || r.pos1() == null || r.pos2() == null) return null;
        World w = r.pos1().getWorld();
        if (w == null) return null;
        int minX = Math.min(r.pos1().getBlockX(), r.pos2().getBlockX());
        int minY = Math.min(r.pos1().getBlockY(), r.pos2().getBlockY());
        int minZ = Math.min(r.pos1().getBlockZ(), r.pos2().getBlockZ());
        int maxX = Math.max(r.pos1().getBlockX(), r.pos2().getBlockX());
        int maxY = Math.max(r.pos1().getBlockY(), r.pos2().getBlockY());
        int maxZ = Math.max(r.pos1().getBlockZ(), r.pos2().getBlockZ());
        int x = minX + (int) Math.floor(Math.random() * (maxX - minX + 1));
        int y = minY + (int) Math.floor(Math.random() * (maxY - minY + 1));
        int z = minZ + (int) Math.floor(Math.random() * (maxZ - minZ + 1));
        return new Location(w, x + 0.5, y + 0.1, z + 0.5);
    }

    public void setEntryActions(UUID owner, HouseSlot slot, String key, ActionList list) {
        RegionData r = get(owner, slot, key);
        if (r == null) return;
        r.setEntryActions(list);
        save(owner, slot);
    }

    public void setExitActions(UUID owner, HouseSlot slot, String key, ActionList list) {
        RegionData r = get(owner, slot, key);
        if (r == null) return;
        r.setExitActions(list);
        save(owner, slot);
    }

    private static String key(UUID owner, HouseSlot slot) {
        return owner + ":" + slot.index();
    }
}
