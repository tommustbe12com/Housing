package com.tommustbe12.housing.actions.conditions;

import com.tommustbe12.housing.actions.ActionContext;

public final class MaxHealthCondition implements Condition {
    private final CompareOp op;
    private final double value;

    public MaxHealthCondition(CompareOp op, double value) {
        this.op = op;
        this.value = value;
    }

    @Override
    public String type() {
        return "max_player_health";
    }

    @Override
    public boolean test(ActionContext ctx) {
        if (ctx.player() == null) return false;
        return ConditionUtil.compare(Double.toString(ctx.player().getMaxHealth()), op, Double.toString(value));
    }

    public CompareOp op() { return op; }
    public double value() { return value; }
}

