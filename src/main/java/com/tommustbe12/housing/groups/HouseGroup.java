package com.tommustbe12.housing.groups;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public final class HouseGroup {
    private final UUID id;
    private String name; // editor name
    private String tag; // chat/tab tag, supports & codes
    private int priority; // 1..N (higher = more power)
    private DefaultGameMode defaultGameMode;
    private final Map<HousePermission, Boolean> perms = new EnumMap<>(HousePermission.class);

    public HouseGroup(UUID id, String name, String tag, int priority, DefaultGameMode defaultGameMode) {
        this.id = id;
        this.name = name;
        this.tag = tag;
        this.priority = priority;
        this.defaultGameMode = defaultGameMode;
        for (HousePermission p : HousePermission.values()) perms.put(p, false);
    }

    public UUID id() { return id; }
    public String name() { return name; }
    public void setName(String name) { this.name = name; }
    public String tag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    public int priority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public DefaultGameMode defaultGameMode() { return defaultGameMode; }
    public void setDefaultGameMode(DefaultGameMode defaultGameMode) { this.defaultGameMode = defaultGameMode; }

    public boolean has(HousePermission perm) {
        return perms.getOrDefault(perm, false);
    }

    public void set(HousePermission perm, boolean enabled) {
        perms.put(perm, enabled);
    }

    public Map<HousePermission, Boolean> perms() {
        return perms;
    }
}

