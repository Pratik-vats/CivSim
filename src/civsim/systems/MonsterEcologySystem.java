package civsim.systems;

import civsim.ProgressionConfig;
import civsim.SimulationConfig;
import civsim.model.*;
import civsim.spatial.Biome;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * Phase 2B.5 — Master Monster Ecology, Monster Lairs, Generational Evolution,
 * and MonsterTide Invasions System.
 * Coordinates autonomous monster populations, regional prey biomass constraints,
 * mutated lair lifecycles, bounded generational trait evolution, Seer early warnings,
 * multi-wave invasion battles via Phase 2B.1 CombatEngine, breach consequences,
 * and monster core/material recovery logistics.
 */
public class MonsterEcologySystem {

    // Hard Global and Regional Safety Caps
    public static final int MAX_MONSTERS_PER_ORDINARY_LAIR = 100;
    public static final int MAX_MONSTERS_PER_HIGH_GRADE_LAIR = 250;
    public static final int MAX_GLOBAL_MONSTERS = 2000;
    public static final int MAX_GLOBAL_ALPHAS = 12;
    public static final int MAX_GLOBAL_ANCIENTS = 2;

    // Registries
    private final Map<String, MonsterSpeciesDefinition> speciesCatalog = new LinkedHashMap<>();
    private final Map<String, MonsterLair> lairs = new LinkedHashMap<>();
    private final Map<String, Monster> monsters = new LinkedHashMap<>();
    private final Map<String, EcologicalRegionState> ecologicalRegions = new LinkedHashMap<>();
    private final Map<String, MonsterTide> tides = new LinkedHashMap<>();
    private final List<MonsterDropRecord> dropRecords = new ArrayList<>();

    // Idempotency Guards
    private final Set<String> processedLairEventIds = new HashSet<>();
    private final Set<String> processedSpawnEventIds = new HashSet<>();
    private final Set<String> processedDeathEventIds = new HashSet<>();
    private final Set<String> processedDropEventIds = new HashSet<>();
    private final Set<String> processedBreachEventIds = new HashSet<>();

