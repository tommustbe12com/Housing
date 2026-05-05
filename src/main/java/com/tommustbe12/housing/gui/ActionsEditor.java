package com.tommustbe12.housing.gui;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.ActionList;
import com.tommustbe12.housing.actions.HouseActionsService;
import com.tommustbe12.housing.actions.impl.*;
import com.tommustbe12.housing.actions.placeholders.Placeholders;
import com.tommustbe12.housing.actions.placeholders.VariablesStore;
import com.tommustbe12.housing.actions.storage.HouseActionsStorage;
import com.tommustbe12.housing.actions.storage.SimpleActionSerializer;
import com.tommustbe12.housing.chat.ChatPrompts;
import com.tommustbe12.housing.debug.Debug;
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

public final class ActionsEditor {
    private static final String TITLE_PREFIX = "Actions: ";

    private final Plugin plugin;
    private final Debug debug;
    private final ChatPrompts prompts;
    private final HouseActionsStorage storage;
    private final SimpleActionSerializer serializer = new SimpleActionSerializer();
    private final HouseManager houses;

    private final VariablesStore variables;
    private final Placeholders placeholders;

    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    public ActionsEditor(Plugin plugin, Debug debug, ChatPrompts prompts, HouseManager houses) {
        this.plugin = plugin;
        this.debug = debug;
        this.prompts = prompts;
        this.storage = new HouseActionsStorage(plugin);
        this.houses = houses;
        this.variables = new VariablesStore(plugin);
        this.placeholders = new Placeholders(variables);
    }

    public boolean isEditorTitle(String title) {
        return title != null && title.startsWith(TITLE_PREFIX);
    }

    public void openEventActions(Player player, UUID owner, HouseSlot slot, String eventKey) {
        Session session = new Session(owner, slot, eventKey.toLowerCase(Locale.ROOT), new HashMap<>());
        session.events.putAll(storage.loadEventActions(owner, slot, new com.tommustbe12.housing.actions.storage.SimpleActionCodec(placeholders, variables, houses)));
        sessions.put(player.getUniqueId(), session);
        openList(player, session);
    }

    private void openList(Player player, Session session) {
        String title = TITLE_PREFIX + session.eventKey;
        Inventory inv = Bukkit.createInventory(null, 54, title);
        fill(inv);

        ActionList list = session.events.computeIfAbsent(session.eventKey, k -> new ActionList());
        int i = 0;
        for (Action action : list.actions()) {
            inv.setItem(i++, actionItem(action));
            if (i >= 45) break;
        }

        inv.setItem(49, named(Material.ANVIL, "§aAdd Action", List.of("§7Click to add.")));
        inv.setItem(53, named(Material.ARROW, "§7Back", List.of("§7Return.")));
        player.openInventory(inv);
    }

    public void handleClick(Player player, String title, int rawSlot, ItemStack clicked, org.bukkit.event.inventory.ClickType clickType, Runnable backAction) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;

        if (rawSlot == 53) {
            sessions.remove(player.getUniqueId());
            backAction.run();
            return;
        }
        if (rawSlot == 49) {
            openAddPicker(player, session);
            return;
        }

        if (rawSlot >= 0 && rawSlot < 45) {
            ActionList list = session.events.computeIfAbsent(session.eventKey, k -> new ActionList());
            if (rawSlot >= list.actions().size()) return;
            if (clickType.isRightClick()) {
                list.actions().remove(rawSlot);
                save(session);
                openList(player, session);
            }
        }
    }

    private void openAddPicker(Player player, Session session) {
        Inventory inv = Bukkit.createInventory(null, 54, "Add Action");
        fill(inv);
        inv.setItem(10, named(Material.PAPER, "§eChat Message", List.of("§7Send a message.")));
        inv.setItem(11, named(Material.CLOCK, "§bAction Bar", List.of("§7Display actionbar.")));
        inv.setItem(12, named(Material.NAME_TAG, "§dTitle", List.of("§7Display title/subtitle.")));
        inv.setItem(13, named(Material.TOTEM_OF_UNDYING, "§aFull Heal", List.of("§7Heal player.")));
        inv.setItem(14, named(Material.SKELETON_SKULL, "§cKill Player", List.of("§7Kill player.")));
        inv.setItem(15, named(Material.BARRIER, "§cReset Inventory", List.of("§7Clear inventory.")));
        inv.setItem(16, named(Material.OAK_DOOR, "§aSend To Hub", List.of("§7Teleport to hub.")));
        inv.setItem(19, named(Material.COMPARATOR, "§6Change Variable", List.of("§7Set %stats.x%.")));
        inv.setItem(49, named(Material.ARROW, "§7Back", List.of("§7Return.")));
        player.openInventory(inv);
    }

    public void handleAddPickerClick(Player player, ItemStack clicked) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;
        if (clicked == null || clicked.getType().isAir()) return;
        if (clicked.getType() == Material.ARROW) {
            openList(player, session);
            return;
        }

        ActionList list = session.events.computeIfAbsent(session.eventKey, k -> new ActionList());
        switch (clicked.getType()) {
            case PAPER -> prompts.prompt(player, "Enter message:", msg -> {
                if (msg.equalsIgnoreCase("cancel")) return;
                list.actions().add(new SendChatMessageAction(placeholders, msg));
                save(session);
                Bukkit.getScheduler().runTask(plugin, () -> openList(player, session));
            });
            case CLOCK -> prompts.prompt(player, "Enter actionbar text:", msg -> {
                if (msg.equalsIgnoreCase("cancel")) return;
                list.actions().add(new DisplayActionBarAction(placeholders, msg));
                save(session);
                Bukkit.getScheduler().runTask(plugin, () -> openList(player, session));
            });
            case NAME_TAG -> prompts.prompt(player, "Enter title text (use | for subtitle optional):", msg -> {
                if (msg.equalsIgnoreCase("cancel")) return;
                String[] parts = msg.split("\\|", 2);
                String t = parts[0];
                String s = parts.length > 1 ? parts[1] : "";
                list.actions().add(new DisplayTitleAction(placeholders, t, s, 10, 40, 10));
                save(session);
                Bukkit.getScheduler().runTask(plugin, () -> openList(player, session));
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
            case COMPARATOR -> prompts.prompt(player, "Enter variable key (like %stats.kills%) then = then value:", msg -> {
                if (msg.equalsIgnoreCase("cancel")) return;
                String[] parts = msg.split("=", 2);
                if (parts.length < 2) return;
                list.actions().add(new ChangeVariableAction(variables, placeholders, parts[0].trim(), parts[1].trim()));
                save(session);
                Bukkit.getScheduler().runTask(plugin, () -> openList(player, session));
            });
        }
    }

    private void save(Session session) {
        // NOTE: serializer only supports some action params; others are type-only.
        storage.saveEventActions(session.owner, session.slot, session.events, serializer);
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
            default -> Material.BOOK;
        };
        return named(mat, "§f" + action.type(), List.of("§7Right-click to remove"));
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

    private record Session(UUID owner, HouseSlot slot, String eventKey, Map<String, ActionList> events) {
    }
}
