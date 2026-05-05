package com.tommustbe12.housing.actions.storage;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.impl.ChangeVariableAction;
import com.tommustbe12.housing.actions.impl.DisplayActionBarAction;
import com.tommustbe12.housing.actions.impl.DisplayTitleAction;
import com.tommustbe12.housing.actions.impl.FullHealAction;
import com.tommustbe12.housing.actions.impl.KillPlayerAction;
import com.tommustbe12.housing.actions.impl.ResetInventoryAction;
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
            case "kill_player" -> new KillPlayerAction();
            case "full_heal" -> new FullHealAction();
            case "reset_inventory" -> new ResetInventoryAction();
            case "display_action_bar" -> new DisplayActionBarAction(placeholders, string(map, "message"));
            case "display_title" -> new DisplayTitleAction(
                    placeholders,
                    string(map, "title"),
                    string(map, "subtitle"),
                    integer(map, "fadeIn", 10),
                    integer(map, "stay", 40),
                    integer(map, "fadeOut", 10)
            );
            default -> null;
        };
    }

    private static String string(Map<?, ?> map, String key) {
        Object v = map.get(key);
        return v == null ? "" : v.toString();
    }

    private static int integer(Map<?, ?> map, String key, int def) {
        Object v = map.get(key);
        if (v == null) return def;
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
