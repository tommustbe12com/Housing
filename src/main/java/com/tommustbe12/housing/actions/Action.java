package com.tommustbe12.housing.actions;

public interface Action {
    String type();

    void execute(ActionContext ctx);
}

