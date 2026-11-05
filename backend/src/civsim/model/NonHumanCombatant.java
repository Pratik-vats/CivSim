package civsim.model;

import java.util.EnumMap;
import java.util.Map;

/**
 * Phase 2B.1 — Extensible combatant adapter for non-human entities (Bonded Beasts, Monsters, Alpha Monsters).
 */
public class NonHumanCombatant implements Combatant {

    private final String id;
    private final String name;
    private final CombatantType entityType;
    private String sideId;

    private final Map<AttributeType, Double> attributes = new EnumMap<>(AttributeType.class);
    private final CombatState combatState;

    private double equipmentAttackBonus;
    private double equipmentDefenseBonus;
    private double equipmentEvasionBonus;

    private boolean awakened;
    private SupernaturalClass supernaturalClass;
    private AetherAspect aspect;
    private int powerTier;
    private double mastery;
    private DerivedPowerProfile derivedPowerProfile;

    public NonHumanCombatant(String id, String name, CombatantType entityType, String sideId) {
        this.id = id;
        this.name = name != null ? name : id;
        this.entityType = entityType != null ? entityType : CombatantType.MONSTER;
        this.sideId = sideId != null ? sideId : "MONSTERS";
        this.combatState = new CombatState();

        // Default baseline attributes
        for (AttributeType attr : AttributeType.values()) {
            attributes.put(attr, 20.0);
        }
        attributes.put(AttributeType.LUCK, 20.0);

        this.equipmentAttackBonus = 0.0;
        this.equipmentDefenseBonus = 0.0;
        this.equipmentEvasionBonus = 0.0;
        this.awakened = false;
        this.supernaturalClass = null;
        this.aspect = AetherAspect.AETHERIC;
        this.powerTier = 0;
        this.mastery = 0.0;
        this.derivedPowerProfile = null;

        combatState.initializeFullPools(
                getEffectiveVitality(), getEffectiveEndurance(),
                getEffectiveAetherCapacity(), getEffectiveAetherControl(), awakened
        );
    }

    public NonHumanCombatant(String id, String name, CombatantType entityType, String sideId,
                             double str, double agi, double vit, double end,
                             double intl, double per, double wil, double luck,
                             double aethCap, double aethCtrl,
                             double equipAtk, double equipDef) {
        this(id, name, entityType, sideId);
        attributes.put(AttributeType.STRENGTH, str);
        attributes.put(AttributeType.AGILITY, agi);
        attributes.put(AttributeType.VITALITY, vit);
        attributes.put(AttributeType.ENDURANCE, end);
        attributes.put(AttributeType.INTELLIGENCE, intl);
        attributes.put(AttributeType.PERCEPTION, per);
        attributes.put(AttributeType.WILLPOWER, wil);
        attributes.put(AttributeType.LUCK, luck);
        attributes.put(AttributeType.AETHER_CAPACITY, aethCap);
        attributes.put(AttributeType.AETHER_CONTROL, aethCtrl);
        this.equipmentAttackBonus = equipAtk;
        this.equipmentDefenseBonus = equipDef;
        this.awakened = (aethCap > 0.0 || aethCtrl > 0.0);
        this.combatState.recalculateMaxPools(vit, end, aethCap, aethCtrl, this.awakened);
        this.combatState.setCurrentHealth(this.combatState.getMaxHealth());
        this.combatState.setCurrentStamina(this.combatState.getMaxStamina());
        this.combatState.setCurrentAether(this.combatState.getMaxAether());
    }

    public NonHumanCombatant(String id, String name, CombatantType entityType, String sideId,
                             double initHealth, double initStamina, double initAether, double initMorale,
                             double str, double agi, double vit, double end,
                             double intl, double per, double wil, double luck,
                             double equipAtk, double equipDef) {
        this(id, name, entityType, sideId, str, agi, vit, end, intl, per, wil, luck, initAether, 0.0, equipAtk, equipDef);
        if (initHealth > 0.0) {
            this.combatState.setCurrentHealth(Math.min(initHealth, this.combatState.getMaxHealth()));
        }
        if (initStamina > 0.0) {
            this.combatState.setCurrentStamina(Math.min(initStamina, this.combatState.getMaxStamina()));
        }
        if (initMorale > 0.0) {
            this.combatState.setCurrentMorale(initMorale);
        }
    }

