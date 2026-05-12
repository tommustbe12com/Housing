package com.tommustbe12.housing.regions;

import com.tommustbe12.housing.actions.*;
import com.tommustbe12.housing.debug.Debug;
import com.tommustbe12.housing.houses.HouseManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RegionRuntimeListener implements Listener {
    private final Plugin plugin;
    private final Debug debug;
    private final HouseManager houses;
    private final RegionsService regions;
    private final ActionsEngine engine;

    private final Map<UUID, String> currentRegionKey = new ConcurrentHashMap<>();

    public RegionRuntimeListener(Plugin plugin, Debug debug, HouseManager houses, RegionsService regions) {
        this.plugin = plugin;
        this.debug = debug;
        this.houses = houses;
        this.regions = regions;
        this.engine = new ActionsEngine(plugin, debug);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        currentRegionKey.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onQuit(PlayerQuitEvent event) {
        currentRegionKey.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) return;

        Player player = event.getPlayer();
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null) return;

        RegionData now = regions.regionAt(info.owner(), info.slot(), to);
        String nowKey = now == null ? null : now.name().toLowerCase();
        String prevKey = currentRegionKey.get(player.getUniqueId());
        if (eq(prevKey, nowKey)) return;

        RegionData prev = prevKey == null ? null : regions.get(info.owner(), info.slot(), prevKey);
        if (nowKey == null) currentRegionKey.remove(player.getUniqueId());
        else currentRegionKey.put(player.getUniqueId(), nowKey);

        if (prev != null && prev.exitActions() != null && !prev.exitActions().actions().isEmpty()) {
            run(player, info.owner(), info.slot(), prev.exitActions());
        }
        if (now != null && now.entryActions() != null && !now.entryActions().actions().isEmpty()) {
            run(player, info.owner(), info.slot(), now.entryActions());
        }

        // Allow triggering double jump via flight toggle while inside the region (non-creative).
        if (now != null && now.settings().doubleJump && player.getGameMode() != org.bukkit.GameMode.CREATIVE && player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
            if (!player.getAllowFlight()) player.setAllowFlight(true);
        }
    }

    private void run(Player player, UUID owner, com.tommustbe12.housing.houses.HouseSlot slot, ActionList list) {
        ActionContext ctx = new ActionContext(plugin, debug, owner, slot, player.getWorld(), player, null, player.getLocation());
        engine.run(list, ctx);
    }

    private static boolean eq(String a, String b) {
        return a == null ? b == null : a.equalsIgnoreCase(b);
    }
}
