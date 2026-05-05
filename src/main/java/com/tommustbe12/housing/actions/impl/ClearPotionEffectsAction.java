package com.tommustbe12.housing.actions.impl;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.ActionContext;

public final class ClearPotionEffectsAction implements Action {
    @Override
    public String type() {
        return "clear_potion_effects";
    }

    @Override
    public void execute(ActionContext ctx) {
        if (ctx.player() == null) return;
        ctx.player().getActivePotionEffects().forEach(pe -> ctx.player().removePotionEffect(pe.getType()));
    }
}

