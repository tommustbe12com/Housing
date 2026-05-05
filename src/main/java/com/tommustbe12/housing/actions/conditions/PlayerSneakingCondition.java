package com.tommustbe12.housing.actions.conditions;

import com.tommustbe12.housing.actions.ActionContext;

public final class PlayerSneakingCondition implements Condition {
    @Override
    public String type() {
        return "player_sneaking";
    }

    @Override
    public boolean test(ActionContext ctx) {
        return ctx.player() != null && ctx.player().isSneaking();
    }
}

