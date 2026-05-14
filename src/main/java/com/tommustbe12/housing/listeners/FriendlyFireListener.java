package com.tommustbe12.housing.listeners;

import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.teams.TeamsService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class FriendlyFireListener implements Listener {
    private final HouseManager houses;
    private final TeamsService teams;

    public FriendlyFireListener(HouseManager houses, TeamsService teams) {
        this.houses = houses;
        this.teams = teams;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;
        var info = houses.getHouseInfoByWorld(victim.getWorld());
        if (info == null) return;
        if (attacker.getWorld() != victim.getWorld()) return;
        boolean allowed = teams.isFriendlyFireAllowed(info.owner(), info.slot(), attacker, victim);
        if (!allowed) event.setCancelled(true);
    }
}

