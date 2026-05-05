package com.tommustbe12.housing.actions.impl;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.ActionContext;

public final class KillPlayerAction implements Action {
    @Override
    public String type() {
        return "kill_player";
    }

    @Override
    public void execute(ActionContext ctx) {
        if (ctx.player() == null) return;
        ctx.player().setHealth(0.0);
    }
}

