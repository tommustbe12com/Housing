package com.tommustbe12.housing.listeners;

import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.groups.HouseGroupsService;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;

public final class ChatFormatListener implements Listener {
    private final Plugin plugin;
    private final HouseManager houses;
    private final HouseGroupsService groups;

    public ChatFormatListener(Plugin plugin, HouseManager houses, HouseGroupsService groups) {
        this.plugin = plugin;
        this.houses = houses;
        this.groups = groups;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        var player = event.getPlayer();
        var info = houses.getHouseInfoByWorld(player.getWorld());

        // No global chat: chat is either hub-only or per-house.
        if (info == null) {
            World hub = resolveHubWorld();
            if (hub == null) return;
            event.getRecipients().removeIf(p -> p.getWorld() != hub);
            // Hub chat: keep it clean but consistent.
            event.setFormat("§f" + player.getName() + "§7: §f" + event.getMessage());
            return;
        }

        // House chat is scoped to the world for that house only.
        event.getRecipients().removeIf(p -> p.getWorld() != player.getWorld());

        if (groups.isMuted(info.owner(), info.slot(), player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage("§cYou are muted in this house.");
            return;
        }

        String tag = groups.tagForDisplay(info.owner(), info.slot(), player.getUniqueId());
        event.setFormat(tag + " §f" + player.getName() + "§7: §f" + event.getMessage());
    }

    private World resolveHubWorld() {
        String hubWorldName = plugin.getConfig().getString("hub.world", "");
        if (hubWorldName == null || hubWorldName.isBlank()) return Bukkit.getWorlds().getFirst();
        World hub = Bukkit.getWorld(hubWorldName);
        return hub != null ? hub : Bukkit.getWorlds().getFirst();
    }
}
