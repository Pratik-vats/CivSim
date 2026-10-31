package civsim.model;

import java.util.*;

/**
 * Phase 2B.4 -- Authoritative record tracking a contested anomaly encounter through its complete lifecycle.
 */
public class AnomalyContestRecord {
    private final String contestId;
    private final String anomalyId;
    private final int encounterSequence;
    private ContestState state;
    private final int createdYear;
    private final int createdMonth;
    private final long createdTotalMonths;

    private final List<String> participatingExpeditionIds;
    private final List<String> coalitionSideIds;
    private String resolutionEventId;
    private long reengagementCooldownUntilMonth;
    private String battleId;
    private String winningCoalitionSideId;
    private String resolutionSummary;

    public AnomalyContestRecord(
            String contestId,
            String anomalyId,
            int encounterSequence,
            ContestState initialState,
            int createdYear,
            int createdMonth,
            long createdTotalMonths,
            List<String> participatingExpeditionIds) {
        this.contestId = contestId;
        this.anomalyId = anomalyId;
        this.encounterSequence = encounterSequence;
        this.state = initialState != null ? initialState : ContestState.PENDING_RESOLUTION;
        this.createdYear = createdYear;
        this.createdMonth = createdMonth;
        this.createdTotalMonths = createdTotalMonths;

        this.participatingExpeditionIds = new ArrayList<>(participatingExpeditionIds != null ? participatingExpeditionIds : Collections.emptyList());
        this.coalitionSideIds = new ArrayList<>();
        this.resolutionEventId = null;
        this.reengagementCooldownUntilMonth = 0L;
        this.battleId = null;
        this.winningCoalitionSideId = null;
        this.resolutionSummary = "Initialized contest";
    }

    public String getContestId() { return contestId; }
    public String getAnomalyId() { return anomalyId; }
    public int getEncounterSequence() { return encounterSequence; }

    public ContestState getState() { return state; }
    public void setState(ContestState state) { this.state = state; }

    public int getCreatedYear() { return createdYear; }
    public int getCreatedMonth() { return createdMonth; }
    public long getCreatedTotalMonths() { return createdTotalMonths; }

    public List<String> getParticipatingExpeditionIds() { return Collections.unmodifiableList(participatingExpeditionIds); }
    public void addParticipatingExpeditionId(String expId) {
        if (expId != null && !participatingExpeditionIds.contains(expId)) {
            participatingExpeditionIds.add(expId);
        }
    }

    public List<String> getCoalitionSideIds() { return Collections.unmodifiableList(coalitionSideIds); }
    public void setCoalitionSideIds(Collection<String> sides) {
        this.coalitionSideIds.clear();
        if (sides != null) {
            this.coalitionSideIds.addAll(sides);
        }
    }

    public String getResolutionEventId() { return resolutionEventId; }
    public void setResolutionEventId(String eventId) { this.resolutionEventId = eventId; }

    public long getReengagementCooldownUntilMonth() { return reengagementCooldownUntilMonth; }
    public void setReengagementCooldownUntilMonth(long month) { this.reengagementCooldownUntilMonth = month; }

    public String getBattleId() { return battleId; }
    public void setBattleId(String battleId) { this.battleId = battleId; }

    public String getWinningCoalitionSideId() { return winningCoalitionSideId; }
    public void setWinningCoalitionSideId(String sideId) { this.winningCoalitionSideId = sideId; }

    public String getResolutionSummary() { return resolutionSummary; }
    public void setResolutionSummary(String summary) { this.resolutionSummary = summary; }

    public void transitionState(ContestState newState, String summary) {
        this.state = newState;
        if (summary != null) {
            this.resolutionSummary = summary;
        }
    }

    @Override
    public String toString() {
        return String.format("AnomalyContestRecord[%s on %s (Seq %d): %s | Winner:%s Battle:%s]",
                contestId, anomalyId, encounterSequence, state.getDisplayName(), winningCoalitionSideId, battleId);
    }
}
