package civsim.model;

public enum Occupation {
    CHILD("Child"),
    ELDER("Elder"),
    UNEMPLOYED("Unemployed"),
    FARMER("Farmer"),
    BUILDER("Builder"),
    MERCHANT("Merchant"),
    SOLDIER("Soldier"),
    LUMBERJACK("Lumberjack"),
    MINER("Miner"),
    BLACKSMITH("Blacksmith");

    private final String displayName;

    Occupation(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isWorker() {
        return this == FARMER || this == BUILDER || this == MERCHANT || this == SOLDIER
                || this == LUMBERJACK || this == MINER || this == BLACKSMITH;
    }
}
