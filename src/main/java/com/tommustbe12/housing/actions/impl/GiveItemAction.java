package com.tommustbe12.housing.actions.impl;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.ActionContext;
import org.bukkit.inventory.ItemStack;

public final class GiveItemAction implements Action {
    private final ItemStack item;
    private final int amount;
    private final Integer slot;
    private final boolean replaceSlot;

    public GiveItemAction(ItemStack item, int amount, Integer slot, boolean replaceSlot) {
        this.item = item;
        this.amount = Math.max(1, amount);
        this.slot = slot;
        this.replaceSlot = replaceSlot;
    }

    public ItemStack item() { return item; }
    public int amount() { return amount; }
    public Integer slot() { return slot; }
    public boolean replaceSlot() { return replaceSlot; }

    @Override
    public String type() {
        return "give_item";
    }

    @Override
    public void execute(ActionContext ctx) {
        if (ctx == null || item == null || item.getType().isAir()) return;
        var target = ctx.other() != null ? ctx.other() : ctx.player();
        if (target == null) return;

        ItemStack give = item.clone();
        give.setAmount(Math.min(give.getMaxStackSize(), amount));

        if (slot != null && slot >= 0 && slot < target.getInventory().getSize()) {
            if (replaceSlot) {
                target.getInventory().setItem(slot, give);
                target.updateInventory();
                return;
            }
            // Try put into that slot only if empty, otherwise fallback to add.
            ItemStack existing = target.getInventory().getItem(slot);
            if (existing == null || existing.getType().isAir()) {
                target.getInventory().setItem(slot, give);
                target.updateInventory();
                return;
            }
        }

        var leftovers = target.getInventory().addItem(give);
        if (!leftovers.isEmpty()) {
            for (ItemStack it : leftovers.values()) {
                if (it == null || it.getType().isAir()) continue;
                target.getWorld().dropItemNaturally(target.getLocation(), it);
            }
        }
        target.updateInventory();
    }
}

