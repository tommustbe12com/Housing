package com.tommustbe12.housing.actions.placeholders;

import com.tommustbe12.housing.actions.ActionContext;

public final class Placeholders {
    private final VariablesStore variables;

    public Placeholders(VariablesStore variables) {
        this.variables = variables;
    }

    public String resolve(ActionContext ctx, String input) {
        if (input == null) return "";
        String out = input;

        if (ctx.player() != null) {
            out = out.replace("%player.name%", ctx.player().getName());
            out = out.replace("%player.uuid%", ctx.player().getUniqueId().toString());
        }
        if (ctx.other() != null) {
            out = out.replace("%other.name%", ctx.other().getName());
            out = out.replace("%other.uuid%", ctx.other().getUniqueId().toString());
        }

        // %stat.key% or %stats.key%
        int guard = 0;
        while (guard++ < 100) {
            int startStats = out.indexOf("%stats.");
            int startStat = out.indexOf("%stat.");
            int start;
            String prefix;
            if (startStats == -1 && startStat == -1) break;
            if (startStats != -1 && (startStat == -1 || startStats < startStat)) {
                start = startStats;
                prefix = "%stats.";
            } else {
                start = startStat;
                prefix = "%stat.";
            }
            int end = out.indexOf('%', start + prefix.length());
            if (end == -1) break;
            String token = out.substring(start, end + 1);
            String key = token.substring(prefix.length(), token.length() - 1);
            String val = variables.get(ctx.houseOwner(), ctx.houseSlot(), ctx.player() == null ? null : ctx.player().getUniqueId(), key);
            out = out.replace(token, val == null ? "" : val);
        }

        return out;
    }
}
