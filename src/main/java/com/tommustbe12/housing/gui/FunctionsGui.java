package com.tommustbe12.housing.gui;

import com.tommustbe12.housing.actions.ActionList;
import com.tommustbe12.housing.actions.placeholders.Placeholders;
import com.tommustbe12.housing.actions.placeholders.VariablesStore;
import com.tommustbe12.housing.actions.storage.SimpleActionCodec;
import com.tommustbe12.housing.actions.storage.SimpleActionSerializer;
import com.tommustbe12.housing.chat.ChatPrompts;
import com.tommustbe12.housing.debug.Debug;
import com.tommustbe12.housing.functions.FunctionStorage;
import com.tommustbe12.housing.groups.HouseGroupsService;
import com.tommustbe12.housing.groups.HousePermission;
import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.houses.HouseSlot;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class FunctionsGui {
    private static final String TITLE_LIST = "Functions";
    private static final String TITLE_CONFIRM = "Delete Function?";

    private final Plugin plugin;
    private final Debug debug;
    private final ChatPrompts prompts;
    private final ActionsEditor actionsEditor;
    private final HouseManager houses;
    private final HouseGroupsService groups;
    private final FunctionStorage storage;
    private final SimpleActionSerializer serializer = new SimpleActionSerializer();
    private final SimpleActionCodec codec;

    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    public FunctionsGui(Plugin plugin, Debug debug, ChatPrompts prompts, ActionsEditor actionsEditor, HouseManager houses, HouseGroupsService groups) {
        this.plugin = plugin;
        this.debug = debug;
        this.prompts = prompts;
        this.actionsEditor = actionsEditor;
        this.houses = houses;
        this.groups = groups;
        this.storage = new FunctionStorage(plugin);
        VariablesStore vars = new VariablesStore(plugin);
        Placeholders placeholders = new Placeholders(vars);
        this.codec = new SimpleActionCodec(placeholders, vars, houses, (ctx, fn, global) -> {},
                new com.tommustbe12.housing.inventorylayouts.InventoryLayoutsService(plugin),
                new com.tommustbe12.housing.custommenus.CustomMenusService(plugin, houses),
                new com.tommustbe12.housing.teams.TeamsService(plugin));
    }

    public boolean isTitle(String title) {
        return TITLE_LIST.equals(title) || title.startsWith(TITLE_LIST + " (") || title.startsWith(TITLE_CONFIRM);
    }

    public void open(Player player) {
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null) {
            player.sendMessage("§cYou can only edit functions inside a house.");
            return;
        }

        boolean inOwn = info.owner().equals(player.getUniqueId());
        if (!inOwn && (groups == null || !groups.has(info.owner(), info.slot(), player.getUniqueId(), HousePermission.EDIT_FUNCTIONS))) {
            player.sendMessage("§cYou don't have permission to edit functions in this house.");
            return;
        }

        Session session = new Session(info.owner(), info.slot());
        sessions.put(player.getUniqueId(), session);

        Map<String, ActionList> funcs = storage.loadAll(info.owner(), info.slot(), codec);
        session.functions.clear();
        session.functions.putAll(funcs);

        Inventory inv = Bukkit.createInventory(null, 54, TITLE_LIST);
        fill(inv);

        int i = 0;
        for (String name : new TreeSet<>(session.functions.keySet())) {
            ActionList list = session.functions.get(name);
            inv.setItem(i++, named(Material.BOOK, "§f" + name, List.of(
                    "§7Actions: §b" + list.actions().size(),
                    "§7Left-click: edit",
                    "§7Right-click: rename",
                    "§7Drop-key: delete"
            )));
            if (i >= 45) break;
        }

        inv.setItem(49, named(Material.ANVIL, "§aCreate Function", List.of("§7Click to create.")));
        inv.setItem(53, named(Material.ARROW, "§7Back", List.of("§7Return to Systems.")));
        player.openInventory(inv);
    }

    public void handleClick(Player player, String title, int rawSlot, ItemStack clicked, org.bukkit.event.inventory.ClickType clickType, Runnable backToSystems) {
        if (!TITLE_LIST.equals(title) && !title.startsWith(TITLE_CONFIRM)) return;
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;

        if (TITLE_LIST.equals(title)) {
            if (rawSlot == 53) {
                sessions.remove(player.getUniqueId());
                backToSystems.run();
                return;
            }
            if (rawSlot == 49) {
                prompts.prompt(player, "Enter function name:", msg -> {
                    if (msg.equalsIgnoreCase("cancel")) return;
                    String name = sanitizeName(msg);
                    if (name.isBlank()) return;
                    session.functions.putIfAbsent(name, new ActionList());
                    save(session);
                    Bukkit.getScheduler().runTask(plugin, () -> open(player));
                });
                return;
            }

            if (rawSlot >= 0 && rawSlot < 45) {
                ItemMeta meta = clicked.getItemMeta();
                if (meta == null || meta.getDisplayName() == null) return;
                String name = stripColor(meta.getDisplayName());
                if (!session.functions.containsKey(name)) return;

                if (clickType == org.bukkit.event.inventory.ClickType.DROP) {
                    openDeleteConfirm(player, name);
                    return;
                }
                if (clickType.isRightClick()) {
                    prompts.prompt(player, "Rename function '" + name + "' to:", msg -> {
                        if (msg.equalsIgnoreCase("cancel")) return;
                        String newName = sanitizeName(msg);
                        if (newName.isBlank()) return;
                        ActionList list = session.functions.remove(name);
                        session.functions.put(newName, list);
                        save(session);
                        Bukkit.getScheduler().runTask(plugin, () -> open(player));
                    });
                    return;
                }
                if (clickType.isLeftClick()) {
                    ActionList list = session.functions.get(name);
                    actionsEditor.openStandalone(player, "function:" + name, list, updated -> {
                        session.functions.put(name, updated);
                        save(session);
                    }, () -> open(player));
                }
            }
        }

        if (title.startsWith(TITLE_CONFIRM)) {
            String name = title.substring(TITLE_CONFIRM.length()).trim();
            if (clicked.getType() == Material.RED_CONCRETE) {
                open(player);
                return;
            }
            if (clicked.getType() == Material.LIME_CONCRETE) {
                session.functions.remove(name);
                save(session);
                player.sendMessage("§aFunction deleted.");
                open(player);
            }
        }
    }

    private void openDeleteConfirm(Player player, String name) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_CONFIRM + " " + name);
        fill(inv);
        inv.setItem(11, named(Material.LIME_CONCRETE, "§aConfirm Delete", List.of("§cThis cannot be undone.")));
        inv.setItem(15, named(Material.RED_CONCRETE, "§cCancel", List.of("§7Go back.")));
        player.openInventory(inv);
    }

    private void save(Session session) {
        storage.saveAll(session.owner, session.slot, session.functions, serializer);
        debug.toOps("Saved functions owner=" + session.owner + " slot=" + session.slot.index());
    }

    private static String sanitizeName(String input) {
        String n = input.trim();
        if (n.length() > 24) n = n.substring(0, 24);
        return n.replaceAll("[^a-zA-Z0-9_\\- ]", "").trim();
    }

    private static String stripColor(String s) {
        return s.replaceAll("§.", "");
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

    private record Session(UUID owner, HouseSlot slot, Map<String, ActionList> functions) {
        private Session(UUID owner, HouseSlot slot) {
            this(owner, slot, new HashMap<>());
        }
    }
}
