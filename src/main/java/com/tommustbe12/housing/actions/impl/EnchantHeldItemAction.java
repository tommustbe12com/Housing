package com.tommustbe12.housing.actions.impl;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.ActionContext;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

public final class EnchantHeldItemAction implements Action {
    private final String enchantKey;
    private final int level;

    public EnchantHeldItemAction(String enchantKey, int level) {
        this.enchantKey = enchantKey == null ? "" : enchantKey;
        this.level = Math.max(1, level);
    }

    public String enchantKey() { return enchantKey; }
    public int level() { return level; }

    @Override
    public String type() {
        return "enchant_held_item";
    }

    @Override
    public void execute(ActionContext ctx) {
        if (ctx == null) return;
        var target = ctx.other() != null ? ctx.other() : ctx.player();
        if (target == null) return;
        ItemStack held = target.getInventory().getItemInMainHand();
        if (held == null || held.getType().isAir()) return;
        Enchantment ench = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(enchantKey.toLowerCase(java.util.Locale.ROOT)));
        if (ench == null) return;
        held.addUnsafeEnchantment(ench, level);
        target.updateInventory();
    }
}

