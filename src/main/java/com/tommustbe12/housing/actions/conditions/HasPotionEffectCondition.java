package com.tommustbe12.housing.actions.conditions;

import com.tommustbe12.housing.actions.ActionContext;
import org.bukkit.potion.PotionEffectType;

public final class HasPotionEffectCondition implements Condition {
    private final String effect;

    public HasPotionEffectCondition(String effect) {
        this.effect = effect;
    }

    @Override
    public String type() {
        return "has_potion_effect";
    }

    @Override
    public boolean test(ActionContext ctx) {
        if (ctx.player() == null) return false;
        PotionEffectType type = PotionEffectType.getByName(effect.toUpperCase());
        if (type == null) return false;
        return ctx.player().hasPotionEffect(type);
    }

    public String effect() { return effect; }
}

