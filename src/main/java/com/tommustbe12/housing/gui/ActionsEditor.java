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

    private final Plugin plugin;
    private final Debug debug;
    private final ChatPrompts prompts;
    private final HouseManager houses;
    private final FunctionStorage functionStorage;
    private final HouseActionsStorage eventStorage;
    private final SimpleActionSerializer serializer = new SimpleActionSerializer();

    private final VariablesStore variables;
    private final Placeholders placeholders;

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
            case COMPARATOR -> prompt(player, "Enter variable key then = then value (ex: %stat.kills%=5):", msg -> {
                String[] parts = msg.split("=", 2);
                if (parts.length < 2) return;
                list.actions().add(new ChangeVariableAction(variables, placeholders, parts[0].trim(), parts[1].trim()));
                save(session);
                openList(player, session);
            });
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
            prompt(player, "Edit key=value:", msg -> {
                String[] parts = msg.split("=", 2);
                if (parts.length < 2) return;
                list.actions().set(index, new ChangeVariableAction(variables, placeholders, parts[0].trim(), parts[1].trim()));
                save(session);
                openList(player, session);
            });
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
        return new SimpleActionCodec(placeholders, variables, houses, (ctx, fn, global) -> {});
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
            lore.add("§7set: §f" + v.key() + " §7= §f" + v.value());
        } else if (action instanceof GiveExpLevelsAction exp) {
            lore.add("§7levels: §f" + exp.levels());
        } else if (action instanceof ApplyPotionEffectAction pot) {
            lore.add("§7effect: §f" + pot.effect() + " §7dur: §f" + pot.durationTicks() + " §7amp: §f" + pot.amplifier());
        } else if (action instanceof RunFunctionAction fn) {
            lore.add("§7fn: §f" + fn.functionName() + " §7scope: §f" + (fn.global() ? "global" : "personal"));
        } else if (action instanceof ConditionalAction cond) {
            lore.add("§7conditions: §f" + cond.conditions().size() + " §7mode: §f" + (cond.matchAny() ? "ANY" : "ALL"));
            lore.add("§7then: §f" + cond.thenList().actions().size() + " §7else: §f" + (cond.elseList() == null ? 0 : cond.elseList().actions().size()));
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
