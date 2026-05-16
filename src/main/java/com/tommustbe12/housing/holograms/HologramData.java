package com.tommustbe12.housing.holograms;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class HologramData {
    private final UUID id;
    private Location location;
    private final List<String> lines = new ArrayList<>();

    public HologramData(UUID id) {
        this.id = id;
    }

    public UUID id() { return id; }

    public Location location() { return location; }
    public void setLocation(Location location) { this.location = location; }

    public List<String> lines() { return lines; }
}

