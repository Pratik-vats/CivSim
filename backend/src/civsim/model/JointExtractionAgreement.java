package civsim.model;

import java.util.*;

/**
 * Phase 2B.4 -- Peaceful extraction agreement dividing anomaly yield proportionally among cooperative coalitions.
 */
public final class JointExtractionAgreement {
    private final String agreementId;
    private final String anomalyId;
    private final String contestId;
    private final List<String> participatingCoalitions;
    private final Map<String, Double> extractionShares;
    private final int agreedYear;
    private final int agreedMonth;

    public JointExtractionAgreement(
            String agreementId,
            String anomalyId,
            String contestId,
            List<String> participatingCoalitions,
            Map<String, Double> extractionShares,
            int agreedYear,
            int agreedMonth) {
        this.agreementId = agreementId;
        this.anomalyId = anomalyId;
        this.contestId = contestId;
        this.participatingCoalitions = List.copyOf(participatingCoalitions != null ? participatingCoalitions : Collections.emptyList());
        this.extractionShares = Map.copyOf(extractionShares != null ? extractionShares : Collections.emptyMap());
        this.agreedYear = agreedYear;
        this.agreedMonth = agreedMonth;
    }

    public String getAgreementId() { return agreementId; }
    public String getAnomalyId() { return anomalyId; }
    public String getContestId() { return contestId; }
    public List<String> getParticipatingCoalitions() { return participatingCoalitions; }
    public Map<String, Double> getExtractionShares() { return extractionShares; }
    public int getAgreedYear() { return agreedYear; }
    public int getAgreedMonth() { return agreedMonth; }

    public double getShareForCoalition(String coalitionId) {
        return extractionShares.getOrDefault(coalitionId, 0.0);
    }

    @Override
    public String toString() {
        return String.format("JointExtractionAgreement[%s on %s: %s at Y%d:M%d]",
                agreementId, anomalyId, extractionShares, agreedYear, agreedMonth);
    }
}
