package civsim.model;

import civsim.spatial.Biome;
import java.util.*;

/**
 * Phase 2B.5 — Core living Monster entity in the autonomous monster ecology.
 * Implements the shared Phase 2B.1 Combatant interface, cleanly separated from the human Person model.
 * A Monster is never counted in human populations, never holds a human Family or bank account,
 * and never triggers human estate inheritance upon death.
 */
public class Monster implements Combatant {

    private final String monsterId;
    private final String speciesId;
    private String displayName;
    private final MonsterCategory category;
    private final MonsterThreatRank threatRank;

    // Spatial & Habitat
    private double posX;
    private double posY;
    private Biome currentBiome;
    private final String originLairId;
    private String currentLairId;

    // Attributes (0-100 scale, modified by ThreatRank and TraitProfile)
    private double strength;
    private double agility;
    private double vitality;
    private double endurance;
    private double intelligence;
    private double perception;
    private double willpower;
    private double luck;
    private double aetherCapacity;
    private double aetherControl;

    private double naturalArmor;
    private double baseSpeed;
    private double siegeDamageMultiplier;

    // State & Ecology
    private final MonsterSpeciesDefinition speciesDef;
    private final CombatState combatState;
    private MonsterBehaviorState behaviorState;
    private final int evolutionaryGeneration;
    private final MonsterTraitProfile traitProfile;
    private boolean alive;
    private Integer deathTotalMonths;
    private String currentSideId;

    // Lifecycle & Lineage
    private int ageMonths;
    private final int maturityAgeMonths;
    private final int maximumAgeMonths;
    private int birthTotalMonths;
    private final List<String> parentMonsterIds = new ArrayList<>();
    private boolean displaced;
    private double starvedMonths;
    private String deathCause;

    public Monster(
            String monsterId,
            String speciesId,
            String displayName,
            MonsterCategory category,
            MonsterThreatRank threatRank,
            double posX,
            double posY,
            Biome currentBiome,
            String originLairId,
            MonsterSpeciesDefinition speciesDef,
            MonsterTraitProfile traitProfile,
            int evolutionaryGeneration) {

        this.monsterId = monsterId;
        this.speciesId = speciesId;
        this.speciesDef = speciesDef;
        this.displayName = displayName != null ? displayName : (speciesDef != null ? speciesDef.getDisplayName() : speciesId);
        this.category = category != null ? category : (speciesDef != null ? speciesDef.getCategory() : MonsterCategory.BEAST);
        this.threatRank = threatRank != null ? threatRank : MonsterThreatRank.STANDARD;

        this.posX = posX;
        this.posY = posY;
        this.currentBiome = currentBiome != null ? currentBiome : Biome.FOREST;
        this.originLairId = originLairId;
        this.currentLairId = originLairId;
        this.currentSideId = "MONSTERS";

        this.evolutionaryGeneration = Math.max(1, evolutionaryGeneration);
        this.traitProfile = traitProfile != null ? traitProfile : MonsterTraitProfile.createDefault();
        this.behaviorState = MonsterBehaviorState.IDLE;
        this.alive = true;
        this.deathTotalMonths = null;

        this.maturityAgeMonths = (speciesDef != null) ? speciesDef.getMaturityAgeMonths() : 12;
        this.maximumAgeMonths = (speciesDef != null) ? speciesDef.getMaximumAgeMonths() : 360;
        this.ageMonths = 0;
        this.birthTotalMonths = 0;
        this.displaced = false;
        this.starvedMonths = 0.0;
        this.deathCause = null;

        // Initialize scaled attributes from species baseline, threat rank, and trait profile
        double pwrMult = this.threatRank.getPowerMultiplier();
        double ferocity = this.traitProfile.getFerocity();
        double armorBonus = this.traitProfile.getNaturalArmor();
        double spdBonus = this.traitProfile.getSpeed();
        double perBonus = this.traitProfile.getPerception();
        double aethBonus = this.traitProfile.getAetherAffinity();

        if (speciesDef != null) {
            this.strength = clampAttr(speciesDef.getBaseStrength() * (1.0 + 0.10 * (pwrMult - 1.0)) + ferocity);
            this.agility = clampAttr(speciesDef.getBaseAgility() * (1.0 + 0.08 * (pwrMult - 1.0)) + spdBonus);
            this.vitality = clampAttr(speciesDef.getBaseVitality() * (1.0 + 0.15 * (pwrMult - 1.0)));
            this.endurance = clampAttr(speciesDef.getBaseEndurance() * (1.0 + 0.12 * (pwrMult - 1.0)));
            this.intelligence = clampAttr(speciesDef.getBaseIntelligence() * (1.0 + 0.05 * (pwrMult - 1.0)));
            this.perception = clampAttr(speciesDef.getBasePerception() * (1.0 + 0.08 * (pwrMult - 1.0)) + perBonus);
            this.willpower = clampAttr(speciesDef.getBaseWillpower() * (1.0 + 0.10 * (pwrMult - 1.0)));
            this.luck = 20.0;
            this.aetherCapacity = clampAttr(speciesDef.getBaseAetherCapacity() * (1.0 + 0.15 * (pwrMult - 1.0)) + aethBonus);
            this.aetherControl = clampAttr(speciesDef.getBaseAetherControl() * (1.0 + 0.12 * (pwrMult - 1.0)) + aethBonus);

            this.naturalArmor = Math.max(0.0, speciesDef.getNaturalArmor() + armorBonus);
            this.baseSpeed = Math.max(1.0, speciesDef.getBaseSpeed() + (spdBonus * 0.20));
            this.siegeDamageMultiplier = speciesDef.getSiegeDamageMultiplier();
        } else {
            this.strength = 30.0;
            this.agility = 30.0;
            this.vitality = 30.0;
            this.endurance = 30.0;
            this.intelligence = 15.0;
            this.perception = 30.0;
            this.willpower = 25.0;
            this.luck = 20.0;
            this.aetherCapacity = 10.0;
            this.aetherControl = 10.0;
            this.naturalArmor = 3.0;
            this.baseSpeed = 15.0;
            this.siegeDamageMultiplier = 1.0;
        }

        // Initialize CombatState
        this.combatState = new CombatState();
        boolean isAwakened = this.aetherCapacity > 15.0 || this.threatRank.isApexOrAncient();
        combatState.initializeFullPools(
                this.vitality, this.endurance, this.aetherCapacity, this.aetherControl, isAwakened
        );
        // Threat rank health scaling
        double baseHp = combatState.getMaxHealth() * (0.8 + 0.4 * pwrMult);
        combatState.setCurrentHealth(baseHp);
    }

