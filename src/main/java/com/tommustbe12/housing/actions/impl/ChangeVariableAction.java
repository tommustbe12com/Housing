package com.tommustbe12.housing.actions.impl;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.ActionContext;
import com.tommustbe12.housing.actions.placeholders.Placeholders;
import com.tommustbe12.housing.actions.placeholders.VariablesStore;

public final class ChangeVariableAction implements Action {
    public enum Operation { SET, ADD, SUBTRACT }

    private final VariablesStore variables;
    private final Placeholders placeholders;
    private final String key;
    private final String value;
    private final Operation operation;

    public ChangeVariableAction(VariablesStore variables, Placeholders placeholders, String key, String value) {
        this(variables, placeholders, key, value, Operation.SET);
    }

    public ChangeVariableAction(VariablesStore variables, Placeholders placeholders, String key, String value, Operation operation) {
        this.variables = variables;
        this.placeholders = placeholders;
        this.key = key;
        this.value = value;
        this.operation = operation == null ? Operation.SET : operation;
    }

    @Override
    public String type() {
        return "change_variable";
    }

    @Override
    public void execute(ActionContext ctx) {
        if (ctx.player() == null) return;
        String cleaned = key;
        if (cleaned.endsWith("%")) {
            if (cleaned.startsWith("%stats.")) {
                cleaned = cleaned.substring("%stats.".length(), cleaned.length() - 1);
            } else if (cleaned.startsWith("%stat.")) {
                cleaned = cleaned.substring("%stat.".length(), cleaned.length() - 1);
            }
        }
        String resolved = placeholders.resolve(ctx, value);
        if (operation == Operation.SET) {
            variables.set(ctx.houseOwner(), ctx.houseSlot(), ctx.player().getUniqueId(), cleaned, resolved);
            return;
        }

        double delta;
        try { delta = Double.parseDouble(resolved.trim()); } catch (Exception e) { return; }
        String currentRaw = variables.get(ctx.houseOwner(), ctx.houseSlot(), ctx.player().getUniqueId(), cleaned);
        double current = 0;
        try { current = Double.parseDouble((currentRaw == null ? "0" : currentRaw).trim()); } catch (Exception ignored) {}
        double next = operation == Operation.ADD ? (current + delta) : (current - delta);
        // store as integer if it's integral
        if (Math.floor(next) == next) {
            variables.set(ctx.houseOwner(), ctx.houseSlot(), ctx.player().getUniqueId(), cleaned, Long.toString((long) next));
        } else {
            variables.set(ctx.houseOwner(), ctx.houseSlot(), ctx.player().getUniqueId(), cleaned, Double.toString(next));
        }
    }

    public String key() {
        return key;
    }

    public String value() {
        return value;
    }

    public Operation operation() {
        return operation;
    }
}
