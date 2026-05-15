package com.tommustbe12.housing.actions.impl;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.ActionContext;
import org.bukkit.attribute.Attribute;

public final class ChangeMaxHealthAction implements Action {
    private final double maxHealth;

    public ChangeMaxHealthAction(double maxHealth) {
        this.maxHealth = maxHealth;
    }

    public double maxHealth() { return maxHealth; }

    @Override
    public String type() {
        return "change_max_health";
    }

    @Override
    public void execute(ActionContext ctx) {
        if (ctx == null) return;
        var target = ctx.other() != null ? ctx.other() : ctx.player();
        if (target == null) return;
        var attr = target.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr == null) return;
        double clamped = Math.max(1.0, Math.min(2048.0, maxHealth));
        attr.setBaseValue(clamped);
        if (target.getHealth() > clamped) target.setHealth(clamped);
    }
}

