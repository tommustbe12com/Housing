package com.tommustbe12.housing.listeners;

import com.tommustbe12.housing.debug.Debug;
import com.tommustbe12.housing.gui.*;
import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.houses.HouseSlot;
import com.tommustbe12.housing.util.HousingItems;
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
    private final InventoryLayoutsGui inventoryLayoutsGui;
    private final NpcsGui npcsGui;
    private final CustomMenusGui customMenusGui;

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
            InventoryLayoutsGui inventoryLayoutsGui,
            NpcsGui npcsGui,
            CustomMenusGui customMenusGui
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
        this.inventoryLayoutsGui = inventoryLayoutsGui;
        this.npcsGui = npcsGui;
        this.customMenusGui = customMenusGui;
        this.hotOwnerKey = new NamespacedKey(plugin, "hot_owner");
        this.hotSlotKey = new NamespacedKey(plugin, "hot_slot");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getAction().isRightClick()) return;
        Player player = event.getPlayer();
        if (!HousingItems.isMenuStar(plugin, player.getInventory().getItemInMainHand())) return;
        event.setCancelled(true);
        openMainMenu(player);
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
                || title.startsWith(TITLE_ICON_PICKER)
                || title.startsWith(TITLE_DELETE_CONFIRM)
                || actionsEditor.isEditorTitle(title)
                || actionsEditor.isAddTitle(title)
                || actionsEditor.isFunctionPickerTitle(title)
                || actionsEditor.isLayoutPickerTitle(title)
                || actionsEditor.isMenuPickerTitle(title)
                || actionsEditor.isChangeVariableTitle(title)
                || functionsGui.isTitle(title)
                || (conditionalGui != null && conditionalGui.isTitle(title))
                || (scoreboardEditorGui != null && scoreboardEditorGui.isTitle(title))
                || (commandsGui != null && commandsGui.isTitle(title))
                || (houseSettingsGui != null && houseSettingsGui.isTitle(title))
                || (inventoryLayoutsGui != null && inventoryLayoutsGui.isTitle(title))
                || (npcsGui != null && npcsGui.isTitle(title));

        if (!ours) return;

        // Allow editing inventory layouts / npc equipment slots
        if (title != null && (title.startsWith("Edit Layout: ") || "NPC Equipment".equals(title))) {
            int raw = event.getRawSlot();
            if (title.startsWith("Edit Layout: ")) {
                if (raw == 8) event.setCancelled(true);
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
        if (actionsEditor.isMenuPickerTitle(title)) {
            actionsEditor.handleMenuPickerClick(player, clicked);
            return;
        }
        if (actionsEditor.isChangeVariableTitle(title)) {
            actionsEditor.handleChangeVariableClick(player, event.getRawSlot(), clicked);
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
            houseSettingsGui.handleClick(player, event.getRawSlot(), clicked, () -> openMainMenu(player));
            return;
        }
        if (inventoryLayoutsGui != null && inventoryLayoutsGui.isTitle(title)) {
            inventoryLayoutsGui.handleClick(player, title, event.getRawSlot(), clicked, event.getClick(), () -> openSystemsMenu(player), () -> openSystemsMenu(player));
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

        // Main menu
        if (TITLE_MAIN.equals(title)) {
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
                if (info == null || !info.owner().equals(player.getUniqueId())) {
                    player.sendMessage("§cSystems is only available in your own house.");
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
                houses.joinHouse(player, owner, slot);
                player.closeInventory();
            } catch (IllegalArgumentException ignored) {
            }
            return;
        }

        // Systems menu
        if (TITLE_SYSTEMS.equals(title)) {
            if (clicked.getType() == Material.ARROW) { openMainMenu(player); return; }
            if (clicked.getType() == Material.REDSTONE) { openEventActionsMenu(player); return; }
            if (clicked.getType() == Material.OAK_SIGN) {
                var info = houses.getHouseInfoByWorld(player.getWorld());
                if (info == null || !info.owner().equals(player.getUniqueId())) return;
                scoreboardEditorGui.open(player, info.owner(), info.slot());
                return;
            }
            if (clicked.getType() == Material.COMMAND_BLOCK) { commandsGui.open(player); return; }
            if (clicked.getType() == Material.BOOK) { functionsGui.open(player); return; }
            if (clicked.getType() == Material.CHEST) { inventoryLayoutsGui.open(player); return; }
            if (clicked.getType() == Material.ARMOR_STAND) { npcsGui.open(player); return; }
            if (clicked.getType() == Material.ITEM_FRAME && customMenusGui != null) { customMenusGui.open(player, () -> openSystemsMenu(player)); return; }
            player.sendMessage("§7That system is coming soon.");
            return;
        }

        // Event actions
        if (TITLE_EVENT_ACTIONS.equals(title)) {
            if (clicked.getType() == Material.ARROW) { openSystemsMenu(player); return; }
            var info = houses.getHouseInfoByWorld(player.getWorld());
            if (info == null || !info.owner().equals(player.getUniqueId())) return;
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
                || title.startsWith(TITLE_ICON_PICKER)
                || title.startsWith(TITLE_DELETE_CONFIRM)
                || actionsEditor.isEditorTitle(title)
                || actionsEditor.isAddTitle(title)
                || actionsEditor.isFunctionPickerTitle(title)
                || actionsEditor.isLayoutPickerTitle(title)
                || functionsGui.isTitle(title)
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

        if (info == null) {
            inv.setItem(11, named(Material.OAK_DOOR, "§aYour Houses", List.of("§7Create or join your houses.")));
            inv.setItem(13, named(Material.FIREWORK_STAR, "§6Hot Houses", List.of("§7Top houses by cookies.")));
            inv.setItem(26, named(Material.ARROW, "§7Back", List.of("§7Close this menu.")));
        } else if (inOwn) {
            inv.setItem(11, named(Material.REPEATER, "§bSystems", List.of("§7Customize your house.")));
            inv.setItem(13, named(Material.COMPARATOR, "§eSettings", List.of("§7Edit house settings.")));
            inv.setItem(18, named(Material.ARROW, "§7Back", List.of("§7Close this menu.")));
            inv.setItem(26, named(Material.OAK_DOOR, "§cBack to Hub", List.of("§7Teleport back to the hub.")));
        } else {
            inv.setItem(13, named(Material.FIREWORK_STAR, "§6Hot Houses", List.of("§7Top houses by cookies.")));
            inv.setItem(18, named(Material.ARROW, "§7Back", List.of("§7Close this menu.")));
            inv.setItem(26, named(Material.OAK_DOOR, "§cBack to Hub", List.of("§7Teleport back to the hub.")));
        }
        player.openInventory(inv);
    }

    private void openSystemsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_SYSTEMS);
        ItemStack border = named(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, border);

        int[] clear = new int[]{10,12,14,16,20,22,24};
        for (int idx : clear) inv.setItem(idx, null);

        inv.setItem(10, named(Material.REDSTONE, "§eEvent Actions", List.of("§7Configure triggers.")));
        inv.setItem(12, named(Material.BOOK, "§fFunctions", List.of("§7Reusable action lists.")));
        inv.setItem(14, named(Material.COMMAND_BLOCK, "§dCommands", List.of("§7House commands.")));
        inv.setItem(16, named(Material.OAK_SIGN, "§bScoreboard", List.of("§7Edit scoreboard.")));
        inv.setItem(20, named(Material.CHEST, "§6Inventory Layouts", List.of("§7Saved inventories.")));
        inv.setItem(22, named(Material.ITEM_FRAME, "§aCustom Menus", List.of("§7Create clickable GUIs.")));
        inv.setItem(24, named(Material.ARMOR_STAND, "§eNPCs", List.of("§7Create and edit NPCs.")));
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
                Material.CHEST, Material.REDSTONE, Material.BOOK, Material.BEACON,
                Material.DRAGON_EGG, Material.AMETHYST_SHARD, Material.CLOCK
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
