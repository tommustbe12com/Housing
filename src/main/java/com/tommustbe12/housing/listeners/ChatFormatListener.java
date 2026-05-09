package com.tommustbe12.housing.listeners;

import com.tommustbe12.housing.houses.HouseManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;

public final class ChatFormatListener implements Listener {
    private final Plugin plugin;
    private final HouseManager houses;

    public ChatFormatListener(Plugin plugin, HouseManager houses) {
        this.plugin = plugin;
        this.houses = houses;
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
            event.setFormat("Â§7[HUB] Â§f" + player.getName() + "Â§7: Â§f" + event.getMessage());
            return;
        }

        // House chat is scoped to the world for that house only.
        event.getRecipients().removeIf(p -> p.getWorld() != player.getWorld());

        boolean isOwner = player.getUniqueId().equals(info.owner());
        if (isOwner) {
            event.setFormat("Â§6[OWNER] Â§f" + player.getName() + "Â§7: Â§f" + event.getMessage());
        } else {
            event.setFormat("Â§f" + player.getName() + "Â§7: Â§f" + event.getMessage());
        }
    }

    private World resolveHubWorld() {
        String hubWorldName = plugin.getConfig().getString("hub.world", "");
        if (hubWorldName == null || hubWorldName.isBlank()) return Bukkit.getWorlds().getFirst();
        World hub = Bukkit.getWorld(hubWorldName);
        return hub != null ? hub : Bukkit.getWorlds().getFirst();
    }
}

