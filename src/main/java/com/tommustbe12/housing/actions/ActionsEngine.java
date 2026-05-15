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
        runFrom(list, ctx, 0);
    }

    public void runLater(ActionList list, ActionContext ctx, long ticks) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> run(list, ctx), ticks);
    }

    private void runFrom(ActionList list, ActionContext ctx, int startIndex) {
        if (list == null || list.actions() == null) return;

        for (int i = Math.max(0, startIndex); i < list.actions().size(); i++) {
            Action action = list.actions().get(i);
            if (action == null) continue;

            try {
                // Pause execution: schedule remaining actions and stop now.
                if (action instanceof com.tommustbe12.housing.actions.impl.PauseExecutionAction pause) {
                    long ticks = Math.max(0L, pause.ticks());

                    final int nextIndex = i + 1;

                    Bukkit.getScheduler().runTaskLater(
                            plugin,
                            () -> runFrom(list, ctx, nextIndex),
                            ticks
                    );

                    return;
                }

                action.execute(ctx);

            } catch (Throwable t) {
                debug.error("Action failed: " + action.type(), t);
            }
        }
    }
}
