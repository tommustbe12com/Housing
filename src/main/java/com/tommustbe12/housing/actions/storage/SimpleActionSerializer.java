package com.tommustbe12.housing.actions.storage;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.impl.ApplyPotionEffectAction;
import com.tommustbe12.housing.actions.impl.ChangeVariableAction;
import com.tommustbe12.housing.actions.impl.GiveExpLevelsAction;
import com.tommustbe12.housing.actions.impl.RunFunctionAction;
import com.tommustbe12.housing.actions.impl.ConditionalAction;
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
        } else if (action instanceof ConditionalAction cond) {
            out.put("left", cond.left());
            out.put("op", cond.op().name());
            out.put("right", cond.right());
            out.put("then", cond.thenList().actions().stream().map(this::serialize).toList());
            out.put("else", cond.elseList() == null ? java.util.List.of() : cond.elseList().actions().stream().map(this::serialize).toList());
        }
        return out;
    }
}
