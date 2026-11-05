package civsim.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Phase 2C.1 -- Formal data-driven blueprint for crafting equipment items.
 */
public class EquipmentRecipe {

    private final String recipeId;
    private final String name;
    private final EquipmentCategory category;
    private final EquipmentSlot slot;
    private final EquipmentType type;
    private final boolean twoHanded;
    private final ClassRarity targetRarity;
    private final int targetTier;
    private final FacilityGrade minFacilityGrade;
    private final int minTechLevel;
    private final boolean supernatural;
    private final CrafterSpecialization specialization;
    private final double minProficiency;
    private final int durationMonths;

    // Mundane Material Costs
    private final double oreCost;
    private final double woodCost;
    private final double toolsCost;
    private final double totalIronCost;

    // Supernatural Material Costs
    private final Map<SupernaturalResourceType, Double> supernaturalCosts;
    private final Map<MonsterMaterialType, Double> monsterMaterialCosts;
    private final ResourceGrade minMonsterCoreGrade;

    // Output Base Stat Profile
    private final EquipmentStatProfile baseStats;

    public EquipmentRecipe(
            String recipeId,
            String name,
            EquipmentCategory category,
            EquipmentSlot slot,
            EquipmentType type,
            boolean twoHanded,
            ClassRarity targetRarity,
            int targetTier,
            FacilityGrade minFacilityGrade,
            int minTechLevel,
            boolean supernatural,
            CrafterSpecialization specialization,
            double minProficiency,
            int durationMonths,
            double oreCost,
            double woodCost,
            double toolsCost,
            double totalIronCost,
            Map<SupernaturalResourceType, Double> supernaturalCosts,
            Map<MonsterMaterialType, Double> monsterMaterialCosts,
            ResourceGrade minMonsterCoreGrade,
            EquipmentStatProfile baseStats) {
        this.recipeId = recipeId;
        this.name = name;
        this.category = category;
        this.slot = slot;
        this.type = type;
        this.twoHanded = twoHanded;
        this.targetRarity = targetRarity != null ? targetRarity : ClassRarity.COMMON;
        this.targetTier = Math.max(1, Math.min(10, targetTier));
        this.minFacilityGrade = minFacilityGrade != null ? minFacilityGrade : FacilityGrade.BASIC_FORGE;
        this.minTechLevel = minTechLevel;
        this.supernatural = supernatural;
        this.specialization = specialization != null ? specialization : CrafterSpecialization.WEAPON_FORGING;
        this.minProficiency = Math.max(0.0, minProficiency);
        this.durationMonths = Math.max(1, durationMonths);
        this.oreCost = Math.max(0.0, oreCost);
        this.woodCost = Math.max(0.0, woodCost);
        this.toolsCost = Math.max(0.0, toolsCost);
        this.totalIronCost = Math.max(0.0, totalIronCost);
        this.supernaturalCosts = (supernaturalCosts != null) ? new EnumMap<>(supernaturalCosts) : new EnumMap<>(SupernaturalResourceType.class);
        this.monsterMaterialCosts = (monsterMaterialCosts != null) ? new EnumMap<>(monsterMaterialCosts) : new EnumMap<>(MonsterMaterialType.class);
        this.minMonsterCoreGrade = minMonsterCoreGrade;
        this.baseStats = baseStats != null ? baseStats : EquipmentStatProfile.zero();
    }

    public String getRecipeId() { return recipeId; }
    public String getName() { return name; }
    public EquipmentCategory getCategory() { return category; }
    public EquipmentSlot getSlot() { return slot; }
    public EquipmentType getType() { return type; }
    public boolean isTwoHanded() { return twoHanded; }
    public ClassRarity getTargetRarity() { return targetRarity; }
    public int getTargetTier() { return targetTier; }
    public FacilityGrade getMinFacilityGrade() { return minFacilityGrade; }
    public int getMinTechLevel() { return minTechLevel; }
    public boolean isSupernatural() { return supernatural; }
    public CrafterSpecialization getSpecialization() { return specialization; }
    public double getMinProficiency() { return minProficiency; }
    public int getDurationMonths() { return durationMonths; }

    public double getOreCost() { return oreCost; }
    public double getWoodCost() { return woodCost; }
    public double getToolsCost() { return toolsCost; }
    public double getTotalIronCost() { return totalIronCost; }
    public Map<SupernaturalResourceType, Double> getSupernaturalCosts() { return Collections.unmodifiableMap(supernaturalCosts); }
    public Map<MonsterMaterialType, Double> getMonsterMaterialCosts() { return Collections.unmodifiableMap(monsterMaterialCosts); }
    public ResourceGrade getMinMonsterCoreGrade() { return minMonsterCoreGrade; }
    public EquipmentStatProfile getBaseStats() { return baseStats; }
}

