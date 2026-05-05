package com.tommustbe12.housing.actions.storage;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.impl.ChangeVariableAction;
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
        }
        return out;
    }
}
