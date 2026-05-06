package com.tommustbe12.housing.actions.impl;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.ActionContext;
import com.tommustbe12.housing.inventorylayouts.InventoryLayout;
import com.tommustbe12.housing.inventorylayouts.InventoryLayoutsService;

import java.util.UUID;

public final class ApplyInventoryLayoutAction implements Action {
    private final InventoryLayoutsService layouts;
    private final UUID layoutId;

    public ApplyInventoryLayoutAction(InventoryLayoutsService layouts, UUID layoutId) {
        this.layouts = layouts;
        this.layoutId = layoutId;
    }

    @Override
    public String type() {
        return "apply_inventory_layout";
    }

    public UUID layoutId() {
        return layoutId;
    }

    @Override
    public void execute(ActionContext ctx) {
        if (ctx == null || ctx.player() == null || layoutId == null) return;
        InventoryLayout layout = layouts.find(ctx.houseOwner(), ctx.houseSlot(), layoutId);
        if (layout == null) return;
        layouts.apply(ctx.player(), layout);
        com.tommustbe12.housing.util.HousingItems.ensureMenuStar(ctx.plugin(), ctx.player());
    }
}
