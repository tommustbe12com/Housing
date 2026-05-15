package com.tommustbe12.housing.actions.impl;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.ActionContext;
import com.tommustbe12.housing.houses.HouseManager;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public final class LaunchToTargetAction implements Action {
    public enum Target { EDITOR, COORDS, HOUSE_SPAWN }

    private final HouseManager houses;
    private final Target target;
    private final double x, y, z;
    private final double strength;

    public LaunchToTargetAction(HouseManager houses, Target target, double x, double y, double z, double strength) {
        this.houses = houses;
        this.target = target == null ? Target.EDITOR : target;
        this.x = x; this.y = y; this.z = z;
        this.strength = Math.max(0.0, strength);
    }

    public Target target() { return target; }
    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }
    public double strength() { return strength; }

    @Override
    public String type() { return "launch_to_target"; }

    @Override
    public void execute(ActionContext ctx) {
        if (ctx == null || ctx.world() == null) return;
        var p = ctx.other() != null ? ctx.other() : ctx.player();
        if (p == null) return;
        Location t = null;
        if (target == Target.EDITOR && ctx.location() != null) t = ctx.location();
        else if (target == Target.COORDS) t = new Location(ctx.world(), x, y, z);
        else if (target == Target.HOUSE_SPAWN && houses != null) {
            try {
                var data = houses.getHouse(ctx.houseOwner(), ctx.houseSlot());
                if (data != null) t = data.spawnInWorld(ctx.world());
            } catch (Exception ignored) {}
        }
        if (t == null) return;
        Vector dir = t.toVector().subtract(p.getLocation().toVector());
        if (dir.lengthSquared() <= 0.0001) return;
        dir.normalize().multiply(strength);
        p.setVelocity(dir);
    }
}

