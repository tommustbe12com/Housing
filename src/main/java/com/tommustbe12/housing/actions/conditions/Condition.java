package com.tommustbe12.housing.actions.conditions;

import com.tommustbe12.housing.actions.ActionContext;

public interface Condition {
    String type();

    boolean test(ActionContext ctx);
}

