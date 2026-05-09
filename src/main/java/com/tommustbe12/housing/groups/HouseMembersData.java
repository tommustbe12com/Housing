package com.tommustbe12.housing.groups;

import java.util.Map;
import java.util.UUID;

public record HouseMembersData(
        Map<UUID, UUID> groupByPlayer,
        Map<UUID, Boolean> muted,
        Map<UUID, Boolean> banned
) {
    public boolean isMuted(UUID player) {
        return muted.getOrDefault(player, false);
    }

    public boolean isBanned(UUID player) {
        return banned.getOrDefault(player, false);
    }
}

