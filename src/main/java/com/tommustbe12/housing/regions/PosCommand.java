package com.tommustbe12.housing.regions;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class PosCommand implements CommandExecutor {
    private final RegionSelectionService selections;
    private final RegionSelectionVisualizer visualizer;
    private final boolean pos1;

    public PosCommand(RegionSelectionService selections, RegionSelectionVisualizer visualizer, boolean pos1) {
        this.selections = selections;
        this.visualizer = visualizer;
        this.pos1 = pos1;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (pos1) {
            selections.setPos1(player, player.getLocation());
            var loc = selections.pos1(player);
            player.sendMessage("§aSet region pos1 to §f" + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ());
        } else {
            selections.setPos2(player, player.getLocation());
            var loc = selections.pos2(player);
            player.sendMessage("§aSet region pos2 to §f" + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ());
        }
        if (visualizer != null) visualizer.refresh(player);
        return true;
    }
}
