package civsim.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Phase 2B.1 — Persistent combat state attached to an entity.
 * Stores persistent current/max health, stamina, aether reserves, active injuries,
 * and encounter-scoped morale and temporary tactical conditions.
 */
public class CombatState {

    private double currentHealth;
    private double maxHealth;

    private double currentStamina;
    private double maxStamina;

    private double currentAether;
    private double maxAether;

    private double currentMorale; // bounded [0.0, 100.0]

    private boolean dead;
    private boolean incapacitated;
    private boolean escaped;
    private boolean normalEscapeAttemptedThisBattle;
    private boolean initializedInLife;

    private String lastKillerCombatantId;
    private String lastKillerPersonId;
    private String lastKillerSideId;

    private final List<CombatInjury> activeInjuries = new ArrayList<>();
    private final List<TemporaryCombatEffect> activeEffects = new ArrayList<>();
    private final Set<String> processedCombatEventIds = new HashSet<>();

    private Person ownerPerson;

    public CombatState() {
        this(null);
    }

    public CombatState(Person ownerPerson) {
        this.ownerPerson = ownerPerson;
        this.maxHealth = 40.0;
        this.currentHealth = 40.0;
        this.maxStamina = 20.0;
        this.currentStamina = 20.0;
        this.maxAether = 0.0;
        this.currentAether = 0.0;
        this.currentMorale = 50.0;
        this.dead = false;
        this.incapacitated = false;
        this.escaped = false;
        this.normalEscapeAttemptedThisBattle = false;
        this.initializedInLife = false;
    }

    public CombatState(double maxHealth, double maxStamina, double maxAether) {
        this(null);
        this.maxHealth = maxHealth;
        this.currentHealth = maxHealth;
        this.maxStamina = maxStamina;
        this.currentStamina = maxStamina;
        this.maxAether = maxAether;
        this.currentAether = maxAether;
        this.initializedInLife = true;
    }

    public boolean isExhausted() {
        return currentStamina <= 0.0;
    }

    public double getBarrierAbsorption() {
        TemporaryCombatEffect eff = getEffect(TemporaryCombatEffect.EffectType.WARDEN_BARRIER);
        return eff != null ? eff.getMagnitude() : 0.0;
    }

    public Person getOwnerPerson() { return ownerPerson; }
    public void setOwnerPerson(Person ownerPerson) { this.ownerPerson = ownerPerson; }

    /**
     * Recalculates maximum resource pools without refilling missing resources.
     */
    public void recalculateMaxPools(double effVitality, double effEndurance,
                                    double effAetherCapacity, double effAetherControl,
                                    boolean awakened) {
        this.maxHealth = Math.max(40.0, Math.min(300.0, 40.0 + (1.5 * effVitality) + (1.0 * effEndurance)));
        this.maxStamina = Math.max(20.0, Math.min(200.0, 20.0 + (1.2 * effEndurance) + (0.5 * effVitality)));

        if (awakened) {
            this.maxAether = Math.max(5.0, Math.min(250.0, 5.0 + (2.0 * effAetherCapacity) + (0.5 * effAetherControl)));
        } else {
            this.maxAether = 0.0;
        }

        // Clamp current values to new maximums without granting free refills
        this.currentHealth = Math.max(0.0, Math.min(maxHealth, currentHealth));
        this.currentStamina = Math.min(maxStamina, currentStamina);
        this.currentAether = Math.min(maxAether, currentAether);
    }

    /**
     * Initializes pools to full capacity. Should be called only once when the entity
     * is first created in life or first enters active service.
     */
    public void initializeFullPools(double effVitality, double effEndurance,
                                    double effAetherCapacity, double effAetherControl,
                                    boolean awakened) {
        recalculateMaxPools(effVitality, effEndurance, effAetherCapacity, effAetherControl, awakened);
        this.currentHealth = this.maxHealth;
        this.currentStamina = this.maxStamina;
        this.currentAether = this.maxAether;
        this.initializedInLife = true;
    }

