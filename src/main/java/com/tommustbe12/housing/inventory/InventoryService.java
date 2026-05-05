package com.tommustbe12.housing.inventory;

import com.tommustbe12.housing.debug.Debug;
import com.tommustbe12.housing.houses.HouseSlot;
import com.tommustbe12.housing.listeners.HouseItemListener;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InventoryService {
    private final Debug debug;
    private final HouseInventoryStorage storage;
    private final HouseItemListener menuItemGiver;

    private final Map<UUID, PlayerInventorySnapshot> hubSnapshots = new ConcurrentHashMap<>();

    public InventoryService(Plugin plugin, Debug debug, HouseItemListener menuItemGiver) {
        this.debug = debug;
        this.storage = new HouseInventoryStorage(plugin);
        this.menuItemGiver = menuItemGiver;
    }

    public void snapshotHubInventory(Player player) {
        hubSnapshots.put(player.getUniqueId(), take(player.getInventory()));
        debug.toOps("Snapshotted hub inventory for " + player.getName());
    }

    public void restoreHubInventory(Player player) {
        PlayerInventorySnapshot snap = hubSnapshots.remove(player.getUniqueId());
        if (snap == null) return;
        apply(player.getInventory(), snap);
        debug.toOps("Restored hub inventory for " + player.getName());
    }

    public void applyHouseInventoryOrDefault(Player player, UUID houseOwner, HouseSlot slot) {
        PlayerInventorySnapshot snap = storage.load(houseOwner, slot, player.getUniqueId());
        if (snap != null) {
            apply(player.getInventory(), snap);
            menuItemGiver.giveMenuItem(player);
            return;
        }

        // Default house inventory for first-time join of that house:
        PlayerInventory inv = player.getInventory();
        inv.clear();
        inv.setArmorContents(null);
        inv.setItemInOffHand(null);
        menuItemGiver.giveMenuItem(player);
    }

    public void saveHouseInventory(Player player, UUID houseOwner, HouseSlot slot) {
        storage.save(houseOwner, slot, player.getUniqueId(), take(player.getInventory()));
        debug.toOps("Saved house inventory: player=" + player.getName() + " owner=" + houseOwner + " slot=" + slot.index());
    }

    private static PlayerInventorySnapshot take(PlayerInventory inv) {
        return new PlayerInventorySnapshot(
                inv.getContents().clone(),
                inv.getArmorContents().clone(),
                inv.getItemInOffHand()
        );
    }

    private static void apply(PlayerInventory inv, PlayerInventorySnapshot snap) {
        inv.setContents(snap.contents());
        inv.setArmorContents(snap.armor());
        inv.setItemInOffHand(snap.offhand());
    }
}

