package com.tommustbe12.housing.gui;

import com.tommustbe12.housing.chat.ChatPrompts;
import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.houses.HouseSlot;
import com.tommustbe12.housing.npcs.NpcBehavior;
import com.tommustbe12.housing.npcs.NpcData;
import com.tommustbe12.housing.npcs.NpcManager;
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

public final class NpcsGui {
    private static final String TITLE_LIST = "NPCs";
    private static final String TITLE_EDIT_PREFIX = "NPC: ";
    private static final String TITLE_EQUIP = "NPC Equipment";

    private final Plugin plugin;
    private final ChatPrompts prompts;
    private final HouseManager houses;
    private final NpcManager npcs;
    private final ActionsEditor actionsEditor;

    private final ConcurrentHashMap<UUID, UUID> equipSessionNpcId = new ConcurrentHashMap<>();

    public NpcsGui(Plugin plugin, ChatPrompts prompts, HouseManager houses, NpcManager npcs, ActionsEditor actionsEditor) {
        this.plugin = plugin;
        this.prompts = prompts;
        this.houses = houses;
        this.npcs = npcs;
        this.actionsEditor = actionsEditor;
    }

    public boolean isTitle(String title) {
        return TITLE_LIST.equals(title) || (title != null && title.startsWith(TITLE_EDIT_PREFIX)) || TITLE_EQUIP.equals(title);
    }

