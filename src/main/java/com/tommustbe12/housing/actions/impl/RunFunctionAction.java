package com.tommustbe12.housing.actions.impl;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.ActionContext;

public final class RunFunctionAction implements Action {
    public interface FunctionRunner {
        void runFunction(ActionContext ctx, String functionName, boolean global);
    }

    private final FunctionRunner runner;
    private final String functionName;
    private final boolean global;

    public RunFunctionAction(FunctionRunner runner, String functionName, boolean global) {
        this.runner = runner;
        this.functionName = functionName;
        this.global = global;
    }

    @Override
    public String type() {
        return "run_function";
    }

    @Override
    public void execute(ActionContext ctx) {
        runner.runFunction(ctx, functionName, global);
    }

    public String functionName() {
        return functionName;
    }

    public boolean global() {
        return global;
    }
}

