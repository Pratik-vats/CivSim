package civsim.model;

/**
 * Phase 2C.1 -- Physical location and lifecycle state of an equipment item.
 * Enforces the Single Physical Identity Invariant: an item cannot be simultaneously
 * EQUIPPED and IN_INVENTORY or duplicated across entities.
 */
public enum EquipmentLifecycleStatus {
    IN_INVENTORY("In Inventory"),
    EQUIPPED("Equipped"),
    IN_CLAN_VAULT("In Clan Vault"),
    IN_TRANSIT("In Transit"),
    RESERVED_FOR_ORDER("Reserved for Work Order"),
    UNRECOVERED_AT_SITE("Unrecovered at Site"),
    DESTROYED("Destroyed");

    private final String displayName;

    EquipmentLifecycleStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isAvailableForCustody() {
        return this == IN_INVENTORY || this == IN_CLAN_VAULT;
    }
}

