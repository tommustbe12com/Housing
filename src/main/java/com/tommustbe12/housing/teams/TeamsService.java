package com.tommustbe12.housing.teams;

import com.tommustbe12.housing.houses.HouseSlot;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TeamsService {
    private final TeamsStorage storage;
    private final ConcurrentHashMap<String, HouseTeamsData> cache = new ConcurrentHashMap<>();

    public TeamsService(Plugin plugin) {
        this.storage = new TeamsStorage(plugin);
    }

    public HouseTeamsData teams(UUID owner, HouseSlot slot) {
        return cache.computeIfAbsent(key(owner, slot), k -> storage.load(owner, slot));
    }

    public List<HouseTeam> list(UUID owner, HouseSlot slot) {
        return new ArrayList<>(teams(owner, slot).teams().values());
    }

    public void save(UUID owner, HouseSlot slot) {
        storage.save(owner, slot, teams(owner, slot));
    }

    public HouseTeam find(UUID owner, HouseSlot slot, UUID teamId) {
        if (teamId == null) return null;
        return teams(owner, slot).get(teamId);
    }

    public UUID teamIdFor(UUID owner, HouseSlot slot, UUID playerId) {
        return teams(owner, slot).teamForPlayer(playerId);
    }

    public HouseTeam teamFor(UUID owner, HouseSlot slot, UUID playerId) {
        UUID id = teamIdFor(owner, slot, playerId);
        return id == null ? null : find(owner, slot, id);
    }

    public void setPlayerTeam(UUID owner, HouseSlot slot, UUID playerId, UUID teamId) {
        HouseTeamsData data = teams(owner, slot);
        if (teamId != null && !data.teams().containsKey(teamId)) teamId = null;
        data.setPlayerTeam(playerId, teamId);
        save(owner, slot);
    }

    public String tagForDisplay(UUID owner, HouseSlot slot, UUID playerId) {
        HouseTeam t = teamFor(owner, slot, playerId);
        if (t == null) return "";
        String tag = t.tag();
        if (tag == null || tag.isBlank()) return "";
        ChatColor color = t.color() == null ? ChatColor.WHITE : t.color();
        return color + tag;
    }

    public static String defaultTagForName(String name) {
        String n = name == null ? "" : name.trim();
        if (n.isBlank()) return "";
        return "[" + n.toUpperCase(Locale.ROOT) + "]";
    }

    private static String key(UUID owner, HouseSlot slot) {
        return owner + ":" + slot.index();
    }

    public boolean isFriendlyFireAllowed(UUID owner, HouseSlot slot, Player a, Player b) {
        if (a == null || b == null) return true;
        HouseTeam ta = teamFor(owner, slot, a.getUniqueId());
        HouseTeam tb = teamFor(owner, slot, b.getUniqueId());
        if (ta == null || tb == null) return true;
        if (!ta.id().equals(tb.id())) return true;
        return ta.friendlyFire();
    }
}

