package com.tommustbe12.housing.actions.impl;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.ActionContext;
import com.tommustbe12.housing.houses.HouseManager;
import org.bukkit.Location;

public final class TeleportPlayerAction implements Action {
    public enum Mode { CURRENT_EDITOR, COORDS, HOUSE_SPAWN }

    private final HouseManager houses;
    private final Mode mode;
    private final double x, y, z;
    private final float yaw, pitch;

    public TeleportPlayerAction(HouseManager houses, Mode mode, double x, double y, double z, float yaw, float pitch) {
        this.houses = houses;
        this.mode = mode == null ? Mode.HOUSE_SPAWN : mode;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public Mode mode() { return mode; }
    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }
    public float yaw() { return yaw; }
    public float pitch() { return pitch; }

    @Override
    public String type() {
        return "teleport_player";
    }

    @Override
    public void execute(ActionContext ctx) {
        if (ctx == null) return;
        var target = ctx.other() != null ? ctx.other() : ctx.player();
        if (target == null) return;

        Location dest = null;
        if (mode == Mode.HOUSE_SPAWN && houses != null) {
            try {
                var data = houses.getHouse(ctx.houseOwner(), ctx.houseSlot());
                if (data != null) dest = data.spawnInWorld(ctx.world());
            } catch (Exception ignored) {}
        } else if (mode == Mode.COORDS) {
            if (ctx.world() != null) dest = new Location(ctx.world(), x, y, z, yaw, pitch);
        } else if (mode == Mode.CURRENT_EDITOR) {
            if (ctx.location() != null) dest = ctx.location().clone();
        }

        if (dest == null) return;
        target.teleport(dest);
    }
}

