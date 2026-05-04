package com.tommustbe12.housing.houses;

public enum HouseSlot {
    SLOT_1(1),
    SLOT_2(2),
    SLOT_3(3);

    private final int index;

    HouseSlot(int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }

    public static HouseSlot fromIndex(int index) {
        for (HouseSlot slot : values()) {
            if (slot.index == index) return slot;
        }
        return null;
    }
}

