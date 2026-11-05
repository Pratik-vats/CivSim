package civsim.model;

import java.util.*;

/**
 * Phase 2C.2 — Persistent representation of an ongoing social, family, or clan conflict.
 * Contains one or more related historical grievances and manages the feud lifecycle, escalation,
 * active operations, and anti-loop cooldowns.
 */
public class FeudCase {
    private final String feudId;
    private final String feudName;
    private final String originGrievanceId;
    private final List<String> relatedGrievanceIds = new ArrayList<>();

    private final int originTotalMonths;
    private int lastActivityTotalMonths;
    private int nextEvaluationTotalMonths;
    private int lastActionTotalMonths = -1;
    private int lastRetaliationTotalMonths = -1;
    private int lastEscalationTotalMonths = -1;
    private int lastFailedRetaliationMonth = -100;
    private int cooldownUntilMonth = -100;

    private FeudEscalationLevel currentEscalationLevel;
    private double currentSeverity;
    private FeudStatus currentFeudStatus;

    private String initiatingSideId;
    private final List<String> opposingSideIds = new ArrayList<>();
    private String initiatingOrgId;
    private String opposingOrgId;
    private Integer initiatingFamilyId;
    private Integer opposingFamilyId;

    private final Set<String> responsiblePersonIds = new LinkedHashSet<>();
    private final Set<String> claimantPersonIds = new LinkedHashSet<>();
    private final List<String> inheritedClaimIds = new ArrayList<>();
    private final List<FeudActionRecord> actionHistory = new ArrayList<>();

    private String activeOperationId;
    private String activeAgreementId;
    private String feudType; // PERSONAL, FAMILY, CLAN, MIXED

    private int originGeneration = 0;
    private int currentClaimantGeneration = 0;

    public FeudCase(
            String feudId,
            String feudName,
            String originGrievanceId,
            int originTotalMonths,
            double initialSeverity,
            FeudStatus initialStatus,
            String initiatingSideId,
            List<String> opposingSideIds,
            String initiatingOrgId,
            String opposingOrgId,
            Integer initiatingFamilyId,
            Integer opposingFamilyId,
            String feudType) {
        this.feudId = feudId;
        this.feudName = feudName != null ? feudName : feudId;
        this.originGrievanceId = originGrievanceId;
        this.originTotalMonths = originTotalMonths;
        this.lastActivityTotalMonths = originTotalMonths;
        this.nextEvaluationTotalMonths = originTotalMonths + 3;
        this.currentSeverity = Math.max(0.0, Math.min(100.0, initialSeverity));
        this.currentFeudStatus = initialStatus != null ? initialStatus : FeudStatus.OPEN;
        this.currentEscalationLevel = FeudEscalationLevel.LEVEL_0_UNRESOLVED_GRIEVANCE;

        this.initiatingSideId = initiatingSideId;
        if (opposingSideIds != null) {
            this.opposingSideIds.addAll(opposingSideIds);
        }
        this.initiatingOrgId = initiatingOrgId;
        this.opposingOrgId = opposingOrgId;
        this.initiatingFamilyId = initiatingFamilyId;
        this.opposingFamilyId = opposingFamilyId;
        this.feudType = feudType != null ? feudType : "PERSONAL";

        if (originGrievanceId != null) {
            this.relatedGrievanceIds.add(originGrievanceId);
        }
    }

    public String getFeudId() { return feudId; }
    public String getFeudName() { return feudName; }
    public String getOriginGrievanceId() { return originGrievanceId; }
    public List<String> getRelatedGrievanceIds() { return Collections.unmodifiableList(relatedGrievanceIds); }
    public int getOriginTotalMonths() { return originTotalMonths; }
    public int getLastActivityTotalMonths() { return lastActivityTotalMonths; }
    public int getNextEvaluationTotalMonths() { return nextEvaluationTotalMonths; }
    public int getLastActionTotalMonths() { return lastActionTotalMonths; }
    public int getLastRetaliationTotalMonths() { return lastRetaliationTotalMonths; }
    public int getLastEscalationTotalMonths() { return lastEscalationTotalMonths; }
    public int getLastFailedRetaliationMonth() { return lastFailedRetaliationMonth; }
    public int getCooldownUntilMonth() { return cooldownUntilMonth; }

