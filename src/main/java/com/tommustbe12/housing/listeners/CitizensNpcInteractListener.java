package com.tommustbe12.housing.listeners;

import com.tommustbe12.housing.gui.NpcsGui;
import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.npcs.NpcData;
import com.tommustbe12.housing.npcs.NpcManager;
import net.citizensnpcs.api.event.NPCLeftClickEvent;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class CitizensNpcInteractListener implements Listener {
    private final HouseManager houses;
    private final NpcManager npcs;
    private final NpcsGui gui;

    public CitizensNpcInteractListener(HouseManager houses, NpcManager npcs, NpcsGui gui) {
        this.houses = houses;
        this.npcs = npcs;
        this.gui = gui;
    }

    @EventHandler
    public void onRightClick(NPCRightClickEvent event) {
        handle(event.getClicker(), event.getNPC().getId(), true);
    }

    @EventHandler
    public void onLeftClick(NPCLeftClickEvent event) {
        handle(event.getClicker(), event.getNPC().getId(), false);
    }

    private void handle(org.bukkit.entity.Player player, int citizensId, boolean rightClick) {
        var info = houses.getHouseInfoByWorld(player.getWorld());
        if (info == null) return;
        NpcData npc = npcs.getByCitizensId(info.owner(), info.slot(), player.getWorld(), citizensId);
        if (npc == null) return;

        boolean isOwner = info.owner().equals(player.getUniqueId());
        if (isOwner && player.isSneaking() && rightClick) {
            gui.openNpcEditor(player, info.owner(), info.slot(), npc);
            return;
        }
        npcs.clickNpc(player, info.owner(), info.slot(), player.getWorld(), npc);
    }
}

