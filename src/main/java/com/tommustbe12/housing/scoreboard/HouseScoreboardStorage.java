package com.tommustbe12.housing.scoreboard;

import com.tommustbe12.housing.houses.HouseSlot;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class HouseScoreboardStorage {
    private final Plugin plugin;
    private final File dir;

    public HouseScoreboardStorage(Plugin plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "scoreboards");
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
    }

    public List<String> load(UUID owner, HouseSlot slot) {
        File file = fileFor(owner, slot);
        if (!file.exists()) return defaultLines();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        List<String> lines = yaml.getStringList("lines");
        if (lines == null || lines.isEmpty()) return defaultLines();
        return new ArrayList<>(lines);
    }

    public void save(UUID owner, HouseSlot slot, List<String> lines) {
        File file = fileFor(owner, slot);
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("lines", lines);
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed saving scoreboard: " + e.getMessage());
        }
    }

    private File fileFor(UUID owner, HouseSlot slot) {
        return new File(dir, owner + "-" + slot.index() + ".yml");
    }

    private static List<String> defaultLines() {
        return List.of("&7Welcome!", "&f%player.name%", "&6Cookies: &f%stat.cookies%");
    }
}

