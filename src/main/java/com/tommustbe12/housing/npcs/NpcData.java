package com.tommustbe12.housing.npcs;

import com.tommustbe12.housing.actions.ActionList;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class NpcData {
    private final UUID id;
    private int citizensNpcId = -1;
    private String name;
    private String skinUsername;
    private Location location;
    private NpcBehavior behavior;

    private ItemStack helmet, chestplate, leggings, boots, hand, offhand;
    private final ActionList clickActions = new ActionList();

    public NpcData(UUID id) {
        this.id = id;
        this.name = "Bob";
        this.skinUsername = "Steve";
        this.behavior = NpcBehavior.STATIC;
    }

    public UUID id() { return id; }

    public int citizensNpcId() { return citizensNpcId; }
    public void setCitizensNpcId(int citizensNpcId) { this.citizensNpcId = citizensNpcId; }

    public String name() { return name; }
    public void setName(String name) { this.name = name; }
    public String skinUsername() { return skinUsername; }
    public void setSkinUsername(String skinUsername) { this.skinUsername = skinUsername; }
    public Location location() { return location; }
    public void setLocation(Location location) { this.location = location; }
    public NpcBehavior behavior() { return behavior; }
    public void setBehavior(NpcBehavior behavior) { this.behavior = behavior; }

    public ItemStack helmet() { return helmet; }
    public void setHelmet(ItemStack helmet) { this.helmet = helmet; }
    public ItemStack chestplate() { return chestplate; }
    public void setChestplate(ItemStack chestplate) { this.chestplate = chestplate; }
    public ItemStack leggings() { return leggings; }
    public void setLeggings(ItemStack leggings) { this.leggings = leggings; }
    public ItemStack boots() { return boots; }
    public void setBoots(ItemStack boots) { this.boots = boots; }
    public ItemStack hand() { return hand; }
    public void setHand(ItemStack hand) { this.hand = hand; }
    public ItemStack offhand() { return offhand; }
    public void setOffhand(ItemStack offhand) { this.offhand = offhand; }

    public ActionList clickActions() { return clickActions; }
}
