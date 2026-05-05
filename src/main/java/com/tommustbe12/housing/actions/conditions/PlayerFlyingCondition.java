package com.tommustbe12.housing.actions.conditions;

import com.tommustbe12.housing.actions.ActionContext;

public final class PlayerFlyingCondition implements Condition {
    @Override
    public String type() {
        return "player_flying";
    }

    @Override
    public boolean test(ActionContext ctx) {
        return ctx.player() != null && ctx.player().isFlying();
    }
}

