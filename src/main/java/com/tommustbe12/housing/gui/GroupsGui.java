package com.tommustbe12.housing.gui;

import com.tommustbe12.housing.chat.ChatPrompts;
import com.tommustbe12.housing.groups.*;
import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.houses.HouseSlot;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class GroupsGui {
    private static final String TITLE_LIST = "Groups";
    private static final String TITLE_EDIT_PREFIX = "Edit Group: ";
    private static final String TITLE_PERMS_PREFIX = "Group Perms: ";

    private final Plugin plugin;
    private final ChatPrompts prompts;
    private final HouseManager houses;
    private final HouseGroupsService groups;

    private final Map<UUID, UUID> editingGroup = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> permsPage = new ConcurrentHashMap<>();
    private final Map<UUID, List<UUID>> listViewOrder = new ConcurrentHashMap<>();

    public GroupsGui(Plugin plugin, ChatPrompts prompts, HouseManager houses, HouseGroupsService groups) {
        this.plugin = plugin;
        this.prompts = prompts;
        this.houses = houses;
        this.groups = groups;
    }

    public boolean isTitle(String title) {
        return TITLE_LIST.equals(title)
                || (title != null && title.startsWith(TITLE_EDIT_PREFIX))
                || (title != null && title.startsWith(TITLE_PERMS_PREFIX));
    }

    public void open(Player player, Runnable back) {
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null || !info.owner().equals(player.getUniqueId())) {
            player.sendMessage("§cGroups can only be edited in your own house.");
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_LIST);
        fill(inv);

        HouseGroupsData data = groups.groups(info.owner(), info.slot());
        data.ensureDefaultsPresent();
        List<UUID> order = groupsInDisplayOrder(data);
        listViewOrder.put(player.getUniqueId(), order);

        int i = 0;
        for (UUID gid : order) {
            HouseGroup g = data.get(gid);
            if (g == null) continue;
            inv.setItem(i++, groupListItem(data, g));
            if (i >= 45) break;
        }

        inv.setItem(49, named(Material.OAK_SIGN, "§aCreate Group", List.of("§7Click to create a new group.")));
        inv.setItem(53, named(Material.ARROW, "§7Back", List.of("§7Return.")));
        player.openInventory(inv);
    }

    public void handleClick(Player player, String title, int rawSlot, ItemStack clicked, org.bukkit.event.inventory.ClickType click, Runnable back) {
        if (clicked == null || clicked.getType().isAir()) return;
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null || !info.owner().equals(player.getUniqueId())) return;

        HouseGroupsData data = groups.groups(info.owner(), info.slot());

        if (TITLE_LIST.equals(title)) {
            if (clicked.getType() == Material.ARROW) {
                back.run();
                return;
            }
            if (clicked.getType() == Material.OAK_SIGN) {
                if (data.groups().size() >= 15) {
                    player.sendMessage("§cYou cannot create more than 15 groups.");
                    return;
                }
                prompts.prompt(player, "Enter group name:", msg -> {
                    if (msg.equalsIgnoreCase("cancel")) return;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        data.ensureDefaultsPresent();
                        String name = msg.trim();
                        int priority = nextCustomPriority(data);
                        HouseGroup g = new HouseGroup(UUID.randomUUID(), name, "&7[" + name.toUpperCase(Locale.ROOT) + "]", priority, DefaultGameMode.ADVENTURE);
                        // Default perms: same as Visitor (safe baseline)
                        HouseGroup visitor = data.get(data.visitorId());
                        if (visitor != null) {
                            for (HousePermission p : HousePermission.values()) g.set(p, visitor.has(p));
                        }
                        data.groups().put(g.id(), g);
                        groups.save(info.owner(), info.slot());
                        openEdit(player, info.owner(), info.slot(), g);
                    });
                });
                return;
            }

            UUID gid = groupIdAtSlot(player, rawSlot);
            if (gid == null) return;
            HouseGroup g = data.get(gid);
            if (g == null) return;
            openEdit(player, info.owner(), info.slot(), g);
            return;
        }

        if (title != null && title.startsWith(TITLE_EDIT_PREFIX)) {
            UUID gid = editingGroup.get(player.getUniqueId());
            HouseGroup g = data.get(gid);
            if (g == null) {
                open(player, back);
                return;
            }

            if (clicked.getType() == Material.ARROW) {
                open(player, back);
                return;
            }
            if (clicked.getType() == Material.PAPER) {
                prompts.prompt(player, "Enter new group name:", msg -> {
                    if (msg.equalsIgnoreCase("cancel")) return;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        g.setName(msg.trim());
                        groups.save(info.owner(), info.slot());
                        openEdit(player, info.owner(), info.slot(), g);
                    });
                });
                return;
            }
            if (clicked.getType() == Material.OAK_SIGN) {
                prompts.prompt(player, "Enter tag (use & codes), ex: &c&lADMIN:", msg -> {
                    if (msg.equalsIgnoreCase("cancel")) return;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        g.setTag(msg.trim());
                        groups.save(info.owner(), info.slot());
                        openEdit(player, info.owner(), info.slot(), g);
                    });
                });
                return;
            }
            if (clicked.getType() == Material.GOLD_INGOT) {
                int max = Math.max(1, data.groups().size());
                int next = g.priority() + 1;
                if (next > max) next = 1;
                g.setPriority(next);
                data.ensureDefaultsPresent();
                groups.save(info.owner(), info.slot());
                openEdit(player, info.owner(), info.slot(), g);
                return;
            }
            if (clicked.getType() == Material.DIAMOND) {
                if (gid.equals(data.ownerId())) {
                    player.sendMessage("§cOwner cannot be the default group.");
                    return;
                }
                player.sendMessage("§eWarning: Changing the default group affects all new members.");
                data.setDefaultGroupId(gid);
                groups.save(info.owner(), info.slot());
                openEdit(player, info.owner(), info.slot(), g);
                return;
            }
            if (clicked.getType() == Material.BOOK) {
                permsPage.put(player.getUniqueId(), 0);
                openPerms(player, g, 0);
                return;
            }
            if (clicked.getType() == Material.BARRIER) {
                if (gid.equals(data.ownerId())) {
                    player.sendMessage("§cYou cannot delete Owner.");
                    return;
                }
                if (gid.equals(data.defaultGroupId())) {
                    player.sendMessage("§cYou cannot delete the default group.");
                    return;
                }
                data.groups().remove(gid);
                groups.members(info.owner(), info.slot()).groupByPlayer().entrySet().removeIf(e -> e.getValue().equals(gid));
                data.ensureDefaultsPresent();
                groups.save(info.owner(), info.slot());
                player.sendMessage("§aGroup deleted.");
                open(player, back);
                return;
            }
            if (clicked.getType() == Material.LAVA_BUCKET) {
                if (gid.equals(data.ownerId())) {
                    player.sendMessage("§cYou cannot clear Owner members.");
                    return;
                }
                if (gid.equals(data.defaultGroupId())) {
                    player.sendMessage("§cYou cannot clear the default group (it is the fallback for all players).");
                    return;
                }
                groups.members(info.owner(), info.slot()).groupByPlayer().entrySet().removeIf(e -> e.getValue().equals(gid) && !e.getKey().equals(info.owner()));
                groups.save(info.owner(), info.slot());
                player.sendMessage("§aCleared group members.");
                openEdit(player, info.owner(), info.slot(), g);
                return;
            }
        }

        if (title != null && title.startsWith(TITLE_PERMS_PREFIX)) {
            UUID gid = editingGroup.get(player.getUniqueId());
            HouseGroup g = data.get(gid);
            if (g == null) {
                open(player, back);
                return;
            }
            int page = permsPage.getOrDefault(player.getUniqueId(), 0);

            if (clicked.getType() == Material.ARROW) {
                openEdit(player, info.owner(), info.slot(), g);
                return;
            }
            if (clicked.getType() == Material.PAPER) {
                int next = page + 1;
                permsPage.put(player.getUniqueId(), next);
                openPerms(player, g, next);
                return;
            }
            if (clicked.getType() == Material.MAP) {
                int prev = Math.max(0, page - 1);
                permsPage.put(player.getUniqueId(), prev);
                openPerms(player, g, prev);
                return;
            }

            PermissionEntry entry = permAt(rawSlot, page);
            if (entry == null) return;

            if (entry.perm == null) {
                DefaultGameMode next = switch (g.defaultGameMode()) {
                    case ADVENTURE -> DefaultGameMode.SURVIVAL;
                    case SURVIVAL -> DefaultGameMode.CREATIVE;
                    case CREATIVE -> DefaultGameMode.ADVENTURE;
                };
                if (next == DefaultGameMode.CREATIVE && !g.has(HousePermission.BUILD)) {
                    player.sendMessage("§cCreative requires BUILD permission.");
                    return;
                }
                g.setDefaultGameMode(next);
                groups.save(info.owner(), info.slot());
                openPerms(player, g, page);
                return;
            }

            boolean next = !g.has(entry.perm);
            g.set(entry.perm, next);
            if (entry.perm == HousePermission.BUILD && next) {
                player.sendMessage("§eWarning: Make sure you trust the person you give this permission to...");
            }
            groups.save(info.owner(), info.slot());
            openPerms(player, g, page);
        }
    }

    private void openEdit(Player player, UUID owner, HouseSlot slot, HouseGroup g) {
        editingGroup.put(player.getUniqueId(), g.id());
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_EDIT_PREFIX + g.name());
        fill(inv);

        boolean isDefault = g.id().equals(groups.groups(owner, slot).defaultGroupId());
        inv.setItem(11, named(Material.PAPER, "§eRename Group", List.of("§7Editor name only.")));
        inv.setItem(12, named(Material.OAK_SIGN, "§bEdit Tag", List.of("§7Current: §f" + ChatColor.translateAlternateColorCodes('&', g.tag()))));
        inv.setItem(13, named(Material.GOLD_INGOT, "§6Priority", List.of("§7Current: §f" + g.priority(), "§7Click to cycle.")));
        inv.setItem(14, named(Material.BOOK, "§aEdit Permissions", List.of("§7Click to open permissions.")));
        inv.setItem(29, named(Material.DIAMOND, isDefault ? "§bDefault Group §7(Selected)" : "§bSet As Default",
                List.of("§7Click to set default.", "§eWarning: affects new members.")));
        inv.setItem(49, named(Material.BARRIER, "§cDelete Group", List.of("§7Cannot delete Owner/default.")));
        inv.setItem(50, named(Material.LAVA_BUCKET, "§cClear Group Players", List.of("§7Removes all players from this group.")));
        inv.setItem(53, named(Material.ARROW, "§7Back", List.of("§7Return.")));
        player.openInventory(inv);
    }

    private void openPerms(Player player, HouseGroup g, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_PERMS_PREFIX + g.name());
        fill(inv);
        inv.setItem(45, named(Material.MAP, "§7Prev Page", List.of()));
        inv.setItem(49, named(Material.ARROW, "§7Back", List.of("§7Return.")));
        inv.setItem(53, named(Material.PAPER, "§7Next Page", List.of()));

        List<PermissionEntry> entries = allPermEntries();
        int perPage = 45;
        int start = page * perPage;
        if (start >= entries.size()) start = 0;
        int end = Math.min(entries.size(), start + perPage);
        int slot = 0;
        for (int i = start; i < end; i++) {
            PermissionEntry e = entries.get(i);
            inv.setItem(slot++, permItem(g, e));
        }
        player.openInventory(inv);
    }

    private static ItemStack permItem(HouseGroup g, PermissionEntry entry) {
        boolean enabled = entry.perm == null || g.has(entry.perm);
        Material mat;
        String name;
        List<String> lore = new ArrayList<>();

        if (entry.perm == null) {
            mat = Material.COMPASS;
            name = "§eDefault GameMode: §f" + g.defaultGameMode().name();
            lore.add("§7Click to cycle.");
        } else {
            mat = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
            name = (enabled ? "§a" : "§7") + entry.displayName;
            if (entry.perm == HousePermission.BUILD) {
                lore.add("§cWarning:");
                lore.add("§7Make sure you trust the person...");
            }
            lore.add(enabled ? "§aEnabled" : "§7Disabled");
        }
        return named(mat, name, lore);
    }

    private record PermissionEntry(HousePermission perm, String displayName) {}

    private static List<PermissionEntry> allPermEntries() {
        List<PermissionEntry> list = new ArrayList<>();
        list.add(new PermissionEntry(HousePermission.FLY, "Fly"));
        list.add(new PermissionEntry(HousePermission.WOOD_DOOR, "Wood Door"));
        list.add(new PermissionEntry(HousePermission.IRON_DOOR, "Iron Door"));
        list.add(new PermissionEntry(HousePermission.WOOD_TRAPDOOR, "Wood Trap Door"));
        list.add(new PermissionEntry(HousePermission.IRON_TRAPDOOR, "Iron Trap Door"));
        list.add(new PermissionEntry(HousePermission.FENCE_GATE, "Fence Gate"));
        list.add(new PermissionEntry(HousePermission.BUTTON, "Button"));
        list.add(new PermissionEntry(HousePermission.LEVER, "Lever"));
        list.add(new PermissionEntry(HousePermission.TP_SELF, "/tp"));
        list.add(new PermissionEntry(HousePermission.TP_OTHERS, "/tp other players"));
        list.add(new PermissionEntry(HousePermission.JUKEBOX, "Jukebox"));
        list.add(new PermissionEntry(HousePermission.KICK, "Kick"));
        list.add(new PermissionEntry(HousePermission.BAN, "Ban"));
        list.add(new PermissionEntry(HousePermission.MUTE, "Mute"));
        list.add(new PermissionEntry(HousePermission.BUILD, "Build"));
        list.add(new PermissionEntry(HousePermission.OFFLINE_BUILD, "Offline Build"));
        list.add(new PermissionEntry(HousePermission.FLUID, "Fluid"));
        list.add(new PermissionEntry(HousePermission.PRO_TOOLS, "Pro Tools"));
        list.add(new PermissionEntry(HousePermission.USE_CHESTS, "Use Chests"));
        list.add(new PermissionEntry(HousePermission.USE_ENDER_CHESTS, "Use Ender Chests"));
        list.add(new PermissionEntry(HousePermission.ITEM_EDITOR, "Item Editor (/edit)"));
        list.add(new PermissionEntry(null, "Default GameMode"));
        list.add(new PermissionEntry(HousePermission.SWITCH_GAMEMODE, "Switch GameMode"));
        list.add(new PermissionEntry(HousePermission.EDIT_VARIABLES, "Edit Variables"));
        list.add(new PermissionEntry(HousePermission.CHANGE_GROUP, "Change Group"));
        list.add(new PermissionEntry(HousePermission.CHANGE_SETTINGS, "Change Settings"));
        list.add(new PermissionEntry(HousePermission.TEAM_CHAT_SPY, "Team Chat Spy"));
        list.add(new PermissionEntry(HousePermission.EDIT_ACTIONS, "Edit Actions"));
        list.add(new PermissionEntry(HousePermission.EDIT_REGIONS, "Edit Regions"));
        list.add(new PermissionEntry(HousePermission.EDIT_SCOREBOARD, "Edit Scoreboard"));
        list.add(new PermissionEntry(HousePermission.EDIT_EVENT_ACTIONS, "Edit Event Actions"));
        list.add(new PermissionEntry(HousePermission.EDIT_COMMANDS, "Edit Commands"));
        list.add(new PermissionEntry(HousePermission.EDIT_FUNCTIONS, "Edit Functions"));
        list.add(new PermissionEntry(HousePermission.EDIT_INVENTORY_LAYOUTS, "Edit Inventory Layouts"));
        list.add(new PermissionEntry(HousePermission.EDIT_TEAMS, "Edit Teams"));
        list.add(new PermissionEntry(HousePermission.EDIT_CUSTOM_MENUS, "Edit Custom Menus"));
        list.add(new PermissionEntry(HousePermission.USE_NPCS, "Use NPCs"));
        return list;
    }

    private static PermissionEntry permAt(int rawSlot, int page) {
        if (rawSlot < 0 || rawSlot >= 45) return null;
        List<PermissionEntry> entries = allPermEntries();
        int idx = page * 45 + rawSlot;
        if (idx < 0 || idx >= entries.size()) return null;
        return entries.get(idx);
    }

    private UUID groupIdAtSlot(Player player, int slot) {
        if (slot < 0 || slot >= 45) return null;
        List<UUID> order = listViewOrder.get(player.getUniqueId());
        if (order == null) return null;
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

    private static List<UUID> groupsInDisplayOrder(HouseGroupsData data) {
        List<UUID> out = new ArrayList<>();
        for (HouseGroup g : data.groupsInEditorOrder()) out.add(g.id());
        return out;
    }

    private static int nextCustomPriority(HouseGroupsData data) {
        int maxCustom = 1;
        for (HouseGroup g : data.groups().values()) {
            if (g == null) continue;
            if (g.id().equals(data.visitorId())) continue;
            if (g.id().equals(data.coOwnerId())) continue;
            if (g.id().equals(data.ownerId())) continue;
            if (g.priority() >= 2 && g.priority() < 100) maxCustom = Math.max(maxCustom, g.priority());
        }
        return Math.min(99, Math.max(2, maxCustom + 1));
    }

    private static ItemStack groupListItem(HouseGroupsData data, HouseGroup g) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta rawMeta = item.getItemMeta();
        if (rawMeta instanceof SkullMeta meta) {
            meta.setOwningPlayer(headOwnerForGroup(data, g));
            meta.setDisplayName(priorityPrefix(data, g) + "§f" + g.name());
            meta.setLore(List.of(
                    "§7Tag: §f" + ChatColor.translateAlternateColorCodes('&', g.tag()),
                    "§7Priority: §f" + g.priority(),
                    "§7Click to edit"
            ));
            item.setItemMeta(meta);
            return item;
        }
        return named(Material.PLAYER_HEAD, "§f" + g.name(), List.of("§7Click to edit"));
    }

    private static OfflinePlayer headOwnerForGroup(HouseGroupsData data, HouseGroup g) {
        String skin = "MHF_Question";
        if (g.id().equals(data.visitorId())) skin = "MHF_Villager";
        else if (g.id().equals(data.coOwnerId())) skin = "MHF_Exclamation";
        else if (g.id().equals(data.ownerId())) skin = "MHF_Chest";
        return Bukkit.getOfflinePlayer(skin);
    }

    private static String priorityPrefix(HouseGroupsData data, HouseGroup g) {
        // Keep ASCII-only to avoid client/font/unicode artifacts.
        if (g.id().equals(data.visitorId())) return "§7[LOW] ";
        if (g.id().equals(data.coOwnerId())) return "§b[HIGH] ";
        if (g.id().equals(data.ownerId())) return "§6[TOP] ";
        if (g.priority() < 10) return "§7[1-9] ";
        if (g.priority() < 50) return "§a[10-49] ";
        return "§e[50+] ";
    }
}
