package com.tommustbe12.housing.holograms;

import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.houses.HouseSlot;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HologramsService {
    private final Plugin plugin;
    private final HologramsStorage storage;
    private final ConcurrentHashMap<String, List<HologramData>> cache = new ConcurrentHashMap<>();

    public HologramsService(Plugin plugin, HouseManager houses) {
        this.plugin = plugin;
        this.storage = new HologramsStorage(plugin);
    }

    public List<HologramData> get(UUID owner, HouseSlot slot, World world) {
        return cache.computeIfAbsent(key(owner, slot), k -> storage.load(owner, slot, world));
    }

    public void save(UUID owner, HouseSlot slot, World world) {
        storage.save(owner, slot, get(owner, slot, world));
    }

    public HologramData find(UUID owner, HouseSlot slot, World world, UUID id) {
        for (HologramData h : get(owner, slot, world)) if (h.id().equals(id)) return h;
        return null;
    }

    private static String key(UUID owner, HouseSlot slot) {
        return owner + ":" + slot.index();
    }
}

