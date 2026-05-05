package com.tommustbe12.housing.inventory;

import org.bukkit.inventory.ItemStack;

public record PlayerInventorySnapshot(
        ItemStack[] contents,
        ItemStack[] armor,
        ItemStack offhand
) {
}

