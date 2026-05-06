package com.tommustbe12.housing.inventorylayouts;

import com.tommustbe12.housing.houses.HouseSlot;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InventoryLayoutsService {
    private final Plugin plugin;
    private final InventoryLayoutsStorage storage;
    private final ConcurrentHashMap<String, List<InventoryLayout>> cache = new ConcurrentHashMap<>();

    public InventoryLayoutsService(Plugin plugin) {
        this.plugin = plugin;
        this.storage = new InventoryLayoutsStorage(plugin);
    }

    public List<InventoryLayout> get(UUID owner, HouseSlot slot) {
        return cache.computeIfAbsent(key(owner, slot), k -> storage.load(owner, slot));
    }

    public void save(UUID owner, HouseSlot slot) {
        storage.save(owner, slot, get(owner, slot));
    }

    public InventoryLayout find(UUID owner, HouseSlot slot, UUID id) {
        for (InventoryLayout l : get(owner, slot)) if (l.id().equals(id)) return l;
        return null;
    }

    public void apply(Player player, InventoryLayout layout) {
        if (player == null || layout == null) return;
        ItemStack[] contents = layout.contents();
        if (contents == null) contents = new ItemStack[36];
        player.getInventory().setContents(contents);
        player.getInventory().setHelmet(layout.helmet());
        player.getInventory().setChestplate(layout.chestplate());
        player.getInventory().setLeggings(layout.leggings());
        player.getInventory().setBoots(layout.boots());
        player.getInventory().setItemInOffHand(layout.offhand());
        // never allow losing the Housing menu item
        com.tommustbe12.housing.util.HousingItems.ensureMenuStar(plugin, player);
        player.updateInventory();
    }

    private static String key(UUID owner, HouseSlot slot) {
        return owner + ":" + slot.index();
    }
}
