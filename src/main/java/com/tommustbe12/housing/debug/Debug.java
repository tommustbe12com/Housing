package com.tommustbe12.housing.debug;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class Debug {
    private final Plugin plugin;
    private final Logger logger;

    public Debug(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("debug.enabled", true);
    }

    public void info(String message) {
        if (!isEnabled()) return;
        logger.info("[DEBUG] " + message);
    }

    public void warn(String message) {
        if (!isEnabled()) return;
        logger.warning("[DEBUG] " + message);
    }

    public void error(String message, Throwable throwable) {
        if (!isEnabled()) return;
        logger.log(Level.SEVERE, "[DEBUG] " + message, throwable);
    }

    public void toOps(String message) {
        if (!isEnabled()) return;
        String formatted = "§7[§bHousing§7] §8[§eDBG§8] §f" + message;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isOp()) player.sendMessage(formatted);
        }
        logger.info("[OPS-DEBUG] " + message);
    }

    public void to(CommandSender sender, String message) {
        if (!isEnabled()) return;
        if (sender instanceof Player player && !player.isOp()) return;
        sender.sendMessage("§7[§bHousing§7] §8[§eDBG§8] §f" + message);
    }
}

