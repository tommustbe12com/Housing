package com.tommustbe12.housing.gui;

import com.tommustbe12.housing.chat.ChatPrompts;
import com.tommustbe12.housing.util.HousingItems;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ItemsGui {
    private static final String TITLE = "Items";
    private static final String TITLE_SKULLS = "Skulls";

    private final Plugin plugin;
    private final ChatPrompts prompts;
    private final Map<UUID, Runnable> skullBack = new ConcurrentHashMap<>();

    public ItemsGui(Plugin plugin, ChatPrompts prompts) {
        this.plugin = plugin;
        this.prompts = prompts;
    }

    public boolean isTitle(String title) {
        return TITLE.equals(title) || TITLE_SKULLS.equals(title);
    }

    public void open(Player player, Runnable backToMain) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        fill(inv);
        inv.setItem(20, HousingItems.createNpcPlacerItem(plugin));
        inv.setItem(22, HousingItems.createHologramPlacerItem(plugin));
        inv.setItem(24, named(Material.PLAYER_HEAD, "§bSkulls", List.of("§7Get player skulls.", "§7Click to open.")));
        inv.setItem(49, named(Material.ARROW, "§7Back", List.of("§7Return to main menu.")));
        player.openInventory(inv);
    }

    public void handleClick(Player player, String title, int rawSlot, ItemStack clicked, Runnable backToMain) {
        if (clicked == null) return;

        if (TITLE_SKULLS.equals(title)) {
            if (clicked.getType() == Material.ARROW) {
                Runnable back = skullBack.remove(player.getUniqueId());
                if (back != null) back.run();
                else open(player, backToMain);
                return;
            }
            if (clicked.getType() == Material.ANVIL) {
                prompts.prompt(player, "Type a username for the skull:", msg -> {
                    if (msg.equalsIgnoreCase("cancel")) return;
                    String name = msg.trim();
                    if (name.isBlank()) return;
                    OfflinePlayer off = Bukkit.getOfflinePlayer(name);
                    ItemStack skull = playerSkull(off);
                    player.getInventory().addItem(skull);
                    player.sendMessage("§aGiven skull for §e" + name + "§a.");
                    Bukkit.getScheduler().runTask(plugin, () -> openSkulls(player, skullBack.getOrDefault(player.getUniqueId(), () -> open(player, backToMain))));
                });
                return;
            }
            if (clicked.getType() == Material.PLAYER_HEAD) {
                String name = clicked.hasItemMeta() ? clicked.getItemMeta().getDisplayName() : "";
                name = name == null ? "" : name.replace("§f", "").trim();
                if (name.isBlank()) return;
                OfflinePlayer off = Bukkit.getOfflinePlayer(name);
                ItemStack skull = playerSkull(off);
                player.getInventory().addItem(skull);
                player.sendMessage("§aGiven skull for §e" + name + "§a.");
            }
            return;
        }

        if (HousingItems.isNpcPlacerItem(plugin, clicked)) {
            player.getInventory().addItem(HousingItems.createNpcPlacerItem(plugin));
            player.sendMessage("§aGiven NPC placer.");
            return;
        }
        if (HousingItems.isHologramPlacerItem(plugin, clicked)) {
            player.getInventory().addItem(HousingItems.createHologramPlacerItem(plugin));
            player.sendMessage("§aGiven hologram placer.");
            return;
        }
        if (clicked.getType() == Material.PLAYER_HEAD) {
            openSkulls(player, () -> open(player, backToMain));
            return;
        }
        if (clicked.getType() == Material.ARROW) backToMain.run();
    }

    private void openSkulls(Player player, Runnable backToItems) {
        skullBack.put(player.getUniqueId(), backToItems == null ? () -> {} : backToItems);
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_SKULLS);
        fill(inv);
        inv.setItem(49, named(Material.ANVIL, "§eSearch Username", List.of("§7Click then type a username.", "§7Type 'cancel' to cancel.")));
        inv.setItem(53, named(Material.ARROW, "§7Back", List.of("§7Return to Items.")));

        List<String> presets = List.of("MHF_Steve", "MHF_Alex", "MHF_Creeper", "MHF_Skeleton", "MHF_Zombie");
        int i = 0;
        for (String name : presets) {
            if (i >= 45) break;
            inv.setItem(i++, namedSkull(name));
        }
        player.openInventory(inv);
    }

    private static ItemStack namedSkull(String username) {
        OfflinePlayer off = Bukkit.getOfflinePlayer(username);
        ItemStack skull = playerSkull(off);
        ItemMeta meta = skull.getItemMeta();
        meta.setDisplayName("§f" + username);
        meta.setLore(List.of("§7Click to get this skull."));
        skull.setItemMeta(meta);
        return skull;
    }

    private static ItemStack playerSkull(OfflinePlayer off) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwningPlayer(off);
        skull.setItemMeta(meta);
        return skull;
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

