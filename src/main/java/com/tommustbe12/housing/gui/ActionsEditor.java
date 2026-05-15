package com.tommustbe12.housing.gui;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.ActionList;
import com.tommustbe12.housing.actions.conditions.CompareOp;
import com.tommustbe12.housing.actions.impl.*;
import com.tommustbe12.housing.actions.placeholders.Placeholders;
import com.tommustbe12.housing.actions.placeholders.VariablesStore;
import com.tommustbe12.housing.actions.storage.HouseActionsStorage;
import com.tommustbe12.housing.actions.storage.SimpleActionCodec;
import com.tommustbe12.housing.actions.storage.SimpleActionSerializer;
import com.tommustbe12.housing.chat.ChatPrompts;
import com.tommustbe12.housing.debug.Debug;
import com.tommustbe12.housing.functions.FunctionStorage;
import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.houses.HouseSlot;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class ActionsEditor {
    private static final String TITLE_PREFIX = "Actions: ";
    private static final String TITLE_ADD_PREFIX = "Add Action";
    // Back-compat for older code paths that still use TITLE_ADD.
    private static final String TITLE_ADD = TITLE_ADD_PREFIX;
    private static final String TITLE_PICK_FUNCTION = "Choose Function";
    private static final String TITLE_PICK_LAYOUT = "Choose Layout";
    private static final String TITLE_PICK_MENU = "Choose Custom Menu";
    private static final String TITLE_PICK_TEAM = "Choose Team";
    private static final String TITLE_PICK_GROUP = "Choose Group";
    private static final String TITLE_GIVE_ITEM = "Give Item";
    private static final String TITLE_REMOVE_ITEM = "Remove Item";
    private static final String TITLE_PICK_SLOT = "Pick Slot";
    private static final String TITLE_COMPASS = "Compass Target";
    private static final String TITLE_GAMEMODE = "Pick Gamemode";
    private static final String TITLE_DROP_ITEM = "Drop Item";
    private static final String TITLE_VELOCITY = "Change Velocity";
    private static final String TITLE_LAUNCH = "Launch To Target";
    private static final String TITLE_ENCHANT = "Enchant Held Item";
    private static final String TITLE_RANDOM = "Random Action";
    private static final String TITLE_CHANGE_VARIABLE = "Edit Stat Change";
    private static final String TITLE_PLAY_SOUND = "Play Sound";
    private static final String TITLE_SOUNDS = "Sounds";
    private static final String TITLE_PICK_SOUND = "Choose Sound";

    private final Plugin plugin;
    private final Debug debug;
    private final ChatPrompts prompts;
    private final HouseManager houses;
    private final FunctionStorage functionStorage;
    private final HouseActionsStorage eventStorage;
    private final SimpleActionSerializer serializer = new SimpleActionSerializer();

    private final VariablesStore variables;
    private final Placeholders placeholders;
    private final com.tommustbe12.housing.inventorylayouts.InventoryLayoutsService inventoryLayouts;
    private final NamespacedKey menuPickIdKey;
    private final NamespacedKey soundPickIdKey;

    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
    private ConditionalGui conditionalGui;

    public ActionsEditor(Plugin plugin, Debug debug, ChatPrompts prompts, HouseManager houses) {
        this.plugin = plugin;
        this.debug = debug;
        this.prompts = prompts;
        this.houses = houses;
        this.functionStorage = new FunctionStorage(plugin);
        this.eventStorage = new HouseActionsStorage(plugin);
        this.variables = new VariablesStore(plugin);
        this.placeholders = new Placeholders(variables);
        this.inventoryLayouts = new com.tommustbe12.housing.inventorylayouts.InventoryLayoutsService(plugin);
        this.menuPickIdKey = new NamespacedKey(plugin, "pick_menu_id");
        this.soundPickIdKey = new NamespacedKey(plugin, "pick_sound");
    }

    public void setConditionalGui(ConditionalGui conditionalGui) {
        this.conditionalGui = conditionalGui;
    }

    public boolean isEditorTitle(String title) {
        return title != null && title.startsWith(TITLE_PREFIX);
    }

    public boolean isAddTitle(String title) {
        return title != null && title.startsWith(TITLE_ADD_PREFIX);
    }

    public boolean isFunctionPickerTitle(String title) {
        return TITLE_PICK_FUNCTION.equals(title);
    }

    public boolean isLayoutPickerTitle(String title) {
        return TITLE_PICK_LAYOUT.equals(title);
    }

    public boolean isTeamPickerTitle(String title) {
        return TITLE_PICK_TEAM.equals(title);
    }

    public boolean isGroupPickerTitle(String title) {
        return TITLE_PICK_GROUP.equals(title);
    }

    public boolean isMenuPickerTitle(String title) {
        return TITLE_PICK_MENU.equals(title);
    }

    public boolean isChangeVariableTitle(String title) {
        return TITLE_CHANGE_VARIABLE.equals(title);
    }

    public boolean isPlaySoundTitle(String title) {
        return TITLE_PLAY_SOUND.equals(title) || TITLE_SOUNDS.equals(title) || TITLE_PICK_SOUND.equals(title);
    }

    public boolean isGiveItemTitle(String title) { return TITLE_GIVE_ITEM.equals(title); }
    public boolean isRemoveItemTitle(String title) { return TITLE_REMOVE_ITEM.equals(title); }
    public boolean isPickSlotTitle(String title) { return TITLE_PICK_SLOT.equals(title); }
    public boolean isCompassTitle(String title) { return TITLE_COMPASS.equals(title); }
    public boolean isGamemodeTitle(String title) { return TITLE_GAMEMODE.equals(title); }
    public boolean isDropItemTitle(String title) { return TITLE_DROP_ITEM.equals(title); }
    public boolean isVelocityTitle(String title) { return TITLE_VELOCITY.equals(title); }
    public boolean isLaunchTitle(String title) { return TITLE_LAUNCH.equals(title); }
    public boolean isEnchantTitle(String title) { return TITLE_ENCHANT.equals(title); }
    public boolean isRandomTitle(String title) { return TITLE_RANDOM.equals(title); }

    public void openEventActions(Player player, UUID owner, HouseSlot slot, String eventKey, Runnable back) {
        Session session = Session.forHouseEvent(owner, slot, eventKey.toLowerCase(Locale.ROOT), back,
                new HashMap<>(eventStorage.loadEventActions(owner, slot, newCodec())));
        sessions.put(player.getUniqueId(), session);
        openList(player, session);
    }

    public void openStandalone(Player player, String key, ActionList list, Consumer<ActionList> onSave, Runnable back) {
        Session session = Session.forStandalone(key.toLowerCase(Locale.ROOT), list, onSave, back);
        sessions.put(player.getUniqueId(), session);
        openList(player, session);
    }

    public void openStandaloneHouse(Player player, UUID owner, HouseSlot slot, String key, ActionList list, Consumer<ActionList> onSave, Runnable back) {
        Session session = Session.forStandaloneHouse(owner, slot, key.toLowerCase(Locale.ROOT), list, onSave, back);
        sessions.put(player.getUniqueId(), session);
        openList(player, session);
    }

    public void handleClick(Player player, String title, int rawSlot, ItemStack clicked, org.bukkit.event.inventory.ClickType clickType, Runnable fallbackBack) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;

        if (rawSlot == 53) {
            sessions.remove(player.getUniqueId());
            (session.back != null ? session.back : fallbackBack).run();
            return;
        }
        if (rawSlot == 49) {
            openAddPicker(player);
            return;
        }

        if (rawSlot >= 0 && rawSlot < 45) {
            ActionList list = session.list();
            if (rawSlot >= list.actions().size()) return;
            if (clickType.isRightClick()) {
                list.actions().remove(rawSlot);
                save(session);
                openList(player, session);
                return;
            }
            if (clickType.isLeftClick()) {
                editAction(player, session, rawSlot, list.actions().get(rawSlot));
            }
        }
    }

    public void handleAddPickerClick(Player player, ItemStack clicked) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;
        if (clicked == null || clicked.getType().isAir()) return;

        if (clicked.getType() == Material.ARROW) {
            // In the Add Action GUI, slot 53 is used for page navigation.
            String t = player.getOpenInventory().getTitle();
            if (t != null && t.startsWith(TITLE_ADD_PREFIX)) {
                int raw = player.getOpenInventory().getTopInventory().first(clicked);
                if (raw == 53) {
                    boolean page2 = t.contains("(2/2)");
                    openAddPicker(player, page2 ? 1 : 2);
                    return;
                }
            }
            openList(player, session);
            return;
        }

        String title = player.getOpenInventory().getTitle();
        boolean page2 = title != null && title.startsWith(TITLE_ADD_PREFIX) && title.contains("(2/2)");

        ActionList list = session.list();
        switch (clicked.getType()) {
            case FILLED_MAP -> prompt(player, "Enter message:", msg -> {
                list.actions().add(new SendChatMessageAction(placeholders, msg));
                save(session);
                openList(player, session);
            });
            case WRITABLE_BOOK -> prompt(player, "Enter actionbar text:", msg -> {
                list.actions().add(new DisplayActionBarAction(placeholders, msg));
                save(session);
                openList(player, session);
            });
            case BOOK -> prompt(player, "Enter title text (use | for subtitle optional):", msg -> {
                String[] parts = msg.split("\\|", 2);
                list.actions().add(new DisplayTitleAction(placeholders, parts[0], parts.length > 1 ? parts[1] : "", 10, 40, 10));
                save(session);
                openList(player, session);
            });
            case GOLDEN_APPLE -> {
                list.actions().add(new FullHealAction());
                save(session);
                openList(player, session);
            }
            case SKELETON_SKULL -> {
                list.actions().add(new KillPlayerAction());
                save(session);
                openList(player, session);
            }
            case STONE -> {
                list.actions().add(new ResetInventoryAction());
                save(session);
                openList(player, session);
            }
            case OAK_DOOR -> {
                list.actions().add(new SendToHubAction(houses));
                save(session);
                openList(player, session);
            }
            case FEATHER -> {
                ChangeVariableAction action = new ChangeVariableAction(variables, placeholders, "%stat.kills%", "1", ChangeVariableAction.Operation.ADD);
                list.actions().add(action);
                session.replaceIndex = list.actions().size() - 1;
                session.varKey = action.key();
                session.varValue = action.value();
                session.varOp = action.operation();
                save(session);
                openChangeVariableGui(player, session);
            }
            case EXPERIENCE_BOTTLE -> prompt(player, "Enter levels (number):", msg -> {
                list.actions().add(new GiveExpLevelsAction(Integer.parseInt(msg.trim())));
                save(session);
                openList(player, session);
            });
            case GLASS_BOTTLE -> {
                list.actions().add(new ClearPotionEffectsAction());
                save(session);
                openList(player, session);
            }
            case POTION -> prompt(player, "Enter effect,durationTicks,amplifier (ex: SPEED,200,1):", msg -> {
                String[] parts = msg.split(",", 3);
                if (parts.length < 3) return;
                list.actions().add(new ApplyPotionEffectAction(parts[0].trim(), Integer.parseInt(parts[1].trim()), Integer.parseInt(parts[2].trim())));
                save(session);
                openList(player, session);
            });
            case ACTIVATOR_RAIL -> openFunctionPicker(player);
            case IRON_AXE -> openLayoutPicker(player);
            case OAK_SIGN -> openTeamPicker(player);
            case CHEST -> {
                // Page 1: Give Item editor. Page 2: Display Menu (custom menu picker).
                if (page2) {
                    list.actions().add(new OpenCustomMenuAction(new com.tommustbe12.housing.custommenus.CustomMenusService(plugin, houses), null));
                    session.replaceIndex = list.actions().size() - 1;
                    save(session);
                    openMenuPicker(player);
                } else {
                    session.replaceIndex = null;
                    session.giveItemStack = null;
                    session.giveAmount = 1;
                    session.giveSlot = null;
                    session.giveReplace = false;
                    openGiveItemGui(player, session);
                }
            }
            case NOTE_BLOCK -> {
                list.actions().add(new PlaySoundAction("ENTITY_EXPERIENCE_ORB_PICKUP", 1.0f, 1.0f));
                session.replaceIndex = list.actions().size() - 1;
                session.soundName = "ENTITY_EXPERIENCE_ORB_PICKUP";
                session.soundVolume = 1.0f;
                session.soundPitch = 1.0f;
                session.soundPage = 0;
                session.soundCategory = null;
                session.soundQuery = "";
                save(session);
                openPlaySoundGui(player, session);
            }
            case REDSTONE -> {
                if (conditionalGui == null) {
                    player.sendMessage("§cConditional editor not available.");
                    return;
                }
                ConditionalAction cond = new ConditionalAction(placeholders, List.of(), false, new ActionList(), new ActionList());
                list.actions().add(cond);
                int idx = list.actions().size() - 1;
                conditionalGui.open(player, session.owner, session.slot, cond, updated -> {
                    list.actions().set(idx, updated);
                    save(session);
                }, () -> openList(player, session));
            }
            case PLAYER_HEAD -> openGroupPicker(player);
            case DANDELION -> prompt(player, "Set max health:", msg -> {
                list.actions().add(new ChangeMaxHealthAction(Double.parseDouble(msg.trim())));
                save(session);
                openList(player, session);
            });
            case HOPPER -> {
                session.replaceIndex = null;
                session.removeItemStack = null;
                session.removeAmount = 1;
                openRemoveItemGui(player, session);
            }
            case APPLE -> prompt(player, "Set health:", msg -> {
                list.actions().add(new ChangeHealthAction(Double.parseDouble(msg.trim())));
                save(session);
                openList(player, session);
            });
            case BEEF -> prompt(player, "Set hunger (0-20):", msg -> {
                list.actions().add(new ChangeHungerLevelAction(Integer.parseInt(msg.trim())));
                save(session);
                openList(player, session);
            });
            case ENDER_PEARL -> {
                // Default to house spawn, editable later.
                list.actions().add(new TeleportPlayerAction(houses, TeleportPlayerAction.Mode.HOUSE_SPAWN, 0, 0, 0, 0f, 0f));
                save(session);
                openList(player, session);
            }
            case RED_BED -> prompt(player, "Pause ticks:", msg -> {
                list.actions().add(new PauseExecutionAction(Long.parseLong(msg.trim())));
                save(session);
                openList(player, session);
            });
            case COMPASS -> openCompassGui(player, session);
            case GRASS_BLOCK -> openGamemodeGui(player, session);
            case DISPENSER -> {
                // Page 1 uses dispenser for Random Action, page 2 uses dispenser for Drop Item.
                String t = player.getOpenInventory().getTitle();
                boolean page2 = t != null && t.startsWith(TITLE_ADD_PREFIX) && t.contains("(2/2)");
                if (page2) {
                    session.dropItemStack = null;
                    session.dropAmount = 1;
                    session.dropWhere = DropItemAction.Where.PLAYER;
                    session.dropX = session.dropY = session.dropZ = 0;
                    openDropItemGui(player, session);
                } else {
                    session.randomTypes.clear();
                    openRandomGui(player, session);
                }
            }
            case SLIME_BLOCK -> { session.velX = 0; session.velY = 0; session.velZ = 0; openVelocityGui(player, session); }
            case FIREWORK_ROCKET -> { session.launchTarget = LaunchToTargetAction.Target.EDITOR; session.launchStrength = 1.0; openLaunchGui(player, session); }
            case ENCHANTED_BOOK -> { session.enchantKey = "sharpness"; session.enchantLevel = 1; openEnchantGui(player, session); }
        }
    }

    private void openGiveItemGui(Player player, Session session) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_GIVE_ITEM);
        fill(inv);
        inv.setItem(11, named(Material.PAPER, "§ePick Item", List.of("§7Place the item in the center slot.")));
        inv.setItem(13, session.giveItemStack == null ? null : session.giveItemStack);
        inv.setItem(15, named(Material.NAME_TAG, "§aAmount: §f" + session.giveAmount, List.of("§7Click to edit in chat.")));
        inv.setItem(21, named(Material.CHEST, "§bSlot: §f" + (session.giveSlot == null ? "Any" : session.giveSlot), List.of("§7Click to pick slot.")));
        inv.setItem(22, named(Material.LEVER, "§dReplace Slot: §f" + (session.giveReplace ? "ON" : "OFF"), List.of("§7Click to toggle.")));
        inv.setItem(23, named(Material.LIME_CONCRETE, "§aDone", List.of("§7Save action.")));
        inv.setItem(26, named(Material.ARROW, "§7Back", List.of("§7Cancel.")));
        player.openInventory(inv);
    }

    private void openRemoveItemGui(Player player, Session session) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_REMOVE_ITEM);
        fill(inv);
        inv.setItem(11, named(Material.PAPER, "§ePick Item", List.of("§7Place the item in the center slot.")));
        inv.setItem(13, session.removeItemStack == null ? null : session.removeItemStack);
        inv.setItem(15, named(Material.NAME_TAG, "§aAmount: §f" + session.removeAmount, List.of("§7Click to edit in chat.")));
        inv.setItem(23, named(Material.LIME_CONCRETE, "§aDone", List.of("§7Save action.")));
        inv.setItem(26, named(Material.ARROW, "§7Back", List.of("§7Cancel.")));
        player.openInventory(inv);
    }

    private void openPickSlotGui(Player player, Session session) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_PICK_SLOT);
        fill(inv);
        for (int i = 0; i < 45; i++) inv.setItem(i, named(Material.GRAY_STAINED_GLASS_PANE, "§7Slot " + i, List.of("§7Click to select.")));
        inv.setItem(49, named(Material.ARROW, "§7Back", List.of("§7Return.")));
        player.openInventory(inv);
    }

    private void openCompassGui(Player player, Session session) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_COMPASS);
        fill(inv);
        inv.setItem(13, named(Material.COMPASS, "§eDirection: §f" + session.compassDir.name(), List.of("§7Click to cycle.")));
        inv.setItem(22, named(Material.LIME_CONCRETE, "§aDone", List.of("§7Add action.")));
        inv.setItem(26, named(Material.ARROW, "§7Back", List.of("§7Cancel.")));
        player.openInventory(inv);
    }

    private void openGamemodeGui(Player player, Session session) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_GAMEMODE);
        fill(inv);
        inv.setItem(10, named(Material.GRASS_BLOCK, "§aSURVIVAL", List.of("§7Click to select")));
        inv.setItem(12, named(Material.ENDER_EYE, "§bADVENTURE", List.of("§7Click to select")));
        inv.setItem(14, named(Material.FEATHER, "§eSPECTATOR", List.of("§7Click to select")));
        inv.setItem(16, named(Material.DIAMOND_PICKAXE, "§dCREATIVE", List.of("§7Click to select")));
        inv.setItem(26, named(Material.ARROW, "§7Back", List.of("§7Cancel.")));
        player.openInventory(inv);
    }

    private void openDropItemGui(Player player, Session session) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_DROP_ITEM);
        fill(inv);
        inv.setItem(11, named(Material.PAPER, "§ePick Item", List.of("§7Place the item in the center slot.")));
        inv.setItem(13, session.dropItemStack == null ? null : session.dropItemStack);
        inv.setItem(15, named(Material.NAME_TAG, "§aAmount: §f" + session.dropAmount, List.of("§7Click to edit in chat.")));
        inv.setItem(21, named(Material.COMPASS, "§bWhere: §f" + session.dropWhere.name(), List.of("§7Click to cycle.")));
        inv.setItem(22, named(Material.OAK_SIGN, "§eCoords: §f" + session.dropX + "," + session.dropY + "," + session.dropZ, List.of("§7Used only for COORDS.", "§7Click to edit in chat.")));
        inv.setItem(23, named(Material.LIME_CONCRETE, "§aDone", List.of("§7Add action.")));
        inv.setItem(26, named(Material.ARROW, "§7Back", List.of("§7Cancel.")));
        player.openInventory(inv);
    }

    private void openVelocityGui(Player player, Session session) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_VELOCITY);
        fill(inv);
        inv.setItem(13, named(Material.SLIME_BLOCK, "§aVelocity", List.of("§7Current: §f" + session.velX + "," + session.velY + "," + session.velZ, "§7Click to edit in chat.")));
        inv.setItem(22, named(Material.LIME_CONCRETE, "§aDone", List.of("§7Add action.")));
        inv.setItem(26, named(Material.ARROW, "§7Back", List.of("§7Cancel.")));
        player.openInventory(inv);
    }

    private void openLaunchGui(Player player, Session session) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_LAUNCH);
        fill(inv);
        inv.setItem(11, named(Material.COMPASS, "§eTarget: §f" + session.launchTarget.name(), List.of("§7Click to cycle.")));
        inv.setItem(13, named(Material.OAK_SIGN, "§eCoords", List.of("§7" + session.launchX + "," + session.launchY + "," + session.launchZ, "§7Used only for COORDS.", "§7Click to edit in chat.")));
        inv.setItem(15, named(Material.NAME_TAG, "§aStrength: §f" + session.launchStrength, List.of("§7Click to edit in chat.")));
        inv.setItem(22, named(Material.LIME_CONCRETE, "§aDone", List.of("§7Add action.")));
        inv.setItem(26, named(Material.ARROW, "§7Back", List.of("§7Cancel.")));
        player.openInventory(inv);
    }

    private void openEnchantGui(Player player, Session session) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_ENCHANT);
        fill(inv);
        inv.setItem(11, named(Material.ENCHANTED_BOOK, "§dEnchant", List.of("§7Key: §f" + session.enchantKey, "§7Click to edit in chat (ex: sharpness).")));
        inv.setItem(15, named(Material.NAME_TAG, "§aLevel: §f" + session.enchantLevel, List.of("§7Click to edit in chat.")));
        inv.setItem(22, named(Material.LIME_CONCRETE, "§aDone", List.of("§7Add action.")));
        inv.setItem(26, named(Material.ARROW, "§7Back", List.of("§7Cancel.")));
        player.openInventory(inv);
    }

    private void openRandomGui(Player player, Session session) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_RANDOM);
        fill(inv);
        String current = session.randomTypes.isEmpty() ? "(none)" : String.join(",", session.randomTypes);
        inv.setItem(13, named(Material.DISPENSER, "§eRandom Pool", List.of("§7" + current, "§7Click to toggle safe defaults.")));
        inv.setItem(22, named(Material.LIME_CONCRETE, "§aDone", List.of("§7Add action.")));
        inv.setItem(26, named(Material.ARROW, "§7Back", List.of("§7Cancel.")));
        player.openInventory(inv);
    }

    public void handleFunctionPickerClick(Player player, ItemStack clicked, org.bukkit.event.inventory.ClickType clickType) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;
        if (clicked == null || clicked.getType().isAir()) return;
        if (clicked.getType() == Material.ARROW) {
            openAddPicker(player);
            return;
        }
        if (clicked.getType() != Material.BOOK) return;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || meta.getDisplayName() == null) return;
        String name = stripColor(meta.getDisplayName());
        boolean global = clickType.isRightClick();
        session.list().actions().add(new RunFunctionAction((ctx, fn, g) -> {}, name, global));
        save(session);
        openList(player, session);
    }

    public void handleLayoutPickerClick(Player player, ItemStack clicked) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;
        if (clicked == null || clicked.getType().isAir()) return;
        if (clicked.getType() == Material.ARROW) {
            openAddPicker(player);
            return;
        }
        if (clicked.getType() != Material.CHEST) return;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || meta.getDisplayName() == null) return;
        String pickedName = stripColor(meta.getDisplayName());
        com.tommustbe12.housing.inventorylayouts.InventoryLayout picked = null;
        for (com.tommustbe12.housing.inventorylayouts.InventoryLayout l : inventoryLayouts.get(session.owner, session.slot)) {
            if (l.name().equalsIgnoreCase(pickedName)) { picked = l; break; }
        }
        if (picked == null) return;
        session.list().actions().add(new ApplyInventoryLayoutAction(inventoryLayouts, picked.id()));
        save(session);
        openList(player, session);
    }

    public void handleTeamPickerClick(Player player, ItemStack clicked) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;
        if (clicked == null || clicked.getType().isAir()) return;
        if (clicked.getType() == Material.ARROW) {
            openAddPicker(player);
            return;
        }
        if (clicked.getType() != Material.WHITE_BANNER) return;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || meta.getDisplayName() == null) return;
        String pickedName = stripColor(meta.getDisplayName());
        com.tommustbe12.housing.teams.TeamsService teams = new com.tommustbe12.housing.teams.TeamsService(plugin);
        com.tommustbe12.housing.teams.HouseTeam picked = null;
        for (com.tommustbe12.housing.teams.HouseTeam t : teams.list(session.owner, session.slot)) {
            if (t != null && t.name().equalsIgnoreCase(pickedName)) { picked = t; break; }
        }
        if (picked == null) return;
        session.list().actions().add(new ChangeTeamAction(teams, picked.id()));
        save(session);
        openList(player, session);
    }

    public void handleGroupPickerClick(Player player, ItemStack clicked) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;
        if (clicked == null || clicked.getType().isAir()) return;
        if (clicked.getType() == Material.ARROW) {
            openAddPicker(player);
            return;
        }
        if (clicked.getType() != Material.PLAYER_HEAD) return;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || meta.getDisplayName() == null) return;
        String pickedName = stripColor(meta.getDisplayName());
        com.tommustbe12.housing.groups.HouseGroupsService groups = new com.tommustbe12.housing.groups.HouseGroupsService(plugin, houses);
        var data = groups.groups(session.owner, session.slot);
        com.tommustbe12.housing.groups.HouseGroup picked = null;
        for (var g : data.groups().values()) {
            if (g != null && g.name().equalsIgnoreCase(pickedName)) { picked = g; break; }
        }
        if (picked == null) return;
        session.list().actions().add(new com.tommustbe12.housing.actions.impl.ChangeGroupAction(groups, picked.id()));
        save(session);
        openList(player, session);
    }

    public void handleGiveItemClick(Player player, int rawSlot, ItemStack clicked) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;
        if (clicked == null || clicked.getType().isAir()) return;
        if (!TITLE_GIVE_ITEM.equals(player.getOpenInventory().getTitle())) return;

        if (clicked.getType() == Material.ARROW) { openAddPicker(player); return; }
        if (clicked.getType() == Material.LEVER) {
            session.giveReplace = !session.giveReplace;
            openGiveItemGui(player, session);
            return;
        }
        if (clicked.getType() == Material.CHEST) {
            openPickSlotGui(player, session);
            return;
        }
        if (clicked.getType() == Material.NAME_TAG) {
            prompt(player, "Amount:", msg -> {
                session.giveAmount = Math.max(1, Integer.parseInt(msg.trim()));
                openGiveItemGui(player, session);
            });
            return;
        }
        if (clicked.getType() == Material.LIME_CONCRETE) {
            // read selected item from slot 13
            ItemStack picked = player.getOpenInventory().getTopInventory().getItem(13);
            if (picked == null || picked.getType().isAir()) {
                player.sendMessage("§cPut an item in the middle slot.");
                return;
            }
            session.list().actions().add(new GiveItemAction(picked.clone(), session.giveAmount, session.giveSlot, session.giveReplace));
            save(session);
            openList(player, session);
        }
    }

    public void handleRemoveItemClick(Player player, ItemStack clicked) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;
        if (clicked == null || clicked.getType().isAir()) return;
        if (!TITLE_REMOVE_ITEM.equals(player.getOpenInventory().getTitle())) return;

        if (clicked.getType() == Material.ARROW) { openAddPicker(player); return; }
        if (clicked.getType() == Material.NAME_TAG) {
            prompt(player, "Amount:", msg -> {
                session.removeAmount = Math.max(1, Integer.parseInt(msg.trim()));
                openRemoveItemGui(player, session);
            });
            return;
        }
        if (clicked.getType() == Material.LIME_CONCRETE) {
            ItemStack picked = player.getOpenInventory().getTopInventory().getItem(13);
            if (picked == null || picked.getType().isAir()) {
                player.sendMessage("§cPut an item in the middle slot.");
                return;
            }
            session.list().actions().add(new RemoveItemAction(picked.clone(), session.removeAmount));
            save(session);
            openList(player, session);
        }
    }

    public void handlePickSlotClick(Player player, int rawSlot, ItemStack clicked) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;
        if (clicked == null || clicked.getType().isAir()) return;
        if (!TITLE_PICK_SLOT.equals(player.getOpenInventory().getTitle())) return;
        if (clicked.getType() == Material.ARROW) { openGiveItemGui(player, session); return; }
        if (rawSlot >= 0 && rawSlot < 45) {
            session.giveSlot = rawSlot;
            openGiveItemGui(player, session);
        }
    }

    public void handleCompassClick(Player player, ItemStack clicked) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;
        if (!TITLE_COMPASS.equals(player.getOpenInventory().getTitle())) return;
        if (clicked == null || clicked.getType().isAir()) return;
        if (clicked.getType() == Material.ARROW) { openAddPicker(player); return; }
        if (clicked.getType() == Material.COMPASS) {
            SetCompassTargetAction.Direction[] vals = SetCompassTargetAction.Direction.values();
            int idx = (session.compassDir.ordinal() + 1) % vals.length;
            session.compassDir = vals[idx];
            openCompassGui(player, session);
            return;
        }
        if (clicked.getType() == Material.LIME_CONCRETE) {
            session.list().actions().add(new SetCompassTargetAction(session.compassDir));
            save(session);
            openList(player, session);
        }
    }

    public void handleGamemodeClick(Player player, ItemStack clicked) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;
        if (!TITLE_GAMEMODE.equals(player.getOpenInventory().getTitle())) return;
        if (clicked == null || clicked.getType().isAir()) return;
        if (clicked.getType() == Material.ARROW) { openAddPicker(player); return; }
        org.bukkit.GameMode picked = null;
        if (clicked.getType() == Material.GRASS_BLOCK) picked = org.bukkit.GameMode.SURVIVAL;
        else if (clicked.getType() == Material.ENDER_EYE) picked = org.bukkit.GameMode.ADVENTURE;
        else if (clicked.getType() == Material.FEATHER) picked = org.bukkit.GameMode.SPECTATOR;
        else if (clicked.getType() == Material.DIAMOND_PICKAXE) picked = org.bukkit.GameMode.CREATIVE;
        if (picked == null) return;
        session.list().actions().add(new SetGamemodeAction(new com.tommustbe12.housing.groups.HouseGroupsService(plugin, houses), picked));
        save(session);
        openList(player, session);
    }

    public void handleDropItemClick(Player player, ItemStack clicked) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;
        if (!TITLE_DROP_ITEM.equals(player.getOpenInventory().getTitle())) return;
        if (clicked == null || clicked.getType().isAir()) return;

        if (clicked.getType() == Material.ARROW) { openAddPicker(player); return; }
        if (clicked.getType() == Material.NAME_TAG) {
            prompt(player, "Amount:", msg -> {
                session.dropAmount = Math.max(1, Integer.parseInt(msg.trim()));
                openDropItemGui(player, session);
            });
            return;
        }
        if (clicked.getType() == Material.COMPASS) {
            DropItemAction.Where[] vals = DropItemAction.Where.values();
            int idx = (session.dropWhere.ordinal() + 1) % vals.length;
            session.dropWhere = vals[idx];
            openDropItemGui(player, session);
            return;
        }
        if (clicked.getType() == Material.OAK_SIGN) {
            prompt(player, "Coords x,y,z:", msg -> {
                String[] p = msg.split(",", 3);
                session.dropX = Double.parseDouble(p[0].trim());
                session.dropY = Double.parseDouble(p[1].trim());
                session.dropZ = Double.parseDouble(p[2].trim());
                openDropItemGui(player, session);
            });
            return;
        }
        if (clicked.getType() == Material.LIME_CONCRETE) {
            ItemStack picked = player.getOpenInventory().getTopInventory().getItem(13);
            if (picked == null || picked.getType().isAir()) {
                player.sendMessage("§cPut an item in the middle slot.");
                return;
            }
            session.list().actions().add(new DropItemAction(houses, picked.clone(), session.dropAmount, session.dropWhere, session.dropX, session.dropY, session.dropZ));
            save(session);
            openList(player, session);
        }
    }

    public void handleVelocityClick(Player player, ItemStack clicked) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;
        if (!TITLE_VELOCITY.equals(player.getOpenInventory().getTitle())) return;
        if (clicked == null || clicked.getType().isAir()) return;
        if (clicked.getType() == Material.ARROW) { openAddPicker(player); return; }
        if (clicked.getType() == Material.SLIME_BLOCK) {
            prompt(player, "Velocity x,y,z:", msg -> {
                String[] p = msg.split(",", 3);
                session.velX = Double.parseDouble(p[0].trim());
                session.velY = Double.parseDouble(p[1].trim());
                session.velZ = Double.parseDouble(p[2].trim());
                openVelocityGui(player, session);
            });
            return;
        }
        if (clicked.getType() == Material.LIME_CONCRETE) {
            session.list().actions().add(new ChangeVelocityAction(session.velX, session.velY, session.velZ));
            save(session);
            openList(player, session);
        }
    }

    public void handleLaunchClick(Player player, ItemStack clicked) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;
        if (!TITLE_LAUNCH.equals(player.getOpenInventory().getTitle())) return;
        if (clicked == null || clicked.getType().isAir()) return;
        if (clicked.getType() == Material.ARROW) { openAddPicker(player); return; }
        if (clicked.getType() == Material.COMPASS) {
            LaunchToTargetAction.Target[] vals = LaunchToTargetAction.Target.values();
            int idx = (session.launchTarget.ordinal() + 1) % vals.length;
            session.launchTarget = vals[idx];
            openLaunchGui(player, session);
            return;
        }
        if (clicked.getType() == Material.OAK_SIGN) {
            prompt(player, "Coords x,y,z:", msg -> {
                String[] p = msg.split(",", 3);
                session.launchX = Double.parseDouble(p[0].trim());
                session.launchY = Double.parseDouble(p[1].trim());
                session.launchZ = Double.parseDouble(p[2].trim());
                openLaunchGui(player, session);
            });
            return;
        }
        if (clicked.getType() == Material.NAME_TAG) {
            prompt(player, "Strength:", msg -> {
                session.launchStrength = Double.parseDouble(msg.trim());
                openLaunchGui(player, session);
            });
            return;
        }
        if (clicked.getType() == Material.LIME_CONCRETE) {
            session.list().actions().add(new LaunchToTargetAction(houses, session.launchTarget, session.launchX, session.launchY, session.launchZ, session.launchStrength));
            save(session);
            openList(player, session);
        }
    }

    public void handleEnchantClick(Player player, ItemStack clicked) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;
        if (!TITLE_ENCHANT.equals(player.getOpenInventory().getTitle())) return;
        if (clicked == null || clicked.getType().isAir()) return;
        if (clicked.getType() == Material.ARROW) { openAddPicker(player); return; }
        if (clicked.getType() == Material.ENCHANTED_BOOK) {
            prompt(player, "Enchant key (ex: sharpness):", msg -> {
                session.enchantKey = msg.trim().toLowerCase(java.util.Locale.ROOT);
                openEnchantGui(player, session);
            });
            return;
        }
        if (clicked.getType() == Material.NAME_TAG) {
            prompt(player, "Level:", msg -> {
                session.enchantLevel = Math.max(1, Integer.parseInt(msg.trim()));
                openEnchantGui(player, session);
            });
            return;
        }
        if (clicked.getType() == Material.LIME_CONCRETE) {
            session.list().actions().add(new EnchantHeldItemAction(session.enchantKey, session.enchantLevel));
            save(session);
            openList(player, session);
        }
    }

    public void handleRandomClick(Player player, ItemStack clicked) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;
        if (!TITLE_RANDOM.equals(player.getOpenInventory().getTitle())) return;
        if (clicked == null || clicked.getType().isAir()) return;
        if (clicked.getType() == Material.ARROW) { openAddPicker(player); return; }
        if (clicked.getType() == Material.DISPENSER) {
            // Toggle a small safe pool
            if (session.randomTypes.isEmpty()) session.randomTypes.addAll(java.util.List.of("full_heal", "give_exp_levels", "clear_potion_effects"));
            else session.randomTypes.clear();
            openRandomGui(player, session);
            return;
        }
        if (clicked.getType() == Material.LIME_CONCRETE) {
            session.list().actions().add(new RandomAction(session.randomTypes));
            save(session);
            openList(player, session);
        }
    }

    public void handleMenuPickerClick(Player player, ItemStack clicked) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;
        if (clicked == null || clicked.getType().isAir()) return;
        if (clicked.getType() == Material.ARROW) {
            openAddPicker(player);
            return;
        }
        UUID id = parseMenuId(clicked);
        if (id == null) {
            player.sendMessage("§cCouldn't read menu id from that item.");
            return;
        }
        if (session.replaceIndex != null && session.replaceIndex >= 0 && session.replaceIndex < session.list().actions().size()) {
            session.list().actions().set(session.replaceIndex, new OpenCustomMenuAction(new com.tommustbe12.housing.custommenus.CustomMenusService(plugin, houses), id));
            session.replaceIndex = null;
        } else {
            session.list().actions().add(new OpenCustomMenuAction(new com.tommustbe12.housing.custommenus.CustomMenusService(plugin, houses), id));
        }
        save(session);
        openList(player, session);
    }

    private void openList(Player player, Session session) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_PREFIX + session.key);
        fill(inv);
        int i = 0;
        for (Action action : session.list().actions()) {
            inv.setItem(i++, actionItem(action));
            if (i >= 45) break;
        }
        inv.setItem(49, named(Material.ANVIL, "§aAdd Action", List.of("§7Click to add.")));
        inv.setItem(53, named(Material.ARROW, "§7Back", List.of("§7Return.")));
        player.openInventory(inv);
    }

    private void openAddPicker(Player player) {
        openAddPicker(player, 1);
    }

    private void openAddPicker(Player player, int page) {
        int p = page <= 1 ? 1 : 2;
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_ADD_PREFIX + " (" + p + "/2)");
        fill(inv);

        if (p == 1) {
            inv.setItem(10, named(Material.REDSTONE, "§eConditional", List.of("§7GUI-based if/else.")));
            inv.setItem(11, named(Material.PLAYER_HEAD, "§eChange Player's Group", List.of("§7Pick a group for the player.")));
            inv.setItem(12, named(Material.GOLDEN_APPLE, "§aFull Heal", List.of("§7Heal player.")));
            inv.setItem(13, named(Material.BOOK, "§dDisplay Title", List.of("§7Display title/subtitle.")));
            inv.setItem(14, named(Material.WRITABLE_BOOK, "§bDisplay Action Bar", List.of("§7Display actionbar.")));
            inv.setItem(15, named(Material.STONE, "§cReset Inventory", List.of("§7Clear inventory.")));
            inv.setItem(16, named(Material.DANDELION, "§eChange Max Health", List.of("§7Set max health.")));
            inv.setItem(19, named(Material.CHEST, "§aGive Item", List.of("§7GUI item picker.")));
            inv.setItem(20, named(Material.HOPPER, "§cRemove Item", List.of("§7GUI item picker.")));
            inv.setItem(21, named(Material.FILLED_MAP, "§eChat Message", List.of("§7Send a message.")));
            inv.setItem(22, named(Material.POTION, "§dApply Potion Effect", List.of("§7GUI-based effect editor.")));
            inv.setItem(23, named(Material.GLASS_BOTTLE, "§bClear Potion Effects", List.of("§7Remove all effects.")));
            inv.setItem(24, named(Material.EXPERIENCE_BOTTLE, "§aGive Exp Levels", List.of("§7Give levels.")));
            inv.setItem(25, named(Material.FEATHER, "§6Change Variable", List.of("§7Set %stat.x%.")));
            inv.setItem(26, named(Material.ENDER_PEARL, "§bTeleport Player", List.of("§7Teleport editor.")));
            inv.setItem(28, named(Material.NOTE_BLOCK, "§aPlay Sound", List.of("§7Configure sound/volume/pitch.")));
            inv.setItem(29, named(Material.COMPASS, "§eSet Compass Target", List.of("§7GUI-based direction.")));
            inv.setItem(30, named(Material.GRASS_BLOCK, "§eSet Gamemode", List.of("§7GUI-based.")));
            inv.setItem(31, named(Material.APPLE, "§cChange Health", List.of("§7Set health.")));
            inv.setItem(32, named(Material.BEEF, "§6Change Hunger Level", List.of("§7Set food level.")));
            inv.setItem(33, named(Material.DISPENSER, "§eRandom Action", List.of("§7Configure random picker.")));
            inv.setItem(53, named(Material.ARROW, "§7Next Page", List.of("§7Go to page 2.")));
        } else {
            inv.setItem(10, named(Material.ACTIVATOR_RAIL, "§fTrigger Function", List.of("§7Pick a function.")));
            inv.setItem(11, named(Material.IRON_AXE, "§6Apply Inventory Layout", List.of("§7Pick a saved layout.")));
            inv.setItem(12, named(Material.ENCHANTED_BOOK, "§dEnchant Held Item", List.of("§7Pick enchant + level.")));
            inv.setItem(13, named(Material.RED_BED, "§cPause Execution", List.of("§7Wait ticks, then continue.")));
            inv.setItem(14, named(Material.OAK_SIGN, "§bSet Player Team", List.of("§7Pick a team for the player.")));
            inv.setItem(15, named(Material.CHEST, "§aDisplay Menu", List.of("§7Pick a custom GUI menu.")));
            inv.setItem(16, named(Material.DISPENSER, "§eDrop Item", List.of("§7Drop item editor.")));
            inv.setItem(19, named(Material.SLIME_BLOCK, "§aChange Velocity", List.of("§7Set velocity vector.")));
            inv.setItem(20, named(Material.FIREWORK_ROCKET, "§bLaunch To Target", List.of("§7Launch towards a target.")));
            inv.setItem(53, named(Material.ARROW, "§7Prev Page", List.of("§7Back to page 1.")));
        }

        inv.setItem(49, named(Material.ARROW, "§7Back", List.of("§7Return.")));
        player.openInventory(inv);
    }

    private void openLayoutPicker(Player player) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;
        if (session.owner.getMostSignificantBits() == 0L && session.owner.getLeastSignificantBits() == 0L) {
            player.sendMessage("§cInventory Layouts require a house context.");
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_PICK_LAYOUT);
        fill(inv);
        int i = 0;
        for (com.tommustbe12.housing.inventorylayouts.InventoryLayout l : inventoryLayouts.get(session.owner, session.slot)) {
            inv.setItem(i++, named(Material.CHEST, "§e" + l.name(), List.of("§7Click to select")));
            if (i >= 45) break;
        }
        inv.setItem(49, named(Material.ARROW, "§7Back", List.of("§7Return.")));
        player.openInventory(inv);
    }

    private void openTeamPicker(Player player) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;
        if (session.owner.getMostSignificantBits() == 0L && session.owner.getLeastSignificantBits() == 0L) {
            player.sendMessage("§cTeams require a house context.");
            return;
        }
        com.tommustbe12.housing.teams.TeamsService teams = new com.tommustbe12.housing.teams.TeamsService(plugin);
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_PICK_TEAM);
        fill(inv);
        int i = 0;
        for (com.tommustbe12.housing.teams.HouseTeam t : teams.list(session.owner, session.slot)) {
            if (t == null) continue;
            inv.setItem(i++, named(Material.WHITE_BANNER, "§b" + t.name(), List.of("§7Click to select")));
            if (i >= 45) break;
        }
        inv.setItem(49, named(Material.ARROW, "§7Back", List.of("§7Return.")));
        player.openInventory(inv);
    }

    private void openGroupPicker(Player player) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;
        if (session.owner.getMostSignificantBits() == 0L && session.owner.getLeastSignificantBits() == 0L) {
            player.sendMessage("§cGroups require a house context.");
            return;
        }
        com.tommustbe12.housing.groups.HouseGroupsService groups = new com.tommustbe12.housing.groups.HouseGroupsService(plugin, houses);
        var data = groups.groups(session.owner, session.slot);
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_PICK_GROUP);
        fill(inv);
        int i = 0;
        for (var g : data.groupsInEditorOrder()) {
            if (g == null) continue;
            inv.setItem(i++, named(Material.PLAYER_HEAD, "§f" + g.name(), List.of("§7Click to select")));
            if (i >= 45) break;
        }
        inv.setItem(49, named(Material.ARROW, "§7Back", List.of("§7Return.")));
        player.openInventory(inv);
    }

    private void openFunctionPicker(Player player) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;
        if (session.kind == Kind.STANDALONE && session.owner.getMostSignificantBits() == 0L && session.owner.getLeastSignificantBits() == 0L) {
            player.sendMessage("§cRun Function is only available for house events/functions.");
            return;
        }
        Map<String, ActionList> funcs = functionStorage.loadAll(session.owner, session.slot, newCodec());
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_PICK_FUNCTION);
        fill(inv);
        int i = 0;
        for (String name : new TreeSet<>(funcs.keySet())) {
            inv.setItem(i++, named(Material.BOOK, "§f" + name, List.of("§7Left-click: personal", "§7Right-click: global")));
            if (i >= 45) break;
        }
        inv.setItem(49, named(Material.ARROW, "§7Back", List.of("§7Return.")));
        player.openInventory(inv);
    }

    private void openMenuPicker(Player player) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;
        if (session.owner.getMostSignificantBits() == 0L && session.owner.getLeastSignificantBits() == 0L) {
            player.sendMessage("§cCustom Menus require a house context.");
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_PICK_MENU);
        fill(inv);
        int i = 0;
        var list = new com.tommustbe12.housing.custommenus.CustomMenusService(plugin, houses).get(session.owner, session.slot);
        for (var m : list) {
            ItemStack it = named(Material.ITEM_FRAME, "§a" + m.name(), List.of("§7Rows: §f" + m.rows(), "§7Click to select"));
            tagMenuId(it, m.id());
            inv.setItem(i++, it);
            if (i >= 45) break;
        }
        inv.setItem(49, named(Material.ARROW, "§7Back", List.of("§7Return.")));
        player.openInventory(inv);
    }

    private void openChangeVariableGui(Player player, Session session) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_CHANGE_VARIABLE);
        fill(inv);

        String opName = session.varOp == null ? ChangeVariableAction.Operation.ADD.name() : session.varOp.name();
        String key = session.varKey == null ? "" : session.varKey;
        String value = session.varValue == null ? "1" : session.varValue;

        inv.setItem(11, named(Material.COMPARATOR, "§eOperation: §f" + opName, List.of("§7Click to cycle", "§7ADD/SUBTRACT/SET")));
        inv.setItem(13, named(Material.NAME_TAG, "§bStat Key", List.of("§7Current: §f" + key, "§7Click to change")));
        inv.setItem(15, named(Material.EXPERIENCE_BOTTLE, "§aAmount", List.of("§7Current: §f" + value, "§7Click to change")));
        inv.setItem(22, named(Material.LIME_CONCRETE, "§aDone", List.of("§7Save and return")));
        inv.setItem(26, named(Material.ARROW, "§7Back", List.of("§7Cancel/return")));
        player.openInventory(inv);
    }

    public void handleChangeVariableClick(Player player, int rawSlot, ItemStack clicked) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;
        if (!TITLE_CHANGE_VARIABLE.equals(player.getOpenInventory().getTitle())) return;

        if (rawSlot == 26) {
            openList(player, session);
            return;
        }
        if (rawSlot == 11) {
            ChangeVariableAction.Operation current = session.varOp == null ? ChangeVariableAction.Operation.ADD : session.varOp;
            ChangeVariableAction.Operation next = switch (current) {
                case ADD -> ChangeVariableAction.Operation.SUBTRACT;
                case SUBTRACT -> ChangeVariableAction.Operation.SET;
                case SET -> ChangeVariableAction.Operation.ADD;
            };
            session.varOp = next;
            openChangeVariableGui(player, session);
            return;
        }
        if (rawSlot == 13) {
            prompt(player, "Type stat key (ex: %stat.kills%) or 'cancel':", msg -> {
                String trimmed = msg.trim();
                if (trimmed.equalsIgnoreCase("cancel")) return;
                session.varKey = trimmed;
                openChangeVariableGui(player, session);
            });
            return;
        }
        if (rawSlot == 15) {
            prompt(player, "Type amount (number) or 'cancel':", msg -> {
                String trimmed = msg.trim();
                if (trimmed.equalsIgnoreCase("cancel")) return;
                session.varValue = trimmed.isBlank() ? "1" : trimmed;
                openChangeVariableGui(player, session);
            });
            return;
        }
        if (rawSlot == 22) {
            int idx = session.replaceIndex == null ? -1 : session.replaceIndex;
            if (idx < 0 || idx >= session.list().actions().size()) {
                openList(player, session);
                return;
            }
            ChangeVariableAction.Operation op = session.varOp == null ? ChangeVariableAction.Operation.ADD : session.varOp;
            String key = session.varKey == null ? "" : session.varKey;
            String value = session.varValue == null ? "1" : session.varValue;
            session.list().actions().set(idx, new ChangeVariableAction(variables, placeholders, key, value, op));
            session.replaceIndex = null;
            save(session);
            openList(player, session);
        }
    }

    private void openPlaySoundGui(Player player, Session session) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_PLAY_SOUND);
        fill(inv);

        String sound = session.soundName == null ? "" : session.soundName;
        float volume = session.soundVolume;
        float pitch = session.soundPitch;

        inv.setItem(11, named(Material.NOTE_BLOCK, "§bSound", List.of("§7Current: §f" + sound, "§7Click to pick")));
        inv.setItem(13, named(Material.AMETHYST_SHARD, "§aVolume", List.of("§7Current: §f" + volume, "§7Left: +0.1  Right: -0.1", "§7Shift: +/-1.0")));
        inv.setItem(15, named(Material.PRISMARINE_SHARD, "§dPitch", List.of("§7Current: §f" + pitch, "§7Left: +0.1  Right: -0.1", "§7Shift: +/-1.0")));
        inv.setItem(22, named(Material.LIME_CONCRETE, "§aDone", List.of("§7Save and return")));
        inv.setItem(26, named(Material.ARROW, "§7Back", List.of("§7Return")));
        player.openInventory(inv);
    }

    private void openSoundPicker(Player player, Session session) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_PICK_SOUND);
        fill(inv);
        java.util.List<String> names = collectSounds(session);
        names.sort(String.CASE_INSENSITIVE_ORDER);

        String header = "§7Showing: §f"
                + (session.soundCategory == null ? "All" : session.soundCategory.name())
                + "§7  Search: §f" + (session.soundQuery == null ? "" : session.soundQuery);
        inv.setItem(50, named(Material.PAPER, "§fFilters", java.util.List.of(header, "§7Back clears filters")));

        int page = Math.max(0, session.soundPage);
        int perPage = 45;
        int start = page * perPage;
        if (start >= names.size()) { page = 0; start = 0; }
        session.soundPage = page;

        int idx = 0;
        for (int i = start; i < Math.min(names.size(), start + perPage); i++) {
            String n = names.get(i);
            ItemStack it = named(Material.NOTE_BLOCK, "§f" + n, java.util.List.of("§7Click to select"));
            ItemMeta meta = it.getItemMeta();
            meta.getPersistentDataContainer().set(soundPickIdKey, PersistentDataType.STRING, n);
            it.setItemMeta(meta);
            inv.setItem(idx++, it);
        }
        inv.setItem(45, named(Material.ARROW, "§7Prev", List.of("§7Page " + (page + 1))));
        inv.setItem(49, named(Material.BARRIER, "§7Back", List.of("§7Return")));
        inv.setItem(53, named(Material.ARROW, "§7Next", List.of("§7Page " + (page + 1))));
        player.openInventory(inv);
    }

    private void openSoundsHome(Player player, Session session) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_SOUNDS);
        fill(inv);
        inv.setItem(10, named(Material.WITHER_ROSE, "§cMobs", List.of("§7ENTITY_* (mob-ish)")));
        inv.setItem(11, named(Material.GRASS_BLOCK, "§aBlocks", List.of("§7BLOCK_*")));
        inv.setItem(12, named(Material.CHEST, "§eItems", List.of("§7ITEM_*")));
        inv.setItem(13, named(Material.NOTE_BLOCK, "§dAmbient", List.of("§7AMBIENT_*")));
        inv.setItem(14, named(Material.WATER_BUCKET, "§bWeather", List.of("§7WEATHER_*")));
        inv.setItem(15, named(Material.JUKEBOX, "§fMusic", List.of("§7MUSIC_*")));
        inv.setItem(16, named(Material.REDSTONE, "§6UI", List.of("§7UI_*")));
        inv.setItem(31, named(Material.SPYGLASS, "§aSearch", List.of("§7Click to type query")));
        inv.setItem(49, named(Material.BARRIER, "§7Back", List.of("§7Return")));
        player.openInventory(inv);
    }

    public void handlePlaySoundClick(Player player, String title, int rawSlot, ItemStack clicked, org.bukkit.event.inventory.ClickType clickType) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;

        if (TITLE_PLAY_SOUND.equals(title)) {
            if (rawSlot == 26) {
                openList(player, session);
                return;
            }
            if (rawSlot == 11) {
                session.soundPage = 0;
                openSoundsHome(player, session);
                return;
            }
            if (rawSlot == 13) {
                float delta = (clickType.isShiftClick() ? 1.0f : 0.1f) * (clickType.isRightClick() ? -1.0f : 1.0f);
                session.soundVolume = round1(clamp(session.soundVolume + delta, 0.0f, 2.0f));
                openPlaySoundGui(player, session);
                return;
            }
            if (rawSlot == 15) {
                float delta = (clickType.isShiftClick() ? 1.0f : 0.1f) * (clickType.isRightClick() ? -1.0f : 1.0f);
                session.soundPitch = round1(clamp(session.soundPitch + delta, 0.5f, 2.0f));
                openPlaySoundGui(player, session);
                return;
            }
            if (rawSlot == 22) {
                int idx = session.replaceIndex == null ? -1 : session.replaceIndex;
                if (idx < 0 || idx >= session.list().actions().size()) {
                    openList(player, session);
                    return;
                }
                String sound = session.soundName == null ? "" : session.soundName;
                session.list().actions().set(idx, new PlaySoundAction(sound, session.soundVolume, session.soundPitch));
                session.replaceIndex = null;
                save(session);
                openList(player, session);
            }
            return;
        }

        if (TITLE_PICK_SOUND.equals(title)) {
            if (rawSlot == 49) {
                // Leaving the picker clears search/category so next time you aren't restricted.
                session.soundCategory = null;
                session.soundQuery = "";
                session.soundPage = 0;
                openSoundsHome(player, session);
                return;
            }
            if (rawSlot == 45) {
                session.soundPage = Math.max(0, session.soundPage - 1);
                openSoundPicker(player, session);
                return;
            }
            if (rawSlot == 53) {
                session.soundPage = session.soundPage + 1;
                openSoundPicker(player, session);
                return;
            }
            if (rawSlot >= 0 && rawSlot < 45) {
                ItemMeta meta = clicked.getItemMeta();
                if (meta == null) return;
                String picked = meta.getPersistentDataContainer().get(soundPickIdKey, PersistentDataType.STRING);
                if (picked == null || picked.isBlank()) return;
                session.soundName = picked;
                openPlaySoundGui(player, session);
            }
            return;
        }

        if (TITLE_SOUNDS.equals(title)) {
            if (rawSlot == 49) {
                openPlaySoundGui(player, session);
                return;
            }
            if (rawSlot == 31) {
                prompt(player, "Search sounds (type text, or 'cancel'):", msg -> {
                    if (msg.equalsIgnoreCase("cancel")) return;
                    session.soundCategory = null;
                    session.soundQuery = msg.trim();
                    session.soundPage = 0;
                    // Search shows the matching list immediately (not the category menu)
                    openSoundPicker(player, session);
                });
                return;
            }
            if (rawSlot == 10) { session.soundCategory = SoundCategory.MOBS; session.soundPage = 0; openSoundPicker(player, session); return; }
            if (rawSlot == 11) { session.soundCategory = SoundCategory.BLOCKS; session.soundPage = 0; openSoundPicker(player, session); return; }
            if (rawSlot == 12) { session.soundCategory = SoundCategory.ITEMS; session.soundPage = 0; openSoundPicker(player, session); return; }
            if (rawSlot == 13) { session.soundCategory = SoundCategory.AMBIENT; session.soundPage = 0; openSoundPicker(player, session); return; }
            if (rawSlot == 14) { session.soundCategory = SoundCategory.WEATHER; session.soundPage = 0; openSoundPicker(player, session); return; }
            if (rawSlot == 15) { session.soundCategory = SoundCategory.MUSIC; session.soundPage = 0; openSoundPicker(player, session); return; }
            if (rawSlot == 16) { session.soundCategory = SoundCategory.UI; session.soundPage = 0; openSoundPicker(player, session); return; }
        }
    }

    private static float clamp(float v, float min, float max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }

    private static float round1(float v) {
        return Math.round(v * 10.0f) / 10.0f;
    }

    private enum SoundCategory { MOBS, BLOCKS, ITEMS, AMBIENT, WEATHER, MUSIC, UI }

    private java.util.List<String> collectSounds(Session session) {
        org.bukkit.Sound[] sounds = org.bukkit.Sound.values();
        java.util.List<String> names = new java.util.ArrayList<>(sounds.length);
        for (org.bukkit.Sound s : sounds) names.add(s.name());

        // Category filter (optional)
        if (session.soundCategory != null) {
            String prefix = switch (session.soundCategory) {
                case BLOCKS -> "BLOCK_";
                case ITEMS -> "ITEM_";
                case AMBIENT -> "AMBIENT_";
                case WEATHER -> "WEATHER_";
                case MUSIC -> "MUSIC_";
                case UI -> "UI_";
                case MOBS -> "ENTITY_";
            };
            names.removeIf(n -> !n.startsWith(prefix));
        }

        // Search filter (optional, case-insensitive)
        String q = session.soundQuery;
        if (q != null && !q.isBlank()) {
            String needle = q.trim().toLowerCase(java.util.Locale.ROOT);
            names.removeIf(n -> !n.toLowerCase(java.util.Locale.ROOT).contains(needle));
        }

        // If MOBS category and ENTITY_ produced nothing (older enums), fall back to unfiltered list+search
        if (names.isEmpty() && session.soundCategory != null) {
            names.clear();
            for (org.bukkit.Sound s : sounds) names.add(s.name());
            if (q != null && !q.isBlank()) {
                String needle = q.trim().toLowerCase(java.util.Locale.ROOT);
                names.removeIf(n -> !n.toLowerCase(java.util.Locale.ROOT).contains(needle));
            }
        }
        return names;
    }

    private void editAction(Player player, Session session, int index, Action action) {
        ActionList list = session.list();
        if (action instanceof SendChatMessageAction) {
            prompt(player, "Edit message:", msg -> {
                list.actions().set(index, new SendChatMessageAction(placeholders, msg));
                save(session);
                openList(player, session);
            });
            return;
        }
        if (action instanceof DisplayActionBarAction) {
            prompt(player, "Edit actionbar:", msg -> {
                list.actions().set(index, new DisplayActionBarAction(placeholders, msg));
                save(session);
                openList(player, session);
            });
            return;
        }
        if (action instanceof DisplayTitleAction) {
            prompt(player, "Edit title (use | for subtitle):", msg -> {
                String[] parts = msg.split("\\|", 2);
                list.actions().set(index, new DisplayTitleAction(placeholders, parts[0], parts.length > 1 ? parts[1] : "", 10, 40, 10));
                save(session);
                openList(player, session);
            });
            return;
        }
        if (action instanceof ChangeVariableAction) {
            ChangeVariableAction v = (ChangeVariableAction) action;
            session.replaceIndex = index;
            session.varKey = v.key();
            session.varValue = v.value();
            session.varOp = v.operation();
            openChangeVariableGui(player, session);
            return;
        }
        if (action instanceof GiveExpLevelsAction) {
            prompt(player, "Edit levels:", msg -> {
                list.actions().set(index, new GiveExpLevelsAction(Integer.parseInt(msg.trim())));
                save(session);
                openList(player, session);
            });
            return;
        }
        if (action instanceof ChangeMaxHealthAction) {
            ChangeMaxHealthAction mh = (ChangeMaxHealthAction) action;
            prompt(player, "Set max health:", msg -> {
                list.actions().set(index, new ChangeMaxHealthAction(Double.parseDouble(msg.trim())));
                save(session);
                openList(player, session);
            });
            return;
        }
        if (action instanceof ChangeHealthAction) {
            prompt(player, "Set health:", msg -> {
                list.actions().set(index, new ChangeHealthAction(Double.parseDouble(msg.trim())));
                save(session);
                openList(player, session);
            });
            return;
        }
        if (action instanceof ChangeHungerLevelAction) {
            prompt(player, "Set hunger (0-20):", msg -> {
                list.actions().set(index, new ChangeHungerLevelAction(Integer.parseInt(msg.trim())));
                save(session);
                openList(player, session);
            });
            return;
        }
        if (action instanceof PauseExecutionAction) {
            PauseExecutionAction p = (PauseExecutionAction) action;
            prompt(player, "Pause ticks:", msg -> {
                list.actions().set(index, new PauseExecutionAction(Long.parseLong(msg.trim())));
                save(session);
                openList(player, session);
            });
            return;
        }
        if (action instanceof TeleportPlayerAction) {
            TeleportPlayerAction tp = (TeleportPlayerAction) action;
            prompt(player, "Teleport mode (HOUSE_SPAWN|CURRENT_EDITOR|COORDS) or coords x,y,z:", msg -> {
                String raw = msg.trim();
                if (raw.contains(",")) {
                    String[] parts = raw.split(",", 3);
                    double x = Double.parseDouble(parts[0].trim());
                    double y = Double.parseDouble(parts[1].trim());
                    double z = Double.parseDouble(parts[2].trim());
                    list.actions().set(index, new TeleportPlayerAction(houses, TeleportPlayerAction.Mode.COORDS, x, y, z, 0f, 0f));
                } else {
                    TeleportPlayerAction.Mode mode;
                    try { mode = TeleportPlayerAction.Mode.valueOf(raw.toUpperCase(Locale.ROOT)); } catch (Exception e) { mode = TeleportPlayerAction.Mode.HOUSE_SPAWN; }
                    list.actions().set(index, new TeleportPlayerAction(houses, mode, tp.x(), tp.y(), tp.z(), tp.yaw(), tp.pitch()));
                }
                save(session);
                openList(player, session);
            });
            return;
        }
        if (action instanceof ApplyPotionEffectAction) {
            prompt(player, "Edit effect,duration,amp:", msg -> {
                String[] parts = msg.split(",", 3);
                if (parts.length < 3) return;
                list.actions().set(index, new ApplyPotionEffectAction(parts[0].trim(), Integer.parseInt(parts[1].trim()), Integer.parseInt(parts[2].trim())));
                save(session);
                openList(player, session);
            });
            return;
        }
        if (action instanceof RunFunctionAction fn) {
            boolean nextGlobal = !fn.global();
            list.actions().set(index, new RunFunctionAction((ctx, f, g) -> {}, fn.functionName(), nextGlobal));
            save(session);
            openList(player, session);
            return;
        }
        if (action instanceof ConditionalAction cond) {
            if (conditionalGui == null) {
                player.sendMessage("§cConditional editor not available.");
                return;
            }
            conditionalGui.open(player, session.owner, session.slot, cond, updated -> {
                list.actions().set(index, updated);
                save(session);
            }, () -> openList(player, session));
            return;
        }
        if (action instanceof OpenCustomMenuAction) {
            session.replaceIndex = index;
            openMenuPicker(player);
            return;
        }
        if (action instanceof PlaySoundAction s) {
            session.replaceIndex = index;
            session.soundName = s.sound();
            session.soundVolume = s.volume();
            session.soundPitch = s.pitch();
            session.soundPage = 0;
            session.soundCategory = null;
            session.soundQuery = "";
            openPlaySoundGui(player, session);
            return;
        }
        player.sendMessage("§7No editable values for this action.");
    }

    private void save(Session session) {
        if (session.kind == Kind.STANDALONE) {
            session.onSave.accept(session.standalone);
            return;
        }
        eventStorage.saveEventActions(session.owner, session.slot, session.events, serializer);
    }

    private SimpleActionCodec newCodec() {
        return new SimpleActionCodec(placeholders, variables, houses, (ctx, fn, global) -> {}, inventoryLayouts,
                new com.tommustbe12.housing.custommenus.CustomMenusService(plugin, houses),
                new com.tommustbe12.housing.teams.TeamsService(plugin),
                new com.tommustbe12.housing.groups.HouseGroupsService(plugin, houses));
    }

    private void prompt(Player player, String question, Consumer<String> onOk) {
        prompts.prompt(player, question, msg -> {
            if (msg.equalsIgnoreCase("cancel")) return;
            Bukkit.getScheduler().runTask(plugin, () -> onOk.accept(msg));
        });
    }

    private static ItemStack actionItem(Action action) {
        Material mat = switch (action.type()) {
            case "chat_message" -> Material.PAPER;
            case "display_action_bar" -> Material.WRITABLE_BOOK;
            case "display_title" -> Material.BOOK;
            case "full_heal" -> Material.GOLDEN_APPLE;
            case "kill_player" -> Material.SKELETON_SKULL;
            case "reset_inventory" -> Material.STONE;
            case "send_to_hub" -> Material.OAK_DOOR;
            case "change_variable" -> Material.FEATHER;
            case "give_exp_levels" -> Material.EXPERIENCE_BOTTLE;
            case "clear_potion_effects" -> Material.MILK_BUCKET;
            case "apply_potion_effect" -> Material.POTION;
            case "run_function" -> Material.ACTIVATOR_RAIL;
            case "conditional" -> Material.REDSTONE;
            case "apply_inventory_layout" -> Material.IRON_AXE;
            case "open_custom_menu" -> Material.CHEST;
            case "play_sound" -> Material.NOTE_BLOCK;
            case "change_team" -> Material.OAK_SIGN;
            case "change_group" -> Material.PLAYER_HEAD;
            case "pause_execution" -> Material.RED_BED;
            case "teleport_player" -> Material.ENDER_PEARL;
            case "change_max_health" -> Material.DANDELION;
            case "change_health" -> Material.APPLE;
            case "change_hunger" -> Material.BEEF;
            case "give_item" -> Material.CHEST;
            case "remove_item" -> Material.HOPPER;
            case "set_compass_target" -> Material.COMPASS;
            case "set_gamemode" -> Material.GRASS_BLOCK;
            case "drop_item" -> Material.DISPENSER;
            case "change_velocity" -> Material.SLIME_BLOCK;
            case "launch_to_target" -> Material.FIREWORK_ROCKET;
            case "enchant_held_item" -> Material.ENCHANTED_BOOK;
            case "random_action" -> Material.DISPENSER;
            default -> Material.BOOK;
        };
        List<String> lore = new ArrayList<>();
        lore.add("§7Left-click to edit");
        lore.add("§7Right-click to remove");
        lore.addAll(previewLore(action));
        return named(mat, "§f" + action.type(), lore);
    }

    private static List<String> previewLore(Action action) {
        List<String> lore = new ArrayList<>();
        if (action instanceof SendChatMessageAction msg) {
            lore.add("§7msg: §f" + ChatColor.translateAlternateColorCodes('&', msg.message()));
        } else if (action instanceof DisplayActionBarAction bar) {
            lore.add("§7bar: §f" + ChatColor.translateAlternateColorCodes('&', bar.message()));
        } else if (action instanceof DisplayTitleAction t) {
            lore.add("§7title: §f" + ChatColor.translateAlternateColorCodes('&', t.title()));
            if (!t.subtitle().isBlank()) lore.add("§7sub: §f" + ChatColor.translateAlternateColorCodes('&', t.subtitle()));
        } else if (action instanceof ChangeVariableAction v) {
            String op = switch (v.operation()) {
                case ADD -> "+=";
                case SUBTRACT -> "-=";
                case SET -> "=";
            };
            lore.add("§7stat: §f" + v.key());
            lore.add("§7op: §f" + op + " " + v.value());
        } else if (action instanceof GiveExpLevelsAction exp) {
            lore.add("§7levels: §f" + exp.levels());
        } else if (action instanceof ApplyPotionEffectAction pot) {
            lore.add("§7effect: §f" + pot.effect() + " §7dur: §f" + pot.durationTicks() + " §7amp: §f" + pot.amplifier());
        } else if (action instanceof RunFunctionAction fn) {
            lore.add("§7fn: §f" + fn.functionName() + " §7scope: §f" + (fn.global() ? "global" : "personal"));
        } else if (action instanceof ConditionalAction cond) {
            lore.add("§7conditions: §f" + cond.conditions().size() + " §7mode: §f" + (cond.matchAny() ? "ANY" : "ALL"));
            lore.add("§7then: §f" + cond.thenList().actions().size() + " §7else: §f" + (cond.elseList() == null ? 0 : cond.elseList().actions().size()));
        } else if (action instanceof ApplyInventoryLayoutAction inv) {
            lore.add("§7layout: §f" + (inv.layoutId() == null ? "" : inv.layoutId().toString()));
        } else if (action instanceof OpenCustomMenuAction menu) {
            lore.add("§7menu: §f" + (menu.menuId() == null ? "" : menu.menuId().toString()));
        } else if (action instanceof PlaySoundAction s) {
            lore.add("§7sound: §f" + s.sound());
            lore.add("§7vol: §f" + s.volume() + " §7pitch: §f" + s.pitch());
        }
        if (action instanceof ChangeTeamAction t) {
            lore.add("§7teamId: §f" + (t.teamId() == null ? "" : t.teamId().toString()));
        }
        return lore;
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

    private static String stripColor(String s) {
        return s.replaceAll("§.", "");
    }

    private UUID parseMenuId(ItemStack item) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        String raw = meta.getPersistentDataContainer().get(menuPickIdKey, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) return null;
        try {
            return UUID.fromString(raw);
        } catch (Exception e) {
            return null;
        }
    }

    private void tagMenuId(ItemStack item, UUID id) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(menuPickIdKey, PersistentDataType.STRING, id.toString());
        item.setItemMeta(meta);
    }

    private enum Kind { HOUSE_EVENT, STANDALONE }

    private static final class Session {
        private final Kind kind;
        private final String key;
        private final UUID owner;
        private final HouseSlot slot;
        private final Map<String, ActionList> events;
        private final ActionList standalone;
        private final Consumer<ActionList> onSave;
        private final Runnable back;
        private Integer replaceIndex;
        private String varKey;
        private String varValue;
        private ChangeVariableAction.Operation varOp;
        private String soundName;
        private float soundVolume = 1.0f;
        private float soundPitch = 1.0f;
        private int soundPage;
        private SoundCategory soundCategory;
        private String soundQuery;

        private ItemStack giveItemStack;
        private int giveAmount = 1;
        private Integer giveSlot;
        private boolean giveReplace;

        private ItemStack removeItemStack;
        private int removeAmount = 1;

        private SetCompassTargetAction.Direction compassDir = SetCompassTargetAction.Direction.N;
        private org.bukkit.GameMode gamemode = org.bukkit.GameMode.ADVENTURE;

        private ItemStack dropItemStack;
        private int dropAmount = 1;
        private DropItemAction.Where dropWhere = DropItemAction.Where.PLAYER;
        private double dropX, dropY, dropZ;

        private double velX, velY, velZ;

        private LaunchToTargetAction.Target launchTarget = LaunchToTargetAction.Target.EDITOR;
        private double launchX, launchY, launchZ;
        private double launchStrength = 1.0;

        private String enchantKey = "sharpness";
        private int enchantLevel = 1;

        private java.util.List<String> randomTypes = new java.util.ArrayList<>();

        private Session(Kind kind, String key, UUID owner, HouseSlot slot, Map<String, ActionList> events, ActionList standalone, Consumer<ActionList> onSave, Runnable back) {
            this.kind = kind;
            this.key = key;
            this.owner = owner;
            this.slot = slot;
            this.events = events;
            this.standalone = standalone;
            this.onSave = onSave;
            this.back = back;
        }

        static Session forHouseEvent(UUID owner, HouseSlot slot, String eventKey, Runnable back, Map<String, ActionList> events) {
            return new Session(Kind.HOUSE_EVENT, eventKey, owner, slot, events, null, null, back);
        }

        static Session forStandalone(String key, ActionList list, Consumer<ActionList> onSave, Runnable back) {
            return new Session(Kind.STANDALONE, key, new UUID(0L, 0L), HouseSlot.SLOT_1, new HashMap<>(), list, onSave, back);
        }

        static Session forStandaloneHouse(UUID owner, HouseSlot slot, String key, ActionList list, Consumer<ActionList> onSave, Runnable back) {
            return new Session(Kind.STANDALONE, key, owner, slot, new HashMap<>(), list, onSave, back);
        }

        ActionList list() {
            return kind == Kind.STANDALONE ? standalone : events.computeIfAbsent(key, k -> new ActionList());
        }
    }
}
