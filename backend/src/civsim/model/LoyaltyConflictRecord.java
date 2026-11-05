package civsim.model;

/**
 * Phase 2B.2 — Record generated when a person holds active memberships in two mutually hostile organizations.
 */
public class LoyaltyConflictRecord {

    private final String conflictId;
    private final String personId;
    private final String organizationIdA;
    private final String organizationIdB;
    private final int detectedYear;
    private final int detectedMonth;
    private final String description;

    private boolean resolved;
    private String resolutionReason;
    private Integer resolvedYear;
    private Integer resolvedMonth;

    public LoyaltyConflictRecord(
            String conflictId,
            String personId,
            String organizationIdA,
            String organizationIdB,
            int detectedYear,
            int detectedMonth,
            String description) {
        this.conflictId = conflictId;
        this.personId = personId;
        this.organizationIdA = organizationIdA;
        this.organizationIdB = organizationIdB;
        this.detectedYear = detectedYear;
        this.detectedMonth = detectedMonth;
        this.description = description != null ? description : "Mutual hostility between affiliated organizations";
        this.resolved = false;
        this.resolutionReason = null;
        this.resolvedYear = null;
        this.resolvedMonth = null;
    }

    public String getConflictId() { return conflictId; }
    public String getPersonId() { return personId; }
    public String getOrganizationIdA() { return organizationIdA; }
    public String getOrganizationIdB() { return organizationIdB; }
    public int getDetectedYear() { return detectedYear; }
    public int getDetectedMonth() { return detectedMonth; }
    public String getDescription() { return description; }
    public boolean isResolved() { return resolved; }
    public String getResolutionReason() { return resolutionReason; }
    public Integer getResolvedYear() { return resolvedYear; }
    public Integer getResolvedMonth() { return resolvedMonth; }

    public void resolve(int year, int month, String reason) {
        this.resolved = true;
        this.resolvedYear = year;
        this.resolvedMonth = month;
        this.resolutionReason = reason != null ? reason : "Resolved";
    }

    @Override
    public String toString() {
        return String.format("LoyaltyConflict[%s: %s between %s and %s (Resolved: %s)]",
                conflictId, personId, organizationIdA, organizationIdB, resolved);
    }
}

