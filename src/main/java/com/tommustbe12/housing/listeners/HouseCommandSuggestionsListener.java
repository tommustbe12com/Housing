package com.tommustbe12.housing.listeners;

import com.tommustbe12.housing.actions.placeholders.Placeholders;
import com.tommustbe12.housing.actions.placeholders.VariablesStore;
import com.tommustbe12.housing.actions.storage.SimpleActionCodec;
import com.tommustbe12.housing.commands.HouseCommandsStorage;
import com.tommustbe12.housing.houses.HouseManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.plugin.Plugin;

public final class HouseCommandSuggestionsListener implements Listener {
    private final HouseManager houses;
    private final HouseCommandsStorage storage;
    private final SimpleActionCodec codec;

    public HouseCommandSuggestionsListener(Plugin plugin, HouseManager houses) {
        this.houses = houses;
        this.storage = new HouseCommandsStorage(plugin);
        VariablesStore vars = new VariablesStore(plugin);
        Placeholders ph = new Placeholders(vars);
        this.codec = new SimpleActionCodec(ph, vars, houses, (ctx, fn, global) -> {},
                new com.tommustbe12.housing.inventorylayouts.InventoryLayoutsService(plugin),
                new com.tommustbe12.housing.custommenus.CustomMenusService(plugin, houses),
                new com.tommustbe12.housing.teams.TeamsService(plugin),
                new com.tommustbe12.housing.groups.HouseGroupsService(plugin, houses));
    }

    @EventHandler
    public void onSend(PlayerCommandSendEvent event) {
        var info = houses.getHouseInfoByWorld(event.getPlayer().getWorld());
        if (info == null) return;
        var cmds = storage.load(info.owner(), info.slot(), codec);
        event.getCommands().addAll(cmds.keySet());
    }
}
