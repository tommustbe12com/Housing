package com.tommustbe12.housing.actions.impl;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.ActionContext;
import com.tommustbe12.housing.teams.TeamsService;

import java.util.UUID;

public final class ChangeTeamAction implements Action {
    private final TeamsService teams;
    private final UUID teamId;

    public ChangeTeamAction(TeamsService teams, UUID teamId) {
        this.teams = teams;
        this.teamId = teamId;
    }

    public UUID teamId() { return teamId; }

    @Override
    public String type() {
        return "change_team";
    }

    @Override
    public void run(ActionContext ctx) {
        if (ctx == null || teams == null) return;
        var target = ctx.other() != null ? ctx.other() : ctx.player();
        if (target == null) return;
        teams.setPlayerTeam(ctx.houseOwner(), ctx.houseSlot(), target.getUniqueId(), teamId);
    }
}

