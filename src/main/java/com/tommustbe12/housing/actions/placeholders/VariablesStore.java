package com.tommustbe12.housing.actions.placeholders;

import com.tommustbe12.housing.houses.HouseSlot;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public final class VariablesStore {
    private final Plugin plugin;
    private final File varsDir;

    public VariablesStore(Plugin plugin) {
        this.plugin = plugin;
        this.varsDir = new File(plugin.getDataFolder(), "variables");
        if (!varsDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            varsDir.mkdirs();
        }
    }

    public String get(UUID houseOwner, HouseSlot slot, UUID player, String key) {
        if (player == null) return "";
        File file = fileFor(houseOwner, slot);
        if (!file.exists()) return "";
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        return yaml.getString(path(player, key), "");
    }

    public void set(UUID houseOwner, HouseSlot slot, UUID player, String key, String value) {
        if (player == null) return;
        File file = fileFor(houseOwner, slot);
        YamlConfiguration yaml = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
        yaml.set(path(player, key), value);
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed saving variables: " + e.getMessage());
        }
    }

    private File fileFor(UUID owner, HouseSlot slot) {
        return new File(varsDir, owner + "-" + slot.index() + ".yml");
    }

    private static String path(UUID player, String key) {
        return "players." + player + "." + key;
    }
}
