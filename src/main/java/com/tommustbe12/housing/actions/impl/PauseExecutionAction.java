package com.tommustbe12.housing.actions.impl;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.ActionContext;

public final class PauseExecutionAction implements Action {
    private final long ticks;

    public PauseExecutionAction(long ticks) {
        this.ticks = Math.max(0L, ticks);
    }

    public long ticks() { return ticks; }

    @Override
    public String type() {
        return "pause_execution";
    }

    @Override
    public void execute(ActionContext ctx) {
        // Handled by ActionsEngine (schedules continuation).
    }
}

