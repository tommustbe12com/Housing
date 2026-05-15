package com.tommustbe12.housing.gui;

import com.tommustbe12.housing.actions.ActionList;
import com.tommustbe12.housing.actions.placeholders.Placeholders;
import com.tommustbe12.housing.actions.placeholders.VariablesStore;
import com.tommustbe12.housing.actions.storage.SimpleActionCodec;
import com.tommustbe12.housing.actions.storage.SimpleActionSerializer;
import com.tommustbe12.housing.chat.ChatPrompts;
import com.tommustbe12.housing.commands.HouseCommandsStorage;
import com.tommustbe12.housing.debug.Debug;
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

public final class CommandsGui {
    private static final String TITLE = "Commands";
    private static final String TITLE_CONFIRM = "Delete Command?";

    private final Plugin plugin;
    private final Debug debug;
    private final ChatPrompts prompts;
    private final ActionsEditor actionsEditor;
    private final HouseManager houses;
    private final HouseGroupsService groups;
    private final HouseCommandsStorage storage;
    private final SimpleActionSerializer serializer = new SimpleActionSerializer();
    private final SimpleActionCodec codec;

    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    public CommandsGui(Plugin plugin, Debug debug, ChatPrompts prompts, ActionsEditor actionsEditor, HouseManager houses, HouseGroupsService groups) {
        this.plugin = plugin;
        this.debug = debug;
        this.prompts = prompts;
        this.actionsEditor = actionsEditor;
        this.houses = houses;
        this.groups = groups;
        this.storage = new HouseCommandsStorage(plugin);
        VariablesStore vars = new VariablesStore(plugin);
        Placeholders ph = new Placeholders(vars);
        this.codec = new SimpleActionCodec(ph, vars, houses, (ctx, fn, global) -> {},
                new com.tommustbe12.housing.inventorylayouts.InventoryLayoutsService(plugin),
                new com.tommustbe12.housing.custommenus.CustomMenusService(plugin, houses),
                new com.tommustbe12.housing.teams.TeamsService(plugin),
                new com.tommustbe12.housing.groups.HouseGroupsService(plugin, houses));
    }

    public boolean isTitle(String title) {
        return TITLE.equals(title) || title.startsWith(TITLE_CONFIRM);
    }

    public void open(Player player) {
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null) {
            player.sendMessage("§cYou can only edit commands inside a house.");
            return;
        }

        boolean inOwn = info.owner().equals(player.getUniqueId());
        if (!inOwn && (groups == null || !groups.has(info.owner(), info.slot(), player.getUniqueId(), HousePermission.EDIT_COMMANDS))) {
            player.sendMessage("§cYou don't have permission to edit commands in this house.");
            return;
        }

        Session session = new Session(info.owner(), info.slot());
        sessions.put(player.getUniqueId(), session);
        session.commands.clear();
        session.commands.putAll(storage.load(info.owner(), info.slot(), codec));

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        fill(inv);
        int i = 0;
        for (String name : new TreeSet<>(session.commands.keySet())) {
            ActionList list = session.commands.get(name);
            inv.setItem(i++, named(Material.COMMAND_BLOCK, "§f/" + name, List.of(
                    "§7Actions: §b" + list.actions().size(),
                    "§7Left-click: edit",
                    "§7Right-click: rename",
                    "§7Drop-key: delete"
            )));
            if (i >= 45) break;
        }
        inv.setItem(49, named(Material.ANVIL, "§aCreate Command", List.of("§7Click to create.")));
        inv.setItem(53, named(Material.ARROW, "§7Back", List.of("§7Return to Systems.")));
        player.openInventory(inv);
    }

    public void handleClick(Player player, String title, int rawSlot, ItemStack clicked, org.bukkit.event.inventory.ClickType clickType, Runnable backToSystems) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;
        if (clicked == null || clicked.getType().isAir()) return;

        if (TITLE.equals(title)) {
            if (rawSlot == 53) {
                sessions.remove(player.getUniqueId());
                backToSystems.run();
                return;
            }
            if (rawSlot == 49) {
                prompts.prompt(player, "Enter command name (no /, no args):", msg -> {
                    if (msg.equalsIgnoreCase("cancel")) return;
                    String name = sanitize(msg);
                    if (name.isBlank()) return;
                    session.commands.putIfAbsent(name, new ActionList());
                    save(session);
                    Bukkit.getScheduler().runTask(plugin, () -> open(player));
                });
                return;
            }
            if (rawSlot >= 0 && rawSlot < 45) {
                String name = strip(clicked);
                if (name.startsWith("/")) name = name.substring(1);
                name = name.toLowerCase(Locale.ROOT);
                if (!session.commands.containsKey(name)) return;

                if (clickType == org.bukkit.event.inventory.ClickType.DROP) {
                    openConfirm(player, name);
                    return;
                }
                if (clickType.isRightClick()) {
                    String old = name;
                    prompts.prompt(player, "Rename /" + old + " to:", msg -> {
                        if (msg.equalsIgnoreCase("cancel")) return;
                        String nn = sanitize(msg);
                        if (nn.isBlank()) return;
                        ActionList list = session.commands.remove(old);
                        session.commands.put(nn, list);
                        save(session);
                        Bukkit.getScheduler().runTask(plugin, () -> open(player));
                    });
                    return;
                }
                if (clickType.isLeftClick()) {
                    ActionList list = session.commands.get(name);
                    String finalName = name;
                    actionsEditor.openStandaloneHouse(player, session.owner, session.slot, "cmd:" + name, list, updated -> {
                        session.commands.put(finalName, updated);
                        save(session);
                    }, () -> open(player));
                }
            }
            return;
        }

        if (title.startsWith(TITLE_CONFIRM)) {
            String name = title.substring(TITLE_CONFIRM.length()).trim();
            if (clicked.getType() == Material.RED_CONCRETE) {
                open(player);
                return;
            }
            if (clicked.getType() == Material.LIME_CONCRETE) {
                session.commands.remove(name.toLowerCase(Locale.ROOT));
                save(session);
                player.sendMessage("§aCommand deleted.");
                open(player);
            }
        }
    }

    private void openConfirm(Player player, String name) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_CONFIRM + " " + name);
        fill(inv);
        inv.setItem(11, named(Material.LIME_CONCRETE, "§aConfirm Delete", List.of("§cThis cannot be undone.")));
        inv.setItem(15, named(Material.RED_CONCRETE, "§cCancel", List.of("§7Go back.")));
        player.openInventory(inv);
    }

    private void save(Session session) {
        storage.save(session.owner, session.slot, session.commands, serializer);
        debug.toOps("Saved house commands owner=" + session.owner + " slot=" + session.slot.index());
    }

    private static String sanitize(String msg) {
        String s = msg.trim().toLowerCase(Locale.ROOT);
        s = s.replace("/", "");
        s = s.replaceAll("\\s+", "");
        s = s.replaceAll("[^a-z0-9_\\-]", "");
        if (s.length() > 16) s = s.substring(0, 16);
        return s;
    }

    private static String strip(ItemStack item) {
        if (item == null || !item.hasItemMeta() || item.getItemMeta().getDisplayName() == null) return "";
        return item.getItemMeta().getDisplayName().replaceAll("§.", "");
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

    private record Session(UUID owner, HouseSlot slot, Map<String, ActionList> commands) {
        private Session(UUID owner, HouseSlot slot) {
            this(owner, slot, new HashMap<>());
        }
    }
}
