package com.tommustbe12.housing.listeners;

import com.tommustbe12.housing.actions.ActionContext;
import com.tommustbe12.housing.actions.ActionsEngine;
import com.tommustbe12.housing.custommenus.CustomMenu;
import com.tommustbe12.housing.custommenus.CustomMenusService;
import com.tommustbe12.housing.custommenus.gui.CustomMenuRuntimeHolder;
import com.tommustbe12.housing.debug.Debug;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public final class CustomMenuRuntimeListener implements Listener {
    private final Plugin plugin;
    private final Debug debug;
    private final CustomMenusService menus;
    private final ActionsEngine engine;

    public CustomMenuRuntimeListener(Plugin plugin, Debug debug, CustomMenusService menus) {
        this.plugin = plugin;
        this.debug = debug;
        this.menus = menus;
        this.engine = new ActionsEngine(plugin, debug);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getView().getType() != InventoryType.CHEST) return;
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof CustomMenuRuntimeHolder holder)) return;

        // Always cancel to prevent taking/moving items.
        event.setCancelled(true);

        int raw = event.getRawSlot();
        if (raw < 0 || raw >= top.getSize()) return;

        ItemStack clicked = top.getItem(raw);
        if (clicked == null || clicked.getType().isAir()) return;

        CustomMenu menu = menus.find(holder.owner(), holder.slot(), holder.menuId());
        if (menu == null) return;

        CustomMenu.SlotActions slotActions = menu.slotActions().get(raw);
        if (slotActions == null) return;

        boolean right = event.isRightClick();
        var list = right ? slotActions.right() : slotActions.left();
        if (list == null || list.actions().isEmpty()) return;

        ActionContext ctx = new ActionContext(plugin, debug, holder.owner(), holder.slot(), player.getWorld(), player, null, player.getLocation());
        engine.run(list, ctx);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getView().getType() != InventoryType.CHEST) return;
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof CustomMenuRuntimeHolder)) return;
        event.setCancelled(true);
    }
}

