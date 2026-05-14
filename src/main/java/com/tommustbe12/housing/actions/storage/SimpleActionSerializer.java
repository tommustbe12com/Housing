package com.tommustbe12.housing.actions.storage;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.impl.ApplyPotionEffectAction;
import com.tommustbe12.housing.actions.impl.ApplyInventoryLayoutAction;
import com.tommustbe12.housing.actions.impl.OpenCustomMenuAction;
import com.tommustbe12.housing.actions.impl.PlaySoundAction;
import com.tommustbe12.housing.actions.impl.ChangeVariableAction;
import com.tommustbe12.housing.actions.impl.ChangeTeamAction;
import com.tommustbe12.housing.actions.impl.GiveExpLevelsAction;
import com.tommustbe12.housing.actions.impl.RunFunctionAction;
import com.tommustbe12.housing.actions.impl.ConditionalAction;
import com.tommustbe12.housing.actions.conditions.*;
import com.tommustbe12.housing.util.ItemStackSerialization;
import com.tommustbe12.housing.actions.impl.DisplayActionBarAction;
import com.tommustbe12.housing.actions.impl.DisplayTitleAction;
import com.tommustbe12.housing.actions.impl.SendChatMessageAction;

import java.util.HashMap;
import java.util.Map;

public final class SimpleActionSerializer implements ActionSerializer {
    @Override
    public Map<String, Object> serialize(Action action) {
        Map<String, Object> out = new HashMap<>();
        out.put("type", action.type());

        if (action instanceof SendChatMessageAction msg) {
            out.put("message", msg.message());
        } else if (action instanceof ChangeVariableAction var) {
            out.put("key", var.key());
            out.put("value", var.value());
            out.put("op", var.operation().name());
        } else if (action instanceof DisplayActionBarAction bar) {
            out.put("message", bar.message());
        } else if (action instanceof DisplayTitleAction title) {
            out.put("title", title.title());
            out.put("subtitle", title.subtitle());
            out.put("fadeIn", title.fadeIn());
            out.put("stay", title.stay());
            out.put("fadeOut", title.fadeOut());
        } else if (action instanceof GiveExpLevelsAction exp) {
            out.put("levels", exp.levels());
        } else if (action instanceof ApplyPotionEffectAction pot) {
            out.put("effect", pot.effect());
            out.put("durationTicks", pot.durationTicks());
            out.put("amplifier", pot.amplifier());
        } else if (action instanceof RunFunctionAction fn) {
            out.put("name", fn.functionName());
            out.put("global", fn.global());
        } else if (action instanceof ApplyInventoryLayoutAction inv) {
            out.put("layoutId", inv.layoutId() == null ? "" : inv.layoutId().toString());
        } else if (action instanceof PlaySoundAction sound) {
            out.put("sound", sound.sound());
            out.put("volume", sound.volume());
            out.put("pitch", sound.pitch());
        } else if (action instanceof OpenCustomMenuAction menu) {
            out.put("menuId", menu.menuId() == null ? "" : menu.menuId().toString());
        } else if (action instanceof ChangeTeamAction team) {
            out.put("teamId", team.teamId() == null ? "" : team.teamId().toString());
        } else if (action instanceof ConditionalAction cond) {
            out.put("matchAny", cond.matchAny());
            out.put("conditions", cond.conditions().stream().map(SimpleActionSerializer::serializeCondition).toList());
            out.put("then", cond.thenList().actions().stream().map(this::serialize).toList());
            out.put("else", cond.elseList() == null ? java.util.List.of() : cond.elseList().actions().stream().map(this::serialize).toList());
        }
        return out;
    }

    private static java.util.Map<String, Object> serializeCondition(Condition c) {
        java.util.Map<String, Object> out = new java.util.HashMap<>();
        out.put("type", c.type());
        if (c instanceof RequiredGroupCondition g) out.put("group", g.requiredGroup());
        if (c instanceof VariableRequirementCondition v) {
            out.put("key", v.key());
            out.put("op", v.op().name());
            out.put("value", v.value());
        }
        if (c instanceof HasPotionEffectCondition p) out.put("effect", p.effect());
        if (c instanceof RequiredGamemodeCondition gm) out.put("mode", gm.mode().name());
        if (c instanceof PlayerHealthCondition ph) { out.put("op", ph.op().name()); out.put("value", ph.value()); }
        if (c instanceof MaxHealthCondition mh) { out.put("op", mh.op().name()); out.put("value", mh.value()); }
        if (c instanceof PlayerHungerCondition hg) { out.put("op", hg.op().name()); out.put("value", hg.value()); }
        if (c instanceof HasItemCondition hi) out.put("item", ItemStackSerialization.toBase64(hi.item()));
        return out;
    }
}
