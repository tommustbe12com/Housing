package com.tommustbe12.housing.teams;

import org.bukkit.ChatColor;

import java.util.UUID;

public final class HouseTeam {
    private final UUID id;
    private String name;
    private String tag;
    private ChatColor color;
    private boolean friendlyFire;

    public HouseTeam(UUID id, String name) {
        this.id = id;
        this.name = name == null ? "Team" : name;
        this.tag = "";
        this.color = ChatColor.WHITE;
        this.friendlyFire = false;
    }

    public UUID id() { return id; }
    public String name() { return name; }
    public String tag() { return tag; }
    public ChatColor color() { return color; }
    public boolean friendlyFire() { return friendlyFire; }

    public void setName(String name) { this.name = name == null ? "" : name; }
    public void setTag(String tag) { this.tag = tag == null ? "" : tag; }
    public void setColor(ChatColor color) { this.color = color == null ? ChatColor.WHITE : color; }
    public void setFriendlyFire(boolean friendlyFire) { this.friendlyFire = friendlyFire; }
}

