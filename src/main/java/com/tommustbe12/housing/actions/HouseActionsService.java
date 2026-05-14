package com.tommustbe12.housing.actions;

import com.tommustbe12.housing.actions.placeholders.Placeholders;
import com.tommustbe12.housing.actions.placeholders.VariablesStore;
import com.tommustbe12.housing.actions.storage.HouseActionsStorage;
import com.tommustbe12.housing.actions.storage.SimpleActionCodec;
import com.tommustbe12.housing.actions.impl.RunFunctionAction;
import com.tommustbe12.housing.debug.Debug;
import com.tommustbe12.housing.functions.FunctionStorage;
import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.houses.HouseSlot;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class HouseActionsService {
    private final Plugin plugin;
    private final Debug debug;
    private final ActionsEngine engine;
    private final HouseActionsStorage storage;
    private final SimpleActionCodec codec;
    private final FunctionStorage functions;

    public HouseActionsService(Plugin plugin, Debug debug, HouseManager houses) {
        this.plugin = plugin;
        this.debug = debug;
        this.engine = new ActionsEngine(plugin, debug);
        this.storage = new HouseActionsStorage(plugin);
        this.functions = new FunctionStorage(plugin);
        VariablesStore variables = new VariablesStore(plugin);
        Placeholders placeholders = new Placeholders(variables);
        this.codec = new SimpleActionCodec(placeholders, variables, houses, this::runFunction,
                new com.tommustbe12.housing.inventorylayouts.InventoryLayoutsService(plugin),
                new com.tommustbe12.housing.custommenus.CustomMenusService(plugin, houses),
                new com.tommustbe12.housing.teams.TeamsService(plugin));
    }

    public void runEvent(UUID owner, HouseSlot slot, World world, Player player, String eventKey) {
        Map<String, ActionList> events = storage.loadEventActions(owner, slot, codec);
        ActionList list = events.get(eventKey.toLowerCase(Locale.ROOT));
        if (list == null) return;
        ActionContext ctx = new ActionContext(plugin, debug, owner, slot, world, player, null, player == null ? null : player.getLocation());
        engine.run(list, ctx);
    }

    public void runFunction(UUID owner, HouseSlot slot, World world, Player player, String functionName, boolean global) {
        ActionContext ctx = new ActionContext(plugin, debug, owner, slot, world, player, null, player == null ? null : player.getLocation());
        runFunction(ctx, functionName, global);
    }

    private void runFunction(ActionContext ctx, String functionName, boolean global) {
        if (functionName == null || functionName.isBlank()) return;
        var all = functions.loadAll(ctx.houseOwner(), ctx.houseSlot(), codec);
        ActionList fn = all.get(functionName);
        if (fn == null) return;

        if (global) {
            for (Player p : ctx.world().getPlayers()) {
                ActionContext per = new ActionContext(ctx.plugin(), ctx.debug(), ctx.houseOwner(), ctx.houseSlot(), ctx.world(), p, ctx.other(), p.getLocation());
                engine.run(fn, per);
            }
        } else {
            engine.run(fn, ctx);
        }
    }

    public void invalidate(UUID owner, HouseSlot slot) {
        // no-op (no cache)
    }

    public void clearAll() {
        // no-op (no cache)
    }

    private static String cacheKey(UUID owner, HouseSlot slot) {
        return owner + ":" + slot.index();
    }
}
