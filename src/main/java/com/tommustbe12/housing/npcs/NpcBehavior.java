package com.tommustbe12.housing.npcs;

public enum NpcBehavior {
    STATIC,
    WALK,
    JUMP,
    WALK_JUMP;

    public static NpcBehavior next(NpcBehavior b) {
        NpcBehavior[] vals = values();
        return vals[(b.ordinal() + 1) % vals.length];
    }
}

