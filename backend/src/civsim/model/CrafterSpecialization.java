package civsim.model;

/**
 * Phase 2C.1 -- Crafter artisan specializations.
 */
public enum CrafterSpecialization {
    WEAPON_FORGING("Weapon Forging"),
    ARMOR_FORGING("Armor Forging"),
    AETHERIC_CRAFTING("Aetheric Crafting"),
    ENCHANTING("Enchanting"),
    REPAIR_AND_RESTORATION("Repair & Restoration");

    private final String displayName;

    CrafterSpecialization(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

