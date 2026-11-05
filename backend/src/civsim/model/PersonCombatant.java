package civsim.model;

import civsim.ProgressionConfig;
import civsim.systems.PowerAssessmentCalculator;

/**
 * Phase 2B.1 — Combatant adapter directly wrapping a Person instance.
 * Serves as the single source of truth for humans, mortal soldiers, and awakened champions.
 */
public class PersonCombatant implements Combatant {

    private final Person person;
    private String sideId;
    private double equipmentAttackBonus;
    private double equipmentDefenseBonus;
    private double equipmentEvasionBonus;
    private double equipmentAccuracyBonus;
    private double equipmentAetherPotencyBonus;
    private double equipmentAetherCostEfficiency;
    private double equipmentBarrierBonus;

    public PersonCombatant(Person person, String sideId) {
        if (person == null) {
            throw new IllegalArgumentException("Person cannot be null.");
        }
        this.person = person;
        this.sideId = sideId != null ? sideId : "DEFAULT";
        this.equipmentAttackBonus = 0.0;
        this.equipmentDefenseBonus = 0.0;
        this.equipmentEvasionBonus = 0.0;

        // Auto-detect military equipment from village / occupation
        if (person.getVillage() != null && person.getVillage().hasWeaponsForMilitary()) {
            this.equipmentAttackBonus = 10.0;
            this.equipmentDefenseBonus = 5.0;
        } else if (person.getOccupation() == Occupation.SOLDIER) {
            this.equipmentAttackBonus = 5.0;
            this.equipmentDefenseBonus = 3.0;
        }

        // Phase 2C.1 Persistent Equipment Integration
        civsim.systems.EquipmentCraftingSystem ecs = civsim.systems.EquipmentCraftingSystem.getActiveInstance();
        if (ecs != null) {
            DerivedEquipmentProfile profile = ecs.computeDerivedEquipmentProfile(person);
            if (profile != null && profile.getTotalEquippedItems() > 0) {
                applyDerivedEquipmentProfile(profile);
            }
        }

        // Initialize CombatState on person if not present
        CombatState cs = person.getOrCreateCombatState();
        if (!cs.isInitializedInLife()) {
            cs.initializeFullPools(
                    getEffectiveVitality(),
                    getEffectiveEndurance(),
                    getEffectiveAetherCapacity(),
                    getEffectiveAetherControl(),
                    isAwakened()
            );
        } else {
            cs.recalculateMaxPools(
                    getEffectiveVitality(),
                    getEffectiveEndurance(),
                    getEffectiveAetherCapacity(),
                    getEffectiveAetherControl(),
                    isAwakened()
            );
        }
    }

    @Override
    public String getCombatantId() {
        return "PERSON_" + person.getId();
    }

    @Override
    public CombatantType getEntityType() {
        return CombatantType.HUMAN;
    }

    @Override
    public String getName() {
        return person.getFullName();
    }

    @Override
    public String getSideId() {
        return sideId;
    }

    @Override
    public void setSideId(String sideId) {
        this.sideId = sideId;
    }

    @Override
    public boolean isAlive() {
        return person.isAlive() && !getCombatState().isDead();
    }

    @Override
    public boolean canActInCombat() {
        return isAlive()
                && !getCombatState().isIncapacitated()
                && !getCombatState().isEscaped()
                && getCombatState().getCurrentHealth() > 0.0;
    }

    @Override
    public CombatState getCombatState() {
        return person.getOrCreateCombatState();
    }

    @Override
    public double getEffectiveStrength() {
        AetherProfile ap = person.getAetherProfile();
        return ap != null ? ap.getEffectiveStrength() : Math.max(1.0, 15.0 - getCombatState().getTotalInjuryPenalty(AttributeType.STRENGTH));
    }

    @Override
    public double getEffectiveAgility() {
        AetherProfile ap = person.getAetherProfile();
        return ap != null ? ap.getEffectiveAgility() : Math.max(1.0, 15.0 - getCombatState().getTotalInjuryPenalty(AttributeType.AGILITY));
    }

    @Override
    public double getEffectiveVitality() {
        AetherProfile ap = person.getAetherProfile();
        return ap != null ? ap.getEffectiveVitality() : Math.max(1.0, 15.0 - getCombatState().getTotalInjuryPenalty(AttributeType.VITALITY));
    }

    @Override
    public double getEffectiveEndurance() {
        AetherProfile ap = person.getAetherProfile();
        return ap != null ? ap.getEffectiveEndurance() : Math.max(1.0, 15.0 - getCombatState().getTotalInjuryPenalty(AttributeType.ENDURANCE));
    }

    @Override
    public double getEffectiveIntelligence() {
        AetherProfile ap = person.getAetherProfile();
        return ap != null ? ap.getEffectiveIntelligence() : 15.0;
    }

