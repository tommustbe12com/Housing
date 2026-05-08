package com.tommustbe12.housing.custommenus;

import com.tommustbe12.housing.actions.ActionList;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CustomMenu {
    public enum ClickKind { LEFT, RIGHT }

    public static final class SlotActions {
        private final ActionList left = new ActionList();
        private final ActionList right = new ActionList();
        public ActionList left() { return left; }
        public ActionList right() { return right; }
    }

    private final UUID id;
    private String name;
    private int rows; // 3 or 6
    private ItemStack[] contents; // size rows*9
    private final Map<Integer, SlotActions> slotActions = new HashMap<>();

    public CustomMenu(UUID id, String name, int rows) {
        this.id = id;
        this.name = name;
        this.rows = rows;
        this.contents = new ItemStack[rows * 9];
    }

    public UUID id() { return id; }
    public String name() { return name; }
    public void setName(String name) { this.name = name; }
    public int rows() { return rows; }
    public void setRows(int rows) {
        this.rows = rows;
        this.contents = new ItemStack[rows * 9];
        this.slotActions.clear();
    }

    public ItemStack[] contents() { return contents; }
    public void setContents(ItemStack[] contents) { this.contents = contents; }

    public Map<Integer, SlotActions> slotActions() { return slotActions; }
}
