package civsim.model;

import java.util.*;

/**
 * Phase 2B.4, 2C.1 & 2C.2 — Structured, immutable historical record of a grievance.
 * Serves as an unalterable historical fact. Grievance records are never rewritten or erased.
 */
public final class HistoricalGrievanceRecord {
    private final String grievanceId;
    private final String battleId;
    private final String contestId;
    private final String anomalyId;
    private final int simulationYear;
    private final int simulationMonth;

    private final String victimPersonId;
    private final int victimFamilyId;
    private final String victimPrimaryOrganizationId;
    private final Integer victimSettlementId;

    private final String killerPersonId;
    private final String killerCoalitionSideId;
    private final List<String> responsibleOrganizationIds;
    private final String description;
    private final String grievanceType;

    // Phase 2C.2 Extensions
    private final GrievanceCategory category;
    private final AttributionConfidence confidence;
    private final double severity;
    private final String directPerpetratorId;
    private final String commandLeaderId;
    private final String witnessOrEvidence;
    private final boolean isDeliberate;
    private final ResponsibilityType responsibilityType;
    private String feudId; // Set when linked to a FeudCase

    public HistoricalGrievanceRecord(
            String grievanceId,
            String battleId,
            String contestId,
            String anomalyId,
            int simulationYear,
            int simulationMonth,
            String victimPersonId,
            int victimFamilyId,
            String victimPrimaryOrganizationId,
            Integer victimSettlementId,
            String killerPersonId,
            String killerCoalitionSideId,
            List<String> responsibleOrganizationIds,
            String description) {
        this(grievanceId, battleId, contestId, anomalyId, simulationYear, simulationMonth,
                victimPersonId, victimFamilyId, victimPrimaryOrganizationId, victimSettlementId,
                killerPersonId, killerCoalitionSideId, responsibleOrganizationIds, description, "FATALITY_IN_COMBAT");
    }

    public HistoricalGrievanceRecord(
            String grievanceId,
            String battleId,
            String contestId,
            String anomalyId,
            int simulationYear,
            int simulationMonth,
            String victimPersonId,
            int victimFamilyId,
            String victimPrimaryOrganizationId,
            Integer victimSettlementId,
            String killerPersonId,
            String killerCoalitionSideId,
            List<String> responsibleOrganizationIds,
            String description,
            String grievanceType) {
        this(grievanceId, battleId, contestId, anomalyId, simulationYear, simulationMonth,
                victimPersonId, victimFamilyId, victimPrimaryOrganizationId, victimSettlementId,
                killerPersonId, killerCoalitionSideId, responsibleOrganizationIds, description, grievanceType,
                mapLegacyCategory(grievanceType),
                mapLegacyConfidence(killerPersonId, killerCoalitionSideId, grievanceType),
                mapLegacySeverity(grievanceType),
                killerPersonId,
                null,
                battleId != null ? "BATTLE:" + battleId : "EYEWITNESS_REPORT",
                "HEIRLOOM_OR_EQUIPMENT_CAPTURED".equalsIgnoreCase(grievanceType),
                killerPersonId != null ? ResponsibilityType.DIRECT_PERPETRATOR : ResponsibilityType.INSTITUTIONAL
        );
    }

