package com.tommustbe12.housing.listeners;

import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.npcs.NpcManager;
import com.tommustbe12.housing.util.HousingItems;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public final class NpcPlacerItemListener implements Listener {
    private final Plugin plugin;
    private final HouseManager houses;
    private final NpcManager npcs;

    public NpcPlacerItemListener(Plugin plugin, HouseManager houses, NpcManager npcs) {
        this.plugin = plugin;
        this.houses = houses;
        this.npcs = npcs;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getAction().isRightClick()) return;
        if (event.getClickedBlock() == null) return;

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!HousingItems.isNpcPlacerItem(plugin, hand)) return;

        event.setCancelled(true);
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null) return;
        if (npcs == null || !npcs.isCitizensAvailable()) {
            player.sendMessage("§cNPCs are unavailable (Citizens not installed).");
            return;
        }

        Location base = event.getClickedBlock().getLocation().add(0.5, 1.0, 0.5);
        base.setYaw(player.getLocation().getYaw());
        base.setPitch(0f);
        npcs.createNpc(info.owner(), info.slot(), player.getWorld(), base);
        player.sendMessage("§aNPC placed.");
    }
}

