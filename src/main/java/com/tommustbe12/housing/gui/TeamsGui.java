package com.tommustbe12.housing.gui;

import com.tommustbe12.housing.chat.ChatPrompts;
import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.houses.HouseSlot;
import com.tommustbe12.housing.teams.HouseTeam;
import com.tommustbe12.housing.teams.TeamsService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class TeamsGui {
    private static final String TITLE_LIST = "Teams";
    private static final String TITLE_EDIT_PREFIX = "Edit Team: ";
    private static final String TITLE_SETTINGS = "Team Settings";
    private static final String TITLE_COLOR = "Pick Team Color";
    private static final String TITLE_DELETE_PREFIX = "Delete Team?";

    private final Plugin plugin;
    private final ChatPrompts prompts;
    private final HouseManager houses;
    private final TeamsService teams;

    private final Map<UUID, UUID> editingTeamId = new ConcurrentHashMap<>();
    private final Map<UUID, List<UUID>> listViewOrder = new ConcurrentHashMap<>();

    public TeamsGui(Plugin plugin, ChatPrompts prompts, HouseManager houses, TeamsService teams) {
        this.plugin = plugin;
        this.prompts = prompts;
        this.houses = houses;
        this.teams = teams;
    }

    public boolean isTitle(String title) {
        return TITLE_LIST.equals(title)
                || TITLE_SETTINGS.equals(title)
                || TITLE_COLOR.equals(title)
                || (title != null && title.startsWith(TITLE_EDIT_PREFIX))
                || (title != null && title.startsWith(TITLE_DELETE_PREFIX));
    }

    public void open(Player player, Runnable backToSystems) {
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null || !info.owner().equals(player.getUniqueId())) {
            player.sendMessage("§cTeams can only be edited in your own house.");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, TITLE_LIST);
        fill(inv);

        List<HouseTeam> list = teams.list(info.owner(), info.slot());
        List<UUID> order = new ArrayList<>();
        for (HouseTeam t : list) if (t != null) order.add(t.id());
        listViewOrder.put(player.getUniqueId(), order);

        int i = 0;
        for (HouseTeam t : list) {
            if (t == null) continue;
            inv.setItem(i++, teamListItem(t));
            if (i >= 45) break;
        }

        inv.setItem(48, named(Material.COMPARATOR, "§eOverall Team Settings", List.of("§7Coming soon.")));
        inv.setItem(49, named(Material.ARROW, "§7Back", List.of("§7Return to Systems.")));
        inv.setItem(50, named(Material.PAPER, "§aCreate Team", List.of("§7Click to create a new team.")));
        player.openInventory(inv);
    }

    public void handleClick(Player player, String title, int rawSlot, ItemStack clicked, org.bukkit.event.inventory.ClickType clickType, Runnable backToSystems) {
        if (clicked == null || clicked.getType().isAir()) return;
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null || !info.owner().equals(player.getUniqueId())) return;

        if (TITLE_LIST.equals(title)) {
            if (clicked.getType() == Material.ARROW) { backToSystems.run(); return; }
            if (clicked.getType() == Material.COMPARATOR) { openSettings(player, backToSystems); return; }
            if (clicked.getType() == Material.PAPER) {
                prompts.prompt(player, "Enter team name:", msg -> {
                    if (msg.equalsIgnoreCase("cancel")) return;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        String name = msg.trim();
                        if (name.isBlank()) return;
                        HouseTeam t = new HouseTeam(UUID.randomUUID(), name);
                        t.setTag(TeamsService.defaultTagForName(name));
                        teams.teams(info.owner(), info.slot()).teams().put(t.id(), t);
                        teams.save(info.owner(), info.slot());
                        openEdit(player, info.owner(), info.slot(), t, backToSystems);
                    });
                });
                return;
            }

            UUID id = teamIdAtSlot(player, rawSlot);
            if (id == null) return;
            HouseTeam t = teams.find(info.owner(), info.slot(), id);
            if (t == null) return;
            openEdit(player, info.owner(), info.slot(), t, backToSystems);
            return;
        }

        if (TITLE_SETTINGS.equals(title)) {
            if (clicked.getType() == Material.ARROW) { open(player, backToSystems); return; }
            if (clicked.getType() == Material.NAME_TAG) {
                var data = teams.teams(info.owner(), info.slot());
                data.setShowTagsEverywhere(!data.showTagsEverywhere());
                teams.save(info.owner(), info.slot());
                openSettings(player, backToSystems);
                return;
            }
            return;
        }

        if (title != null && title.startsWith(TITLE_EDIT_PREFIX)) {
            HouseTeam t = findEditing(player, info.owner(), info.slot());
            if (t == null) { open(player, backToSystems); return; }

            switch (clicked.getType()) {
                case ARROW -> open(player, backToSystems);
                case PAPER -> prompts.prompt(player, "Enter new team name:", msg -> {
                    if (msg.equalsIgnoreCase("cancel")) return;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        t.setName(msg.trim());
                        teams.save(info.owner(), info.slot());
                        openEdit(player, info.owner(), info.slot(), t, backToSystems);
                    });
                });
                case OAK_SIGN -> prompts.prompt(player, "Enter new team tag (ex: [TEAM]):", msg -> {
                    if (msg.equalsIgnoreCase("cancel")) return;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        t.setTag(msg.trim());
                        teams.save(info.owner(), info.slot());
                        openEdit(player, info.owner(), info.slot(), t, backToSystems);
                    });
                });
                case REDSTONE -> openColorPicker(player, info.owner(), info.slot(), t, backToSystems);
                case IRON_SWORD -> {
                    t.setFriendlyFire(!t.friendlyFire());
                    teams.save(info.owner(), info.slot());
                    openEdit(player, info.owner(), info.slot(), t, backToSystems);
                }
                case TNT -> openDeleteConfirm(player, info.owner(), info.slot(), t, backToSystems);
            }
            return;
        }

        if (TITLE_COLOR.equals(title)) {
            HouseTeam t = findEditing(player, info.owner(), info.slot());
            if (t == null) { open(player, backToSystems); return; }
            if (clicked.getType() == Material.ARROW) {
                openEdit(player, info.owner(), info.slot(), t, backToSystems);
                return;
            }
            ChatColor picked = colorFor(clicked.getType());
            if (picked == null) return;
            t.setColor(picked);
            teams.save(info.owner(), info.slot());
            openEdit(player, info.owner(), info.slot(), t, backToSystems);
            return;
        }

        if (title != null && title.startsWith(TITLE_DELETE_PREFIX)) {
            HouseTeam t = findEditing(player, info.owner(), info.slot());
            if (t == null) { open(player, backToSystems); return; }
            if (clicked.getType() == Material.ARROW) {
                openEdit(player, info.owner(), info.slot(), t, backToSystems);
                return;
            }
            if (clicked.getType() == Material.LIME_CONCRETE) {
                teams.teams(info.owner(), info.slot()).teams().remove(t.id());
                teams.teams(info.owner(), info.slot()).playerTeams().values().removeIf(id -> id != null && id.equals(t.id()));
                teams.save(info.owner(), info.slot());
                editingTeamId.remove(player.getUniqueId());
                player.sendMessage("§aTeam deleted.");
                open(player, backToSystems);
            }
            return;
        }
    }

    private void openEdit(Player player, UUID owner, HouseSlot slot, HouseTeam t, Runnable backToSystems) {
        editingTeamId.put(player.getUniqueId(), t.id());
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_EDIT_PREFIX + t.name());
        fill(inv);

        inv.setItem(11, named(Material.PAPER, "§eRename", List.of("§7Renames the team only.")));
        inv.setItem(13, named(Material.OAK_SIGN, "§bChange Tag", List.of("§7Current: §f" + coloredTagPreview(t), "§7Click to edit.")));
        inv.setItem(15, named(Material.REDSTONE, "§dChange Color", List.of("§7Current: §f" + t.color().name(), "§7Click to pick.")));

        inv.setItem(21, named(Material.IRON_SWORD, "§cFriendly Fire: " + (t.friendlyFire() ? "§aEnabled" : "§7Disabled"),
                List.of("§7If disabled, teammates cannot hit each other.", "§7Click to toggle.")));

        inv.setItem(49, named(Material.ARROW, "§7Back", List.of("§7Return to Teams.")));
        inv.setItem(50, named(Material.TNT, "§cDelete Team", List.of("§7Click to delete.")));
        player.openInventory(inv);
    }

    private void openSettings(Player player, Runnable backToSystems) {
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null) return;
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_SETTINGS);
        fill(inv);
        boolean show = teams.teams(info.owner(), info.slot()).showTagsEverywhere();
        inv.setItem(13, named(Material.NAME_TAG, "§bShow Team Tags: " + (show ? "§aON" : "§7OFF"),
                List.of("§7Affects: chat, tab list, nametags.", "§7Click to toggle.")));
        inv.setItem(22, named(Material.ARROW, "§7Back", List.of("§7Return.")));
        player.openInventory(inv);
    }

    private void openColorPicker(Player player, UUID owner, HouseSlot slot, HouseTeam t, Runnable backToSystems) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_COLOR);
        fill(inv);
        // 16-ish common colors, simple picker.
        inv.setItem(10, colorItem(Material.WHITE_WOOL, ChatColor.WHITE));
        inv.setItem(11, colorItem(Material.LIGHT_GRAY_WOOL, ChatColor.GRAY));
        inv.setItem(12, colorItem(Material.GRAY_WOOL, ChatColor.DARK_GRAY));
        inv.setItem(13, colorItem(Material.BLACK_WOOL, ChatColor.BLACK));
        inv.setItem(14, colorItem(Material.RED_WOOL, ChatColor.RED));
        inv.setItem(15, colorItem(Material.ORANGE_WOOL, ChatColor.GOLD));
        inv.setItem(16, colorItem(Material.YELLOW_WOOL, ChatColor.YELLOW));
        inv.setItem(19, colorItem(Material.LIME_WOOL, ChatColor.GREEN));
        inv.setItem(20, colorItem(Material.GREEN_WOOL, ChatColor.DARK_GREEN));
        inv.setItem(21, colorItem(Material.LIGHT_BLUE_WOOL, ChatColor.AQUA));
        inv.setItem(22, colorItem(Material.CYAN_WOOL, ChatColor.DARK_AQUA));
        inv.setItem(23, colorItem(Material.BLUE_WOOL, ChatColor.BLUE));
        inv.setItem(24, colorItem(Material.PURPLE_WOOL, ChatColor.DARK_PURPLE));
        inv.setItem(25, colorItem(Material.MAGENTA_WOOL, ChatColor.LIGHT_PURPLE));
        inv.setItem(26, colorItem(Material.PINK_WOOL, ChatColor.LIGHT_PURPLE));
        inv.setItem(49, named(Material.ARROW, "§7Back", List.of("§7Return.")));
        player.openInventory(inv);
    }

    private void openDeleteConfirm(Player player, UUID owner, HouseSlot slot, HouseTeam t, Runnable backToSystems) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_DELETE_PREFIX + " " + t.name());
        fill(inv);
        inv.setItem(11, named(Material.LIME_CONCRETE, "§aConfirm Delete", List.of("§cThis cannot be undone.")));
        inv.setItem(15, named(Material.RED_CONCRETE, "§cCancel", List.of("§7Go back.")));
        inv.setItem(22, named(Material.ARROW, "§7Back", List.of("§7Return.")));
        player.openInventory(inv);
    }

    private HouseTeam findEditing(Player player, UUID owner, HouseSlot slot) {
        UUID id = editingTeamId.get(player.getUniqueId());
        if (id == null) return null;
        return teams.find(owner, slot, id);
    }

    private UUID teamIdAtSlot(Player player, int rawSlot) {
        if (rawSlot < 0 || rawSlot >= 45) return null;
        List<UUID> order = listViewOrder.get(player.getUniqueId());
        if (order == null) return null;
        if (rawSlot >= order.size()) return null;
        return order.get(rawSlot);
    }

    private static ItemStack teamListItem(HouseTeam t) {
        String tag = t.tag() == null ? "" : t.tag();
        String color = t.color() == null ? ChatColor.WHITE.name() : t.color().name();
        return named(Material.NAME_TAG, "§f" + t.name(), List.of(
                "§7Tag: §f" + (tag.isBlank() ? "(none)" : (t.color() + tag + ChatColor.RESET)),
                "§7Color: §f" + color,
                "§7Friendly Fire: §f" + (t.friendlyFire() ? "Enabled" : "Disabled"),
                "§7Click to edit"
        ));
    }

    private static String coloredTagPreview(HouseTeam t) {
        String tag = t.tag() == null ? "" : t.tag();
        if (tag.isBlank()) return "(none)";
        return (t.color() == null ? ChatColor.WHITE : t.color()) + tag + ChatColor.RESET;
    }

    private static ItemStack colorItem(Material wool, ChatColor color) {
        return named(wool, color + color.name(), List.of("§7Click to select"));
    }

    private static ChatColor colorFor(Material mat) {
        return switch (mat) {
            case WHITE_WOOL -> ChatColor.WHITE;
            case LIGHT_GRAY_WOOL, GRAY_WOOL -> ChatColor.GRAY;
            case BLACK_WOOL -> ChatColor.BLACK;
            case RED_WOOL -> ChatColor.RED;
            case ORANGE_WOOL -> ChatColor.GOLD;
            case YELLOW_WOOL -> ChatColor.YELLOW;
            case LIME_WOOL -> ChatColor.GREEN;
            case GREEN_WOOL -> ChatColor.DARK_GREEN;
            case LIGHT_BLUE_WOOL -> ChatColor.AQUA;
            case CYAN_WOOL -> ChatColor.DARK_AQUA;
            case BLUE_WOOL -> ChatColor.BLUE;
            case PURPLE_WOOL -> ChatColor.DARK_PURPLE;
            case MAGENTA_WOOL, PINK_WOOL -> ChatColor.LIGHT_PURPLE;
            default -> null;
        };
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
