package civsim.test;

import civsim.AnnualReport;
import civsim.ProgressionConfig;
import civsim.SimulationConfig;
import civsim.SimulationEngine;
import civsim.model.*;
import civsim.spatial.Biome;
import civsim.systems.AnomalyExplorationSystem;
import civsim.systems.DemographicsSystem;
import civsim.systems.OrganizationSystem;
import civsim.systems.ProgressionSystem;

import java.util.*;

/**
 * Phase 2B.3 Automated Acceptance Test Suite.
 * Validates Rare Resource Anomalies, Scheduling, Spatial Placement, Two-Stage Appearance,
 * Seer Precognition, Information Privacy & Sharing, Expedition Planning & Garrison Safeguards,
 * Travel Timing, Contention Hand-off to 2B.4 (No Combat), Uncontested Securing, Extraction,
 * Strict 4-Bucket Resource Conservation, and 6-Month Mutation to MONSTER_LAIR_PENDING.
 */
public class TestStage2B3AnomalyExploration {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   PHASE 2B.3 TEST SUITE: RARE RESOURCE ANOMALIES & EXPEDITION EXPLORATION");
        System.out.println("================================================================================");

        try {
            runSuite1_EraGatingSchedulingAndSpatialPlacement();
            runSuite2_TwoStageAppearanceSeerPrecognitionAndSharing();
            runSuite3_ExpeditionDispatchGarrisonSafeguardsAndTravel();
            runSuite4_ContentionSecuringExtractionAndConservation();
            runSuite5_SixMonthUnclaimedMutationToMonsterLair();

            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(String.format("TOTAL TESTS: %d | PASSED: %d | FAILED: %d",
                    testsPassed + testsFailed, testsPassed, testsFailed));
            System.out.println("--------------------------------------------------------------------------------");

            if (testsFailed > 0) {
                System.err.println("FAILED: " + testsFailed + " tests failed!");
                System.exit(1);
            } else {
                System.out.println("ALL STAGE 2B.3 ACCEPTANCE TESTS PASSED SUCCESSFULLY!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void assertTrue(String message, boolean condition) {
        if (condition) {
            testsPassed++;
            System.out.println("  [PASS] " + message);
        } else {
            testsFailed++;
            System.err.println("  [FAIL] " + message);
        }
    }

    private static void assertEquals(String message, Object expected, Object actual) {
        if (Objects.equals(expected, actual)) {
            testsPassed++;
            System.out.println("  [PASS] " + message + " (Expected: " + expected + ", Got: " + actual + ")");
        } else {
            testsFailed++;
            System.err.println("  [FAIL] " + message + " (Expected: " + expected + ", Got: " + actual + ")");
        }
    }

    private static void assertDoubleEquals(String message, double expected, double actual, double delta) {
        if (Math.abs(expected - actual) <= delta) {
            testsPassed++;
            System.out.println("  [PASS] " + message + " (" + actual + " ~= " + expected + ")");
        } else {
            testsFailed++;
            System.err.println("  [FAIL] " + message + " (Expected: " + expected + ", Got: " + actual + ")");
        }
    }

    // --- Helper Factory Methods ---

    private static Village createVillage(int id, String name, double x, double y, Biome biome) {
        return new Village(id, name, 1.0, 0.05, x, y, biome);
    }

    private static Person createPerson(int id, String name, int age, Village v, Occupation occ, double wealth) {
        Person p = new Person(id, name, Gender.MALE, age, -age, v, null, null, null, wealth);
        Family f = new Family(id, "Family-" + id, 0, p);
        p.setFamily(f);
        p.setOccupation(occ != null ? occ : Occupation.FARMER);
        if (v != null) {
            v.addResident(p);
        }
        return p;
    }

    private static void awakenPerson(Person p, SupernaturalClass sc, int tier, double perception) {
        AetherProfile ap = AetherProfile.createForTesting(sc, ClassRarity.RARE, tier, false);
        p.setAetherProfile(ap);
    }

    // =========================================================================
    // SUITE 1: ERA GATING, SCHEDULING & SPATIAL PLACEMENT
    // =========================================================================
    private static void runSuite1_EraGatingSchedulingAndSpatialPlacement() {
        System.out.println("\n--- SUITE 1: Era Gating, Scheduling & Spatial Placement Tests ---");

        AnomalyExplorationSystem system = new AnomalyExplorationSystem();
        SimulationConfig config = SimulationConfig.createDefault();
        Random random = new Random(12345L);

        Village v1 = createVillage(1, "Solitude", 25.0, 25.0, Biome.RIVER_VALLEY);
        Village v2 = createVillage(2, "Stormhold", 85.0, 85.0, Biome.HIGHLAND_MINE);
        List<Village> villages = Arrays.asList(v1, v2);

        // 1.1 Mundane Era (Year 0 to 49): Zero supernatural anomalies scheduled or spawned
        for (int yr = 0; yr < 50; yr++) {
            for (int m = 0; m < 12; m++) {
                long totalM = (long) yr * 12 + m;
                system.evaluateMonthlySpawning(yr, m, totalM, villages, random, config, null);
            }
        }
        assertEquals("Zero anomalies spawned during Mundane Era (Years 0-49)", 0, system.getAllAnomalies().size());

        // 1.2 Year 50+: Scheduled spawning initiates deterministically
        for (int yr = 50; yr < 70; yr++) {
            for (int m = 0; m < 12; m++) {
                long totalM = (long) yr * 12 + m;
                system.evaluateMonthlySpawning(yr, m, totalM, villages, random, config, null);
            }
        }
        assertTrue("Anomalies spawned in Awakening Era (Year 50+)", system.getAllAnomalies().size() > 0);

        // 1.3 Active Anomaly Cap (Max 5 across the world)
        assertTrue("Simultaneously active anomalies <= 5 at all times", system.countActiveAnomalies() <= AnomalyExplorationSystem.MAX_ACTIVE_ANOMALIES);

        // 1.4 Spatial placement constraints
        for (WorldAnomaly a : system.getAllAnomalies()) {
            // Check separation from settlements (>= 8.0 units)
            for (Village v : villages) {
                double d = Math.hypot(v.getX() - a.getPosX(), v.getY() - a.getPosY());
                assertTrue(String.format("Anomaly %s at (%.1f, %.1f) is >= 8.0 from settlement %s (dist: %.1f)",
                        a.getAnomalyId(), a.getPosX(), a.getPosY(), v.getName(), d), d >= AnomalyExplorationSystem.MIN_DISTANCE_FROM_SETTLEMENT - 0.001);
            }
        }

        // 1.5 Resource Grade and Quantity Multipliers
        for (WorldAnomaly a : system.getAllAnomalies()) {
            assertTrue("Anomaly initial quantity >= 1", a.getInitialQuantity() >= 1);
            assertEquals("Initial quantity equals remaining quantity at spawn", a.getInitialQuantity(), a.getRemainingQuantity());
            assertTrue("Resource grade assigned", a.getResourceGrade() != null);
            assertTrue("Supernatural resource category assigned", a.getSupernaturalResourceType() != null);
        }
    }

    // =========================================================================
    // SUITE 2: TWO-STAGE APPEARANCE, SEER PRECOGNITION & SHARING
    // =========================================================================
    private static void runSuite2_TwoStageAppearanceSeerPrecognitionAndSharing() {
        System.out.println("\n--- SUITE 2: Two-Stage Appearance, Seer Precognition & Information Sharing Tests ---");

        AnomalyExplorationSystem system = new AnomalyExplorationSystem();
        SimulationConfig config = SimulationConfig.createDefault();
        ProgressionConfig progConfig = ProgressionConfig.createDefault();
        ProgressionSystem progSys = new ProgressionSystem();
        OrganizationSystem orgSys = new OrganizationSystem();
        Random random = new Random(42L);

        Village v1 = createVillage(1, "Solitude", 20.0, 20.0, Biome.RIVER_VALLEY);
        Person seerPerson = createPerson(1, "Eldric the Seer", 35, v1, Occupation.MERCHANT, 100.0);
        awakenPerson(seerPerson, SupernaturalClass.SEER, 3, 25.0);
        List<Person> people = Collections.singletonList(seerPerson);
        orgSys.setPeopleSupplier(() -> people);

        // Spawn a manual anomaly in FORMING state at Month 600
        WorldAnomaly formingAnomaly = new WorldAnomaly(
                "ANOM_SEER_TEST", "Veil of Aether", 28.0, 28.0, Biome.RIVER_VALLEY,
                SupernaturalResourceType.RAW_AETHER_CRYSTAL, ResourceGrade.RARE, 80, 600
        );
        system.registerAnomaly(formingAnomaly);

        assertEquals("Anomaly starts in FORMING state", AnomalyState.FORMING, formingAnomaly.getState());
        assertEquals("Public appearance month is formation + 2 (602)", 602, formingAnomaly.getPublicAppearanceMonth());
        assertTrue("Invisible to non-Seers while FORMING", !formingAnomaly.isPubliclyObservable());

        // 2.1 Seer Precognition during FORMING window (Month 600 -> 2 months before public appearance at 602)
        system.processMonthlyAppearanceAndDetection(50, 0, 600L, people, Collections.singletonList(v1),
                orgSys, progSys, progConfig, random, null);

        List<DiscoveryRecord> seerDiscoveries = system.getAllDiscoveries();
        assertTrue("Seer detected forming anomaly via SEER_PRECOGNITION", !seerDiscoveries.isEmpty());
        DiscoveryRecord seerRecord = seerDiscoveries.get(0);
        assertEquals("Detection method is SEER_PRECOGNITION", DetectionMethod.SEER_PRECOGNITION, seerRecord.getMethod());
        assertEquals("Discoverer person ID matches Seer", String.valueOf(seerPerson.getId()), seerRecord.getDiscovererPersonId());
        assertTrue("Bounded confidence in [0.25, 1.0]", seerRecord.getInformationConfidence() >= 0.25 && seerRecord.getInformationConfidence() <= 1.0);

        // Seer received Stage 2A.4 progression event
        assertTrue("Seer progression event processed", progSys.getEventsProcessedThisYear() > 0);

        // 2.2 Entitlement Isolation: Unknown to other factions without information sharing
        assertTrue("Discovered anomaly is known to Seer's village", formingAnomaly.isKnownByFaction("VILLAGE_1"));
        assertTrue("Discovered anomaly is NOT known to unrelated faction", !formingAnomaly.isKnownByFaction("VILLAGE_2"));

        // 2.3 Explicit Information Sharing (InformationShareEvent)
        InformationShareEvent shareEvt = new InformationShareEvent(
                "SHARE_01", seerRecord.getDiscoveryId(), formingAnomaly.getAnomalyId(),
                "VILLAGE_1", "VILLAGE_2", 601, 0.80
        );
        boolean shared = system.shareDiscovery(shareEvt, orgSys, null);
        assertTrue("Information shared with Village 2", shared);
        assertTrue("Anomaly now known by Village 2 after explicit sharing", formingAnomaly.isKnownByFaction("VILLAGE_2"));

        // 2.4 Transition to REVEALED at Month 602
        system.processMonthlyAppearanceAndDetection(50, 2, 602L, people, Collections.singletonList(v1),
                orgSys, progSys, progConfig, random, null);
        assertTrue("Anomaly transitioned to public observation at publicAppearanceMonth", formingAnomaly.isPubliclyObservable());
    }

    // =========================================================================
    // SUITE 3: EXPEDITION DISPATCH, GARRISON SAFEGUARDS & TRAVEL
    // =========================================================================
    private static void runSuite3_ExpeditionDispatchGarrisonSafeguardsAndTravel() {
        System.out.println("\n--- SUITE 3: Expedition Dispatch, Garrison Safeguards & Travel Timing Tests ---");

        AnomalyExplorationSystem system = new AnomalyExplorationSystem();
        OrganizationSystem orgSys = new OrganizationSystem();

        Village v1 = createVillage(1, "Solitude", 20.0, 20.0, Biome.RIVER_VALLEY);
        v1.getStockpile().addFood(500.0); // plentiful food

        // Add 6 soldiers to village
        List<Person> people = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            people.add(createPerson(i, "Soldier-" + i, 25, v1, Occupation.SOLDIER, 50.0));
        }
        orgSys.setPeopleSupplier(() -> people);

        // Register revealed anomaly at (40.0, 20.0) -> distance exactly 20.0 units
        WorldAnomaly anomaly = new WorldAnomaly(
                "ANOM_TRAVEL_TEST", "Golden Vein", 40.0, 20.0, Biome.RIVER_VALLEY,
                SupernaturalResourceType.ANOMALOUS_ORE, ResourceGrade.UNCOMMON, 50, 600
        );
        anomaly.transitionToRevealed();
        system.registerAnomaly(anomaly);

        // Village 1 discovers the anomaly
        DiscoveryRecord disc = new DiscoveryRecord(
                "DISC_V1", anomaly.getAnomalyId(), null, "VILLAGE_1", 602,
                DetectionMethod.DIRECT_SCOUTING, 1.0, ResourceGrade.UNCOMMON, true, 40.0, 20.0, false
        );
        system.registerDiscovery(disc, anomaly);

        // 3.1 Expedition Dispatch Evaluation
        double foodBeforeDispatch = v1.getStockpile().getFoodStockpile();
        system.evaluateAndDispatchExpeditions(50, 2, 602L, people, Collections.singletonList(v1), orgSys, null);

        List<Expedition> exps = system.getAllExpeditions();
        assertEquals("Exactly one expedition dispatched", 1, exps.size());
        Expedition exp = exps.get(0);

        assertEquals("Expedition status is EN_ROUTE_TO_ANOMALY", ExpeditionStatus.EN_ROUTE_TO_ANOMALY, exp.getStatus());
        assertEquals("Distance = 20.0 units -> Exactly 1 month travel duration", 1, exp.getOneWayTravelMonths());
        assertEquals("ETA arrival total month is 603", 603L, exp.getArrivalTotalMonth());

        // 3.2 Food Rations Deducted from origin settlement
        // Round trip = 2 * 1 month * 3 participants * 1.2 food = 7.2 food
        assertDoubleEquals("Food rations deducted atomically", foodBeforeDispatch - 7.2, v1.getStockpile().getFoodStockpile(), 0.01);

        // 3.3 Mandatory Garrison Rule: At least max(2, 35% of soldiers) remained at home
        int remainingAtHome = 6 - exp.getRosterPersonIds().size();
        assertTrue("Home garrison retainment satisfied (at least 2 soldiers stayed behind)", remainingAtHome >= 2);

        // 3.4 Travel & Arrival at Month 603
        system.processMonthlyTravelAndContention(50, 3, 603L, orgSys, Collections.singletonList(v1), null);
        assertEquals("Expedition arrived at site at Month 603", ExpeditionStatus.SECURING_SITE, exp.getStatus());
        assertEquals("Anomaly transitioned to SECURING", AnomalyState.SECURING, anomaly.getState());
    }

    // =========================================================================
    // SUITE 4: CONTENTION, SECURING, EXTRACTION & CONSERVATION
    // =========================================================================
    private static void runSuite4_ContentionSecuringExtractionAndConservation() {
        System.out.println("\n--- SUITE 4: Contention Check, Extraction & Strict 4-Bucket Resource Conservation Tests ---");

        AnomalyExplorationSystem system = new AnomalyExplorationSystem();
        OrganizationSystem orgSys = new OrganizationSystem();

        Village v1 = createVillage(1, "Solitude", 20.0, 20.0, Biome.RIVER_VALLEY);
        Village v2 = createVillage(2, "Stormhold", 80.0, 80.0, Biome.HIGHLAND_MINE);
        List<Village> villages = Arrays.asList(v1, v2);

        WorldAnomaly anomaly = new WorldAnomaly(
                "ANOM_CONSERVE_TEST", "Radiant Aether Well", 50.0, 50.0, Biome.RIVER_VALLEY,
                SupernaturalResourceType.RAW_AETHER_CRYSTAL, ResourceGrade.COMMON, 30, 600
        );
        anomaly.transitionToRevealed();
        system.registerAnomaly(anomaly);

        // 4.1 Uncontested Securing -> Controlled -> Extraction
        Expedition exp1 = new Expedition(
                "EXP_UNCONTESTED", "VILLAGE_1", "SETTLEMENT", anomaly.getAnomalyId(),
                20.0, 20.0, ClaimObjective.EXCLUSIVE_CLAIM, Arrays.asList("1", "2"),
                100.0, 20.0, 50, 2
        );
        exp1.setStatus(ExpeditionStatus.PRESENT_AT_ANOMALY);
        exp1.setOneWayTravelMonths(2);
        system.registerExpedition(exp1);
        anomaly.addActiveExpedition(exp1.getExpeditionId());

        // Month 602: Securing initiates
        system.processMonthlyTravelAndContention(50, 2, 602L, orgSys, villages, null);
        assertEquals("Anomaly is SECURING", AnomalyState.SECURING, anomaly.getState());

        // Month 603: Securing completes -> CONTROLLED & Extraction begins
        system.processMonthlyTravelAndContention(50, 3, 603L, orgSys, villages, null);
        assertEquals("Anomaly transitioned to CONTROLLED", AnomalyState.CONTROLLED, anomaly.getState());
        assertTrue("Expedition extracted resources", exp1.getCarriedResources() > 0.0);

        // Strict 4-Bucket Conservation holds mid-extraction
        ResourceConservationSnapshot snap1 = system.validateConservation(anomaly.getAnomalyId());
        assertTrue("Conservation Law holds during extraction: Init == Rem + Carried + Delivered + Mutated",
                snap1.isConserved(0.001));

        // Month 604: Finish extraction to depletion
        system.processMonthlyTravelAndContention(50, 4, 604L, orgSys, villages, null);
        assertEquals("Deposit completely DEPLETED", AnomalyState.DEPLETED, anomaly.getState());
        assertEquals("Expedition started return journey upon depletion", ExpeditionStatus.RETURNING, exp1.getStatus());

        // Month 606: Expedition arrives home and delivers supernatural resources
        system.processMonthlyTravelAndContention(50, 6, 606L, orgSys, villages, null);
        assertEquals("Expedition disbanded upon return delivery", ExpeditionStatus.DISBANDED, exp1.getStatus());

        double deliveredToVillage = v1.getSupernaturalInventory().getAmount(SupernaturalResourceType.RAW_AETHER_CRYSTAL);
        assertEquals("Entire 30.0 units delivered to Village 1 inventory", 30.0, deliveredToVillage);

        ResourceConservationSnapshot snapFinal = system.validateConservation(anomaly.getAnomalyId());
        assertTrue("Final 4-Bucket Conservation holds perfectly with 0 discrepancy", snapFinal.isConserved(0.001));
        assertDoubleEquals("Discrepancy is 0.0", 0.0, snapFinal.getDiscrepancy(), 0.0001);

        // 4.2 Multi-Rival Contention -> Handoff to 2B.4 without combat
        WorldAnomaly contestedAnomaly = new WorldAnomaly(
                "ANOM_RIVAL_TEST", "Contested Rift", 50.0, 50.0, Biome.RIVER_VALLEY,
                SupernaturalResourceType.CONDENSED_AETHER_ESSENCE, ResourceGrade.RARE, 40, 610
        );
        contestedAnomaly.transitionToRevealed();
        system.registerAnomaly(contestedAnomaly);

        Expedition rivalA = new Expedition("EXP_RIVAL_A", "VILLAGE_1", "SETTLEMENT", contestedAnomaly.getAnomalyId(),
                20.0, 20.0, ClaimObjective.EXCLUSIVE_CLAIM, Collections.singletonList("1"), 50.0, 10.0, 50, 10);
        rivalA.setStatus(ExpeditionStatus.PRESENT_AT_ANOMALY);

        Expedition rivalB = new Expedition("EXP_RIVAL_B", "VILLAGE_2", "SETTLEMENT", contestedAnomaly.getAnomalyId(),
                80.0, 80.0, ClaimObjective.EXCLUSIVE_CLAIM, Collections.singletonList("3"), 50.0, 10.0, 50, 10);
        rivalB.setStatus(ExpeditionStatus.PRESENT_AT_ANOMALY);

        system.registerExpedition(rivalA);
        system.registerExpedition(rivalB);
        contestedAnomaly.addActiveExpedition(rivalA.getExpeditionId());
        contestedAnomaly.addActiveExpedition(rivalB.getExpeditionId());

        system.processMonthlyTravelAndContention(50, 10, 610L, orgSys, villages, null);
        assertEquals("Anomaly transitioned to CONTESTED upon rival arrival", AnomalyState.CONTESTED, contestedAnomaly.getState());

        List<AnomalyContestRecord> contests = system.getPendingContests();
        assertTrue("Created AnomalyContestRecord queued for 2B.4", !contests.isEmpty());
        AnomalyContestRecord latestContest = contests.get(contests.size() - 1);
        assertEquals("Contest state is PENDING_RESOLUTION (NO combat executed in 2B.3)",
                ContestState.PENDING_RESOLUTION, latestContest.getState());
    }

    // =========================================================================
    // SUITE 5: SIX-MONTH UNCLAIMED MUTATION TO MONSTER LAIR
    // =========================================================================
    private static void runSuite5_SixMonthUnclaimedMutationToMonsterLair() {
        System.out.println("\n--- SUITE 5: 6-Month Unclaimed Mutation to MONSTER_LAIR_PENDING Tests ---");

        AnomalyExplorationSystem system = new AnomalyExplorationSystem();
        SimulationConfig config = SimulationConfig.createDefault();
        Random random = new Random(999L);

        // Spawn anomaly at Month 600.
        // Public appearance at Month 602.
        // Unclaimed mutation deadline = 602 + 6 = Month 608.
        WorldAnomaly unclaimedAnomaly = new WorldAnomaly(
                "ANOM_MUTATION_TEST", "Abandoned Aether Fissure", 60.0, 60.0, Biome.HIGHLAND_MINE,
                SupernaturalResourceType.RAW_AETHER_CRYSTAL, ResourceGrade.RARE, 45, 600
        );
        system.registerAnomaly(unclaimedAnomaly);

        assertEquals("Formation month = 600", 600, unclaimedAnomaly.getFormationMonth());
        assertEquals("Public appearance month = 602", 602, unclaimedAnomaly.getPublicAppearanceMonth());
        assertEquals("Mutation deadline month = 608", 608, unclaimedAnomaly.getUnclaimedExpirationMonth());

        // Advance to Month 607 (5 months post-manifestation): still active and not mutated
        system.processMonthlyAppearanceAndDetection(50, 7, 607L, Collections.emptyList(),
                Collections.emptyList(), null, null, null, random, null);
        assertTrue("Still active prior to 6-month deadline", unclaimedAnomaly.getState() != AnomalyState.MONSTER_LAIR_PENDING);

        // Advance to Month 608 (6 months post-manifestation): Mutation deadline strikes!
        system.processMonthlyAppearanceAndDetection(50, 8, 608L, Collections.emptyList(),
                Collections.emptyList(), null, null, null, random, null);

        assertEquals("Anomaly transitioned to MONSTER_LAIR_PENDING",
                AnomalyState.MONSTER_LAIR_PENDING, unclaimedAnomaly.getState());

        List<PendingMonsterLairRecord> lairs = system.getPendingLairs();
        assertEquals("Exactly one PendingMonsterLairRecord emitted for Phase 2B.5", 1, lairs.size());
        PendingMonsterLairRecord lair = lairs.get(0);

        assertEquals("Lair record matches anomaly ID", unclaimedAnomaly.getAnomalyId(), lair.getAnomalyId());
        assertEquals("Remaining quantity transferred to mutated/abandoned bucket", 45.0, lair.getRemainingQuantityAtMutation());
        assertEquals("Mutation month recorded correctly", 608, lair.getMutationMonth());

        // Conservation Law verified with mutated/abandoned bucket
        ResourceConservationSnapshot snap = system.validateConservation(unclaimedAnomaly.getAnomalyId());
        assertTrue("Conservation Law holds after monster lair mutation", snap.isConserved(0.001));
        assertEquals("Mutated or abandoned total equals initial quantity", 45.0, snap.getExplicitlyAbandonedOrMutated());
    }
}
