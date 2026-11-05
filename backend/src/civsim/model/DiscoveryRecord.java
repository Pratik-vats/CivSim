package civsim.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Phase 2B.3 / Phase 2B.4 -- Record of an anomaly discovery by an Organization, Settlement, or Person.
 * Enforces Entitlement Isolation: factions may only plan expeditions to anomalies for which
 * they hold a valid DiscoveryRecord.
 */
public final class DiscoveryRecord {
    private final String discoveryId;
    private final String anomalyId;
    private final String discovererPersonId;
    private final String owningOrganizationId; // Organization ID or Village ID
    private final int discoveryTotalMonths;
    private final int discoveredYear;
    private final int discoveredMonth;
    private final DetectionMethod method;
    private final double informationConfidence; // [0.25, 1.00]
    private final ResourceGrade estimatedGrade;
    private final boolean exactQuantityKnown;
    private final double reportedX;
    private final double reportedY;
    private boolean keptPrivate;
    private final Set<String> sharedRecipientIds = new HashSet<>();

    /**
     * Phase 2B.3 Authoritative Full Constructor.
     */
    public DiscoveryRecord(
            String discoveryId,
            String anomalyId,
            String discovererPersonId,
            String owningOrganizationId,
            int discoveryTotalMonths,
            DetectionMethod method,
            double informationConfidence,
            ResourceGrade estimatedGrade,
            boolean exactQuantityKnown,
            double reportedX,
            double reportedY,
            boolean keptPrivate) {
        this.discoveryId = discoveryId;
        this.anomalyId = anomalyId;
        this.discovererPersonId = discovererPersonId;
        this.owningOrganizationId = owningOrganizationId;
        this.discoveryTotalMonths = discoveryTotalMonths;
        this.discoveredYear = discoveryTotalMonths / 12;
        this.discoveredMonth = discoveryTotalMonths % 12;
        this.method = method != null ? method : DetectionMethod.DIRECT_SCOUTING;
        this.informationConfidence = Math.max(0.25, Math.min(1.0, informationConfidence));
        this.estimatedGrade = estimatedGrade != null ? estimatedGrade : ResourceGrade.COMMON;
        this.exactQuantityKnown = exactQuantityKnown;
        this.reportedX = reportedX;
        this.reportedY = reportedY;
        this.keptPrivate = keptPrivate;
        if (owningOrganizationId != null) {
            this.sharedRecipientIds.add(owningOrganizationId);
        }
    }

    /**
     * Phase 2B.4 Legacy Constructor.
     */
    public DiscoveryRecord(
            String discoveryId,
            String anomalyId,
            String discovererId,
            int discoveredYear,
            int discoveredMonth,
            long discoveredTotalMonths,
            double posX,
            double posY) {
        this(
                discoveryId,
                anomalyId,
                discovererId,
                discovererId,
                (int) discoveredTotalMonths,
                DetectionMethod.DIRECT_SCOUTING,
                1.0,
                ResourceGrade.COMMON,
                true,
                posX,
                posY,
                false
        );
    }

    public String getDiscoveryId() { return discoveryId; }
    public String getAnomalyId() { return anomalyId; }
    public String getDiscovererPersonId() { return discovererPersonId; }
    public String getDiscovererId() { return discovererPersonId != null ? discovererPersonId : owningOrganizationId; }
    public String getOwningOrganizationId() { return owningOrganizationId; }
    public int getDiscoveryTotalMonths() { return discoveryTotalMonths; }
    public long getDiscoveredTotalMonths() { return discoveryTotalMonths; }
    public int getDiscoveredYear() { return discoveredYear; }
    public int getDiscoveredMonth() { return discoveredMonth; }
    public DetectionMethod getMethod() { return method; }
    public double getInformationConfidence() { return informationConfidence; }
    public ResourceGrade getEstimatedGrade() { return estimatedGrade; }
    public boolean isExactQuantityKnown() { return exactQuantityKnown; }
    public double getReportedX() { return reportedX; }
    public double getReportedY() { return reportedY; }
    public double getPosX() { return reportedX; }
    public double getPosY() { return reportedY; }
    public boolean isKeptPrivate() { return keptPrivate; }
    public void setKeptPrivate(boolean keptPrivate) { this.keptPrivate = keptPrivate; }

    public Set<String> getSharedRecipientIds() {
        return Collections.unmodifiableSet(sharedRecipientIds);
    }

    public void addSharedRecipient(String recipientId) {
        if (recipientId != null) {
            this.sharedRecipientIds.add(recipientId);
        }
    }

    public boolean isSharedWith(String recipientId) {
        return recipientId != null && sharedRecipientIds.contains(recipientId);
    }

    @Override
    public String toString() {
        return String.format("DiscoveryRecord[%s: Anomaly %s by Person %s (Org: %s) via %s conf:%.2f at M%d]",
                discoveryId, anomalyId, discovererPersonId, owningOrganizationId, method.getDisplayName(),
                informationConfidence, discoveryTotalMonths);
    }
}
