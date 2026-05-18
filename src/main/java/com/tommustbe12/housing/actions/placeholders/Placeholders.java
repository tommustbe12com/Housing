package com.tommustbe12.housing.actions.placeholders;

import com.tommustbe12.housing.actions.ActionContext;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.UUID;

public final class Placeholders {
    private final VariablesStore variables;

    public Placeholders(VariablesStore variables) {
        this.variables = variables;
    }

    public String resolve(ActionContext ctx, String input) {
        if (input == null) return "";
        String out = input;

        if (ctx.player() != null) {
            out = out.replace("%player.name%", ctx.player().getName());
            out = out.replace("%player.uuid%", ctx.player().getUniqueId().toString());
        }
        if (ctx.other() != null) {
            out = out.replace("%other.name%", ctx.other().getName());
            out = out.replace("%other.uuid%", ctx.other().getUniqueId().toString());
        }

        // Team placeholders (player only): %team.name% %team.tag% %team.color%
        if (ctx.player() != null) {
            out = out.replace("%team.name%", resolveTeamStat(ctx, ctx.player().getUniqueId(), "name"));
            out = out.replace("%team.tag%", resolveTeamStat(ctx, ctx.player().getUniqueId(), "tag"));
            out = out.replace("%team.color%", resolveTeamStat(ctx, ctx.player().getUniqueId(), "color"));
        }

        // %stat.key% or %stats.key%
        int guard = 0;
        while (guard++ < 100) {
            int startStats = out.indexOf("%stats.");
            int startStat = out.indexOf("%stat.");
            int start;
            String prefix;
            if (startStats == -1 && startStat == -1) break;
            if (startStats != -1 && (startStat == -1 || startStats < startStat)) {
                start = startStats;
                prefix = "%stats.";
            } else {
                start = startStat;
                prefix = "%stat.";
            }
            int end = out.indexOf('%', start + prefix.length());
            if (end == -1) break;
            String token = out.substring(start, end + 1);
            String key = token.substring(prefix.length(), token.length() - 1);
            String val;
            if ("cookies".equalsIgnoreCase(key) || "house".equalsIgnoreCase(key)) {
                val = resolveHouseStat(ctx, key);
            } else {
                UUID pid = ctx.player() == null ? null : ctx.player().getUniqueId();
                val = variables.get(ctx.houseOwner(), ctx.houseSlot(), pid, key);
                // If a stat is referenced but missing, initialize it to 0 so displays (scoreboards, holograms, etc)
                // don't show blank values.
                if (pid != null && (val == null || val.isBlank())) {
                    variables.set(ctx.houseOwner(), ctx.houseSlot(), pid, key, "0");
                    val = "0";
                }
            }
            out = out.replace(token, val == null ? "" : val);
        }

        return out;
    }

    private static String resolveHouseStat(ActionContext ctx, String key) {
        try {
            File housesDir = new File(ctx.plugin().getDataFolder(), "houses");
            File file = new File(housesDir, ctx.houseOwner() + "-" + ctx.houseSlot().index() + ".yml");
            if (!file.exists()) return "";
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            if ("cookies".equalsIgnoreCase(key)) {
                // New format: cookies.week + cookies.count (fallback to old cookies key)
                int count = yaml.getInt("cookies.count", yaml.getInt("cookies", 0));
                return Integer.toString(count);
            }
            if ("house".equalsIgnoreCase(key)) return yaml.getString("name", "");
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    private static String resolveTeamStat(ActionContext ctx, UUID playerId, String key) {
        try {
            File dir = new File(ctx.plugin().getDataFolder(), "teams");
            File file = new File(dir, ctx.houseOwner() + "-" + ctx.houseSlot().index() + ".yml");
            if (!file.exists()) return "";
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            boolean show = yaml.getBoolean("settings.showTagsEverywhere", true);
            if (!show) return "";
            String teamId = yaml.getString("players." + playerId, "");
            if (teamId == null || teamId.isBlank()) return "";
            String base = "teams." + teamId + ".";
            if ("name".equalsIgnoreCase(key)) return yaml.getString(base + "name", "");
            if ("tag".equalsIgnoreCase(key)) {
                String tag = yaml.getString(base + "tag", "");
                if (tag == null || tag.isBlank()) return "";
                String colorName = yaml.getString(base + "color", "WHITE");
                org.bukkit.ChatColor c;
                try { c = org.bukkit.ChatColor.valueOf(colorName); } catch (Exception e) { c = org.bukkit.ChatColor.WHITE; }
                return c + tag + org.bukkit.ChatColor.RESET;
            }
            if ("color".equalsIgnoreCase(key)) {
                String colorName = yaml.getString(base + "color", "WHITE");
                org.bukkit.ChatColor c;
                try { c = org.bukkit.ChatColor.valueOf(colorName); } catch (Exception e) { c = org.bukkit.ChatColor.WHITE; }
                // Return the actual color code (e.g. §c), not the enum name.
                return c.toString();
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }
}
