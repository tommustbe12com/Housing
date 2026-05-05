package com.tommustbe12.housing.actions.impl;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.ActionContext;
import com.tommustbe12.housing.actions.placeholders.Placeholders;
import com.tommustbe12.housing.actions.placeholders.VariablesStore;

public final class ChangeVariableAction implements Action {
    private final VariablesStore variables;
    private final Placeholders placeholders;
    private final String key;
    private final String value;

    public ChangeVariableAction(VariablesStore variables, Placeholders placeholders, String key, String value) {
        this.variables = variables;
        this.placeholders = placeholders;
        this.key = key;
        this.value = value;
    }

    @Override
    public String type() {
        return "change_variable";
    }

    @Override
    public void execute(ActionContext ctx) {
        if (ctx.player() == null) return;
        String cleaned = key;
        if (cleaned.startsWith("%stats.") && cleaned.endsWith("%")) {
            cleaned = cleaned.substring("%stats.".length(), cleaned.length() - 1);
        }
        variables.set(ctx.houseOwner(), ctx.houseSlot(), ctx.player().getUniqueId(), cleaned, placeholders.resolve(ctx, value));
    }
}

