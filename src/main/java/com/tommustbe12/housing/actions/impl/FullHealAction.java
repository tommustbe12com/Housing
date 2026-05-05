package com.tommustbe12.housing.actions.impl;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.ActionContext;

public final class FullHealAction implements Action {
    @Override
    public String type() {
        return "full_heal";
    }

    @Override
    public void execute(ActionContext ctx) {
        if (ctx.player() == null) return;
        ctx.player().setHealth(ctx.player().getMaxHealth());
        ctx.player().setFoodLevel(20);
        ctx.player().setSaturation(20f);
        ctx.player().setFireTicks(0);
    }
}

