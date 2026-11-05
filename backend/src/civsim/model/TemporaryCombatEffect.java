package civsim.model;

/**
 * Phase 2B.1 — Temporary, round-based combat effect, barrier, or mark active within a battle encounter.
 */
public class TemporaryCombatEffect {

    public enum EffectType {
        WARDEN_BARRIER,
        SEER_INSIGHT,
        VANGUARD_REINFORCEMENT,
        RIFTWALKER_BLINK,
        BEAST_COORDINATION,
        DEFENSIVE_STANCE
    }

    private final EffectType type;
    private int remainingRounds;
    private double magnitude;
    private final String targetCombatantId;

    public TemporaryCombatEffect(EffectType type, int remainingRounds, double magnitude, String targetCombatantId) {
        this.type = type;
        this.remainingRounds = remainingRounds;
        this.magnitude = magnitude;
        this.targetCombatantId = targetCombatantId;
    }

    public TemporaryCombatEffect(EffectType type, int remainingRounds, double magnitude) {
        this(type, remainingRounds, magnitude, null);
    }

    public TemporaryCombatEffect(EffectType type, double magnitude, int remainingRounds) {
        this(type, remainingRounds, magnitude, null);
    }

    public EffectType getType() { return type; }
    public int getRemainingRounds() { return remainingRounds; }
    public double getMagnitude() { return magnitude; }
    public String getTargetCombatantId() { return targetCombatantId; }

    public boolean isExpired() {
        return remainingRounds <= 0;
    }

    public void decrementRound() {
        if (remainingRounds > 0) {
            remainingRounds--;
        }
    }

    public void deductMagnitude(double amount) {
        this.magnitude = Math.max(0.0, this.magnitude - amount);
    }

    public void setMagnitude(double magnitude) {
        this.magnitude = magnitude;
    }

    public void refresh(int rounds, double newMagnitude) {
        this.remainingRounds = rounds;
        this.magnitude = Math.max(this.magnitude, newMagnitude);
    }
}
