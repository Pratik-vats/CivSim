package civsim.model;

import civsim.spatial.Biome;

import java.util.*;

/**
 * Phase 2B.5 — Data-driven definition catalog entry for a Monster species.
 */
public final class MonsterSpeciesDefinition {

    private final String speciesId;
    private final String displayName;
    private final MonsterCategory category;
    private final Set<Biome> compatibleBiomes;

    // Baseline Attributes (0-100 scale)
    private final double baseStrength;
    private final double baseAgility;
    private final double baseVitality;
    private final double baseEndurance;
    private final double baseIntelligence;
    private final double basePerception;
    private final double baseWillpower;
    private final double baseAetherCapacity;
    private final double baseAetherControl;

    // Movement, Ecology & Defense
    private final double baseSpeed; // Distance units per month
    private final double perceptionRadius;
    private final double monthlyBiomassConsumption;
    private final double territoryRadius;
    private final Set<MonsterThreatRank> allowedThreatRanks;
    private final double naturalArmor;
    private final double siegeDamageMultiplier;

    // Drop Profile
    private final MonsterMaterialType primaryMaterial;
    private final MonsterMaterialType secondaryMaterial;
    private final int maxRegionalDensity;

    // Lifecycle & Reproduction
    private final MonsterReproductionMode reproductionMode;
    private final int maturityAgeMonths;
    private final int maximumAgeMonths;

    public MonsterSpeciesDefinition(
            String speciesId,
            String displayName,
            MonsterCategory category,
            Set<Biome> compatibleBiomes,
            double baseStrength,
            double baseAgility,
            double baseVitality,
            double baseEndurance,
            double baseIntelligence,
            double basePerception,
            double baseWillpower,
            double baseAetherCapacity,
            double baseAetherControl,
            double baseSpeed,
            double perceptionRadius,
            double monthlyBiomassConsumption,
            double territoryRadius,
            Set<MonsterThreatRank> allowedThreatRanks,
            double naturalArmor,
            double siegeDamageMultiplier,
            MonsterMaterialType primaryMaterial,
            MonsterMaterialType secondaryMaterial,
            int maxRegionalDensity) {
        this(speciesId, displayName, category, compatibleBiomes, baseStrength, baseAgility, baseVitality,
                baseEndurance, baseIntelligence, basePerception, baseWillpower, baseAetherCapacity,
                baseAetherControl, baseSpeed, perceptionRadius, monthlyBiomassConsumption, territoryRadius,
                allowedThreatRanks, naturalArmor, siegeDamageMultiplier, primaryMaterial, secondaryMaterial,
                maxRegionalDensity,
                (category == MonsterCategory.SPIRIT) ? MonsterReproductionMode.AETHER_SPAWN : (category == MonsterCategory.CONSTRUCT ? MonsterReproductionMode.ASSEMBLY : MonsterReproductionMode.PAIR_BREEDING),
                12, 360);
    }

