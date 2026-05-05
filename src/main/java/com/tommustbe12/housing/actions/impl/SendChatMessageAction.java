package com.tommustbe12.housing.actions.impl;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.ActionContext;
import com.tommustbe12.housing.actions.placeholders.Placeholders;
import org.bukkit.ChatColor;

public final class SendChatMessageAction implements Action {
    private final Placeholders placeholders;
    private final String message;

    public SendChatMessageAction(Placeholders placeholders, String message) {
        this.placeholders = placeholders;
        this.message = message;
    }

    @Override
    public String type() {
        return "chat_message";
    }

    @Override
    public void execute(ActionContext ctx) {
        if (ctx.player() == null) return;
        String msg = ChatColor.translateAlternateColorCodes('&', placeholders.resolve(ctx, message));
        ctx.player().sendMessage(msg);
    }
}