    public FeudEscalationLevel getCurrentEscalationLevel() { return currentEscalationLevel; }
    public double getCurrentSeverity() { return currentSeverity; }
    public FeudStatus getCurrentFeudStatus() { return currentFeudStatus; }

    public String getInitiatingSideId() { return initiatingSideId; }
    public List<String> getOpposingSideIds() { return Collections.unmodifiableList(opposingSideIds); }
    public String getInitiatingOrgId() { return initiatingOrgId; }
    public String getOpposingOrgId() { return opposingOrgId; }
    public Integer getInitiatingFamilyId() { return initiatingFamilyId; }
    public Integer getOpposingFamilyId() { return opposingFamilyId; }

    public Set<String> getResponsiblePersonIds() { return Collections.unmodifiableSet(responsiblePersonIds); }
    public Set<String> getClaimantPersonIds() { return Collections.unmodifiableSet(claimantPersonIds); }
    public List<String> getInheritedClaimIds() { return Collections.unmodifiableList(inheritedClaimIds); }
    public List<FeudActionRecord> getActionHistory() { return Collections.unmodifiableList(actionHistory); }

    public String getActiveOperationId() { return activeOperationId; }
    public String getActiveAgreementId() { return activeAgreementId; }
    public String getFeudType() { return feudType; }
    public int getOriginGeneration() { return originGeneration; }
    public int getCurrentClaimantGeneration() { return currentClaimantGeneration; }

    public void setInitiatingSideId(String id) { this.initiatingSideId = id; }
    public void setInitiatingOrgId(String orgId) { this.initiatingOrgId = orgId; }
    public void setOpposingOrgId(String orgId) { this.opposingOrgId = orgId; }
    public void setFeudType(String feudType) { this.feudType = feudType; }
    public void setOriginGeneration(int gen) { this.originGeneration = gen; }
    public void setCurrentClaimantGeneration(int gen) { this.currentClaimantGeneration = gen; }
    public void setNextEvaluationTotalMonths(int months) { this.nextEvaluationTotalMonths = months; }
    public void setActiveOperationId(String opId) { this.activeOperationId = opId; }
    public void setActiveAgreementId(String agrId) { this.activeAgreementId = agrId; }
    public void setCurrentEscalationLevel(FeudEscalationLevel level) { if (level != null) this.currentEscalationLevel = level; }
    public void setCurrentSeverity(double s) { this.currentSeverity = Math.max(0.0, Math.min(100.0, s)); }
    public void setCurrentFeudStatus(FeudStatus st) { if (st != null) this.currentFeudStatus = st; }

    public void addResponsiblePerson(String personId) {
        if (personId != null) responsiblePersonIds.add(personId);
    }

    public void addClaimantPerson(String personId) {
        if (personId != null) claimantPersonIds.add(personId);
    }

    public void removeClaimantPerson(String personId) {
        if (personId != null) claimantPersonIds.remove(personId);
    }

    public void addInheritedClaimId(String claimId) {
        if (claimId != null && !inheritedClaimIds.contains(claimId)) {
            inheritedClaimIds.add(claimId);
        }
    }

    public void addRelatedGrievance(String grievanceId, double severityContribution, int totalMonths) {
        if (grievanceId != null && !relatedGrievanceIds.contains(grievanceId)) {
            relatedGrievanceIds.add(grievanceId);
            this.currentSeverity = Math.min(100.0, this.currentSeverity + severityContribution * 0.5);
            this.lastActivityTotalMonths = totalMonths;
        }
    }

    public void addAction(FeudActionRecord action) {
        if (action != null) {
            actionHistory.add(action);
            this.lastActionTotalMonths = action.getSimulationYear() * 12 + action.getSimulationMonth();
            this.lastActivityTotalMonths = this.lastActionTotalMonths;
            if (action.getActionType().isHostile()) {
                this.lastRetaliationTotalMonths = this.lastActionTotalMonths;
            }
        }
    }

    public boolean isCooldownActive(int currentTotalMonths) {
        return currentTotalMonths < cooldownUntilMonth;
    }

    public boolean isRetryCooldownActive(int currentTotalMonths) {
        return (currentTotalMonths - lastFailedRetaliationMonth) < 12;
    }

