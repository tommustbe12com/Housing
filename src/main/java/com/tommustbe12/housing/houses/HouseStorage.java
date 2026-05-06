package com.tommustbe12.housing.houses;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public final class HouseStorage {
    private final Plugin plugin;
    private final File housesDir;

    public HouseStorage(Plugin plugin) {
        this.plugin = plugin;
        this.housesDir = new File(plugin.getDataFolder(), "houses");
        if (!housesDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            housesDir.mkdirs();
        }
    }

    public HouseData load(UUID owner, HouseSlot slot) {
        File file = fileFor(owner, slot);
        HouseData data = new HouseData(owner, slot);
        if (!file.exists()) return data;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        data.setName(yaml.getString("name", data.name()));
        data.setMaxPlayers(yaml.getInt("maxPlayers", data.maxPlayers()));
        data.setTimeOfDay(yaml.getLong("timeOfDay", data.timeOfDay()));
        int cookies = yaml.getInt("cookies", 0);
        if (cookies != 0) data.addCookies(cookies);
        data.setIconMaterial(yaml.getString("icon", data.iconMaterial()));

        if (yaml.getBoolean("spawn.set", false)) {
            double x = yaml.getDouble("spawn.x");
            double y = yaml.getDouble("spawn.y");
            double z = yaml.getDouble("spawn.z");
            float yaw = (float) yaml.getDouble("spawn.yaw");
            float pitch = (float) yaml.getDouble("spawn.pitch");
            data.setSpawn(new org.bukkit.Location(plugin.getServer().getWorlds().getFirst(), x, y, z, yaw, pitch));
        }

        return data;
    }

    public void save(HouseData data) {
        File file = fileFor(data.owner(), data.slot());
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("name", data.name());
        yaml.set("maxPlayers", data.maxPlayers());
        yaml.set("timeOfDay", data.timeOfDay());
        yaml.set("cookies", data.cookies());
        yaml.set("icon", data.iconMaterial());
        yaml.set("spawn.set", data.hasSpawn());
        if (data.hasSpawn()) {
            yaml.set("spawn.x", data.spawnX());
            yaml.set("spawn.y", data.spawnY());
            yaml.set("spawn.z", data.spawnZ());
            yaml.set("spawn.yaw", data.spawnYaw());
            yaml.set("spawn.pitch", data.spawnPitch());
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed saving " + file.getName() + ": " + e.getMessage());
        }
    }

    private File fileFor(UUID owner, HouseSlot slot) {
        return new File(housesDir, owner + "-" + slot.index() + ".yml");
    }

    public boolean fileExists(UUID owner, HouseSlot slot) {
        return fileFor(owner, slot).exists();
    }

    public void delete(UUID owner, HouseSlot slot) {
        File file = fileFor(owner, slot);
        if (file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    public File housesDir() {
        return housesDir;
    }
}
