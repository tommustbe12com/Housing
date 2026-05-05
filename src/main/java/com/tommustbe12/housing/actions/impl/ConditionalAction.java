package com.tommustbe12.housing.actions.impl;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.ActionContext;
import com.tommustbe12.housing.actions.ActionList;
import com.tommustbe12.housing.actions.placeholders.Placeholders;

public final class ConditionalAction implements Action {
    public enum Op { EQ, NEQ, GT, GTE, LT, LTE }

    private final Placeholders placeholders;
    private final String left;
    private final Op op;
    private final String right;
    private final ActionList thenList;
    private final ActionList elseList;

    public ConditionalAction(Placeholders placeholders, String left, Op op, String right, ActionList thenList, ActionList elseList) {
        this.placeholders = placeholders;
        this.left = left;
        this.op = op;
        this.right = right;
        this.thenList = thenList;
        this.elseList = elseList;
    }

    @Override
    public String type() {
        return "conditional";
    }

    @Override
    public void execute(ActionContext ctx) {
        String l = placeholders.resolve(ctx, left);
        String r = placeholders.resolve(ctx, right);
        boolean result = compare(l, r);
        if (result) runList(thenList, ctx);
        else if (elseList != null) runList(elseList, ctx);
    }

    private static void runList(ActionList list, ActionContext ctx) {
        if (list == null) return;
        for (Action action : list.actions()) action.execute(ctx);
    }

    private boolean compare(String l, String r) {
        Double ln = parseNumber(l);
        Double rn = parseNumber(r);
        if (ln != null && rn != null && (op == Op.GT || op == Op.GTE || op == Op.LT || op == Op.LTE)) {
            return switch (op) {
                case GT -> ln > rn;
                case GTE -> ln >= rn;
                case LT -> ln < rn;
                case LTE -> ln <= rn;
                default -> false;
            };
        }
        return switch (op) {
            case EQ -> l.equalsIgnoreCase(r);
            case NEQ -> !l.equalsIgnoreCase(r);
            case GT -> l.compareToIgnoreCase(r) > 0;
            case GTE -> l.compareToIgnoreCase(r) >= 0;
            case LT -> l.compareToIgnoreCase(r) < 0;
            case LTE -> l.compareToIgnoreCase(r) <= 0;
        };
    }

    private static Double parseNumber(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    public String left() { return left; }
    public Op op() { return op; }
    public String right() { return right; }
    public ActionList thenList() { return thenList; }
    public ActionList elseList() { return elseList; }
}

