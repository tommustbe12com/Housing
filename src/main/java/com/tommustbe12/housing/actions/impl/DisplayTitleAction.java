package com.tommustbe12.housing.actions.impl;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.ActionContext;
import com.tommustbe12.housing.actions.placeholders.Placeholders;
import org.bukkit.ChatColor;

public final class DisplayTitleAction implements Action {
    private final Placeholders placeholders;
    private final String title;
    private final String subtitle;
    private final int fadeIn;
    private final int stay;
    private final int fadeOut;

    public DisplayTitleAction(Placeholders placeholders, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        this.placeholders = placeholders;
        this.title = title;
        this.subtitle = subtitle;
        this.fadeIn = fadeIn;
        this.stay = stay;
        this.fadeOut = fadeOut;
    }

    @Override
    public String type() {
        return "display_title";
    }

    @Override
    public void execute(ActionContext ctx) {
        if (ctx.player() == null) return;
        String t = ChatColor.translateAlternateColorCodes('&', placeholders.resolve(ctx, title));
        String s = ChatColor.translateAlternateColorCodes('&', placeholders.resolve(ctx, subtitle));
        ctx.player().sendTitle(t, s, fadeIn, stay, fadeOut);
    }

    public String title() { return title; }
    public String subtitle() { return subtitle; }
    public int fadeIn() { return fadeIn; }
    public int stay() { return stay; }
    public int fadeOut() { return fadeOut; }
}