    public void recordMajorAction(int currentTotalMonths) {
        this.cooldownUntilMonth = currentTotalMonths + 6; // 6-month cooldown after major action
        this.lastActionTotalMonths = currentTotalMonths;
        this.lastActivityTotalMonths = currentTotalMonths;
    }

    public void recordFailedRetaliation(int currentTotalMonths) {
        this.lastFailedRetaliationMonth = currentTotalMonths;
        this.cooldownUntilMonth = currentTotalMonths + 6;
        this.activeOperationId = null;
    }

    public void recordSuccessRetaliation(int currentTotalMonths) {
        this.cooldownUntilMonth = currentTotalMonths + 6;
        this.activeOperationId = null;
    }

    public boolean canTransitionTo(FeudStatus targetStatus) {
        if (this.currentFeudStatus == targetStatus) return true;
        if (this.currentFeudStatus.isTerminal()) return false; // Settled, expired, terminated cannot mutate directly

        switch (this.currentFeudStatus) {
            case PENDING_ATTRIBUTION:
                return targetStatus == FeudStatus.OPEN || targetStatus == FeudStatus.DORMANT;
            case OPEN:
                return targetStatus == FeudStatus.DIPLOMATIC_DISPUTE
                        || targetStatus == FeudStatus.ESCALATING
                        || targetStatus == FeudStatus.TRUCE
                        || targetStatus == FeudStatus.DORMANT
                        || targetStatus == FeudStatus.SETTLED;
            case DIPLOMATIC_DISPUTE:
                return targetStatus == FeudStatus.ESCALATING
                        || targetStatus == FeudStatus.NEGOTIATION
                        || targetStatus == FeudStatus.TRUCE
                        || targetStatus == FeudStatus.SETTLED
                        || targetStatus == FeudStatus.DORMANT;
            case ESCALATING:
                return targetStatus == FeudStatus.ACTIVE_RETALIATION
                        || targetStatus == FeudStatus.NEGOTIATION
                        || targetStatus == FeudStatus.TRUCE
                        || targetStatus == FeudStatus.DORMANT
                        || targetStatus == FeudStatus.SETTLED;
            case ACTIVE_RETALIATION:
                return targetStatus == FeudStatus.NEGOTIATION
                        || targetStatus == FeudStatus.ESCALATING
                        || targetStatus == FeudStatus.OPEN
                        || targetStatus == FeudStatus.DORMANT
                        || targetStatus == FeudStatus.SETTLED;
            case NEGOTIATION:
                return targetStatus == FeudStatus.TRUCE
                        || targetStatus == FeudStatus.SETTLED
                        || targetStatus == FeudStatus.ESCALATING
                        || targetStatus == FeudStatus.DORMANT;
            case TRUCE:
                return targetStatus == FeudStatus.ESCALATING // Truce violation reopens hostilities
                        || targetStatus == FeudStatus.SETTLED
                        || targetStatus == FeudStatus.EXPIRED
                        || targetStatus == FeudStatus.DORMANT;
            case DORMANT:
                return targetStatus == FeudStatus.OPEN // Reactivated by new evidence or inheriting claimant
                        || targetStatus == FeudStatus.EXPIRED
                        || targetStatus == FeudStatus.TERMINATED;
            default:
                return false;
        }
    }

    public boolean transitionStatus(FeudStatus targetStatus, int totalMonths, String reason) {
        if (!canTransitionTo(targetStatus)) {
            return false;
        }
        this.currentFeudStatus = targetStatus;
        this.lastActivityTotalMonths = totalMonths;
        return true;
    }

    public void setEscalationLevel(FeudEscalationLevel level, int totalMonths) {
        if (level != null && level != this.currentEscalationLevel) {
            this.currentEscalationLevel = level;
            this.lastEscalationTotalMonths = totalMonths;
            this.lastActivityTotalMonths = totalMonths;
        }
    }

    @Override
    public String toString() {
        return String.format("FeudCase[%s: %s (Status:%s, Lvl:%s, Sev:%.1f)]",
                feudId, feudName, currentFeudStatus, currentEscalationLevel.getShortName(), currentSeverity);
    }
}
