package civsim.model;

/**
 * Phase 2C.2 — Attribution confidence of responsibility for a historical grievance.
 */
public enum AttributionConfidence {
    CONFIRMED(true, "Unambiguously verified by eyewitnesses, formal records, or admission"),
    STRONGLY_SUPPORTED(true, "Strong circumstantial or battlefield evidence pointing to responsible party"),
    DISPUTED(false, "Responsibility is contested, ambiguous, or contested by counter-evidence"),
    UNKNOWN(false, "Perpetrator or commanding party is entirely unidentified");

    private final boolean retaliationPermitted;
    private final String description;

    AttributionConfidence(boolean retaliationPermitted, String description) {
        this.retaliationPermitted = retaliationPermitted;
        this.description = description;
    }

    public boolean isRetaliationPermitted() {
        return retaliationPermitted;
    }

    public String getDescription() {
        return description;
    }
}