    private static double clampAttr(double val) {
        return Math.max(1.0, Math.min(100.0, val));
    }

    // =========================================================================
    // COMBATANT INTERFACE IMPLEMENTATION
    // =========================================================================

    @Override
    public String getCombatantId() {
        return monsterId;
    }

    @Override
    public CombatantType getEntityType() {
        return (threatRank == MonsterThreatRank.ALPHA || threatRank == MonsterThreatRank.ANCIENT)
                ? CombatantType.ALPHA_MONSTER
                : CombatantType.MONSTER;
    }

    @Override
    public String getName() {
        return displayName != null ? displayName : speciesId + "_" + monsterId;
    }

    @Override
    public String getSideId() {
        return currentSideId;
    }

    @Override
    public void setSideId(String sideId) {
        this.currentSideId = (sideId != null) ? sideId : "MONSTERS";
    }

    @Override
    public boolean isAlive() {
        return alive && (combatState == null || !combatState.isDead());
    }

    @Override
    public boolean canActInCombat() {
        return isAlive()
                && combatState != null
                && !combatState.isIncapacitated()
                && !combatState.isEscaped()
                && combatState.getCurrentHealth() > 0.0;
    }

    @Override
    public CombatState getCombatState() {
        return combatState;
    }

    @Override
    public double getEffectiveStrength() { return strength; }
    @Override
    public double getEffectiveAgility() { return agility; }
    @Override
    public double getEffectiveVitality() { return vitality; }
    @Override
    public double getEffectiveEndurance() { return endurance; }
    @Override
    public double getEffectiveIntelligence() { return intelligence; }
    @Override
    public double getEffectivePerception() { return perception; }
    @Override
    public double getEffectiveWillpower() { return willpower; }
    @Override
    public double getEffectiveLuck() { return luck; }
    @Override
    public double getEffectiveAetherCapacity() { return aetherCapacity; }
    @Override
    public double getEffectiveAetherControl() { return aetherControl; }

    @Override
    public double getPhysicalAttackRating() {
        return (strength * 0.60) + (agility * 0.40);
    }

    @Override
    public double getPhysicalDefenseRating() {
        return naturalArmor + (endurance * 0.30);
    }

    @Override
    public double getEquipmentAttackBonus() { return 0.0; }
    @Override
    public double getEquipmentDefenseBonus() { return naturalArmor; }
    @Override
    public double getEquipmentEvasionBonus() { return Math.max(0.0, traitProfile.getSpeed() * 0.20); }

    @Override
    public boolean isAwakened() {
        return aetherCapacity > 15.0 || threatRank.isApexOrAncient();
    }

    @Override
    public SupernaturalClass getSupernaturalClass() { return null; }
    @Override
    public AetherAspect getAspect() { return AetherAspect.AETHERIC; }

