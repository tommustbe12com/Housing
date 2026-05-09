package com.tommustbe12.housing.groups;

import com.tommustbe12.housing.houses.HouseSlot;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class HouseMembersStore {
    private final Plugin plugin;
    private final File dir;

    public HouseMembersStore(Plugin plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "members");
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
    }

    public HouseMembersData load(UUID owner, HouseSlot slot) {
        File file = file(owner, slot);
        if (!file.exists()) return new HouseMembersData(new HashMap<>(), new HashMap<>(), new HashMap<>());
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        Map<UUID, UUID> groupByPlayer = new HashMap<>();
        var gsec = yaml.getConfigurationSection("groups");
        if (gsec != null) {
            for (String key : gsec.getKeys(false)) {
                try {
                    UUID player = UUID.fromString(key);
                    UUID group = UUID.fromString(gsec.getString(key, ""));
                    groupByPlayer.put(player, group);
                } catch (Exception ignored) {
                }
            }
        }

        Map<UUID, Boolean> muted = new HashMap<>();
        var msec = yaml.getConfigurationSection("muted");
        if (msec != null) {
            for (String key : msec.getKeys(false)) {
                try {
                    muted.put(UUID.fromString(key), msec.getBoolean(key, false));
                } catch (Exception ignored) {
                }
            }
        }

        Map<UUID, Boolean> banned = new HashMap<>();
        var bsec = yaml.getConfigurationSection("banned");
        if (bsec != null) {
            for (String key : bsec.getKeys(false)) {
                try {
                    banned.put(UUID.fromString(key), bsec.getBoolean(key, false));
                } catch (Exception ignored) {
                }
            }
        }

        return new HouseMembersData(groupByPlayer, muted, banned);
    }

    public void save(UUID owner, HouseSlot slot, HouseMembersData data) {
        File file = file(owner, slot);
        YamlConfiguration yaml = new YamlConfiguration();
        for (var e : data.groupByPlayer().entrySet()) {
            yaml.set("groups." + e.getKey(), e.getValue().toString());
        }
        for (var e : data.muted().entrySet()) {
            yaml.set("muted." + e.getKey(), e.getValue());
        }
        for (var e : data.banned().entrySet()) {
            yaml.set("banned." + e.getKey(), e.getValue());
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed saving members: " + e.getMessage());
        }
    }

    private File file(UUID owner, HouseSlot slot) {
        return new File(dir, owner + "-" + slot.index() + ".yml");
    }
}

