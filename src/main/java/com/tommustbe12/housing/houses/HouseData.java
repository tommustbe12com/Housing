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
    private double spawnX;
    private double spawnY;
    private double spawnZ;
    private float spawnYaw;
    private float spawnPitch;
    private boolean hasSpawn;
    private String iconMaterial;

    public HouseData(UUID owner, HouseSlot slot) {
        this.owner = owner;
        this.slot = slot;
        this.name = "&aMy House";
        this.maxPlayers = 25;
        this.timeOfDay = 6000L;
        this.cookies = 0;
        this.hasSpawn = false;
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

    public boolean hasSpawn() {
        return hasSpawn;
    }

    public void setSpawn(Location spawn) {
        if (spawn == null) {
            this.hasSpawn = false;
            return;
        }
        this.spawnX = spawn.getX();
        this.spawnY = spawn.getY();
        this.spawnZ = spawn.getZ();
        this.spawnYaw = spawn.getYaw();
        this.spawnPitch = spawn.getPitch();
        this.hasSpawn = true;
    }

    public Location spawnInWorld(org.bukkit.World world) {
        if (!hasSpawn || world == null) return null;
        return new Location(world, spawnX, spawnY, spawnZ, spawnYaw, spawnPitch);
    }

    public double spawnX() { return spawnX; }
    public double spawnY() { return spawnY; }
    public double spawnZ() { return spawnZ; }
    public float spawnYaw() { return spawnYaw; }
    public float spawnPitch() { return spawnPitch; }

    public String iconMaterial() {
        return iconMaterial;
    }

    public void setIconMaterial(String iconMaterial) {
        this.iconMaterial = iconMaterial;
    }
}
