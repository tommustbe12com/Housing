package com.tommustbe12.housing.listeners;

import com.tommustbe12.housing.debug.Debug;
import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.npcs.NpcManager;
import com.tommustbe12.housing.tags.OwnerTagService;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class PlayerJoinListener implements Listener {
    private final Debug debug;
    private final HouseItemListener houseItemListener;
    private final HouseManager houses;
    private final OwnerTagService ownerTags;
    private final NpcManager npcs;

    public PlayerJoinListener(Debug debug, HouseItemListener houseItemListener, HouseManager houses, OwnerTagService ownerTags, NpcManager npcs) {
        this.debug = debug;
        this.houseItemListener = houseItemListener;
        this.houses = houses;
        this.ownerTags = ownerTags;
        this.npcs = npcs;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        houseItemListener.giveMenuItem(event.getPlayer());
        // Always force hub on join, and hard-reset tag + scoreboard so nothing leaks from prior session.
        houses.sendToHub(event.getPlayer());
        ownerTags.clear(event.getPlayer());
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) event.getPlayer().setScoreboard(manager.getNewScoreboard());
        debug.toOps("Gave housing menu item to " + event.getPlayer().getName());
    }
}
