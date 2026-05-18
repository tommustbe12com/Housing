package com.tommustbe12.housing.gui;

import com.tommustbe12.housing.actions.ActionList;
import com.tommustbe12.housing.actions.conditions.*;
import com.tommustbe12.housing.actions.impl.ConditionalAction;
import com.tommustbe12.housing.actions.placeholders.Placeholders;
import com.tommustbe12.housing.actions.placeholders.VariablesStore;
import com.tommustbe12.housing.chat.ChatPrompts;
import com.tommustbe12.housing.groups.HouseGroup;
import com.tommustbe12.housing.groups.HouseGroupsData;
import com.tommustbe12.housing.groups.HouseGroupsStore;
import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.houses.HouseSlot;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class ConditionalGui {
    private static final String TITLE_SETTINGS = "Action Settings";
    private static final String TITLE_CONDITIONS = "Edit Conditions";
    private static final String TITLE_ADD_COND = "Add Condition";
    private static final String TITLE_SET_ITEM = "Set Required Item";
    private static final String TITLE_PICK_OP = "Choose Comparison";
    private static final String TITLE_PICK_GROUP = "Choose Group";

    private final Plugin plugin;
    private final ChatPrompts prompts;
    private final ActionsEditor actionsEditor;
    private final Placeholders placeholders;
    private final HouseManager houses;
    private final HouseGroupsStore groupsStore;

    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    public ConditionalGui(Plugin plugin, ChatPrompts prompts, ActionsEditor actionsEditor, HouseManager houses) {
        this.plugin = plugin;
        this.prompts = prompts;
        this.actionsEditor = actionsEditor;
        this.houses = houses;
        this.groupsStore = new HouseGroupsStore(plugin);
        VariablesStore vars = new VariablesStore(plugin);
        this.placeholders = new Placeholders(vars);
    }

    public boolean isTitle(String title) {
        return TITLE_SETTINGS.equals(title)
                || TITLE_CONDITIONS.equals(title)
                || TITLE_ADD_COND.equals(title)
                || TITLE_SET_ITEM.equals(title)
                || TITLE_PICK_OP.equals(title)
                || TITLE_PICK_GROUP.equals(title);
    }

    public void open(Player player, UUID owner, HouseSlot slot, ConditionalAction conditional, Consumer<ConditionalAction> onSave, Runnable back) {
        sessions.put(player.getUniqueId(), new Session(owner, slot, conditional, onSave, back));
        openSettings(player);
    }

    public void handleClick(Player player, String title, ItemStack clicked, org.bukkit.event.inventory.ClickType clickType) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;
        if (clicked == null || clicked.getType().isAir()) return;

        if (TITLE_SETTINGS.equals(title)) {
            switch (clicked.getType()) {
                case BOOK -> openConditions(player);
                case LIME_DYE, GRAY_DYE -> {
                    session.matchAny = !session.matchAny;
                    openSettings(player);
                }
                case LIME_CONCRETE -> actionsEditor.openStandaloneHouse(player, session.owner, session.slot, "if_actions", session.thenList, updated -> {
                    session.thenList = updated;
                    save(session);
                }, () -> openSettings(player));
                case RED_CONCRETE -> actionsEditor.openStandaloneHouse(player, session.owner, session.slot, "else_actions", session.elseList, updated -> {
                    session.elseList = updated;
                    save(session);
                }, () -> openSettings(player));
                case ARROW -> {
                    save(session);
                    sessions.remove(player.getUniqueId());
                    session.back.run();
                }
            }
            return;
        }

        if (TITLE_CONDITIONS.equals(title)) {
            if (clicked.getType() == Material.ARROW) {
                openSettings(player);
                return;
            }
            if (clicked.getType() == Material.ANVIL) {
                openAdd(player);
                return;
            }
            // right-click delete
            if (clickType.isRightClick()) {
                int idx = parseIndex(clicked);
                if (idx >= 0 && idx < session.conditions.size()) {
                    session.conditions.remove(idx);
                    save(session);
                    openConditions(player);
                }
                return;
            }
            // left-click edit
            int idx = parseIndex(clicked);
            if (idx >= 0 && idx < session.conditions.size()) {
                editCondition(player, session, idx);
            }
            return;
        }

        if (TITLE_ADD_COND.equals(title)) {
            if (clicked.getType() == Material.ARROW) {
                openConditions(player);
                return;
            }
            addConditionFromIcon(player, session, clicked.getType());
            return;
        }

        if (TITLE_PICK_GROUP.equals(title)) {
            if (clicked.getType() == Material.ARROW) {
                openConditions(player);
                return;
            }
            if (session.pendingGroupConsumer == null) return;
            UUID gid = groupIdFromItem(clicked);
            if (gid == null) return;
            Consumer<UUID> consumer = session.pendingGroupConsumer;
            session.pendingGroupConsumer = null;
            consumer.accept(gid);
            return;
        }

        if (TITLE_SET_ITEM.equals(title)) {
            // confirm/cancel
            if (clicked.getType() == Material.LIME_CONCRETE) {
                ItemStack item = player.getOpenInventory().getTopInventory().getItem(13);
                if (item == null || item.getType().isAir()) {
                    player.sendMessage("§cPlace an item in the middle slot first.");
                    return;
                }
                session.pendingItemConsumer.accept(item.clone());
                session.pendingItemConsumer = null;
                openConditions(player);
            } else if (clicked.getType() == Material.RED_CONCRETE) {
                session.pendingItemConsumer = null;
                openConditions(player);
            }
            return;
        }

        if (TITLE_PICK_OP.equals(title)) {
            if (clicked.getType() == Material.ARROW) {
                openConditions(player);
                return;
            }
            CompareOp op = opFromIcon(clicked.getType());
            if (op == null) return;
            if (session.pendingOpConsumer != null) {
                var consumer = session.pendingOpConsumer;
                session.pendingOpConsumer = null;
                consumer.accept(op);
            }
            return;
        }
    }

    private void openSettings(Player player) {
        Session s = sessions.get(player.getUniqueId());
        if (s == null) return;
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_SETTINGS);
        fill(inv);
        inv.setItem(11, named(Material.BOOK, "§eConditions", List.of("§7Edit conditions.", "§7Count: §f" + s.conditions.size())));
        inv.setItem(13, named(s.matchAny ? Material.LIME_DYE : Material.GRAY_DYE,
                "§aMatch Any Conditions", List.of("§7Toggle: " + (s.matchAny ? "§aANY" : "§7ALL"))));
        inv.setItem(15, named(Material.LIME_CONCRETE, "§aIf Actions", List.of("§7Edit actions when true.", "§7Count: §f" + s.thenList.actions().size())));
        inv.setItem(16, named(Material.RED_CONCRETE, "§cElse Actions", List.of("§7Edit actions when false.", "§7Count: §f" + s.elseList.actions().size())));
        inv.setItem(26, named(Material.ARROW, "§7Back", List.of("§7Return.")));
        player.openInventory(inv);
    }

    private void openConditions(Player player) {
        Session s = sessions.get(player.getUniqueId());
        if (s == null) return;
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_CONDITIONS);
        fill(inv);
        int i = 0;
        for (int idx = 0; idx < s.conditions.size() && i < 45; idx++) {
            Condition c = s.conditions.get(idx);
            inv.setItem(i++, conditionItem(idx, c));
        }
        inv.setItem(49, named(Material.ANVIL, "§aAdd Condition", List.of("§7Click to add.")));
        inv.setItem(53, named(Material.ARROW, "§7Back", List.of("§7Return.")));
        player.openInventory(inv);
    }

    private void openAdd(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_ADD_COND);
        fill(inv);
        inv.setItem(10, named(Material.NAME_TAG, "§eRequired Group", List.of("§7Requires %stat.group%.")));
        inv.setItem(11, named(Material.COMPARATOR, "§bVariable Requirement", List.of("§7Compare %stat.x% to value.")));
        inv.setItem(12, named(Material.CHEST, "§aHas Item", List.of("§7Uses your held item.")));
        inv.setItem(13, named(Material.POTION, "§dHas Potion Effect", List.of("§7Pick effect.")));
        inv.setItem(14, named(Material.FEATHER, "§fPlayer Sneaking", List.of("§7Must be sneaking.")));
        inv.setItem(15, named(Material.ELYTRA, "§fPlayer Flying", List.of("§7Must be flying.")));
        inv.setItem(16, named(Material.APPLE, "§cPlayer Health", List.of("§7Compare health.")));
        inv.setItem(19, named(Material.GOLDEN_APPLE, "§6Max Player Health", List.of("§7Compare max health.")));
        inv.setItem(20, named(Material.COOKED_BEEF, "§ePlayer Hunger", List.of("§7Compare hunger level.")));
        inv.setItem(21, named(Material.GRASS_BLOCK, "§aRequired Gamemode", List.of("§7Pick gamemode.")));
        inv.setItem(49, named(Material.ARROW, "§7Back", List.of("§7Return.")));
        player.openInventory(inv);
    }

    private void addConditionFromIcon(Player player, Session s, Material icon) {
        if (icon == Material.NAME_TAG) {
            openGroupPicker(player, s, gid -> {
                s.conditions.add(new RequiredGroupCondition(gid));
                save(s);
                openConditions(player);
            });
            return;
        }
        if (icon == Material.COMPARATOR) {
            prompts.prompt(player, "Variable key (ex: %stat.kills%):", key -> {
                if (key.equalsIgnoreCase("cancel")) return;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    s.conditions.add(new VariableRequirementCondition(placeholders, key.trim(), CompareOp.EQ, "0"));
                    save(s);
                    openConditions(player);
                });
            });
            return;
        }
        if (icon == Material.CHEST) {
            openSetItemGui(player, s, item -> {
                s.conditions.add(new HasItemCondition(item));
                save(s);
                openConditions(player);
            });
            return;
        }
        if (icon == Material.POTION) {
            prompts.prompt(player, "Potion effect name (ex: SPEED):", eff -> {
                if (eff.equalsIgnoreCase("cancel")) return;
                s.conditions.add(new HasPotionEffectCondition(eff.trim()));
                save(s);
                Bukkit.getScheduler().runTask(plugin, () -> openConditions(player));
            });
            return;
        }
        if (icon == Material.FEATHER) {
            s.conditions.add(new PlayerSneakingCondition());
            save(s);
            openConditions(player);
            return;
        }
        if (icon == Material.ELYTRA) {
            s.conditions.add(new PlayerFlyingCondition());
            save(s);
            openConditions(player);
            return;
        }
        if (icon == Material.APPLE) {
            s.conditions.add(new PlayerHealthCondition(CompareOp.GT, 0));
            save(s);
            openConditions(player);
            return;
        }
        if (icon == Material.GOLDEN_APPLE) {
            s.conditions.add(new MaxHealthCondition(CompareOp.GT, 0));
            save(s);
            openConditions(player);
            return;
        }
        if (icon == Material.COOKED_BEEF) {
            s.conditions.add(new PlayerHungerCondition(CompareOp.GT, 0));
            save(s);
            openConditions(player);
            return;
        }
        if (icon == Material.GRASS_BLOCK) {
            s.conditions.add(new RequiredGamemodeCondition(GameMode.SURVIVAL));
            save(s);
            openConditions(player);
        }
    }

    private void openSetItemGui(Player player, Session s, Consumer<ItemStack> onSet) {
        s.pendingItemConsumer = onSet;
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_SET_ITEM);
        fill(inv);
        inv.setItem(11, named(Material.LIME_CONCRETE, "§aSave", List.of("§7Save this item as requirement.")));
        inv.setItem(15, named(Material.RED_CONCRETE, "§cCancel", List.of("§7Return without saving.")));
        inv.setItem(13, null); // empty slot for item
        player.openInventory(inv);
    }

    private void editCondition(Player player, Session s, int idx) {
        Condition c = s.conditions.get(idx);
        if (c instanceof VariableRequirementCondition var) {
            openOpPicker(player, s, op -> prompts.prompt(player, "Enter value to compare against:", msg -> {
                if (msg.equalsIgnoreCase("cancel")) return;
                String val = msg.trim();
                s.conditions.set(idx, new VariableRequirementCondition(placeholders, var.key(), op, val));
                save(s);
                Bukkit.getScheduler().runTask(plugin, () -> openConditions(player));
            }));
        } else if (c instanceof PlayerHealthCondition ph) {
            openOpPicker(player, s, op -> prompts.prompt(player, "Enter health value:", msg -> {
                if (msg.equalsIgnoreCase("cancel")) return;
                try {
                    double v = Double.parseDouble(msg.trim());
                    s.conditions.set(idx, new PlayerHealthCondition(op, v));
                    save(s);
                    Bukkit.getScheduler().runTask(plugin, () -> openConditions(player));
                } catch (Exception ignored) {}
            }));
        } else if (c instanceof MaxHealthCondition mh) {
            openOpPicker(player, s, op -> prompts.prompt(player, "Enter max health value:", msg -> {
                if (msg.equalsIgnoreCase("cancel")) return;
                try {
                    double v = Double.parseDouble(msg.trim());
                    s.conditions.set(idx, new MaxHealthCondition(op, v));
                    save(s);
                    Bukkit.getScheduler().runTask(plugin, () -> openConditions(player));
                } catch (Exception ignored) {}
            }));
        } else if (c instanceof PlayerHungerCondition hg) {
            openOpPicker(player, s, op -> prompts.prompt(player, "Enter hunger value (0-20):", msg -> {
                if (msg.equalsIgnoreCase("cancel")) return;
                try {
                    int v = Integer.parseInt(msg.trim());
                    s.conditions.set(idx, new PlayerHungerCondition(op, v));
                    save(s);
                    Bukkit.getScheduler().runTask(plugin, () -> openConditions(player));
                } catch (Exception ignored) {}
            }));
        } else if (c instanceof RequiredGamemodeCondition gm) {
            GameMode next = switch (gm.mode()) {
                case SURVIVAL -> GameMode.CREATIVE;
                case CREATIVE -> GameMode.ADVENTURE;
                case ADVENTURE -> GameMode.SPECTATOR;
                case SPECTATOR -> GameMode.SURVIVAL;
            };
            s.conditions.set(idx, new RequiredGamemodeCondition(next));
            save(s);
            openConditions(player);
        } else if (c instanceof RequiredGroupCondition grp) {
            openGroupPicker(player, s, gid -> {
                s.conditions.set(idx, new RequiredGroupCondition(gid, grp.requiredGroupLegacyName()));
                save(s);
                openConditions(player);
            });
        } else if (c instanceof HasPotionEffectCondition pot) {
            prompts.prompt(player, "Edit potion effect name:", msg -> {
                if (msg.equalsIgnoreCase("cancel")) return;
                s.conditions.set(idx, new HasPotionEffectCondition(msg.trim()));
                save(s);
                Bukkit.getScheduler().runTask(plugin, () -> openConditions(player));
            });
        } else if (c instanceof HasItemCondition) {
            ItemStack held = player.getInventory().getItemInMainHand();
            if (held == null || held.getType().isAir()) {
                player.sendMessage("§cHold the required item in your main hand to replace it.");
                return;
            }
            s.conditions.set(idx, new HasItemCondition(held.clone()));
            save(s);
            openConditions(player);
        }
    }

    private void save(Session s) {
        s.onSave.accept(new ConditionalAction(placeholders, s.conditions, s.matchAny, s.thenList, s.elseList));
    }

    private void openOpPicker(Player player, Session s, Consumer<CompareOp> onPick) {
        s.pendingOpConsumer = onPick;
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_PICK_OP);
        fill(inv);
        inv.setItem(10, named(Material.NAME_TAG, "§aIs equal to", List.of("§7Value is exactly equal")));
        inv.setItem(11, named(Material.BARRIER, "§cIs not equal to", List.of("§7Value is not equal")));
        inv.setItem(12, named(Material.ARROW, "§eIs greater than", List.of("§7Value is greater")));
        inv.setItem(13, named(Material.SPECTRAL_ARROW, "§eIs greater or equal", List.of("§7Value is greater or equal")));
        inv.setItem(14, named(Material.FEATHER, "§bIs less than", List.of("§7Value is less")));
        inv.setItem(15, named(Material.LEAD, "§bIs less or equal", List.of("§7Value is less or equal")));
        inv.setItem(22, named(Material.ARROW, "§7Back", List.of("§7Return.")));
        player.openInventory(inv);
    }

    private void openGroupPicker(Player player, Session s, Consumer<UUID> onPick) {
        s.pendingGroupConsumer = onPick;
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_PICK_GROUP);
        fill(inv);

        HouseGroupsData data = groupsStore.load(s.owner, s.slot);
        int i = 0;
        if (data != null) {
            for (HouseGroup g : data.groupsInEditorOrder()) {
                if (i >= 45) break;
                inv.setItem(i++, groupItem(g));
            }
        }
        inv.setItem(53, named(Material.ARROW, "Â§7Back", List.of("Â§7Return.")));
        player.openInventory(inv);
    }

    private static ItemStack groupItem(HouseGroup g) {
        List<String> lore = new ArrayList<>();
        lore.add("Â§7Click to select");
        lore.add("Â§8id: " + g.id());
        lore.add("Â§7tag: " + (g.tag() == null ? "" : g.tag()));
        return named(Material.NAME_TAG, "Â§f" + g.name(), lore);
    }

    private static UUID groupIdFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || item.getItemMeta().getLore() == null) return null;
        for (String line : item.getItemMeta().getLore()) {
            if (line.startsWith("Â§8id: ")) {
                try { return UUID.fromString(line.substring("Â§8id: ".length()).trim()); } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private static CompareOp opFromIcon(Material mat) {
        return switch (mat) {
            case NAME_TAG -> CompareOp.EQ;
            case BARRIER -> CompareOp.NEQ;
            case ARROW -> CompareOp.GT;
            case SPECTRAL_ARROW -> CompareOp.GTE;
            case FEATHER -> CompareOp.LT;
            case LEAD -> CompareOp.LTE;
            default -> null;
        };
    }

    private static ItemStack conditionItem(int idx, Condition c) {
        Material mat = switch (c.type()) {
            case "required_group" -> Material.NAME_TAG;
            case "variable_requirement" -> Material.COMPARATOR;
            case "has_item" -> Material.CHEST;
            case "has_potion_effect" -> Material.POTION;
            case "player_sneaking" -> Material.FEATHER;
            case "player_flying" -> Material.ELYTRA;
            case "player_health" -> Material.APPLE;
            case "max_player_health" -> Material.GOLDEN_APPLE;
            case "player_hunger" -> Material.COOKED_BEEF;
            case "required_gamemode" -> Material.GRASS_BLOCK;
            default -> Material.PAPER;
        };
        List<String> lore = new ArrayList<>();
        lore.add("§7Left-click: edit");
        lore.add("§7Right-click: remove");
        lore.add("§8#" + idx);
        if (c instanceof VariableRequirementCondition v) lore.add("§7" + v.key() + " §f" + v.op() + " §7" + v.value());
        if (c instanceof RequiredGroupCondition g) lore.add("§7groupId: §f" + (g.requiredGroupId() == null ? "" : g.requiredGroupId()));
        if (c instanceof HasPotionEffectCondition p) lore.add("§7effect: §f" + p.effect());
        if (c instanceof RequiredGamemodeCondition gm) lore.add("§7mode: §f" + gm.mode().name());
        return named(mat, "§f" + c.type(), lore);
    }

    private static int parseIndex(ItemStack item) {
        if (item == null || !item.hasItemMeta() || item.getItemMeta().getLore() == null) return -1;
        for (String line : item.getItemMeta().getLore()) {
            if (line.startsWith("§8#")) {
                try { return Integer.parseInt(line.substring(3)); } catch (Exception ignored) {}
            }
        }
        return -1;
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
        inv.setItem(13, null);
    }

    private static final class Session {
        private final UUID owner;
        private final HouseSlot slot;
        private final List<Condition> conditions;
        private boolean matchAny;
        private ActionList thenList;
        private ActionList elseList;
        private final Consumer<ConditionalAction> onSave;
        private final Runnable back;
        private Consumer<ItemStack> pendingItemConsumer;
        private Consumer<CompareOp> pendingOpConsumer;
        private Consumer<UUID> pendingGroupConsumer;

        private Session(UUID owner, HouseSlot slot, ConditionalAction cond, Consumer<ConditionalAction> onSave, Runnable back) {
            this.owner = owner;
            this.slot = slot;
            this.conditions = new ArrayList<>(cond.conditions());
            this.matchAny = cond.matchAny();
            this.thenList = cond.thenList();
            this.elseList = cond.elseList();
            this.onSave = onSave;
            this.back = back;
        }
    }
}
