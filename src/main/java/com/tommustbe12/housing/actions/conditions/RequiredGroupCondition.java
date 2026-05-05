package com.tommustbe12.housing.actions.conditions;

import com.tommustbe12.housing.actions.ActionContext;
import com.tommustbe12.housing.actions.placeholders.Placeholders;

public final class RequiredGroupCondition implements Condition {
    private final Placeholders placeholders;
    private final String requiredGroup;

    public RequiredGroupCondition(Placeholders placeholders, String requiredGroup) {
        this.placeholders = placeholders;
        this.requiredGroup = requiredGroup;
    }

    @Override
    public String type() {
        return "required_group";
    }

    @Override
    public boolean test(ActionContext ctx) {
        // V1 group system: treat %stat.group% as the player's group string.
        String group = placeholders.resolve(ctx, "%stat.group%");
        return group != null && group.equalsIgnoreCase(requiredGroup);
    }

    public String requiredGroup() {
        return requiredGroup;
    }
}

