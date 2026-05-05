package com.tommustbe12.housing.actions.conditions;

public enum CompareOp {
    EQ, NEQ, GT, GTE, LT, LTE;

    public static CompareOp next(CompareOp op) {
        CompareOp[] vals = values();
        return vals[(op.ordinal() + 1) % vals.length];
    }
}

