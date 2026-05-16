package com.tommustbe12.housing.listeners;

import com.tommustbe12.housing.gui.HologramsGui;
import com.tommustbe12.housing.holograms.HologramData;
import com.tommustbe12.housing.holograms.HologramsRuntime;
import com.tommustbe12.housing.holograms.HologramsService;
import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.util.HousingItems;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public final class HologramListener implements Listener {
    private final Plugin plugin;
    private final HouseManager houses;
    private final HologramsService holograms;
    private final HologramsRuntime runtime;
    private final HologramsGui gui;

    public HologramListener(Plugin plugin, HouseManager houses, HologramsService holograms, HologramsRuntime runtime, HologramsGui gui) {
        this.plugin = plugin;
        this.houses = houses;
        this.holograms = holograms;
        this.runtime = runtime;
        this.gui = gui;
    }

    @EventHandler
    public void onPlace(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getAction().isRightClick()) return;
        if (event.getClickedBlock() == null) return;

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!HousingItems.isHologramPlacerItem(plugin, hand)) return;

        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null) return;

        event.setCancelled(true);
        Location base = event.getClickedBlock().getLocation().add(0.5, 1.2, 0.5);
        base.setYaw(player.getLocation().getYaw());
        base.setPitch(0f);
        base = runtime.ensureAboveGround(base);

        HologramData h = new HologramData(UUID.randomUUID());
        h.setLocation(base);
        h.lines().add("§fNew hologram");
        holograms.get(info.owner(), info.slot(), player.getWorld()).add(h);
        holograms.save(info.owner(), info.slot(), player.getWorld());
        runtime.spawnOrUpdate(info.owner(), info.slot(), player.getWorld(), h);
        player.sendMessage("§aHologram placed. Shift-right-click it to edit.");
    }

    @EventHandler
    public void onClickEntity(PlayerInteractAtEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null) return;

        Entity e = event.getRightClicked();
        if (e == null) return;
        var pdc = e.getPersistentDataContainer();
        String idStr = pdc.get(runtime.holoIdKey(), PersistentDataType.STRING);
        if (idStr == null) return;

        event.setCancelled(true);
        if (!player.isSneaking()) return;
        try {
            UUID id = UUID.fromString(idStr);
            gui.open(player, id);
        } catch (Exception ignored) {}
    }
}

