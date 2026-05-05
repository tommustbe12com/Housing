package com.tommustbe12.housing.listeners;

import com.tommustbe12.housing.actions.ActionContext;
import com.tommustbe12.housing.actions.ActionsEngine;
import com.tommustbe12.housing.actions.placeholders.Placeholders;
import com.tommustbe12.housing.actions.placeholders.VariablesStore;
import com.tommustbe12.housing.actions.storage.SimpleActionCodec;
import com.tommustbe12.housing.actions.storage.SimpleActionSerializer;
import com.tommustbe12.housing.debug.Debug;
import com.tommustbe12.housing.gui.ItemEditGui;
import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.items.ItemActionsStorage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public final class ItemActionListener implements Listener {
    private final Plugin plugin;
    private final Debug debug;
    private final HouseManager houses;
    private final ItemEditGui itemEditGui;
    private final ItemActionsStorage storage;
    private final SimpleActionCodec codec;
    private final ActionsEngine engine;

    public ItemActionListener(Plugin plugin, Debug debug, HouseManager houses, ItemEditGui itemEditGui) {
        this.plugin = plugin;
        this.debug = debug;
        this.houses = houses;
        this.itemEditGui = itemEditGui;
        this.storage = new ItemActionsStorage(plugin);
        VariablesStore vars = new VariablesStore(plugin);
        Placeholders placeholders = new Placeholders(vars);
        this.codec = new SimpleActionCodec(placeholders, vars, houses);
        this.engine = new ActionsEngine(plugin, debug);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getAction().isRightClick()) return;

        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        UUID itemId = itemEditGui.getItemId(item);
        if (itemId == null) return;

        // Don't trigger actions while holding the housing menu item
        if (item.getType().name().equals("NETHER_STAR") && item.hasItemMeta() && item.getItemMeta().getDisplayName() != null
                && item.getItemMeta().getDisplayName().contains("Housing")) return;

        var info = houses.getHouseInfoByWorld(event.getPlayer().getWorld());
        if (info == null) return; // only run in houses for now

        var list = storage.load(itemId, codec);
        engine.run(list, new ActionContext(plugin, debug, info.owner(), info.slot(), event.getPlayer().getWorld(), event.getPlayer(), null, event.getPlayer().getLocation()));
    }
}

