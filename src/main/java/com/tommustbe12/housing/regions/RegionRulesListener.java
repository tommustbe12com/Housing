package com.tommustbe12.housing.regions;

import com.tommustbe12.housing.houses.HouseManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RegionRulesListener implements Listener {
    private final Plugin plugin;
    private final HouseManager houses;
    private final RegionsService regions;

    private final Map<UUID, Long> lastDoubleJumpMs = new ConcurrentHashMap<>();

    public RegionRulesListener(Plugin plugin, HouseManager houses, RegionsService regions) {
        this.plugin = plugin;
        this.houses = houses;
        this.regions = regions;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null) return;
        RegionData r = regions.regionAt(info.owner(), info.slot(), player.getLocation());
        if (r == null) return;
        RegionSettings st = r.settings();

        if (event instanceof EntityDamageByEntityEvent by) {
            if (by.getDamager() instanceof Player) {
                if (!st.pvpDamage) event.setCancelled(true);
                return;
            }
        }

        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.FIRE || cause == EntityDamageEvent.DamageCause.FIRE_TICK || cause == EntityDamageEvent.DamageCause.LAVA || cause == EntityDamageEvent.DamageCause.HOT_FLOOR) {
            if (!st.fireDamage) event.setCancelled(true);
            return;
        }
        if (cause == EntityDamageEvent.DamageCause.FALL) {
            if (!st.fallDamage) {
                if (player.getAllowFlight() || recentDoubleJump(player)) {
                    event.setCancelled(true);
                    return;
                }
                event.setCancelled(true);
            }
            return;
        }
        if (cause == EntityDamageEvent.DamageCause.POISON || cause == EntityDamageEvent.DamageCause.WITHER || cause == EntityDamageEvent.DamageCause.CONTACT) {
            if (!st.poisonWitherRoseDamage) event.setCancelled(true);
            return;
        }
        if (cause == EntityDamageEvent.DamageCause.SUFFOCATION) {
            if (!st.suffocation) event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null) return;
        RegionData r = regions.regionAt(info.owner(), info.slot(), player.getLocation());
        if (r == null) return;
        if (!r.settings().doubleJump) return;

        event.setCancelled(true);
        player.setFlying(false);
        player.setAllowFlight(false);

        Vector v = player.getLocation().getDirection().multiply(0.9).setY(0.9);
        player.setVelocity(v);
        lastDoubleJumpMs.put(player.getUniqueId(), System.currentTimeMillis());

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            // Re-allow toggling flight to trigger another double jump.
            if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;
            player.setAllowFlight(true);
        }, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFood(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null) return;
        RegionData r = regions.regionAt(info.owner(), info.slot(), player.getLocation());
        if (r == null) return;
        if (!r.settings().hunger) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRegen(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null) return;
        RegionData r = regions.regionAt(info.owner(), info.slot(), player.getLocation());
        if (r == null) return;
        if (!r.settings().naturalRegen && event.getRegainReason() == EntityRegainHealthEvent.RegainReason.REGEN) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null) return;
        RegionData r = regions.regionAt(info.owner(), info.slot(), player.getLocation());
        if (r == null) return;
        RegionSettings st = r.settings();
        if (!st.killDeathMessages) event.setDeathMessage(null);
        if (st.keepInventory) {
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null) return;
        RegionData r = regions.regionAt(info.owner(), info.slot(), event.getRespawnLocation());
        if (r == null) return;
        if (r.settings().instantRespawn) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    player.spigot().respawn();
                } catch (Throwable ignored) {
                }
            }, 1L);
        }
    }

    private boolean recentDoubleJump(Player player) {
        Long ms = lastDoubleJumpMs.get(player.getUniqueId());
        return ms != null && (System.currentTimeMillis() - ms) <= 2500L;
    }
}

