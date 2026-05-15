package com.tommustbe12.housing.actions.impl;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.ActionContext;
import org.bukkit.inventory.ItemStack;

public final class RemoveItemAction implements Action {
    private final ItemStack match;
    private final int amount;

    public RemoveItemAction(ItemStack match, int amount) {
        this.match = match;
        this.amount = Math.max(1, amount);
    }

    public ItemStack match() { return match; }
    public int amount() { return amount; }

    @Override
    public String type() {
        return "remove_item";
    }

    @Override
    public void execute(ActionContext ctx) {
        if (ctx == null || match == null || match.getType().isAir()) return;
        var target = ctx.other() != null ? ctx.other() : ctx.player();
        if (target == null) return;

        int remaining = amount;
        var inv = target.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack it = inv.getItem(i);
            if (it == null || it.getType().isAir()) continue;
            if (!it.isSimilar(match)) continue;
            int take = Math.min(remaining, it.getAmount());
            it.setAmount(it.getAmount() - take);
            if (it.getAmount() <= 0) inv.setItem(i, null);
            remaining -= take;
            if (remaining <= 0) break;
        }
        target.updateInventory();
    }
}

