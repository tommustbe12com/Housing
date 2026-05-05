package com.tommustbe12.housing.tags;

import com.tommustbe12.housing.debug.Debug;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class OwnerTagService {
    private static final String TEAM_OWNER = "housing_owner";

    private final Plugin plugin;
    private final Debug debug;
    private final Scoreboard scoreboard;
    private final Team ownerTeam;
    private final ConcurrentHashMap<UUID, Original> originals = new ConcurrentHashMap<>();

    public OwnerTagService(Plugin plugin, Debug debug) {
        this.plugin = plugin;
        this.debug = debug;
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            throw new IllegalStateException("ScoreboardManager is not available.");
        }
        this.scoreboard = manager.getMainScoreboard();

        Team existing = scoreboard.getTeam(TEAM_OWNER);
        if (existing != null) {
            this.ownerTeam = existing;
        } else {
            this.ownerTeam = scoreboard.registerNewTeam(TEAM_OWNER);
            this.ownerTeam.setPrefix("§6[OWNER] §f");
            this.ownerTeam.setColor(ChatColor.GOLD);
            this.ownerTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
            this.ownerTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.ALWAYS);
        }
    }

    public void applyOwner(Player player, UUID houseOwner) {
        boolean isOwner = player.getUniqueId().equals(houseOwner);
        if (isOwner) {
            // Capture original names once per session before we mutate anything
            originals.putIfAbsent(player.getUniqueId(), new Original(player.getPlayerListName(), player.getDisplayName()));
            if (!ownerTeam.hasEntry(player.getName())) ownerTeam.addEntry(player.getName());
            player.setScoreboard(scoreboard);
            player.setPlayerListName(ownerTeam.getPrefix() + player.getName());
            player.setDisplayName(ownerTeam.getPrefix() + player.getName());
            debug.toOps("Owner tag applied to " + player.getName());
        } else {
            clear(player);
        }
    }

    public void clear(Player player) {
        if (ownerTeam.hasEntry(player.getName())) ownerTeam.removeEntry(player.getName());
        originals.remove(player.getUniqueId());
        // Always hard-reset to vanilla names to avoid sticky prefixes
        player.setPlayerListName(player.getName());
        player.setDisplayName(player.getName());
    }

    private record Original(String playerListName, String displayName) {
    }
}
