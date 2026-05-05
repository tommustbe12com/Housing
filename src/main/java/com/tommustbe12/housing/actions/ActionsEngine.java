package com.tommustbe12.housing.actions;

import com.tommustbe12.housing.debug.Debug;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class ActionsEngine {
    private final Plugin plugin;
    private final Debug debug;

    public ActionsEngine(Plugin plugin, Debug debug) {
        this.plugin = plugin;
        this.debug = debug;
    }

    public void run(ActionList list, ActionContext ctx) {
        if (list == null) return;
        for (Action action : list.actions()) {
            try {
                action.execute(ctx);
            } catch (Throwable t) {
                debug.error("Action failed: " + action.type(), t);
            }
        }
    }

    public void runLater(ActionList list, ActionContext ctx, long ticks) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> run(list, ctx), ticks);
    }
}

