package civsim.model;

/**
 * Phase 2B.3 -- Methods by which World Anomalies are detected and discovered.
 */
public enum DetectionMethod {
    SEER_PRECOGNITION("Seer Precognition"),
    SUPERNATURAL_SENSING("Supernatural Sensing"),
    DIRECT_SCOUTING("Direct Scouting"),
    PUBLIC_OBSERVATION("Public Observation"),
    SHARED_INTELLIGENCE("Shared Intelligence");

    private final String displayName;

    DetectionMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isPrecognition() {
        return this == SEER_PRECOGNITION;
    }
}

