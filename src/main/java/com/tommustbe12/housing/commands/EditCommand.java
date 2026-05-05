package com.tommustbe12.housing.commands;

import com.tommustbe12.housing.gui.ItemEditGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class EditCommand implements CommandExecutor {
    private final ItemEditGui gui;

    public EditCommand(ItemEditGui gui) {
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this.");
            return true;
        }
        gui.open(player);
        return true;
    }
}

