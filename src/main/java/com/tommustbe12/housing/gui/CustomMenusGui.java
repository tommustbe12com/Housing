package com.tommustbe12.housing.gui;

import com.tommustbe12.housing.actions.ActionList;
import com.tommustbe12.housing.chat.ChatPrompts;
import com.tommustbe12.housing.custommenus.CustomMenu;
import com.tommustbe12.housing.custommenus.CustomMenusService;
import com.tommustbe12.housing.custommenus.gui.CustomMenuEditorHolder;
import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.houses.HouseSlot;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class CustomMenusGui {
    private static final String TITLE_LIST = "Custom Menus";
    private static final String TITLE_EDIT_PREFIX = "Edit Menu: ";
    private static final String TITLE_DELETE_CONFIRM = "Delete Menu?";

    private final Plugin plugin;
    private final ChatPrompts prompts;
    private final ActionsEditor actionsEditor;
    private final HouseManager houses;
    private final CustomMenusService menus;
    private final NamespacedKey menuIdKey;

    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    public CustomMenusGui(Plugin plugin, ChatPrompts prompts, ActionsEditor actionsEditor, HouseManager houses, CustomMenusService menus) {
        this.plugin = plugin;
        this.prompts = prompts;
        this.actionsEditor = actionsEditor;
        this.houses = houses;
        this.menus = menus;
        this.menuIdKey = new NamespacedKey(plugin, "custom_menu_id");
    }

    public boolean isTitle(String title) {
        return TITLE_LIST.equals(title) || TITLE_DELETE_CONFIRM.equals(title) || (title != null && title.startsWith(TITLE_EDIT_PREFIX));
    }

    public void open(Player player, Runnable back) {
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null || !info.owner().equals(player.getUniqueId())) {
            player.sendMessage("§cCustom Menus is only available in your own house.");
            return;
        }
        Session s = new Session(info.owner(), info.slot(), back);
        sessions.put(player.getUniqueId(), s);
        openList(player, s);
    }

    public void handleClick(Player player, String title, int rawSlot, ItemStack clicked, ClickType clickType) {
        Session s = sessions.get(player.getUniqueId());
        if (s == null) return;

        if (TITLE_LIST.equals(title)) {
            if (rawSlot == 49) {
                close(player);
                if (s.back != null) s.back.run();
                return;
            }
            if (rawSlot == 53) {
                prompts.prompt(player, "Enter menu name (or 'cancel'):", msg -> {
                    if (msg.equalsIgnoreCase("cancel")) return;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        String name = msg.trim().isBlank() ? "Menu" : msg.trim();
                        CustomMenu m = new CustomMenu(UUID.randomUUID(), name, 3);
                        menus.get(s.owner, s.slot).add(m);
                        menus.save(s.owner, s.slot);
                        s.openMenuId = m.id();
                        s.actionMode = CustomMenu.ClickKind.LEFT;
                        openEditor(player, s, m.id());
                    });
                });
                return;
            }
            if (rawSlot >= 0 && rawSlot < 45) {
                UUID menuId = menuId(clicked);
                if (menuId == null) return;
                if (clickType.isRightClick()) {
                    s.pendingDeleteMenuId = menuId;
                    openDeleteConfirm(player, s, menuId);
                } else {
                    s.openMenuId = menuId;
                    s.actionMode = CustomMenu.ClickKind.LEFT;
                    openEditor(player, s, menuId);
                }
            }
            return;
        }

        if (TITLE_DELETE_CONFIRM.equals(title)) {
            if (rawSlot == 11) {
                UUID id = s.pendingDeleteMenuId;
                if (id != null) {
                    menus.get(s.owner, s.slot).removeIf(x -> x.id().equals(id));
                    menus.save(s.owner, s.slot);
                }
                s.pendingDeleteMenuId = null;
                openList(player, s);
                return;
            }
            if (rawSlot == 15 || rawSlot == 26) {
                s.pendingDeleteMenuId = null;
                openList(player, s);
                return;
            }
            return;
        }

        if (title != null && title.startsWith(TITLE_EDIT_PREFIX)) {
            if (rawSlot == 45) {
                s.actionMode = CustomMenu.ClickKind.LEFT;
                openEditor(player, s, s.openMenuId);
                return;
            }
            if (rawSlot == 46) {
                s.actionMode = CustomMenu.ClickKind.RIGHT;
                openEditor(player, s, s.openMenuId);
                return;
            }
            if (rawSlot == 47) {
                CustomMenu m = menus.find(s.owner, s.slot, s.openMenuId);
                if (m == null) return;
                prompt(player, "New menu name (or 'cancel'):", name -> {
                    m.setName(name);
                    menus.save(s.owner, s.slot);
                    openEditor(player, s, m.id());
                });
                return;
            }
            if (rawSlot == 48) {
                CustomMenu m = menus.find(s.owner, s.slot, s.openMenuId);
                if (m == null) return;
                m.setRows(m.rows() == 3 ? 6 : 3);
                menus.save(s.owner, s.slot);
                openEditor(player, s, m.id());
                return;
            }
            if (rawSlot == 51) {
                CustomMenu m = menus.find(s.owner, s.slot, s.openMenuId);
                if (m == null) return;
                prompt(player, "New menu title (supports & colors, or 'cancel'):", msg -> {
                    String t = org.bukkit.ChatColor.translateAlternateColorCodes('&', msg);
                    m.setTitle(t);
                    menus.save(s.owner, s.slot);
                    openEditor(player, s, m.id());
                });
                return;
            }
            if (rawSlot == 49) {
                saveFromEditor(player, s);
                openList(player, s);
                return;
            }
            if (rawSlot == 50) {
                CustomMenu m = menus.find(s.owner, s.slot, s.openMenuId);
                if (m == null) return;
                menus.get(s.owner, s.slot).removeIf(x -> x.id().equals(m.id()));
                menus.save(s.owner, s.slot);
                openList(player, s);
                return;
            }

            // Clicking a menu slot in the editor opens its action list (left/right mode)
            if (rawSlot >= 0 && rawSlot < 45) {
                CustomMenu m = menus.find(s.owner, s.slot, s.openMenuId);
                if (m == null) return;
                int max = m.rows() * 9;
                if (rawSlot >= max) return;
                CustomMenu.SlotActions slotActions = m.slotActions().computeIfAbsent(rawSlot, k -> new CustomMenu.SlotActions());
                ActionList source = (s.actionMode == CustomMenu.ClickKind.RIGHT) ? slotActions.right() : slotActions.left();
                ActionList workingCopy = copyOf(source);
                String key = "Menu " + m.name() + " (slot " + rawSlot + ", " + (s.actionMode == CustomMenu.ClickKind.RIGHT ? "right" : "left") + ")";
                actionsEditor.openStandaloneHouse(player, s.owner, s.slot, key, workingCopy, updated -> {
                    // IMPORTANT: never mutate the same ActionList instance we handed to ActionsEditor,
                    // otherwise clear()+addAll() can clear the updated list too.
                    ActionList target = (s.actionMode == CustomMenu.ClickKind.RIGHT) ? slotActions.right() : slotActions.left();
                    target.actions().clear();
                    target.actions().addAll(updated.actions());
                    menus.save(s.owner, s.slot);
                }, () -> openEditor(player, s, m.id()));
            }
        }
    }

    public void handleClose(Player player, Inventory inv) {
        if (!(inv.getHolder() instanceof CustomMenuEditorHolder)) return;
        Session s = sessions.get(player.getUniqueId());
        if (s == null) return;
        saveFromEditor(player, s);
    }

    private void openList(Player player, Session s) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_LIST);
        fill(inv);
        int i = 0;
        for (CustomMenu m : menus.get(s.owner, s.slot)) {
            ItemStack it = named(Material.ITEM_FRAME, "§a" + m.name(), List.of("§7Rows: §f" + m.rows(), "§7Left-click: edit", "§7Right-click: delete"));
            tagMenuId(it, m.id());
            inv.setItem(i++, it);
            if (i >= 45) break;
        }
        inv.setItem(49, named(Material.ARROW, "§7Back", List.of("§7Return.")));
        inv.setItem(53, named(Material.LIME_CONCRETE, "§aCreate Menu", List.of("§7Click to create.")));
        player.openInventory(inv);
    }

    private void openDeleteConfirm(Player player, Session s, UUID menuId) {
        CustomMenu m = menus.find(s.owner, s.slot, menuId);
        String name = m == null ? "Menu" : m.name();
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_DELETE_CONFIRM);
        fill(inv);
        inv.setItem(11, named(Material.RED_CONCRETE, "§cDelete", List.of("§7Delete: §f" + name)));
        inv.setItem(15, named(Material.LIME_CONCRETE, "§aCancel", List.of("§7Return")));
        inv.setItem(26, named(Material.ARROW, "§7Back", List.of("§7Return")));
        player.openInventory(inv);
    }

    private void openEditor(Player player, Session s, UUID menuId) {
        if (menuId == null) return;
        CustomMenu m = menus.find(s.owner, s.slot, menuId);
        if (m == null) {
            openList(player, s);
            return;
        }

        CustomMenuEditorHolder holder = new CustomMenuEditorHolder(s.owner, s.slot, menuId);
        Inventory inv = Bukkit.createInventory(holder, 54, TITLE_EDIT_PREFIX + m.name());
        ItemStack border = named(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, border);

        int max = m.rows() * 9;
        for (int i = 0; i < max; i++) inv.setItem(i, null);
        ItemStack[] contents = m.contents();
        for (int i = 0; i < Math.min(max, contents.length); i++) {
            ItemStack it = contents[i];
            if (it != null && !it.getType().isAir()) inv.setItem(i, it.clone());
        }

        inv.setItem(45, named(s.actionMode == CustomMenu.ClickKind.LEFT ? Material.LIME_DYE : Material.GRAY_DYE, "§aMode: Left Click", List.of("§7Click to select")));
        inv.setItem(46, named(s.actionMode == CustomMenu.ClickKind.RIGHT ? Material.LIME_DYE : Material.GRAY_DYE, "§aMode: Right Click", List.of("§7Click to select")));
        inv.setItem(47, named(Material.ANVIL, "§eRename", List.of("§7Chat prompt")));
        inv.setItem(48, named(Material.IRON_DOOR, "§bToggle Rows", List.of("§7Now: §f" + m.rows())));
        inv.setItem(49, named(Material.ARROW, "§7Back", List.of("§7Save + return")));
        inv.setItem(50, named(Material.RED_CONCRETE, "§cDelete", List.of("§7Delete this menu")));
        inv.setItem(51, named(Material.OAK_SIGN, "§bTitle", List.of("§7Current: §f" + stripColors(m.title()), "§7Click to change")));
        inv.setItem(53, named(Material.PAPER, "§fTip", List.of("§7Click a slot to edit actions", "§7Items placed here keep NBT/PDC")));
        player.openInventory(inv);
    }

    private static String stripColors(String s) {
        if (s == null) return "";
        return s.replaceAll("§.", "");
    }

    private void saveFromEditor(Player player, Session s) {
        if (s.openMenuId == null) return;
        Inventory top = player.getOpenInventory().getTopInventory();
        if (!(top.getHolder() instanceof CustomMenuEditorHolder holder)) return;
        if (!holder.menuId().equals(s.openMenuId)) return;
        CustomMenu m = menus.find(s.owner, s.slot, s.openMenuId);
        if (m == null) return;
        int max = m.rows() * 9;
        ItemStack[] newContents = new ItemStack[max];
        for (int i = 0; i < max; i++) {
            ItemStack it = top.getItem(i);
            if (it == null || it.getType().isAir()) continue;
            newContents[i] = it.clone();
        }
        m.setContents(newContents);
        menus.save(s.owner, s.slot);
    }

    private void close(Player player) {
        sessions.remove(player.getUniqueId());
        player.closeInventory();
    }

    private void prompt(Player player, String question, java.util.function.Consumer<String> onOk) {
        prompts.prompt(player, question, msg -> {
            if (msg.equalsIgnoreCase("cancel")) return;
            Bukkit.getScheduler().runTask(plugin, () -> onOk.accept(msg));
        });
    }

    private static ActionList copyOf(ActionList src) {
        ActionList out = new ActionList();
        if (src != null) out.actions().addAll(src.actions());
        return out;
    }

    private static final class Session {
        private final UUID owner;
        private final HouseSlot slot;
        private final Runnable back;
        private UUID openMenuId;
        private UUID pendingDeleteMenuId;
        private CustomMenu.ClickKind actionMode = CustomMenu.ClickKind.LEFT;

        private Session(UUID owner, HouseSlot slot, Runnable back) {
            this.owner = owner;
            this.slot = slot;
            this.back = back;
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
        for (int i = 0; i < inv.getSize(); i++) if (inv.getItem(i) == null) inv.setItem(i, filler);
    }

    private UUID menuId(ItemStack item) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        String raw = meta.getPersistentDataContainer().get(menuIdKey, PersistentDataType.STRING);
        // Fallback for older items (pre-PDC): localizedName "menu:<uuid>"
        if (raw == null) {
            String s = meta.getLocalizedName();
            if (s == null || !s.startsWith("menu:")) return null;
            try { return UUID.fromString(s.substring("menu:".length())); } catch (Exception e) { return null; }
        }
        try { return UUID.fromString(raw); } catch (Exception e) { return null; }
    }

    private void tagMenuId(ItemStack item, UUID id) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(menuIdKey, PersistentDataType.STRING, id.toString());
        item.setItemMeta(meta);
    }
}
