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
        Attribute maxHealthAttr = resolveMaxHealthAttribute();
        if (maxHealthAttr == null) return;
        var attr = target.getAttribute(maxHealthAttr);
        if (attr == null) return;
        double clamped = Math.max(1.0, Math.min(2048.0, maxHealth));
        attr.setBaseValue(clamped);
        if (target.getHealth() > clamped) target.setHealth(clamped);
    }

    private static Attribute resolveMaxHealthAttribute() {
        for (Attribute a : Attribute.values()) {
            if (a == null) continue;
            String n = a.name();
            if ("GENERIC_MAX_HEALTH".equals(n) || "MAX_HEALTH".equals(n)) return a;
        }
        return null;
    }
}
