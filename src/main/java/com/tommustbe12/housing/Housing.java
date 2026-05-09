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
import com.tommustbe12.housing.gui.ScoreboardEditorGui;
import com.tommustbe12.housing.scoreboard.HouseScoreboardService;
import com.tommustbe12.housing.gui.CommandsGui;
import com.tommustbe12.housing.gui.HouseSettingsGui;
import com.tommustbe12.housing.gui.InventoryLayoutsGui;
import com.tommustbe12.housing.gui.CustomMenusGui;
import com.tommustbe12.housing.cookies.CookieItemListener;
import com.tommustbe12.housing.cookies.CookieService;
import com.tommustbe12.housing.npcs.NpcManager;
import com.tommustbe12.housing.gui.NpcsGui;
import com.tommustbe12.housing.commands.EditCommand;
import com.tommustbe12.housing.listeners.ItemEditGuiListener;
import com.tommustbe12.housing.listeners.ItemActionListener;
import com.tommustbe12.housing.listeners.CustomMenusGuiListener;
import com.tommustbe12.housing.listeners.CustomMenuRuntimeListener;
import com.tommustbe12.housing.listeners.HouseEventActionsListener;
import com.tommustbe12.housing.listeners.PlayerJoinListener;
import com.tommustbe12.housing.listeners.PlayerQuitListener;
import com.tommustbe12.housing.listeners.HouseWorldLifecycleListener;
import com.tommustbe12.housing.listeners.HouseVisibilityListener;
import com.tommustbe12.housing.listeners.HouseVerticalBorderListener;
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
    private HouseScoreboardService scoreboardService;
    private ScoreboardEditorGui scoreboardEditorGui;
    private CommandsGui commandsGui;
    private HouseSettingsGui houseSettingsGui;
    private InventoryLayoutsGui inventoryLayoutsGui;
    private CookieService cookieService;
    private CookieItemListener cookieItemListener;
    private NpcManager npcManager;
    private NpcsGui npcsGui;
    private com.tommustbe12.housing.custommenus.CustomMenusService customMenusService;
    private CustomMenusGui customMenusGui;

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
        this.scoreboardService = new HouseScoreboardService(this, houseManager);
        this.scoreboardEditorGui = new ScoreboardEditorGui(this, chatPrompts, houseManager, scoreboardService);
        this.commandsGui = new CommandsGui(this, debug, chatPrompts, actionsEditor, houseManager);
        this.houseSettingsGui = new HouseSettingsGui(this, chatPrompts, houseManager);
        this.inventoryLayoutsGui = new InventoryLayoutsGui(this, chatPrompts, houseManager, new com.tommustbe12.housing.inventorylayouts.InventoryLayoutsService(this));
        this.itemEditGui = new ItemEditGui(this, debug, chatPrompts, actionsEditor, houseManager);
        this.functionsGui = new FunctionsGui(this, debug, chatPrompts, actionsEditor, houseManager);
        this.customMenusService = new com.tommustbe12.housing.custommenus.CustomMenusService(this, houseManager);
        this.customMenusGui = new CustomMenusGui(this, chatPrompts, houseManager, customMenusService, actionsEditor);
        this.npcManager = new NpcManager(this, debug, houseManager);
        this.npcManager.start();
        // NPCs should only despawn when a house is deactivated (after inactivity timer) or deleted.
        this.houseManager.setOnHouseDeactivated(world -> npcManager.despawnAll(world));
        this.npcsGui = new NpcsGui(this, chatPrompts, houseManager, npcManager, actionsEditor);

        HouseItemListener houseItemListener = new HouseItemListener(this, debug, houseManager, actionsEditor, functionsGui, conditionalGui, scoreboardEditorGui, commandsGui, houseSettingsGui, inventoryLayoutsGui, customMenusGui, npcsGui);
        this.inventoryService = new InventoryService(this, debug, houseItemListener);
        this.cookieService = new CookieService(this, debug, houseManager);
        this.cookieItemListener = new CookieItemListener(this, houseManager, cookieService);
        this.inventoryService.setCookieItemListener(cookieItemListener);
        getServer().getPluginManager().registerEvents(houseItemListener, this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(debug, houseItemListener, houseManager, ownerTagService, npcManager), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(debug), this);
        getServer().getPluginManager().registerEvents(new HouseWorldLifecycleListener(this, debug, houseManager, ownerTagService, inventoryService, actionsService, scoreboardService), this);
        getServer().getPluginManager().registerEvents(new ChatFormatListener(this, houseManager), this);
        getServer().getPluginManager().registerEvents(new HouseVisibilityListener(this, houseManager), this);
        getServer().getPluginManager().registerEvents(new HouseVerticalBorderListener(this, houseManager), this);
        getServer().getPluginManager().registerEvents(new ChatPromptListener(chatPrompts), this);
        getServer().getPluginManager().registerEvents(new ItemEditGuiListener(itemEditGui), this);
        getServer().getPluginManager().registerEvents(new ItemActionListener(this, debug, houseManager, itemEditGui), this);
        getServer().getPluginManager().registerEvents(new CustomMenusGuiListener(customMenusGui), this);
        getServer().getPluginManager().registerEvents(new CustomMenuRuntimeListener(this, debug, customMenusService), this);
        getServer().getPluginManager().registerEvents(new HouseEventActionsListener(houseManager, actionsService), this);
        getServer().getPluginManager().registerEvents(cookieItemListener, this);
        getServer().getPluginManager().registerEvents(new com.tommustbe12.housing.listeners.CitizensNpcInteractListener(houseManager, npcManager, npcsGui), this);
        getServer().getPluginManager().registerEvents(new com.tommustbe12.housing.listeners.NpcEquipCloseListener(npcsGui), this);
        getServer().getPluginManager().registerEvents(new com.tommustbe12.housing.listeners.HouseNpcLifecycleListener(houseManager, npcManager), this);
        getServer().getPluginManager().registerEvents(new com.tommustbe12.housing.listeners.HouseCommandsListener(this, debug, houseManager, actionsService), this);
        getServer().getPluginManager().registerEvents(new com.tommustbe12.housing.listeners.HouseCommandSuggestionsListener(this, houseManager), this);
        getServer().getPluginManager().registerEvents(new HouseRespawnListener(this, houseManager, actionsService, inventoryService), this);
        getServer().getPluginManager().registerEvents(new com.tommustbe12.housing.listeners.NetherStarLockListener(this), this);
        getServer().getPluginManager().registerEvents(new CustomMenusGuiListener(customMenusGui), this);
        getServer().getPluginManager().registerEvents(new CustomMenuRuntimeListener(this, debug, customMenusService), this);

        // 24/7 ensure players always have the Housing nether star in slot 9 (index 8)
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (var p : getServer().getOnlinePlayers()) {
                com.tommustbe12.housing.util.HousingItems.ensureMenuStar(this, p);
            }
        }, 20L, 20L);

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
        if (npcManager != null) {
            npcManager.stop();
        }
    }

    private static void deleteDir(java.io.File dir) {
        if (dir == null || !dir.exists()) return;
        java.io.File[] files = dir.listFiles();
        if (files != null) {
            for (java.io.File f : files) {
                if (f.isDirectory()) deleteDir(f);
                //noinspection ResultOfMethodCallIgnored
                f.delete();
            }
        }
        //noinspection ResultOfMethodCallIgnored
        dir.delete();
    }
}
