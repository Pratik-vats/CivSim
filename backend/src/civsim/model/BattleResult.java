package civsim.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase 2B.1 — Immutable combat outcome record returned by the tactical combat engine.
 */
public final class BattleResult {
    private final String battleId;
    private final int simulationYear;
    private final int simulationMonth;
    private final BattleType battleType;
    private final BattleOutcome outcome;
    private final String victoriousSideId;
    private final int totalRounds;
    private final Map<String, Integer> initialCountsPerSide;
    private final Map<String, Integer> finalLivingCountsPerSide;
    private final Map<String, Double> damageDealtPerSide;
    private final List<String> fatalities;
    private final List<String> incapacitated;
    private final List<String> escaped;
    private final int totalInjuriesInflicted;
    private final String summaryText;

    public BattleResult(String battleId, int simulationYear, int simulationMonth,
                        BattleType battleType, BattleOutcome outcome, String victoriousSideId,
                        int totalRounds,
                        Map<String, Integer> initialCountsPerSide,
                        Map<String, Integer> finalLivingCountsPerSide,
                        Map<String, Double> damageDealtPerSide,
                        List<String> fatalities,
                        List<String> incapacitated,
                        List<String> escaped,
                        int totalInjuriesInflicted,
                        String summaryText) {
        this.battleId = battleId;
        this.simulationYear = simulationYear;
        this.simulationMonth = simulationMonth;
        this.battleType = battleType;
        this.outcome = outcome;
        this.victoriousSideId = victoriousSideId;
        this.totalRounds = totalRounds;
        this.initialCountsPerSide = (initialCountsPerSide != null) ? Collections.unmodifiableMap(new HashMap<>(initialCountsPerSide)) : Collections.emptyMap();
        this.finalLivingCountsPerSide = (finalLivingCountsPerSide != null) ? Collections.unmodifiableMap(new HashMap<>(finalLivingCountsPerSide)) : Collections.emptyMap();
        this.damageDealtPerSide = (damageDealtPerSide != null) ? Collections.unmodifiableMap(new HashMap<>(damageDealtPerSide)) : Collections.emptyMap();
        this.fatalities = (fatalities != null) ? Collections.unmodifiableList(fatalities) : Collections.emptyList();
        this.incapacitated = (incapacitated != null) ? Collections.unmodifiableList(incapacitated) : Collections.emptyList();
        this.escaped = (escaped != null) ? Collections.unmodifiableList(escaped) : Collections.emptyList();
        this.totalInjuriesInflicted = totalInjuriesInflicted;
        this.summaryText = summaryText != null ? summaryText : "";
    }

    public String getBattleId() { return battleId; }
    public int getSimulationYear() { return simulationYear; }
    public int getSimulationMonth() { return simulationMonth; }
    public BattleType getBattleType() { return battleType; }
    public BattleOutcome getOutcome() { return outcome; }
    public String getVictoriousSideId() { return victoriousSideId; }
    public int getTotalRounds() { return totalRounds; }
    public Map<String, Integer> getInitialCountsPerSide() { return initialCountsPerSide; }
    public Map<String, Integer> getFinalLivingCountsPerSide() { return finalLivingCountsPerSide; }
    public Map<String, Double> getDamageDealtPerSide() { return damageDealtPerSide; }
    public List<String> getFatalities() { return fatalities; }
    public List<String> getIncapacitated() { return incapacitated; }
    public List<String> getEscaped() { return escaped; }
    public int getTotalInjuriesInflicted() { return totalInjuriesInflicted; }
    public String getSummaryText() { return summaryText; }

    // Convenience record-style accessor aliases
    public String battleId() { return battleId; }
    public int simulationYear() { return simulationYear; }
    public int simulationMonth() { return simulationMonth; }
    public BattleType battleType() { return battleType; }
    public BattleOutcome outcome() { return outcome; }
    public String victoriousSideId() { return victoriousSideId; }
    public int totalRounds() { return totalRounds; }
    public Map<String, Integer> initialCountsPerSide() { return initialCountsPerSide; }
    public Map<String, Integer> finalLivingCountsPerSide() { return finalLivingCountsPerSide; }
    public Map<String, Integer> survivorCountsBySide() { return finalLivingCountsPerSide; }
    public Map<String, Double> damageDealtPerSide() { return damageDealtPerSide; }
    public Map<String, Double> totalDamageDealt() { return damageDealtPerSide; }
    public List<String> fatalities() { return fatalities; }
    public List<String> incapacitated() { return incapacitated; }
    public List<String> escaped() { return escaped; }
    public int totalEscaped() { return escaped.size(); }
    public int totalInjuriesInflicted() { return totalInjuriesInflicted; }
    public String summaryText() { return summaryText; }

    @Override
    public String toString() {
        return String.format("BattleResult[%s (%s Y%d M%d): %s (Winner: %s) after %d rds | Deaths:%d Incap:%d Esc:%d Inj:%d]",
                battleId, battleType.getDisplayName(), simulationYear, simulationMonth,
                outcome.getDisplayName(), victoriousSideId != null ? victoriousSideId : "None",
                totalRounds, fatalities.size(), incapacitated.size(), escaped.size(), totalInjuriesInflicted);
    }
}
