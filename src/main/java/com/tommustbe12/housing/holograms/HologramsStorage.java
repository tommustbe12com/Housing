package com.tommustbe12.housing.holograms;

import com.tommustbe12.housing.houses.HouseSlot;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class HologramsStorage {
    private final Plugin plugin;
    private final File dir;

    public HologramsStorage(Plugin plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "holograms");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
    }

    public List<HologramData> load(UUID owner, HouseSlot slot, World world) {
        File file = fileFor(owner, slot);
        if (!file.exists()) return new ArrayList<>();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = yaml.getConfigurationSection("holograms");
        if (sec == null) return new ArrayList<>();
        List<HologramData> out = new ArrayList<>();
        for (String idStr : sec.getKeys(false)) {
            try {
                UUID id = UUID.fromString(idStr);
                ConfigurationSection h = sec.getConfigurationSection(idStr);
                if (h == null) continue;
                double x = h.getDouble("x");
                double y = h.getDouble("y");
                double z = h.getDouble("z");
                float yaw = (float) h.getDouble("yaw", 0d);
                float pitch = (float) h.getDouble("pitch", 0d);
                List<String> lines = h.getStringList("lines");
                HologramData data = new HologramData(id);
                data.setLocation(new Location(world, x, y, z, yaw, pitch));
                if (lines != null) data.lines().addAll(lines);
                out.add(data);
            } catch (Exception ignored) {}
        }
        return out;
    }

    public void save(UUID owner, HouseSlot slot, List<HologramData> holograms) {
        File file = fileFor(owner, slot);
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection sec = yaml.createSection("holograms");
        for (HologramData h : holograms) {
            if (h.location() == null) continue;
            ConfigurationSection s = sec.createSection(h.id().toString());
            s.set("x", h.location().getX());
            s.set("y", h.location().getY());
            s.set("z", h.location().getZ());
            s.set("yaw", h.location().getYaw());
            s.set("pitch", h.location().getPitch());
            s.set("lines", new ArrayList<>(h.lines()));
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed saving holograms: " + e.getMessage());
        }
    }

    private File fileFor(UUID owner, HouseSlot slot) {
        return new File(dir, owner + "-" + slot.index() + ".yml");
    }
}

