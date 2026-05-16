package com.tommustbe12.housing.custommenus;

import com.tommustbe12.housing.actions.ActionList;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CustomMenu {
    public enum ClickKind { LEFT, RIGHT }

    public static final int FIXED_ROWS = 3;
    public static final int FIXED_SIZE = FIXED_ROWS * 9;

    public static final class SlotActions {
        private final ActionList left = new ActionList();
        private final ActionList right = new ActionList();
        public ActionList left() { return left; }
        public ActionList right() { return right; }
    }

    private final UUID id;
    private String name;
    private String title;
    private int rows; // fixed to 3
    private ItemStack[] contents; // size FIXED_SIZE
    private final Map<Integer, SlotActions> slotActions = new HashMap<>();

    public CustomMenu(UUID id, String name, int rows) {
        this.id = id;
        this.name = name;
        this.rows = FIXED_ROWS;
        this.contents = new ItemStack[FIXED_SIZE];
    }

    public UUID id() { return id; }
    public String name() { return name; }
    public void setName(String name) { this.name = name; }
    public String title() { return title == null || title.isBlank() ? ("Menu: " + name) : title; }
    public void setTitle(String title) { this.title = title; }
    public int rows() { return rows; }
    public void setRows(int rows) {
        // Legacy method: custom menus are fixed to 3 rows.
        this.rows = FIXED_ROWS;
        if (this.contents == null || this.contents.length != FIXED_SIZE) this.contents = new ItemStack[FIXED_SIZE];
    }

    public ItemStack[] contents() { return contents; }
    public void setContents(ItemStack[] contents) { this.contents = contents; }

    public Map<Integer, SlotActions> slotActions() { return slotActions; }
}
