package com.tommustbe12.housing.groups;

import org.bukkit.GameMode;

public enum DefaultGameMode {
    ADVENTURE(GameMode.ADVENTURE),
    SURVIVAL(GameMode.SURVIVAL),
    CREATIVE(GameMode.CREATIVE);

    private final GameMode bukkit;

    DefaultGameMode(GameMode bukkit) {
        this.bukkit = bukkit;
    }

    public GameMode bukkit() {
        return bukkit;
    }
}

