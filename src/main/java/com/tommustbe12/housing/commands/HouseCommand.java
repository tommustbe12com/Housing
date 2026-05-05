package com.tommustbe12.housing.commands;

import com.tommustbe12.housing.debug.Debug;
import com.tommustbe12.housing.houses.HouseData;
import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.houses.HouseSlot;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class HouseCommand implements TabExecutor, TabCompleter {
    private final Plugin plugin;
    private final Debug debug;
    private final HouseManager houses;

    public HouseCommand(Plugin plugin, Debug debug, HouseManager houses) {
        this.plugin = plugin;
        this.debug = debug;
        this.houses = houses;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§bHousing§7: §fUse the §bHousing §7nether star menu.");
            sender.sendMessage("§bHousing§7: §f/house setname <slot> <name...>");
            sender.sendMessage("§bHousing§7: §f/house setspawn <slot>");
            sender.sendMessage("§bHousing§7: §f/house cookie give <player> <slot> [amount]");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "join" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Only players can use this.");
                    return true;
                }
                if (args.length < 3) {
                    player.sendMessage("§cUsage: /house join <player> <slot>");
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                UUID owner = target.getUniqueId();
                if (owner == null) {
                    player.sendMessage("§cUnknown player.");
                    return true;
                }
                HouseSlot slot = parseSlot(args[2]);
                if (slot == null) {
                    player.sendMessage("§cSlot must be 1-3.");
                    return true;
                }
                houses.createIfMissing(owner, slot);
                return houses.joinHouse(player, owner, slot);
            }
            case "setname" -> {
                if (!(sender instanceof Player player)) return true;
                if (args.length < 3) {
                    player.sendMessage("§cUsage: /house setname <slot> <name...>");
                    return true;
                }
                HouseSlot slot = parseSlot(args[1]);
                if (slot == null) {
                    player.sendMessage("§cSlot must be 1-3.");
                    return true;
                }
                String name = String.join(" ", slice(args, 2));
                HouseData data = houses.getHouse(player.getUniqueId(), slot);
                data.setName(name);
                houses.saveHouse(data);
                player.sendMessage("§aHouse name set to §f" + ChatColor.translateAlternateColorCodes('&', name));
                return true;
            }
            case "setspawn" -> {
                if (!(sender instanceof Player player)) return true;
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /house setspawn <slot>");
                    return true;
                }
                HouseSlot slot = parseSlot(args[1]);
                if (slot == null) {
                    player.sendMessage("§cSlot must be 1-3.");
                    return true;
                }
                HouseData data = houses.getHouse(player.getUniqueId(), slot);
                data.setSpawn(player.getLocation());
                houses.saveHouse(data);
                player.sendMessage("§aSpawn set.");
                return true;
            }
            case "cookie" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /house cookie give <player> <slot> [amount]");
                    return true;
                }
                if (!args[1].equalsIgnoreCase("give")) {
                    sender.sendMessage("§cUsage: /house cookie give <player> <slot> [amount]");
                    return true;
                }
                if (args.length < 4) {
                    sender.sendMessage("§cUsage: /house cookie give <player> <slot> [amount]");
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
                UUID owner = target.getUniqueId();
                if (owner == null) {
                    sender.sendMessage("§cUnknown player.");
                    return true;
                }
                HouseSlot slot = parseSlot(args[3]);
                if (slot == null) {
                    sender.sendMessage("§cSlot must be 1-3.");
                    return true;
                }
                int amount = 1;
                if (args.length >= 5) {
                    try {
                        amount = Math.max(1, Integer.parseInt(args[4]));
                    } catch (NumberFormatException ignored) {
                    }
                }
                houses.addCookie(owner, slot, amount);
                sender.sendMessage("§aGave §f" + amount + "§a cookie(s) to §f" + target.getName() + "§a's house §f" + slot.index() + "§a.");
                debug.to(sender, "Cookie add: owner=" + owner + " slot=" + slot.index() + " amount=" + amount);
                return true;
            }
            case "hot" -> {
                sender.sendMessage("§cHot Houses is GUI-only in V1. Use the nether star menu.");
                return true;
            }
            case "debug" -> {
                if (!sender.isOp()) {
                    sender.sendMessage("§cNo permission.");
                    return true;
                }
                boolean enabled = plugin.getConfig().getBoolean("debug.enabled", true);
                plugin.getConfig().set("debug.enabled", !enabled);
                plugin.saveConfig();
                sender.sendMessage("§aDebug is now §f" + (!enabled) + "§a.");
                return true;
            }
        }

        sender.sendMessage("§cUnknown subcommand.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return partial(args[0], List.of("join", "setname", "setspawn", "cookie", "debug"));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("cookie")) {
            return partial(args[1], List.of("give"));
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("join") || (args[0].equalsIgnoreCase("cookie") && args[1].equalsIgnoreCase("give")))) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
            return partial(args[2], names);
        }
        if ((args.length == 3 && (args[0].equalsIgnoreCase("setname") || args[0].equalsIgnoreCase("setspawn")))
                || (args.length == 4 && args[0].equalsIgnoreCase("join"))
                || (args.length == 4 && args[0].equalsIgnoreCase("cookie") && args[1].equalsIgnoreCase("give"))) {
            return partial(args[args.length - 1], List.of("1", "2", "3"));
        }
        return List.of();
    }

    private static HouseSlot parseSlot(String s) {
        try {
            return HouseSlot.fromIndex(Integer.parseInt(s));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static List<String> slice(String[] arr, int start) {
        List<String> out = new ArrayList<>();
        for (int i = start; i < arr.length; i++) out.add(arr[i]);
        return out;
    }

    private static List<String> partial(String token, List<String> options) {
        if (token == null || token.isEmpty()) return options;
        String lower = token.toLowerCase();
        List<String> out = new ArrayList<>();
        for (String opt : options) {
            if (opt.toLowerCase().startsWith(lower)) out.add(opt);
        }
        return out;
    }
}
