package com.tommustbe12.housing.listeners;

import com.tommustbe12.housing.houses.HouseManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public final class ChatFormatListener implements Listener {
    private final HouseManager houses;

    public ChatFormatListener(HouseManager houses) {
        this.houses = houses;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        var player = event.getPlayer();
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null) return;

        boolean isOwner = player.getUniqueId().equals(info.owner());
        if (isOwner) {
            event.setFormat("§6[OWNER] §f" + player.getName() + "§7: §f" + event.getMessage());
        } else {
            event.setFormat("§f" + player.getName() + "§7: §f" + event.getMessage());
        }
    }
}
