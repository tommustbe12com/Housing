package com.tommustbe12.housing.actions.storage;

import com.tommustbe12.housing.actions.Action;

import java.util.Map;

public interface ActionSerializer {
    Map<String, Object> serialize(Action action);
}

