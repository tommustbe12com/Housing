package com.tommustbe12.housing.custommenus.gui;

import com.tommustbe12.housing.houses.HouseSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public final class CustomMenuRuntimeHolder implements InventoryHolder {
    private final UUID owner;
    private final HouseSlot slot;
    private final UUID menuId;

    public CustomMenuRuntimeHolder(UUID owner, HouseSlot slot, UUID menuId) {
        this.owner = owner;
        this.slot = slot;
        this.menuId = menuId;
    }

    public UUID owner() { return owner; }
    public HouseSlot slot() { return slot; }
    public UUID menuId() { return menuId; }

    @Override
    public Inventory getInventory() {
        return null;
    }
}

