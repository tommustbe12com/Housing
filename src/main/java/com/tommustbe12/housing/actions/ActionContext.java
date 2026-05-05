package com.tommustbe12.housing.actions;

import com.tommustbe12.housing.debug.Debug;
import com.tommustbe12.housing.houses.HouseSlot;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public record ActionContext(
        Plugin plugin,
        Debug debug,
        UUID houseOwner,
        HouseSlot houseSlot,
        World world,
        Player player,
        Player other,
        Location location
) {
}

