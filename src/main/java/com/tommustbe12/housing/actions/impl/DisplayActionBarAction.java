package com.tommustbe12.housing.actions.impl;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.ActionContext;
import com.tommustbe12.housing.actions.placeholders.Placeholders;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;

public final class DisplayActionBarAction implements Action {
    private final Placeholders placeholders;
    private final String message;

    public DisplayActionBarAction(Placeholders placeholders, String message) {
        this.placeholders = placeholders;
        this.message = message;
    }

    @Override
    public String type() {
        return "display_action_bar";
    }

    @Override
    public void execute(ActionContext ctx) {
        if (ctx.player() == null) return;
        String msg = ChatColor.translateAlternateColorCodes('&', placeholders.resolve(ctx, message));
        ctx.player().sendActionBar(Component.text(msg));
    }

    public String message() { return message; }
}

