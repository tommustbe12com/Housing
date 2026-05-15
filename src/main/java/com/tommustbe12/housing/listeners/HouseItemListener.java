package com.tommustbe12.housing.listeners;

import com.tommustbe12.housing.debug.Debug;
import com.tommustbe12.housing.gui.*;
import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.houses.HouseSlot;
import com.tommustbe12.housing.util.HousingItems;
import com.tommustbe12.housing.groups.HouseGroupsService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class HouseItemListener implements Listener {
    private static final String TITLE_MAIN = "Housing";
    private static final String TITLE_HOUSES = "Your Houses";
    private static final String TITLE_HOT = "Hot Houses";
    private static final String TITLE_SYSTEMS = "Systems";
    private static final String TITLE_EVENT_ACTIONS = "Event Actions";
    private static final String TITLE_ICON_PICKER = "Choose Icon";
    private static final String TITLE_DELETE_CONFIRM = "Delete House?";

    private final Plugin plugin;
    private final Debug debug;
    private final HouseManager houses;
    private final ActionsEditor actionsEditor;
    private final FunctionsGui functionsGui;
    private final ConditionalGui conditionalGui;
    private final ScoreboardEditorGui scoreboardEditorGui;
    private final CommandsGui commandsGui;
    private final HouseSettingsGui houseSettingsGui;
    private final GroupsGui groupsGui;
    private final InventoryLayoutsGui inventoryLayoutsGui;
    private final NpcsGui npcsGui;
    private final CustomMenusGui customMenusGui;
    private final HouseGroupsService groups;
    private final RegionsGui regionsGui;
    private final WeatherGui weatherGui;
    private final BiomesSkiesGui biomesSkiesGui;
    private final ItemsGui itemsGui;
    private final BlocksGui blocksGui;
    private final HousePlayersGui housePlayersGui;
    private final TeamsGui teamsGui;

    private final NamespacedKey hotOwnerKey;
    private final NamespacedKey hotSlotKey;

    public HouseItemListener(
            Plugin plugin,
            Debug debug,
            HouseManager houses,
            ActionsEditor actionsEditor,
            FunctionsGui functionsGui,
            ConditionalGui conditionalGui,
            ScoreboardEditorGui scoreboardEditorGui,
            CommandsGui commandsGui,
            HouseSettingsGui houseSettingsGui,
            GroupsGui groupsGui,
            InventoryLayoutsGui inventoryLayoutsGui,
            CustomMenusGui customMenusGui,
            NpcsGui npcsGui,
            HouseGroupsService groups,
            RegionsGui regionsGui,
            WeatherGui weatherGui,
            BiomesSkiesGui biomesSkiesGui,
            ItemsGui itemsGui,
            BlocksGui blocksGui,
            HousePlayersGui housePlayersGui,
            TeamsGui teamsGui
    ) {
        this.plugin = plugin;
        this.debug = debug;
        this.houses = houses;
        this.actionsEditor = actionsEditor;
        this.functionsGui = functionsGui;
        this.conditionalGui = conditionalGui;
        this.scoreboardEditorGui = scoreboardEditorGui;
        this.commandsGui = commandsGui;
        this.houseSettingsGui = houseSettingsGui;
        this.groupsGui = groupsGui;
        this.inventoryLayoutsGui = inventoryLayoutsGui;
        this.customMenusGui = customMenusGui;
        this.npcsGui = npcsGui;
        this.groups = groups;
        this.regionsGui = regionsGui;
        this.weatherGui = weatherGui;
        this.biomesSkiesGui = biomesSkiesGui;
        this.itemsGui = itemsGui;
        this.blocksGui = blocksGui;
        this.housePlayersGui = housePlayersGui;
        this.teamsGui = teamsGui;
        this.hotOwnerKey = new NamespacedKey(plugin, "hot_owner");
        this.hotSlotKey = new NamespacedKey(plugin, "hot_slot");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getAction().isRightClick()) return;
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (HousingItems.isMenuStar(plugin, hand)) {
            event.setCancelled(true);
            openMainMenuV2(player);
            return;
        }
        if (HousingItems.isHubHousesItem(plugin, hand)) {
            event.setCancelled(true);
            openHousesMenu(player);
            return;
        }
        if (HousingItems.isHubHotItem(plugin, hand)) {
            event.setCancelled(true);
            openHotMenu(player);
        }
    }

    public void giveMenuItem(Player player) {
        HousingItems.ensureMenuStar(plugin, player);
    }

    @EventHandler
    public void onInvClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getView().getType() != InventoryType.CHEST) return;
        String title = event.getView().getTitle();

        boolean ours = TITLE_MAIN.equals(title)
                || TITLE_HOUSES.equals(title)
                || TITLE_HOT.equals(title)
                || TITLE_SYSTEMS.equals(title)
                || TITLE_EVENT_ACTIONS.equals(title)
                || (weatherGui != null && weatherGui.isTitle(title))
                || (biomesSkiesGui != null && biomesSkiesGui.isTitle(title))
                || (itemsGui != null && itemsGui.isTitle(title))
                || (blocksGui != null && blocksGui.isTitle(title))
                || (housePlayersGui != null && housePlayersGui.isTitle(title))
                || title.startsWith(TITLE_ICON_PICKER)
                || title.startsWith(TITLE_DELETE_CONFIRM)
                || actionsEditor.isEditorTitle(title)
                || actionsEditor.isAddTitle(title)
                || actionsEditor.isFunctionPickerTitle(title)
                || actionsEditor.isLayoutPickerTitle(title)
                || actionsEditor.isTeamPickerTitle(title)
                || actionsEditor.isGroupPickerTitle(title)
                || actionsEditor.isMenuPickerTitle(title)
                || actionsEditor.isChangeVariableTitle(title)
                || actionsEditor.isGiveItemTitle(title)
                || actionsEditor.isRemoveItemTitle(title)
                || actionsEditor.isPickSlotTitle(title)
                || actionsEditor.isCompassTitle(title)
                || actionsEditor.isGamemodeTitle(title)
                || actionsEditor.isDropItemTitle(title)
                || actionsEditor.isVelocityTitle(title)
                || actionsEditor.isLaunchTitle(title)
                || actionsEditor.isEnchantTitle(title)
                || actionsEditor.isEnchantSelectTitle(title)
                || actionsEditor.isRandomTitle(title)
                || actionsEditor.isPotionTitle(title)
                || actionsEditor.isPotionEffectTitle(title)
                || actionsEditor.isNumberTitle(title)
                || actionsEditor.isTeleportTitle(title)
                || actionsEditor.isPauseTitle(title)
                || actionsEditor.isMaxHealthTitle(title)
                || actionsEditor.isHealthTitle(title)
                || actionsEditor.isHungerTitle(title)
                || actionsEditor.isPlaySoundTitle(title)
                || functionsGui.isTitle(title)
                || (regionsGui != null && regionsGui.isTitle(title))
                || (conditionalGui != null && conditionalGui.isTitle(title))
                || (scoreboardEditorGui != null && scoreboardEditorGui.isTitle(title))
                || (commandsGui != null && commandsGui.isTitle(title))
                || (houseSettingsGui != null && houseSettingsGui.isTitle(title))
                || (inventoryLayoutsGui != null && inventoryLayoutsGui.isTitle(title))
                || (teamsGui != null && teamsGui.isTitle(title))
                || (npcsGui != null && npcsGui.isTitle(title));

        if (!ours) return;

        // Allow editing inventory layouts / npc equipment slots
        if (title != null && (title.startsWith("Edit Layout: ") || "NPC Equipment".equals(title))) {
            int raw = event.getRawSlot();
            if (title.startsWith("Edit Layout: ")) {
                // Hotbar slot 8 (reserved for Housing menu star) is editor slot 35 (bottom-right).
                if (raw == 35) event.setCancelled(true);
                if (raw == 49 || raw == 52 || raw == 53) event.setCancelled(true);
            } else {
                int topSize = event.getView().getTopInventory().getSize();
                if (raw < topSize) {
                    boolean editable = raw == 10 || raw == 11 || raw == 12 || raw == 13 || raw == 15 || raw == 16;
                    if (!editable && raw != 26) event.setCancelled(true);
                } // allow bottom inventory interactions + shift-clicking into the editor
            }
        } else {
            event.setCancelled(true);
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        // Actions editor
        if (actionsEditor.isEditorTitle(title)) {
            actionsEditor.handleClick(player, title, event.getRawSlot(), clicked, event.getClick(), () -> openEventActionsMenu(player));
            return;
        }
        if (actionsEditor.isAddTitle(title)) {
            actionsEditor.handleAddPickerClick(player, clicked);
            return;
        }
        if (actionsEditor.isFunctionPickerTitle(title)) {
            actionsEditor.handleFunctionPickerClick(player, clicked, event.getClick());
            return;
        }
        if (actionsEditor.isLayoutPickerTitle(title)) {
            actionsEditor.handleLayoutPickerClick(player, clicked);
            return;
        }
        if (actionsEditor.isTeamPickerTitle(title)) {
            actionsEditor.handleTeamPickerClick(player, clicked);
            return;
        }
        if (actionsEditor.isGroupPickerTitle(title)) {
            actionsEditor.handleGroupPickerClick(player, clicked);
            return;
        }
        if (actionsEditor.isGiveItemTitle(title)) {
            actionsEditor.handleGiveItemClick(player, event.getRawSlot(), clicked);
            return;
        }
        if (actionsEditor.isRemoveItemTitle(title)) {
            actionsEditor.handleRemoveItemClick(player, clicked);
            return;
        }
        if (actionsEditor.isPickSlotTitle(title)) {
            actionsEditor.handlePickSlotClick(player, event.getRawSlot(), clicked);
            return;
        }
        if (actionsEditor.isCompassTitle(title)) {
            actionsEditor.handleCompassClick(player, clicked);
            return;
        }
        if (actionsEditor.isGamemodeTitle(title)) {
            actionsEditor.handleGamemodeClick(player, clicked);
            return;
        }
        if (actionsEditor.isDropItemTitle(title)) {
            actionsEditor.handleDropItemClick(player, clicked);
            return;
        }
        if (actionsEditor.isVelocityTitle(title)) {
            actionsEditor.handleVelocityClick(player, clicked);
            return;
        }
        if (actionsEditor.isLaunchTitle(title)) {
            actionsEditor.handleLaunchClick(player, clicked);
            return;
        }
        if (actionsEditor.isEnchantTitle(title)) {
            actionsEditor.handleEnchantClick(player, clicked);
            return;
        }
        if (actionsEditor.isRandomTitle(title)) {
            actionsEditor.handleRandomClick(player, clicked);
            return;
        }
        if (actionsEditor.isPotionTitle(title)) {
            actionsEditor.handlePotionClick(player, clicked);
            return;
        }
        if (actionsEditor.isPotionEffectTitle(title)) {
            actionsEditor.handlePotionEffectPickerClick(player, clicked);
            return;
        }
        if (actionsEditor.isEnchantSelectTitle(title)) {
            actionsEditor.handleEnchantPickerClick(player, clicked);
            return;
        }
        if (actionsEditor.isNumberTitle(title)) {
            actionsEditor.handleNumberClick(player, clicked);
            return;
        }
        if (actionsEditor.isTeleportTitle(title)) {
            actionsEditor.handleTeleportClick(player, clicked);
            return;
        }
        if (actionsEditor.isPauseTitle(title)) {
            actionsEditor.handlePauseClick(player, clicked);
            return;
        }
        if (actionsEditor.isMaxHealthTitle(title)) {
            actionsEditor.handleMaxHealthClick(player, clicked);
            return;
        }
        if (actionsEditor.isHealthTitle(title)) {
            actionsEditor.handleHealthClick(player, clicked);
            return;
        }
        if (actionsEditor.isHungerTitle(title)) {
            actionsEditor.handleHungerClick(player, clicked);
            return;
        }
        if (actionsEditor.isMenuPickerTitle(title)) {
            actionsEditor.handleMenuPickerClick(player, clicked);
            return;
        }
        if (actionsEditor.isChangeVariableTitle(title)) {
            actionsEditor.handleChangeVariableClick(player, event.getRawSlot(), clicked);
            return;
        }
        if (actionsEditor.isPlaySoundTitle(title)) {
            actionsEditor.handlePlaySoundClick(player, title, event.getRawSlot(), clicked, event.getClick());
            return;
        }
        if (conditionalGui != null && conditionalGui.isTitle(title)) {
            conditionalGui.handleClick(player, title, clicked, event.getClick());
            return;
        }
        if (scoreboardEditorGui != null && scoreboardEditorGui.isTitle(title)) {
            scoreboardEditorGui.handleClick(player, event.getRawSlot(), clicked, () -> openSystemsMenu(player));
            return;
        }
        if (commandsGui != null && commandsGui.isTitle(title)) {
            commandsGui.handleClick(player, title, event.getRawSlot(), clicked, event.getClick(), () -> openSystemsMenu(player));
            return;
        }
        if (houseSettingsGui != null && houseSettingsGui.isTitle(title)) {
            houseSettingsGui.handleClick(player, event.getRawSlot(), clicked, () -> openMainMenuV2(player));
            return;
        }
        if (inventoryLayoutsGui != null && inventoryLayoutsGui.isTitle(title)) {
            inventoryLayoutsGui.handleClick(player, title, event.getRawSlot(), clicked, event.getClick(), () -> openSystemsMenu(player), () -> openSystemsMenu(player));
            return;
        }
        if (teamsGui != null && teamsGui.isTitle(title)) {
            teamsGui.handleClick(player, title, event.getRawSlot(), clicked, event.getClick(), () -> openSystemsMenu(player));
            return;
        }
        if (npcsGui != null && npcsGui.isTitle(title)) {
            npcsGui.handleClick(player, title, event.getRawSlot(), clicked, event.getClick(), () -> openSystemsMenu(player));
            return;
        }
        if (functionsGui.isTitle(title)) {
            functionsGui.handleClick(player, title, event.getRawSlot(), clicked, event.getClick(), () -> openSystemsMenu(player));
            return;
        }

        // New main menu (5 rows)
        if (TITLE_MAIN.equals(title)) {
            var info = houses.getHouseInfoByWorld(player.getWorld());
            boolean inHouse = info != null;
            boolean inOwn = inHouse && info.owner().equals(player.getUniqueId());

            if (clicked.getType() == Material.ARROW) { player.closeInventory(); return; } // bottom middle

            if (!inHouse) {
                if (clicked.getType() == Material.SPRUCE_DOOR) openHotMenu(player);
                return;
            }

            if (clicked.getType() == Material.POWERED_RAIL) {
                boolean canSystems = inOwn;
                if (!inOwn && groups != null) {
                    canSystems = groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.EDIT_ACTIONS)
                            || groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.EDIT_EVENT_ACTIONS)
                            || groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.EDIT_SCOREBOARD)
                            || groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.EDIT_COMMANDS)
                            || groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.EDIT_FUNCTIONS)
                            || groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.EDIT_INVENTORY_LAYOUTS)
                            || groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.EDIT_CUSTOM_MENUS)
                            || groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.USE_NPCS)
                            || groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.EDIT_REGIONS);
                }
                if (canSystems) openSystemsMenu(player);
                return;
            }

            if (clicked.getType() == Material.COMPARATOR && houseSettingsGui != null) { houseSettingsGui.open(player); return; }
            if (clicked.getType() == Material.FILLED_MAP && groupsGui != null) {
                if (!inOwn && (groups == null || !groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.CHANGE_GROUP))) return;
                groupsGui.open(player, () -> openMainMenuV2(player));
                return;
            }
            if (clicked.getType() == Material.WRITABLE_BOOK && housePlayersGui != null) { housePlayersGui.open(player, () -> openMainMenuV2(player)); return; }
            if (clicked.getType() == Material.CAULDRON) { player.getInventory().clear(); player.sendMessage("§aInventory cleared."); return; }
            if (clicked.getType() == Material.SPRUCE_DOOR) { openHotMenu(player); return; }

            if (clicked.getType() == Material.STONE_PICKAXE && inOwn) {
                var data = houses.getHouse(info.owner(), info.slot());
                var spawn = data.spawnInWorld(player.getWorld());
                if (spawn != null) player.teleport(spawn);
                player.setGameMode(org.bukkit.GameMode.ADVENTURE);
                openMainMenuV2(player);
                return;
            }
            if (clicked.getType() == Material.PUFFERFISH && inOwn) {
                player.setGameMode(org.bukkit.GameMode.CREATIVE);
                openMainMenuV2(player);
                return;
            }

            if (clicked.getType() == Material.EMERALD && itemsGui != null) {
                if (!inOwn && (groups == null || !groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.BUILD))) return;
                itemsGui.open(player, () -> openMainMenuV2(player));
                return;
            }
            if (clicked.getType() == Material.CLOCK && weatherGui != null) {
                if (!inOwn && (groups == null || !groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.CHANGE_SETTINGS))) return;
                weatherGui.open(player, () -> openMainMenuV2(player));
                return;
            }
            if (clicked.getType() == Material.GRASS_BLOCK && biomesSkiesGui != null) {
                if (!inOwn && (groups == null || !groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.CHANGE_SETTINGS))) return;
                biomesSkiesGui.open(player, () -> openMainMenuV2(player));
            }
            if (clicked.getType() == Material.BRICKS && blocksGui != null) {
                if (!inOwn && (groups == null || !groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.BUILD))) return;
                blocksGui.open(player, () -> openMainMenuV2(player));
            }
            return;
        }

        if (weatherGui != null && weatherGui.isTitle(title)) {
            weatherGui.handleClick(player, clicked, () -> openMainMenuV2(player));
            return;
        }
        if (biomesSkiesGui != null && biomesSkiesGui.isTitle(title)) {
            biomesSkiesGui.handleClick(player, event.getRawSlot(), clicked, () -> openMainMenuV2(player));
            return;
        }
        if (itemsGui != null && itemsGui.isTitle(title)) {
            itemsGui.handleClick(player, clicked, () -> openMainMenuV2(player));
            return;
        }
        if (blocksGui != null && blocksGui.isTitle(title)) {
            blocksGui.handleClick(player, clicked, () -> openMainMenuV2(player));
            return;
        }
        if (housePlayersGui != null && housePlayersGui.isTitle(title)) {
            housePlayersGui.handleClick(player, event.getRawSlot(), clicked, () -> openMainMenuV2(player));
            return;
        }

        // Main menu
        if (false && TITLE_MAIN.equals(title)) {
            int raw = event.getRawSlot();
            if (clicked.getType() == Material.ARROW) {
                player.closeInventory();
                return;
            }
            if (clicked.getType() == Material.OAK_DOOR) {
                if (raw == 26) {
                    var info = houses.getHouseInfoByWorld(player.getWorld());
                    if (info != null) houses.sendToHub(player);
                    player.closeInventory();
                } else {
                    openHousesMenu(player);
                }
                return;
            }
            if (clicked.getType() == Material.FIREWORK_STAR) {
                openHotMenu(player);
                return;
            }
            if (clicked.getType() == Material.REPEATER) {
                var info = houses.getHouseInfoByWorld(player.getWorld());
                if (info == null) return;
                boolean inOwn = info.owner().equals(player.getUniqueId());
                boolean canSystems = false;
                if (!inOwn && groups != null) {
                    canSystems = groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.EDIT_ACTIONS)
                            || groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.EDIT_EVENT_ACTIONS)
                            || groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.EDIT_SCOREBOARD)
                            || groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.EDIT_COMMANDS)
                            || groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.EDIT_FUNCTIONS)
                            || groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.EDIT_INVENTORY_LAYOUTS)
                            || groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.EDIT_CUSTOM_MENUS)
                            || groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.USE_NPCS);
                }
                if (!inOwn && !canSystems) {
                    player.sendMessage("§cYou don't have permission to open Systems in this house.");
                    return;
                }
                openSystemsMenu(player);
                return;
            }
            if (clicked.getType() == Material.COMPARATOR) {
                houseSettingsGui.open(player);
            }
            return;
        }

        // Houses menu
        if (TITLE_HOUSES.equals(title)) {
            if (clicked.getType() == Material.ARROW) {
                openMainMenu(player);
                return;
            }

            int raw = event.getRawSlot();
            int slotIndex = raw == 10 ? 1 : raw == 13 ? 2 : raw == 16 ? 3 : -1;
            if (slotIndex == -1) return;
            HouseSlot slot = HouseSlot.fromIndex(slotIndex);
            if (slot == null) return;

            // right click icon picker
            if (event.isRightClick() && houses.houseExists(player.getUniqueId(), slot)) {
                openIconPicker(player, slotIndex);
                return;
            }

            // drop = delete confirm
            if (event.getClick() == org.bukkit.event.inventory.ClickType.DROP && houses.houseExists(player.getUniqueId(), slot)) {
                openDeleteConfirm(player, slotIndex);
                return;
            }

            if (!houses.houseExists(player.getUniqueId(), slot)) {
                houses.createIfMissing(player.getUniqueId(), slot);
            }
            if (groups != null && groups.isBanned(player.getUniqueId(), slot, player.getUniqueId())) {
                player.sendMessage("§cYou are banned from this house.");
                return;
            }
            houses.joinHouse(player, player.getUniqueId(), slot);
            player.closeInventory();
            return;
        }

        // Hot houses
        if (TITLE_HOT.equals(title)) {
            if (clicked.getType() == Material.ARROW) {
                openMainMenu(player);
                return;
            }
            ItemMeta meta = clicked.getItemMeta();
            if (meta == null) return;
            String ownerStr = meta.getPersistentDataContainer().get(hotOwnerKey, PersistentDataType.STRING);
            Integer slotIdx = meta.getPersistentDataContainer().get(hotSlotKey, PersistentDataType.INTEGER);
            if (ownerStr == null || slotIdx == null) return;
            try {
                UUID owner = UUID.fromString(ownerStr);
                HouseSlot slot = HouseSlot.fromIndex(slotIdx);
                if (slot == null) return;
                if (groups != null && groups.isBanned(owner, slot, player.getUniqueId())) {
                    player.sendMessage("§cYou are banned from this house.");
                    return;
                }
                houses.joinHouse(player, owner, slot);
                player.closeInventory();
            } catch (IllegalArgumentException ignored) {
            }
            return;
        }

        // Systems menu
        if (TITLE_SYSTEMS.equals(title)) {
            if (clicked.getType() == Material.ARROW) { openMainMenuV2(player); return; }
            var info = houses.getHouseInfoByWorld(player.getWorld());
            boolean inOwn = info != null && info.owner().equals(player.getUniqueId());
            if (clicked.getType() == Material.REDSTONE) { openEventActionsMenu(player); return; }
            if (clicked.getType() == Material.WHITE_BANNER && teamsGui != null) {
                if (info == null) return;
                if (!inOwn && (groups == null || !groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.EDIT_TEAMS))) return;
                teamsGui.open(player, () -> openSystemsMenu(player));
                return;
            }
            if (clicked.getType() == Material.MAP && regionsGui != null) {
                if (info == null) return;
                if (!inOwn && (groups == null || !groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.EDIT_REGIONS))) return;
                regionsGui.openList(player, () -> openSystemsMenu(player));
                return;
            }
            if (clicked.getType() == Material.OAK_SIGN) {
                if (info == null) return;
                if (!inOwn && (groups == null || !groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.EDIT_SCOREBOARD))) return;
                scoreboardEditorGui.open(player, info.owner(), info.slot());
                return;
            }
            if (clicked.getType() == Material.COMMAND_BLOCK) {
                if (info == null) return;
                if (!inOwn && (groups == null || !groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.EDIT_COMMANDS))) return;
                commandsGui.open(player);
                return;
            }
            if (clicked.getType() == Material.BOOK) {
                if (info == null) return;
                if (!inOwn && (groups == null || !groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.EDIT_FUNCTIONS))) return;
                functionsGui.open(player);
                return;
            }
            if (clicked.getType() == Material.CHEST) {
                if (info == null) return;
                if (!inOwn && (groups == null || !groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.EDIT_INVENTORY_LAYOUTS))) return;
                inventoryLayoutsGui.open(player);
                return;
            }
            if (clicked.getType() == Material.ARMOR_STAND) {
                if (info == null) return;
                if (!inOwn && (groups == null || !groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.USE_NPCS))) return;
                npcsGui.open(player);
                return;
            }
            if (clicked.getType() == Material.ITEM_FRAME && customMenusGui != null) {
                if (info == null) return;
                if (!inOwn && (groups == null || !groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.EDIT_CUSTOM_MENUS))) return;
                customMenusGui.open(player, () -> openSystemsMenu(player));
                return;
            }
            player.sendMessage("§7That system is coming soon.");
            return;
        }

        if (regionsGui != null && regionsGui.isTitle(title)) {
            regionsGui.handleClick(player, title, event.getRawSlot(), clicked, event.getClick(), () -> openSystemsMenu(player));
            return;
        }

        // Event actions
        if (TITLE_EVENT_ACTIONS.equals(title)) {
            if (clicked.getType() == Material.ARROW) { openSystemsMenu(player); return; }
            var info = houses.getHouseInfoByWorld(player.getWorld());
            if (info == null) return;
            boolean inOwn = info.owner().equals(player.getUniqueId());
            if (!inOwn && (groups == null || !groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.EDIT_EVENT_ACTIONS))) return;
            String eventKey = switch (clicked.getType()) {
                case LIME_DYE -> "player_join";
                case GRAY_DYE -> "player_quit";
                case RED_DYE -> "player_death";
                case IRON_SWORD -> "player_kill";
                case TOTEM_OF_UNDYING -> "player_respawn";
                case NAME_TAG -> "group_change";
                case SHIELD -> "pvp_state_change";
                case FISHING_ROD -> "fish_caught";
                case ENDER_PEARL -> "enter_portal";
                case ANVIL -> "player_damage";
                case DIAMOND_PICKAXE -> "block_break";
                case DROPPER -> "drop_item";
                case HOPPER -> "pickup_item";
                case STICK -> "held_item_change";
                case FEATHER -> "toggle_sneak";
                case ELYTRA -> "toggle_flight";
                default -> null;
            };
            if (eventKey == null) return;
            actionsEditor.openEventActions(player, info.owner(), info.slot(), eventKey, () -> openEventActionsMenu(player));
            return;
        }

        // Icon picker
        if (title.startsWith(TITLE_ICON_PICKER)) {
            if (clicked.getType() == Material.ARROW) { openHousesMenu(player); return; }
            int slotIndex = parseSlotFromTitle(title);
            HouseSlot slot = HouseSlot.fromIndex(slotIndex);
            if (slot == null) return;
            // Never allow choosing filler panes (or any stained glass pane) as the house icon.
            if (clicked.getType().name().endsWith("_STAINED_GLASS_PANE")) {
                player.sendMessage("§cThat icon isn't allowed. Pick a real item/block icon.");
                return;
            }
            var data = houses.getHouse(player.getUniqueId(), slot);
            data.setIconMaterial(clicked.getType().name());
            houses.saveHouse(data);
            openHousesMenu(player);
            return;
        }

        // Delete confirm
        if (title.startsWith(TITLE_DELETE_CONFIRM)) {
            int slotIndex = parseSlotFromTitle(title);
            if (clicked.getType() == Material.RED_CONCRETE) { openHousesMenu(player); return; }
            if (clicked.getType() == Material.LIME_CONCRETE) {
                HouseSlot slot = HouseSlot.fromIndex(slotIndex);
                if (slot == null) return;
                houses.deleteHouse(player.getUniqueId(), slot);
                player.sendMessage("§aHouse deleted.");
                openHousesMenu(player);
            }
        }
    }

    @EventHandler
    public void onInvDrag(InventoryDragEvent event) {
        if (event.getView().getType() != InventoryType.CHEST) return;
        String title = event.getView().getTitle();
        boolean ours = TITLE_MAIN.equals(title)
                || TITLE_HOUSES.equals(title)
                || TITLE_HOT.equals(title)
                || TITLE_SYSTEMS.equals(title)
                || TITLE_EVENT_ACTIONS.equals(title)
                || (weatherGui != null && weatherGui.isTitle(title))
                || (biomesSkiesGui != null && biomesSkiesGui.isTitle(title))
                || (itemsGui != null && itemsGui.isTitle(title))
                || (blocksGui != null && blocksGui.isTitle(title))
                || (housePlayersGui != null && housePlayersGui.isTitle(title))
                || title.startsWith(TITLE_ICON_PICKER)
                || title.startsWith(TITLE_DELETE_CONFIRM)
                || actionsEditor.isEditorTitle(title)
                || actionsEditor.isAddTitle(title)
                || actionsEditor.isFunctionPickerTitle(title)
                || actionsEditor.isLayoutPickerTitle(title)
                || actionsEditor.isTeamPickerTitle(title)
                || actionsEditor.isGroupPickerTitle(title)
                || actionsEditor.isGiveItemTitle(title)
                || actionsEditor.isRemoveItemTitle(title)
                || actionsEditor.isPickSlotTitle(title)
                || actionsEditor.isCompassTitle(title)
                || actionsEditor.isGamemodeTitle(title)
                || actionsEditor.isDropItemTitle(title)
                || actionsEditor.isVelocityTitle(title)
                || actionsEditor.isLaunchTitle(title)
                || actionsEditor.isEnchantTitle(title)
                || actionsEditor.isEnchantSelectTitle(title)
                || actionsEditor.isRandomTitle(title)
                || actionsEditor.isPotionTitle(title)
                || actionsEditor.isPotionEffectTitle(title)
                || actionsEditor.isNumberTitle(title)
                || actionsEditor.isTeleportTitle(title)
                || actionsEditor.isPauseTitle(title)
                || actionsEditor.isMaxHealthTitle(title)
                || actionsEditor.isHealthTitle(title)
                || actionsEditor.isHungerTitle(title)
                || functionsGui.isTitle(title)
                || (regionsGui != null && regionsGui.isTitle(title))
                || (conditionalGui != null && conditionalGui.isTitle(title))
                || (scoreboardEditorGui != null && scoreboardEditorGui.isTitle(title))
                || (commandsGui != null && commandsGui.isTitle(title))
                || (houseSettingsGui != null && houseSettingsGui.isTitle(title))
                || (inventoryLayoutsGui != null && inventoryLayoutsGui.isTitle(title) && !title.startsWith("Edit Layout: "))
                || (npcsGui != null && npcsGui.isTitle(title) && !"NPC Equipment".equals(title));
        if (ours) event.setCancelled(true);
    }

    private void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_MAIN);
        fill(inv);
        var info = houses.getHouseInfoByWorld(player.getWorld());
        boolean inOwn = info != null && info.owner().equals(player.getUniqueId());
        boolean canSystems = false;
        boolean canSettings = false;
        if (info != null && groups != null) {
            canSystems = groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.EDIT_ACTIONS)
                    || groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.EDIT_EVENT_ACTIONS)
                    || groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.EDIT_SCOREBOARD)
                    || groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.EDIT_COMMANDS)
                    || groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.EDIT_FUNCTIONS)
                    || groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.EDIT_INVENTORY_LAYOUTS)
                    || groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.EDIT_CUSTOM_MENUS)
                    || groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.USE_NPCS);
            canSettings = groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.CHANGE_SETTINGS);
        }

        if (info == null) {
            inv.setItem(11, named(Material.OAK_DOOR, "§aYour Houses", List.of("§7Create or join your houses.")));
            inv.setItem(13, named(Material.FIREWORK_STAR, "§6Hot Houses", List.of("§7Top houses by cookies.")));
            inv.setItem(26, named(Material.ARROW, "§7Back", List.of("§7Close this menu.")));
        } else if (inOwn || canSystems || canSettings) {
            if (inOwn || canSystems) inv.setItem(11, named(Material.REPEATER, "§bSystems", List.of("§7Customize your house.")));
            if (inOwn || canSettings) inv.setItem(13, named(Material.COMPARATOR, "§eSettings", List.of("§7Edit house settings.")));
            inv.setItem(18, named(Material.ARROW, "§7Back", List.of("§7Close this menu.")));
            inv.setItem(26, named(Material.OAK_DOOR, "§cBack to Hub", List.of("§7Teleport back to the hub.")));
        } else {
            inv.setItem(13, named(Material.FIREWORK_STAR, "§6Hot Houses", List.of("§7Top houses by cookies.")));
            inv.setItem(18, named(Material.ARROW, "§7Back", List.of("§7Close this menu.")));
            inv.setItem(26, named(Material.OAK_DOOR, "§cBack to Hub", List.of("§7Teleport back to the hub.")));
        }
        player.openInventory(inv);
    }

    private void openMainMenuV2(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, TITLE_MAIN);
        fill(inv);
        var info = houses.getHouseInfoByWorld(player.getWorld());
        boolean inOwn = info != null && info.owner().equals(player.getUniqueId());

        inv.setItem(40, named(Material.ARROW, "§7Back", List.of("§7Close this menu.")));

        if (info == null) {
            inv.setItem(36, named(Material.SPRUCE_DOOR, "§aTravel to someone else's house", List.of("§7Opens Hot Houses.")));
            player.openInventory(inv);
            return;
        }

        boolean canBuild = inOwn || (groups != null && groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.BUILD));
        boolean canSettings = inOwn || (groups != null && groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.CHANGE_SETTINGS));
        boolean canGroups = inOwn || (groups != null && groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.CHANGE_GROUP));
        boolean canSystems = inOwn;
        if (!inOwn && groups != null) {
            canSystems = groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.EDIT_ACTIONS)
                    || groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.EDIT_EVENT_ACTIONS)
                    || groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.EDIT_SCOREBOARD)
                    || groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.EDIT_COMMANDS)
                    || groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.EDIT_FUNCTIONS)
                    || groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.EDIT_INVENTORY_LAYOUTS)
                    || groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.EDIT_CUSTOM_MENUS)
                    || groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.USE_NPCS)
                    || groups.has(info.owner(), info.slot(), player.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.EDIT_REGIONS);
        }

        // Only show "op-ish" build tools if they have permissions.
        if (canBuild) {
            inv.setItem(0, named(Material.STICK, "§ePro Tools", List.of("§7Coming soon.")));
            inv.setItem(2, named(Material.BRICKS, "§aBlocks", List.of("§7Special building blocks.")));
            inv.setItem(4, named(Material.SKELETON_SKULL, "§fSkulls", List.of("§7Coming soon.")));
            inv.setItem(6, named(Material.EMERALD, "§aItems", List.of("§7Open items menu.")));
        }

        if (canSettings) {
            inv.setItem(12, named(Material.CLOCK, "§bWeather", List.of("§7Change house weather.")));
            inv.setItem(14, named(Material.GRASS_BLOCK, "§aBiomes & Skies", List.of("§7Change biome/sky.")));
        }

        if (canSystems) {
            inv.setItem(31, named(Material.POWERED_RAIL, "§bSystems", List.of("§7Customize your house.")));
        }
        if (inOwn) {
            boolean buildMode = player.getGameMode() == org.bukkit.GameMode.CREATIVE;
            inv.setItem(35, named(buildMode ? Material.STONE_PICKAXE : Material.PUFFERFISH,
                    buildMode ? "§eExit Build Mode" : "§aEnter Build Mode",
                    List.of("§7Click to toggle.")));
        }

        inv.setItem(36, named(Material.SPRUCE_DOOR, "§aTravel to someone else's house", List.of("§7Opens Hot Houses.")));
        if (canSettings) inv.setItem(30, named(Material.COMPARATOR, "§eHouse Settings", List.of("§7Edit house settings.")));
        if (canGroups) inv.setItem(32, named(Material.FILLED_MAP, "§bPermissions & Groups", List.of("§7Edit groups and permissions.")));
        if (canGroups) inv.setItem(37, named(Material.WRITABLE_BOOK, "§fPlayers Here", List.of("§7Manage players currently in this house.")));
        inv.setItem(43, named(Material.CAULDRON, "§7Clear Inventory", List.of("§7Clears your inventory.")));
        inv.setItem(44, named(Material.JUKEBOX, "§dJukebox", List.of("§7Coming soon.")));

        player.openInventory(inv);
    }

    private void openSystemsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_SYSTEMS);
        ItemStack border = named(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, border);

        int[] clear = new int[]{10,12,14,16,20,22,24,26,28};
        for (int idx : clear) inv.setItem(idx, null);

        inv.setItem(10, named(Material.REDSTONE, "§eEvent Actions", List.of("§7Configure triggers.")));
        inv.setItem(12, named(Material.BOOK, "§fFunctions", List.of("§7Reusable action lists.")));
        inv.setItem(14, named(Material.COMMAND_BLOCK, "§dCommands", List.of("§7House commands.")));
        inv.setItem(16, named(Material.OAK_SIGN, "§bScoreboard", List.of("§7Edit scoreboard.")));
        inv.setItem(20, named(Material.CHEST, "§6Inventory Layouts", List.of("§7Saved inventories.")));
        inv.setItem(22, named(Material.ITEM_FRAME, "§aCustom Menus", List.of("§7Create clickable GUIs.")));
        inv.setItem(24, named(Material.ARMOR_STAND, "§eNPCs", List.of("§7Create and edit NPCs.")));
        inv.setItem(26, named(Material.MAP, "§aRegions", List.of("§7WorldEdit-style regions.")));
        inv.setItem(28, named(Material.WHITE_BANNER, "§bTeams", List.of("§7Create and edit teams.")));
        inv.setItem(49, named(Material.ARROW, "§7Back", List.of("§7Return to main menu.")));
        player.openInventory(inv);
    }

    private void openEventActionsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_EVENT_ACTIONS);
        fill(inv);
        inv.setItem(10, named(Material.LIME_DYE, "§aPlayer Join", List.of("§7Edit actions.")));
        inv.setItem(11, named(Material.GRAY_DYE, "§7Player Quit", List.of("§7Edit actions.")));
        inv.setItem(12, named(Material.RED_DYE, "§cPlayer Death", List.of("§7Edit actions.")));
        inv.setItem(13, named(Material.IRON_SWORD, "§ePlayer Kill", List.of("§7Edit actions.")));
        inv.setItem(14, named(Material.TOTEM_OF_UNDYING, "§bPlayer Respawn", List.of("§7Edit actions.")));

        inv.setItem(16, named(Material.NAME_TAG, "§dGroup Change", List.of("§7Edit actions.")));
        inv.setItem(19, named(Material.SHIELD, "§ePvP State Change", List.of("§7Edit actions.")));
        inv.setItem(20, named(Material.FISHING_ROD, "§bFish Caught", List.of("§7Edit actions.")));
        inv.setItem(21, named(Material.ENDER_PEARL, "§aEnter Portal", List.of("§7Edit actions.")));
        inv.setItem(22, named(Material.ANVIL, "§cPlayer Damage", List.of("§7Edit actions.")));

        inv.setItem(24, named(Material.DIAMOND_PICKAXE, "§eBlock Break", List.of("§7Edit actions.")));
        inv.setItem(25, named(Material.DROPPER, "§eDrop Item", List.of("§7Edit actions.")));
        inv.setItem(28, named(Material.HOPPER, "§ePick Up Item", List.of("§7Edit actions.")));
        inv.setItem(29, named(Material.STICK, "§bHeld Item Change", List.of("§7Edit actions.")));
        inv.setItem(30, named(Material.FEATHER, "§aToggle Sneak", List.of("§7Edit actions.")));
        inv.setItem(31, named(Material.ELYTRA, "§bToggle Flight", List.of("§7Edit actions.")));

        inv.setItem(49, named(Material.ARROW, "§7Back", List.of("§7Return to systems.")));
        player.openInventory(inv);
    }

    private void openHousesMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_HOUSES);
        fill(inv);
        inv.setItem(10, houseSlotItem(player.getUniqueId(), 1));
        inv.setItem(13, houseSlotItem(player.getUniqueId(), 2));
        inv.setItem(16, houseSlotItem(player.getUniqueId(), 3));
        inv.setItem(22, named(Material.ARROW, "§7Back", List.of("§7Return to main menu.")));
        player.openInventory(inv);
    }

    private ItemStack houseSlotItem(UUID owner, int slotIndex) {
        HouseSlot slot = HouseSlot.fromIndex(slotIndex);
        if (slot == null) return named(Material.BARRIER, "§cInvalid slot", List.of());
        boolean exists = houses.houseExists(owner, slot);
        if (!exists) return named(Material.OAK_BUTTON, "§aCreate House (Slot " + slotIndex + ")", List.of("§7Click to create and join."));
        var data = houses.getHouse(owner, slot);
        String displayName = ChatColor.translateAlternateColorCodes('&', data.name());
        List<String> lore = new ArrayList<>();
        lore.add("§7Click to join.");
        lore.add("§7Right-click to change icon.");
        lore.add("§7Drop-key to delete.");
        lore.add("§7Cookies: §6" + data.cookies());
        Material icon = Material.matchMaterial(data.iconMaterial());
        if (icon == null) icon = Material.GRASS_BLOCK;
        return named(icon, "§eHouse " + slotIndex + ": §f" + displayName, lore);
    }

    private void openIconPicker(Player player, int slotIndex) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_ICON_PICKER + " (Slot " + slotIndex + ")");
        fill(inv);
        Material[] icons = new Material[]{
                Material.GRASS_BLOCK, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD, Material.BOW,
                Material.GOLDEN_APPLE, Material.ENDER_PEARL, Material.TOTEM_OF_UNDYING,
                Material.CHEST, Material.ENDER_CHEST, Material.REDSTONE, Material.BOOK, Material.BEACON,
                Material.DRAGON_EGG, Material.AMETHYST_SHARD, Material.CLOCK,
                Material.BRICKS, Material.OAK_PLANKS, Material.SPRUCE_PLANKS, Material.STONE_BRICKS,
                Material.COBBLESTONE, Material.QUARTZ_BLOCK, Material.SEA_LANTERN,
                Material.LANTERN, Material.PAINTING, Material.ITEM_FRAME,
                Material.ANVIL, Material.ENCHANTING_TABLE,
                Material.NOTE_BLOCK, Material.JUKEBOX,
                Material.SPAWNER, Material.NETHER_STAR,
                Material.IRON_PICKAXE, Material.DIAMOND_PICKAXE,
                Material.PAPER, Material.BOOKSHELF,
                Material.CAKE, Material.PUMPKIN, Material.JACK_O_LANTERN
        };
        int i = 0;
        for (Material mat : icons) inv.setItem(10 + i++, named(mat, "§a" + mat.name(), List.of("§7Click to set icon.")));
        inv.setItem(49, named(Material.ARROW, "§7Back", List.of("§7Return to houses.")));
        player.openInventory(inv);
    }

    private void openDeleteConfirm(Player player, int slotIndex) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_DELETE_CONFIRM + " (Slot " + slotIndex + ")");
        fill(inv);
        inv.setItem(11, named(Material.LIME_CONCRETE, "§aConfirm Delete", List.of("§cThis cannot be undone.")));
        inv.setItem(15, named(Material.RED_CONCRETE, "§cCancel", List.of("§7Go back.")));
        player.openInventory(inv);
    }

    private void openHotMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_HOT);
        fill(inv);
        var top = houses.topHousesByCookies(45);
        int slot = 0;
        for (var data : top) {
            String name = ChatColor.translateAlternateColorCodes('&', data.name());
            String ownerName = Bukkit.getOfflinePlayer(data.owner()).getName();
            if (ownerName == null) ownerName = data.owner().toString();
            Material icon = Material.matchMaterial(data.iconMaterial());
            if (icon == null) icon = Material.CAKE;
            ItemStack item = named(icon, "§6" + name, List.of("§7By: §b" + ownerName, "§7Cookies: §6" + data.cookies(), "§7Click to join."));
            ItemMeta meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(hotOwnerKey, PersistentDataType.STRING, data.owner().toString());
            meta.getPersistentDataContainer().set(hotSlotKey, PersistentDataType.INTEGER, data.slot().index());
            item.setItemMeta(meta);
            inv.setItem(slot++, item);
            if (slot >= 45) break;
        }
        inv.setItem(49, named(Material.ARROW, "§7Back", List.of("§7Return.")));
        player.openInventory(inv);
    }

    private static int parseSlotFromTitle(String title) {
        int idx = title.indexOf("Slot ");
        if (idx == -1) return 1;
        try { return Integer.parseInt(title.substring(idx + 5, idx + 6)); } catch (Exception e) { return 1; }
    }

    private static ItemStack named(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static void fill(Inventory inv) {
        ItemStack filler = named(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inv.getSize(); i++) if (inv.getItem(i) == null) inv.setItem(i, filler);
    }
}
