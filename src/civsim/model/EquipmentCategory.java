package civsim.model;

/**
 * Phase 2C.1 -- High-level functional category of an equipment item.
 */
public enum EquipmentCategory {
    WEAPON("Weapon"),
    DEFENSIVE_ARMOR("Defensive Armor"),
    ACCESSORY("Accessory"),
    SPECIAL_RELIC("Special Relic");

    private final String displayName;

    EquipmentCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