    public MonsterSpeciesDefinition(
            String speciesId,
            String displayName,
            MonsterCategory category,
            Set<Biome> compatibleBiomes,
            double baseStrength,
            double baseAgility,
            double baseVitality,
            double baseEndurance,
            double baseIntelligence,
            double basePerception,
            double baseWillpower,
            double baseAetherCapacity,
            double baseAetherControl,
            double baseSpeed,
            double perceptionRadius,
            double monthlyBiomassConsumption,
            double territoryRadius,
            Set<MonsterThreatRank> allowedThreatRanks,
            double naturalArmor,
            double siegeDamageMultiplier,
            MonsterMaterialType primaryMaterial,
            MonsterMaterialType secondaryMaterial,
            int maxRegionalDensity,
            MonsterReproductionMode reproductionMode,
            int maturityAgeMonths,
            int maximumAgeMonths) {
        this.speciesId = speciesId;
        this.displayName = displayName != null ? displayName : speciesId;
        this.category = category != null ? category : MonsterCategory.BEAST;
        this.compatibleBiomes = Set.copyOf(compatibleBiomes != null ? compatibleBiomes : Collections.emptySet());
        this.baseStrength = baseStrength;
        this.baseAgility = baseAgility;
        this.baseVitality = baseVitality;
        this.baseEndurance = baseEndurance;
        this.baseIntelligence = baseIntelligence;
        this.basePerception = basePerception;
        this.baseWillpower = baseWillpower;
        this.baseAetherCapacity = baseAetherCapacity;
        this.baseAetherControl = baseAetherControl;
        this.baseSpeed = Math.max(1.0, baseSpeed);
        this.perceptionRadius = Math.max(5.0, perceptionRadius);
        this.monthlyBiomassConsumption = Math.max(0.1, monthlyBiomassConsumption);
        this.territoryRadius = Math.max(5.0, territoryRadius);
        this.allowedThreatRanks = Set.copyOf(allowedThreatRanks != null ? allowedThreatRanks : EnumSet.of(MonsterThreatRank.STANDARD));
        this.naturalArmor = Math.max(0.0, naturalArmor);
        this.siegeDamageMultiplier = Math.max(1.0, siegeDamageMultiplier);
        this.primaryMaterial = primaryMaterial != null ? primaryMaterial : MonsterMaterialType.BEAST_HIDE;
        this.secondaryMaterial = secondaryMaterial != null ? secondaryMaterial : MonsterMaterialType.MONSTER_BONE;
        this.maxRegionalDensity = Math.max(10, maxRegionalDensity);
        this.reproductionMode = reproductionMode != null ? reproductionMode : MonsterReproductionMode.PAIR_BREEDING;
        this.maturityAgeMonths = Math.max(1, maturityAgeMonths);
        this.maximumAgeMonths = Math.max(this.maturityAgeMonths + 12, maximumAgeMonths);
    }

    public boolean isCompatibleWithBiome(Biome biome) {
        if (biome == null) return false;
        return compatibleBiomes.contains(biome);
    }

    // --- Getters ---
    public String getSpeciesId() { return speciesId; }
    public String getDisplayName() { return displayName; }
    public MonsterCategory getCategory() { return category; }
    public Set<Biome> getCompatibleBiomes() { return compatibleBiomes; }
    public double getBaseStrength() { return baseStrength; }
    public double getBaseAgility() { return baseAgility; }
    public double getBaseVitality() { return baseVitality; }
    public double getBaseEndurance() { return baseEndurance; }
    public double getBaseIntelligence() { return baseIntelligence; }
    public double getBasePerception() { return basePerception; }
    public double getBaseWillpower() { return baseWillpower; }
    public double getBaseAetherCapacity() { return baseAetherCapacity; }
    public double getBaseAetherControl() { return baseAetherControl; }
    public double getBaseSpeed() { return baseSpeed; }
    public double getPerceptionRadius() { return perceptionRadius; }
    public double getMonthlyBiomassConsumption() { return monthlyBiomassConsumption; }
    public double getTerritoryRadius() { return territoryRadius; }
    public Set<MonsterThreatRank> getAllowedThreatRanks() { return allowedThreatRanks; }
    public double getNaturalArmor() { return naturalArmor; }
    public double getSiegeDamageMultiplier() { return siegeDamageMultiplier; }
    public MonsterMaterialType getPrimaryMaterial() { return primaryMaterial; }
    public MonsterMaterialType getSecondaryMaterial() { return secondaryMaterial; }
    public int getMaxRegionalDensity() { return maxRegionalDensity; }

    public MonsterReproductionMode getReproductionMode() { return reproductionMode; }
    public int getMaturityAgeMonths() { return maturityAgeMonths; }
    public int getMaximumAgeMonths() { return maximumAgeMonths; }
    public boolean isSiegeCapable() { return siegeDamageMultiplier >= 1.5 || category == MonsterCategory.SIEGE_CREATURE; }

    // =========================================================================
    // 10 INITIAL SPECIES REGISTRATION FACTORY
    // =========================================================================

