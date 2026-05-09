package com.tommustbe12.housing.scoreboard;

import com.tommustbe12.housing.actions.ActionContext;
import com.tommustbe12.housing.actions.placeholders.Placeholders;
import com.tommustbe12.housing.actions.placeholders.VariablesStore;
import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.houses.HouseSlot;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HouseScoreboardService {
    private final Plugin plugin;
    private final HouseManager houses;
    private final HouseScoreboardStorage storage;
    private final Placeholders placeholders;

    private final Map<UUID, BukkitTask> tasks = new ConcurrentHashMap<>();
    private final Map<UUID, Scoreboard> personalBoards = new ConcurrentHashMap<>();

    public HouseScoreboardService(Plugin plugin, HouseManager houses) {
        this.plugin = plugin;
        this.houses = houses;
        this.storage = new HouseScoreboardStorage(plugin);
        VariablesStore vars = new VariablesStore(plugin);
        this.placeholders = new Placeholders(vars);
    }

    public void start(Player player, UUID owner, HouseSlot slot) {
        stop(player);

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            Scoreboard personal = manager.getNewScoreboard();
            personalBoards.put(player.getUniqueId(), personal);
            player.setScoreboard(personal);
        }

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> apply(player, owner, slot), 1L, 20L);
        tasks.put(player.getUniqueId(), task);
    }

    public void stop(Player player) {
        BukkitTask task = tasks.remove(player.getUniqueId());
        if (task != null) task.cancel();
        personalBoards.remove(player.getUniqueId());
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) player.setScoreboard(manager.getNewScoreboard());
    }

    private void apply(Player player, UUID owner, HouseSlot slot) {
        if (!player.isOnline()) {
            stop(player);
            return;
        }
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null) {
            stop(player);
            return;
        }

        Scoreboard sb = personalBoards.get(player.getUniqueId());
        if (sb == null) return;

        Objective existing = sb.getObjective("housing");
        if (existing != null) try { existing.unregister(); } catch (Exception ignored) {}
        Objective obj = sb.registerNewObjective("housing", "dummy", ChatColor.translateAlternateColorCodes('&', "&b&lHousing"));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        List<String> lines = storage.load(owner, slot);
        ActionContext ctx = new ActionContext(plugin, null, owner, slot, player.getWorld(), player, null, player.getLocation());

        int score = lines.size();
        Map<String, Integer> used = new HashMap<>();
        for (String line : lines) {
            String resolved = placeholders.resolve(ctx, line);
            String colored = ChatColor.translateAlternateColorCodes('&', resolved);
            String entry = unique(colored, used);
            obj.getScore(entry).setScore(score--);
            if (score <= 0) break;
        }

        // Don't overwrite the scoreboard reference; we updated the player's personal board in-place.
    }

    private static String unique(String base, Map<String, Integer> used) {
        if (base.length() > 40) base = base.substring(0, 40);
        int n = used.getOrDefault(base, 0);
        used.put(base, n + 1);
        if (n == 0) return base;
        String suffix = ChatColor.COLOR_CHAR + Integer.toHexString(n % 16);
        String trimmed = base;
        if (trimmed.length() + suffix.length() > 40) trimmed = trimmed.substring(0, 40 - suffix.length());
        return trimmed + suffix;
    }

    public HouseScoreboardStorage storage() {
        return storage;
    }
}
