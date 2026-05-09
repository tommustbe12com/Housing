package com.tommustbe12.housing.groups;

import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.houses.HouseSlot;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class HouseGroupsService {
    private final Plugin plugin;
    private final HouseManager houses;
    private final HouseGroupsStore groupsStore;
    private final HouseMembersStore membersStore;

    private final Map<String, HouseGroupsData> groupsCache = new ConcurrentHashMap<>();
    private final Map<String, HouseMembersData> membersCache = new ConcurrentHashMap<>();

    public HouseGroupsService(Plugin plugin, HouseManager houses) {
        this.plugin = plugin;
        this.houses = houses;
        this.groupsStore = new HouseGroupsStore(plugin);
        this.membersStore = new HouseMembersStore(plugin);
    }

    public HouseGroupsData groups(UUID owner, HouseSlot slot) {
        return groupsCache.computeIfAbsent(key(owner, slot), k -> groupsStore.load(owner, slot));
    }

    public HouseMembersData members(UUID owner, HouseSlot slot) {
        return membersCache.computeIfAbsent(key(owner, slot), k -> membersStore.load(owner, slot));
    }

    public void save(UUID owner, HouseSlot slot) {
        HouseGroupsData g = groups(owner, slot);
        groupsStore.save(owner, slot, g);
        HouseMembersData m = members(owner, slot);
        membersStore.save(owner, slot, m);
    }

    public UUID groupIdFor(UUID owner, HouseSlot slot, UUID player) {
        if (player == null) return null;
        if (player.equals(owner)) return groups(owner, slot).ownerId();
        HouseMembersData m = members(owner, slot);
        UUID g = m.groupByPlayer().get(player);
        if (g != null && groups(owner, slot).groups().containsKey(g)) return g;
        return groups(owner, slot).defaultGroupId();
    }

    public HouseGroup groupFor(UUID owner, HouseSlot slot, UUID player) {
        UUID gid = groupIdFor(owner, slot, player);
        return groups(owner, slot).get(gid);
    }

    public boolean has(UUID owner, HouseSlot slot, UUID player, HousePermission perm) {
        if (player == null) return false;
        if (player.equals(owner)) return true;
        HouseGroup g = groupFor(owner, slot, player);
        return g != null && g.has(perm);
    }

    public boolean isMuted(UUID owner, HouseSlot slot, UUID player) {
        if (player == null) return false;
        if (player.equals(owner)) return false;
        return members(owner, slot).isMuted(player);
    }

    public boolean isBanned(UUID owner, HouseSlot slot, UUID player) {
        if (player == null) return false;
        if (player.equals(owner)) return false;
        return members(owner, slot).isBanned(player);
    }

    public void setMuted(UUID owner, HouseSlot slot, UUID player, boolean muted) {
        if (player == null || player.equals(owner)) return;
        members(owner, slot).muted().put(player, muted);
        save(owner, slot);
    }

    public void setBanned(UUID owner, HouseSlot slot, UUID player, boolean banned) {
        if (player == null || player.equals(owner)) return;
        members(owner, slot).banned().put(player, banned);
        save(owner, slot);
    }

    public void setGroup(UUID owner, HouseSlot slot, UUID player, UUID groupId) {
        if (player == null || player.equals(owner)) return;
        if (groupId == null) {
            members(owner, slot).groupByPlayer().remove(player);
        } else {
            members(owner, slot).groupByPlayer().put(player, groupId);
        }
        save(owner, slot);
    }

    public boolean canAssignGroup(UUID owner, HouseSlot slot, Player actor, UUID targetGroupId) {
        if (actor == null) return false;
        if (actor.getUniqueId().equals(owner)) return true;
        if (!has(owner, slot, actor.getUniqueId(), HousePermission.CHANGE_GROUP)) return false;

        HouseGroupsData data = groups(owner, slot);
        // Owner group is reserved for the actual house owner and cannot be assigned.
        if (targetGroupId != null && targetGroupId.equals(data.ownerId())) return false;
        HouseGroup actorGroup = groupFor(owner, slot, actor.getUniqueId());
        HouseGroup target = data.get(targetGroupId);
        if (actorGroup == null || target == null) return false;

        // Can only assign groups strictly below your own priority.
        return target.priority() < actorGroup.priority();
    }

    public void applyDefaultModeIfNeeded(Player player) {
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null) return;
        if (player.getUniqueId().equals(info.owner())) return;
        HouseGroup g = groupFor(info.owner(), info.slot(), player.getUniqueId());
        if (g == null) return;
        DefaultGameMode mode = g.defaultGameMode();
        if (mode == DefaultGameMode.CREATIVE && !has(info.owner(), info.slot(), player.getUniqueId(), HousePermission.BUILD)) {
            mode = DefaultGameMode.ADVENTURE;
        }
        GameMode gm = mode.bukkit();
        player.setGameMode(gm);
    }

    public String tagForDisplay(UUID owner, HouseSlot slot, UUID player) {
        if (player == null) return "";
        // Owner's effective group tag should still come from the Owner group tag if present.
        if (player.equals(owner)) {
            HouseGroup og = groups(owner, slot).get(groups(owner, slot).ownerId());
            if (og != null && og.tag() != null && !og.tag().isBlank()) return colorize(og.tag());
            return colorize("&6[OWNER]");
        }
        HouseGroup g = groupFor(owner, slot, player);
        if (g == null) return colorize("&7[VISITOR]");
        return colorize(g.tag());
    }

    private static String key(UUID owner, HouseSlot slot) {
        return owner + ":" + slot.index();
    }

    private static String colorize(String s) {
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }
}
