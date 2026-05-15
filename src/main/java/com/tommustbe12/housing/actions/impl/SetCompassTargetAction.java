package com.tommustbe12.housing.actions.impl;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.ActionContext;
import org.bukkit.Location;

public final class SetCompassTargetAction implements Action {
    public enum Direction { N, NE, E, SE, S, SW, W, NW }

    private final Direction direction;

    public SetCompassTargetAction(Direction direction) {
        this.direction = direction == null ? Direction.N : direction;
    }

    public String directionName() { return direction.name(); }

    @Override
    public String type() {
        return "set_compass_target";
    }

    @Override
    public void execute(ActionContext ctx) {
        if (ctx == null || ctx.world() == null) return;
        var target = ctx.other() != null ? ctx.other() : ctx.player();
        if (target == null) return;
        Location base = target.getLocation();
        double dx = 0, dz = 0;
        switch (direction) {
            case N -> dz = -1000;
            case NE -> { dx = 1000; dz = -1000; }
            case E -> dx = 1000;
            case SE -> { dx = 1000; dz = 1000; }
            case S -> dz = 1000;
            case SW -> { dx = -1000; dz = 1000; }
            case W -> dx = -1000;
            case NW -> { dx = -1000; dz = -1000; }
        }
        target.setCompassTarget(new Location(ctx.world(), base.getX() + dx, base.getY(), base.getZ() + dz));
    }
}

