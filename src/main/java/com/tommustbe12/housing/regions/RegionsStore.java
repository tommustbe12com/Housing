package com.tommustbe12.housing.regions;

import com.tommustbe12.housing.actions.ActionList;
import com.tommustbe12.housing.actions.storage.SimpleActionCodec;
import com.tommustbe12.housing.actions.storage.SimpleActionSerializer;
import com.tommustbe12.housing.houses.HouseSlot;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class RegionsStore {
    private final Plugin plugin;
    private final SimpleActionSerializer serializer = new SimpleActionSerializer();

    public RegionsStore(Plugin plugin) {
        this.plugin = plugin;
    }

    public Map<String, RegionData> load(UUID owner, HouseSlot slot, SimpleActionCodec codec) {
        File file = file(owner, slot);
        if (!file.exists()) return new LinkedHashMap<>();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        Map<String, RegionData> out = new LinkedHashMap<>();
        ConfigurationSection regions = yaml.getConfigurationSection("regions");
        if (regions == null) return out;
        for (String key : regions.getKeys(false)) {
            ConfigurationSection sec = regions.getConfigurationSection(key);
            if (sec == null) continue;
            String name = sec.getString("name", key);
            Location p1 = loadLoc(sec.getConfigurationSection("pos1"));
            Location p2 = loadLoc(sec.getConfigurationSection("pos2"));
            if (p1 == null || p2 == null) continue;
            RegionData r = new RegionData(name, p1, p2);

            ConfigurationSection s = sec.getConfigurationSection("settings");
            if (s != null) {
                RegionSettings st = r.settings();
                st.pvpDamage = s.getBoolean("pvpDamage", st.pvpDamage);
                st.doubleJump = s.getBoolean("doubleJump", st.doubleJump);
                st.fireDamage = s.getBoolean("fireDamage", st.fireDamage);
                st.fallDamage = s.getBoolean("fallDamage", st.fallDamage);
                st.poisonWitherRoseDamage = s.getBoolean("poisonWitherRoseDamage", st.poisonWitherRoseDamage);
                st.suffocation = s.getBoolean("suffocation", st.suffocation);
                st.hunger = s.getBoolean("hunger", st.hunger);
                st.naturalRegen = s.getBoolean("naturalRegen", st.naturalRegen);
                st.killDeathMessages = s.getBoolean("killDeathMessages", st.killDeathMessages);
                st.instantRespawn = s.getBoolean("instantRespawn", st.instantRespawn);
                st.keepInventory = s.getBoolean("keepInventory", st.keepInventory);
            }

            r.setEntryActions(readActionList(sec, "entryActions", codec));
            r.setExitActions(readActionList(sec, "exitActions", codec));

            out.put(key.toLowerCase(), r);
        }
        return out;
    }

    public void save(UUID owner, HouseSlot slot, Map<String, RegionData> regions, SimpleActionCodec codec) {
        File file = file(owner, slot);
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection root = yaml.createSection("regions");
        for (var e : regions.entrySet()) {
            String key = e.getKey();
            RegionData r = e.getValue();
            if (r == null || r.pos1() == null || r.pos2() == null) continue;
            ConfigurationSection sec = root.createSection(key);
            sec.set("name", r.name());
            saveLoc(sec.createSection("pos1"), r.pos1());
            saveLoc(sec.createSection("pos2"), r.pos2());

            RegionSettings st = r.settings();
            ConfigurationSection s = sec.createSection("settings");
            s.set("pvpDamage", st.pvpDamage);
            s.set("doubleJump", st.doubleJump);
            s.set("fireDamage", st.fireDamage);
            s.set("fallDamage", st.fallDamage);
            s.set("poisonWitherRoseDamage", st.poisonWitherRoseDamage);
            s.set("suffocation", st.suffocation);
            s.set("hunger", st.hunger);
            s.set("naturalRegen", st.naturalRegen);
            s.set("killDeathMessages", st.killDeathMessages);
            s.set("instantRespawn", st.instantRespawn);
            s.set("keepInventory", st.keepInventory);

            writeActionList(sec, "entryActions", r.entryActions(), codec);
            writeActionList(sec, "exitActions", r.exitActions(), codec);
        }
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed saving regions: " + ex.getMessage());
        }
    }

    private ActionList readActionList(ConfigurationSection regionSec, String key, SimpleActionCodec codec) {
        if (regionSec == null) return new ActionList();
        java.util.List<java.util.Map<?, ?>> raw = regionSec.getMapList(key + ".actions");
        ActionList list = new ActionList();
        for (java.util.Map<?, ?> m : raw) {
            var action = codec.decode(m);
            if (action != null) list.actions().add(action);
        }
        return list;
    }

    private void writeActionList(ConfigurationSection regionSec, String key, ActionList list, SimpleActionCodec codec) {
        if (regionSec == null) return;
        if (list == null) list = new ActionList();
        java.util.List<java.util.Map<String, Object>> raw = new java.util.ArrayList<>();
        for (var action : list.actions()) raw.add(serializer.serialize(action));
        regionSec.set(key + ".actions", raw);
    }

    private File file(UUID owner, HouseSlot slot) {
        File dir = new File(plugin.getDataFolder(), "regions");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, owner + "-" + slot.index() + ".yml");
    }

    private static void saveLoc(ConfigurationSection sec, Location loc) {
        sec.set("world", loc.getWorld() == null ? null : loc.getWorld().getUID().toString());
        sec.set("x", loc.getBlockX());
        sec.set("y", loc.getBlockY());
        sec.set("z", loc.getBlockZ());
    }

    private static Location loadLoc(ConfigurationSection sec) {
        if (sec == null) return null;
        String w = sec.getString("world", "");
        if (w == null || w.isBlank()) return null;
        try {
            UUID id = UUID.fromString(w);
            var world = Bukkit.getWorld(id);
            if (world == null) return null;
            return new Location(world, sec.getInt("x"), sec.getInt("y"), sec.getInt("z"));
        } catch (Exception e) {
            return null;
        }
    }
}