    public static Map<String, MonsterSpeciesDefinition> createInitialCatalog() {
        Map<String, MonsterSpeciesDefinition> catalog = new LinkedHashMap<>();

        // 1. DIRE_WOLF (FOREST — BEAST)
        catalog.put("DIRE_WOLF", new MonsterSpeciesDefinition(
                "DIRE_WOLF", "Dire Wolf", MonsterCategory.BEAST,
                EnumSet.of(Biome.FOREST),
                30.0, 38.0, 28.0, 32.0, 15.0, 35.0, 25.0, 5.0, 5.0,
                20.0, 25.0, 1.2, 20.0,
                EnumSet.of(MonsterThreatRank.MINOR, MonsterThreatRank.STANDARD, MonsterThreatRank.ELITE, MonsterThreatRank.ALPHA),
                2.0, 1.0, MonsterMaterialType.BEAST_HIDE, MonsterMaterialType.MONSTER_BONE, 100,
                MonsterReproductionMode.PAIR_BREEDING, 12, 180
        ));

        // 2. THORN_STALKER (FOREST — ABERRATION)
        catalog.put("THORN_STALKER", new MonsterSpeciesDefinition(
                "THORN_STALKER", "Thorn Stalker", MonsterCategory.ABERRATION,
                EnumSet.of(Biome.FOREST),
                26.0, 42.0, 24.0, 26.0, 18.0, 45.0, 28.0, 12.0, 10.0,
                16.0, 30.0, 1.5, 25.0,
                EnumSet.of(MonsterThreatRank.MINOR, MonsterThreatRank.STANDARD, MonsterThreatRank.ELITE),
                3.0, 1.0, MonsterMaterialType.CHITIN_PLATE, MonsterMaterialType.VENOM_SAC, 80,
                MonsterReproductionMode.PAIR_BREEDING, 10, 144
        ));

        // 3. STONEBACK_BRUTE (HIGHLAND_MINE — CONSTRUCT / SIEGE_CREATURE)
        catalog.put("STONEBACK_BRUTE", new MonsterSpeciesDefinition(
                "STONEBACK_BRUTE", "Stoneback Brute", MonsterCategory.CONSTRUCT,
                EnumSet.of(Biome.HIGHLAND_MINE),
                55.0, 12.0, 50.0, 55.0, 10.0, 18.0, 40.0, 15.0, 10.0,
                8.0, 15.0, 2.0, 25.0,
                EnumSet.of(MonsterThreatRank.STANDARD, MonsterThreatRank.ELITE, MonsterThreatRank.ALPHA),
                14.0, 2.5, MonsterMaterialType.CHITIN_PLATE, MonsterMaterialType.MONSTER_BONE, 60,
                MonsterReproductionMode.ASSEMBLY, 24, 720
        ));

        // 4. ORE_BURROWER (HIGHLAND_MINE — ABERRATION)
        catalog.put("ORE_BURROWER", new MonsterSpeciesDefinition(
                "ORE_BURROWER", "Ore Burrower", MonsterCategory.ABERRATION,
                EnumSet.of(Biome.HIGHLAND_MINE),
                35.0, 25.0, 35.0, 35.0, 14.0, 28.0, 25.0, 18.0, 14.0,
                12.0, 20.0, 1.0, 20.0,
                EnumSet.of(MonsterThreatRank.MINOR, MonsterThreatRank.STANDARD, MonsterThreatRank.ELITE),
                8.0, 1.5, MonsterMaterialType.CHITIN_PLATE, MonsterMaterialType.AETHER_RESIDUE, 90,
                MonsterReproductionMode.PAIR_BREEDING, 12, 240
        ));

        // 5. MIRE_STALKER (RIVER_VALLEY — BEAST)
        catalog.put("MIRE_STALKER", new MonsterSpeciesDefinition(
                "MIRE_STALKER", "Mire Stalker", MonsterCategory.BEAST,
                EnumSet.of(Biome.RIVER_VALLEY),
                34.0, 32.0, 30.0, 30.0, 16.0, 36.0, 22.0, 8.0, 6.0,
                15.0, 22.0, 1.4, 22.0,
                EnumSet.of(MonsterThreatRank.MINOR, MonsterThreatRank.STANDARD, MonsterThreatRank.ELITE),
                4.0, 1.0, MonsterMaterialType.BEAST_HIDE, MonsterMaterialType.VENOM_SAC, 90,
                MonsterReproductionMode.PAIR_BREEDING, 14, 192
        ));

        // 6. MARSH_HYDRA (RIVER_VALLEY — ABERRATION / ALPHA-capable)
        catalog.put("MARSH_HYDRA", new MonsterSpeciesDefinition(
                "MARSH_HYDRA", "Marsh Hydra", MonsterCategory.ABERRATION,
                EnumSet.of(Biome.RIVER_VALLEY),
                48.0, 22.0, 60.0, 50.0, 22.0, 32.0, 42.0, 30.0, 25.0,
                11.0, 30.0, 3.0, 35.0,
                EnumSet.of(MonsterThreatRank.STANDARD, MonsterThreatRank.ELITE, MonsterThreatRank.ALPHA, MonsterThreatRank.ANCIENT),
                7.0, 1.8, MonsterMaterialType.HYDRA_BLOOD, MonsterMaterialType.CHITIN_PLATE, 40,
                MonsterReproductionMode.PAIR_BREEDING, 36, 1200
        ));

        // 7. TIDE_MAW (COASTAL — BEAST)
        catalog.put("TIDE_MAW", new MonsterSpeciesDefinition(
                "TIDE_MAW", "Tide Maw", MonsterCategory.BEAST,
                EnumSet.of(Biome.COASTAL),
                45.0, 26.0, 45.0, 40.0, 14.0, 30.0, 30.0, 12.0, 10.0,
                14.0, 25.0, 2.2, 30.0,
                EnumSet.of(MonsterThreatRank.STANDARD, MonsterThreatRank.ELITE, MonsterThreatRank.ALPHA),
                6.0, 1.2, MonsterMaterialType.BEAST_HIDE, MonsterMaterialType.MONSTER_BONE, 70,
                MonsterReproductionMode.PAIR_BREEDING, 18, 360
        ));

        // 8. AETHER_SIREN (COASTAL — SPIRIT)
        catalog.put("AETHER_SIREN", new MonsterSpeciesDefinition(
                "AETHER_SIREN", "Aether Siren", MonsterCategory.SPIRIT,
                EnumSet.of(Biome.COASTAL),
                20.0, 35.0, 25.0, 25.0, 38.0, 50.0, 40.0, 45.0, 45.0,
                16.0, 35.0, 1.8, 30.0,
                EnumSet.of(MonsterThreatRank.MINOR, MonsterThreatRank.STANDARD, MonsterThreatRank.ELITE, MonsterThreatRank.ALPHA),
                2.0, 1.0, MonsterMaterialType.SIREN_VOCAL_CORD, MonsterMaterialType.AETHER_RESIDUE, 50,
                MonsterReproductionMode.AETHER_SPAWN, 24, 1440
        ));

        // 9. AETHER_WRAITH (All Biomes near High-Aether Lairs — SPIRIT)
        catalog.put("AETHER_WRAITH", new MonsterSpeciesDefinition(
                "AETHER_WRAITH", "Aether Wraith", MonsterCategory.SPIRIT,
                EnumSet.allOf(Biome.class),
                24.0, 38.0, 35.0, 30.0, 42.0, 45.0, 45.0, 55.0, 55.0,
                18.0, 35.0, 1.5, 35.0,
                EnumSet.of(MonsterThreatRank.STANDARD, MonsterThreatRank.ELITE, MonsterThreatRank.ALPHA, MonsterThreatRank.ANCIENT),
                10.0, 1.0, MonsterMaterialType.SPECTRAL_DUST, MonsterMaterialType.AETHER_RESIDUE, 50,
                MonsterReproductionMode.AETHER_SPAWN, 12, 2400
        ));

        // 10. RIFT_BEAST (All Biomes near High-Grade Spatial/Anomaly Lairs — ABERRATION)
        catalog.put("RIFT_BEAST", new MonsterSpeciesDefinition(
                "RIFT_BEAST", "Rift Beast", MonsterCategory.ABERRATION,
                EnumSet.allOf(Biome.class),
                44.0, 48.0, 40.0, 38.0, 30.0, 44.0, 38.0, 45.0, 40.0,
                22.0, 35.0, 2.0, 35.0,
                EnumSet.of(MonsterThreatRank.STANDARD, MonsterThreatRank.ELITE, MonsterThreatRank.ALPHA, MonsterThreatRank.ANCIENT),
                5.0, 1.4, MonsterMaterialType.SPATIAL_SHARD, MonsterMaterialType.AETHER_RESIDUE, 45,
                MonsterReproductionMode.AETHER_SPAWN, 18, 1800
        ));

        return catalog;
    }
}

