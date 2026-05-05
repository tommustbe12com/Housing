package com.tommustbe12.housing.gui;

import com.tommustbe12.housing.chat.ChatPrompts;
import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.houses.HouseSlot;
import com.tommustbe12.housing.scoreboard.HouseScoreboardService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ScoreboardEditorGui {
    private static final String TITLE = "Scoreboard Editor";

    private final Plugin plugin;
    private final ChatPrompts prompts;
    private final HouseManager houses;
    private final HouseScoreboardService scoreboards;

    public ScoreboardEditorGui(Plugin plugin, ChatPrompts prompts, HouseManager houses, HouseScoreboardService scoreboards) {
        this.plugin = plugin;
        this.prompts = prompts;
        this.houses = houses;
        this.scoreboards = scoreboards;
    }

    public boolean isTitle(String title) {
        return TITLE.equals(title);
    }

    public void open(Player player, UUID owner, HouseSlot slot) {
        List<String> lines = new ArrayList<>(scoreboards.storage().load(owner, slot));
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        fill(inv);
        for (int i = 0; i < Math.min(15, lines.size()); i++) {
            inv.setItem(i, lineItem(i, lines.get(i)));
        }
        inv.setItem(49, named(Material.ANVIL, "§aAdd Line", List.of("§7Add a new line.")));
        inv.setItem(53, named(Material.ARROW, "§7Back", List.of("§7Return.")));
        player.openInventory(inv);
    }

    public void handleClick(Player player, int rawSlot, ItemStack clicked, Runnable back) {
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null || !info.owner().equals(player.getUniqueId())) return;
        UUID owner = info.owner();
        HouseSlot slot = info.slot();
        List<String> lines = new ArrayList<>(scoreboards.storage().load(owner, slot));

        if (rawSlot == 53) {
            back.run();
            return;
        }
        if (rawSlot == 49) {
            prompts.prompt(player, "Enter new line (& codes allowed):", msg -> {
                if (msg.equalsIgnoreCase("cancel")) return;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    lines.add(msg);
                    scoreboards.storage().save(owner, slot, lines);
                    open(player, owner, slot);
                });
            });
            return;
        }
        if (rawSlot >= 0 && rawSlot < 15 && rawSlot < lines.size()) {
            if (clicked.getType() == Material.PAPER) {
                int idx = rawSlot;
                prompts.prompt(player, "Edit line " + (idx + 1) + " (or 'delete'):", msg -> {
                    if (msg.equalsIgnoreCase("cancel")) return;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (msg.equalsIgnoreCase("delete")) {
                            lines.remove(idx);
                        } else {
                            lines.set(idx, msg);
                        }
                        scoreboards.storage().save(owner, slot, lines);
                        open(player, owner, slot);
                    });
                });
            }
        }
    }

    private static ItemStack lineItem(int index, String raw) {
        String preview = ChatColor.translateAlternateColorCodes('&', raw);
        return named(Material.PAPER, "§fLine " + (index + 1), List.of("§7" + preview, "§7Click to edit"));
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

