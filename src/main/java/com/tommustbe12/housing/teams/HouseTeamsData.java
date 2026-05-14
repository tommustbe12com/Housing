package com.tommustbe12.housing.teams;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class HouseTeamsData {
    private final Map<UUID, HouseTeam> teams = new LinkedHashMap<>();
    private final Map<UUID, UUID> playerTeams = new LinkedHashMap<>();

    public Map<UUID, HouseTeam> teams() { return teams; }
    public Map<UUID, UUID> playerTeams() { return playerTeams; }

    public HouseTeam get(UUID id) { return teams.get(id); }

    public UUID teamForPlayer(UUID playerId) {
        if (playerId == null) return null;
        return playerTeams.get(playerId);
    }

    public void setPlayerTeam(UUID playerId, UUID teamId) {
        if (playerId == null) return;
        if (teamId == null) playerTeams.remove(playerId);
        else playerTeams.put(playerId, teamId);
    }
}

