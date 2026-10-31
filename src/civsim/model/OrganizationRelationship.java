package civsim.model;

/**
 * Phase 2B.2 — Pairwise diplomatic relationship and formal treaties between two Organizations.
 * Stored canonically using symmetric key: minOrgId + ":" + maxOrgId.
 */
public class OrganizationRelationship {

    private final String organizationIdA;
    private final String organizationIdB;

    private double relationshipScore;
    private DiplomaticStatus diplomaticStatus;
    private boolean treatyActive;
    private Integer treatySignedYear;
    private Integer treatySignedMonth;
    private int lastUpdatedYear;
    private int lastUpdatedMonth;

    public OrganizationRelationship(String id1, String id2, int currentYear, int currentMonth) {
        if (id1.compareTo(id2) <= 0) {
            this.organizationIdA = id1;
            this.organizationIdB = id2;
        } else {
            this.organizationIdA = id2;
            this.organizationIdB = id1;
        }
        this.relationshipScore = 0.0;
        this.diplomaticStatus = DiplomaticStatus.NEUTRAL;
        this.treatyActive = false;
        this.treatySignedYear = null;
        this.treatySignedMonth = null;
        this.lastUpdatedYear = currentYear;
        this.lastUpdatedMonth = currentMonth;
    }

    public static String canonicalKey(String id1, String id2) {
        if (id1 == null || id2 == null) return "UNKNOWN:UNKNOWN";
        return (id1.compareTo(id2) <= 0) ? (id1 + ":" + id2) : (id2 + ":" + id1);
    }

    public String getKey() {
        return canonicalKey(organizationIdA, organizationIdB);
    }

    public String getOrganizationIdA() { return organizationIdA; }
    public String getOrganizationIdB() { return organizationIdB; }
    public double getRelationshipScore() { return relationshipScore; }
    public DiplomaticStatus getDiplomaticStatus() { return diplomaticStatus; }
    public boolean isTreatyActive() { return treatyActive; }
    public Integer getTreatySignedYear() { return treatySignedYear; }
    public Integer getTreatySignedMonth() { return treatySignedMonth; }
    public int getLastUpdatedYear() { return lastUpdatedYear; }
    public int getLastUpdatedMonth() { return lastUpdatedMonth; }

    public void adjustScore(double delta, int currentYear, int currentMonth) {
        this.relationshipScore = Math.max(-100.0, Math.min(100.0, this.relationshipScore + delta));
        this.lastUpdatedYear = currentYear;
        this.lastUpdatedMonth = currentMonth;

        // If formal treaty is NOT active, update diplomatic status according to score band
        if (!treatyActive) {
            this.diplomaticStatus = DiplomaticStatus.fromScore(this.relationshipScore);
        }
    }

    public void setRelationshipScore(double score, int currentYear, int currentMonth) {
        this.relationshipScore = Math.max(-100.0, Math.min(100.0, score));
        this.lastUpdatedYear = currentYear;
        this.lastUpdatedMonth = currentMonth;
        if (!treatyActive) {
            this.diplomaticStatus = DiplomaticStatus.fromScore(this.relationshipScore);
        }
    }

    public void setDiplomaticStatus(DiplomaticStatus status, int currentYear, int currentMonth) {
        if (status == null) return;
        this.diplomaticStatus = status;
        this.treatyActive = status.isTreatyActive();
        if (treatyActive) {
            this.treatySignedYear = currentYear;
            this.treatySignedMonth = currentMonth;
        } else {
            this.treatySignedYear = null;
            this.treatySignedMonth = null;
        }
        this.lastUpdatedYear = currentYear;
        this.lastUpdatedMonth = currentMonth;
    }

    public void breakTreaty(int currentYear, int currentMonth, double penalty) {
        this.treatyActive = false;
        this.treatySignedYear = null;
        this.treatySignedMonth = null;
        adjustScore(-Math.abs(penalty), currentYear, currentMonth);
        this.diplomaticStatus = DiplomaticStatus.fromScore(this.relationshipScore);
    }

    public boolean isOther(String orgId) {
        return organizationIdA.equals(orgId) || organizationIdB.equals(orgId);
    }

    public String getOther(String orgId) {
        if (organizationIdA.equals(orgId)) return organizationIdB;
        if (organizationIdB.equals(orgId)) return organizationIdA;
        return null;
    }

    @Override
    public String toString() {
        return String.format("Relationship[%s <-> %s: %.1f (%s, Treaty: %s)]",
                organizationIdA, organizationIdB, relationshipScore, diplomaticStatus.getDisplayName(), treatyActive);
    }
}

