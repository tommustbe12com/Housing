package com.tommustbe12.housing.gui;

import com.tommustbe12.housing.chat.ChatPrompts;
import com.tommustbe12.housing.holograms.HologramData;
import com.tommustbe12.housing.holograms.HologramsRuntime;
import com.tommustbe12.housing.holograms.HologramsService;
import com.tommustbe12.housing.houses.HouseManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HologramsGui {
    private static final String TITLE = "Edit Hologram";
    private static final int MAX_LINES = 10;

    private final Plugin plugin;
    private final ChatPrompts prompts;
    private final HouseManager houses;
    private final HologramsService holograms;
    private final HologramsRuntime runtime;

    private final Map<UUID, UUID> editingHologramId = new ConcurrentHashMap<>();

    public HologramsGui(Plugin plugin, ChatPrompts prompts, HouseManager houses, HologramsService holograms, HologramsRuntime runtime) {
        this.plugin = plugin;
        this.prompts = prompts;
        this.houses = houses;
        this.holograms = holograms;
        this.runtime = runtime;
    }

    public boolean isTitle(String title) {
        return TITLE.equals(title);
    }

    public void open(Player player, UUID hologramId) {
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null) return;
        HologramData h = holograms.find(info.owner(), info.slot(), player.getWorld(), hologramId);
        if (h == null) return;
        editingHologramId.put(player.getUniqueId(), hologramId);

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        fill(inv);
        inv.setItem(45, named(Material.LIME_CONCRETE, "§aSave", List.of("§7Save hologram changes.")));
        inv.setItem(53, named(Material.ARROW, "§7Back", List.of("§7Return.")));
        inv.setItem(49, named(Material.BARRIER, "§cDelete", List.of("§7Delete this hologram.")));
        inv.setItem(46, named(Material.GREEN_DYE, "§aAdd Line", List.of("§7Max 10 lines.")));
        inv.setItem(47, named(Material.RED_DYE, "§cRemove Line", List.of("§7Remove last line.")));

        List<String> lines = h.lines();
        for (int i = 0; i < Math.min(lines.size(), MAX_LINES); i++) {
            inv.setItem(10 + i, named(Material.PAPER, "§fLine " + (i + 1), List.of("§7Click to edit", "§7Current: " + lines.get(i))));
        }
        player.openInventory(inv);
    }

    public void handleClick(Player player, int rawSlot, ItemStack clicked) {
        if (clicked == null || clicked.getType().isAir()) return;
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null) return;
        UUID hid = editingHologramId.get(player.getUniqueId());
        if (hid == null) return;
        HologramData h = holograms.find(info.owner(), info.slot(), player.getWorld(), hid);
        if (h == null) return;

        if (clicked.getType() == Material.ARROW) {
            editingHologramId.remove(player.getUniqueId());
            player.closeInventory();
            return;
        }
        if (clicked.getType() == Material.GREEN_DYE) {
            if (h.lines().size() >= MAX_LINES) return;
            h.lines().add("§fNew line");
            holograms.save(info.owner(), info.slot(), player.getWorld());
            runtime.spawnOrUpdate(info.owner(), info.slot(), player.getWorld(), h);
            open(player, hid);
            return;
        }
        if (clicked.getType() == Material.RED_DYE) {
            if (!h.lines().isEmpty()) h.lines().remove(h.lines().size() - 1);
            holograms.save(info.owner(), info.slot(), player.getWorld());
            runtime.spawnOrUpdate(info.owner(), info.slot(), player.getWorld(), h);
            open(player, hid);
            return;
        }
        if (clicked.getType() == Material.BARRIER) {
            var list = holograms.get(info.owner(), info.slot(), player.getWorld());
            list.removeIf(x -> x.id().equals(hid));
            holograms.save(info.owner(), info.slot(), player.getWorld());
            runtime.despawn(player.getWorld(), hid);
            editingHologramId.remove(player.getUniqueId());
            player.closeInventory();
            player.sendMessage("§aHologram deleted.");
            return;
        }
        if (clicked.getType() == Material.LIME_CONCRETE) {
            holograms.save(info.owner(), info.slot(), player.getWorld());
            runtime.spawnAll(info.owner(), info.slot(), player.getWorld());
            player.sendMessage("§aHologram saved.");
            return;
        }

        if (clicked.getType() == Material.PAPER) {
            int idx = rawSlot - 10;
            if (idx < 0 || idx >= h.lines().size()) return;
            int lineIndex = idx;
            prompts.prompt(player, "Enter line text (& colors, placeholders ok):", msg -> {
                if (msg.equalsIgnoreCase("cancel")) return;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    h.lines().set(lineIndex, msg);
                    holograms.save(info.owner(), info.slot(), player.getWorld());
                    runtime.spawnOrUpdate(info.owner(), info.slot(), player.getWorld(), h);
                    open(player, hid);
                });
            });
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
        for (int i = 0; i < 10; i++) inv.setItem(10 + i, null);
    }
}
