package com.tommustbe12.housing.custommenus;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.ActionList;
import com.tommustbe12.housing.actions.storage.ActionCodec;
import com.tommustbe12.housing.actions.storage.ActionSerializer;
import com.tommustbe12.housing.houses.HouseSlot;
import com.tommustbe12.housing.util.ItemStackSerialization;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class CustomMenusStorage {
    private final Plugin plugin;
    private final File dir;

    public CustomMenusStorage(Plugin plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "custom-menus");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
    }

    public List<CustomMenu> load(UUID owner, HouseSlot slot, ActionCodec codec) {
        File file = fileFor(owner, slot);
        if (!file.exists()) return new ArrayList<>();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = yaml.getConfigurationSection("menus");
        if (sec == null) return new ArrayList<>();
        List<CustomMenu> out = new ArrayList<>();
        for (String idStr : sec.getKeys(false)) {
            try {
                UUID id = UUID.fromString(idStr);
                ConfigurationSection m = sec.getConfigurationSection(idStr);
                if (m == null) continue;
                String name = m.getString("name", "Menu");
                String title = m.getString("title", "");
                int rows = m.getInt("rows", 3);
                CustomMenu menu = new CustomMenu(id, name, rows);
                if (title != null && !title.isBlank()) menu.setTitle(title);
                ItemStack[] contents = new ItemStack[rows * 9];
                ConfigurationSection c = m.getConfigurationSection("contents");
                if (c != null) {
                    for (String k : c.getKeys(false)) {
                        int idx;
                        try { idx = Integer.parseInt(k); } catch (Exception e) { continue; }
                        if (idx < 0 || idx >= contents.length) continue;
                        contents[idx] = ItemStackSerialization.fromBase64(c.getString(k, ""));
                    }
                }
                menu.setContents(contents);

                ConfigurationSection a = m.getConfigurationSection("actions");
                if (a != null) {
                    for (String k : a.getKeys(false)) {
                        int idx;
                        try { idx = Integer.parseInt(k); } catch (Exception e) { continue; }
                        CustomMenu.SlotActions slotActions = new CustomMenu.SlotActions();

                        // v2 format: actions.<slot>.left / actions.<slot>.right
                        ConfigurationSection slotSec = a.getConfigurationSection(k);
                        if (slotSec != null) {
                            loadActionList(slotSec.getMapList("left"), codec, slotActions.left());
                            loadActionList(slotSec.getMapList("right"), codec, slotActions.right());
                        } else {
                            // v1 format: actions.<slot> as a list (assumed left-click)
                            loadActionList(a.getMapList(k), codec, slotActions.left());
                        }
                        menu.slotActions().put(idx, slotActions);
                    }
                }
                out.add(menu);
            } catch (Exception ignored) {}
        }
        out.sort(Comparator.comparing(CustomMenu::name, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    public void save(UUID owner, HouseSlot slot, List<CustomMenu> menus, ActionSerializer serializer) {
        File file = fileFor(owner, slot);
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection sec = yaml.createSection("menus");
        for (CustomMenu menu : menus) {
            ConfigurationSection m = sec.createSection(menu.id().toString());
            m.set("name", menu.name());
            m.set("title", menu.title());
            m.set("rows", menu.rows());
            ConfigurationSection c = m.createSection("contents");
            ItemStack[] contents = menu.contents();
            for (int i = 0; i < contents.length; i++) {
                ItemStack it = contents[i];
                if (it == null || it.getType().isAir()) continue;
                c.set(Integer.toString(i), ItemStackSerialization.toBase64(it));
            }
            ConfigurationSection a = m.createSection("actions");
            for (Map.Entry<Integer, CustomMenu.SlotActions> e : menu.slotActions().entrySet()) {
                ConfigurationSection slotSec = a.createSection(Integer.toString(e.getKey()));
                slotSec.set("left", serializeList(e.getValue().left(), serializer));
                slotSec.set("right", serializeList(e.getValue().right(), serializer));
            }
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed saving custom menus: " + e.getMessage());
        }
    }

    private static void loadActionList(List<Map<?, ?>> raw, ActionCodec codec, ActionList out) {
        if (raw == null) return;
        for (Map<?, ?> map : raw) {
            Action act = codec.decode(map);
            if (act != null) out.actions().add(act);
        }
    }

    private static List<Map<String, Object>> serializeList(ActionList list, ActionSerializer serializer) {
        List<Map<String, Object>> raw = new ArrayList<>();
        if (list == null) return raw;
        for (Action act : list.actions()) raw.add(serializer.serialize(act));
        return raw;
    }

    private File fileFor(UUID owner, HouseSlot slot) {
        return new File(dir, owner + "-" + slot.index() + ".yml");
    }
}
