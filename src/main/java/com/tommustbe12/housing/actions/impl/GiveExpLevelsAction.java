package com.tommustbe12.housing.actions.impl;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.ActionContext;

public final class GiveExpLevelsAction implements Action {
    private final int levels;

    public GiveExpLevelsAction(int levels) {
        this.levels = levels;
    }

    @Override
    public String type() {
        return "give_exp_levels";
    }

    @Override
    public void execute(ActionContext ctx) {
        if (ctx.player() == null) return;
        ctx.player().giveExpLevels(levels);
    }

    public int levels() {
        return levels;
    }
}

