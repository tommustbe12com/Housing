package com.tommustbe12.housing.actions.impl;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.ActionContext;
import com.tommustbe12.housing.houses.HouseManager;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

public final class DropItemAction implements Action {
    public enum Where { PLAYER, EDITOR, COORDS, HOUSE_SPAWN }

    private final HouseManager houses;
    private final ItemStack item;
    private final int amount;
    private final Where where;
    private final double x, y, z;

    public DropItemAction(HouseManager houses, ItemStack item, int amount, Where where, double x, double y, double z) {
        this.houses = houses;
        this.item = item;
        this.amount = Math.max(1, amount);
        this.where = where == null ? Where.PLAYER : where;
        this.x = x; this.y = y; this.z = z;
    }

    public ItemStack item() { return item; }
    public int amount() { return amount; }
    public Where where() { return where; }
    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }

    @Override
    public String type() { return "drop_item"; }

    @Override
    public void execute(ActionContext ctx) {
        if (ctx == null || ctx.world() == null || item == null || item.getType().isAir()) return;
        Location loc = null;
        var target = ctx.other() != null ? ctx.other() : ctx.player();
        if (where == Where.PLAYER && target != null) loc = target.getLocation();
        else if (where == Where.EDITOR && ctx.location() != null) loc = ctx.location();
        else if (where == Where.COORDS) loc = new Location(ctx.world(), x, y, z);
        else if (where == Where.HOUSE_SPAWN && houses != null) {
            try {
                var data = houses.getHouse(ctx.houseOwner(), ctx.houseSlot());
                if (data != null) loc = data.spawnInWorld(ctx.world());
            } catch (Exception ignored) {}
        }
        if (loc == null) return;
        ItemStack drop = item.clone();
        drop.setAmount(Math.min(drop.getMaxStackSize(), amount));
        ctx.world().dropItemNaturally(loc, drop);
    }
}

