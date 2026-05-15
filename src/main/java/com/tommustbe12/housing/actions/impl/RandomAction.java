package com.tommustbe12.housing.actions.impl;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.ActionContext;

import java.util.List;
import java.util.Random;

/**
 * Minimal random action selector.
 * If types list is empty, runs a safe default random action.
 */
public final class RandomAction implements Action {
    private static final Random RNG = new Random();
    private final List<String> types;

    public RandomAction(List<String> types) {
        this.types = types == null ? List.of() : List.copyOf(types);
    }

    public List<String> types() { return types; }

    @Override
    public String type() {
        return "random_action";
    }

    @Override
    public void execute(ActionContext ctx) {
        if (ctx == null) return;
        String picked;
        if (types.isEmpty()) {
            String[] defaults = new String[]{"full_heal", "give_exp_levels"};
            picked = defaults[RNG.nextInt(defaults.length)];
        } else {
            picked = types.get(RNG.nextInt(types.size()));
        }

        Action a = switch (picked) {
            case "full_heal" -> new FullHealAction();
            case "give_exp_levels" -> new GiveExpLevelsAction(1);
            case "clear_potion_effects" -> new ClearPotionEffectsAction();
            default -> null;
        };
        if (a != null) a.execute(ctx);
    }
}

