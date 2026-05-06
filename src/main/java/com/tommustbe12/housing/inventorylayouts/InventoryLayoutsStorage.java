package com.tommustbe12.housing.inventorylayouts;

import com.tommustbe12.housing.houses.HouseSlot;
import com.tommustbe12.housing.util.ItemStackSerialization;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class InventoryLayoutsStorage {
    private final Plugin plugin;
    private final File dir;

    public InventoryLayoutsStorage(Plugin plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "inventory-layouts");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
    }

    public List<InventoryLayout> load(UUID owner, HouseSlot slot) {
        File file = fileFor(owner, slot);
        if (!file.exists()) return new ArrayList<>();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = yaml.getConfigurationSection("layouts");
        if (sec == null) return new ArrayList<>();

        List<InventoryLayout> out = new ArrayList<>();
        for (String key : sec.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                ConfigurationSection l = sec.getConfigurationSection(key);
                if (l == null) continue;
                String name = l.getString("name", "Layout");
                InventoryLayout layout = new InventoryLayout(id, name);

                ItemStack[] contents = new ItemStack[36];
                ConfigurationSection c = l.getConfigurationSection("contents");
                if (c != null) {
                    for (String s : c.getKeys(false)) {
                        int idx;
                        try { idx = Integer.parseInt(s); } catch (Exception e) { continue; }
                        if (idx < 0 || idx >= 36) continue;
                        String b64 = c.getString(s, "");
                        if (b64 == null || b64.isBlank()) continue;
                        contents[idx] = ItemStackSerialization.fromBase64(b64);
                    }
                }
                layout.setContents(contents);
                layout.setHelmet(decodeItem(l.getString("armor.helmet", "")));
                layout.setChestplate(decodeItem(l.getString("armor.chestplate", "")));
                layout.setLeggings(decodeItem(l.getString("armor.leggings", "")));
                layout.setBoots(decodeItem(l.getString("armor.boots", "")));
                layout.setOffhand(decodeItem(l.getString("offhand", "")));
                out.add(layout);
            } catch (Exception ignored) {
            }
        }

        out.sort(Comparator.comparing(InventoryLayout::name, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    public void save(UUID owner, HouseSlot slot, List<InventoryLayout> layouts) {
        File file = fileFor(owner, slot);
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection sec = yaml.createSection("layouts");
        for (InventoryLayout l : layouts) {
            ConfigurationSection s = sec.createSection(l.id().toString());
            s.set("name", l.name());
            ConfigurationSection c = s.createSection("contents");
            ItemStack[] contents = l.contents();
            if (contents != null) {
                for (int i = 0; i < Math.min(36, contents.length); i++) {
                    ItemStack it = contents[i];
                    if (it == null || it.getType().isAir()) continue;
                    c.set(Integer.toString(i), ItemStackSerialization.toBase64(it));
                }
            }
            s.set("armor.helmet", encodeItem(l.helmet()));
            s.set("armor.chestplate", encodeItem(l.chestplate()));
            s.set("armor.leggings", encodeItem(l.leggings()));
            s.set("armor.boots", encodeItem(l.boots()));
            s.set("offhand", encodeItem(l.offhand()));
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed saving " + file.getName() + ": " + e.getMessage());
        }
    }

    private File fileFor(UUID owner, HouseSlot slot) {
        return new File(dir, owner + "-" + slot.index() + ".yml");
    }

    private static String encodeItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return "";
        return ItemStackSerialization.toBase64(item);
    }

    private static ItemStack decodeItem(String b64) {
        if (b64 == null || b64.isBlank()) return null;
        return ItemStackSerialization.fromBase64(b64);
    }
}

