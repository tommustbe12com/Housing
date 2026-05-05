package com.tommustbe12.housing.listeners;

import com.tommustbe12.housing.debug.Debug;
import com.tommustbe12.housing.gui.ActionsEditor;
import com.tommustbe12.housing.houses.HouseManager;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class HouseItemListener implements Listener {
    private final Plugin plugin;
    private final Debug debug;
    private final HouseManager houses;
    private final ActionsEditor actionsEditor;

    private final NamespacedKey housingItemKey;
    private final NamespacedKey hotOwnerKey;
    private final NamespacedKey hotSlotKey;

    private static final String TITLE_MAIN = "Housing";
    private static final String TITLE_HOUSES = "Your Houses";
    private static final String TITLE_HOT = "Hot Houses";
    private static final String TITLE_SYSTEMS = "Systems";
    private static final String TITLE_EVENT_ACTIONS = "Event Actions";
    private static final String TITLE_ICON_PICKER = "Choose Icon";
    private static final String TITLE_DELETE_CONFIRM = "Delete House?";

    public HouseItemListener(Plugin plugin, Debug debug, HouseManager houses, ActionsEditor actionsEditor) {
        this.plugin = plugin;
        this.debug = debug;
        this.houses = houses;
        this.actionsEditor = actionsEditor;
        this.housingItemKey = new NamespacedKey(plugin, "housing_item");
        this.hotOwnerKey = new NamespacedKey(plugin, "hot_owner");
        this.hotSlotKey = new NamespacedKey(plugin, "hot_slot");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getAction().isRightClick()) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isHousingMenuItem(item)) return;

        event.setCancelled(true);
        openMainMenu(player);
        debug.to(player, "Opened main nether-star menu.");
    }

    public void giveMenuItem(Player player) {
        ItemStack star = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = star.getItemMeta();
        meta.setDisplayName("§bHousing");
        meta.setLore(List.of("§7Right-click to open"));
        meta.getPersistentDataContainer().set(housingItemKey, PersistentDataType.BYTE, (byte) 1);
        star.setItemMeta(meta);
        player.getInventory().setItem(8, star);
    }

    private void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_MAIN);
        fill(inv);
        inv.setItem(11, named(Material.OAK_DOOR, "§aYour Houses", List.of("§7Create or join your houses.")));
        boolean inOwnHouse = false;
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info != null && info.owner().equals(player.getUniqueId())) inOwnHouse = true;

        if (inOwnHouse) {
            inv.setItem(13, named(Material.REPEATER, "§bSystems", List.of("§7Customize your house.")));
            inv.setItem(15, named(Material.FIREWORK_STAR, "§6Hot Houses", List.of("§7Top houses by cookies.")));
        } else {
            inv.setItem(13, named(Material.FIREWORK_STAR, "§6Hot Houses", List.of("§7Top houses by cookies.")));
        }
        inv.setItem(26, named(Material.BARRIER, "§cBack to Hub", List.of("§7Teleport back to the hub.")));
        player.openInventory(inv);
    }

    private void openSystemsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_SYSTEMS);
        fill(inv);
        inv.setItem(10, named(Material.GRASS_BLOCK, "§aRegions", List.of("§7Coming in a later version.")));
        inv.setItem(12, named(Material.REDSTONE, "§eEvent Actions", List.of("§7Configure triggers (V1 scaffold).")));
        inv.setItem(14, named(Material.OAK_SIGN, "§bScoreboard Editor", List.of("§7Coming in a later version.")));
        inv.setItem(16, named(Material.COMMAND_BLOCK, "§dCommands", List.of("§7Coming in a later version.")));
        inv.setItem(28, named(Material.BOOK, "§fFunctions", List.of("§7Coming in a later version.")));
        inv.setItem(30, named(Material.CHEST, "§6Inventory Layouts", List.of("§7Coming in a later version.")));
        inv.setItem(32, named(Material.PLAYER_HEAD, "§9Teams", List.of("§7Coming in a later version.")));
        inv.setItem(34, named(Material.ITEM_FRAME, "§aCustom Menus", List.of("§7Coming in a later version.")));
        inv.setItem(38, named(Material.ARMOR_STAND, "§eNPCs", List.of("§7Coming in a later version.")));
        inv.setItem(49, named(Material.ARROW, "§7Back", List.of("§7Return to main menu.")));
        player.openInventory(inv);
    }

    private void openEventActionsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_EVENT_ACTIONS);
        fill(inv);
        inv.setItem(10, named(Material.LIME_DYE, "§aPlayer Join", List.of("§7Coming soon.")));
        inv.setItem(11, named(Material.GRAY_DYE, "§7Player Quit", List.of("§7Coming soon.")));
        inv.setItem(12, named(Material.RED_DYE, "§cPlayer Death", List.of("§7Coming soon.")));
        inv.setItem(13, named(Material.IRON_SWORD, "§ePlayer Kill", List.of("§7Coming soon.")));
        inv.setItem(14, named(Material.TOTEM_OF_UNDYING, "§bPlayer Respawn", List.of("§7Coming soon.")));

        inv.setItem(16, named(Material.NAME_TAG, "§dGroup Change", List.of("§7Coming soon.")));
        inv.setItem(19, named(Material.SHIELD, "§ePvP State Change", List.of("§7Coming soon.")));
        inv.setItem(20, named(Material.FISHING_ROD, "§bFish Caught", List.of("§7Coming soon.")));
        inv.setItem(21, named(Material.ENDER_PEARL, "§aEnter Portal", List.of("§7Coming soon.")));
        inv.setItem(22, named(Material.ANVIL, "§cPlayer Damage", List.of("§7Coming soon.")));

        inv.setItem(24, named(Material.DIAMOND_PICKAXE, "§eBlock Break", List.of("§7Coming soon.")));
        inv.setItem(25, named(Material.DROPPER, "§eDrop Item", List.of("§7Coming soon.")));
        inv.setItem(28, named(Material.HOPPER, "§ePick Up Item", List.of("§7Coming soon.")));
        inv.setItem(29, named(Material.STICK, "§bHeld Item Change", List.of("§7Coming soon.")));
        inv.setItem(30, named(Material.FEATHER, "§aToggle Sneak", List.of("§7Coming soon.")));
        inv.setItem(31, named(Material.ELYTRA, "§bToggle Flight", List.of("§7Coming soon.")));

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

    private ItemStack houseSlotItem(UUID owner, int slot) {
        var data = houses.getHouse(owner, com.tommustbe12.housing.houses.HouseSlot.fromIndex(slot));
        boolean exists = houses.houseExists(owner, com.tommustbe12.housing.houses.HouseSlot.fromIndex(slot));
        if (!exists) {
            return named(Material.OAK_BUTTON, "§aCreate House (Slot " + slot + ")", List.of("§7Click to create and join."));
        }
        String displayName = ChatColor.translateAlternateColorCodes('&', data.name());
        List<String> lore = new ArrayList<>();
        lore.add("§7Click to join.");
        lore.add("§7Right-click to change icon.");
        lore.add("§7Drop-key to delete.");
        lore.add("§7Cookies: §6" + data.cookies());
        Material icon = Material.matchMaterial(data.iconMaterial());
        if (icon == null) icon = Material.GRASS_BLOCK;
        return named(icon, "§eHouse " + slot + ": §f" + displayName, lore);
    }

    private void openIconPicker(Player player, int slotIndex) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_ICON_PICKER + " (Slot " + slotIndex + ")");
        fill(inv);
        // curated "cool" icons
        Material[] icons = new Material[]{
                Material.GRASS_BLOCK, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD, Material.BOW,
                Material.GOLDEN_APPLE, Material.ENDER_PEARL, Material.FIRE_CHARGE, Material.TOTEM_OF_UNDYING,
                Material.CHEST, Material.REDSTONE, Material.BOOK, Material.WRITABLE_BOOK,
                Material.BEACON, Material.DRAGON_EGG, Material.AMETHYST_SHARD, Material.CLOCK
        };
        int i = 0;
        for (Material mat : icons) {
            inv.setItem(10 + i, named(mat, "§a" + mat.name(), List.of("§7Click to set icon.")));
            i++;
        }
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
            ItemStack item = named(Material.CAKE, "§6" + name, List.of("§7Cookies: §6" + data.cookies(), "§7Click to join."));
            ItemMeta meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(hotOwnerKey, PersistentDataType.STRING, data.owner().toString());
            meta.getPersistentDataContainer().set(hotSlotKey, PersistentDataType.INTEGER, data.slot().index());
            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }
        inv.setItem(49, named(Material.ARROW, "§7Back", List.of("§7Return to main menu.")));
        player.openInventory(inv);
    }

    @EventHandler
    public void onInvClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getView().getType() == InventoryType.CHEST) {
            String title = event.getView().getTitle();
            if (!TITLE_MAIN.equals(title)
                    && !TITLE_HOUSES.equals(title)
                    && !TITLE_HOT.equals(title)
                    && !TITLE_SYSTEMS.equals(title)
                    && !TITLE_EVENT_ACTIONS.equals(title)
                    && !"Add Action".equals(title)
                    && !actionsEditor.isEditorTitle(title)
                    && !title.startsWith(TITLE_ICON_PICKER)
                    && !title.startsWith(TITLE_DELETE_CONFIRM)) return;

            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) return;

            // Actions editor overlays (event actions)
            if (actionsEditor.isEditorTitle(title)) {
                actionsEditor.handleClick(player, title, event.getRawSlot(), clicked, event.getClick(), () -> openEventActionsMenu(player));
                return;
            }
            if ("Add Action".equals(title)) {
                actionsEditor.handleAddPickerClick(player, clicked);
                return;
            }

            if (TITLE_MAIN.equals(title)) {
                switch (clicked.getType()) {
                    case OAK_DOOR -> openHousesMenu(player);
                    case REPEATER -> {
                        var info = houses.getHouseInfoByWorld(player.getWorld());
                        if (info == null || !info.owner().equals(player.getUniqueId())) {
                            player.sendMessage("§cSystems is only available in your own house.");
                            return;
                        }
                        openSystemsMenu(player);
                    }
                    case FIREWORK_STAR -> openHotMenu(player);
                    case BARRIER -> {
                        houses.sendToHub(player);
                        player.closeInventory();
                    }
                    default -> {
                    }
                }
                return;
            }

            if (TITLE_HOUSES.equals(title)) {
                if (clicked.getType() == Material.ARROW) {
                    openMainMenu(player);
                    return;
                }
                int raw = event.getRawSlot();
                int houseSlot = raw == 10 ? 1 : raw == 13 ? 2 : raw == 16 ? 3 : -1;

                // Right click = icon picker (do not join)
                if (event.isRightClick()) {
                    if (houseSlot == -1) return;
                    if (!houses.houseExists(player.getUniqueId(), com.tommustbe12.housing.houses.HouseSlot.fromIndex(houseSlot))) return;
                    openIconPicker(player, houseSlot);
                    return;
                }

                // Drop key = delete confirm
                if (event.getClick() == org.bukkit.event.inventory.ClickType.DROP) {
                    if (houseSlot == -1) return;
                    if (!houses.houseExists(player.getUniqueId(), com.tommustbe12.housing.houses.HouseSlot.fromIndex(houseSlot))) return;
                    openDeleteConfirm(player, houseSlot);
                    return;
                }

                if (clicked.getType() == Material.OAK_BUTTON || houseSlot != -1) {
                    int raww = event.getRawSlot();
                    houseSlot = raww == 10 ? 1 : raww == 13 ? 2 : raww == 16 ? 3 : -1;
                    if (houseSlot == -1) return;
                    houses.createIfMissing(player.getUniqueId(), com.tommustbe12.housing.houses.HouseSlot.fromIndex(houseSlot));
                    houses.joinHouse(player, player.getUniqueId(), com.tommustbe12.housing.houses.HouseSlot.fromIndex(houseSlot));
                    player.closeInventory();
                }
                return;
            }

            if (TITLE_HOT.equals(title)) {
                if (clicked.getType() == Material.ARROW) {
                    openMainMenu(player);
                    return;
                }
                ItemMeta meta = clicked.getItemMeta();
                if (meta == null) return;
                String ownerStr = meta.getPersistentDataContainer().get(hotOwnerKey, PersistentDataType.STRING);
                Integer slotIndex = meta.getPersistentDataContainer().get(hotSlotKey, PersistentDataType.INTEGER);
                if (ownerStr == null || slotIndex == null) return;
                try {
                    UUID owner = UUID.fromString(ownerStr);
                    var houseSlot = com.tommustbe12.housing.houses.HouseSlot.fromIndex(slotIndex);
                    if (houseSlot == null) return;
                    houses.joinHouse(player, owner, houseSlot);
                    player.closeInventory();
                } catch (IllegalArgumentException ignored) {
                }
            }

            if (TITLE_SYSTEMS.equals(title)) {
                if (clicked.getType() == Material.ARROW) {
                    openMainMenu(player);
                    return;
                }
                if (clicked.getType() == Material.REDSTONE) {
                    openEventActionsMenu(player);
                    return;
                }
                player.sendMessage("§7That system is coming in a later version.");
            }

            if (TITLE_EVENT_ACTIONS.equals(title)) {
                if (clicked.getType() == Material.ARROW) {
                    openSystemsMenu(player);
                    return;
                }
                // Only allow editing actions while inside your own house
                var info = houses.getHouseInfoByWorld(player.getWorld());
                if (info == null || !info.owner().equals(player.getUniqueId())) {
                    player.sendMessage("§cYou can only edit event actions inside your own house.");
                    return;
                }

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
                actionsEditor.openEventActions(player, info.owner(), info.slot(), eventKey);
            }

            if (title.startsWith(TITLE_ICON_PICKER)) {
                if (clicked.getType() == Material.ARROW) {
                    openHousesMenu(player);
                    return;
                }
                // parse slot from title suffix
                int slotIndex = 1;
                int idx = title.indexOf("Slot ");
                if (idx != -1) {
                    try {
                        slotIndex = Integer.parseInt(title.substring(idx + 5, idx + 6));
                    } catch (Exception ignored) {
                    }
                }
                var slot = com.tommustbe12.housing.houses.HouseSlot.fromIndex(slotIndex);
                if (slot == null) return;
                var data = houses.getHouse(player.getUniqueId(), slot);
                data.setIconMaterial(clicked.getType().name());
                houses.saveHouse(data);
                openHousesMenu(player);
            }

            if (title.startsWith(TITLE_DELETE_CONFIRM)) {
                int slotIndex = 1;
                int idx = title.indexOf("Slot ");
                if (idx != -1) {
                    try {
                        slotIndex = Integer.parseInt(title.substring(idx + 5, idx + 6));
                    } catch (Exception ignored) {
                    }
                }
                if (clicked.getType() == Material.RED_CONCRETE) {
                    openHousesMenu(player);
                    return;
                }
                if (clicked.getType() == Material.LIME_CONCRETE) {
                    var slot = com.tommustbe12.housing.houses.HouseSlot.fromIndex(slotIndex);
                    if (slot == null) return;
                    houses.deleteHouse(player.getUniqueId(), slot);
                    player.sendMessage("§aHouse deleted.");
                    openHousesMenu(player);
                }
            }
        }
    }


    @EventHandler
    public void onInvDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (TITLE_MAIN.equals(title)
                || TITLE_HOUSES.equals(title)
                || TITLE_HOT.equals(title)
                || TITLE_SYSTEMS.equals(title)
                || TITLE_EVENT_ACTIONS.equals(title)
                || "Add Action".equals(title)
                || actionsEditor.isEditorTitle(title)
                || title.startsWith(TITLE_ICON_PICKER)
                || title.startsWith(TITLE_DELETE_CONFIRM)) {
            event.setCancelled(true);
        }
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
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, filler);
        }
    }

    private boolean isHousingMenuItem(ItemStack item) {
        if (item == null) return false;
        if (item.getType() != Material.NETHER_STAR) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        Byte marker = meta.getPersistentDataContainer().get(housingItemKey, PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }
}