    @Override
    public double getEffectivePerception() {
        AetherProfile ap = person.getAetherProfile();
        return ap != null ? ap.getEffectivePerception() : 15.0;
    }

    @Override
    public double getEffectiveWillpower() {
        AetherProfile ap = person.getAetherProfile();
        return ap != null ? ap.getEffectiveWillpower() : 15.0;
    }

    @Override
    public double getEffectiveLuck() {
        AetherProfile ap = person.getAetherProfile();
        return ap != null ? ap.getEffectiveLuckDouble() : 20.0;
    }

    @Override
    public double getEffectiveAetherCapacity() {
        AetherProfile ap = person.getAetherProfile();
        return (ap != null && ap.isAwakened()) ? ap.getEffectiveAetherCapacity() : 0.0;
    }

    @Override
    public double getEffectiveAetherControl() {
        AetherProfile ap = person.getAetherProfile();
        return (ap != null && ap.isAwakened()) ? ap.getEffectiveAetherControl() : 0.0;
    }

    @Override
    public double getPhysicalAttackRating() {
        return 10.0 + (0.45 * getEffectiveStrength())
                + (0.20 * getEffectiveAgility())
                + (0.15 * getEffectiveEndurance())
                + equipmentAttackBonus;
    }

    @Override
    public double getPhysicalDefenseRating() {
        return (0.40 * getEffectiveVitality())
                + (0.30 * getEffectiveEndurance())
                + (0.15 * getEffectiveWillpower())
                + equipmentDefenseBonus;
    }

    @Override
    public double getEquipmentAttackBonus() { return equipmentAttackBonus; }
    public void setEquipmentAttackBonus(double bonus) { this.equipmentAttackBonus = bonus; }

    @Override
    public double getEquipmentDefenseBonus() { return equipmentDefenseBonus; }
    public void setEquipmentDefenseBonus(double bonus) { this.equipmentDefenseBonus = bonus; }

    @Override
    public double getEquipmentEvasionBonus() { return equipmentEvasionBonus; }
    public void setEquipmentEvasionBonus(double bonus) { this.equipmentEvasionBonus = bonus; }

    @Override
    public double getEquipmentAccuracyBonus() { return equipmentAccuracyBonus; }
    public void setEquipmentAccuracyBonus(double bonus) { this.equipmentAccuracyBonus = bonus; }

    @Override
    public double getEquipmentAetherPotencyBonus() { return equipmentAetherPotencyBonus; }
    public void setEquipmentAetherPotencyBonus(double bonus) { this.equipmentAetherPotencyBonus = bonus; }

    @Override
    public double getEquipmentAetherCostEfficiency() { return equipmentAetherCostEfficiency; }
    public void setEquipmentAetherCostEfficiency(double val) { this.equipmentAetherCostEfficiency = val; }

    @Override
    public double getEquipmentBarrierBonus() { return equipmentBarrierBonus; }
    public void setEquipmentBarrierBonus(double bonus) { this.equipmentBarrierBonus = bonus; }

    public void applyDerivedEquipmentProfile(DerivedEquipmentProfile profile) {
        if (profile != null) {
            this.equipmentAttackBonus = profile.getAttackBonus();
            this.equipmentDefenseBonus = profile.getDefenseBonus();
            this.equipmentEvasionBonus = profile.getEvasionBonus();
            this.equipmentAccuracyBonus = profile.getAccuracyBonus();
            this.equipmentAetherPotencyBonus = profile.getAetherPotencyBonus();
            this.equipmentAetherCostEfficiency = profile.getAetherCostEfficiency();
            this.equipmentBarrierBonus = profile.getBarrierOrResistanceBonus();
        }
    }

    @Override
    public boolean isAwakened() {
        return person.isAwakened();
    }

    @Override
    public SupernaturalClass getSupernaturalClass() {
        AetherProfile ap = person.getAetherProfile();
        return (ap != null && ap.isAwakened()) ? ap.getSupernaturalClass() : null;
    }

    @Override
    public AetherAspect getAspect() {
        AetherProfile ap = person.getAetherProfile();
        if (ap != null && ap.isDarkAwakened()) {
            return AetherAspect.DARK;
        }
        return AetherAspect.AETHERIC;
    }

    @Override
    public int getPowerTier() {
        AetherProfile ap = person.getAetherProfile();
        return (ap != null && ap.isAwakened()) ? ap.getPowerTier() : 0;
    }

    @Override
    public double getMastery() {
        AetherProfile ap = person.getAetherProfile();
        if (ap != null && ap.getProgressionRecord() != null) {
            return ap.getProgressionRecord().getMastery();
        }
        return 0.0;
    }

    @Override
    public DerivedPowerProfile getDerivedPowerProfile() {
        if (!isAwakened()) return null;
        return PowerAssessmentCalculator.compute(person, ProgressionConfig.createDefault());
    }

    @Override
    public Person getPerson() {
        return person;
    }
}