    public boolean isInitializedInLife() {
        return initializedInLife;
    }

    /**
     * Initializes encounter-scoped morale based on effective willpower.
     * InitialMorale = clamp(50.0 + 0.5 * EffectiveWillpower, 35.0, 95.0)
     */
    public void initMoraleForBattle(double effectiveWillpower) {
        this.currentMorale = Math.max(35.0, Math.min(95.0, 50.0 + (0.5 * effectiveWillpower)));
        this.normalEscapeAttemptedThisBattle = false;
        this.escaped = false;
        this.incapacitated = (currentHealth <= 0.0);
    }

    // --- Damage, Healing & Resource Management ---

    public void takeDamage(double damage) {
        if (damage <= 0.0) return;
        this.currentHealth = Math.max(0.0, this.currentHealth - damage);
        if (this.currentHealth <= 0.0) {
            this.incapacitated = true;
        }
    }

    public void heal(double amount) {
        if (amount <= 0.0 || dead) return;
        this.currentHealth = Math.min(maxHealth, this.currentHealth + amount);
        if (this.currentHealth > 0.0 && !dead) {
            this.incapacitated = false;
        }
    }

    public void consumeStamina(double amount) {
        this.currentStamina -= amount;
    }

    public void restoreStamina(double amount) {
        if (amount <= 0.0) return;
        this.currentStamina = Math.min(maxStamina, this.currentStamina + amount);
    }

    public void consumeAether(double amount) {
        this.currentAether = Math.max(0.0, this.currentAether - amount);
    }

    public void restoreAether(double amount) {
        if (amount <= 0.0) return;
        this.currentAether = Math.min(maxAether, this.currentAether + amount);
    }

    public void modifyMorale(double delta) {
        this.currentMorale = Math.max(0.0, Math.min(100.0, this.currentMorale + delta));
    }

    // --- Temporary Effects Management ---

    public void addTemporaryEffect(TemporaryCombatEffect effect) {
        addEffect(effect);
    }

    public void addEffect(TemporaryCombatEffect effect) {
        if (effect == null) return;
        // Non-stacking logic for barrier and insights
        if (effect.getType() == TemporaryCombatEffect.EffectType.WARDEN_BARRIER) {
            TemporaryCombatEffect existing = getEffect(TemporaryCombatEffect.EffectType.WARDEN_BARRIER);
            if (existing != null) {
                if (effect.getMagnitude() >= existing.getMagnitude()) {
                    existing.refresh(effect.getRemainingRounds(), effect.getMagnitude());
                }
                return;
            }
        } else if (effect.getType() == TemporaryCombatEffect.EffectType.SEER_INSIGHT) {
            TemporaryCombatEffect existing = getEffect(TemporaryCombatEffect.EffectType.SEER_INSIGHT);
            if (existing != null) {
                existing.refresh(effect.getRemainingRounds(), effect.getMagnitude());
                return;
            }
        }
        activeEffects.add(effect);
    }

    public TemporaryCombatEffect getEffect(TemporaryCombatEffect.EffectType type) {
        for (TemporaryCombatEffect eff : activeEffects) {
            if (eff.getType() == type && !eff.isExpired()) {
                return eff;
            }
        }
        return null;
    }

    public boolean hasEffect(TemporaryCombatEffect.EffectType type) {
        return getEffect(type) != null;
    }

    public void tickEffects() {
        for (TemporaryCombatEffect eff : activeEffects) {
            eff.decrementRound();
        }
        activeEffects.removeIf(TemporaryCombatEffect::isExpired);
    }

    public void clearEncounterEffects() {
        activeEffects.clear();
        normalEscapeAttemptedThisBattle = false;
        escaped = false;
        if (currentHealth > 0.0 && !dead) {
            incapacitated = false;
        }
    }

    // --- Persistent Injuries ---

    public void addInjury(CombatInjury injury) {
        if (injury == null) return;
        activeInjuries.add(injury);
        if (ownerPerson != null && ownerPerson.getAetherProfile() != null) {
            ownerPerson.getAetherProfile().applyCombatInjuryPenalty(injury);
        }
    }

