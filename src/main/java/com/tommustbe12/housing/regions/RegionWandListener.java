package com.tommustbe12.housing.regions;

import com.tommustbe12.housing.util.HousingItems;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;

public final class RegionWandListener implements Listener {
    private final Plugin plugin;
    private final RegionSelectionService selections;
    private final RegionSelectionVisualizer visualizer;

    public RegionWandListener(Plugin plugin, RegionSelectionService selections, RegionSelectionVisualizer visualizer) {
        this.plugin = plugin;
        this.selections = selections;
        this.visualizer = visualizer;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        if (!HousingItems.isRegionWand(plugin, player.getInventory().getItemInMainHand())) return;

        Action a = event.getAction();
        if (a != Action.LEFT_CLICK_BLOCK && a != Action.RIGHT_CLICK_BLOCK) return;
        Block b = event.getClickedBlock();
        if (b == null) return;
        event.setCancelled(true);

        Location loc = b.getLocation();
        if (a == Action.LEFT_CLICK_BLOCK) {
            selections.setPos1(player, loc);
            player.sendMessage("§aSet region pos1 to §f" + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ());
            if (visualizer != null) visualizer.refresh(player);
        } else {
            selections.setPos2(player, loc);
            player.sendMessage("§aSet region pos2 to §f" + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ());
            if (visualizer != null) visualizer.refresh(player);
        }
    }
}
