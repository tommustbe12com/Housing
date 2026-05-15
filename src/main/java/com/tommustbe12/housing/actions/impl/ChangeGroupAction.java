package com.tommustbe12.housing.actions.impl;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.ActionContext;
import com.tommustbe12.housing.groups.HouseGroupsService;

import java.util.UUID;

public final class ChangeGroupAction implements Action {
    private final HouseGroupsService groups;
    private final UUID groupId;

    public ChangeGroupAction(HouseGroupsService groups, UUID groupId) {
        this.groups = groups;
        this.groupId = groupId;
    }

    public UUID groupId() { return groupId; }

    @Override
    public String type() {
        return "change_group";
    }

    @Override
    public void execute(ActionContext ctx) {
        if (ctx == null || groups == null) return;
        var target = ctx.other() != null ? ctx.other() : ctx.player();
        if (target == null) return;
        groups.setGroup(ctx.houseOwner(), ctx.houseSlot(), target.getUniqueId(), groupId);
    }
}

