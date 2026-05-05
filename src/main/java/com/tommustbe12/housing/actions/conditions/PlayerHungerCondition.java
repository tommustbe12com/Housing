package com.tommustbe12.housing.actions.conditions;

import com.tommustbe12.housing.actions.ActionContext;

public final class PlayerHungerCondition implements Condition {
    private final CompareOp op;
    private final int value;

    public PlayerHungerCondition(CompareOp op, int value) {
        this.op = op;
        this.value = value;
    }

    @Override
    public String type() {
        return "player_hunger";
    }

    @Override
    public boolean test(ActionContext ctx) {
        if (ctx.player() == null) return false;
        return ConditionUtil.compare(Integer.toString(ctx.player().getFoodLevel()), op, Integer.toString(value));
    }

    public CompareOp op() { return op; }
    public int value() { return value; }
}

