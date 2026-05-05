package com.tommustbe12.housing.gui;

import com.tommustbe12.housing.actions.ActionList;
import com.tommustbe12.housing.actions.placeholders.Placeholders;
import com.tommustbe12.housing.actions.placeholders.VariablesStore;
import com.tommustbe12.housing.actions.storage.SimpleActionCodec;
import com.tommustbe12.housing.actions.storage.SimpleActionSerializer;
import com.tommustbe12.housing.chat.ChatPrompts;
import com.tommustbe12.housing.debug.Debug;
import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.items.ItemActionsStorage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.UUID;

public final class ItemEditGui {
    private static final String TITLE = "Edit Item";
    private static final String TITLE_ENCHANTS = "Edit Enchants";

    private final Plugin plugin;
    private final Debug debug;
    private final ChatPrompts prompts;
    private final ActionsEditor actionsEditor;
    private final HouseManager houses;

    private final NamespacedKey itemIdKey;
    private final ItemActionsStorage itemActionsStorage;
    private final SimpleActionSerializer serializer = new SimpleActionSerializer();
    private final SimpleActionCodec codec;

    public ItemEditGui(Plugin plugin, Debug debug, ChatPrompts prompts, ActionsEditor actionsEditor, HouseManager houses) {
        this.plugin = plugin;
        this.debug = debug;
        this.prompts = prompts;
        this.actionsEditor = actionsEditor;
        this.houses = houses;
        this.itemIdKey = new NamespacedKey(plugin, "item_action_id");
        this.itemActionsStorage = new ItemActionsStorage(plugin);
        VariablesStore vars = new VariablesStore(plugin);
        Placeholders placeholders = new Placeholders(vars);
        this.codec = new SimpleActionCodec(placeholders, vars, houses, (ctx, fn, global) -> {});
    }

    public boolean isTitle(String title) {
        return TITLE.equals(title) || TITLE_ENCHANTS.equals(title);
    }

    public void open(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType().isAir()) {
            player.sendMessage("§cHold an item first.");
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        fill(inv);
        inv.setItem(11, named(Material.NAME_TAG, "§aRename", List.of("§7Set display name (& codes).")));
        inv.setItem(13, named(Material.ENCHANTED_BOOK, "§bEnchantments", List.of("§7Add/remove enchants.")));
        inv.setItem(15, named(Material.REDSTONE, "§eClick Actions", List.of("§7Edit actions run on right-click.")));
        inv.setItem(26, named(Material.ARROW, "§7Close", List.of("§7Exit editor.")));
        player.openInventory(inv);
    }

    public void handleClick(Player player, String title, int rawSlot, ItemStack clicked) {
        if (TITLE.equals(title)) {
            switch (clicked.getType()) {
                case ARROW -> player.closeInventory();
                case NAME_TAG -> prompts.prompt(player, "Enter item name (& codes). Use 'none' to clear:", msg -> {
                    if (msg.equalsIgnoreCase("cancel")) return;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        ItemStack held = player.getInventory().getItemInMainHand();
                        if (held == null || held.getType().isAir()) return;
                        ItemMeta meta = held.getItemMeta();
                        if (msg.equalsIgnoreCase("none")) {
                            meta.setDisplayName(null);
                        } else {
                            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', msg));
                        }
                        held.setItemMeta(meta);
                        player.sendMessage("§aUpdated name.");
                        open(player);
                    });
                });
                case ENCHANTED_BOOK -> openEnchants(player);
                case REDSTONE -> openClickActions(player);
            }
        } else if (TITLE_ENCHANTS.equals(title)) {
            if (clicked.getType() == Material.ARROW) {
                open(player);
                return;
            }
            if (clicked.getType() == Material.ANVIL) {
                prompts.prompt(player, "Enter enchant like SHARPNESS=5 or clear:", msg -> {
                    if (msg.equalsIgnoreCase("cancel")) return;
                    Bukkit.getScheduler().runTask(plugin, () -> applyEnchantInput(player, msg));
                });
            }
            if (clicked.getType() == Material.BARRIER) {
                Bukkit.getScheduler().runTask(plugin, () -> applyEnchantInput(player, "clear"));
            }
        }
    }

    private void openEnchants(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_ENCHANTS);
        fill(inv);
        inv.setItem(11, named(Material.ANVIL, "§aAdd/Set Enchant", List.of("§7Type in chat: SHARPNESS=5", "§7or 'clear'")));
        inv.setItem(15, named(Material.BARRIER, "§cClear Enchants", List.of("§7Remove all enchants.")));
        inv.setItem(26, named(Material.ARROW, "§7Back", List.of("§7Return.")));
        player.openInventory(inv);
    }

    private void applyEnchantInput(Player player, String msg) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType().isAir()) return;
        if (msg.equalsIgnoreCase("clear")) {
            held.getEnchantments().keySet().forEach(held::removeEnchantment);
            player.sendMessage("§aCleared enchants.");
            openEnchants(player);
            return;
        }
        String[] parts = msg.split("=", 2);
        if (parts.length < 2) return;
        Enchantment ench = Enchantment.getByName(parts[0].trim().toUpperCase());
        if (ench == null) {
            player.sendMessage("§cUnknown enchant.");
            return;
        }
        try {
            int level = Integer.parseInt(parts[1].trim());
            held.addUnsafeEnchantment(ench, level);
            player.sendMessage("§aEnchant set.");
            openEnchants(player);
        } catch (NumberFormatException ignored) {
        }
    }

    private void openClickActions(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        UUID itemId = ensureItemId(held);

        // In V1, item click actions are global (not per-house); later we can scope to house.
        ActionList list = itemActionsStorage.load(itemId, codec);
        actionsEditor.openStandalone(player, "item_click", list, updated -> itemActionsStorage.save(itemId, updated, serializer), () -> open(player));
    }

    private UUID ensureItemId(ItemStack held) {
        ItemMeta meta = held.getItemMeta();
        String existing = meta.getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
        UUID id = existing == null ? UUID.randomUUID() : UUID.fromString(existing);
        meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, id.toString());
        held.setItemMeta(meta);
        return id;
    }

    public UUID getItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String v = item.getItemMeta().getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
        if (v == null) return null;
        try {
            return UUID.fromString(v);
        } catch (IllegalArgumentException e) {
            return null;
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
}
