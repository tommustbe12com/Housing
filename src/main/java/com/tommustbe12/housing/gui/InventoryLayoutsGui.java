package com.tommustbe12.housing.gui;

import com.tommustbe12.housing.chat.ChatPrompts;
import com.tommustbe12.housing.groups.HouseGroupsService;
import com.tommustbe12.housing.groups.HousePermission;
import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.houses.HouseSlot;
import com.tommustbe12.housing.inventorylayouts.InventoryLayout;
import com.tommustbe12.housing.inventorylayouts.InventoryLayoutsService;
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
import java.util.function.Consumer;

public final class InventoryLayoutsGui {
    private static final String TITLE_LIST = "Inventory Layouts";
    private static final String TITLE_EDIT_PREFIX = "Edit Layout: ";
    private static final String TITLE_PICK = "Pick Layout";

    private final Plugin plugin;
    private final ChatPrompts prompts;
    private final HouseManager houses;
    private final InventoryLayoutsService layouts;
    private final HouseGroupsService groups;

    private final ConcurrentHashMap<UUID, UUID> editingLayoutId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Consumer<InventoryLayout>> pickCallbacks = new ConcurrentHashMap<>();

    public InventoryLayoutsGui(Plugin plugin, ChatPrompts prompts, HouseManager houses, InventoryLayoutsService layouts, HouseGroupsService groups) {
        this.plugin = plugin;
        this.prompts = prompts;
        this.houses = houses;
        this.layouts = layouts;
        this.groups = groups;
    }

    public boolean isTitle(String title) {
        return TITLE_LIST.equals(title) || (title != null && title.startsWith(TITLE_EDIT_PREFIX)) || TITLE_PICK.equals(title);
    }

