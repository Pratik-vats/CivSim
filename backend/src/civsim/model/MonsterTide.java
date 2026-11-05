package civsim.model;

import java.util.*;

/**
 * Phase 2B.5 — Authoritative model of an active multi-wave MonsterTide invasion force.
 */
public class MonsterTide {

    private final String tideId;
    private final String sourceLairId;
    private final int targetVillageId;
    private final String targetVillageName;
    private final double targetX;
    private final double targetY;

    private MonsterTideState state;
    private final int mobilizedYear;
    private final int mobilizedMonth;
    private final long mobilizedTotalMonths;

    private final int arrivalYear;
    private final int arrivalMonth;
    private final long arrivalTotalMonths;
    private final int travelDurationMonths;

    private final List<String> wave1MonsterIds = new ArrayList<>();
    private final List<String> wave2MonsterIds = new ArrayList<>();
    private final List<String> wave3MonsterIds = new ArrayList<>();

    private boolean seerDetected;
    private int warningLeadMonths;
    private MonsterTideOutcome outcome;

    // Aftermath & Casualties
    private final List<String> survivingRetreatingMonsterIds = new ArrayList<>();
    private final List<String> slainMonsterIds = new ArrayList<>();
    private int defenderFatalities;
    private int civilianFatalities;
    private double structuralDamagePercent;
    private double foodDestroyed;

    public MonsterTide(
            String tideId,
            String sourceLairId,
            int targetVillageId,
            String targetVillageName,
            double targetX,
            double targetY,
            int mobilizedYear,
            int mobilizedMonth,
            long mobilizedTotalMonths,
            int travelDurationMonths,
            List<String> wave1,
            List<String> wave2,
            List<String> wave3) {

        this.tideId = tideId;
        this.sourceLairId = sourceLairId;
        this.targetVillageId = targetVillageId;
        this.targetVillageName = targetVillageName != null ? targetVillageName : ("Settlement_" + targetVillageId);
        this.targetX = targetX;
        this.targetY = targetY;

        this.state = MonsterTideState.APPROACHING;
        this.mobilizedYear = mobilizedYear;
        this.mobilizedMonth = mobilizedMonth;
        this.mobilizedTotalMonths = mobilizedTotalMonths;
        this.travelDurationMonths = Math.max(1, travelDurationMonths);

        this.arrivalTotalMonths = mobilizedTotalMonths + this.travelDurationMonths;
        this.arrivalYear = (int) (arrivalTotalMonths / 12);
        this.arrivalMonth = (int) (arrivalTotalMonths % 12);

        if (wave1 != null) this.wave1MonsterIds.addAll(wave1);
        if (wave2 != null) this.wave2MonsterIds.addAll(wave2);
        if (wave3 != null) this.wave3MonsterIds.addAll(wave3);

        this.seerDetected = false;
        this.warningLeadMonths = 0;
        this.outcome = null;
        this.defenderFatalities = 0;
        this.civilianFatalities = 0;
        this.structuralDamagePercent = 0.0;
        this.foodDestroyed = 0.0;
    }

    public String getTideId() { return tideId; }
    public String getSourceLairId() { return sourceLairId; }
    public int getTargetVillageId() { return targetVillageId; }
    public String getTargetVillageName() { return targetVillageName; }
    public double getTargetX() { return targetX; }
    public double getTargetY() { return targetY; }

    public MonsterTideState getState() { return state; }
    public void setState(MonsterTideState state) { this.state = state; }

    public int getMobilizedYear() { return mobilizedYear; }
    public int getMobilizedMonth() { return mobilizedMonth; }
    public long getMobilizedTotalMonths() { return mobilizedTotalMonths; }

    public int getArrivalYear() { return arrivalYear; }
    public int getArrivalMonth() { return arrivalMonth; }
    public long getArrivalTotalMonths() { return arrivalTotalMonths; }
    public int getTravelDurationMonths() { return travelDurationMonths; }

    public List<String> getWave1MonsterIds() { return Collections.unmodifiableList(wave1MonsterIds); }
    public List<String> getWave2MonsterIds() { return Collections.unmodifiableList(wave2MonsterIds); }
    public List<String> getWave3MonsterIds() { return Collections.unmodifiableList(wave3MonsterIds); }

    public List<String> getAllMobilizedMonsterIds() {
        List<String> all = new ArrayList<>();
        all.addAll(wave1MonsterIds);
        all.addAll(wave2MonsterIds);
        all.addAll(wave3MonsterIds);
        return all;
    }

    public int getTotalMobilizedCount() {
        return wave1MonsterIds.size() + wave2MonsterIds.size() + wave3MonsterIds.size();
    }

    public boolean isSeerDetected() { return seerDetected; }
    public int getWarningLeadMonths() { return warningLeadMonths; }
    public void markDetectedBySeer(int leadMonths) {
        this.seerDetected = true;
        this.warningLeadMonths = Math.max(1, leadMonths);
        this.state = MonsterTideState.DETECTED;
    }

    public MonsterTideOutcome getOutcome() { return outcome; }
    public void setOutcome(MonsterTideOutcome outcome) { this.outcome = outcome; }

    public List<String> getSurvivingRetreatingMonsterIds() { return Collections.unmodifiableList(survivingRetreatingMonsterIds); }
    public void addSurvivingRetreatingMonster(String mId) {
        if (mId != null && !survivingRetreatingMonsterIds.contains(mId)) {
            survivingRetreatingMonsterIds.add(mId);
        }
    }

    public List<String> getSlainMonsterIds() { return Collections.unmodifiableList(slainMonsterIds); }
    public void addSlainMonster(String mId) {
        if (mId != null && !slainMonsterIds.contains(mId)) {
            slainMonsterIds.add(mId);
        }
    }

    public int getDefenderFatalities() { return defenderFatalities; }
    public void setDefenderFatalities(int count) { this.defenderFatalities = count; }

    public int getCivilianFatalities() { return civilianFatalities; }
    public void setCivilianFatalities(int count) { this.civilianFatalities = count; }

    public double getStructuralDamagePercent() { return structuralDamagePercent; }
    public void setStructuralDamagePercent(double pct) { this.structuralDamagePercent = pct; }

    public double getFoodDestroyed() { return foodDestroyed; }
    public void setFoodDestroyed(double food) { this.foodDestroyed = food; }

    @Override
    public String toString() {
        return String.format("MonsterTide[%s from %s -> %s (dist ETA: M%d) State:%s Monsters:%d Out:%s]",
                tideId, sourceLairId, targetVillageName, arrivalTotalMonths, state.getDisplayName(),
                getTotalMobilizedCount(), outcome != null ? outcome.getDisplayName() : "Pending");
    }
}

