package com.tommustbe12.housing.groups;

import java.util.*;

public final class HouseGroupsData {
    public static final String NAME_VISITOR = "Visitor";
    public static final String NAME_COOWNER = "Co-Owner";
    public static final String NAME_OWNER = "Owner";

    private final UUID houseOwner;
    private final Map<UUID, HouseGroup> groups;
    private UUID defaultGroupId;
    private UUID visitorId;
    private UUID coOwnerId;
    private UUID ownerId;

    public HouseGroupsData(UUID houseOwner, Map<UUID, HouseGroup> groups, UUID defaultGroupId) {
        this.houseOwner = houseOwner;
        this.groups = groups;
        this.defaultGroupId = defaultGroupId;
        indexSpecials();
    }

    public UUID houseOwner() { return houseOwner; }
    public Map<UUID, HouseGroup> groups() { return groups; }
    public UUID defaultGroupId() { return defaultGroupId; }
    public void setDefaultGroupId(UUID defaultGroupId) { this.defaultGroupId = defaultGroupId; }

    public UUID visitorId() { return visitorId; }
    public UUID coOwnerId() { return coOwnerId; }
    public UUID ownerId() { return ownerId; }

    public HouseGroup get(UUID id) {
        return id == null ? null : groups.get(id);
    }

    public List<HouseGroup> groupsInEditorOrder() {
        List<HouseGroup> list = new ArrayList<>(groups.values());
        list.sort(
                Comparator.comparingInt(HouseGroup::priority)
                        .thenComparing(g -> g.name() == null ? "" : g.name(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(HouseGroup::id)
        );
        return list;
    }

    public HouseGroup defaultGroup() {
        HouseGroup g = get(defaultGroupId);
        if (g != null) return g;
        HouseGroup v = get(visitorId);
        return v != null ? v : groups.values().stream().findFirst().orElse(null);
    }

    public void ensureDefaultsPresent() {
        if (visitorId == null || !groups.containsKey(visitorId)) {
            HouseGroup visitor = defaultVisitor();
            groups.put(visitor.id(), visitor);
        }
        if (coOwnerId == null || !groups.containsKey(coOwnerId)) {
            HouseGroup co = defaultCoOwner();
            groups.put(co.id(), co);
        }
        if (ownerId == null || !groups.containsKey(ownerId)) {
            HouseGroup owner = defaultOwner();
            groups.put(owner.id(), owner);
        }
        indexSpecials();
        enforceSpecialPriorities();
        enforceSpecialPermissions();
        if (defaultGroupId == null || defaultGroupId.equals(ownerId) || !groups.containsKey(defaultGroupId)) {
            defaultGroupId = visitorId;
        }
    }

    private void indexSpecials() {
        visitorId = null;
        coOwnerId = null;
        ownerId = null;
        for (HouseGroup g : groups.values()) {
            if (NAME_VISITOR.equalsIgnoreCase(g.name())) visitorId = g.id();
            if (NAME_COOWNER.equalsIgnoreCase(g.name())) coOwnerId = g.id();
            if (NAME_OWNER.equalsIgnoreCase(g.name())) ownerId = g.id();
        }
    }

    private void enforceSpecialPriorities() {
        // Keep special groups far apart to avoid collisions with user-created groups and to make ordering stable.
        // Visitor is always the lowest, Co-Owner is always above all custom groups, Owner is always the highest.
        HouseGroup visitor = get(visitorId);
        if (visitor != null && visitor.priority() != 1) visitor.setPriority(1);

        HouseGroup co = get(coOwnerId);
        if (co != null && co.priority() < 100) co.setPriority(100);

        HouseGroup owner = get(ownerId);
        if (owner != null && owner.priority() < 200) owner.setPriority(200);
    }

    private void enforceSpecialPermissions() {
        // Older saved configs may have special groups present but missing permission nodes.
        // Ensure special groups always have sane defaults.
        HouseGroup visitor = get(visitorId);
        if (visitor != null) {
            HouseGroup def = defaultVisitor();
            for (HousePermission p : HousePermission.values()) {
                if (def.has(p) && !visitor.has(p)) visitor.set(p, true);
            }
        }
        HouseGroup co = get(coOwnerId);
        if (co != null) {
            for (HousePermission p : HousePermission.values()) co.set(p, true);
        }
        HouseGroup owner = get(ownerId);
        if (owner != null) {
            for (HousePermission p : HousePermission.values()) owner.set(p, true);
        }
    }

    public static HouseGroupsData defaults(UUID owner) {
        Map<UUID, HouseGroup> groups = new LinkedHashMap<>();
        HouseGroup visitor = defaultVisitor();
        HouseGroup coOwner = defaultCoOwner();
        HouseGroup ownerG = defaultOwner();
        groups.put(visitor.id(), visitor);
        groups.put(coOwner.id(), coOwner);
        groups.put(ownerG.id(), ownerG);
        HouseGroupsData data = new HouseGroupsData(owner, groups, visitor.id());
        data.ensureDefaultsPresent();
        return data;
    }

    private static HouseGroup defaultVisitor() {
        HouseGroup v = new HouseGroup(UUID.randomUUID(), NAME_VISITOR, "&7[VISITOR]", 1, DefaultGameMode.ADVENTURE);
        v.set(HousePermission.FLY, true);
        v.set(HousePermission.WOOD_DOOR, true);
        v.set(HousePermission.IRON_DOOR, true);
        v.set(HousePermission.WOOD_TRAPDOOR, true);
        v.set(HousePermission.IRON_TRAPDOOR, true);
        v.set(HousePermission.FENCE_GATE, true);
        v.set(HousePermission.BUTTON, true);
        v.set(HousePermission.LEVER, true);
        v.set(HousePermission.USE_CHESTS, true);
        v.set(HousePermission.USE_ENDER_CHESTS, true);
        return v;
    }

    private static HouseGroup defaultCoOwner() {
        HouseGroup c = new HouseGroup(UUID.randomUUID(), NAME_COOWNER, "&b[CO-OWNER]", 100, DefaultGameMode.CREATIVE);
        for (HousePermission p : HousePermission.values()) c.set(p, true);
        return c;
    }

    private static HouseGroup defaultOwner() {
        HouseGroup o = new HouseGroup(UUID.randomUUID(), NAME_OWNER, "&6[OWNER]", 200, DefaultGameMode.CREATIVE);
        for (HousePermission p : HousePermission.values()) o.set(p, true);
        return o;
    }
}
