package com.tommustbe12.housing.actions.impl;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.ActionContext;
import com.tommustbe12.housing.houses.HouseManager;

public final class SendToHubAction implements Action {
    private final HouseManager houses;

    public SendToHubAction(HouseManager houses) {
        this.houses = houses;
    }

    @Override
    public String type() {
        return "send_to_hub";
    }

    @Override
    public void execute(ActionContext ctx) {
        if (ctx.player() == null) return;
        houses.sendToHub(ctx.player());
    }
}

