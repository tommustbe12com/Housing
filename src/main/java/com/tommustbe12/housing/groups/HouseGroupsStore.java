package com.tommustbe12.housing.groups;

import com.tommustbe12.housing.houses.HouseSlot;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class HouseGroupsStore {
    private final Plugin plugin;
    private final File dir;

    public HouseGroupsStore(Plugin plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "groups");
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
    }

    public HouseGroupsData load(UUID owner, HouseSlot slot) {
        File file = file(owner, slot);
        if (!file.exists()) {
            HouseGroupsData data = HouseGroupsData.defaults(owner);
            save(owner, slot, data);
            return data;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String defaultGroupId = yaml.getString("default", null);
        Map<UUID, HouseGroup> groups = new LinkedHashMap<>();

        var sec = yaml.getConfigurationSection("groups");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                UUID id;
                try {
                    id = UUID.fromString(key);
                } catch (Exception ignored) {
                    continue;
                }
                String name = sec.getString(key + ".name", "Group");
                String tag = sec.getString(key + ".tag", "&7[VISITOR]");
                int priority = sec.getInt(key + ".priority", 1);
                DefaultGameMode mode;
                try {
                    mode = DefaultGameMode.valueOf(sec.getString(key + ".default-gamemode", "ADVENTURE"));
                } catch (Exception e) {
                    mode = DefaultGameMode.ADVENTURE;
                }
                HouseGroup g = new HouseGroup(id, name, tag, priority, mode);
                var psec = sec.getConfigurationSection(key + ".perms");
                if (psec != null) {
                    for (String pkey : psec.getKeys(false)) {
                        try {
                            HousePermission perm = HousePermission.valueOf(pkey);
                            g.set(perm, psec.getBoolean(pkey, false));
                        } catch (Exception ignored) {
                        }
                    }
                }
                groups.put(id, g);
            }
        }

        UUID def = null;
        if (defaultGroupId != null) {
            try { def = UUID.fromString(defaultGroupId); } catch (Exception ignored) {}
        }
        HouseGroupsData out = new HouseGroupsData(owner, groups, def);
        out.ensureDefaultsPresent();
        return out;
    }

    public void save(UUID owner, HouseSlot slot, HouseGroupsData data) {
        File file = file(owner, slot);
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("default", data.defaultGroupId() != null ? data.defaultGroupId().toString() : null);
        for (HouseGroup g : data.groupsInEditorOrder()) {
            String key = "groups." + g.id();
            yaml.set(key + ".name", g.name());
            yaml.set(key + ".tag", g.tag());
            yaml.set(key + ".priority", g.priority());
            yaml.set(key + ".default-gamemode", g.defaultGameMode().name());
            for (var e : g.perms().entrySet()) {
                yaml.set(key + ".perms." + e.getKey().name(), e.getValue());
            }
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed saving groups: " + e.getMessage());
        }
    }

    private File file(UUID owner, HouseSlot slot) {
        return new File(dir, owner + "-" + slot.index() + ".yml");
    }
}

