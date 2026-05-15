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
import com.tommustbe12.housing.actions.impl.ApplyInventoryLayoutAction;
import com.tommustbe12.housing.actions.impl.OpenCustomMenuAction;
import com.tommustbe12.housing.actions.impl.PlaySoundAction;
import com.tommustbe12.housing.actions.impl.ChangeTeamAction;
import com.tommustbe12.housing.actions.impl.ChangeGroupAction;
import com.tommustbe12.housing.actions.impl.PauseExecutionAction;
import com.tommustbe12.housing.actions.impl.TeleportPlayerAction;
import com.tommustbe12.housing.actions.impl.ChangeMaxHealthAction;
import com.tommustbe12.housing.actions.impl.ChangeHealthAction;
import com.tommustbe12.housing.actions.impl.ChangeHungerLevelAction;
import com.tommustbe12.housing.actions.impl.GiveItemAction;
import com.tommustbe12.housing.actions.impl.RemoveItemAction;
import com.tommustbe12.housing.actions.impl.SetGamemodeAction;
import com.tommustbe12.housing.actions.impl.SetCompassTargetAction;
import com.tommustbe12.housing.actions.ActionList;
import com.tommustbe12.housing.actions.conditions.*;
import com.tommustbe12.housing.util.ItemStackSerialization;
import com.tommustbe12.housing.actions.placeholders.Placeholders;
import com.tommustbe12.housing.actions.placeholders.VariablesStore;
import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.inventorylayouts.InventoryLayoutsService;
import com.tommustbe12.housing.custommenus.CustomMenusService;
import com.tommustbe12.housing.teams.TeamsService;
import com.tommustbe12.housing.groups.HouseGroupsService;
import com.tommustbe12.housing.util.ItemStackSerialization;

import java.util.Map;
import java.util.UUID;

public final class SimpleActionCodec implements ActionCodec {
    private final Placeholders placeholders;
    private final VariablesStore variables;
    private final HouseManager houses;
    private final RunFunctionAction.FunctionRunner functionRunner;
    private final InventoryLayoutsService inventoryLayouts;
    private final CustomMenusService customMenus;
    private final TeamsService teams;
    private final HouseGroupsService groups;

    public SimpleActionCodec(Placeholders placeholders, VariablesStore variables, HouseManager houses, RunFunctionAction.FunctionRunner functionRunner, InventoryLayoutsService inventoryLayouts, CustomMenusService customMenus, TeamsService teams, HouseGroupsService groups) {
        this.placeholders = placeholders;
        this.variables = variables;
        this.houses = houses;
        this.functionRunner = functionRunner;
        this.inventoryLayouts = inventoryLayouts;
        this.customMenus = customMenus;
        this.teams = teams;
        this.groups = groups;
    }

    @Override
    public Action decode(Map<?, ?> map) {
        String type = ActionCodec.typeOf(map);
        if (type == null) return null;
        return switch (type) {
            case "chat_message" -> new SendChatMessageAction(placeholders, string(map, "message"));
            case "send_to_hub" -> new SendToHubAction(houses);
            case "change_variable" -> new ChangeVariableAction(variables, placeholders, string(map, "key"), string(map, "value"),
                    parseOp(string(map, "op")));
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
            case "apply_inventory_layout" -> {
                UUID id = null;
                try { id = UUID.fromString(string(map, "layoutId")); } catch (Exception ignored) {}
                yield new ApplyInventoryLayoutAction(inventoryLayouts, id);
            }
            case "play_sound" -> new PlaySoundAction(string(map, "sound"), (float) doubleNum(map, "volume", 1.0), (float) doubleNum(map, "pitch", 1.0));
            case "open_custom_menu" -> {
                UUID id = null;
                try { id = UUID.fromString(string(map, "menuId")); } catch (Exception ignored) {}
                yield new OpenCustomMenuAction(customMenus, id);
            }
            case "change_team" -> {
                UUID id = null;
                try { id = UUID.fromString(string(map, "teamId")); } catch (Exception ignored) {}
                yield new ChangeTeamAction(teams, id);
            }
            case "change_group" -> {
                UUID id = null;
                try { id = UUID.fromString(string(map, "groupId")); } catch (Exception ignored) {}
                yield new ChangeGroupAction(groups, id);
            }
            case "pause_execution" -> new PauseExecutionAction((long) doubleNum(map, "ticks", 20));
            case "teleport_player" -> {
                TeleportPlayerAction.Mode mode = TeleportPlayerAction.Mode.HOUSE_SPAWN;
                try { mode = TeleportPlayerAction.Mode.valueOf(string(map, "mode")); } catch (Exception ignored) {}
                yield new TeleportPlayerAction(houses, mode,
                        doubleNum(map, "x", 0), doubleNum(map, "y", 0), doubleNum(map, "z", 0),
                        (float) doubleNum(map, "yaw", 0), (float) doubleNum(map, "pitch", 0));
            }
            case "change_max_health" -> new ChangeMaxHealthAction(doubleNum(map, "maxHealth", 20));
            case "change_health" -> new ChangeHealthAction(doubleNum(map, "health", 20));
            case "change_hunger" -> new ChangeHungerLevelAction(integer(map, "food", 20));
            case "give_item" -> {
                String b64 = string(map, "item");
                var item = b64 == null || b64.isBlank() ? null : ItemStackSerialization.fromBase64(b64);
                int slot = integer(map, "slot", -1);
                yield new GiveItemAction(item, integer(map, "amount", 1), slot < 0 ? null : slot, bool(map, "replaceSlot", false));
            }
            case "remove_item" -> {
                String b64 = string(map, "item");
                var item = b64 == null || b64.isBlank() ? null : ItemStackSerialization.fromBase64(b64);
                yield new RemoveItemAction(item, integer(map, "amount", 1));
            }
            case "set_gamemode" -> {
                org.bukkit.GameMode mode;
                try { mode = org.bukkit.GameMode.valueOf(string(map, "mode")); } catch (Exception e) { mode = org.bukkit.GameMode.ADVENTURE; }
                yield new SetGamemodeAction(groups, mode);
            }
            case "set_compass_target" -> {
                SetCompassTargetAction.Direction d;
                try { d = SetCompassTargetAction.Direction.valueOf(string(map, "dir")); } catch (Exception e) { d = SetCompassTargetAction.Direction.N; }
                yield new SetCompassTargetAction(d);
            }
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
                case "has_item" -> out.add(new HasItemCondition(ItemStackSerialization.fromBase64(string(m, "item"))));
            }
        }
        return out;
    }

    private static CompareOp parseCompare(String raw) {
        try { return CompareOp.valueOf(raw.trim().toUpperCase()); } catch (Exception e) { return CompareOp.EQ; }
    }

    private static ChangeVariableAction.Operation parseOp(String raw) {
        try { return ChangeVariableAction.Operation.valueOf(raw.trim().toUpperCase()); } catch (Exception e) { return ChangeVariableAction.Operation.SET; }
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
