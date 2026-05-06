package com.tommustbe12.housing.inventorylayouts;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class InventoryLayout {
    private final UUID id;
    private String name;

    private ItemStack[] contents; // 36
    private ItemStack helmet;
    private ItemStack chestplate;
    private ItemStack leggings;
    private ItemStack boots;
    private ItemStack offhand;

    public InventoryLayout(UUID id, String name) {
        this.id = id;
        this.name = name;
        this.contents = new ItemStack[36];
    }

    public UUID id() { return id; }
    public String name() { return name; }
    public void setName(String name) { this.name = name; }

    public ItemStack[] contents() { return contents; }
    public void setContents(ItemStack[] contents) { this.contents = contents; }

    public ItemStack helmet() { return helmet; }
    public void setHelmet(ItemStack helmet) { this.helmet = helmet; }
    public ItemStack chestplate() { return chestplate; }
    public void setChestplate(ItemStack chestplate) { this.chestplate = chestplate; }
    public ItemStack leggings() { return leggings; }
    public void setLeggings(ItemStack leggings) { this.leggings = leggings; }
    public ItemStack boots() { return boots; }
    public void setBoots(ItemStack boots) { this.boots = boots; }
    public ItemStack offhand() { return offhand; }
    public void setOffhand(ItemStack offhand) { this.offhand = offhand; }
}

