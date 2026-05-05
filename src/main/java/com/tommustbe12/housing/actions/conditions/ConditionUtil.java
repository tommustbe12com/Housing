package com.tommustbe12.housing.actions.conditions;

final class ConditionUtil {
    private ConditionUtil() {}

    static Double num(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    static boolean compare(String left, CompareOp op, String right) {
        Double ln = num(left);
        Double rn = num(right);
        if (ln != null && rn != null && (op == CompareOp.GT || op == CompareOp.GTE || op == CompareOp.LT || op == CompareOp.LTE)) {
            return switch (op) {
                case GT -> ln > rn;
                case GTE -> ln >= rn;
                case LT -> ln < rn;
                case LTE -> ln <= rn;
                default -> false;
            };
        }
        return switch (op) {
            case EQ -> left.equalsIgnoreCase(right);
            case NEQ -> !left.equalsIgnoreCase(right);
            case GT -> left.compareToIgnoreCase(right) > 0;
            case GTE -> left.compareToIgnoreCase(right) >= 0;
            case LT -> left.compareToIgnoreCase(right) < 0;
            case LTE -> left.compareToIgnoreCase(right) <= 0;
        };
    }
}

