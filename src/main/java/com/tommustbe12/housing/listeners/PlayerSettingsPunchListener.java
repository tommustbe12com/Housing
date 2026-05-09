package com.tommustbe12.housing.listeners;

import com.tommustbe12.housing.gui.PlayerSettingsGui;
import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.util.HousingItems;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;

public final class PlayerSettingsPunchListener implements Listener {
    private final Plugin plugin;
    private final HouseManager houses;
    private final PlayerSettingsGui gui;

    public PlayerSettingsPunchListener(Plugin plugin, HouseManager houses, PlayerSettingsGui gui) {
        this.plugin = plugin;
        this.houses = houses;
        this.gui = gui;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPunch(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Player target)) return;
        Player viewer = event.getPlayer();
        if (!HousingItems.isMenuStar(plugin, viewer.getInventory().getItemInMainHand())) return;
        var info = houses.getHouseInfoByWorld(viewer.getWorld());
        if (info == null) return;
        event.setCancelled(true);
        gui.open(viewer, target, () -> viewer.closeInventory());
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player viewer)) return;
        if (!(event.getEntity() instanceof Player target)) return;
        if (!HousingItems.isMenuStar(plugin, viewer.getInventory().getItemInMainHand())) return;
        var info = houses.getHouseInfoByWorld(viewer.getWorld());
        if (info == null) return;
        event.setCancelled(true);
        gui.open(viewer, target, () -> viewer.closeInventory());
    }
}