    @Override
    public int getPowerTier() {
        switch (threatRank) {
            case ANCIENT: return 6;
            case ALPHA: return 4;
            case ELITE: return 2;
            case STANDARD: return 1;
            default: return 0;
        }
    }

    @Override
    public double getMastery() { return Math.max(0.0, perception * 0.5); }

    @Override
    public DerivedPowerProfile getDerivedPowerProfile() {
        double atk = getPhysicalAttackRating();
        double def = getPhysicalDefenseRating();
        return new DerivedPowerProfile(
                atk + def,
                atk,
                def,
                agility * 0.5,
                perception * 0.5,
                endurance * 0.5
        );
    }

    /**
     * Strict Separation Rule: A Monster is never a human Person. Always returns null.
     */
    @Override
    public Person getPerson() {
        return null;
    }

    // =========================================================================
    // MONSTER SPECIFIC GETTERS & SETTERS
    // =========================================================================

    public String getMonsterId() { return monsterId; }
    public String getSpeciesId() { return speciesId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String name) { this.displayName = name; }
    public MonsterCategory getCategory() { return category; }
    public MonsterThreatRank getThreatRank() { return threatRank; }

    public double getPosX() { return posX; }
    public double getPosY() { return posY; }
    public void setPosition(double x, double y) { this.posX = x; this.posY = y; }
    public Biome getCurrentBiome() { return currentBiome; }
    public void setCurrentBiome(Biome b) { this.currentBiome = b; }

    public String getOriginLairId() { return originLairId; }
    public String getCurrentLairId() { return currentLairId; }
    public void setCurrentLairId(String lairId) { this.currentLairId = lairId; }

    public double getNaturalArmor() { return naturalArmor; }
    public double getBaseSpeed() { return baseSpeed; }
    public double getSiegeDamageMultiplier() { return siegeDamageMultiplier; }

    public MonsterBehaviorState getBehaviorState() { return behaviorState; }
    public void setBehaviorState(MonsterBehaviorState state) { this.behaviorState = state; }

    public int getEvolutionaryGeneration() { return evolutionaryGeneration; }
    public MonsterTraitProfile getTraitProfile() { return traitProfile; }

    public void setAlive(boolean alive) {
        this.alive = alive;
        if (!alive && this.combatState != null) {
            this.combatState.setDead(true);
        }
    }

    public Integer getDeathTotalMonths() { return deathTotalMonths; }
    public void setDeathTotalMonths(Integer m) { this.deathTotalMonths = m; }

    public MonsterSpeciesDefinition getSpeciesDef() { return speciesDef; }
    public int getAgeMonths() { return ageMonths; }
    public void setAgeMonths(int ageMonths) { this.ageMonths = Math.max(0, ageMonths); }
    public int getMaturityAgeMonths() { return maturityAgeMonths; }
    public int getMaximumAgeMonths() { return maximumAgeMonths; }
    public int getBirthTotalMonths() { return birthTotalMonths; }
    public void setBirthTotalMonths(int m) { this.birthTotalMonths = m; }
    public boolean isMature() { return ageMonths >= maturityAgeMonths; }
    public boolean hasExceededLifespan() { return ageMonths >= maximumAgeMonths; }
    public void incrementAgeMonths(int deltaMonths) { this.ageMonths += deltaMonths; }

    public List<String> getParentMonsterIds() { return Collections.unmodifiableList(parentMonsterIds); }
    public void addParentMonsterId(String id) { if (id != null && !parentMonsterIds.contains(id)) parentMonsterIds.add(id); }

    public boolean isDisplaced() { return displaced; }
    public void setDisplaced(boolean displaced) { this.displaced = displaced; }

    public double getStarvedMonths() { return starvedMonths; }
    public void setStarvedMonths(double starvedMonths) { this.starvedMonths = Math.max(0.0, starvedMonths); }

    public String getDeathCause() { return deathCause; }
    public void setDeathCause(String deathCause) { this.deathCause = deathCause; }

    public boolean isSiegeCapable() {
        return (speciesDef != null && speciesDef.isSiegeCapable()) || siegeDamageMultiplier >= 1.5 || category == MonsterCategory.SIEGE_CREATURE;
    }

    public double getElementalResistance() {
        return Math.max(0.0, traitProfile.getElementalResistance());
    }

    public double getEnvironmentalAdaptation() {
        return traitProfile.getEnvironmentalAdaptation();
    }

    public double getGroupCoordination() {
        return traitProfile.getGroupCoordination();
    }

    @Override
    public String toString() {
        return String.format("Monster[%s (%s, %s): %s at (%.1f, %.1f) HP:%.1f Gen:%d]",
                monsterId, speciesId, threatRank.getDisplayName(), behaviorState.getDisplayName(),
                posX, posY, combatState != null ? combatState.getCurrentHealth() : 0.0, evolutionaryGeneration);
    }
}
