package com.tommustbe12.housing.actions.conditions;

import com.tommustbe12.housing.actions.ActionContext;
import com.tommustbe12.housing.groups.HouseGroupsData;
import com.tommustbe12.housing.groups.HouseGroupsStore;
import com.tommustbe12.housing.groups.HouseMembersData;
import com.tommustbe12.housing.groups.HouseMembersStore;

import java.util.UUID;

public final class RequiredGroupCondition implements Condition {
    private final UUID requiredGroupId;
    // Backwards-compat: old configs stored a group name and compared it against %stat.group%.
    private final String requiredGroupLegacyName;

    public RequiredGroupCondition(UUID requiredGroupId) {
        this(requiredGroupId, null);
    }

    public RequiredGroupCondition(UUID requiredGroupId, String requiredGroupLegacyName) {
        this.requiredGroupId = requiredGroupId;
        this.requiredGroupLegacyName = requiredGroupLegacyName;
    }

    @Override
    public String type() {
        return "required_group";
    }

    @Override
    public boolean test(ActionContext ctx) {
        if (ctx.player() == null) return false;
        if (requiredGroupId == null) return false;

        // New group system: resolve the player's group id via stored house groups + membership map.
        UUID owner = ctx.houseOwner();
        var slot = ctx.houseSlot();

        HouseGroupsData groups = new HouseGroupsStore(ctx.plugin()).load(owner, slot);
        if (groups == null) return false;

        UUID playerId = ctx.player().getUniqueId();
        UUID effectiveGroupId;
        if (playerId.equals(owner)) {
            effectiveGroupId = groups.ownerId();
        } else {
            HouseMembersData members = new HouseMembersStore(ctx.plugin()).load(owner, slot);
            UUID explicit = members == null ? null : members.groupByPlayer().get(playerId);
            if (explicit != null && groups.groups().containsKey(explicit)) effectiveGroupId = explicit;
            else effectiveGroupId = groups.defaultGroupId();
        }
        return requiredGroupId.equals(effectiveGroupId);
    }

    public UUID requiredGroupId() {
        return requiredGroupId;
    }

    public String requiredGroupLegacyName() {
        return requiredGroupLegacyName;
    }
}
