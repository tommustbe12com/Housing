package com.tommustbe12.housing.actions.impl;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.ActionContext;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class ApplyPotionEffectAction implements Action {
    private final String effect;
    private final int durationTicks;
    private final int amplifier;

    public ApplyPotionEffectAction(String effect, int durationTicks, int amplifier) {
        this.effect = effect;
        this.durationTicks = durationTicks;
        this.amplifier = amplifier;
    }

    @Override
    public String type() {
        return "apply_potion_effect";
    }

    @Override
    public void execute(ActionContext ctx) {
        if (ctx.player() == null) return;
        PotionEffectType type = PotionEffectType.getByName(effect.toUpperCase());
        if (type == null) return;
        ctx.player().addPotionEffect(new PotionEffect(type, durationTicks, amplifier));
    }

    public String effect() { return effect; }
    public int durationTicks() { return durationTicks; }
    public int amplifier() { return amplifier; }
}