    public HistoricalGrievanceRecord(
            String grievanceId,
            String battleId,
            String contestId,
            String anomalyId,
            int simulationYear,
            int simulationMonth,
            String victimPersonId,
            int victimFamilyId,
            String victimPrimaryOrganizationId,
            Integer victimSettlementId,
            String killerPersonId,
            String killerCoalitionSideId,
            List<String> responsibleOrganizationIds,
            String description,
            String grievanceType,
            GrievanceCategory category,
            AttributionConfidence confidence,
            double severity,
            String directPerpetratorId,
            String commandLeaderId,
            String witnessOrEvidence,
            boolean isDeliberate,
            ResponsibilityType responsibilityType) {
        this.grievanceId = grievanceId;
        this.battleId = battleId;
        this.contestId = contestId;
        this.anomalyId = anomalyId;
        this.simulationYear = simulationYear;
        this.simulationMonth = simulationMonth;
        this.victimPersonId = victimPersonId;
        this.victimFamilyId = victimFamilyId;
        this.victimPrimaryOrganizationId = victimPrimaryOrganizationId;
        this.victimSettlementId = victimSettlementId;
        this.killerPersonId = killerPersonId;
        this.killerCoalitionSideId = killerCoalitionSideId;
        this.responsibleOrganizationIds = List.copyOf(responsibleOrganizationIds != null ? responsibleOrganizationIds : Collections.emptyList());
        this.description = description != null ? description : "Combat grievance";
        this.grievanceType = grievanceType != null ? grievanceType : "FATALITY_IN_COMBAT";

        this.category = category != null ? category : mapLegacyCategory(this.grievanceType);
        this.confidence = confidence != null ? confidence : mapLegacyConfidence(killerPersonId, killerCoalitionSideId, this.grievanceType);
        this.severity = Math.max(0.0, Math.min(100.0, severity > 0.0 ? severity : this.category.getDefaultSeverity()));
        this.directPerpetratorId = directPerpetratorId != null ? directPerpetratorId : killerPersonId;
        this.commandLeaderId = commandLeaderId;
        this.witnessOrEvidence = witnessOrEvidence != null ? witnessOrEvidence : "HISTORICAL_RECORD";
        this.isDeliberate = isDeliberate;
        this.responsibilityType = responsibilityType != null ? responsibilityType :
                (directPerpetratorId != null ? ResponsibilityType.DIRECT_PERPETRATOR : ResponsibilityType.INSTITUTIONAL);
        this.feudId = null;
    }

    private static GrievanceCategory mapLegacyCategory(String type) {
        if ("HEIRLOOM_OR_EQUIPMENT_CAPTURED".equalsIgnoreCase(type)) {
            return GrievanceCategory.HEIRLOOM_CAPTURED;
        }
        return GrievanceCategory.PERSONAL_MURDER;
    }

    private static AttributionConfidence mapLegacyConfidence(String killerPersonId, String sideId, String type) {
        if ("HEIRLOOM_OR_EQUIPMENT_CAPTURED".equalsIgnoreCase(type)) {
            return AttributionConfidence.CONFIRMED;
        }
        if (killerPersonId != null && !killerPersonId.isBlank()) {
            return AttributionConfidence.CONFIRMED;
        }
        if (sideId != null && !sideId.isBlank()) {
            return AttributionConfidence.STRONGLY_SUPPORTED;
        }
        return AttributionConfidence.UNKNOWN;
    }

    private static double mapLegacySeverity(String type) {
        if ("HEIRLOOM_OR_EQUIPMENT_CAPTURED".equalsIgnoreCase(type)) {
            return 55.0;
        }
        return 65.0;
    }

    public String getGrievanceId() { return grievanceId; }
    public String getBattleId() { return battleId; }
    public String getContestId() { return contestId; }
    public String getAnomalyId() { return anomalyId; }
    public int getSimulationYear() { return simulationYear; }
    public int getSimulationMonth() { return simulationMonth; }

    public String getVictimPersonId() { return victimPersonId; }
    public int getVictimFamilyId() { return victimFamilyId; }
    public String getVictimPrimaryOrganizationId() { return victimPrimaryOrganizationId; }
    public Integer getVictimSettlementId() { return victimSettlementId; }

    public String getKillerPersonId() { return killerPersonId; }
    public String getKillerCoalitionSideId() { return killerCoalitionSideId; }
    public List<String> getResponsibleOrganizationIds() { return responsibleOrganizationIds; }
    public String getDescription() { return description; }
    public String getGrievanceType() { return grievanceType; }

    public GrievanceCategory getCategory() { return category; }
    public AttributionConfidence getConfidence() { return confidence; }
    public double getSeverity() { return severity; }
    public String getDirectPerpetratorId() { return directPerpetratorId; }
    public String getCommandLeaderId() { return commandLeaderId; }
    public String getWitnessOrEvidence() { return witnessOrEvidence; }
    public boolean isDeliberate() { return isDeliberate; }
    public ResponsibilityType getResponsibilityType() { return responsibilityType; }

    public String getFeudId() { return feudId; }
    public void setFeudId(String feudId) { this.feudId = feudId; }

    @Override
    public String toString() {
        return String.format("HistoricalGrievance[%s (%s, %s, Sev:%.1f): P%s (Fam%d, Org:%s) vs P%s (Side:%s) Feud:%s]",
                grievanceId, category, confidence, severity, victimPersonId, victimFamilyId, victimPrimaryOrganizationId,
                killerPersonId, killerCoalitionSideId, feudId);
    }
}