    public void open(Player player) {
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null) return;
        boolean isOwner = info.owner().equals(player.getUniqueId());
        if (!isOwner && (groups == null || !groups.has(info.owner(), info.slot(), player.getUniqueId(), HousePermission.EDIT_INVENTORY_LAYOUTS))) {
            player.sendMessage("§cYou don't have permission to edit inventory layouts in this house.");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, TITLE_LIST);
        fill(inv);
        List<InventoryLayout> list = layouts.get(info.owner(), info.slot());
        int i = 0;
        for (InventoryLayout l : list) {
            inv.setItem(i++, named(Material.CHEST, "§e" + l.name(), List.of("§7Click to edit", "§7Right-click to delete")));
            if (i >= 45) break;
        }
        inv.setItem(49, named(Material.ANVIL, "§aCreate Layout", List.of("§7Click to create a new layout.")));
        inv.setItem(53, named(Material.ARROW, "§7Back", List.of("§7Return.")));
        player.openInventory(inv);
    }

    public void openPicker(Player player, UUID owner, HouseSlot slot, Consumer<InventoryLayout> onPick, Runnable back) {
        pickCallbacks.put(player.getUniqueId(), onPick);
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_PICK);
        fill(inv);
        int i = 0;
        for (InventoryLayout l : layouts.get(owner, slot)) {
            inv.setItem(i++, named(Material.CHEST, "§e" + l.name(), List.of("§7Click to select")));
            if (i >= 45) break;
        }
        inv.setItem(49, named(Material.BARRIER, "§cClear", List.of("§7Remove selected layout.")));
        inv.setItem(53, named(Material.ARROW, "§7Back", List.of("§7Return.")));
        player.openInventory(inv);
    }

    public void handleClick(Player player, String title, int rawSlot, ItemStack clicked, org.bukkit.event.inventory.ClickType clickType, Runnable backToSystems, Runnable backToActionEditor) {
        if (clicked == null || clicked.getType().isAir()) return;

        if (TITLE_PICK.equals(title)) {
            if (clicked.getType() == Material.ARROW) {
                backToActionEditor.run();
                return;
            }
            Consumer<InventoryLayout> cb = pickCallbacks.remove(player.getUniqueId());
            if (cb == null) {
                backToActionEditor.run();
                return;
            }
            var info = houses.getHouseInfoByWorld(player.getWorld());
            if (info == null) {
                cb.accept(null);
                return;
            }
            boolean isOwner = info.owner().equals(player.getUniqueId());
            if (!isOwner && (groups == null || !groups.has(info.owner(), info.slot(), player.getUniqueId(), HousePermission.EDIT_INVENTORY_LAYOUTS))) {
                cb.accept(null);
                backToActionEditor.run();
                return;
            }

            if (clicked.getType() == Material.BARRIER) {
                cb.accept(null);
                backToActionEditor.run();
                return;
            }
            int idx = rawSlot;
            List<InventoryLayout> list = layouts.get(info.owner(), info.slot());
            if (idx < 0 || idx >= list.size()) return;
            cb.accept(list.get(idx));
            backToActionEditor.run();
            return;
        }

        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null) return;
        boolean isOwner = info.owner().equals(player.getUniqueId());
        if (!isOwner && (groups == null || !groups.has(info.owner(), info.slot(), player.getUniqueId(), HousePermission.EDIT_INVENTORY_LAYOUTS))) return;

        if (TITLE_LIST.equals(title)) {
            if (clicked.getType() == Material.ARROW) {
                backToSystems.run();
                return;
            }
            if (clicked.getType() == Material.ANVIL) {
                prompts.prompt(player, "Enter layout name:", msg -> {
                    if (msg.equalsIgnoreCase("cancel")) return;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        InventoryLayout l = new InventoryLayout(UUID.randomUUID(), msg.trim());
                        layouts.get(info.owner(), info.slot()).add(l);
                        layouts.save(info.owner(), info.slot());
                        openEdit(player, info.owner(), info.slot(), l);
                    });
                });
                return;
            }
            int idx = rawSlot;
            List<InventoryLayout> list = layouts.get(info.owner(), info.slot());
            if (idx < 0 || idx >= list.size()) return;
            InventoryLayout l = list.get(idx);
            if (clickType.isRightClick()) {
                list.remove(idx);
                layouts.save(info.owner(), info.slot());
                player.sendMessage("§aLayout deleted.");
                open(player);
                return;
            }
            openEdit(player, info.owner(), info.slot(), l);
            return;
        }

        if (title != null && title.startsWith(TITLE_EDIT_PREFIX)) {
            if (clicked.getType() == Material.ARROW) {
                open(player);
                return;
            }
            if (clicked.getType() == Material.NAME_TAG) {
                InventoryLayout l = findEditing(player, info.owner(), info.slot());
                if (l == null) return;
                prompts.prompt(player, "Enter new layout name:", msg -> {
                    if (msg.equalsIgnoreCase("cancel")) return;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        l.setName(msg.trim());
                        layouts.save(info.owner(), info.slot());
                        openEdit(player, info.owner(), info.slot(), l);
                    });
                });
                return;
            }
            if (clicked.getType() == Material.LIME_CONCRETE) {
                saveFromEditor(player, info.owner(), info.slot());
                player.sendMessage("§aLayout saved.");
                open(player);
                return;
            }
            if (clicked.getType() == Material.RED_CONCRETE) {
                editingLayoutId.remove(player.getUniqueId());
                player.sendMessage("§cCanceled.");
                open(player);
            }
        }
    }

    public void handleEditorClose(Player player, Inventory inv) {
        if (!inv.getViewers().isEmpty()) return;
    }

    private void openEdit(Player player, UUID owner, HouseSlot slot, InventoryLayout layout) {
        editingLayoutId.put(player.getUniqueId(), layout.id());
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_EDIT_PREFIX + layout.name());

        ItemStack[] contents = layout.contents();
        if (contents != null) {
            // Render player inventory 9..35 into editor slots 0..26, then hotbar 0..8 into slots 27..35.
            for (int i = 9; i <= 35 && i < contents.length; i++) {
                ItemStack it = contents[i];
                if (it != null && !it.getType().isAir()) inv.setItem(i - 9, it);
            }
            for (int i = 0; i <= 8 && i < contents.length; i++) {
                ItemStack it = contents[i];
                if (it != null && !it.getType().isAir()) inv.setItem(27 + i, it);
            }
        }

        // Hotbar slot 8 is reserved for the Housing menu item; show it and don't allow editing it.
        inv.setItem(35, HousingItems.createMenuStar(plugin));

        ItemStack pane = named(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 36; i <= 44; i++) inv.setItem(i, pane);

        inv.setItem(45, layout.helmet());
        inv.setItem(46, layout.chestplate());
        inv.setItem(47, layout.leggings());
        inv.setItem(48, layout.boots());
        inv.setItem(50, layout.offhand());

        inv.setItem(49, named(Material.NAME_TAG, "§eRename", List.of("§7Click to rename this layout.")));
        inv.setItem(52, named(Material.RED_CONCRETE, "§cCancel", List.of("§7Discard changes.")));
        inv.setItem(53, named(Material.LIME_CONCRETE, "§aSave", List.of("§7Save this layout.")));
        player.openInventory(inv);
    }

    private void saveFromEditor(Player player, UUID owner, HouseSlot slot) {
        InventoryLayout l = findEditing(player, owner, slot);
        if (l == null) return;
        Inventory inv = player.getOpenInventory().getTopInventory();

        // Editor layout uses 4 rows:
        // - Slots 0..26 are the top 3 rows (player inventory 9..35)
        // - Slots 27..35 are the bottom row (player hotbar 0..8)
        ItemStack[] contents = new ItemStack[36];
        for (int i = 0; i <= 26; i++) contents[i + 9] = inv.getItem(i);
        for (int i = 27; i <= 35; i++) contents[i - 27] = inv.getItem(i);
        // Reserve hotbar slot 8 for the Housing menu star.
        contents[8] = HousingItems.createMenuStar(plugin);
        l.setContents(contents);

        l.setHelmet(inv.getItem(45));
        l.setChestplate(inv.getItem(46));
        l.setLeggings(inv.getItem(47));
        l.setBoots(inv.getItem(48));
        l.setOffhand(inv.getItem(50));
        layouts.save(owner, slot);
    }

    private InventoryLayout findEditing(Player player, UUID owner, HouseSlot slot) {
        UUID id = editingLayoutId.get(player.getUniqueId());
        if (id == null) return null;
        return layouts.find(owner, slot, id);
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
