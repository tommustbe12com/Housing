package com.tommustbe12.housing.cookies;

import com.tommustbe12.housing.debug.Debug;
import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.houses.HouseSlot;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public final class CookieService {
    private final Plugin plugin;
    private final Debug debug;
    private final HouseManager houses;
    private final File file;

    public CookieService(Plugin plugin, Debug debug, HouseManager houses) {
        this.plugin = plugin;
        this.debug = debug;
        this.houses = houses;
        this.file = new File(plugin.getDataFolder(), "cookies-weekly.yml");
    }

    public CookieResult giveCookie(UUID giver, UUID houseOwner, HouseSlot slot) {
        YamlConfiguration yaml = load();
        String week = WeekKey.currentWeekKey();
        if (!week.equals(yaml.getString("week", ""))) {
            yaml = new YamlConfiguration();
            yaml.set("week", week);
            save(yaml);
        }

        String houseKey = houseOwner + ":" + slot.index();
        if (yaml.getBoolean("given." + giver + "." + houseKey, false)) {
            return CookieResult.ALREADY_GAVE_HOUSE;
        }

        int used = yaml.getInt("used." + giver, 0);
        if (used >= 10) return CookieResult.WEEKLY_LIMIT;

        yaml.set("given." + giver + "." + houseKey, true);
        yaml.set("used." + giver, used + 1);
        save(yaml);

        houses.addCookie(houseOwner, slot, 1);
        debug.toOps("Cookie: " + giver + " -> " + houseKey);
        return CookieResult.OK;
    }

    public int remainingThisWeek(UUID giver) {
        YamlConfiguration yaml = load();
        String week = WeekKey.currentWeekKey();
        if (!week.equals(yaml.getString("week", ""))) return 10;
        int used = yaml.getInt("used." + giver, 0);
        return Math.max(0, 10 - used);
    }

    private YamlConfiguration load() {
        if (!file.exists()) return new YamlConfiguration();
        return YamlConfiguration.loadConfiguration(file);
    }

    private void save(YamlConfiguration yaml) {
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed saving cookie weekly data: " + e.getMessage());
        }
    }

    public enum CookieResult {
        OK,
        ALREADY_GAVE_HOUSE,
        WEEKLY_LIMIT
    }
}
