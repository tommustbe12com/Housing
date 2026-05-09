package com.tommustbe12.housing.gui;

import com.tommustbe12.housing.chat.ChatPrompts;
import com.tommustbe12.housing.groups.HouseGroup;
import com.tommustbe12.housing.groups.HouseGroupsData;
import com.tommustbe12.housing.groups.HouseGroupsService;
import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.tags.OwnerTagService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerSettingsGui {
    private static final String TITLE_PREFIX = "Settings for ";

    private final Plugin plugin;
    private final ChatPrompts prompts;
    private final HouseManager houses;
    private final HouseGroupsService groups;
    private final OwnerTagService tags;

    private final Map<UUID, UUID> targetByViewer = new ConcurrentHashMap<>();

    public PlayerSettingsGui(Plugin plugin, ChatPrompts prompts, HouseManager houses, HouseGroupsService groups, OwnerTagService tags) {
        this.plugin = plugin;
        this.prompts = prompts;
        this.houses = houses;
        this.groups = groups;
        this.tags = tags;
    }

    public boolean isTitle(String title) {
        return title != null && title.startsWith(TITLE_PREFIX);
    }

    public void open(Player viewer, Player target, Runnable back) {
        var info = houses.getHouseInfoByWorld(viewer.getWorld());
        if (info == null) return;
        targetByViewer.put(viewer.getUniqueId(), target.getUniqueId());

        Inventory inv = Bukkit.createInventory(null, 54, TITLE_PREFIX + target.getName());
        fill(inv);

        HouseGroupsData data = groups.groups(info.owner(), info.slot());
        List<UUID> order = new ArrayList<>();
        if (data.visitorId() != null) order.add(data.visitorId());
        if (data.coOwnerId() != null) order.add(data.coOwnerId());
        if (data.ownerId() != null) order.add(data.ownerId());
        for (UUID id : data.groups().keySet()) if (!order.contains(id)) order.add(id);

        int i = 0;
        for (UUID gid : order) {
            HouseGroup g = data.get(gid);
            if (g == null) continue;
            inv.setItem(i++, named(Material.PLAYER_HEAD, "§f" + g.name(), List.of("§7Click to set group.")));
            if (i >= 36) break;
        }

        boolean muted = groups.isMuted(info.owner(), info.slot(), target.getUniqueId());
        boolean banned = groups.isBanned(info.owner(), info.slot(), target.getUniqueId());
        inv.setItem(45, named(Material.CHEST, "§eClear Inventory", List.of("§7Click to clear target inventory.")));
        inv.setItem(46, named(Material.BARRIER, "§cKick", List.of("§7Kicks them from the house.")));
        inv.setItem(47, named(Material.RED_CONCRETE, banned ? "§cUnban" : "§cBan", List.of("§7Toggle house ban.")));
        inv.setItem(48, named(Material.GRAY_DYE, muted ? "§cUnmute" : "§aMute", List.of("§7Toggle house mute.")));
        inv.setItem(53, named(Material.ARROW, "§7Back", List.of("§7Return.")));

        viewer.openInventory(inv);
    }

    public void handleClick(Player viewer, String title, int rawSlot, ItemStack clicked, Runnable back) {
        if (clicked == null || clicked.getType().isAir()) return;
        var info = houses.getHouseInfoByWorld(viewer.getWorld());
        if (info == null) return;

        UUID targetId = targetByViewer.get(viewer.getUniqueId());
        if (targetId == null) return;
        Player target = Bukkit.getPlayer(targetId);
        OfflinePlayer offline = target != null ? target : Bukkit.getOfflinePlayer(targetId);

        if (clicked.getType() == Material.ARROW) { back.run(); return; }

        if (rawSlot >= 0 && rawSlot < 36) {
            UUID gid = groupIdAtSlot(groups.groups(info.owner(), info.slot()), rawSlot);
            if (gid == null) return;
            if (targetId.equals(info.owner())) {
                viewer.sendMessage("§cYou cannot change the owner's group.");
                return;
            }
            if (!groups.canAssignGroup(info.owner(), info.slot(), viewer, gid)) {
                viewer.sendMessage("§cYou cannot assign that group.");
                return;
            }
            if (gid.equals(groups.groups(info.owner(), info.slot()).coOwnerId())) {
                viewer.sendMessage("§eWarning: Setting someone to Co-Owner is dangerous.");
            }
            groups.setGroup(info.owner(), info.slot(), targetId, gid);
            viewer.sendMessage("§aUpdated group.");
            if (target != null) {
                String tag = groups.tagForDisplay(info.owner(), info.slot(), target.getUniqueId());
                tags.applyTag(target, tag);
                groups.applyDefaultModeIfNeeded(target);
                boolean canFly = groups.has(info.owner(), info.slot(), target.getUniqueId(), com.tommustbe12.housing.groups.HousePermission.FLY);
                target.setAllowFlight(canFly);
                if (!canFly) target.setFlying(false);
            }
            open(viewer, target != null ? target : viewer, back);
            return;
        }

        if (clicked.getType() == Material.CHEST) {
            if (target == null) {
                viewer.sendMessage("§cThat player is not online.");
                return;
            }
            target.getInventory().clear();
            viewer.sendMessage("§aCleared inventory.");
            return;
        }
        if (clicked.getType() == Material.BARRIER) {
            if (target != null) {
                houses.sendToHub(target);
                viewer.sendMessage("§aKicked.");
            }
            return;
        }
        if (clicked.getType() == Material.RED_CONCRETE) {
            boolean banned = groups.isBanned(info.owner(), info.slot(), targetId);
            groups.setBanned(info.owner(), info.slot(), targetId, !banned);
            viewer.sendMessage(!banned ? "§cBanned." : "§aUnbanned.");
            if (!banned && target != null) houses.sendToHub(target);
            return;
        }
        if (clicked.getType() == Material.GRAY_DYE) {
            boolean muted = groups.isMuted(info.owner(), info.slot(), targetId);
            groups.setMuted(info.owner(), info.slot(), targetId, !muted);
            viewer.sendMessage(!muted ? "§aMuted." : "§aUnmuted.");
        }
    }

    private static UUID groupIdAtSlot(HouseGroupsData data, int slot) {
        if (slot < 0 || slot >= 36) return null;
        List<UUID> order = new ArrayList<>();
        if (data.visitorId() != null) order.add(data.visitorId());
        if (data.coOwnerId() != null) order.add(data.coOwnerId());
        if (data.ownerId() != null) order.add(data.ownerId());
        for (UUID id : data.groups().keySet()) if (!order.contains(id)) order.add(id);
        if (slot >= order.size()) return null;
        return order.get(slot);
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
