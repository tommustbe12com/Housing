package com.tommustbe12.housing.actions.impl;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.ActionContext;

public final class ChangeHealthAction implements Action {
    private final double health;

    public ChangeHealthAction(double health) {
        this.health = health;
    }

    public double health() { return health; }

    @Override
    public String type() {
        return "change_health";
    }

    @Override
    public void execute(ActionContext ctx) {
        if (ctx == null) return;
        var target = ctx.other() != null ? ctx.other() : ctx.player();
        if (target == null) return;
        double max = target.getMaxHealth();
        double clamped = Math.max(0.0, Math.min(max, health));
        target.setHealth(clamped);
    }
}