    public void open(Player player) {
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null || !info.owner().equals(player.getUniqueId())) {
            player.sendMessage("§cNPCs can only be edited in your own house.");
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_LIST);
        fill(inv);
        int i = 0;
        for (NpcData npc : npcs.getNpcs(info.owner(), info.slot(), player.getWorld())) {
            inv.setItem(i++, named(Material.ARMOR_STAND, "§f" + npc.name(), List.of("§7Shift-right-click NPC to edit", "§7Click to edit")));
            if (i >= 45) break;
        }
        inv.setItem(49, named(Material.ANVIL, "§aCreate NPC Here", List.of("§7Creates an NPC at your position.")));
        inv.setItem(53, named(Material.ARROW, "§7Back", List.of("§7Return.")));
        player.openInventory(inv);
    }

    public void openNpcEditor(Player player, UUID owner, HouseSlot slot, NpcData npc) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_EDIT_PREFIX + npc.name());
        fill(inv);
        inv.setItem(11, named(Material.NAME_TAG, "§aName", List.of("§7Current: §f" + npc.name(), "§7Click to rename")));
        inv.setItem(12, named(Material.TNT, "§eBehavior", List.of("§7Current: §f" + npc.behavior().name(), "§7Click to cycle")));
        inv.setItem(13, named(Material.CHEST, "§bEquipment", List.of("§7Edit armor/hand/offhand")));
        inv.setItem(14, named(Material.ANVIL, "§dActions", List.of("§7Edit click actions")));
        inv.setItem(16, named(Material.PLAYER_HEAD, "§bSkin", List.of("§7Current: §f" + npc.skinUsername(), "§7Click to set username")));
        inv.setItem(15, named(Material.BARRIER, "§cDelete", List.of("§7Deletes this NPC.")));
        inv.setItem(26, named(Material.ARROW, "§7Back", List.of("§7Return.")));
        player.openInventory(inv);
    }

    public void handleClick(Player player, String title, int rawSlot, ItemStack clicked, org.bukkit.event.inventory.ClickType clickType, Runnable backToSystems) {
        if (clicked == null || clicked.getType().isAir()) return;
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null || !info.owner().equals(player.getUniqueId())) return;

        if (TITLE_LIST.equals(title)) {
            if (rawSlot == 53) { backToSystems.run(); return; }
            if (rawSlot == 49) {
                NpcData npc = npcs.createNpc(info.owner(), info.slot(), player.getWorld(), player.getLocation());
                openNpcEditor(player, info.owner(), info.slot(), npc);
                return;
            }
            if (rawSlot >= 0 && rawSlot < 45) {
                int idx = rawSlot;
                var list = npcs.getNpcs(info.owner(), info.slot(), player.getWorld());
                if (idx >= list.size()) return;
                openNpcEditor(player, info.owner(), info.slot(), list.get(idx));
            }
            return;
        }

        if (TITLE_EQUIP.equals(title)) {
            if (clicked.getType() == Material.ARROW) {
                UUID npcId = equipSessionNpcId.get(player.getUniqueId());
                if (npcId == null) { open(player); return; }
                for (NpcData npc : npcs.getNpcs(info.owner(), info.slot(), player.getWorld())) {
                    if (npc.id().equals(npcId)) { openNpcEditor(player, info.owner(), info.slot(), npc); return; }
                }
                open(player);
            }
            return;
        }

        if (title != null && title.startsWith(TITLE_EDIT_PREFIX)) {
            NpcData npc = findNpcByTitle(info.owner(), info.slot(), player, title);
            if (npc == null) return;
            if (clicked.getType() == Material.ARROW) { open(player); return; }
            if (clicked.getType() == Material.NAME_TAG) {
                prompts.prompt(player, "Enter NPC name:", msg -> {
                    if (msg.equalsIgnoreCase("cancel")) return;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        npc.setName(msg.trim());
                        npcs.save(info.owner(), info.slot(), npcs.getNpcs(info.owner(), info.slot(), player.getWorld()));
                        npcs.refreshNpcEntity(info.owner(), info.slot(), player.getWorld(), npc);
                        openNpcEditor(player, info.owner(), info.slot(), npc);
                    });
                });
                return;
            }
            if (clicked.getType() == Material.TNT) {
                npc.setBehavior(NpcBehavior.next(npc.behavior()));
                npcs.save(info.owner(), info.slot(), npcs.getNpcs(info.owner(), info.slot(), player.getWorld()));
                openNpcEditor(player, info.owner(), info.slot(), npc);
                return;
            }
            if (clicked.getType() == Material.CHEST) {
                openEquip(player, npc);
                return;
            }
            if (clicked.getType() == Material.PLAYER_HEAD) {
                prompts.prompt(player, "Enter skin username:", msg -> {
                    if (msg.equalsIgnoreCase("cancel")) return;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        npc.setSkinUsername(msg.trim());
                        npcs.save(info.owner(), info.slot(), npcs.getNpcs(info.owner(), info.slot(), player.getWorld()));
                        npcs.refreshNpcEntity(info.owner(), info.slot(), player.getWorld(), npc);
                        openNpcEditor(player, info.owner(), info.slot(), npc);
                    });
                });
                return;
            }
            if (clicked.getType() == Material.BARRIER) {
                npcs.deleteNpc(info.owner(), info.slot(), player.getWorld(), npc.id());
                open(player);
                player.sendMessage("§aNPC deleted.");
                return;
            }
            if (clicked.getType() == Material.ANVIL) {
                actionsEditor.openStandaloneHouse(player, info.owner(), info.slot(), npc.name(), npc.clickActions(), updated -> {
                    npcs.save(info.owner(), info.slot(), npcs.getNpcs(info.owner(), info.slot(), player.getWorld()));
                }, () -> openNpcEditor(player, info.owner(), info.slot(), npc));
            }
        }
    }

    private void openEquip(Player player, NpcData npc) {
        equipSessionNpcId.put(player.getUniqueId(), npc.id());
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_EQUIP);

        ItemStack pane = named(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, pane);

        inv.setItem(1, named(Material.PAPER, "§7Helmet", List.of("§8Put item below")));
        inv.setItem(2, named(Material.PAPER, "§7Chestplate", List.of("§8Put item below")));
        inv.setItem(3, named(Material.PAPER, "§7Leggings", List.of("§8Put item below")));
        inv.setItem(4, named(Material.PAPER, "§7Boots", List.of("§8Put item below")));
        inv.setItem(6, named(Material.PAPER, "§7Hand", List.of("§8Put item below")));
        inv.setItem(7, named(Material.PAPER, "§7Offhand", List.of("§8Put item below")));

        // editable slots
        inv.setItem(10, npc.helmet());
        inv.setItem(11, npc.chestplate());
        inv.setItem(12, npc.leggings());
        inv.setItem(13, npc.boots());
        inv.setItem(15, npc.hand());
        inv.setItem(16, npc.offhand());

        inv.setItem(26, named(Material.ARROW, "§7Back", List.of("§7Return.")));
        player.openInventory(inv);
    }

    public void handleEquipClose(Player player, Inventory inv) {
        UUID npcId = equipSessionNpcId.remove(player.getUniqueId());
        if (npcId == null) return;
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null || !info.owner().equals(player.getUniqueId())) return;

        NpcData npc = null;
        for (NpcData n : npcs.getNpcs(info.owner(), info.slot(), player.getWorld())) {
            if (n.id().equals(npcId)) { npc = n; break; }
        }
        if (npc == null) return;

        npc.setHelmet(inv.getItem(10));
        npc.setChestplate(inv.getItem(11));
        npc.setLeggings(inv.getItem(12));
        npc.setBoots(inv.getItem(13));
        npc.setHand(inv.getItem(15));
        npc.setOffhand(inv.getItem(16));
        npcs.save(info.owner(), info.slot(), npcs.getNpcs(info.owner(), info.slot(), player.getWorld()));
        npcs.refreshNpcEntity(info.owner(), info.slot(), player.getWorld(), npc);
    }

    private NpcData findNpcByTitle(UUID owner, HouseSlot slot, Player player, String title) {
        String name = title.substring(TITLE_EDIT_PREFIX.length());
        for (NpcData npc : npcs.getNpcs(owner, slot, player.getWorld())) {
            if (npc.name().equals(name)) return npc;
        }
        return null;
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
