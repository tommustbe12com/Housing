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

public final class ActionsEditor {
    private static final String TITLE_PREFIX = "Actions: ";
    private static final String TITLE_ADD = "Add Action";
    private static final String TITLE_PICK_FUNCTION = "Choose Function";

    private final Plugin plugin;
    private final Debug debug;
    private final ChatPrompts prompts;
    private final HouseActionsStorage storage;
    private final SimpleActionSerializer serializer = new SimpleActionSerializer();
    private final HouseManager houses;
    private final FunctionStorage functionStorage;

    private final VariablesStore variables;
    private final Placeholders placeholders;

    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, CondEdit> condEdits = new ConcurrentHashMap<>();

    public ActionsEditor(Plugin plugin, Debug debug, ChatPrompts prompts, HouseManager houses) {
        this.plugin = plugin;
        this.debug = debug;
        this.prompts = prompts;
        this.storage = new HouseActionsStorage(plugin);
        this.houses = houses;
        this.functionStorage = new FunctionStorage(plugin);
        this.variables = new VariablesStore(plugin);
        this.placeholders = new Placeholders(variables);
    }

    public boolean isEditorTitle(String title) {
        return title != null && title.startsWith(TITLE_PREFIX);
    }

    public void openEventActions(Player player, UUID owner, HouseSlot slot, String eventKey, Runnable back) {
        Session session = new Session(owner, slot, eventKey.toLowerCase(Locale.ROOT), new HashMap<>(), back, null, null);
        session.events.putAll(storage.loadEventActions(owner, slot, new com.tommustbe12.housing.actions.storage.SimpleActionCodec(placeholders, variables, houses, (ctx, fn, g) -> {})));
        sessions.put(player.getUniqueId(), session);
        openList(player, session);
    }

    public void openStandalone(Player player, String key, ActionList list, java.util.function.Consumer<ActionList> onSave, Runnable back) {
        UUID fakeOwner = new UUID(0L, 0L);
        Session session = new Session(fakeOwner, HouseSlot.SLOT_1, key.toLowerCase(Locale.ROOT), new HashMap<>(), back, onSave, list);
        sessions.put(player.getUniqueId(), session);
        openList(player, session);
    }

    private void openList(Player player, Session session) {
        String title = TITLE_PREFIX + session.eventKey;
        Inventory inv = Bukkit.createInventory(null, 54, title);
        fill(inv);

        ActionList list = session.standaloneList != null ? session.standaloneList : session.events.computeIfAbsent(session.eventKey, k -> new ActionList());
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
            if (session.back != null) session.back.run();
            else backAction.run();
            return;
        }
        if (rawSlot == 49) {
            openAddPicker(player, session);
            return;
        }

        if (rawSlot >= 0 && rawSlot < 45) {
            ActionList list = session.standaloneList != null ? session.standaloneList : session.events.computeIfAbsent(session.eventKey, k -> new ActionList());
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

    private void openAddPicker(Player player, Session session) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_ADD);
        fill(inv);
        inv.setItem(10, named(Material.PAPER, "§eChat Message", List.of("§7Send a message.")));
        inv.setItem(11, named(Material.CLOCK, "§bAction Bar", List.of("§7Display actionbar.")));
        inv.setItem(12, named(Material.NAME_TAG, "§dTitle", List.of("§7Display title/subtitle.")));
        inv.setItem(13, named(Material.TOTEM_OF_UNDYING, "§aFull Heal", List.of("§7Heal player.")));
        inv.setItem(14, named(Material.SKELETON_SKULL, "§cKill Player", List.of("§7Kill player.")));
        inv.setItem(15, named(Material.BARRIER, "§cReset Inventory", List.of("§7Clear inventory.")));
        inv.setItem(16, named(Material.OAK_DOOR, "§aSend To Hub", List.of("§7Teleport to hub.")));
        inv.setItem(19, named(Material.COMPARATOR, "§6Change Variable", List.of("§7Set %stats.x%.")));
        inv.setItem(20, named(Material.EXPERIENCE_BOTTLE, "§aGive Exp Levels", List.of("§7Give levels.")));
        inv.setItem(21, named(Material.MILK_BUCKET, "§bClear Potion Effects", List.of("§7Remove all effects.")));
        inv.setItem(22, named(Material.POTION, "§dApply Potion Effect", List.of("§7Give an effect.")));
        inv.setItem(23, named(Material.REPEATER, "§fRun Function", List.of("§7Call a saved function.")));
        inv.setItem(24, named(Material.STRUCTURE_BLOCK, "§eConditional (if/else)", List.of("§7Run actions based on a check.")));
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

