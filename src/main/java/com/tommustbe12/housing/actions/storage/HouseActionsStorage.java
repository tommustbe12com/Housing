package com.tommustbe12.housing.actions.storage;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.ActionList;
import com.tommustbe12.housing.houses.HouseSlot;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class HouseActionsStorage {
    private final Plugin plugin;
    private final File actionsDir;

    public HouseActionsStorage(Plugin plugin) {
        this.plugin = plugin;
        this.actionsDir = new File(plugin.getDataFolder(), "actions");
        if (!actionsDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            actionsDir.mkdirs();
        }
    }

    public Map<String, ActionList> loadEventActions(UUID owner, HouseSlot slot, ActionCodec codec) {
        File file = fileFor(owner, slot);
        if (!file.exists()) return new HashMap<>();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection events = yaml.getConfigurationSection("events");
        if (events == null) return new HashMap<>();

        Map<String, ActionList> out = new HashMap<>();
        for (String key : events.getKeys(false)) {
            List<Map<?, ?>> raw = events.getMapList(key + ".actions");
            ActionList list = new ActionList();
            for (Map<?, ?> entry : raw) {
                if (!(entry instanceof Map<?, ?>)) continue;

                Map<?, ?> map = entry;
                Action action = codec.decode(map);
                if (action != null) list.actions().add(action);
            }
            out.put(key.toLowerCase(Locale.ROOT), list);
        }
        return out;
    }

    public void saveEventActions(UUID owner, HouseSlot slot, Map<String, ActionList> events, ActionSerializer serializer) {
        File file = fileFor(owner, slot);
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, ActionList> entry : events.entrySet()) {
            String eventKey = entry.getKey().toLowerCase(Locale.ROOT);
            List<Map<String, Object>> raw = new ArrayList<>();
            for (Action action : entry.getValue().actions()) {
                raw.add(serializer.serialize(action));
            }
            yaml.set("events." + eventKey + ".actions", raw);
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed saving actions: " + e.getMessage());
        }
    }

    private File fileFor(UUID owner, HouseSlot slot) {
        return new File(actionsDir, owner + "-" + slot.index() + ".yml");
    }
}
