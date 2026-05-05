package com.tommustbe12.housing.actions.conditions;

import com.tommustbe12.housing.actions.ActionContext;
import org.bukkit.inventory.ItemStack;

public final class HasItemCondition implements Condition {
    private final ItemStack item;

    public HasItemCondition(ItemStack item) {
        this.item = item;
    }

    @Override
    public String type() {
        return "has_item";
    }

    @Override
    public boolean test(ActionContext ctx) {
        if (ctx.player() == null) return false;
        if (item == null) return false;
        for (ItemStack stack : ctx.player().getInventory().getContents()) {
            if (stack != null && stack.isSimilar(item)) return true;
        }
        return false;
    }

    public ItemStack item() { return item; }
}

