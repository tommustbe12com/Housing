package com.tommustbe12.housing.npcs;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.ActionList;
import com.tommustbe12.housing.actions.storage.ActionCodec;
import com.tommustbe12.housing.actions.storage.ActionSerializer;
import com.tommustbe12.housing.houses.HouseSlot;
import com.tommustbe12.housing.util.ItemStackSerialization;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class NpcStorage {
    private final Plugin plugin;
    private final File dir;

    public NpcStorage(Plugin plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "npcs");
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
    }

    public List<NpcData> load(UUID owner, HouseSlot slot, org.bukkit.World world, ActionCodec codec) {
        File file = fileFor(owner, slot);
        if (!file.exists()) return new ArrayList<>();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        List<NpcData> out = new ArrayList<>();
        var section = yaml.getConfigurationSection("npcs");
        if (section == null) return out;
        for (String key : section.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                NpcData npc = new NpcData(id);
                npc.setName(yaml.getString("npcs." + key + ".name", "Bob"));
                npc.setSkinUsername(yaml.getString("npcs." + key + ".skin", "Steve"));
                npc.setCitizensNpcId(yaml.getInt("npcs." + key + ".citizensId", -1));
                npc.setBehavior(NpcBehavior.valueOf(yaml.getString("npcs." + key + ".behavior", "STATIC")));
                double x = yaml.getDouble("npcs." + key + ".x");
                double y = yaml.getDouble("npcs." + key + ".y");
                double z = yaml.getDouble("npcs." + key + ".z");
                float yaw = (float) yaml.getDouble("npcs." + key + ".yaw");
                float pitch = (float) yaml.getDouble("npcs." + key + ".pitch");
                npc.setLocation(new Location(world, x, y, z, yaw, pitch));

                npc.setHelmet(ItemStackSerialization.fromBase64(yaml.getString("npcs." + key + ".eq.helmet", "")));
                npc.setChestplate(ItemStackSerialization.fromBase64(yaml.getString("npcs." + key + ".eq.chest", "")));
                npc.setLeggings(ItemStackSerialization.fromBase64(yaml.getString("npcs." + key + ".eq.legs", "")));
                npc.setBoots(ItemStackSerialization.fromBase64(yaml.getString("npcs." + key + ".eq.boots", "")));
                npc.setHand(ItemStackSerialization.fromBase64(yaml.getString("npcs." + key + ".eq.hand", "")));
                npc.setOffhand(ItemStackSerialization.fromBase64(yaml.getString("npcs." + key + ".eq.offhand", "")));

                List<Map<?, ?>> raw = yaml.getMapList("npcs." + key + ".actions");
                for (Map<?, ?> m : raw) {
                    Action a = codec.decode(m);
                    if (a != null) npc.clickActions().actions().add(a);
                }

                out.add(npc);
            } catch (Exception ignored) {
            }
        }
        return out;
    }

    public void save(UUID owner, HouseSlot slot, List<NpcData> npcs, ActionSerializer serializer) {
        File file = fileFor(owner, slot);
        YamlConfiguration yaml = new YamlConfiguration();
        for (NpcData npc : npcs) {
            String key = npc.id().toString();
            yaml.set("npcs." + key + ".name", npc.name());
            yaml.set("npcs." + key + ".skin", npc.skinUsername());
            yaml.set("npcs." + key + ".citizensId", npc.citizensNpcId());
            yaml.set("npcs." + key + ".behavior", npc.behavior().name());
            if (npc.location() != null) {
                yaml.set("npcs." + key + ".x", npc.location().getX());
                yaml.set("npcs." + key + ".y", npc.location().getY());
                yaml.set("npcs." + key + ".z", npc.location().getZ());
                yaml.set("npcs." + key + ".yaw", npc.location().getYaw());
                yaml.set("npcs." + key + ".pitch", npc.location().getPitch());
            }
            yaml.set("npcs." + key + ".eq.helmet", ItemStackSerialization.toBase64(npc.helmet()));
            yaml.set("npcs." + key + ".eq.chest", ItemStackSerialization.toBase64(npc.chestplate()));
            yaml.set("npcs." + key + ".eq.legs", ItemStackSerialization.toBase64(npc.leggings()));
            yaml.set("npcs." + key + ".eq.boots", ItemStackSerialization.toBase64(npc.boots()));
            yaml.set("npcs." + key + ".eq.hand", ItemStackSerialization.toBase64(npc.hand()));
            yaml.set("npcs." + key + ".eq.offhand", ItemStackSerialization.toBase64(npc.offhand()));

            List<Map<String, Object>> raw = new ArrayList<>();
            for (var a : npc.clickActions().actions()) raw.add(serializer.serialize(a));
            yaml.set("npcs." + key + ".actions", raw);
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed saving NPCs: " + e.getMessage());
        }
    }

    private File fileFor(UUID owner, HouseSlot slot) {
        return new File(dir, owner + "-" + slot.index() + ".yml");
    }
}
