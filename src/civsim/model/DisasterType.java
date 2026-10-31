package civsim.model;

public enum DisasterType {
    NONE("None"),
    DROUGHT("Drought"),
    PLAGUE("Plague"),
    EARTHQUAKE("Earthquake");

    private final String displayName;

    DisasterType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