    public void removeInjury(CombatInjury injury) {
        if (injury == null) return;
        activeInjuries.remove(injury);
        if (ownerPerson != null && ownerPerson.getAetherProfile() != null) {
            ownerPerson.getAetherProfile().removeCombatInjuryPenalty(injury);
        }
    }

    public void clearExpiredInjuries(long currentTotalMonths) {
        List<CombatInjury> expired = new ArrayList<>();
        for (CombatInjury inj : activeInjuries) {
            if (inj.isExpired(currentTotalMonths)) {
                expired.add(inj);
            }
        }
        for (CombatInjury inj : expired) {
            removeInjury(inj);
        }
    }

    public boolean hasSevereInjury() {
        for (CombatInjury injury : activeInjuries) {
            if (injury.getSeverity() == CombatInjurySeverity.SEVERE_INJURY) {
                return true;
            }
        }
        return false;
    }

    public double getTotalInjuryPenalty(AttributeType attr) {
        double penalty = 0.0;
        for (CombatInjury injury : activeInjuries) {
            penalty += injury.getAttributePenalty(attr);
        }
        return penalty;
    }

    public boolean isTrainingBlocked() {
        return hasSevereInjury();
    }

    public boolean markEventProcessed(String eventId) {
        return processedCombatEventIds.add(eventId);
    }

    public boolean hasProcessedEvent(String eventId) {
        return processedCombatEventIds.contains(eventId);
    }

    // --- Getters & Setters ---

    public double getCurrentHealth() { return currentHealth; }
    public void setCurrentHealth(double health) {
        this.currentHealth = Math.max(0.0, Math.min(maxHealth, health));
        if (this.currentHealth <= 0.0) {
            this.incapacitated = true;
        }
    }

    public double getMaxHealth() { return maxHealth; }
    public double getCurrentStamina() { return currentStamina; }
    public void setCurrentStamina(double stamina) { this.currentStamina = Math.min(maxStamina, stamina); }
    public double getMaxStamina() { return maxStamina; }

    public double getCurrentAether() { return currentAether; }
    public void setCurrentAether(double aether) { this.currentAether = Math.max(0.0, Math.min(maxAether, aether)); }
    public double getMaxAether() { return maxAether; }

    public double getCurrentMorale() { return currentMorale; }
    public void setCurrentMorale(double morale) { this.currentMorale = Math.max(0.0, Math.min(100.0, morale)); }

    public boolean isDead() { return dead; }
    public void setDead(boolean dead) {
        this.dead = dead;
        if (dead) {
            this.currentHealth = 0.0;
            this.incapacitated = true;
        }
    }

    public boolean isIncapacitated() { return incapacitated || currentHealth <= 0.0; }
    public void setIncapacitated(boolean incapacitated) { this.incapacitated = incapacitated; }

    public boolean isEscaped() { return escaped; }
    public void setEscaped(boolean escaped) { this.escaped = escaped; }

    public boolean hasAttemptedEscapeThisBattle() { return normalEscapeAttemptedThisBattle; }
    public void setNormalEscapeAttemptedThisBattle(boolean attempted) {
        this.normalEscapeAttemptedThisBattle = attempted;
    }

    public List<CombatInjury> getActiveInjuries() {
        return Collections.unmodifiableList(activeInjuries);
    }

    public List<TemporaryCombatEffect> getActiveEffects() {
        return Collections.unmodifiableList(activeEffects);
    }

    public String getLastKillerCombatantId() { return lastKillerCombatantId; }
    public void setLastKillerCombatantId(String id) { this.lastKillerCombatantId = id; }

    public String getLastKillerPersonId() { return lastKillerPersonId; }
    public void setLastKillerPersonId(String id) { this.lastKillerPersonId = id; }

    public String getLastKillerSideId() { return lastKillerSideId; }
    public void setLastKillerSideId(String sideId) { this.lastKillerSideId = sideId; }
}