    private final Map<String, Monster> deadMonstersArchive = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Monster> eldest) {
            return size() > 200;
        }
    };

    private void archiveDeadMonster(Monster m) {
        if (m != null) {
            deadMonstersArchive.put(m.getMonsterId(), m);
        }
    }

    private void boundSet(Set<String> set, int maxSize) {
        if (set.size() > maxSize) {
            Iterator<String> it = set.iterator();
            int toRemove = maxSize / 4;
            while (it.hasNext() && toRemove-- > 0) {
                it.next();
                it.remove();
            }
        }
    }

    // Scheduling State
    private long nextScheduledMonsterTideMonth = -1L;
    private int nextMonsterSequence = 1;
    private int nextLairSequence = 1;
    private int nextTideSequence = 1;

    // Annual Telemetry Counters
    private int annualTidesScheduled = 0;
    private int annualTidesDefended = 0;
    private int annualTidesBreached = 0;
    private int annualMonstersSpawned = 0;
    private int annualMonstersSlain = 0;
    private int annualCoresGenerated = 0;
    private int annualMaterialsGenerated = 0;
    private int annualCivilianBreachDeaths = 0;
    private int annualLairsDestroyed = 0;

    public MonsterEcologySystem() {
        // Pre-populate canonical 10 species definitions
        speciesCatalog.putAll(MonsterSpeciesDefinition.createInitialCatalog());
    }

    public synchronized void resetYearlyCounters() {
        annualTidesScheduled = 0;
        annualTidesDefended = 0;
        annualTidesBreached = 0;
        annualMonstersSpawned = 0;
        annualMonstersSlain = 0;
        annualCoresGenerated = 0;
        annualMaterialsGenerated = 0;
        annualCivilianBreachDeaths = 0;
        annualLairsDestroyed = 0;
    }

    public synchronized void clear() {
        lairs.clear();
        monsters.clear();
        deadMonstersArchive.clear();
        ecologicalRegions.clear();
        tides.clear();
        dropRecords.clear();
        processedLairEventIds.clear();
        processedDropEventIds.clear();
        processedBreachEventIds.clear();
        nextScheduledMonsterTideMonth = -1L;
        nextMonsterSequence = 1;
        nextLairSequence = 1;
        nextTideSequence = 1;
        resetYearlyCounters();
    }

    public synchronized int getDeadMonstersArchiveSize() {
        return deadMonstersArchive.size();
    }

    // =========================================================================
    // 1. SPECIES DEFINITION & REGISTRATION
    // =========================================================================

    public synchronized void registerSpecies(MonsterSpeciesDefinition def) {
        if (def != null) {
            speciesCatalog.put(def.getSpeciesId(), def);
        }
    }

    public synchronized MonsterSpeciesDefinition getSpecies(String speciesId) {
        return speciesCatalog.get(speciesId);
    }

    public synchronized Collection<MonsterSpeciesDefinition> getAllSpecies() {
        return Collections.unmodifiableCollection(speciesCatalog.values());
    }

    // =========================================================================
    // 2. MONSTER LAIR CREATION FROM PendingMonsterLairRecord
    // =========================================================================

    /**
     * Idempotently creates a MonsterLair from a PendingMonsterLairRecord emitted when an unclaimed
     * WorldAnomaly passes its 6-month mutation deadline.
     */
    public synchronized MonsterLair createLairFromPendingRecord(
            PendingMonsterLairRecord record,
            Random random,
            int currentTotalMonths) {

        if (record == null) return null;
        if (!processedLairEventIds.add(record.getLairEventId())) {
            // Already processed this mutation event idempotently
            return lairs.get("LAIR_" + record.getAnomalyId());
        }

        String lairId = "LAIR_" + record.getAnomalyId();
        ResourceGrade grade = record.getResourceGrade();
        Biome biome = determineBiomeForCoordinates(record.getPosX(), record.getPosY());

        // Select primary and secondary species compatible with this biome
        String primarySpecies = selectPrimarySpeciesForBiome(biome, random);
        String secondarySpecies = selectSecondarySpecies(biome, grade, random);

        // Grade-based capacity and territory scaling
        int initialCap;
        int seedMin;
        int seedMax;
        double radius;
        MonsterThreatRank dominantRank;

        switch (grade) {
            case MYTHIC:
                initialCap = 250; seedMin = 35; seedMax = 55; radius = 60.0; dominantRank = MonsterThreatRank.ELITE; break;
            case LEGENDARY:
                initialCap = 180; seedMin = 24; seedMax = 40; radius = 48.0; dominantRank = MonsterThreatRank.ELITE; break;
            case RARE:
                initialCap = 100; seedMin = 16; seedMax = 28; radius = 36.0; dominantRank = MonsterThreatRank.STANDARD; break;
            case UNCOMMON:
                initialCap = 85; seedMin = 12; seedMax = 20; radius = 28.0; dominantRank = MonsterThreatRank.STANDARD; break;
            case COMMON:
            default:
                initialCap = 60; seedMin = 8; seedMax = 14; radius = 20.0; dominantRank = MonsterThreatRank.MINOR; break;
        }

        MonsterLair lair = new MonsterLair(
                lairId,
                record.getAnomalyId(),
                record.getPosX(),
                record.getPosY(),
                biome,
                record.getResourceType(),
                grade,
                primarySpecies,
                secondarySpecies,
                initialCap,
                radius,
                dominantRank,
                currentTotalMonths
        );

        lairs.put(lair.getLairId(), lair);

        // Ensure EcologicalRegionState exists
        String regionId = "REGION_" + biome.name() + "_" + ((int) (record.getPosX() / 50.0)) + "_" + ((int) (record.getPosY() / 50.0));
        ecologicalRegions.computeIfAbsent(regionId, k -> EcologicalRegionState.createDefault(regionId, biome));

        // Seed initial monsters according to grade distribution
        int seedCount = (random != null) ? (seedMin + random.nextInt(Math.max(1, seedMax - seedMin + 1))) : seedMin;
        seedLairInitialMonsters(lair, seedCount, grade, random, currentTotalMonths);

        // Schedule first MonsterTide if this is the first active lair
        if (nextScheduledMonsterTideMonth < 0) {
            int delay = (random != null) ? (24 + random.nextInt(37)) : 36; // 24-60 months
            nextScheduledMonsterTideMonth = currentTotalMonths + delay;
        }

        return lair;
    }

    private void seedLairInitialMonsters(
            MonsterLair lair,
            int count,
            ResourceGrade grade,
            Random random,
            int currentTotalMonths) {

        for (int i = 0; i < count; i++) {
            if (getGlobalLivingMonsterCount() >= MAX_GLOBAL_MONSTERS || lair.isAtCapacity()) {
                break;
            }

            MonsterThreatRank rank = rollThreatRankForGrade(grade, random);
            // Check global Alpha & Ancient caps
            if (rank == MonsterThreatRank.ANCIENT && getGlobalLivingAncientCount() >= MAX_GLOBAL_ANCIENTS) {
                rank = MonsterThreatRank.ALPHA;
            }
            if (rank == MonsterThreatRank.ALPHA && (getGlobalLivingAlphaCount() >= MAX_GLOBAL_ALPHAS || countLairAlphas(lair) >= getMaxAlphasPerLair(lair))) {
                rank = MonsterThreatRank.ELITE;
            }

            String speciesId = (i % 4 == 0 && lair.getSecondarySpeciesId() != null) ? lair.getSecondarySpeciesId() : lair.getPrimarySpeciesId();
            MonsterSpeciesDefinition speciesDef = speciesCatalog.get(speciesId);
            if (speciesDef == null) speciesDef = speciesCatalog.get(lair.getPrimarySpeciesId());

            MonsterTraitProfile traits = MonsterTraitProfile.createRandomInitial(random);
            String monsterId = "M_" + (nextMonsterSequence++);
            String displayName = (rank.isApexOrAncient()) ? (rank.getDisplayName() + " " + speciesDef.getDisplayName() + " " + nextMonsterSequence) : null;

            Monster m = new Monster(
                    monsterId,
                    speciesId,
                    displayName,
                    speciesDef != null ? speciesDef.getCategory() : MonsterCategory.BEAST,
                    rank,
                    lair.getPosX() + ((random != null ? random.nextDouble() * 4.0 - 2.0 : 0.0)),
                    lair.getPosY() + ((random != null ? random.nextDouble() * 4.0 - 2.0 : 0.0)),
                    lair.getBiome(),
                    lair.getLairId(),
                    speciesDef,
                    traits,
                    1 // Initial generation
            );

            int initialAge = (random != null)
                    ? (m.getMaturityAgeMonths() + random.nextInt(Math.max(1, (m.getMaximumAgeMonths() - m.getMaturityAgeMonths()) / 2)))
                    : m.getMaturityAgeMonths();
            m.setAgeMonths(initialAge);
            m.setBirthTotalMonths(currentTotalMonths - initialAge);

            monsters.put(m.getMonsterId(), m);
            lair.addMonster(m.getMonsterId());
            annualMonstersSpawned++;
        }
    }

    private MonsterThreatRank rollThreatRankForGrade(ResourceGrade grade, Random random) {
        if (random == null) return MonsterThreatRank.STANDARD;
        double roll = random.nextDouble();

        switch (grade) {
            case MYTHIC:
                if (roll < 0.01) return MonsterThreatRank.ANCIENT;
                if (roll < 0.15) return MonsterThreatRank.ALPHA;
                if (roll < 0.55) return MonsterThreatRank.ELITE;
                if (roll < 0.90) return MonsterThreatRank.STANDARD;
                return MonsterThreatRank.MINOR;
            case LEGENDARY:
                if (roll < 0.08) return MonsterThreatRank.ALPHA;
                if (roll < 0.40) return MonsterThreatRank.ELITE;
                if (roll < 0.85) return MonsterThreatRank.STANDARD;
                return MonsterThreatRank.MINOR;
            case RARE:
                if (roll < 0.02) return MonsterThreatRank.ALPHA;
                if (roll < 0.20) return MonsterThreatRank.ELITE;
                if (roll < 0.70) return MonsterThreatRank.STANDARD;
                return MonsterThreatRank.MINOR;
            case UNCOMMON:
                if (roll < 0.08) return MonsterThreatRank.ELITE;
                if (roll < 0.50) return MonsterThreatRank.STANDARD;
                return MonsterThreatRank.MINOR;
            case COMMON:
            default:
                if (roll < 0.25) return MonsterThreatRank.STANDARD;
                return MonsterThreatRank.MINOR;
        }
    }

    // =========================================================================
    // 3. REGIONAL ECOLOGY, BIOMASS & GENERATIONAL REPRODUCTION
    // =========================================================================

    /**
     * Executes monthly ecological simulation: prey biomass consumption, regeneration,
     * starvation attrition, and generational reproduction.
     */
    public synchronized void processMonthlyEcology(
            int currentYear,
            int currentMonth,
            long currentTotalMonths,
            SimulationConfig config,
            Random random,
            BiConsumer<String, Boolean> eventRecorder) {

        int awakeningStart = (config != null) ? config.getAwakeningStartYear() : 50;
        if (currentYear < awakeningStart || lairs.isEmpty()) {
            return;
        }

        // Step 1A: Monthly Monster Aging & Natural Lifespan
        for (Monster m : new ArrayList<>(monsters.values())) {
            if (m.isAlive()) {
                m.incrementAgeMonths(1);
                if (m.hasExceededLifespan()) {
                    processMonsterDeath(m, "natural aging", m.getPosX(), m.getPosY(), currentYear, currentMonth, random, false);
                }
            }
        }

        // Step 1B: Single-Region Biomass Consumption Accounting & Starvation
        Map<EcologicalRegionState, Double> regionalDemand = new HashMap<>();
        Map<EcologicalRegionState, List<Monster>> regionalLivingMonsters = new HashMap<>();

        for (Monster m : new ArrayList<>(monsters.values())) {
            if (m.isAlive()) {
                EcologicalRegionState region = findRegionForCoordinates(m.getPosX(), m.getPosY(), m.getCurrentBiome());
                MonsterSpeciesDefinition def = speciesCatalog.get(m.getSpeciesId());
                double baseCons = (def != null) ? def.getMonthlyBiomassConsumption() : 1.5;
                double ferocityMod = 1.0 + (m.getTraitProfile().getFerocity() * 0.02);
                double demand = baseCons * ferocityMod;

                regionalDemand.put(region, regionalDemand.getOrDefault(region, 0.0) + demand);
                regionalLivingMonsters.computeIfAbsent(region, k -> new ArrayList<>()).add(m);
            }
        }

        for (EcologicalRegionState region : ecologicalRegions.values()) {
            double demand = regionalDemand.getOrDefault(region, 0.0);
            EcologicalMonthlyResult result = region.processMonthlyConsumption(demand);

            List<Monster> residentMonsters = regionalLivingMonsters.getOrDefault(region, Collections.emptyList());

            if (result.hasStarvation()) {
                for (Monster m : residentMonsters) {
                    m.setStarvedMonths(m.getStarvedMonths() + 1.0);
                    if (m.getStarvedMonths() <= 2.0) {
                        m.setBehaviorState(MonsterBehaviorState.HUNTING);
                    } else if (m.getStarvedMonths() <= 5.0) {
                        m.setBehaviorState(MonsterBehaviorState.MIGRATING);
                        // Bounded starvation attrition (5% max health per month)
                        if (m.getCombatState() != null) {
                            double dmg = m.getCombatState().getMaxHealth() * 0.05;
                            m.getCombatState().setCurrentHealth(Math.max(1.0, m.getCombatState().getCurrentHealth() - dmg));
                        }
                    } else {
                        // Severe persistent starvation (15% max health per month)
                        if (m.getCombatState() != null) {
                            double dmg = m.getCombatState().getMaxHealth() * 0.15;
                            m.getCombatState().setCurrentHealth(m.getCombatState().getCurrentHealth() - dmg);
                            if (m.getCombatState().getCurrentHealth() <= 0.0) {
                                processMonsterDeath(m, "starvation", m.getPosX(), m.getPosY(), currentYear, currentMonth, random, false);
                            }
                        }
                    }
                }

                // If regional starvation is severe, active lairs transition to dormant
                if (region.isDepleted()) {
                    for (MonsterLair lair : lairs.values()) {
                        if (lair.getState() == MonsterLairState.ACTIVE && findRegionForCoordinates(lair.getPosX(), lair.getPosY(), lair.getBiome()) == region) {
                            lair.transitionToDormant();
                        }
                    }
                }
            } else {
                // Biomass healthy: reset starvation pressure & update behavior
                for (Monster m : residentMonsters) {
                    if (m.getStarvedMonths() > 0.0) {
                        m.setStarvedMonths(0.0);
                        m.setBehaviorState(MonsterBehaviorState.FEEDING);
                    } else if (m.getBehaviorState() == MonsterBehaviorState.FEEDING) {
                        m.setBehaviorState(MonsterBehaviorState.RESTING);
                    } else if (m.getBehaviorState() == MonsterBehaviorState.RESTING) {
                        m.setBehaviorState(MonsterBehaviorState.PATROLLING);
                    }
                }

                // Restore dormant lairs when ecological conditions recover
                for (MonsterLair lair : lairs.values()) {
                    if (lair.getState() == MonsterLairState.DORMANT && findRegionForCoordinates(lair.getPosX(), lair.getPosY(), lair.getBiome()) == region) {
                        lair.transitionToActive();
                    }
                }
            }
        }

        // Step 1C: Physical Migration Movement
        for (Monster m : monsters.values()) {
            if (m.isAlive() && m.getBehaviorState() == MonsterBehaviorState.MIGRATING) {
                // Roam within species compatible territory
                double speed = m.getBaseSpeed();
                double angle = (random != null) ? (random.nextDouble() * 2.0 * Math.PI) : 0.0;
                double newX = Math.max(0.0, Math.min(100.0, m.getPosX() + Math.cos(angle) * Math.min(speed, 5.0)));
                double newY = Math.max(0.0, Math.min(100.0, m.getPosY() + Math.sin(angle) * Math.min(speed, 5.0)));
                Biome newBiome = determineBiomeForCoordinates(newX, newY);
                if (m.getSpeciesDef() != null && m.getSpeciesDef().isCompatibleWithBiome(newBiome)) {
                    m.setPosition(newX, newY);
                    m.setCurrentBiome(newBiome);
                }
            }
        }

        // Step 2: Controlled Reproduction in Active Lairs
        for (MonsterLair lair : lairs.values()) {
            if (!lair.canSpawn() || getGlobalLivingMonsterCount() >= MAX_GLOBAL_MONSTERS) {
                continue;
            }

            if (currentTotalMonths < lair.getNextScheduledSpawnMonth()) {
                continue;
            }

            EcologicalRegionState reg = findRegionForCoordinates(lair.getPosX(), lair.getPosY(), lair.getBiome());
            if (reg != null && reg.isDepleted()) {
                // Defer by 3 months during ecological starvation
                lair.setNextScheduledSpawnMonth((int) currentTotalMonths + 3);
                continue;
            }

            spawnNextGenerationInLair(lair, random, (int) currentTotalMonths);
            lair.setLastSpawnMonth((int) currentTotalMonths);
            int nextDelay = (random != null) ? (6 + random.nextInt(13)) : 12; // 6-18 months
            lair.setNextScheduledSpawnMonth((int) currentTotalMonths + nextDelay);
        }

        // Step 3: Advance Active MonsterTides
        processActiveMonsterTides(currentYear, currentMonth, currentTotalMonths, config, eventRecorder);
    }

    private void spawnNextGenerationInLair(MonsterLair lair, Random random, int currentTotalMonths) {
        String spawnEventId = "MONSTER_SPAWN:" + lair.getLairId() + ":" + lair.getNextScheduledSpawnMonth();
        if (!processedSpawnEventIds.add(spawnEventId)) {
            return; // Idempotently processed
        }

        int livingOriginCount = countLairLivingMonsters(lair);
        int spareCap = lair.getPopulationCapacity() - livingOriginCount;
        if (spareCap <= 0) return;

        String speciesId = lair.getPrimarySpeciesId();
        MonsterSpeciesDefinition speciesDef = speciesCatalog.get(speciesId);
        if (speciesDef == null) return;

        // Check reproduction mode requirements
        List<Monster> matureParents = new ArrayList<>();
        int maxParentGen = 1;
        for (String mId : lair.getMonsterIds()) {
            Monster m = monsters.get(mId);
            if (m != null && m.isAlive()) {
                if (m.isMature()) {
                    matureParents.add(m);
                }
                if (m.getEvolutionaryGeneration() > maxParentGen) {
                    maxParentGen = m.getEvolutionaryGeneration();
                }
            }
        }

        MonsterReproductionMode mode = speciesDef.getReproductionMode();
        if (mode == MonsterReproductionMode.PAIR_BREEDING && matureParents.size() < 2) {
            return; // Needs at least 2 mature parents
        } else if (mode == MonsterReproductionMode.AETHER_SPAWN && lair.getAetherSignature() < 15.0) {
            return; // Insufficient aether
        }

        int toSpawn = Math.min(spareCap, Math.min(3, 1 + (random != null ? random.nextInt(3) : 1)));

        List<MonsterTraitProfile> parentProfiles = new ArrayList<>();
        List<String> chosenParentIds = new ArrayList<>();
        if (!matureParents.isEmpty()) {
            for (int p = 0; p < Math.min(2, matureParents.size()); p++) {
                parentProfiles.add(matureParents.get(p).getTraitProfile());
                chosenParentIds.add(matureParents.get(p).getMonsterId());
            }
        }

        int nextGen = maxParentGen + 1;
        double envDrift = (lair.getSourceResourceGrade().getGradeStep() >= 3) ? 0.35 : 0.10;

        for (int i = 0; i < toSpawn; i++) {
            if (getGlobalLivingMonsterCount() >= MAX_GLOBAL_MONSTERS || countLairLivingMonsters(lair) >= lair.getPopulationCapacity()) {
                break;
            }

            MonsterThreatRank rank = rollThreatRankForGrade(lair.getSourceResourceGrade(), random);
            if (rank == MonsterThreatRank.ANCIENT && (getGlobalLivingAncientCount() >= MAX_GLOBAL_ANCIENTS || nextGen < 3)) {
                rank = MonsterThreatRank.ALPHA;
            }
            if (rank == MonsterThreatRank.ALPHA && (getGlobalLivingAlphaCount() >= MAX_GLOBAL_ALPHAS || countLairAlphas(lair) >= getMaxAlphasPerLair(lair))) {
                rank = MonsterThreatRank.ELITE;
            }

            MonsterTraitProfile inheritedTraits = MonsterTraitProfile.inheritFromParents(parentProfiles, random, envDrift);
            String monsterId = "M_" + (nextMonsterSequence++);
            String displayName = (rank.isApexOrAncient()) ? (rank.getDisplayName() + " " + speciesDef.getDisplayName() + " " + nextMonsterSequence) : null;

            Monster child = new Monster(
                    monsterId,
                    speciesId,
                    displayName,
                    speciesDef.getCategory(),
                    rank,
                    lair.getPosX() + (random != null ? random.nextDouble() * 2.0 - 1.0 : 0.0),
                    lair.getPosY() + (random != null ? random.nextDouble() * 2.0 - 1.0 : 0.0),
                    lair.getBiome(),
                    lair.getLairId(),
                    speciesDef,
                    inheritedTraits,
                    nextGen
            );

            child.setBirthTotalMonths(currentTotalMonths);
            child.setAgeMonths(0);
            for (String pid : chosenParentIds) {
                child.addParentMonsterId(pid);
            }

            monsters.put(child.getMonsterId(), child);
            lair.addMonster(child.getMonsterId());
            annualMonstersSpawned++;
        }
    }

    // =========================================================================
    // 4. PERIODIC MONSTER TIDE SCHEDULING & MOBILIZATION
    // =========================================================================

    /**
     * Evaluates scheduled MonsterTide invasions toward human settlements.
     */
    public synchronized void evaluateMonsterTideScheduling(
            int currentYear,
            int currentMonth,
            long currentTotalMonths,
            List<Village> villages,
            List<Person> allPeople,
            Random random,
            BiConsumer<String, Boolean> eventRecorder) {

        if (villages == null || villages.isEmpty() || lairs.isEmpty()) return;
        if (nextScheduledMonsterTideMonth < 0 || currentTotalMonths < nextScheduledMonsterTideMonth) {
            return;
        }

        // Concurrency Guard: At most 1 newly mobilizing or approaching major world tide at a time
        for (MonsterTide t : tides.values()) {
            if (t.getState().isInTransit() || t.getState() == MonsterTideState.MOBILIZING) {
                // Defer by 6 months without burning spawn
                nextScheduledMonsterTideMonth = currentTotalMonths + 6L;
                return;
            }
        }

        // Find candidate active lairs with minimum viable population (>= 12 living monsters)
        List<MonsterLair> eligibleLairs = new ArrayList<>();
        for (MonsterLair lair : lairs.values()) {
            if (lair.getState() == MonsterLairState.ACTIVE && countLairLivingMonsters(lair) >= 12) {
                eligibleLairs.add(lair);
            }
        }

        if (eligibleLairs.isEmpty()) {
            // Defer by 6 months (never spawn free monsters out of thin air)
            nextScheduledMonsterTideMonth = currentTotalMonths + 6L;
            return;
        }

        // Select source lair
        MonsterLair sourceLair = eligibleLairs.get(random != null ? random.nextInt(eligibleLairs.size()) : 0);
        EcologicalRegionState region = findRegionForCoordinates(sourceLair.getPosX(), sourceLair.getPosY(), sourceLair.getBiome());

        // Select target settlement using SettlementAttractionEvaluator
        Village targetVillage = SettlementAttractionEvaluator.selectTargetSettlement(
                sourceLair, villages, region, allPeople, currentTotalMonths, random
        );

        if (targetVillage == null) {
            nextScheduledMonsterTideMonth = currentTotalMonths + 6L;
            return;
        }

        // Mobilize 35% - 65% of source lair's living monsters
        double mobFraction = (random != null) ? (0.35 + (random.nextDouble() * 0.30)) : 0.50;
        int totalLiving = countLairLivingMonsters(sourceLair);
        int mobilizeCount = Math.max(8, (int) Math.round(totalLiving * mobFraction));

        List<String> pool = new ArrayList<>();
        for (String mId : sourceLair.getMonsterIds()) {
            Monster m = monsters.get(mId);
            if (m != null && m.isAlive()) {
                pool.add(mId);
            }
        }

        if (random != null) {
            Collections.shuffle(pool, random);
        }

        List<String> wave1 = new ArrayList<>(); // Minor / Scouts
        List<String> wave2 = new ArrayList<>(); // Standard / Elite
        List<String> wave3 = new ArrayList<>(); // Siege / Alpha / Ancient

        int chosen = 0;
        for (String mId : pool) {
            if (chosen >= mobilizeCount) break;
            Monster m = monsters.get(mId);
            if (m == null || !m.isAlive()) continue;

            if (m.getThreatRank() == MonsterThreatRank.MINOR) {
                wave1.add(mId);
            } else if (m.getThreatRank() == MonsterThreatRank.STANDARD || m.getThreatRank() == MonsterThreatRank.ELITE) {
                if (m.isSiegeCapable()) {
                    wave3.add(mId);
                } else {
                    wave2.add(mId);
                }
            } else {
                wave3.add(mId); // Alpha or Ancient
            }
            chosen++;
        }

        // Ensure at least wave 1 or 2 is non-empty, and wave 1 has vanguard / scouts
        if (wave1.isEmpty() && !wave2.isEmpty()) {
            int advanceCount = Math.max(1, wave2.size() / 3);
            for (int i = 0; i < advanceCount; i++) {
                wave1.add(wave2.remove(0));
            }
        } else if (wave1.isEmpty() && !wave3.isEmpty()) {
            wave1.add(wave3.remove(0));
        }

        // Calculate travel duration using distance and mobilized force speed
        double dx = sourceLair.getPosX() - targetVillage.getX();
        double dy = sourceLair.getPosY() - targetVillage.getY();
        double dist = Math.sqrt(dx * dx + dy * dy);

        double minSpeed = 25.0;
        for (String mId : pool) {
            Monster m = monsters.get(mId);
            if (m != null && m.getBaseSpeed() < minSpeed) {
                minSpeed = m.getBaseSpeed();
            }
        }
        int travelMonths = Math.max(1, (int) Math.ceil(dist / Math.max(8.0, minSpeed)));

        String tideId = "TIDE:" + sourceLair.getLairId() + ":V" + targetVillage.getId() + ":M" + currentTotalMonths;
        MonsterTide tide = new MonsterTide(
                tideId,
                sourceLair.getLairId(),
                targetVillage.getId(),
                targetVillage.getName(),
                targetVillage.getX(),
                targetVillage.getY(),
                (int) (currentTotalMonths / 12),
                (int) (currentTotalMonths % 12),
                currentTotalMonths,
                travelMonths,
                wave1, wave2, wave3
        );

        tides.put(tide.getTideId(), tide);
        sourceLair.setLastInvasionMonth((int) currentTotalMonths);
        annualTidesScheduled++;

        // Schedule next invasion evaluation in 24-60 months
        int nextDelay = (random != null) ? (24 + random.nextInt(37)) : 36;
        nextScheduledMonsterTideMonth = currentTotalMonths + nextDelay;

        // Seer early reconnaissance detection check
        evaluateSeerEarlyWarning(tide, targetVillage, allPeople, random, (int) (currentTotalMonths / 12), eventRecorder);

        if (eventRecorder != null) {
            eventRecorder.accept(
                    String.format("Year %d (Month %d): MonsterTide mobilizing from %s targeting %s (%d monsters, ETA: %d months).",
                            currentYear, currentMonth, sourceLair.getLairId(), targetVillage.getName(), tide.getTotalMobilizedCount(), travelMonths),
                    false
            );
        }
    }

    private void evaluateSeerEarlyWarning(
            MonsterTide tide,
            Village targetVillage,
            List<Person> allPeople,
            Random random,
            int currentYear,
            BiConsumer<String, Boolean> eventRecorder) {

        if (allPeople == null || tide == null || targetVillage == null) return;

        for (Person p : allPeople) {
            if (p != null && p.isAlive() && targetVillage.equals(p.getVillage()) && p.isAwakened()) {
                if (p.getAetherProfile() != null && p.getAetherProfile().getSupernaturalClass() == SupernaturalClass.SEER) {
                    double per = p.getAetherProfile().getEffectivePerception();
                    double mastery = p.getAetherProfile().getMastery();
                    double chance = 0.50 + ((per - 20.0) / 100.0) + (mastery / 200.0);

                    if (random == null || random.nextDouble() < chance) {
                        int lead = Math.min(tide.getTravelDurationMonths(), 1 + (random != null ? random.nextInt(3) : 1));
                        tide.markDetectedBySeer(lead);

                        // Resource-backed settlement preparation: spend Ore/Wood/Tools to repair fortifications
                        if (targetVillage.getWoodStockpile() >= 20.0 && targetVillage.getIronStoneStockpile() >= 10.0) {
                            targetVillage.deductWood(20.0);
                            targetVillage.deductIronStone(10.0);
                            targetVillage.setFortificationLevel(targetVillage.getFortificationLevel() + 0.5);
                        }

                        if (eventRecorder != null) {
                            eventRecorder.accept(
                                    String.format("Year %d: Seer %s foresaw incoming MonsterTide! Settlement %s fortified defenses.",
                                            currentYear, p.getName(), targetVillage.getName()),
                                    false
                            );
                        }
                        break;
                    }
                }
            }
        }
    }

    private void processActiveMonsterTides(
            int currentYear,
            int currentMonth,
            long currentTotalMonths,
            SimulationConfig config,
            BiConsumer<String, Boolean> eventRecorder) {

        for (MonsterTide tide : tides.values()) {
            if (tide.getState().isInTransit()) {
                if (currentTotalMonths >= tide.getArrivalTotalMonths()) {
                    tide.setState(MonsterTideState.INVADING);
                }
            }
        }
    }

    // =========================================================================
    // 5. COMBINED-ARMS DEFENSE & MULTI-WAVE COMBAT RESOLUTION
    // =========================================================================

    /**
     * Resolves multi-wave MonsterTide invasion tactical battles using Phase 2B.1 CombatEngine.
     */
    public synchronized MonsterTideOutcome resolveMonsterTideInvasion(
            MonsterTide tide,
            Village targetVillage,
            List<Person> allPeople,
            SimulationConfig simConfig,
            ProgressionConfig progConfig,
            DemographicsSystem demographicsSystem,
            AwakeningSystem awakeningSystem,
            ProgressionSystem progressionSystem,
            Random random,
            List<Couple> allCouples,
            BiConsumer<String, Boolean> eventRecorder,
            int currentYear,
            int currentMonth) {

        if (tide == null || targetVillage == null || tide.getState().isTerminal()) {
            return null;
        }

        // Assemble defending combatants from living residents present at the settlement
        List<Combatant> defenders = new ArrayList<>();
        Map<String, Person> defenderPeopleMap = new HashMap<>();

        if (allPeople != null) {
            for (Person p : allPeople) {
                if (p != null && p.isAlive() && targetVillage.equals(p.getVillage())) {
                    // Check if eligible combatant: Soldier, Awakened, or able-bodied adult with weapon
                    boolean isCombatant = (p.getOccupation() == Occupation.SOLDIER) || p.isAwakened();
                    if (isCombatant) {
                        CombatState cs = p.getCombatState();
                        if (cs == null || (!cs.isDead() && !cs.isIncapacitated() && cs.getCurrentHealth() > 0.0)) {
                            PersonCombatant pc = new PersonCombatant(p, "DEFENDERS");
                            // Fortification armor bonus
                            pc.setEquipmentDefenseBonus(targetVillage.getFortificationLevel() * 2.0);
                            defenders.add(pc);
                            defenderPeopleMap.put(String.valueOf(p.getId()), p);
                        }
                    }
                }
            }
        }

        if (defenders.isEmpty()) {
            // Immediate breach aftermath
            return resolveBreachAftermath(tide, targetVillage, allPeople, demographicsSystem, simConfig, eventRecorder, currentYear);
        }

        List<List<String>> waves = Arrays.asList(
                tide.getWave1MonsterIds(),
                tide.getWave2MonsterIds(),
                tide.getWave3MonsterIds()
        );

        boolean breachOccurred = false;
        int waveIdx = 0;

        for (List<String> waveMonsterIds : waves) {
            if (waveMonsterIds.isEmpty()) continue;
            waveIdx++;

            // Gather active monster combatants for this wave
            List<Combatant> waveMonsters = new ArrayList<>();
            boolean alphaOrAncientPresent = false;

            for (String mId : waveMonsterIds) {
                Monster m = monsters.get(mId);
                if (m != null && m.isAlive()) {
                    m.setSideId("MONSTERS");
                    waveMonsters.add(m);
                    if (m.getThreatRank().isApexOrAncient()) {
                        alphaOrAncientPresent = true;
                    }
                }
            }

            if (waveMonsters.isEmpty()) {
                continue;
            }

            // Filter active defenders (persisting wounds from previous wave)
            List<Combatant> activeDefenders = new ArrayList<>();
            for (Combatant d : defenders) {
                if (d.canActInCombat()) {
                    activeDefenders.add(d);
                }
            }

            if (activeDefenders.isEmpty()) {
                breachOccurred = true;
                break;
            }

            List<Combatant> battleCombatants = new ArrayList<>();
            battleCombatants.addAll(activeDefenders);
            battleCombatants.addAll(waveMonsters);

            String battleId = "MONSTER_TIDE:" + tide.getTideId() + ":W" + waveIdx;
            BattleRequest request = new BattleRequest(
                    battleId,
                    currentYear,
                    currentMonth,
                    BattleType.MONSTER_INVASION,
                    targetVillage.getX(),
                    targetVillage.getY(),
                    1.2, // Settlement defensive terrain bonus
                    battleCombatants
            );

            BattleResult result = CombatEngine.resolveBattle(
                    request,
                    random,
                    progConfig,
                    demographicsSystem,
                    awakeningSystem,
                    progressionSystem,
                    simConfig,
                    allCouples,
                    eventRecorder,
                    15
            );

            // Process monster casualties & generate drops
            for (Combatant mc : waveMonsters) {
                if (mc instanceof Monster) {
                    Monster m = (Monster) mc;
                    if (!m.isAlive() || (m.getCombatState() != null && m.getCombatState().isDead())) {
                        processMonsterDeath(m, "combat", targetVillage.getX(), targetVillage.getY(), currentYear, currentMonth, random, true);
                        tide.addSlainMonster(m.getMonsterId());
                    } else if (m.getCombatState() != null && m.getCombatState().isEscaped()) {
                        tide.addSurvivingRetreatingMonster(m.getMonsterId());
                    }
                }
            }

            // Check if defenders were wiped out in this wave
            boolean anyDefenderAlive = false;
            for (Combatant d : activeDefenders) {
                if (d.canActInCombat()) {
                    anyDefenderAlive = true;
                    break;
                }
            }

            if (!anyDefenderAlive) {
                breachOccurred = true;
                break;
            }

            // Stage 2A.1 Crimson Crucible rolls for surviving unawakened mortal defenders
            if (awakeningSystem != null) {
                for (Combatant d : activeDefenders) {
                    if (d.getPerson() != null && !d.isAwakened() && d.canActInCombat()) {
                        String eventId = "CRUCIBLE:" + tide.getTideId() + ":" + d.getCombatantId();
                        awakeningSystem.attemptCrimsonCrucibleCombat(
                                d.getPerson(),
                                currentYear,
                                eventId,
                                random,
                                simConfig,
                                progConfig,
                                eventRecorder
                        );
                    }
                }
            }
        }

        // Update target settlement cooldown
        targetVillage.setLastMonsterTideMonth((currentYear * 12L) + currentMonth);
        tide.setState(MonsterTideState.RESOLVED);

        if (breachOccurred) {
            annualTidesBreached++;
            return resolveBreachAftermath(tide, targetVillage, allPeople, demographicsSystem, simConfig, eventRecorder, currentYear);
        } else {
            annualTidesDefended++;
            int initialDefenders = defenders.size();
            int survivingDefenders = 0;
            for (Combatant d : defenders) {
                if (d.canActInCombat()) survivingDefenders++;
            }
            boolean costly = (initialDefenders > 0 && ((double) survivingDefenders / initialDefenders) <= 0.50);
            MonsterTideOutcome defenseOutcome = costly ? MonsterTideOutcome.COSTLY_VICTORY : MonsterTideOutcome.SUCCESSFUL_DEFENSE;
            tide.setOutcome(defenseOutcome);

            // Recover drops directly to defending village
            recoverDropsForSettlement(targetVillage, targetVillage.getX(), targetVillage.getY(), 10.0);

            if (eventRecorder != null) {
                eventRecorder.accept(
                        String.format("Year %d (Month %d): Settlement %s successfully repulsed MonsterTide %s!",
                                currentYear, currentMonth, targetVillage.getName(), tide.getTideId()),
                        true
                );
            }
            return MonsterTideOutcome.SUCCESSFUL_DEFENSE;
        }
    }

    private MonsterTideOutcome resolveBreachAftermath(
            MonsterTide tide,
            Village targetVillage,
            List<Person> allPeople,
            DemographicsSystem demographicsSystem,
            SimulationConfig simConfig,
            BiConsumer<String, Boolean> eventRecorder,
            int currentYear) {

        String breachEventId = "BREACH:" + tide.getTideId();
        if (!processedBreachEventIds.add(breachEventId)) {
            return tide.getOutcome() != null ? tide.getOutcome() : MonsterTideOutcome.PARTIAL_BREACH;
        }

        // 1. Structural Damage (15% - 50%)
        double damagePct = 25.0;
        targetVillage.damageFortification(damagePct);
        tide.setStructuralDamagePercent(damagePct);

        // 2. Food Destruction (15% - 40%)
        double foodToDestroy = targetVillage.getFoodStockpile() * 0.30;
        targetVillage.deductFood(foodToDestroy);
        tide.setFoodDestroyed(foodToDestroy);

        // 3. Civilian Exposure Casualties committed through DemographicsSystem
        int civilianCasualties = 0;
        if (allPeople != null && demographicsSystem != null) {
            for (Person p : allPeople) {
                if (p != null && p.isAlive() && targetVillage.equals(p.getVillage()) && !p.isAwakened() && p.getOccupation() != Occupation.SOLDIER) {
                    // Check exposure survival
                    if (civilianCasualties < 8) {
                        civilianCasualties++;
                        String cause = "killed in monster invasion (" + tide.getTideId() + ")";
                        demographicsSystem.killPerson(p, Collections.emptyList(), currentYear, cause, simConfig, eventRecorder);
                    }
                }
            }
        }

        tide.setCivilianFatalities(civilianCasualties);
        annualCivilianBreachDeaths += civilianCasualties;

        MonsterTideOutcome outcome = (targetVillage.getLivingPopulationCount() <= 0)
                ? MonsterTideOutcome.SETTLEMENT_OVERRUN
                : MonsterTideOutcome.PARTIAL_BREACH;

        tide.setOutcome(outcome);

        if (eventRecorder != null) {
            eventRecorder.accept(
                    String.format("Year %d: BREACH at %s by MonsterTide! Fortifications damaged, %.1f food lost, %d civilians slain.",
                            currentYear, targetVillage.getName(), foodToDestroy, civilianCasualties),
                    true
            );
        }

        return outcome;
    }

    // =========================================================================
    // 6. UNIFIED AUTHORITATIVE MONSTER DEATH & DROP LOGISTICS
    // =========================================================================

    public synchronized MonsterDropRecord processMonsterDeath(
            Monster monster,
            String cause,
            double deathPosX,
            double deathPosY,
            int currentYear,
            int currentMonth,
            Random random,
            boolean generateDrops) {

        if (monster == null || !monster.isAlive()) {
            return null;
        }

        String deathEventId = "MONSTER_DEATH:" + monster.getMonsterId();
        if (!processedDeathEventIds.add(deathEventId)) {
            return null; // Already dead idempotently
        }

        monster.setAlive(false);
        monster.setDeathTotalMonths((currentYear * 12) + currentMonth);
        monster.setDeathCause(cause != null ? cause : "unknown");
        monster.setBehaviorState(MonsterBehaviorState.DEAD);
        annualMonstersSlain++;

        // Memory optimization: unregister dead monster from active map and lair
        monsters.remove(monster.getMonsterId());
        archiveDeadMonster(monster);
        if (monster.getOriginLairId() != null) {
            MonsterLair originLair = lairs.get(monster.getOriginLairId());
            if (originLair != null) {
                originLair.removeMonster(monster.getMonsterId());
            }
        }
        boundSet(processedDeathEventIds, 1000);

        if (generateDrops) {
            return generateMonsterDrop(monster, deathPosX, deathPosY, currentYear, currentMonth, random);
        }
        return null;
    }

    public synchronized MonsterDropRecord generateMonsterDrop(
            Monster monster,
            double posX,
            double posY,
            int year,
            int month,
            Random random) {

        if (monster == null) return null;
        String dropId = "MONSTER_DROP:" + monster.getMonsterId();
        if (!processedDropEventIds.add(dropId)) {
            return null; // Already dropped idempotently
        }

        MonsterSpeciesDefinition def = speciesCatalog.get(monster.getSpeciesId());
        MonsterThreatRank rank = monster.getThreatRank();

        boolean dropsCore = false;
        double roll = (random != null) ? random.nextDouble() : 0.0;
        if (roll < rank.getBaseCoreDropChance()) {
            dropsCore = true;
        }

        MonsterMaterialType matType;
        ResourceGrade coreGrade = null;
        double quantity = 1.0;

        if (dropsCore) {
            matType = MonsterMaterialType.MONSTER_CORE;
            coreGrade = rank.getBaseCoreGrade();
            annualCoresGenerated++;
        } else {
            matType = (def != null && random != null && random.nextBoolean())
                    ? def.getPrimaryMaterial()
                    : MonsterMaterialType.BEAST_HIDE;
            quantity = (rank.isApexOrAncient()) ? 4.0 : 1.0;
            annualMaterialsGenerated++;
        }

        MonsterDropRecord record = new MonsterDropRecord(
                dropId,
                monster.getMonsterId(),
                monster.getSpeciesId(),
                rank,
                matType,
                coreGrade,
                quantity,
                posX, posY,
                year, month
        );

        dropRecords.add(record);
        return record;
    }

    public synchronized void recoverDropsForSettlement(Village village, double posX, double posY, double radius) {
        if (village == null) return;
        for (MonsterDropRecord drop : dropRecords) {
            if (!drop.isRecovered()) {
                double dx = drop.getPosX() - posX;
                double dy = drop.getPosY() - posY;
                if (Math.sqrt(dx * dx + dy * dy) <= radius) {
                    drop.markRecovered("VILLAGE_" + village.getId());
                    if (drop.isCore()) {
                        village.addMonsterCore((int) drop.getQuantity());
                    } else {
                        village.addMonsterMaterial(drop.getMaterialType(), drop.getQuantity());
                    }
                }
            }
        }
    }

    // =========================================================================
    // 7. EXPLICIT LAIR ASSAULT EXPEDITIONS
    // =========================================================================

    public synchronized BattleResult assaultMonsterLair(
            MonsterLair lair,
            List<Person> assaultTeam,
            Random random,
            ProgressionConfig progConfig,
            DemographicsSystem demographicsSystem,
            AwakeningSystem awakeningSystem,
            ProgressionSystem progressionSystem,
            SimulationConfig simConfig,
            List<Couple> allCouples,
            BiConsumer<String, Boolean> eventRecorder,
            int currentYear,
            int currentMonth) {

        if (lair == null || assaultTeam == null || assaultTeam.isEmpty()) return null;
        if (lair.getState().isTerminal()) return null;

        List<Combatant> defenders = new ArrayList<>();
        for (String mId : lair.getMonsterIds()) {
            Monster m = monsters.get(mId);
            if (m != null && m.isAlive()) {
                m.setSideId("DEFENDING_MONSTERS");
                defenders.add(m);
            }
        }

        if (defenders.isEmpty()) {
            lair.transitionToDestroyed();
            annualLairsDestroyed++;
            return null;
        }

        List<Combatant> attackers = new ArrayList<>();
        for (Person p : assaultTeam) {
            if (p != null && p.isAlive()) {
                attackers.add(new PersonCombatant(p, "ASSAULT_FORCE"));
            }
        }

        List<Combatant> all = new ArrayList<>();
        all.addAll(attackers);
        all.addAll(defenders);

        String battleId = "LAIR_ASSAULT:" + lair.getLairId() + ":M" + ((currentYear * 12) + currentMonth);
        BattleRequest request = new BattleRequest(
                battleId,
                currentYear,
                currentMonth,
                BattleType.ANOMALY_SKIRMISH,
                lair.getPosX(),
                lair.getPosY(),
                0.9,
                all
        );

        BattleResult result = CombatEngine.resolveBattle(
                request,
                random,
                progConfig,
                demographicsSystem,
                awakeningSystem,
                progressionSystem,
                simConfig,
                allCouples,
                eventRecorder,
                15
        );

        // Process monster deaths & check lair status
        int remainingDefenders = 0;
        for (Combatant mc : defenders) {
            if (mc instanceof Monster) {
                Monster m = (Monster) mc;
                if (!m.isAlive() || (m.getCombatState() != null && m.getCombatState().isDead())) {
                    processMonsterDeath(m, "combat (lair assault)", lair.getPosX(), lair.getPosY(), currentYear, currentMonth, random, true);
                    lair.removeMonster(m.getMonsterId());
                } else {
                    remainingDefenders++;
                }
            }
        }

        if (remainingDefenders == 0 && "ASSAULT_FORCE".equals(result.getVictoriousSideId())) {
            lair.transitionToDestroyed();
            annualLairsDestroyed++;

            // Displace any living monsters that originated from this lair but were deployed away
            for (Monster m : monsters.values()) {
                if (m.isAlive() && lair.getLairId().equals(m.getOriginLairId())) {
                    m.setDisplaced(true);
                    m.setBehaviorState(MonsterBehaviorState.PATROLLING);
                }
            }
        }

        return result;
    }

    // =========================================================================
    // 8. TELEMETRY & ANNUAL REPORT GENERATION
    // =========================================================================

    public synchronized String generateAnnualSummary(int currentYear) {
        if (lairs.isEmpty() && monsters.isEmpty()) {
            return null;
        }

        int livingMonsters = getGlobalLivingMonsterCount();
        int activeLairs = 0;
        int dormantLairs = 0;
        int suppressedLairs = 0;
        int destroyedLairs = 0;

        for (MonsterLair lair : lairs.values()) {
            switch (lair.getState()) {
                case ACTIVE: activeLairs++; break;
                case DORMANT: dormantLairs++; break;
                case SUPPRESSED: suppressedLairs++; break;
                case DESTROYED:
                case COLLAPSED: destroyedLairs++; break;
                default: break;
            }
        }

        int livingAlphas = getGlobalLivingAlphaCount();
        int livingAncients = getGlobalLivingAncientCount();

        return String.format(
                "Monsters[Living:%d Lairs(Act:%d Drm:%d Sup:%d Dest:%d) Alpha:%d Anc:%d] | Tides[Yr:%d Vic:%d Brch:%d] | Drops[Cores:%d Mat:%d]",
                livingMonsters, activeLairs, dormantLairs, suppressedLairs, destroyedLairs,
                livingAlphas, livingAncients,
                annualTidesScheduled, annualTidesDefended, annualTidesBreached,
                annualCoresGenerated, annualMaterialsGenerated
        );
    }

    // =========================================================================
    // 9. HELPER & QUERY METHODS
    // =========================================================================

    public synchronized int getGlobalLivingMonsterCount() {
        int count = 0;
        for (Monster m : monsters.values()) {
            if (m.isAlive()) count++;
        }
        return count;
    }

    public synchronized int getGlobalLivingAlphaCount() {
        int count = 0;
        for (Monster m : monsters.values()) {
            if (m.isAlive() && m.getThreatRank() == MonsterThreatRank.ALPHA) count++;
        }
        return count;
    }

    public synchronized int getGlobalLivingAncientCount() {
        int count = 0;
        for (Monster m : monsters.values()) {
            if (m.isAlive() && m.getThreatRank() == MonsterThreatRank.ANCIENT) count++;
        }
        return count;
    }

    private int countLairLivingMonsters(MonsterLair lair) {
        if (lair == null) return 0;
        int count = 0;
        for (Monster m : monsters.values()) {
            if (m.isAlive() && lair.getLairId().equals(m.getOriginLairId())) count++;
        }
        return count;
    }

    private int countLairAlphas(MonsterLair lair) {
        if (lair == null) return 0;
        int count = 0;
        for (Monster m : monsters.values()) {
            if (m.isAlive() && lair.getLairId().equals(m.getOriginLairId()) && m.getThreatRank().isApexOrAncient()) count++;
        }
        return count;
    }

    private int getMaxAlphasPerLair(MonsterLair lair) {
        if (lair == null) return 1;
        return (lair.getSourceResourceGrade() == ResourceGrade.LEGENDARY || lair.getSourceResourceGrade() == ResourceGrade.MYTHIC) ? 2 : 1;
    }

    private Biome determineBiomeForCoordinates(double x, double y) {
        if (x < 40.0 && y < 40.0) return Biome.RIVER_VALLEY;
        if (x > 60.0 && y > 60.0) return Biome.HIGHLAND_MINE;
        if (x > 50.0 && y < 40.0) return Biome.COASTAL;
        return Biome.FOREST;
    }

    private String selectPrimarySpeciesForBiome(Biome biome, Random random) {
        switch (biome != null ? biome : Biome.FOREST) {
            case HIGHLAND_MINE:
                return (random != null && random.nextBoolean()) ? "STONEBACK_BRUTE" : "ORE_BURROWER";
            case RIVER_VALLEY:
                return (random != null && random.nextBoolean()) ? "MARSH_HYDRA" : "MIRE_STALKER";
            case COASTAL:
                return (random != null && random.nextBoolean()) ? "TIDE_MAW" : "AETHER_SIREN";
            case FOREST:
            default:
                return (random != null && random.nextBoolean()) ? "DIRE_WOLF" : "THORN_STALKER";
        }
    }

    private String selectSecondarySpecies(Biome biome, ResourceGrade grade, Random random) {
        if (grade == ResourceGrade.MYTHIC || grade == ResourceGrade.LEGENDARY) {
            return (random != null && random.nextBoolean()) ? "AETHER_WRAITH" : "RIFT_BEAST";
        }
        return null;
    }

    private EcologicalRegionState findRegionForCoordinates(double x, double y, Biome biome) {
        String regionId = "REGION_" + biome.name() + "_" + ((int) (x / 50.0)) + "_" + ((int) (y / 50.0));
        return ecologicalRegions.computeIfAbsent(regionId, k -> EcologicalRegionState.createDefault(regionId, biome));
    }

    // Getters for Registries
    public synchronized int getTotalLivingMonsterCount() { return getGlobalLivingMonsterCount(); }
    public synchronized Collection<MonsterLair> getAllLairs() { return Collections.unmodifiableCollection(lairs.values()); }
    public synchronized List<MonsterLair> getActiveLairs() {
        List<MonsterLair> active = new ArrayList<>();
        for (MonsterLair lair : lairs.values()) {
            if (lair.isActive()) active.add(lair);
        }
        return active;
    }
    public synchronized Map<String, MonsterLair> getLairs() { return Collections.unmodifiableMap(lairs); }
    public synchronized Map<String, Monster> getMonsters() { return Collections.unmodifiableMap(monsters); }
    public synchronized Map<String, MonsterTide> getTides() { return Collections.unmodifiableMap(tides); }
    public synchronized List<MonsterDropRecord> getDropRecords() { return Collections.unmodifiableList(dropRecords); }
    public synchronized Map<String, EcologicalRegionState> getEcologicalRegions() { return Collections.unmodifiableMap(ecologicalRegions); }

    public synchronized MonsterLair getLair(String lairId) { return lairs.get(lairId); }
    public synchronized Monster getMonster(String monsterId) {
        Monster m = monsters.get(monsterId);
        return (m != null) ? m : deadMonstersArchive.get(monsterId);
    }
    public synchronized MonsterTide getTide(String tideId) { return tides.get(tideId); }

    public synchronized long getNextScheduledMonsterTideMonth() { return nextScheduledMonsterTideMonth; }
    public synchronized void setNextScheduledMonsterTideMonth(long month) { this.nextScheduledMonsterTideMonth = month; }
}
