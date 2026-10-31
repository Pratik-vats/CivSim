package civsim.model;

/**
 * Phase 2B.3 -- Explicit, idempotent event recording the transmission of anomaly
 * intelligence from one Organization or Settlement to an Allied or Friendly recipient.
 */
public final class InformationShareEvent {
    private final String eventId;
    private final String discoveryId;
    private final String anomalyId;
    private final String sharingPartyId;
    private final String recipientPartyId;
    private final int shareTotalMonths;
    private final double informationConfidence;

    public InformationShareEvent(
            String eventId,
            String discoveryId,
            String anomalyId,
            String sharingPartyId,
            String recipientPartyId,
            int shareTotalMonths,
            double informationConfidence) {
        this.eventId = eventId;
        this.discoveryId = discoveryId;
        this.anomalyId = anomalyId;
        this.sharingPartyId = sharingPartyId;
        this.recipientPartyId = recipientPartyId;
        this.shareTotalMonths = shareTotalMonths;
        this.informationConfidence = Math.max(0.25, Math.min(1.0, informationConfidence));
    }

    public String getEventId() { return eventId; }
    public String getDiscoveryId() { return discoveryId; }
    public String getAnomalyId() { return anomalyId; }
    public String getSharingPartyId() { return sharingPartyId; }
    public String getRecipientPartyId() { return recipientPartyId; }
    public int getShareTotalMonths() { return shareTotalMonths; }
    public double getInformationConfidence() { return informationConfidence; }

    @Override
    public String toString() {
        return String.format("InformationShareEvent[%s: Anomaly %s shared by %s to %s (conf: %.2f) at month %d]",
                eventId, anomalyId, sharingPartyId, recipientPartyId, informationConfidence, shareTotalMonths);
    }
}

