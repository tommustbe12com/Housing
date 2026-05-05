package com.tommustbe12.housing.actions.impl;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.ActionContext;
import com.tommustbe12.housing.actions.ActionList;
import com.tommustbe12.housing.actions.placeholders.Placeholders;
import com.tommustbe12.housing.actions.conditions.Condition;

import java.util.ArrayList;
import java.util.List;

public final class ConditionalAction implements Action {
    public enum Op { EQ, NEQ, GT, GTE, LT, LTE }

    private final Placeholders placeholders;
    private final List<Condition> conditions;
    private final boolean matchAny;
    private final ActionList thenList;
    private final ActionList elseList;

    public ConditionalAction(Placeholders placeholders, List<Condition> conditions, boolean matchAny, ActionList thenList, ActionList elseList) {
        this.placeholders = placeholders;
        this.conditions = conditions == null ? new ArrayList<>() : new ArrayList<>(conditions);
        this.matchAny = matchAny;
        this.thenList = thenList;
        this.elseList = elseList;
    }

    @Override
    public String type() {
        return "conditional";
    }

    @Override
    public void execute(ActionContext ctx) {
        boolean result = evaluate(ctx);
        if (result) runList(thenList, ctx);
        else if (elseList != null) runList(elseList, ctx);
    }

    private static void runList(ActionList list, ActionContext ctx) {
        if (list == null) return;
        for (Action action : list.actions()) action.execute(ctx);
    }

    private boolean evaluate(ActionContext ctx) {
        if (conditions.isEmpty()) return false;
        if (matchAny) {
            for (Condition c : conditions) if (c.test(ctx)) return true;
            return false;
        }
        for (Condition c : conditions) if (!c.test(ctx)) return false;
        return true;
    }

    public List<Condition> conditions() { return new ArrayList<>(conditions); }
    public boolean matchAny() { return matchAny; }
    public ActionList thenList() { return thenList; }
    public ActionList elseList() { return elseList; }
}
