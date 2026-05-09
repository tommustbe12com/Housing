package com.tommustbe12.housing.gui;

import com.tommustbe12.housing.chat.ChatPrompts;
import com.tommustbe12.housing.custommenus.CustomMenu;
import com.tommustbe12.housing.custommenus.CustomMenusService;
import com.tommustbe12.housing.groups.HouseGroupsService;
import com.tommustbe12.housing.groups.HousePermission;
import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.houses.HouseSlot;
import com.tommustbe12.housing.util.HousingItems;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CustomMenusGui {
    private static final String TITLE_LIST = "Custom Menus";
    private static final String TITLE_EDIT_PREFIX = "Edit Menu: ";

    private final Plugin plugin;
    private final ChatPrompts prompts;
    private final HouseManager houses;
    private final CustomMenusService menus;
    private final ActionsEditor actionsEditor;
    private final HouseGroupsService groups;

    private final ConcurrentHashMap<UUID, UUID> editingMenuId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Runnable> backActions = new ConcurrentHashMap<>();

    public CustomMenusGui(Plugin plugin, ChatPrompts prompts, HouseManager houses, CustomMenusService menus, ActionsEditor actionsEditor, HouseGroupsService groups) {
        this.plugin = plugin;
        this.prompts = prompts;
        this.houses = houses;
        this.menus = menus;
        this.actionsEditor = actionsEditor;
        this.groups = groups;
    }

    public boolean isTitle(String title) {
        return TITLE_LIST.equals(title) || (title != null && title.startsWith(TITLE_EDIT_PREFIX));
    }

    public void open(Player player) {
        open(player, () -> {});
    }

    public void open(Player player, Runnable backToSystems) {
        backActions.put(player.getUniqueId(), backToSystems == null ? () -> {} : backToSystems);
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null) return;
        boolean isOwner = info.owner().equals(player.getUniqueId());
        if (!isOwner && (groups == null || !groups.has(info.owner(), info.slot(), player.getUniqueId(), HousePermission.EDIT_CUSTOM_MENUS))) {
            player.sendMessage("§cYou don't have permission to edit custom menus in this house.");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, TITLE_LIST);
        fill(inv);
        int i = 0;
        for (CustomMenu m : menus.get(info.owner(), info.slot())) {
            inv.setItem(i++, named(Material.ITEM_FRAME, "§f" + m.name(), List.of("§7Click to edit", "§7Right-click to delete")));
            if (i >= 45) break;
        }
        inv.setItem(49, named(Material.ANVIL, "§aCreate Menu", List.of("§7Click to create a new menu.")));
        inv.setItem(53, named(Material.ARROW, "§7Back", List.of("§7Return.")));
        player.openInventory(inv);
    }

    public void handleClick(Player player, String title, int rawSlot, ItemStack clicked, org.bukkit.event.inventory.ClickType clickType) {
        Runnable back = backActions.getOrDefault(player.getUniqueId(), () -> {});
        handleClick(player, title, rawSlot, clicked, clickType, back);
    }

    public void handleClick(Player player, String title, int rawSlot, ItemStack clicked, org.bukkit.event.inventory.ClickType clickType, Runnable backToSystems) {
        if (clicked == null || clicked.getType().isAir()) return;
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null) return;
        boolean isOwner = info.owner().equals(player.getUniqueId());
        if (!isOwner && (groups == null || !groups.has(info.owner(), info.slot(), player.getUniqueId(), HousePermission.EDIT_CUSTOM_MENUS))) return;

        if (TITLE_LIST.equals(title)) {
            if (clicked.getType() == Material.ARROW) {
                backToSystems.run();
                return;
            }
            if (clicked.getType() == Material.ANVIL) {
                prompts.prompt(player, "Enter menu name:", msg -> {
                    if (msg.equalsIgnoreCase("cancel")) return;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        CustomMenu m = new CustomMenu(UUID.randomUUID(), msg.trim(), 3);
                        menus.get(info.owner(), info.slot()).add(m);
                        menus.save(info.owner(), info.slot());
                        openEditor(player, info.owner(), info.slot(), m);
                    });
                });
                return;
            }
            int idx = rawSlot;
            var list = menus.get(info.owner(), info.slot());
            if (idx < 0 || idx >= list.size()) return;
            CustomMenu m = list.get(idx);
            if (clickType.isRightClick()) {
                list.remove(idx);
                menus.save(info.owner(), info.slot());
                player.sendMessage("§aMenu deleted.");
                open(player);
                return;
            }
            openEditor(player, info.owner(), info.slot(), m);
            return;
        }

        if (title != null && title.startsWith(TITLE_EDIT_PREFIX)) {
            CustomMenu menu = findEditing(info.owner(), info.slot(), player);
            if (menu == null) {
                open(player);
                return;
            }

            if (clicked.getType() == Material.ARROW) {
                open(player);
                return;
            }
            if (clicked.getType() == Material.NAME_TAG) {
                prompts.prompt(player, "Enter new menu name:", msg -> {
                    if (msg.equalsIgnoreCase("cancel")) return;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        menu.setName(msg.trim());
                        menus.save(info.owner(), info.slot());
                        openEditor(player, info.owner(), info.slot(), menu);
                    });
                });
                return;
            }
            if (clicked.getType() == Material.CHEST) {
                int nextRows = menu.rows() == 3 ? 6 : 3;
                menu.setRows(nextRows);
                menus.save(info.owner(), info.slot());
                openEditor(player, info.owner(), info.slot(), menu);
                return;
            }
            if (clicked.getType() == Material.LIME_CONCRETE) {
                saveFromEditor(player, info.owner(), info.slot(), menu);
                player.sendMessage("§aMenu saved.");
                open(player);
                return;
            }
            if (clicked.getType() == Material.RED_CONCRETE) {
                player.sendMessage("§cCanceled.");
                open(player);
                return;
            }

            if (rawSlot >= 0 && rawSlot < menu.rows() * 9 && clickType.isRightClick()) {
                int slotIndex = rawSlot;
                CustomMenu.SlotActions slotActions = menu.slotActions().computeIfAbsent(slotIndex, k -> new CustomMenu.SlotActions());
                var list = slotActions.right();
                actionsEditor.openStandaloneHouse(player, info.owner(), info.slot(),
                        "Menu: " + menu.name() + " (Slot " + (slotIndex + 1) + ")",
                        list,
                        updated -> {
                            slotActions.right().actions().clear();
                            slotActions.right().actions().addAll(updated.actions());
                            menus.save(info.owner(), info.slot());
                        },
                        () -> openEditor(player, info.owner(), info.slot(), menu));
            }
        }
    }

    public void handleClose(Player player, Inventory inv) {
        editingMenuId.remove(player.getUniqueId());
    }

    private void openEditor(Player player, UUID owner, HouseSlot slot, CustomMenu menu) {
        editingMenuId.put(player.getUniqueId(), menu.id());
        int size = menu.rows() * 9;
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_EDIT_PREFIX + menu.name());

        ItemStack pane = named(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 54; i++) inv.setItem(i, i >= 45 ? pane : null);

        ItemStack[] contents = menu.contents();
        for (int i = 0; i < Math.min(size, contents.length); i++) inv.setItem(i, contents[i]);

        inv.setItem(45, named(Material.NAME_TAG, "§eRename", List.of("§7Rename this menu.")));
        inv.setItem(46, named(Material.CHEST, "§bRows", List.of("§7Current: §f" + menu.rows(), "§7Click to toggle 3/6")));
        inv.setItem(49, named(Material.RED_CONCRETE, "§cCancel", List.of("§7Discard changes.")));
        inv.setItem(53, named(Material.LIME_CONCRETE, "§aSave", List.of("§7Save menu items.")));
        inv.setItem(52, named(Material.ARROW, "§7Back", List.of("§7Return.")));

        HousingItems.ensureMenuStar(plugin, player);
        player.openInventory(inv);
    }

    private void saveFromEditor(Player player, UUID owner, HouseSlot slot, CustomMenu menu) {
        Inventory inv = player.getOpenInventory().getTopInventory();
        int size = menu.rows() * 9;
        ItemStack[] contents = new ItemStack[size];
        for (int i = 0; i < size; i++) {
            ItemStack it = inv.getItem(i);
            if (HousingItems.isMenuStar(plugin, it)) it = null;
            contents[i] = it;
        }
        menu.setContents(contents);
        menus.save(owner, slot);
    }

    private CustomMenu findEditing(UUID owner, HouseSlot slot, Player player) {
        UUID id = editingMenuId.get(player.getUniqueId());
        if (id == null) return null;
        return menus.find(owner, slot, id);
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
}

