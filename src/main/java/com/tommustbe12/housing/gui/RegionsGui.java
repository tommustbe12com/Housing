package com.tommustbe12.housing.gui;

import com.tommustbe12.housing.chat.ChatPrompts;
import com.tommustbe12.housing.groups.HouseGroupsService;
import com.tommustbe12.housing.groups.HousePermission;
import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.regions.RegionData;
import com.tommustbe12.housing.regions.RegionSelectionService;
import com.tommustbe12.housing.regions.RegionsService;
import com.tommustbe12.housing.util.HousingItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RegionsGui {
    private static final String TITLE_REGIONS = "Regions";
    private static final String TITLE_REGION_PREFIX = "Region: ";
    private static final String TITLE_PVP_PREFIX = "PvP Settings: ";

    private final Plugin plugin;
    private final HouseManager houses;
    private final HouseGroupsService groups;
    private final RegionsService regions;
    private final RegionSelectionService selections;
    private final ChatPrompts prompts;
    private final ActionsEditor actionsEditor;

    public RegionsGui(Plugin plugin, HouseManager houses, HouseGroupsService groups, RegionsService regions, RegionSelectionService selections, ChatPrompts prompts, ActionsEditor actionsEditor) {
        this.plugin = plugin;
        this.houses = houses;
        this.groups = groups;
        this.regions = regions;
        this.selections = selections;
        this.prompts = prompts;
        this.actionsEditor = actionsEditor;
    }

    public boolean isTitle(String title) {
        return TITLE_REGIONS.equals(title)
                || (title != null && (title.startsWith(TITLE_REGION_PREFIX) || title.startsWith(TITLE_PVP_PREFIX)));
    }

    public void openList(Player player, Runnable backToSystems) {
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null) return;
        if (!canEdit(player, info.owner(), info.slot())) return;

        Inventory inv = Bukkit.createInventory(null, 54, TITLE_REGIONS);
        fill(inv);

        inv.setItem(45, named(Material.BLAZE_ROD, "§eGet Region Wand", List.of("§7Click to receive the wand.")));
        inv.setItem(49, named(Material.ARROW, "§7Back", List.of("§7Return to Systems.")));
        inv.setItem(53, named(Material.ANVIL, "§aCreate Region", List.of("§7Uses your pos1/pos2 selection.")));

        int slot = 0;
        for (RegionData r : regions.regions(info.owner(), info.slot()).values()) {
            if (slot >= 45) break;
            inv.setItem(slot++, named(Material.MAP, "§b" + r.name(), List.of("§7Click to edit.")));
        }
        player.openInventory(inv);
    }

    public void handleClick(Player player, String title, int rawSlot, ItemStack clicked, ClickType clickType, Runnable backToSystems) {
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null) return;
        if (!canEdit(player, info.owner(), info.slot())) return;
        if (clicked == null) return;

        if (TITLE_REGIONS.equals(title)) {
            if (clicked.getType() == Material.ARROW) { backToSystems.run(); return; }
            if (clicked.getType() == Material.BLAZE_ROD) {
                player.getInventory().addItem(HousingItems.createRegionWand(plugin));
                player.sendMessage("§aGiven §eRegion Wand§a.");
                return;
            }
            if (clicked.getType() == Material.ANVIL) {
                if (!selections.isComplete(player)) {
                    player.sendMessage("§cSet pos1 and pos2 first (use the wand or /pos1 and /pos2).");
                    return;
                }
                prompts.prompt(player, "Region name?", answer -> Bukkit.getScheduler().runTask(plugin, () -> {
                    String name = sanitizeName(answer);
                    if (name == null) {
                        player.sendMessage("§cCancelled.");
                        openList(player, backToSystems);
                        return;
                    }
                    if (name.isBlank()) {
                        player.sendMessage("§cName can't be blank.");
                        openList(player, backToSystems);
                        return;
                    }
                    if (regions.get(info.owner(), info.slot(), name) != null) {
                        player.sendMessage("§cA region with that name already exists.");
                        openList(player, backToSystems);
                        return;
                    }
                    var sel = selections.get(player);
                    RegionData r = new RegionData(name, sel.pos1(), sel.pos2());
                    regions.add(info.owner(), info.slot(), r);
                    openRegion(player, r.name(), backToSystems);
                }));
                return;
            }
            if (clicked.getType() == Material.MAP) {
                String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName()).trim();
                openRegion(player, name, backToSystems);
                return;
            }
            return;
        }

        if (title.startsWith(TITLE_REGION_PREFIX)) {
            String key = title.substring(TITLE_REGION_PREFIX.length()).trim();
            RegionData r = regions.get(info.owner(), info.slot(), key);
            if (r == null) { openList(player, backToSystems); return; }

            switch (clicked.getType()) {
                case ARROW -> openList(player, backToSystems);
                case NAME_TAG -> prompts.prompt(player, "New region name? (no & codes)", answer -> Bukkit.getScheduler().runTask(plugin, () -> {
                    String name = sanitizeName(answer);
                    if (name == null) { openRegion(player, r.name(), backToSystems); return; }
                    if (name.contains("&")) {
                        player.sendMessage("§cNo & codes.");
                        openRegion(player, r.name(), backToSystems);
                        return;
                    }
                    if (!regions.rename(info.owner(), info.slot(), r.name(), name)) {
                        player.sendMessage("§cThat name is already taken.");
                    }
                    openRegion(player, name, backToSystems);
                }));
                case ENDER_PEARL -> {
                    var loc = regions.randomPointInside(r);
                    if (loc != null) player.teleport(loc);
                }
                case PISTON -> {
                    if (!selections.isComplete(player)) {
                        player.sendMessage("§cSet pos1 and pos2 first (use the wand or /pos1 and /pos2).");
                        return;
                    }
                    var sel = selections.get(player);
                    regions.moveToSelection(info.owner(), info.slot(), r.name(), sel.pos1(), sel.pos2());
                    player.sendMessage("§aMoved region to your current selection.");
                    openRegion(player, r.name(), backToSystems);
                }
                case IRON_SWORD -> openPvp(player, r.name(), backToSystems);
                case PAPER -> {
                    if (rawSlot == 31) {
                        actionsEditor.openStandaloneHouse(player, info.owner(), info.slot(),
                                "region_entry:" + r.name().toLowerCase(Locale.ROOT),
                                r.entryActions(),
                                list -> regions.setEntryActions(info.owner(), info.slot(), r.name(), list),
                                () -> openRegion(player, r.name(), backToSystems));
                    } else if (rawSlot == 33) {
                        actionsEditor.openStandaloneHouse(player, info.owner(), info.slot(),
                                "region_exit:" + r.name().toLowerCase(Locale.ROOT),
                                r.exitActions(),
                                list -> regions.setExitActions(info.owner(), info.slot(), r.name(), list),
                                () -> openRegion(player, r.name(), backToSystems));
                    }
                }
                case TNT -> {
                    regions.delete(info.owner(), info.slot(), r.name());
                    player.sendMessage("§cRegion deleted.");
                    openList(player, backToSystems);
                }
            }
            return;
        }

        if (title.startsWith(TITLE_PVP_PREFIX)) {
            String key = title.substring(TITLE_PVP_PREFIX.length()).trim();
            RegionData r = regions.get(info.owner(), info.slot(), key);
            if (r == null) { openList(player, backToSystems); return; }
            if (clicked.getType() == Material.ARROW) { openRegion(player, r.name(), backToSystems); return; }

            var st = r.settings();
            if (rawSlot == 10) st.pvpDamage = !st.pvpDamage;
            if (rawSlot == 11) st.doubleJump = !st.doubleJump;
            if (rawSlot == 12) st.fireDamage = !st.fireDamage;
            if (rawSlot == 13) st.fallDamage = !st.fallDamage;
            if (rawSlot == 14) st.poisonWitherRoseDamage = !st.poisonWitherRoseDamage;
            if (rawSlot == 15) st.suffocation = !st.suffocation;
            if (rawSlot == 16) st.hunger = !st.hunger;
            if (rawSlot == 19) st.naturalRegen = !st.naturalRegen;
            if (rawSlot == 20) st.killDeathMessages = !st.killDeathMessages;
            if (rawSlot == 21) st.instantRespawn = !st.instantRespawn;
            if (rawSlot == 22) st.keepInventory = !st.keepInventory;
            regions.save(info.owner(), info.slot());
            openPvp(player, r.name(), backToSystems);
        }
    }

    private void openRegion(Player player, String nameKey, Runnable backToSystems) {
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null) return;
        RegionData r = regions.get(info.owner(), info.slot(), nameKey);
        if (r == null) { openList(player, backToSystems); return; }

        Inventory inv = Bukkit.createInventory(null, 54, TITLE_REGION_PREFIX + r.name());
        fill(inv);
        // Row 2 (index 1): rename / teleport / move
        inv.setItem(11, named(Material.NAME_TAG, "§eRename Region", List.of("§7Click to rename in chat.")));
        inv.setItem(13, named(Material.ENDER_PEARL, "§aTeleport to Region", List.of("§7Teleport somewhere inside.")));
        inv.setItem(15, named(Material.PISTON, "§bMove Region", List.of("§7Move to current selection.")));

        // Row 4 (index 3): pvp / entry / exit
        inv.setItem(29, named(Material.IRON_SWORD, "§cPvP Settings", List.of("§7Toggle damage rules.")));
        inv.setItem(31, named(Material.PAPER, "§fEntry Actions", List.of("§7Actions when entering.")));
        inv.setItem(33, named(Material.PAPER, "§fExit Actions", List.of("§7Actions when exiting.")));

        // Bottom row: delete + back
        inv.setItem(49, named(Material.TNT, "§cDelete Region", List.of("§7This cannot be undone.")));
        inv.setItem(53, named(Material.ARROW, "§7Back", List.of("§7Return to regions.")));
        player.openInventory(inv);
    }

    private void openPvp(Player player, String nameKey, Runnable backToSystems) {
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null) return;
        RegionData r = regions.get(info.owner(), info.slot(), nameKey);
        if (r == null) { openList(player, backToSystems); return; }
        var st = r.settings();

        Inventory inv = Bukkit.createInventory(null, 27, TITLE_PVP_PREFIX + r.name());
        fill(inv);

        inv.setItem(10, toggle(Material.DIAMOND_SWORD, "PvP/Damage", st.pvpDamage));
        inv.setItem(11, toggle(Material.FEATHER, "Double Jump", st.doubleJump));
        inv.setItem(12, toggle(Material.FLINT_AND_STEEL, "Fire Damage", st.fireDamage));
        inv.setItem(13, toggle(Material.FEATHER, "Fall Damage", st.fallDamage));
        inv.setItem(14, toggle(Material.FERMENTED_SPIDER_EYE, "Poison/Wither Rose", st.poisonWitherRoseDamage));
        inv.setItem(15, toggle(Material.SAND, "Suffocation", st.suffocation));
        inv.setItem(16, toggle(Material.COOKED_BEEF, "Hunger", st.hunger));

        inv.setItem(19, toggle(Material.GOLDEN_APPLE, "Natural Regeneration", st.naturalRegen));
        inv.setItem(20, toggle(Material.WRITABLE_BOOK, "Kill/Death Messages", st.killDeathMessages));
        inv.setItem(21, toggle(Material.TOTEM_OF_UNDYING, "Instant Respawn", st.instantRespawn));
        inv.setItem(22, toggle(Material.CHEST, "Keep Inventory", st.keepInventory));

        inv.setItem(26, named(Material.ARROW, "§7Back", List.of("§7Return to region.")));
        player.openInventory(inv);
    }

    private boolean canEdit(Player player, java.util.UUID owner, com.tommustbe12.housing.houses.HouseSlot slot) {
        if (player.getUniqueId().equals(owner)) return true;
        return groups != null && groups.has(owner, slot, player.getUniqueId(), HousePermission.EDIT_REGIONS);
    }

    private static ItemStack toggle(Material mat, String name, boolean on) {
        String state = on ? "§aON" : "§cOFF";
        return named(mat, "§e" + name + ": " + state, List.of("§7Click to toggle."));
    }

    private static String sanitizeName(String answer) {
        if (answer == null) return null;
        if ("cancel".equalsIgnoreCase(answer.trim())) return null;
        String stripped = ChatColor.stripColor(answer);
        stripped = stripped == null ? "" : stripped.trim();
        return stripped;
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
