package com.tommustbe12.housing.actions.impl;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.ActionContext;
import com.tommustbe12.housing.groups.HouseGroupsService;
import com.tommustbe12.housing.groups.HousePermission;
import org.bukkit.GameMode;

public final class SetGamemodeAction implements Action {
    private final HouseGroupsService groups;
    private final GameMode mode;

    public SetGamemodeAction(HouseGroupsService groups, GameMode mode) {
        this.groups = groups;
        this.mode = mode == null ? GameMode.ADVENTURE : mode;
    }

    public String modeName() { return mode.name(); }

    @Override
    public String type() {
        return "set_gamemode";
    }

    @Override
    public void execute(ActionContext ctx) {
        if (ctx == null) return;
        var target = ctx.other() != null ? ctx.other() : ctx.player();
        if (target == null) return;
        if (mode == GameMode.CREATIVE && groups != null) {
            boolean canBuild = target.getUniqueId().equals(ctx.houseOwner())
                    || groups.has(ctx.houseOwner(), ctx.houseSlot(), target.getUniqueId(), HousePermission.BUILD);
            if (!canBuild) return;
        }
        target.setGameMode(mode);
    }
}

