package com.tommustbe12.housing.regions;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RegionSelectionService {
    public record Selection(Location pos1, Location pos2) {}

    private final Map<UUID, Selection> selections = new ConcurrentHashMap<>();

    public Selection get(Player player) {
        if (player == null) return null;
        return selections.get(player.getUniqueId());
    }

    public Location pos1(Player player) {
        Selection s = get(player);
        return s == null ? null : s.pos1();
    }

    public Location pos2(Player player) {
        Selection s = get(player);
        return s == null ? null : s.pos2();
    }

    public void setPos1(Player player, Location loc) {
        if (player == null) return;
        Selection cur = get(player);
        selections.put(player.getUniqueId(), new Selection(blockify(loc), cur == null ? null : cur.pos2()));
    }

    public void setPos2(Player player, Location loc) {
        if (player == null) return;
        Selection cur = get(player);
        selections.put(player.getUniqueId(), new Selection(cur == null ? null : cur.pos1(), blockify(loc)));
    }

    public boolean isComplete(Player player) {
        Selection s = get(player);
        return s != null && s.pos1() != null && s.pos2() != null && sameWorld(s.pos1(), s.pos2());
    }

    private static boolean sameWorld(Location a, Location b) {
        World aw = a.getWorld();
        World bw = b.getWorld();
        return aw != null && bw != null && aw.getUID().equals(bw.getUID());
    }

    private static Location blockify(Location loc) {
        if (loc == null || loc.getWorld() == null) return loc;
        return new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
}

