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
import com.tommustbe12.housing.actions.conditions.*;
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
        boolean matchAny = bool(map, "matchAny", false);
        java.util.List<Condition> conds = decodeConditions(map.get("conditions"));
        ActionList thenList = decodeNested(map.get("then"));
        ActionList elseList = decodeNested(map.get("else"));
        return new ConditionalAction(placeholders, conds, matchAny, thenList, elseList);
    }

    private java.util.List<Condition> decodeConditions(Object raw) {
        java.util.List<Condition> out = new java.util.ArrayList<>();
        if (!(raw instanceof java.util.List<?> list)) return out;
        for (Object o : list) {
            if (!(o instanceof Map<?, ?> m)) continue;
            String type = ActionCodec.typeOf(m);
            if (type == null) continue;
            switch (type) {
                case "required_group" -> out.add(new RequiredGroupCondition(placeholders, string(m, "group")));
                case "variable_requirement" -> out.add(new VariableRequirementCondition(placeholders, string(m, "key"),
                        parseCompare(string(m, "op")), string(m, "value")));
                case "has_potion_effect" -> out.add(new HasPotionEffectCondition(string(m, "effect")));
                case "player_sneaking" -> out.add(new PlayerSneakingCondition());
                case "player_flying" -> out.add(new PlayerFlyingCondition());
                case "required_gamemode" -> {
                    try { out.add(new RequiredGamemodeCondition(org.bukkit.GameMode.valueOf(string(m, "mode")))); } catch (Exception ignored) {}
                }
                case "player_health" -> out.add(new PlayerHealthCondition(parseCompare(string(m, "op")), doubleNum(m, "value", 0)));
                case "max_player_health" -> out.add(new MaxHealthCondition(parseCompare(string(m, "op")), doubleNum(m, "value", 0)));
                case "player_hunger" -> out.add(new PlayerHungerCondition(parseCompare(string(m, "op")), integer(m, "value", 0)));
                // has_item omitted from persistence for now (requires item serialization); GUI still supports it but won’t save across restart yet.
            }
        }
        return out;
    }

    private static CompareOp parseCompare(String raw) {
        try { return CompareOp.valueOf(raw.trim().toUpperCase()); } catch (Exception e) { return CompareOp.EQ; }
    }

    private static double doubleNum(Map<?, ?> map, String key, double def) {
        Object v = map.get(key);
        if (v == null) return def;
        try { return Double.parseDouble(v.toString()); } catch (Exception e) { return def; }
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
