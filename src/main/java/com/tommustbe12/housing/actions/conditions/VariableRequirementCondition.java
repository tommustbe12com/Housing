package com.tommustbe12.housing.actions.conditions;

import com.tommustbe12.housing.actions.ActionContext;
import com.tommustbe12.housing.actions.placeholders.Placeholders;

public final class VariableRequirementCondition implements Condition {
    private final Placeholders placeholders;
    private final String key; // %stat.x% or raw key
    private final CompareOp op;
    private final String value;

    public VariableRequirementCondition(Placeholders placeholders, String key, CompareOp op, String value) {
        this.placeholders = placeholders;
        this.key = key;
        this.op = op;
        this.value = value;
    }

    @Override
    public String type() {
        return "variable_requirement";
    }

    @Override
    public boolean test(ActionContext ctx) {
        String resolved = placeholders.resolve(ctx, key);
        String expected = placeholders.resolve(ctx, value);
        return ConditionUtil.compare(resolved, op, expected);
    }

    public String key() { return key; }
    public CompareOp op() { return op; }
    public String value() { return value; }
}

