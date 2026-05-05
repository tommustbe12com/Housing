package com.tommustbe12.housing.items;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.ActionList;
import com.tommustbe12.housing.actions.storage.ActionCodec;
import com.tommustbe12.housing.actions.storage.ActionSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ItemActionsStorage {
    private final Plugin plugin;
    private final File file;

    public ItemActionsStorage(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "item-actions.yml");
    }

    public ActionList load(UUID itemId, ActionCodec codec) {
        if (!file.exists()) return new ActionList();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        List<Map<?, ?>> raw = yaml.getMapList("items." + itemId + ".actions");
        ActionList list = new ActionList();
        for (Map<?, ?> map : raw) {
            Action action = codec.decode(map);
            if (action != null) list.actions().add(action);
        }
        return list;
    }

    public void save(UUID itemId, ActionList list, ActionSerializer serializer) {
        YamlConfiguration yaml = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
        List<Map<String, Object>> raw = new ArrayList<>();
        for (Action action : list.actions()) raw.add(serializer.serialize(action));
        yaml.set("items." + itemId + ".actions", raw);
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed saving item actions: " + e.getMessage());
        }
    }
}

