package com.tommustbe12.housing.actions.storage;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.impl.ChangeVariableAction;
import com.tommustbe12.housing.actions.impl.SendChatMessageAction;
import com.tommustbe12.housing.actions.impl.SendToHubAction;
import com.tommustbe12.housing.actions.placeholders.Placeholders;
import com.tommustbe12.housing.actions.placeholders.VariablesStore;
import com.tommustbe12.housing.houses.HouseManager;

import java.util.Map;

public final class SimpleActionCodec implements ActionCodec {
    private final Placeholders placeholders;
    private final VariablesStore variables;
    private final HouseManager houses;

    public SimpleActionCodec(Placeholders placeholders, VariablesStore variables, HouseManager houses) {
        this.placeholders = placeholders;
        this.variables = variables;
        this.houses = houses;
    }

    @Override
    public Action decode(Map<?, ?> map) {
        String type = ActionCodec.typeOf(map);
        if (type == null) return null;
        return switch (type) {
            case "chat_message" -> new SendChatMessageAction(placeholders, string(map, "message"));
            case "send_to_hub" -> new SendToHubAction(houses);
            case "change_variable" -> new ChangeVariableAction(variables, placeholders, string(map, "key"), string(map, "value"));
            default -> null;
        };
    }

    private static String string(Map<?, ?> map, String key) {
        Object v = map.get(key);
        return v == null ? "" : v.toString();
    }
}

