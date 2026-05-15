package com.tommustbe12.housing.teams;

import com.tommustbe12.housing.houses.HouseSlot;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public final class TeamsStorage {
    private final Plugin plugin;
    private final File dir;

    public TeamsStorage(Plugin plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "teams");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
    }

    public HouseTeamsData load(UUID owner, HouseSlot slot) {
        File file = fileFor(owner, slot);
        HouseTeamsData out = new HouseTeamsData();
        if (!file.exists()) return out;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        out.setShowTagsEverywhere(yaml.getBoolean("settings.showTagsEverywhere", true));

        ConfigurationSection teamsSec = yaml.getConfigurationSection("teams");
        if (teamsSec != null) {
            for (String key : teamsSec.getKeys(false)) {
                try {
                    UUID id = UUID.fromString(key);
                    ConfigurationSection t = teamsSec.getConfigurationSection(key);
                    if (t == null) continue;
                    HouseTeam team = new HouseTeam(id, t.getString("name", "Team"));
                    team.setTag(t.getString("tag", ""));
                    try { team.setColor(ChatColor.valueOf(t.getString("color", "WHITE"))); } catch (Exception ignored) {}
                    team.setFriendlyFire(t.getBoolean("friendlyFire", false));
                    out.teams().put(id, team);
                } catch (Exception ignored) {
                }
            }
        }

        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players != null) {
            for (String key : players.getKeys(false)) {
                try {
                    UUID playerId = UUID.fromString(key);
                    String teamIdRaw = players.getString(key, "");
                    if (teamIdRaw == null || teamIdRaw.isBlank()) continue;
                    UUID teamId = UUID.fromString(teamIdRaw);
                    out.playerTeams().put(playerId, teamId);
                } catch (Exception ignored) {
                }
            }
        }

        return out;
    }

    public void save(UUID owner, HouseSlot slot, HouseTeamsData data) {
        File file = fileFor(owner, slot);
        YamlConfiguration yaml = new YamlConfiguration();

        yaml.set("settings.showTagsEverywhere", data.showTagsEverywhere());

        ConfigurationSection teamsSec = yaml.createSection("teams");
        for (HouseTeam team : data.teams().values()) {
            if (team == null) continue;
            ConfigurationSection t = teamsSec.createSection(team.id().toString());
            t.set("name", team.name());
            t.set("tag", team.tag());
            t.set("color", team.color().name());
            t.set("friendlyFire", team.friendlyFire());
        }

        ConfigurationSection players = yaml.createSection("players");
        for (Map.Entry<UUID, UUID> e : data.playerTeams().entrySet()) {
            if (e.getKey() == null) continue;
            players.set(e.getKey().toString(), e.getValue() == null ? "" : e.getValue().toString());
        }

        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed saving " + file.getName() + ": " + e.getMessage());
        }
    }

    private File fileFor(UUID owner, HouseSlot slot) {
        return new File(dir, owner + "-" + slot.index() + ".yml");
    }
}
