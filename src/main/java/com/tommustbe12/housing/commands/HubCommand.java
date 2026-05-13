package com.tommustbe12.housing.commands;

import com.tommustbe12.housing.houses.HouseManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class HubCommand implements CommandExecutor {
    private final HouseManager houses;

    public HubCommand(HouseManager houses) {
        this.houses = houses;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        houses.sendToHub(player);
        return true;
    }
}

