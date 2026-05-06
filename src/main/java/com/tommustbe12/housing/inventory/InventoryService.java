package com.tommustbe12.housing.inventory;

import com.tommustbe12.housing.debug.Debug;
import com.tommustbe12.housing.houses.HouseSlot;
import com.tommustbe12.housing.listeners.HouseItemListener;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.UUID;

public final class InventoryService {
    private final Debug debug;
    private final HouseItemListener menuItemGiver;
    private com.tommustbe12.housing.cookies.CookieItemListener cookieItemListener;

    public InventoryService(Plugin plugin, Debug debug, HouseItemListener menuItemGiver) {
        this.debug = debug;
        this.menuItemGiver = menuItemGiver;

        // User requested: no saved inventories anywhere. Delete legacy inventory storage folder.
        File dir = new File(plugin.getDataFolder(), "inventories");
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) for (File f : files) {
                //noinspection ResultOfMethodCallIgnored
                f.delete();
            }
            //noinspection ResultOfMethodCallIgnored
            dir.delete();
        }
    }

    public void setCookieItemListener(com.tommustbe12.housing.cookies.CookieItemListener cookieItemListener) {
        this.cookieItemListener = cookieItemListener;
    }

    public void applyHouseInventoryOrDefault(Player player, UUID houseOwner, HouseSlot slot) {
        // No persistence (until inventory layouts system is implemented): always apply default loadout.
        PlayerInventory inv = player.getInventory();
        inv.clear();
        inv.setArmorContents(null);
        inv.setItemInOffHand(null);
        menuItemGiver.giveMenuItem(player);
        giveCookieIfEmpty(player, inv);
    }

    private void giveCookieIfEmpty(Player player, PlayerInventory inv) {
        if (cookieItemListener == null) return;
        int slot = 4;
        ItemStack existing = inv.getItem(slot);
        if (existing == null || existing.getType().isAir()) {
            inv.setItem(slot, cookieItemListener.createCookieItem(player));
        }
    }

}
