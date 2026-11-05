package civsim.model;

/**
 * Phase 2C.1 -- Specific item form-factor and weapon/armor archetype.
 */
public enum EquipmentType {
    ONE_HANDED_SWORD("One-Handed Sword", EquipmentCategory.WEAPON, EquipmentSlot.MAIN_HAND, false),
    TWO_HANDED_SWORD("Two-Handed Sword", EquipmentCategory.WEAPON, EquipmentSlot.MAIN_HAND, true),
    AXE("War Axe", EquipmentCategory.WEAPON, EquipmentSlot.MAIN_HAND, false),
    SPEAR("Spear / Polearm", EquipmentCategory.WEAPON, EquipmentSlot.MAIN_HAND, true),
    DAGGER("Dagger", EquipmentCategory.WEAPON, EquipmentSlot.MAIN_HAND, false),
    BOW("Longbow", EquipmentCategory.WEAPON, EquipmentSlot.MAIN_HAND, true),
    STAFF("Aether Staff", EquipmentCategory.WEAPON, EquipmentSlot.MAIN_HAND, true),
    SHIELD("Shield", EquipmentCategory.DEFENSIVE_ARMOR, EquipmentSlot.OFF_HAND, false),
    HELMET("Helmet", EquipmentCategory.DEFENSIVE_ARMOR, EquipmentSlot.HEAD, false),
    CHESTPLATE("Chestplate", EquipmentCategory.DEFENSIVE_ARMOR, EquipmentSlot.CHEST, false),
    GAUNTLETS("Gauntlets", EquipmentCategory.DEFENSIVE_ARMOR, EquipmentSlot.ARMS, false),
    GREAVES("Greaves", EquipmentCategory.DEFENSIVE_ARMOR, EquipmentSlot.LEGS, false),
    BOOTS("Boots", EquipmentCategory.DEFENSIVE_ARMOR, EquipmentSlot.FEET, false),
    RING("Ring", EquipmentCategory.ACCESSORY, EquipmentSlot.ACCESSORY_1, false),
    AMULET("Amulet", EquipmentCategory.ACCESSORY, EquipmentSlot.ACCESSORY_2, false),
    TALISMAN("Talisman", EquipmentCategory.ACCESSORY, EquipmentSlot.ACCESSORY_1, false),
    CATALYST_RELIC("Catalyst Relic", EquipmentCategory.SPECIAL_RELIC, EquipmentSlot.FOCUS, false);

    private final String displayName;
    private final EquipmentCategory category;
    private final EquipmentSlot defaultSlot;
    private final boolean defaultTwoHanded;

    EquipmentType(String displayName, EquipmentCategory category, EquipmentSlot defaultSlot, boolean defaultTwoHanded) {
        this.displayName = displayName;
        this.category = category;
        this.defaultSlot = defaultSlot;
        this.defaultTwoHanded = defaultTwoHanded;
    }

    public String getDisplayName() {
        return displayName;
    }

    public EquipmentCategory getCategory() {
        return category;
    }

    public EquipmentSlot getDefaultSlot() {
        return defaultSlot;
    }

    public boolean isDefaultTwoHanded() {
        return defaultTwoHanded;
    }
}

