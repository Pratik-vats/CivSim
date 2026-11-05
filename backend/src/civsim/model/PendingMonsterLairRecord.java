package civsim.model;

/**
 * Phase 2B.3 / Bridge to Phase 2B.5 -- Record emitted when an unclaimed World Anomaly
 * passes its 6-month mutation deadline and evolves into a Monster Lair.
 */
public final class PendingMonsterLairRecord {
    private final String lairEventId;
    private final String anomalyId;
    private final double posX;
    private final double posY;
    private final SupernaturalResourceType resourceType;
    private final ResourceGrade resourceGrade;
    private final double remainingQuantityAtMutation;
    private final boolean wasContested;
    private final int mutationMonth;
    private final String threatSignature;

    public PendingMonsterLairRecord(
            String lairEventId,
            String anomalyId,
            double posX,
            double posY,
            SupernaturalResourceType resourceType,
            ResourceGrade resourceGrade,
            double remainingQuantityAtMutation,
            boolean wasContested,
            int mutationMonth,
            String threatSignature) {
        this.lairEventId = lairEventId;
        this.anomalyId = anomalyId;
        this.posX = posX;
        this.posY = posY;
        this.resourceType = resourceType != null ? resourceType : SupernaturalResourceType.RAW_AETHER_CRYSTAL;
        this.resourceGrade = resourceGrade != null ? resourceGrade : ResourceGrade.COMMON;
        this.remainingQuantityAtMutation = Math.max(0.0, remainingQuantityAtMutation);
        this.wasContested = wasContested;
        this.mutationMonth = mutationMonth;
        this.threatSignature = threatSignature != null ? threatSignature : "DORMANT_AETHER_CORRUPTION";
    }

    public String getLairEventId() { return lairEventId; }
    public String getAnomalyId() { return anomalyId; }
    public double getPosX() { return posX; }
    public double getPosY() { return posY; }
    public SupernaturalResourceType getResourceType() { return resourceType; }
    public ResourceGrade getResourceGrade() { return resourceGrade; }
    public double getRemainingQuantityAtMutation() { return remainingQuantityAtMutation; }
    public boolean wasContested() { return wasContested; }
    public int getMutationMonth() { return mutationMonth; }
    public String getThreatSignature() { return threatSignature; }

    @Override
    public String toString() {
        return String.format("PendingMonsterLairRecord[%s: Anomaly %s (%s, %s) at (%.1f, %.1f) mut:M%d Rem:%.1f Threat:%s]",
                lairEventId, anomalyId, resourceType.getDisplayName(), resourceGrade.getDisplayName(),
                posX, posY, mutationMonth, remainingQuantityAtMutation, threatSignature);
    }
}

