package com.tommustbe12.housing;

import com.tommustbe12.housing.commands.HouseCommand;
import com.tommustbe12.housing.actions.HouseActionsService;
import com.tommustbe12.housing.debug.Debug;
import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.inventory.InventoryService;
import com.tommustbe12.housing.listeners.ChatFormatListener;
import com.tommustbe12.housing.listeners.HouseItemListener;
import com.tommustbe12.housing.listeners.HouseRespawnListener;
import com.tommustbe12.housing.listeners.ChatPromptListener;
import com.tommustbe12.housing.gui.ActionsEditor;
import com.tommustbe12.housing.gui.ItemEditGui;
import com.tommustbe12.housing.gui.FunctionsGui;
import com.tommustbe12.housing.gui.ConditionalGui;
import com.tommustbe12.housing.commands.EditCommand;
import com.tommustbe12.housing.listeners.ItemEditGuiListener;
import com.tommustbe12.housing.listeners.ItemActionListener;
import com.tommustbe12.housing.listeners.HouseEventActionsListener;
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
    private HouseActionsService actionsService;
    private com.tommustbe12.housing.chat.ChatPrompts chatPrompts;
    private ActionsEditor actionsEditor;
    private ItemEditGui itemEditGui;
    private FunctionsGui functionsGui;
    private ConditionalGui conditionalGui;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.debug = new Debug(this);
        this.houseManager = new HouseManager(this, debug);
        this.ownerTagService = new OwnerTagService(this, debug);

        this.chatPrompts = new com.tommustbe12.housing.chat.ChatPrompts();
        this.actionsService = new HouseActionsService(this, debug, houseManager);
        this.actionsEditor = new ActionsEditor(this, debug, chatPrompts, houseManager);
        this.conditionalGui = new ConditionalGui(this, chatPrompts, actionsEditor, houseManager);
        this.actionsEditor.setConditionalGui(conditionalGui);
        this.itemEditGui = new ItemEditGui(this, debug, chatPrompts, actionsEditor, houseManager);
        this.functionsGui = new FunctionsGui(this, debug, chatPrompts, actionsEditor, houseManager);
        HouseItemListener houseItemListener = new HouseItemListener(this, debug, houseManager, actionsEditor, functionsGui, conditionalGui);
        this.inventoryService = new InventoryService(this, debug, houseItemListener);
        getServer().getPluginManager().registerEvents(houseItemListener, this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(debug, houseItemListener, houseManager, ownerTagService), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(debug), this);
        getServer().getPluginManager().registerEvents(new HouseWorldLifecycleListener(this, debug, houseManager, ownerTagService, inventoryService, actionsService), this);
        getServer().getPluginManager().registerEvents(new ChatFormatListener(houseManager), this);
        getServer().getPluginManager().registerEvents(new ChatPromptListener(chatPrompts), this);
        getServer().getPluginManager().registerEvents(new ItemEditGuiListener(itemEditGui), this);
        getServer().getPluginManager().registerEvents(new ItemActionListener(this, debug, houseManager, itemEditGui), this);
        getServer().getPluginManager().registerEvents(new HouseEventActionsListener(houseManager, actionsService), this);
        getServer().getPluginManager().registerEvents(new HouseRespawnListener(this, houseManager, actionsService, inventoryService), this);

        HouseCommand houseCommand = new HouseCommand(this, debug, houseManager);
        houseCommand.setActions(actionsService);
        if (getCommand("house") != null) {
            getCommand("house").setExecutor(houseCommand);
            getCommand("house").setTabCompleter(houseCommand);
        } else {
            debug.warn("Command 'house' not found in plugin.yml");
        }

        if (getCommand("edit") != null) {
            getCommand("edit").setExecutor(new EditCommand(itemEditGui));
        } else {
            debug.warn("Command 'edit' not found in plugin.yml");
        }

    }

    @Override
    public void onDisable() {
        if (houseManager != null) {
            houseManager.shutdown();
        }
    }
}
