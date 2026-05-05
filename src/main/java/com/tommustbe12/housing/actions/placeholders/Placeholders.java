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

        // %stats.key%
        int guard = 0;
        while (guard++ < 50) {
            int start = out.indexOf("%stats.");
            if (start == -1) break;
            int end = out.indexOf('%', start + 1);
            if (end == -1) break;
            String token = out.substring(start, end + 1);
            String key = token.substring("%stats.".length(), token.length() - 1);
            String val = variables.get(ctx.houseOwner(), ctx.houseSlot(), ctx.player() == null ? null : ctx.player().getUniqueId(), key);
            out = out.replace(token, val == null ? "" : val);
        }

        return out;
    }
}

