package com.tommustbe12.housing.regions;

import com.tommustbe12.housing.actions.ActionList;
import org.bukkit.Location;

public final class RegionData {
    private String name;
    private Location pos1;
    private Location pos2;
    private final RegionSettings settings;
    private ActionList entryActions;
    private ActionList exitActions;

    public RegionData(String name, Location pos1, Location pos2) {
        this.name = name;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.settings = new RegionSettings();
        this.entryActions = new ActionList();
        this.exitActions = new ActionList();
    }

    public String name() { return name; }
    public void setName(String name) { this.name = name; }

    public Location pos1() { return pos1; }
    public Location pos2() { return pos2; }
    public void setPos1(Location pos1) { this.pos1 = pos1; }
    public void setPos2(Location pos2) { this.pos2 = pos2; }

    public RegionSettings settings() { return settings; }

    public ActionList entryActions() { return entryActions; }
    public void setEntryActions(ActionList entryActions) { this.entryActions = entryActions == null ? new ActionList() : entryActions; }

    public ActionList exitActions() { return exitActions; }
    public void setExitActions(ActionList exitActions) { this.exitActions = exitActions == null ? new ActionList() : exitActions; }
}

