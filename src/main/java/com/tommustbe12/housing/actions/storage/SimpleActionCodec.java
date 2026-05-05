package com.tommustbe12.housing.actions.storage;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.impl.ChangeVariableAction;
import com.tommustbe12.housing.actions.impl.ClearPotionEffectsAction;
import com.tommustbe12.housing.actions.impl.DisplayActionBarAction;
import com.tommustbe12.housing.actions.impl.DisplayTitleAction;
import com.tommustbe12.housing.actions.impl.FullHealAction;
import com.tommustbe12.housing.actions.impl.GiveExpLevelsAction;
import com.tommustbe12.housing.actions.impl.KillPlayerAction;
import com.tommustbe12.housing.actions.impl.RunFunctionAction;
import com.tommustbe12.housing.actions.impl.ResetInventoryAction;
import com.tommustbe12.housing.actions.impl.SendChatMessageAction;
import com.tommustbe12.housing.actions.impl.SendToHubAction;
import com.tommustbe12.housing.actions.impl.ApplyPotionEffectAction;
import com.tommustbe12.housing.actions.impl.ConditionalAction;
import com.tommustbe12.housing.actions.ActionList;
import com.tommustbe12.housing.actions.placeholders.Placeholders;
import com.tommustbe12.housing.actions.placeholders.VariablesStore;
import com.tommustbe12.housing.houses.HouseManager;

import java.util.Map;

public final class SimpleActionCodec implements ActionCodec {
    private final Placeholders placeholders;
    private final VariablesStore variables;
    private final HouseManager houses;
    private final RunFunctionAction.FunctionRunner functionRunner;

    public SimpleActionCodec(Placeholders placeholders, VariablesStore variables, HouseManager houses, RunFunctionAction.FunctionRunner functionRunner) {
        this.placeholders = placeholders;
        this.variables = variables;
        this.houses = houses;
        this.functionRunner = functionRunner;
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
            case "give_exp_levels" -> new GiveExpLevelsAction(integer(map, "levels", 1));
            case "clear_potion_effects" -> new ClearPotionEffectsAction();
            case "apply_potion_effect" -> new ApplyPotionEffectAction(
                    string(map, "effect"),
                    integer(map, "durationTicks", 200),
                    integer(map, "amplifier", 0)
            );
            case "run_function" -> new RunFunctionAction(functionRunner, string(map, "name"), bool(map, "global", false));
            case "conditional" -> decodeConditional(map);
            default -> null;
        };
    }

    private Action decodeConditional(Map<?, ?> map) {
        String left = string(map, "left");
        String opRaw = string(map, "op").toUpperCase();
        String right = string(map, "right");
        ConditionalAction.Op op;
        try { op = ConditionalAction.Op.valueOf(opRaw); } catch (Exception e) { op = ConditionalAction.Op.EQ; }

        ActionList thenList = decodeNested(map.get("then"));
        ActionList elseList = decodeNested(map.get("else"));
        return new ConditionalAction(placeholders, left, op, right, thenList, elseList);
    }

    private ActionList decodeNested(Object raw) {
        ActionList list = new ActionList();
        if (!(raw instanceof java.util.List<?> arr)) return list;
        for (Object o : arr) {
            if (o instanceof Map<?, ?> m) {
                Action a = decode(m);
                if (a != null) list.actions().add(a);
            }
        }
        return list;
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

    private static boolean bool(Map<?, ?> map, String key, boolean def) {
        Object v = map.get(key);
        if (v == null) return def;
        return Boolean.parseBoolean(v.toString());
    }
}
