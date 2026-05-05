package com.tommustbe12.housing.chat;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class ChatPrompts {
    private final Map<UUID, Consumer<String>> pending = new ConcurrentHashMap<>();

    public void prompt(Player player, String question, Consumer<String> onAnswer) {
        pending.put(player.getUniqueId(), onAnswer);
        player.sendMessage("§bHousing§7: §f" + question + " §7(type in chat)");
        player.sendMessage("§7Type §ccancel §7to abort.");
    }

    public boolean handle(Player player, String message) {
        Consumer<String> consumer = pending.remove(player.getUniqueId());
        if (consumer == null) return false;
        consumer.accept(message);
        return true;
    }
}

