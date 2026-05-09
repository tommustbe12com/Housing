package com.tommustbe12.housing.listeners;

import com.tommustbe12.housing.groups.HouseGroupsService;
import com.tommustbe12.housing.groups.HousePermission;
import com.tommustbe12.housing.houses.HouseManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Locale;

public final class HouseCommandPermissionListener implements Listener {
    private final HouseManager houses;
    private final HouseGroupsService groups;

    public HouseCommandPermissionListener(HouseManager houses, HouseGroupsService groups) {
        this.houses = houses;
        this.groups = groups;
    }

    @EventHandler(ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null) return;

        String msg = event.getMessage();
        String lower = msg.toLowerCase(Locale.ROOT);

        if (lower.startsWith("/edit")) {
            if (!groups.has(info.owner(), info.slot(), player.getUniqueId(), HousePermission.ITEM_EDITOR)) {
                event.setCancelled(true);
                player.sendMessage("§cYou don't have permission to use /edit in this house.");
            }
            return;
        }

        if (lower.startsWith("/tp ")) {
            if (!groups.has(info.owner(), info.slot(), player.getUniqueId(), HousePermission.TP_SELF)) {
                event.setCancelled(true);
                player.sendMessage("§cYou don't have permission to use /tp in this house.");
                return;
            }
            // If attempting to teleport other players, require TP_OTHERS (best-effort check).
            String[] parts = lower.split("\\s+");
            if (parts.length >= 3 && !groups.has(info.owner(), info.slot(), player.getUniqueId(), HousePermission.TP_OTHERS)) {
                event.setCancelled(true);
                player.sendMessage("§cYou don't have permission to teleport other players in this house.");
            }
            return;
        }

        if (lower.startsWith("/gamemode") || lower.startsWith("/gm")) {
            if (!groups.has(info.owner(), info.slot(), player.getUniqueId(), HousePermission.SWITCH_GAMEMODE)) {
                event.setCancelled(true);
                player.sendMessage("§cYou don't have permission to change gamemode in this house.");
            }
            return;
        }

        if (lower.startsWith("/chatspy")) {
            if (!groups.has(info.owner(), info.slot(), player.getUniqueId(), HousePermission.TEAM_CHAT_SPY)) {
                event.setCancelled(true);
                player.sendMessage("§cYou don't have permission to use /chatspy.");
            }
        }
    }
}

