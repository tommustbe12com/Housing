package com.tommustbe12.housing.regions;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RegionSelectionVisualizer {
    private final Plugin plugin;
    private final RegionSelectionService selections;
    private final Map<UUID, BukkitTask> tasks = new ConcurrentHashMap<>();

    public RegionSelectionVisualizer(Plugin plugin, RegionSelectionService selections) {
        this.plugin = plugin;
        this.selections = selections;
    }

    public void refresh(Player player) {
        if (player == null) return;
        stop(player.getUniqueId());
        if (!selections.isComplete(player)) return;

        UUID id = player.getUniqueId();
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) { stop(id); return; }
            RegionSelectionService.Selection sel = selections.get(player);
            if (sel == null || sel.pos1() == null || sel.pos2() == null) { stop(id); return; }
            if (sel.pos1().getWorld() == null || sel.pos2().getWorld() == null) { stop(id); return; }
            if (!sel.pos1().getWorld().getUID().equals(sel.pos2().getWorld().getUID())) { stop(id); return; }
            showOutline(player, sel.pos1(), sel.pos2());
        }, 0L, 10L);
        tasks.put(id, task);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> stop(id), 20L * 15L);
    }

    private void stop(UUID id) {
        BukkitTask t = tasks.remove(id);
        if (t != null) t.cancel();
    }

    private static void showOutline(Player player, Location a, Location b) {
        World w = a.getWorld();
        if (w == null) return;
        int minX = Math.min(a.getBlockX(), b.getBlockX());
        int minY = Math.min(a.getBlockY(), b.getBlockY());
        int minZ = Math.min(a.getBlockZ(), b.getBlockZ());
        int maxX = Math.max(a.getBlockX(), b.getBlockX());
        int maxY = Math.max(a.getBlockY(), b.getBlockY());
        int maxZ = Math.max(a.getBlockZ(), b.getBlockZ());

        Particle p = Particle.END_ROD;
        int step = 1;

        for (int x = minX; x <= maxX; x += step) {
            spawn(player, p, w, x, minY, minZ);
            spawn(player, p, w, x, minY, maxZ);
            spawn(player, p, w, x, maxY, minZ);
            spawn(player, p, w, x, maxY, maxZ);
        }
        for (int y = minY; y <= maxY; y += step) {
            spawn(player, p, w, minX, y, minZ);
            spawn(player, p, w, minX, y, maxZ);
            spawn(player, p, w, maxX, y, minZ);
            spawn(player, p, w, maxX, y, maxZ);
        }
        for (int z = minZ; z <= maxZ; z += step) {
            spawn(player, p, w, minX, minY, z);
            spawn(player, p, w, maxX, minY, z);
            spawn(player, p, w, minX, maxY, z);
            spawn(player, p, w, maxX, maxY, z);
        }
    }

    private static void spawn(Player player, Particle p, World w, int x, int y, int z) {
        player.spawnParticle(p, new Location(w, x + 0.5, y + 0.5, z + 0.5), 1, 0, 0, 0, 0);
    }
}