        ActionList list = session.standaloneList != null ? session.standaloneList : session.events.computeIfAbsent(session.eventKey, k -> new ActionList());
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
            case EXPERIENCE_BOTTLE -> prompts.prompt(player, "Enter levels (number):", msg -> {
                if (msg.equalsIgnoreCase("cancel")) return;
                int levels;
                try { levels = Integer.parseInt(msg.trim()); } catch (Exception e) { return; }
                list.actions().add(new GiveExpLevelsAction(levels));
                save(session);
                Bukkit.getScheduler().runTask(plugin, () -> openList(player, session));
            });
            case MILK_BUCKET -> {
                list.actions().add(new ClearPotionEffectsAction());
                save(session);
                openList(player, session);
            }
            case POTION -> prompts.prompt(player, "Enter effect,durationTicks,amplifier (ex: SPEED,200,1):", msg -> {
                if (msg.equalsIgnoreCase("cancel")) return;
                String[] parts = msg.split(",", 3);
                if (parts.length < 3) return;
                try {
                    String effect = parts[0].trim();
                    int dur = Integer.parseInt(parts[1].trim());
                    int amp = Integer.parseInt(parts[2].trim());
                    list.actions().add(new ApplyPotionEffectAction(effect, dur, amp));
                    save(session);
                    Bukkit.getScheduler().runTask(plugin, () -> openList(player, session));
                } catch (Exception ignored) {
                }
            });
            case REPEATER -> openFunctionPicker(player, session);
            case STRUCTURE_BLOCK -> prompts.prompt(player, "Enter condition: left op right (op: EQ,NEQ,GT,GTE,LT,LTE):", msg -> {
                if (msg.equalsIgnoreCase("cancel")) return;
                String[] parts = msg.trim().split(" ", 3);
                if (parts.length < 3) return;
                var cond = new ConditionalAction(placeholders, parts[0], parseOp(parts[1]), parts[2], new ActionList(), new ActionList());
                list.actions().add(cond);
                save(session);
                Bukkit.getScheduler().runTask(plugin, () -> openList(player, session));
            });
        }
    }

    public boolean isAddTitle(String title) {
        return TITLE_ADD.equals(title);
    }

    public boolean isFunctionPickerTitle(String title) {
        return title != null && title.startsWith(TITLE_PICK_FUNCTION);
    }

    public boolean isConditionalEditTitle(String title) {
        return "Edit Conditional".equals(title);
    }

    public void handleConditionalEditClick(Player player, ItemStack clicked) {
        CondEdit edit = condEdits.get(player.getUniqueId());
        Session session = sessions.get(player.getUniqueId());
        if (edit == null || session == null) return;
        if (clicked == null || clicked.getType().isAir()) return;

        ActionList parent = session.standaloneList != null ? session.standaloneList : session.events.computeIfAbsent(session.eventKey, k -> new ActionList());
        Action action = parent.actions().get(edit.index);
        if (!(action instanceof ConditionalAction cond)) return;

        if (clicked.getType() == Material.ARROW) {
            openList(player, session);
            return;
        }
        if (clicked.getType() == Material.COMPARATOR) {
            editAction(player, session, edit.index, cond);
            return;
        }
        if (clicked.getType() == Material.LIME_CONCRETE) {
            openStandalone(player, "then", cond.thenList(), updated -> {
                parent.actions().set(edit.index, new ConditionalAction(placeholders, cond.left(), cond.op(), cond.right(), updated, cond.elseList()));
                save(session);
            }, () -> openList(player, session));
            return;
        }
        if (clicked.getType() == Material.RED_CONCRETE) {
            openStandalone(player, "else", cond.elseList(), updated -> {
                parent.actions().set(edit.index, new ConditionalAction(placeholders, cond.left(), cond.op(), cond.right(), cond.thenList(), updated));
                save(session);
            }, () -> openList(player, session));
        }
    }

    private void openFunctionPicker(Player player, Session session) {
        // Only meaningful in a house context (event actions / functions editing). For standalone item actions, just block.
        if (session.owner.getMostSignificantBits() == 0L && session.owner.getLeastSignificantBits() == 0L) {
            player.sendMessage("§cRun Function is only available for house events/functions.");
            return;
        }
        Map<String, ActionList> funcs = functionStorage.loadAll(session.owner, session.slot,
                new com.tommustbe12.housing.actions.storage.SimpleActionCodec(placeholders, variables, houses, (ctx, fn, g) -> {}));
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

    public void handleFunctionPickerClick(Player player, ItemStack clicked, org.bukkit.event.inventory.ClickType clickType) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;
        if (clicked == null || clicked.getType().isAir()) return;
        if (clicked.getType() == Material.ARROW) {
            openAddPicker(player, session);
            return;
        }
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || meta.getDisplayName() == null) return;
        String name = stripColor(meta.getDisplayName());
        boolean global = clickType.isRightClick();
        ActionList list = session.standaloneList != null ? session.standaloneList : session.events.computeIfAbsent(session.eventKey, k -> new ActionList());
        list.actions().add(new RunFunctionAction((ctx, fn, g) -> {}, name, global));
        save(session);
        openList(player, session);
    }

    private void editAction(Player player, Session session, int index, Action action) {
        ActionList list = session.standaloneList != null ? session.standaloneList : session.events.computeIfAbsent(session.eventKey, k -> new ActionList());

        if (action instanceof SendChatMessageAction) {
            prompts.prompt(player, "Edit message:", msg -> {
                if (msg.equalsIgnoreCase("cancel")) return;
                list.actions().set(index, new SendChatMessageAction(placeholders, msg));
                save(session);
                Bukkit.getScheduler().runTask(plugin, () -> openList(player, session));
            });
            return;
        }
        if (action instanceof DisplayActionBarAction) {
            prompts.prompt(player, "Edit actionbar text:", msg -> {
                if (msg.equalsIgnoreCase("cancel")) return;
                list.actions().set(index, new DisplayActionBarAction(placeholders, msg));
                save(session);
                Bukkit.getScheduler().runTask(plugin, () -> openList(player, session));
            });
            return;
        }
        if (action instanceof DisplayTitleAction) {
            prompts.prompt(player, "Edit title text (use | for subtitle):", msg -> {
                if (msg.equalsIgnoreCase("cancel")) return;
                String[] parts = msg.split("\\|", 2);
                String t = parts[0];
                String s = parts.length > 1 ? parts[1] : "";
                list.actions().set(index, new DisplayTitleAction(placeholders, t, s, 10, 40, 10));
                save(session);
                Bukkit.getScheduler().runTask(plugin, () -> openList(player, session));
            });
            return;
        }
        if (action instanceof ChangeVariableAction) {
            prompts.prompt(player, "Edit variable key=value (ex: %stats.kills%=5):", msg -> {
                if (msg.equalsIgnoreCase("cancel")) return;
                String[] parts = msg.split("=", 2);
                if (parts.length < 2) return;
                list.actions().set(index, new ChangeVariableAction(variables, placeholders, parts[0].trim(), parts[1].trim()));
                save(session);
                Bukkit.getScheduler().runTask(plugin, () -> openList(player, session));
            });
            return;
        }
        if (action instanceof GiveExpLevelsAction) {
            prompts.prompt(player, "Edit levels (number):", msg -> {
                if (msg.equalsIgnoreCase("cancel")) return;
                try {
                    int levels = Integer.parseInt(msg.trim());
                    list.actions().set(index, new GiveExpLevelsAction(levels));
                    save(session);
                    Bukkit.getScheduler().runTask(plugin, () -> openList(player, session));
                } catch (Exception ignored) {
                }
            });
            return;
        }
        if (action instanceof ApplyPotionEffectAction) {
            prompts.prompt(player, "Edit effect,durationTicks,amplifier:", msg -> {
                if (msg.equalsIgnoreCase("cancel")) return;
                String[] parts = msg.split(",", 3);
                if (parts.length < 3) return;
                try {
                    list.actions().set(index, new ApplyPotionEffectAction(parts[0].trim(),
                            Integer.parseInt(parts[1].trim()),
                            Integer.parseInt(parts[2].trim())));
                    save(session);
                    Bukkit.getScheduler().runTask(plugin, () -> openList(player, session));
                } catch (Exception ignored) {
                }
            });
            return;
        }
        if (action instanceof RunFunctionAction fn) {
            prompts.prompt(player, "Edit function name,scope (name,global|personal):", msg -> {
                if (msg.equalsIgnoreCase("cancel")) return;
                String[] parts = msg.split(",", 2);
                if (parts.length < 2) return;
                String name = parts[0].trim();
                boolean global = parts[1].trim().equalsIgnoreCase("global");
                list.actions().set(index, new RunFunctionAction((ctx, f, g) -> {}, name, global));
                save(session);
                Bukkit.getScheduler().runTask(plugin, () -> openList(player, session));
            });
            return;
        }
        if (action instanceof ConditionalAction cond) {
            Inventory inv = Bukkit.createInventory(null, 27, "Edit Conditional");
            fill(inv);
            inv.setItem(11, named(Material.LIME_CONCRETE, "§aEdit THEN actions", List.of("§7Open THEN action list.")));
            inv.setItem(13, named(Material.COMPARATOR, "§eEdit Condition", List.of("§7" + cond.left() + " " + cond.op() + " " + cond.right())));
            inv.setItem(15, named(Material.RED_CONCRETE, "§cEdit ELSE actions", List.of("§7Open ELSE action list.")));
            inv.setItem(26, named(Material.ARROW, "§7Back", List.of("§7Return.")));
            condEdits.put(player.getUniqueId(), new CondEdit(index));
            player.openInventory(inv);
            return;
        }

        player.sendMessage("§7That action has no editable values (yet).");
    }

    private void save(Session session) {
        if (session.onSave != null && session.standaloneList != null) {
            session.onSave.accept(session.standaloneList);
            return;
        }
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
            case "run_function" -> Material.REPEATER;
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
            lore.add("§7if: §f" + cond.left() + " " + cond.op() + " " + cond.right());
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

    private static ConditionalAction.Op parseOp(String raw) {
        try { return ConditionalAction.Op.valueOf(raw.trim().toUpperCase()); } catch (Exception e) { return ConditionalAction.Op.EQ; }
    }

    private record Session(UUID owner, HouseSlot slot, String eventKey, Map<String, ActionList> events,
                           Runnable back, java.util.function.Consumer<ActionList> onSave, ActionList standaloneList) {
        private Session(UUID owner, HouseSlot slot, String eventKey, Map<String, ActionList> events) {
            this(owner, slot, eventKey, events, null, null, null);
        }
    }

    private record CondEdit(int index) {}
}
