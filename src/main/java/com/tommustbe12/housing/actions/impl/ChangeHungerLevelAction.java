package com.tommustbe12.housing.actions.impl;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.ActionContext;

public final class ChangeHungerLevelAction implements Action {
    private final int foodLevel;

    public ChangeHungerLevelAction(int foodLevel) {
        this.foodLevel = foodLevel;
    }

    public int foodLevel() { return foodLevel; }

    @Override
    public String type() {
        return "change_hunger";
    }

    @Override
    public void execute(ActionContext ctx) {
        if (ctx == null) return;
        var target = ctx.other() != null ? ctx.other() : ctx.player();
        if (target == null) return;
        int clamped = Math.max(0, Math.min(20, foodLevel));
        target.setFoodLevel(clamped);
    }
}

