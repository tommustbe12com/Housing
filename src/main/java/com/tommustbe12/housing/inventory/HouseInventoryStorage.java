package com.tommustbe12.housing.inventory;

import com.tommustbe12.housing.houses.HouseSlot;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public final class HouseInventoryStorage {
    private final Plugin plugin;
    private final File invDir;

    public HouseInventoryStorage(Plugin plugin) {
        this.plugin = plugin;
        this.invDir = new File(plugin.getDataFolder(), "inventories");
        if (!invDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            invDir.mkdirs();
        }
    }

    public boolean has(UUID houseOwner, HouseSlot slot, UUID player) {
        File file = fileFor(houseOwner, slot);
        if (!file.exists()) return false;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        return yaml.isString(path(player, "contents"));
    }

    public void save(UUID houseOwner, HouseSlot slot, UUID player, PlayerInventorySnapshot snap) {
        File file = fileFor(houseOwner, slot);
        YamlConfiguration yaml = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();

        yaml.set(path(player, "contents"), ItemSerialization.toBase64(snap.contents()));
        yaml.set(path(player, "armor"), ItemSerialization.toBase64(snap.armor()));
        yaml.set(path(player, "offhand"), snap.offhand() == null ? "" : ItemSerialization.toBase64(new ItemStack[]{snap.offhand()}));

        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed saving inventories: " + e.getMessage());
        }
    }

    public PlayerInventorySnapshot load(UUID houseOwner, HouseSlot slot, UUID player) {
        File file = fileFor(houseOwner, slot);
        if (!file.exists()) return null;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        String contents = yaml.getString(path(player, "contents"), null);
        String armor = yaml.getString(path(player, "armor"), null);
        String offhand = yaml.getString(path(player, "offhand"), "");
        if (contents == null || armor == null) return null;

        ItemStack[] off = offhand == null || offhand.isBlank() ? new ItemStack[]{null} : ItemSerialization.fromBase64(offhand);
        return new PlayerInventorySnapshot(
                ItemSerialization.fromBase64(contents),
                ItemSerialization.fromBase64(armor),
                off.length > 0 ? off[0] : null
        );
    }

    private File fileFor(UUID owner, HouseSlot slot) {
        return new File(invDir, owner + "-" + slot.index() + ".yml");
    }

    private static String path(UUID player, String key) {
        return "players." + player + "." + key;
    }
}

