package com.tommustbe12.housing;

import com.tommustbe12.housing.commands.HouseCommand;
import com.tommustbe12.housing.debug.Debug;
import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.listeners.HouseItemListener;
import com.tommustbe12.housing.listeners.PlayerJoinListener;
import com.tommustbe12.housing.listeners.PlayerQuitListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class Housing extends JavaPlugin {

    private Debug debug;
    private HouseManager houseManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.debug = new Debug(this);
        this.houseManager = new HouseManager(this, debug);

        HouseItemListener houseItemListener = new HouseItemListener(this, debug, houseManager);
        getServer().getPluginManager().registerEvents(houseItemListener, this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(debug, houseItemListener), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(debug), this);

        HouseCommand houseCommand = new HouseCommand(this, debug, houseManager);
        if (getCommand("house") != null) {
            getCommand("house").setExecutor(houseCommand);
            getCommand("house").setTabCompleter(houseCommand);
        } else {
            debug.warn("Command 'house' not found in plugin.yml stuhpid");
        }

    }

    @Override
    public void onDisable() {
        if (houseManager != null) {
            houseManager.shutdown();
        }
    }
}
