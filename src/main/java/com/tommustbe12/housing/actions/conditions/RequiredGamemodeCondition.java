package com.tommustbe12.housing.actions.conditions;

import com.tommustbe12.housing.actions.ActionContext;
import org.bukkit.GameMode;

public final class RequiredGamemodeCondition implements Condition {
    private final GameMode mode;

    public RequiredGamemodeCondition(GameMode mode) {
        this.mode = mode;
    }

    @Override
    public String type() {
        return "required_gamemode";
    }

    @Override
    public boolean test(ActionContext ctx) {
        return ctx.player() != null && ctx.player().getGameMode() == mode;
    }

    public GameMode mode() { return mode; }
}

