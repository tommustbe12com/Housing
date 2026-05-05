package com.tommustbe12.housing.actions.impl;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.ActionContext;

public final class ResetInventoryAction implements Action {
    @Override
    public String type() {
        return "reset_inventory";
    }

    @Override
    public void execute(ActionContext ctx) {
        if (ctx.player() == null) return;
        ctx.player().getInventory().clear();
        ctx.player().getInventory().setArmorContents(null);
        ctx.player().getInventory().setItemInOffHand(null);
    }
}

