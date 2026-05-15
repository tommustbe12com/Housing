package com.tommustbe12.housing.custommenus;

import com.tommustbe12.housing.actions.placeholders.Placeholders;
import com.tommustbe12.housing.actions.placeholders.VariablesStore;
import com.tommustbe12.housing.actions.storage.SimpleActionCodec;
import com.tommustbe12.housing.actions.storage.SimpleActionSerializer;
import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.houses.HouseSlot;
import com.tommustbe12.housing.inventorylayouts.InventoryLayoutsService;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CustomMenusService {
    private final Plugin plugin;
    private final CustomMenusStorage storage;
    private final SimpleActionSerializer serializer = new SimpleActionSerializer();
    private final ConcurrentHashMap<String, List<CustomMenu>> cache = new ConcurrentHashMap<>();

    private final SimpleActionCodec codec;

    public CustomMenusService(Plugin plugin, HouseManager houses) {
        this.plugin = plugin;
        this.storage = new CustomMenusStorage(plugin);
        VariablesStore vars = new VariablesStore(plugin);
        Placeholders ph = new Placeholders(vars);
        this.codec = new SimpleActionCodec(ph, vars, houses, (ctx, fn, global) -> {},
                new InventoryLayoutsService(plugin),
                this,
                new com.tommustbe12.housing.teams.TeamsService(plugin),
                new com.tommustbe12.housing.groups.HouseGroupsService(plugin, houses));
    }

    public List<CustomMenu> get(UUID owner, HouseSlot slot) {
        return cache.computeIfAbsent(key(owner, slot), k -> storage.load(owner, slot, codec));
    }

    public void save(UUID owner, HouseSlot slot) {
        storage.save(owner, slot, get(owner, slot), serializer);
    }

    public CustomMenu find(UUID owner, HouseSlot slot, UUID id) {
        for (CustomMenu m : get(owner, slot)) if (m.id().equals(id)) return m;
        return null;
    }

    private static String key(UUID owner, HouseSlot slot) {
        return owner + ":" + slot.index();
    }
}
