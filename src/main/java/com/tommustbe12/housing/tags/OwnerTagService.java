package com.tommustbe12.housing.tags;

import com.tommustbe12.housing.debug.Debug;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Legacy name: applies group tags (including Owner) to nametag + tab + display.
 */
public final class OwnerTagService {
    private static final String TEAM_PREFIX = "housing_g_";

    private final Plugin plugin;
    private final Debug debug;

    private final ConcurrentHashMap<UUID, Original> originals = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> playerTeam = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> viewerTargetTeam = new ConcurrentHashMap<>();

    public OwnerTagService(Plugin plugin, Debug debug) {
        this.plugin = plugin;
        this.debug = debug;
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) throw new IllegalStateException("ScoreboardManager is not available.");
    }

    public void applyOwner(Player player, UUID houseOwner) {
        boolean isOwner = player.getUniqueId().equals(houseOwner);
        applyTag(player, isOwner ? "§6[OWNER]" : "");
    }

    public void applyTag(Player player, String rawTag) {
        String tag = rawTag == null ? "" : rawTag.trim();

        originals.putIfAbsent(player.getUniqueId(), new Original(player.getPlayerListName(), player.getDisplayName()));

        // Update tab+display for the player themselves.
        player.setPlayerListName((tag.isBlank() ? "" : (tag + " §f")) + player.getName());
        player.setDisplayName((tag.isBlank() ? "" : (tag + " §f")) + player.getName());

        // Apply nametag prefixes for *all viewers in the same world* (viewer scoreboards drive nametag rendering).
        for (Player viewer : player.getWorld().getPlayers()) {
            setViewerTag(viewer, player, tag);
        }
    }

    public void clear(Player player) {
        for (Player viewer : player.getWorld().getPlayers()) {
            setViewerTag(viewer, player, "");
        }
        originals.remove(player.getUniqueId());
        player.setPlayerListName(player.getName());
        player.setDisplayName(player.getName());
    }

    private void setViewerTag(Player viewer, Player target, String tag) {
        Scoreboard scoreboard = viewer.getScoreboard();
        if (scoreboard == null) return;
        String key = viewer.getUniqueId() + ":" + target.getUniqueId();

        String prevTeam = viewerTargetTeam.remove(key);
        if (prevTeam != null) {
            Team t = scoreboard.getTeam(prevTeam);
            if (t != null && t.hasEntry(target.getName())) t.removeEntry(target.getName());
        }

        if (tag == null || tag.isBlank()) return;

        String teamName = teamNameForTag(tag);
        Team team = scoreboard.getTeam(teamName);
        if (team == null) team = scoreboard.registerNewTeam(teamName);

        team.setPrefix(tag + " §f");
        team.setColor(ChatColor.WHITE);
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.ALWAYS);

        if (!team.hasEntry(target.getName())) team.addEntry(target.getName());
        viewerTargetTeam.put(key, teamName);
    }

    private static String teamNameForTag(String tag) {
        // Scoreboard team name max is 16 chars; keep it stable.
        byte[] bytes = tag.getBytes(StandardCharsets.UTF_8);
        String b64 = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String shortHash = b64.length() > 10 ? b64.substring(0, 10) : b64;
        return TEAM_PREFIX + shortHash;
    }

    private record Original(String playerListName, String displayName) {}
}
