package com.tommustbe12.housing.commands;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.ActionList;
import com.tommustbe12.housing.actions.storage.ActionCodec;
import com.tommustbe12.housing.actions.storage.ActionSerializer;
import com.tommustbe12.housing.houses.HouseSlot;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class HouseCommandsStorage {
    private final Plugin plugin;
    private final File dir;

    public HouseCommandsStorage(Plugin plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "house-commands");
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
    }

    public Map<String, ActionList> load(UUID owner, HouseSlot slot, ActionCodec codec) {
        File file = fileFor(owner, slot);
        if (!file.exists()) return new HashMap<>();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection cmds = yaml.getConfigurationSection("commands");
        if (cmds == null) return new HashMap<>();

        Map<String, ActionList> out = new HashMap<>();
        for (String key : cmds.getKeys(false)) {
            List<Map<?, ?>> raw = cmds.getMapList(key + ".actions");
            ActionList list = new ActionList();
            for (Map<?, ?> m : raw) {
                Action a = codec.decode(m);
                if (a != null) list.actions().add(a);
            }
            out.put(key.toLowerCase(Locale.ROOT), list);
        }
        return out;
    }

    public void save(UUID owner, HouseSlot slot, Map<String, ActionList> commands, ActionSerializer serializer) {
        File file = fileFor(owner, slot);
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, ActionList> e : commands.entrySet()) {
            List<Map<String, Object>> raw = new ArrayList<>();
            for (Action a : e.getValue().actions()) raw.add(serializer.serialize(a));
            yaml.set("commands." + e.getKey().toLowerCase(Locale.ROOT) + ".actions", raw);
        }
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed saving house commands: " + ex.getMessage());
        }
    }

    private File fileFor(UUID owner, HouseSlot slot) {
        return new File(dir, owner + "-" + slot.index() + ".yml");
    }
}

