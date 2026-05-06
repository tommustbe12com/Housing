package com.tommustbe12.housing.listeners;

import com.tommustbe12.housing.actions.ActionContext;
import com.tommustbe12.housing.actions.ActionsEngine;
import com.tommustbe12.housing.actions.placeholders.Placeholders;
import com.tommustbe12.housing.actions.placeholders.VariablesStore;
import com.tommustbe12.housing.actions.storage.SimpleActionCodec;
import com.tommustbe12.housing.commands.HouseCommandsStorage;
import com.tommustbe12.housing.debug.Debug;
import com.tommustbe12.housing.houses.HouseManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;

import java.util.Locale;

public final class HouseCommandsListener implements Listener {
    private final Plugin plugin;
    private final Debug debug;
    private final HouseManager houses;
    private final HouseCommandsStorage storage;
    private final SimpleActionCodec codec;
    private final ActionsEngine engine;
    private final com.tommustbe12.housing.actions.HouseActionsService actions;

    public HouseCommandsListener(Plugin plugin, Debug debug, HouseManager houses, com.tommustbe12.housing.actions.HouseActionsService actions) {
        this.plugin = plugin;
        this.debug = debug;
        this.houses = houses;
        this.actions = actions;
        this.storage = new HouseCommandsStorage(plugin);
        VariablesStore vars = new VariablesStore(plugin);
        Placeholders ph = new Placeholders(vars);
        this.codec = new SimpleActionCodec(ph, vars, houses, (ctx, fn, global) -> actions.runFunction(ctx.houseOwner(), ctx.houseSlot(), ctx.world(), ctx.player(), fn, global));
        this.engine = new ActionsEngine(plugin, debug);
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        var info = houses.getHouseInfoByWorld(event.getPlayer().getWorld());
        if (info == null) return;

        String msg = event.getMessage();
        if (msg == null || msg.length() < 2 || !msg.startsWith("/")) return;
        String label = msg.substring(1).trim();
        if (label.contains(" ")) return; // no args supported
        label = label.toLowerCase(Locale.ROOT);

        var map = storage.load(info.owner(), info.slot(), codec);
        var list = map.get(label);
        if (list == null) return;

        event.setCancelled(true);
        debug.toOps("House cmd /" + label + " by " + event.getPlayer().getName());
        engine.run(list, new ActionContext(plugin, debug, info.owner(), info.slot(), event.getPlayer().getWorld(), event.getPlayer(), null, event.getPlayer().getLocation()));
    }
}
