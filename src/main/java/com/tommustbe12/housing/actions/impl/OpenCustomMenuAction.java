package com.tommustbe12.housing.actions.impl;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.ActionContext;
import com.tommustbe12.housing.custommenus.CustomMenu;
import com.tommustbe12.housing.custommenus.CustomMenusService;
import com.tommustbe12.housing.custommenus.gui.CustomMenuRuntimeHolder;
import com.tommustbe12.housing.gui.CustomMenusGui;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class OpenCustomMenuAction implements Action {
    private final CustomMenusService menus;
    private final UUID menuId;

    public OpenCustomMenuAction(CustomMenusService menus, UUID menuId) {
        this.menus = menus;
        this.menuId = menuId;
    }

    @Override
    public String type() {
        return "open_custom_menu";
    }

    public UUID menuId() {
        return menuId;
    }

    @Override
    public void execute(ActionContext ctx) {
        if (ctx == null) return;
        Player player = ctx.player();
        if (player == null || menuId == null) return;
        CustomMenu menu = menus.find(ctx.houseOwner(), ctx.houseSlot(), menuId);
        if (menu == null) return;
        CustomMenuRuntimeHolder holder = new CustomMenuRuntimeHolder(ctx.houseOwner(), ctx.houseSlot(), menu.id());
        Inventory inv = Bukkit.createInventory(holder, CustomMenu.FIXED_SIZE, menu.title());
        ItemStack[] src = menu.contents();
        ItemStack[] out = new ItemStack[CustomMenu.FIXED_SIZE];
        ItemStack pane = CustomMenusGui.dividerPane();
        for (int i = 0; i < out.length; i++) {
            if (CustomMenusGui.isDividerSlot(i)) out[i] = pane;
            else out[i] = (src != null && i < src.length) ? src[i] : null;
        }
        inv.setContents(out);
        player.closeInventory();
        player.openInventory(inv);
    }
}
