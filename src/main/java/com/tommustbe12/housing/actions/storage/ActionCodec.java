package com.tommustbe12.housing.actions.storage;

import com.tommustbe12.housing.actions.Action;

import java.util.Locale;
import java.util.Map;

public interface ActionCodec {
    Action decode(Map<?, ?> map);

    static String typeOf(Map<?, ?> map) {
        Object t = map.get("type");
        if (t == null) return null;
        return t.toString().toLowerCase(Locale.ROOT);
    }
}

