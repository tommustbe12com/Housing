package com.tommustbe12.housing.listeners;

import com.tommustbe12.housing.actions.HouseActionsService;
import com.tommustbe12.housing.houses.HouseManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.Material;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HouseEventActionsListener implements Listener {
    private final HouseManager houses;
    private final HouseActionsService actions;
    private final Map<UUID, Material> lastPortal = new ConcurrentHashMap<>();

    public HouseEventActionsListener(HouseManager houses, HouseActionsService actions) {
        this.houses = houses;
        this.actions = actions;
    }

    private void run(Player player, String eventKey) {
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null) return;
        actions.runEvent(info.owner(), info.slot(), player.getWorld(), player, eventKey);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        run(event.getPlayer(), "player_quit");
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        run(event.getEntity(), "player_death");
        if (event.getEntity().getKiller() != null) {
            run(event.getEntity().getKiller(), "player_kill");
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        run(player, "player_damage");
    }

    @EventHandler
    public void onPvpDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        // Treat as pvp-state-change placeholder trigger for now
        run(victim, "pvp_state_change");
        run(damager, "pvp_state_change");
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH || event.getState() == PlayerFishEvent.State.CAUGHT_ENTITY) {
            run(event.getPlayer(), "fish_caught");
        }
    }

    @EventHandler
    public void onPortalTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL
                || event.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL
                || event.getCause() == PlayerTeleportEvent.TeleportCause.END_GATEWAY) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPortal(PlayerPortalEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        Material type = event.getTo().getBlock().getType();
        if (type != Material.NETHER_PORTAL && type != Material.END_PORTAL && type != Material.END_GATEWAY) {
            lastPortal.remove(event.getPlayer().getUniqueId());
            return;
        }
        Material last = lastPortal.put(event.getPlayer().getUniqueId(), type);
        if (last == type) return; // still inside same portal
        run(event.getPlayer(), "enter_portal");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        run(event.getPlayer(), "block_break");
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        run(event.getPlayer(), "drop_item");
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        run(player, "pickup_item");
    }

    @EventHandler
    public void onHeld(PlayerItemHeldEvent event) {
        run(event.getPlayer(), "held_item_change");
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        run(event.getPlayer(), "toggle_sneak");
    }

    @EventHandler
    public void onFlight(PlayerToggleFlightEvent event) {
        run(event.getPlayer(), "toggle_flight");
    }
}
