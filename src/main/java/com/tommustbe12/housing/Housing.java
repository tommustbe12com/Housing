package com.tommustbe12.housing;

import com.tommustbe12.housing.commands.HouseCommand;
import com.tommustbe12.housing.debug.Debug;
import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.inventory.InventoryService;
import com.tommustbe12.housing.listeners.ChatFormatListener;
import com.tommustbe12.housing.listeners.HouseItemListener;
import com.tommustbe12.housing.listeners.PlayerJoinListener;
import com.tommustbe12.housing.listeners.PlayerQuitListener;
import com.tommustbe12.housing.listeners.HouseWorldLifecycleListener;
import com.tommustbe12.housing.tags.OwnerTagService;
import org.bukkit.plugin.java.JavaPlugin;

public final class Housing extends JavaPlugin {

    private Debug debug;
    private HouseManager houseManager;
    private OwnerTagService ownerTagService;
    private InventoryService inventoryService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.debug = new Debug(this);
        this.houseManager = new HouseManager(this, debug);
        this.ownerTagService = new OwnerTagService(this, debug);

        HouseItemListener houseItemListener = new HouseItemListener(this, debug, houseManager);
        this.inventoryService = new InventoryService(this, debug, houseItemListener);
        getServer().getPluginManager().registerEvents(houseItemListener, this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(debug, houseItemListener), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(debug), this);
        getServer().getPluginManager().registerEvents(new HouseWorldLifecycleListener(this, debug, houseManager, ownerTagService, inventoryService), this);
        getServer().getPluginManager().registerEvents(new ChatFormatListener(houseManager), this);

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
