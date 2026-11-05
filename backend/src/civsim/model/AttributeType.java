package civsim.model;

/**
 * Stage 2A.6 — The 10 core individual attributes.
 * 9 are directly trainable through focused attribute drills;
 * Luck is not directly trainable via ordinary drills.
 */
public enum AttributeType {
    STRENGTH("Strength", true),
    AGILITY("Agility", true),
    VITALITY("Vitality", true),
    ENDURANCE("Endurance", true),
    INTELLIGENCE("Intelligence", true),
    PERCEPTION("Perception", true),
    WILLPOWER("Willpower", true),
    LUCK("Luck", false),
    AETHER_CAPACITY("Aether Capacity", true),
    AETHER_CONTROL("Aether Control", true);

    private final String displayName;
    private final boolean trainable;

    AttributeType(String displayName, boolean trainable) {
        this.displayName = displayName;
        this.trainable = trainable;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isTrainable() {
        return trainable;
    }

    /**
     * Case-insensitive name and alias resolver.
     */
    public static AttributeType fromString(String name) {
        if (name == null) return null;
        String clean = name.trim().toUpperCase().replace(" ", "_").replace("-", "_");
        switch (clean) {
            case "STRENGTH": case "STR":
                return STRENGTH;
            case "AGILITY": case "AGI":
                return AGILITY;
            case "VITALITY": case "VIT":
                return VITALITY;
            case "ENDURANCE": case "END":
                return ENDURANCE;
            case "INTELLIGENCE": case "INT":
                return INTELLIGENCE;
            case "PERCEPTION": case "PER":
                return PERCEPTION;
            case "WILLPOWER": case "WIL": case "WISDOM": case "WIS":
                return WILLPOWER;
            case "LUCK": case "LCK":
                return LUCK;
            case "AETHER_CAPACITY": case "AETHERCAPACITY": case "AET_CAP": case "CAPACITY":
                return AETHER_CAPACITY;
            case "AETHER_CONTROL": case "AETHERCONTROL": case "AET_CTRL": case "CONTROL": case "AETHER": case "AET":
                return AETHER_CONTROL;
            default:
                for (AttributeType at : values()) {
                    if (at.name().equalsIgnoreCase(clean) || at.displayName.equalsIgnoreCase(name.trim())) {
                        return at;
                    }
                }
                return null;
        }
    }
}
