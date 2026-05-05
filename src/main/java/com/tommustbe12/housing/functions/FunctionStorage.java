package com.tommustbe12.housing.functions;

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

public final class FunctionStorage {
    private final Plugin plugin;
    private final File dir;

    public FunctionStorage(Plugin plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "functions");
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
    }

    public Map<String, ActionList> loadAll(UUID owner, HouseSlot slot, ActionCodec codec) {
        File file = fileFor(owner, slot);
        if (!file.exists()) return new HashMap<>();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection funcs = yaml.getConfigurationSection("functions");
        if (funcs == null) return new HashMap<>();

        Map<String, ActionList> out = new HashMap<>();
        for (String key : funcs.getKeys(false)) {
            List<Map<?, ?>> raw = funcs.getMapList(key + ".actions");
            ActionList list = new ActionList();
            for (Map<?, ?> map : raw) {
                Action action = codec.decode(map);
                if (action != null) list.actions().add(action);
            }
            out.put(key, list);
        }
        return out;
    }

    public void saveAll(UUID owner, HouseSlot slot, Map<String, ActionList> functions, ActionSerializer serializer) {
        File file = fileFor(owner, slot);
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, ActionList> entry : functions.entrySet()) {
            List<Map<String, Object>> raw = new ArrayList<>();
            for (Action action : entry.getValue().actions()) raw.add(serializer.serialize(action));
            yaml.set("functions." + entry.getKey() + ".actions", raw);
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed saving functions: " + e.getMessage());
        }
    }

    private File fileFor(UUID owner, HouseSlot slot) {
        return new File(dir, owner + "-" + slot.index() + ".yml");
    }
}

