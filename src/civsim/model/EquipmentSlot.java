package civsim.model;

/**
 * Phase 2C.1 -- Canonical equipment slots on a humanoid combatant.
 */
public enum EquipmentSlot {
    MAIN_HAND("Main Hand"),
    OFF_HAND("Off Hand"),
    HEAD("Head"),
    CHEST("Chest"),
    ARMS("Arms"),
    LEGS("Legs"),
    FEET("Feet"),
    ACCESSORY_1("Accessory 1"),
    ACCESSORY_2("Accessory 2"),
    FOCUS("Focus");

    private final String displayName;

    EquipmentSlot(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

