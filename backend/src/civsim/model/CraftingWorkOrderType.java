package civsim.model;

/**
 * Phase 2C.1 -- Type of crafting work order.
 */
public enum CraftingWorkOrderType {
    NEW_CRAFT("New Craft"),
    ENCHANT("Enchant"),
    UPGRADE("Reforge / Upgrade"),
    REPAIR("Repair");

    private final String displayName;

    CraftingWorkOrderType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

