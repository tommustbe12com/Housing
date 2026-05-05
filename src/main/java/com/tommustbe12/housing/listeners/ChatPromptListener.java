package com.tommustbe12.housing.listeners;

import com.tommustbe12.housing.chat.ChatPrompts;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public final class ChatPromptListener implements Listener {
    private final ChatPrompts prompts;

    public ChatPromptListener(ChatPrompts prompts) {
        this.prompts = prompts;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (prompts.handle(event.getPlayer(), event.getMessage())) {
            event.setCancelled(true);
        }
    }
}

