package com.tommustbe12.housing.cookies;

import com.tommustbe12.housing.houses.HouseManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class CookieItemListener implements Listener {
    private final HouseManager houses;
    private final CookieService cookies;
    private final NamespacedKey cookieKey;

    public CookieItemListener(Plugin plugin, HouseManager houses, CookieService cookies) {
        this.houses = houses;
        this.cookies = cookies;
        this.cookieKey = new NamespacedKey(plugin, "housing_cookie");
    }

    public ItemStack createCookieItem(Player player) {
        int remaining = cookies.remainingThisWeek(player.getUniqueId());
        ItemStack item = new ItemStack(Material.COOKIE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6Give Cookie");
        meta.setLore(java.util.List.of(
                "§7Right-click to give this house a cookie.",
                "§7Remaining this week: §f" + remaining + "§7/10",
                "§7Limit: §f1 per house/week"
        ));
        meta.getPersistentDataContainer().set(cookieKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isCookieItem(ItemStack item) {
        if (item == null || item.getType() != Material.COOKIE || !item.hasItemMeta()) return false;
        Byte b = item.getItemMeta().getPersistentDataContainer().get(cookieKey, PersistentDataType.BYTE);
        return b != null && b == (byte) 1;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getAction().isRightClick()) return;
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isCookieItem(item)) return;

        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null) return;

        event.setCancelled(true);
        CookieService.CookieResult res = cookies.giveCookie(player.getUniqueId(), info.owner(), info.slot());
        switch (res) {
            case OK -> player.sendMessage("§aYou gave this house a cookie!");
            case ALREADY_GAVE_HOUSE -> player.sendMessage("§cYou've already given this house a cookie this week!");
            case WEEKLY_LIMIT -> player.sendMessage("§cYou've used all 10 cookies for this week!");
        }

        // update remaining count on item
        player.getInventory().setItem(4, createCookieItem(player));
    }
}
