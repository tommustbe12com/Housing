package com.tommustbe12.housing.houses;

import org.bukkit.Location;

import java.util.UUID;

public final class HouseData {
    private final UUID owner;
    private final HouseSlot slot;

    private String name;
    private int maxPlayers;
    private long timeOfDay;
    private int cookies;
    private Location spawn;
    private String iconMaterial;

    public HouseData(UUID owner, HouseSlot slot) {
        this.owner = owner;
        this.slot = slot;
        this.name = "&aMy House";
        this.maxPlayers = 25;
        this.timeOfDay = 6000L;
        this.cookies = 0;
        this.spawn = null;
        this.iconMaterial = "GRASS_BLOCK";
    }

    public UUID owner() {
        return owner;
    }

    public HouseSlot slot() {
        return slot;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int maxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public long timeOfDay() {
        return timeOfDay;
    }

    public void setTimeOfDay(long timeOfDay) {
        this.timeOfDay = timeOfDay;
    }

    public int cookies() {
        return cookies;
    }

    public void addCookies(int amount) {
        this.cookies = Math.max(0, this.cookies + amount);
    }

    public Location spawn() {
        return spawn;
    }

    public void setSpawn(Location spawn) {
        this.spawn = spawn;
    }

    public String iconMaterial() {
        return iconMaterial;
    }

    public void setIconMaterial(String iconMaterial) {
        this.iconMaterial = iconMaterial;
    }
}
