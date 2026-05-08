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
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class ActionsEditor {
    private static final String TITLE_PREFIX = "Actions: ";
    private static final String TITLE_ADD = "Add Action";
    private static final String TITLE_PICK_FUNCTION = "Choose Function";
    private static final String TITLE_PICK_LAYOUT = "Choose Layout";
    private static final String TITLE_PICK_MENU = "Choose Custom Menu";
    private static final String TITLE_CHANGE_VARIABLE = "Edit Stat Change";
    private static final String TITLE_PLAY_SOUND = "Play Sound";
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
    }

    public void setConditionalGui(ConditionalGui conditionalGui) {
        this.conditionalGui = conditionalGui;
    }

    public boolean isEditorTitle(String title) {
        return title != null && title.startsWith(TITLE_PREFIX);
    }

    public boolean isAddTitle(String title) {
        return TITLE_ADD.equals(title);
    }

    public boolean isFunctionPickerTitle(String title) {
        return TITLE_PICK_FUNCTION.equals(title);
    }

    public boolean isLayoutPickerTitle(String title) {
        return TITLE_PICK_LAYOUT.equals(title);
    }

    public boolean isMenuPickerTitle(String title) {
        return TITLE_PICK_MENU.equals(title);
    }

    public boolean isChangeVariableTitle(String title) {
        return TITLE_CHANGE_VARIABLE.equals(title);
    }

    public boolean isPlaySoundTitle(String title) {
        return TITLE_PLAY_SOUND.equals(title) || TITLE_PICK_SOUND.equals(title);
    }

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
            openList(player, session);
            return;
        }

        ActionList list = session.list();
        switch (clicked.getType()) {
            case PAPER -> prompt(player, "Enter message:", msg -> {
                list.actions().add(new SendChatMessageAction(placeholders, msg));
                save(session);
                openList(player, session);
            });
            case CLOCK -> prompt(player, "Enter actionbar text:", msg -> {
                list.actions().add(new DisplayActionBarAction(placeholders, msg));
                save(session);
                openList(player, session);
            });
            case NAME_TAG -> prompt(player, "Enter title text (use | for subtitle optional):", msg -> {
                String[] parts = msg.split("\\|", 2);
                list.actions().add(new DisplayTitleAction(placeholders, parts[0], parts.length > 1 ? parts[1] : "", 10, 40, 10));
                save(session);
                openList(player, session);
            });
            case TOTEM_OF_UNDYING -> {
                list.actions().add(new FullHealAction());
                save(session);
                openList(player, session);
            }
            case SKELETON_SKULL -> {
                list.actions().add(new KillPlayerAction());
                save(session);
                openList(player, session);
            }
            case BARRIER -> {
                list.actions().add(new ResetInventoryAction());
                save(session);
                openList(player, session);
            }
            case OAK_DOOR -> {
                list.actions().add(new SendToHubAction(houses));
                save(session);
                openList(player, session);
            }
            case COMPARATOR -> {
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
            case MILK_BUCKET -> {
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
            case REPEATER -> openFunctionPicker(player);
            case CHEST -> openLayoutPicker(player);
            case ITEM_FRAME -> {
                list.actions().add(new OpenCustomMenuAction(new com.tommustbe12.housing.custommenus.CustomMenusService(plugin, houses), null));
                session.replaceIndex = list.actions().size() - 1;
                save(session);
                openMenuPicker(player);
            }
            case NOTE_BLOCK -> {
                list.actions().add(new PlaySoundAction("ENTITY_EXPERIENCE_ORB_PICKUP", 1.0f, 1.0f));
                session.replaceIndex = list.actions().size() - 1;
                session.soundName = "ENTITY_EXPERIENCE_ORB_PICKUP";
                session.soundVolume = 1.0f;
                session.soundPitch = 1.0f;
                session.soundPage = 0;
                save(session);
                openPlaySoundGui(player, session);
            }
            case STRUCTURE_BLOCK -> {
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
        }
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

    public void handleMenuPickerClick(Player player, ItemStack clicked) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;
        if (clicked == null || clicked.getType().isAir()) return;
        if (clicked.getType() == Material.ARROW) {
            openAddPicker(player);
            return;
        }
        UUID id = parseMenuId(clicked);
        if (id == null) return;
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
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_ADD);
        fill(inv);
        inv.setItem(10, named(Material.PAPER, "§eChat Message", List.of("§7Send a message.")));
        inv.setItem(11, named(Material.CLOCK, "§bAction Bar", List.of("§7Display actionbar.")));
        inv.setItem(12, named(Material.NAME_TAG, "§dTitle", List.of("§7Display title/subtitle.")));
        inv.setItem(13, named(Material.TOTEM_OF_UNDYING, "§aFull Heal", List.of("§7Heal player.")));
        inv.setItem(14, named(Material.SKELETON_SKULL, "§cKill Player", List.of("§7Kill player.")));
        inv.setItem(15, named(Material.BARRIER, "§cReset Inventory", List.of("§7Clear inventory.")));
        inv.setItem(16, named(Material.OAK_DOOR, "§aSend To Hub", List.of("§7Teleport to hub.")));
        inv.setItem(19, named(Material.COMPARATOR, "§6Change Variable", List.of("§7Set %stat.x%.")));
        inv.setItem(20, named(Material.EXPERIENCE_BOTTLE, "§aGive Exp Levels", List.of("§7Give levels.")));
        inv.setItem(21, named(Material.MILK_BUCKET, "§bClear Potion Effects", List.of("§7Remove all effects.")));
        inv.setItem(22, named(Material.POTION, "§dApply Potion Effect", List.of("§7Give an effect.")));
        inv.setItem(23, named(Material.REPEATER, "§fRun Function", List.of("§7Pick a function.")));
        inv.setItem(24, named(Material.STRUCTURE_BLOCK, "§eConditional", List.of("§7GUI-based if/else.")));
        inv.setItem(25, named(Material.CHEST, "§6Apply Inventory Layout", List.of("§7Pick a saved layout.")));
        inv.setItem(28, named(Material.ITEM_FRAME, "§aOpen Custom Menu", List.of("§7Pick a custom GUI menu.")));
        inv.setItem(29, named(Material.NOTE_BLOCK, "§aPlay Sound", List.of("§7Configure sound/volume/pitch.")));
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
        org.bukkit.Sound[] sounds = org.bukkit.Sound.values();
        java.util.List<String> names = new java.util.ArrayList<>(sounds.length);
        for (org.bukkit.Sound s : sounds) names.add(s.name());
        names.sort(String.CASE_INSENSITIVE_ORDER);

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
            meta.setLocalizedName("sound:" + n);
            it.setItemMeta(meta);
            inv.setItem(idx++, it);
        }
        inv.setItem(45, named(Material.ARROW, "§7Prev", List.of("§7Page " + (page + 1))));
        inv.setItem(49, named(Material.BARRIER, "§7Back", List.of("§7Return")));
        inv.setItem(53, named(Material.ARROW, "§7Next", List.of("§7Page " + (page + 1))));
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
                openSoundPicker(player, session);
                return;
            }
            if (rawSlot == 13) {
                float delta = (clickType.isShiftClick() ? 1.0f : 0.1f) * (clickType.isRightClick() ? -1.0f : 1.0f);
                session.soundVolume = clamp(session.soundVolume + delta, 0.0f, 10.0f);
                openPlaySoundGui(player, session);
                return;
            }
            if (rawSlot == 15) {
                float delta = (clickType.isShiftClick() ? 1.0f : 0.1f) * (clickType.isRightClick() ? -1.0f : 1.0f);
                session.soundPitch = clamp(session.soundPitch + delta, 0.0f, 5.0f);
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
                openPlaySoundGui(player, session);
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
                String loc = meta.getLocalizedName();
                if (loc == null || !loc.startsWith("sound:")) return;
                session.soundName = loc.substring("sound:".length());
                openPlaySoundGui(player, session);
            }
        }
    }

    private static float clamp(float v, float min, float max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
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
                new com.tommustbe12.housing.custommenus.CustomMenusService(plugin, houses));
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
            case "display_action_bar" -> Material.CLOCK;
            case "display_title" -> Material.NAME_TAG;
            case "full_heal" -> Material.TOTEM_OF_UNDYING;
            case "kill_player" -> Material.SKELETON_SKULL;
            case "reset_inventory" -> Material.BARRIER;
            case "send_to_hub" -> Material.OAK_DOOR;
            case "change_variable" -> Material.COMPARATOR;
            case "give_exp_levels" -> Material.EXPERIENCE_BOTTLE;
            case "clear_potion_effects" -> Material.MILK_BUCKET;
            case "apply_potion_effect" -> Material.POTION;
            case "run_function" -> Material.REPEATER;
            case "conditional" -> Material.STRUCTURE_BLOCK;
            case "apply_inventory_layout" -> Material.CHEST;
            case "open_custom_menu" -> Material.ITEM_FRAME;
            case "play_sound" -> Material.NOTE_BLOCK;
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

    private static UUID parseMenuId(ItemStack item) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        String s = meta.getLocalizedName();
        if (s == null || !s.startsWith("menu:")) return null;
        try {
            return UUID.fromString(s.substring("menu:".length()));
        } catch (Exception e) {
            return null;
        }
    }

    private static void tagMenuId(ItemStack item, UUID id) {
        ItemMeta meta = item.getItemMeta();
        meta.setLocalizedName("menu:" + id);
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