    public void setAttribute(AttributeType attr, double value) {
        attributes.put(attr, value);
        combatState.recalculateMaxPools(
                getEffectiveVitality(), getEffectiveEndurance(),
                getEffectiveAetherCapacity(), getEffectiveAetherControl(), awakened
        );
    }

    @Override
    public String getCombatantId() { return id; }

    @Override
    public CombatantType getEntityType() { return entityType; }

    @Override
    public String getName() { return name; }

    @Override
    public String getSideId() { return sideId; }

    @Override
    public void setSideId(String sideId) { this.sideId = sideId; }

    @Override
    public boolean isAlive() {
        return !combatState.isDead();
    }

    @Override
    public boolean canActInCombat() {
        return isAlive()
                && !combatState.isIncapacitated()
                && !combatState.isEscaped()
                && combatState.getCurrentHealth() > 0.0;
    }

    @Override
    public CombatState getCombatState() { return combatState; }

    @Override
    public double getEffectiveStrength() {
        return Math.max(1.0, attributes.getOrDefault(AttributeType.STRENGTH, 20.0) - combatState.getTotalInjuryPenalty(AttributeType.STRENGTH));
    }

    @Override
    public double getEffectiveAgility() {
        return Math.max(1.0, attributes.getOrDefault(AttributeType.AGILITY, 20.0) - combatState.getTotalInjuryPenalty(AttributeType.AGILITY));
    }

    @Override
    public double getEffectiveVitality() {
        return Math.max(1.0, attributes.getOrDefault(AttributeType.VITALITY, 20.0) - combatState.getTotalInjuryPenalty(AttributeType.VITALITY));
    }

    @Override
    public double getEffectiveEndurance() {
        return Math.max(1.0, attributes.getOrDefault(AttributeType.ENDURANCE, 20.0) - combatState.getTotalInjuryPenalty(AttributeType.ENDURANCE));
    }

    @Override
    public double getEffectiveIntelligence() {
        return attributes.getOrDefault(AttributeType.INTELLIGENCE, 20.0);
    }

    @Override
    public double getEffectivePerception() {
        return attributes.getOrDefault(AttributeType.PERCEPTION, 20.0);
    }

    @Override
    public double getEffectiveWillpower() {
        return attributes.getOrDefault(AttributeType.WILLPOWER, 20.0);
    }

    @Override
    public double getEffectiveLuck() {
        return attributes.getOrDefault(AttributeType.LUCK, 20.0);
    }

    @Override
    public double getEffectiveAetherCapacity() {
        return awakened ? attributes.getOrDefault(AttributeType.AETHER_CAPACITY, 0.0) : 0.0;
    }

    @Override
    public double getEffectiveAetherControl() {
        return awakened ? attributes.getOrDefault(AttributeType.AETHER_CONTROL, 0.0) : 0.0;
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
    public boolean isAwakened() { return awakened; }
    public void setAwakened(boolean awakened) { this.awakened = awakened; }

    @Override
    public SupernaturalClass getSupernaturalClass() { return supernaturalClass; }
    public void setSupernaturalClass(SupernaturalClass cls) { this.supernaturalClass = cls; }

    @Override
    public AetherAspect getAspect() { return aspect; }
    public void setAspect(AetherAspect aspect) { this.aspect = aspect; }

    @Override
    public int getPowerTier() { return powerTier; }
    public void setPowerTier(int tier) { this.powerTier = tier; }

    @Override
    public double getMastery() { return mastery; }
    public void setMastery(double mastery) { this.mastery = mastery; }

    @Override
    public DerivedPowerProfile getDerivedPowerProfile() { return derivedPowerProfile; }
    public void setDerivedPowerProfile(DerivedPowerProfile profile) { this.derivedPowerProfile = profile; }

    @Override
    public Person getPerson() { return null; }
}
