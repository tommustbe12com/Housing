package com.tommustbe12.housing.actions.impl;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.ActionContext;
import org.bukkit.util.Vector;

public final class ChangeVelocityAction implements Action {
    private final double x;
    private final double y;
    private final double z;

    public ChangeVelocityAction(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }

    @Override
    public String type() {
        return "change_velocity";
    }

    @Override
    public void execute(ActionContext ctx) {
        if (ctx == null) return;
        var target = ctx.other() != null ? ctx.other() : ctx.player();
        if (target == null) return;
        target.setVelocity(new Vector(x, y, z));
    }
}

