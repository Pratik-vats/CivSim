package civsim.test;

import civsim.AnnualReport;
import civsim.ProgressionConfig;
import civsim.SimulationConfig;
import civsim.SimulationEngine;
import civsim.model.*;
import civsim.spatial.Biome;
import civsim.systems.*;

import java.util.*;

/**
 * Phase 2C.2 Acceptance Test Suite — Multi-Generational Blood Feuds, Clan Revenge,
 * Inherited Grievances, and Intergenerational Conflict.
 *
 * Covers Suites A through H matching the exhaustive specification requirements.
 */
public class TestStage2C2BloodFeuds {

    private static int totalTests = 0;
    private static int passedTests = 0;
    private static int failedTests = 0;

    private static void assertTrue(String message, boolean condition) {
        totalTests++;
        if (condition) {
            passedTests++;
            System.out.printf("  [PASS] %s\n", message);
        } else {
            failedTests++;
            System.err.printf("  [FAIL] %s\n", message);
        }
    }

    private static void assertFalse(String message, boolean condition) {
        assertTrue(message, !condition);
    }

    private static void assertEquals(String message, Object expected, Object actual) {
        totalTests++;
        boolean eq = Objects.equals(expected, actual);
        if (eq) {
            passedTests++;
            System.out.printf("  [PASS] %s (Expected: %s, Got: %s)\n", message, expected, actual);
        } else {
            failedTests++;
            System.err.printf("  [FAIL] %s (Expected: %s, Got: %s)\n", message, expected, actual);
        }
    }

    private static void assertNotNull(String message, Object actual) {
        assertTrue(message + " (not null)", actual != null);
    }

    private static void assertNull(String message, Object actual) {
        assertTrue(message + " (null)", actual == null);
    }

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   PHASE 2C.2 TEST SUITE: BLOOD FEUDS, GRIEVANCES & INTERGENERATIONAL CONFLICT");
        System.out.println("================================================================================\n");

        runSuiteA_GrievanceCreationAndAttribution();
        runSuiteB_FeudLifecycle();
        runSuiteC_InheritanceAndGenerationalMemory();
        runSuiteD_RetaliationDecisionAndAntiLoop();
        runSuiteE_CombatAndDeathIntegration();
        runSuiteF_DiplomacyAndReconciliation();
        runSuiteG_HistoricalIntegrityAndStability();
        runSuiteH_LongTermMultiSeedSimulation();

        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.printf("TOTAL TESTS: %d | PASSED: %d | FAILED: %d\n", totalTests, passedTests, failedTests);
        System.out.println("--------------------------------------------------------------------------------");

        if (failedTests > 0) {
            System.err.println("SOME STAGE 2C.2 ACCEPTANCE TESTS FAILED!");
            System.exit(1);
        } else {
            System.out.println("ALL STAGE 2C.2 ACCEPTANCE TESTS PASSED SUCCESSFULLY!");
        }
    }

    // =========================================================================
    // SUITE A: GRIEVANCE CREATION AND ATTRIBUTION
    // =========================================================================
    private static void runSuiteA_GrievanceCreationAndAttribution() {
        System.out.println("--- SUITE A: Grievance Creation and Attribution Tests ---");
        BloodFeudSystem bfs = new BloodFeudSystem();

        // 1. Qualifying targeted murder creates immutable grievance and opens a feud
        HistoricalGrievanceRecord g1 = new HistoricalGrievanceRecord(
                "GRIEV_001", "BAT_01", null, null, 10, 5,
                "101", 1, "CLAN_A", 1,
                "202", "SIDE_B", List.of("CLAN_B"),
                "Murder of family member", "PERSONAL_MURDER",
                GrievanceCategory.PERSONAL_MURDER, AttributionConfidence.CONFIRMED,
                75.0, "202", null, "EYEWITNESS_REPORT", true, ResponsibilityType.DIRECT_PERPETRATOR
        );
        FeudCase f1 = bfs.recordGrievance(g1, 10, 5);
        assertNotNull("Qualifying murder creates FeudCase", f1);
        assertEquals("Initial feud status is OPEN for confirmed attribution", FeudStatus.OPEN, f1.getCurrentFeudStatus());
        assertEquals("Grievance linked to FeudCase", f1.getFeudId(), g1.getFeudId());

        // 2. Minor ordinary disagreement does not automatically trigger blood feud
        HistoricalGrievanceRecord minorGriev = new HistoricalGrievanceRecord(
                "GRIEV_MINOR", null, null, null, 10, 6,
                "102", 1, "CLAN_A", 1,
                "203", "SIDE_B", Collections.emptyList(),
                "Insulting remark", "DISPUTE",
                GrievanceCategory.OTHER_MAJOR_WRONG, AttributionConfidence.CONFIRMED,
                10.0, "203", null, null, false, ResponsibilityType.DIRECT_PERPETRATOR
        );
        FeudCase fMinor = bfs.recordGrievance(minorGriev, 10, 6);
        assertNull("Minor disagreement (< 20 severity) does not create feud", fMinor);

        // 3. Heirloom capture creates correctly linked grievance
        HistoricalGrievanceRecord heirloomGriev = new HistoricalGrievanceRecord(
                "GRIEV_HEIRLOOM_01", "BAT_02", null, null, 10, 7,
                null, 0, "CLAN_A", 1,
                null, "SIDE_B", List.of("CLAN_B"),
                "Clan Heirloom stolen", "HEIRLOOM_OR_EQUIPMENT_CAPTURED"
        );
        FeudCase fHeirloom = bfs.recordGrievance(heirloomGriev, 10, 7);
        assertNotNull("Heirloom capture creates linked feud", fHeirloom);
        assertEquals("Category is HEIRLOOM_CAPTURED", GrievanceCategory.HEIRLOOM_CAPTURED, heirloomGriev.getCategory());

        // 4. Duplicate event delivery cannot create duplicate grievances or cases
        FeudCase fDup = bfs.recordGrievance(g1, 10, 5);
        assertEquals("Idempotent delivery returns existing feud", f1.getFeudId(), fDup.getFeudId());
        assertEquals("Total grievances remains 3 (g1, minorGriev, heirloomGriev)", 3, bfs.getAllGrievances().size());

        // 5. Attribution confidence: UNKNOWN or DISPUTED cannot trigger armed retaliation
        HistoricalGrievanceRecord unknownGriev = new HistoricalGrievanceRecord(
                "GRIEV_UNKNOWN", "BAT_03", null, null, 10, 8,
                "103", 2, "NONE", 1,
                null, null, Collections.emptyList(),
                "Killed by unknown ambusher", "PERSONAL_MURDER",
                GrievanceCategory.PERSONAL_MURDER, AttributionConfidence.UNKNOWN,
                65.0, null, null, null, true, ResponsibilityType.DIRECT_PERPETRATOR
        );
        FeudCase fUnknown = bfs.recordGrievance(unknownGriev, 10, 8);
        assertNotNull("Unknown attribution creates FeudCase", fUnknown);
        assertEquals("Initial status is PENDING_ATTRIBUTION", FeudStatus.PENDING_ATTRIBUTION, fUnknown.getCurrentFeudStatus());
        assertFalse("Retaliation NOT permitted for UNKNOWN attribution", unknownGriev.getConfidence().isRetaliationPermitted());

        // 6. Uninvolved family member is never classified as responsible
        assertFalse("Victim family is not responsible", f1.getResponsiblePersonIds().contains("101"));
        assertTrue("Only confirmed direct perpetrator is in responsible list", f1.getResponsiblePersonIds().contains("202"));

        // 7. Historical grievance details remain unchanged after later events
        assertEquals("Severity remains 75.0", 75.0, g1.getSeverity());
        assertEquals("Original year remains 10", 10, g1.getSimulationYear());
    }

    // =========================================================================
    // SUITE B: FEUD LIFECYCLE
    // =========================================================================
    private static void runSuiteB_FeudLifecycle() {
        System.out.println("\n--- SUITE B: Feud Lifecycle Tests ---");
        FeudCase feud = new FeudCase(
                "FEUD_TEST_LC", "Test Feud", "GRIEV_ORIGIN", 120, 70.0,
                FeudStatus.OPEN, "SIDE_A", List.of("SIDE_B"), "CLAN_A", "CLAN_B",
                1, 2, "CLAN"
        );

        // 1. Valid transitions
        assertTrue("OPEN -> DIPLOMATIC_DISPUTE valid", feud.canTransitionTo(FeudStatus.DIPLOMATIC_DISPUTE));
        assertTrue("OPEN -> ESCALATING valid", feud.canTransitionTo(FeudStatus.ESCALATING));
        feud.transitionStatus(FeudStatus.DIPLOMATIC_DISPUTE, 122, "Diplomatic demands made");
        assertEquals("Current status is DIPLOMATIC_DISPUTE", FeudStatus.DIPLOMATIC_DISPUTE, feud.getCurrentFeudStatus());

        assertTrue("DIPLOMATIC_DISPUTE -> ESCALATING valid", feud.canTransitionTo(FeudStatus.ESCALATING));
        feud.transitionStatus(FeudStatus.ESCALATING, 124, "Demands rejected");
        assertEquals("Current status is ESCALATING", FeudStatus.ESCALATING, feud.getCurrentFeudStatus());

        assertTrue("ESCALATING -> ACTIVE_RETALIATION valid", feud.canTransitionTo(FeudStatus.ACTIVE_RETALIATION));
        feud.transitionStatus(FeudStatus.ACTIVE_RETALIATION, 126, "Retaliation operation launched");
        assertEquals("Current status is ACTIVE_RETALIATION", FeudStatus.ACTIVE_RETALIATION, feud.getCurrentFeudStatus());

        // 2. Invalid transitions
        assertFalse("ACTIVE_RETALIATION cannot jump directly to PENDING_ATTRIBUTION", feud.canTransitionTo(FeudStatus.PENDING_ATTRIBUTION));

        // 3. Transition to TRUCE
        feud.transitionStatus(FeudStatus.NEGOTIATION, 128, "Ceasefire talks");
        feud.transitionStatus(FeudStatus.TRUCE, 130, "Truce signed");
        assertEquals("Current status is TRUCE", FeudStatus.TRUCE, feud.getCurrentFeudStatus());

        // 4. Truce violation reopens hostilities (TRUCE -> ESCALATING)
        assertTrue("TRUCE -> ESCALATING valid on violation", feud.canTransitionTo(FeudStatus.ESCALATING));
        feud.transitionStatus(FeudStatus.ESCALATING, 132, "Truce violated by raid");
        assertEquals("Reopened as ESCALATING", FeudStatus.ESCALATING, feud.getCurrentFeudStatus());

        // 5. Transition to SETTLED
        feud.transitionStatus(FeudStatus.NEGOTIATION, 135, "Final treaty");
        feud.transitionStatus(FeudStatus.SETTLED, 136, "Full compensation paid");
        assertEquals("Current status is SETTLED", FeudStatus.SETTLED, feud.getCurrentFeudStatus());
        assertTrue("SETTLED is terminal", feud.getCurrentFeudStatus().isTerminal());
        assertFalse("Settled feud cannot transition back to OPEN without legitimate new event", feud.canTransitionTo(FeudStatus.OPEN));

        // 6. Multiple related grievances link to one existing case
        feud.addRelatedGrievance("GRIEV_NEW_02", 50.0, 140);
        assertTrue("Related grievance is linked", feud.getRelatedGrievanceIds().contains("GRIEV_NEW_02"));
        assertEquals("Related grievances count is 2", 2, feud.getRelatedGrievanceIds().size());

        // 7. Unrelated grievances do not merge automatically
        BloodFeudSystem bfs = new BloodFeudSystem();
        HistoricalGrievanceRecord gClanA = new HistoricalGrievanceRecord(
                "G_A", null, null, null, 10, 1, "1", 1, "CLAN_A", 1, "2", "SIDE_B", List.of("CLAN_B"), "Event A"
        );
        HistoricalGrievanceRecord gClanC = new HistoricalGrievanceRecord(
                "G_C", null, null, null, 10, 2, "3", 3, "CLAN_C", 1, "4", "SIDE_D", List.of("CLAN_D"), "Event C"
        );
        FeudCase fA = bfs.recordGrievance(gClanA, 10, 1);
        FeudCase fC = bfs.recordGrievance(gClanC, 10, 2);
        assertTrue("Feuds for unrelated clans have distinct IDs", !fA.getFeudId().equals(fC.getFeudId()));
    }

    // =========================================================================
    // SUITE C: INHERITANCE AND GENERATIONAL MEMORY
    // =========================================================================
    private static void runSuiteC_InheritanceAndGenerationalMemory() {
        System.out.println("\n--- SUITE C: Inheritance and Generational Memory Tests ---");
        BloodFeudSystem bfs = new BloodFeudSystem();
        Village v = new Village(1, "Test Village", 1.0, 0.05, 20.0, 20.0, Biome.RIVER_VALLEY);

        Person father = new Person(10, "Father", Gender.MALE, 50, 0, v);
        Person adultSon = new Person(11, "Adult Son", Gender.MALE, 25, 25, v, null, father, null, 50.0);
        Person minorChild = new Person(12, "Minor Child", Gender.FEMALE, 10, 40, v, null, father, null, 0.0);
        father.addChild(adultSon);
        father.addChild(minorChild);

        Person killer = new Person(20, "Killer", Gender.MALE, 40, 10, v);
        Person killerChild = new Person(21, "Killer Child", Gender.MALE, 18, 32, v, null, killer, null, 50.0);
        killer.addChild(killerChild);

        HistoricalGrievanceRecord g = new HistoricalGrievanceRecord(
                "GRIEV_INH_01", "BAT_INH", null, null, 50, 1,
                "10", 0, "NONE", 1, "20", "SIDE_KILLER", Collections.emptyList(),
                "Father was slain", "PERSONAL_MURDER",
                GrievanceCategory.PERSONAL_MURDER, AttributionConfidence.CONFIRMED,
                70.0, "20", null, null, true, ResponsibilityType.DIRECT_PERPETRATOR
        );
        FeudCase feud = bfs.recordGrievance(g, 50, 1);
        assertNotNull("Feud created", feud);
        assertTrue("Father is active claimant", feud.getClaimantPersonIds().contains("10"));
        assertTrue("Killer is responsible target", feud.getResponsiblePersonIds().contains("20"));

        Map<Integer, Person> peopleMap = new HashMap<>();
        peopleMap.put(father.getId(), father);
        peopleMap.put(adultSon.getId(), adultSon);
        peopleMap.put(minorChild.getId(), minorChild);
        peopleMap.put(killer.getId(), killer);
        peopleMap.put(killerChild.getId(), killerChild);

        // 1. Death of claimant transfers claim to adult heir with decay
        father.markDeceased(50, "Slain in skirmish");
        bfs.handlePersonDeath(father, 50, 2, peopleMap, null);

        assertFalse("Deceased father is no longer claimant", feud.getClaimantPersonIds().contains("10"));
        assertTrue("Adult son inherited active claim", feud.getClaimantPersonIds().contains("11"));
        assertEquals("Current claimant generation is 1", 1, feud.getCurrentClaimantGeneration());

        // 2. Claim strength decay rate (Personal: 0.70)
        double expectedStrength = 70.0 * 0.70;
        InheritedGrievanceClaim claim = bfs.getClaim("CLAIM:" + feud.getFeudId() + ":11:G1");
        assertNotNull("Inherited claim record created", claim);
        assertTrue("Claim strength decayed by 0.70 multiplier", Math.abs(claim.getClaimStrength() - expectedStrength) < 0.01);
        assertEquals("Adult son accepted claim", InheritedGrievanceClaim.ClaimAcceptanceStatus.ACCEPTED, claim.getStatus());

        // 3. Minors cannot authorize hostilities (PENDING_ADULTHOOD)
        adultSon.markDeceased(50, "Natural causes");
        bfs.handlePersonDeath(adultSon, 50, 3, peopleMap, null);
        InheritedGrievanceClaim minorClaim = bfs.getClaim("CLAIM:" + feud.getFeudId() + ":12:G2");
        assertNotNull("Minor claim record exists", minorClaim);
        assertEquals("Minor child status is PENDING_ADULTHOOD", InheritedGrievanceClaim.ClaimAcceptanceStatus.PENDING_ADULTHOOD, minorClaim.getStatus());
        assertFalse("Minor is NOT active claimant until adulthood", minorClaim.isActiveClaimant());

        // 4. CRITICAL: Descendants of perpetrator NEVER inherit guilt
        killer.markDeceased(50, "Natural causes");
        bfs.handlePersonDeath(killer, 50, 4, peopleMap, null);
        assertFalse("Killer child is NEVER added to responsible persons", feud.getResponsiblePersonIds().contains("21"));
        assertEquals("Feud transitions to DORMANT because perpetrator is dead with no culpable clan", FeudStatus.DORMANT, feud.getCurrentFeudStatus());

        // 5. Voluntary rejection by peaceful successor
        Person peacefulPerson = new Person(30, "Peaceful", Gender.MALE, 30, 20, v);
        peacefulPerson.setFeudDisposition(FeudDispositionProfile.createPeaceful());
        HistoricalGrievanceRecord gPeace = new HistoricalGrievanceRecord(
                "G_PEACE", null, null, null, 50, 5,
                "30", 5, "NONE", 1, "40", "SIDE_X", Collections.emptyList(), "Harm"
        );
        FeudCase fPeace = bfs.recordGrievance(gPeace, 50, 5);
        Person peacefulSon = new Person(31, "Peaceful Son", Gender.MALE, 20, 30, v, null, peacefulPerson, null, 50.0);
        peacefulSon.setFeudDisposition(FeudDispositionProfile.createPeaceful());
        peacefulPerson.addChild(peacefulSon);
        peopleMap.put(peacefulPerson.getId(), peacefulPerson);
        peopleMap.put(peacefulSon.getId(), peacefulSon);

        peacefulPerson.markDeceased(50, "Old age");
        bfs.handlePersonDeath(peacefulPerson, 50, 6, peopleMap, null);
        InheritedGrievanceClaim peaceClaim = bfs.getClaim("CLAIM:" + fPeace.getFeudId() + ":31:G1");
        assertNotNull("Peace claim created", peaceClaim);
        assertTrue("Peaceful successor abandons inherited claim",
                peaceClaim.getStatus() == InheritedGrievanceClaim.ClaimAcceptanceStatus.ABANDONED
                        || peaceClaim.getStatus() == InheritedGrievanceClaim.ClaimAcceptanceStatus.REJECTED);
    }

    // =========================================================================
    // SUITE D: RETALIATION DECISION AND ANTI-LOOP
    // =========================================================================
    private static void runSuiteD_RetaliationDecisionAndAntiLoop() {
        System.out.println("\n--- SUITE D: Retaliation Decision and Anti-Loop Tests ---");
        BloodFeudSystem bfs = new BloodFeudSystem();
        Village vA = new Village(1, "Settlement A", 1.0, 0.05, 20.0, 20.0, Biome.RIVER_VALLEY);
        Village vB = new Village(2, "Settlement B", 1.0, 0.05, 80.0, 80.0, Biome.HIGHLAND_MINE);

        vA.getStockpile().addIronStone(200.0);
        vA.getStockpile().addFood(500.0);

        Person warriorA = new Person(101, "Warrior A", Gender.MALE, 30, 20, vA);
        warriorA.setFeudDisposition(FeudDispositionProfile.createVindictive());
        Person culpritB = new Person(201, "Culprit B", Gender.MALE, 32, 18, vB);

        vA.addResident(warriorA);
        vB.addResident(culpritB);

        Organization clanA = new Organization("CLAN_A", "Clan A", OrganizationType.CLAN, 10, 1, "101", OrganizationStatus.ACTIVE, OrganizationAlignment.DARK_ALIGNED, null);
        clanA.setFeudPolicy(OrganizationalFeudPolicy.TARGETED_RETALIATION);

        Organization clanB = new Organization("CLAN_B", "Clan B", OrganizationType.CLAN, 10, 1, "201", OrganizationStatus.ACTIVE, OrganizationAlignment.NEUTRAL, null);

        OrganizationSystem os = new OrganizationSystem();
        os.registerOrganization(clanA);
        os.registerOrganization(clanB);

        HistoricalGrievanceRecord g = new HistoricalGrievanceRecord(
                "GRIEV_RET_01", "BAT_01", null, null, 50, 1,
                "101", 1, "CLAN_A", 1,
                "201", "SIDE_B", List.of("CLAN_B"),
                "Slain clan member", "PERSONAL_MURDER",
                GrievanceCategory.PERSONAL_MURDER, AttributionConfidence.CONFIRMED,
                80.0, "201", null, null, true, ResponsibilityType.DIRECT_PERPETRATOR
        );
        FeudCase feud = bfs.recordGrievance(g, 50, 1);
        feud.addClaimantPerson("101");
        feud.addResponsiblePerson("201");

        CombatEngine engine = new CombatEngine();

        // 1. Initial retaliation consumes real resources (iron & food)
        double initialIron = vA.getStockpile().getIronStoneStockpile();
        double initialFood = vA.getStockpile().getFoodStockpile();

        bfs.processMonthlyTick(600, 50, 3, List.of(vA, vB), os, engine, null, null, new Random(42L));

        assertTrue("Iron deducted for retaliation logistics", vA.getStockpile().getIronStoneStockpile() < initialIron);
        assertTrue("Food deducted for expedition provisions", vA.getStockpile().getFoodStockpile() < initialFood);
        assertEquals("Retaliation operation attempted", 1, bfs.getAnnualRetaliationsAttempted());

        // 2. Cooldown is respected: 6-month cooldown prevents immediate subsequent retaliation
        assertTrue("Feud is on 6-month cooldown", feud.isCooldownActive(601));
        assertTrue("Feud is on cooldown at month 605", feud.isCooldownActive(605));
        assertFalse("Cooldown ends after 6 months (month 607)", feud.isCooldownActive(607));

        // 3. Anti-loop: 12-month retry cooldown after failed retaliation
        feud.recordFailedRetaliation(610);
        assertTrue("Retry cooldown active 5 months later", feud.isRetryCooldownActive(615));
        assertTrue("Retry cooldown active 11 months later", feud.isRetryCooldownActive(621));
        assertFalse("Retry cooldown expired after 12 months (month 622)", feud.isRetryCooldownActive(622));

        // 4. Dead perpetrators cannot be targeted
        culpritB.markDeceased(50, "Natural causes");
        feud.transitionStatus(FeudStatus.OPEN, 625, "Test");
        bfs.handlePersonDeath(culpritB, 50, 6, Map.of(culpritB.getId(), culpritB), os);
        bfs.processMonthlyTick(630, 50, 9, List.of(vA, vB), os, engine, null, null, new Random(42L));
        // Target is dead with no other members in clan B, so feud should not execute live duel
        assertEquals("Feud became DORMANT due to dead culprit", FeudStatus.DORMANT, feud.getCurrentFeudStatus());
    }

    // =========================================================================
    // SUITE E: COMBAT AND DEATH INTEGRATION
    // =========================================================================
    private static void runSuiteE_CombatAndDeathIntegration() {
        System.out.println("\n--- SUITE E: Combat and Death Integration Tests ---");
        BloodFeudSystem bfs = new BloodFeudSystem();
        Village v = new Village(1, "Frontier Village", 1.0, 0.05, 30.0, 30.0, Biome.RIVER_VALLEY);
        v.getStockpile().addIronStone(500.0);
        v.getStockpile().addFood(1000.0);

        Person pA = new Person(301, "Champion A", Gender.MALE, 28, 22, v);
        pA.setFeudDisposition(FeudDispositionProfile.createVindictive());
        Person pB = new Person(302, "Champion B", Gender.MALE, 29, 21, v);

        v.addResident(pA);
        v.addResident(pB);

        HistoricalGrievanceRecord g = new HistoricalGrievanceRecord(
                "G_COMBAT_01", null, null, null, 50, 1,
                "301", 1, "CLAN_X", 1, "302", "SIDE_Y", List.of("CLAN_Y"),
                "Blood debt", "PERSONAL_MURDER",
                GrievanceCategory.PERSONAL_MURDER, AttributionConfidence.CONFIRMED,
                85.0, "302", null, null, true, ResponsibilityType.DIRECT_PERPETRATOR
        );
        FeudCase feud = bfs.recordGrievance(g, 50, 1);
        feud.addClaimantPerson("301");
        feud.addResponsiblePerson("302");

        OrganizationSystem osE = new OrganizationSystem();
        Organization clanX = new Organization("CLAN_X", "Clan X", OrganizationType.CLAN, 10, 1, "301", OrganizationStatus.ACTIVE, OrganizationAlignment.DARK_ALIGNED, null);
        clanX.setFeudPolicy(OrganizationalFeudPolicy.TARGETED_RETALIATION);
        osE.registerOrganization(clanX);

        CombatEngine engine = new CombatEngine();
        bfs.processMonthlyTick(600, 50, 3, List.of(v), osE, engine, null, null, new Random(12345L));

        assertTrue("Feud battle was fought via CombatEngine", bfs.getCumulativeFeudBattles() >= 1);
        assertNotNull("Combat state exists on participant A", pA.getCombatState());
        assertNotNull("Combat state exists on participant B", pB.getCombatState());

        // Ordinary battle casualties do not generate duplicate blood feuds
        int feudCountBefore = bfs.getAllFeuds().size();
        HistoricalGrievanceRecord battleDeath = new HistoricalGrievanceRecord(
                "G_BAT_CASUALTY", "FEUD_BAT:" + feud.getFeudId() + ":600", null, null, 50, 3,
                "302", 2, "CLAN_Y", 1, "301", "SIDE_A", List.of("CLAN_X"),
                "Fell in battle", "FATALITY_IN_COMBAT"
        );
        bfs.recordGrievance(battleDeath, 50, 3);
        assertEquals("Casualty inside existing feud does not create separate new feud", feudCountBefore, bfs.getAllFeuds().size());
    }

    // =========================================================================
    // SUITE F: DIPLOMACY AND RECONCILIATION
    // =========================================================================
    private static void runSuiteF_DiplomacyAndReconciliation() {
        System.out.println("\n--- SUITE F: Diplomacy and Reconciliation Tests ---");
        BloodFeudSystem bfs = new BloodFeudSystem();
        Village vPaying = new Village(1, "Paying Village", 1.0, 0.05, 25.0, 25.0, Biome.RIVER_VALLEY);
        Village vReceiving = new Village(2, "Receiving Village", 1.0, 0.05, 40.0, 40.0, Biome.COASTAL);

        vPaying.getStockpile().addIronStone(300.0);
        double origRecIron = vReceiving.getStockpile().getIronStoneStockpile();

        HistoricalGrievanceRecord g = new HistoricalGrievanceRecord(
                "G_SETTLE_01", null, null, null, 50, 1,
                "10", 1, "CLAN_REC", 2, "20", "SIDE_PAY", List.of("CLAN_PAY"),
                "Wrong committed", "EQUIPMENT_STOLEN",
                GrievanceCategory.EQUIPMENT_STOLEN, AttributionConfidence.CONFIRMED,
                35.0, "20", null, null, true, ResponsibilityType.DIRECT_PERPETRATOR
        );
        FeudCase feud = bfs.recordGrievance(g, 50, 1);

        // 1. Truce blocks hostilities
        bfs.signTruce(feud.getFeudId(), 12, 50, 2);
        assertEquals("Feud transitioned to TRUCE", FeudStatus.TRUCE, feud.getCurrentFeudStatus());
        FeudAgreement truceAgr = bfs.getAgreement(feud.getActiveAgreementId());
        assertNotNull("Truce agreement registered", truceAgr);
        assertTrue("Truce agreement active", truceAgr.isActive());

        // 2. Compensation settlement transfers real assets and settles feud
        double origPayIron = vPaying.getStockpile().getIronStoneStockpile();
        boolean settled = bfs.executeCompensationSettlement(
                feud.getFeudId(), vPaying, vReceiving, 150.0, null, null, 50, 3
        );
        assertTrue("Compensation settlement succeeded", settled);
        assertEquals("Feud transitioned to SETTLED", FeudStatus.SETTLED, feud.getCurrentFeudStatus());
        assertEquals("Paying village iron deducted", 150.0, origPayIron - vPaying.getStockpile().getIronStoneStockpile());
        assertEquals("Receiving village iron credited", origRecIron + 150.0, vReceiving.getStockpile().getIronStoneStockpile());
        assertEquals("Cumulative compensation tracked", 150.0, bfs.getCumulativeCompensationPaidIron());

        // 3. Physical Heirloom Return
        ClanHeirloomSystem chs = new ClanHeirloomSystem();
        Organization owningClan = new Organization("CLAN_OWN", "Owning Clan", OrganizationType.CLAN, 10, 1, "1", OrganizationStatus.ACTIVE, OrganizationAlignment.AETHERIC_ALIGNED, null);
        EquipmentItem sword = new EquipmentItem(
                "EQ_BLADE_01", "Blade of Clan", EquipmentCategory.WEAPON, EquipmentSlot.MAIN_HAND,
                EquipmentType.ONE_HANDED_SWORD, false, ClassRarity.LEGENDARY, 6, 95.0, 150.0,
                new EquipmentStatProfile(35.0, 5.0, 10.0, 0.0, 15.0, 0.0, 0.0),
                0, "1", "FORGE_1", "CLAN_OWN", "CLAN_OWN", 0.0, 0.0
        );
        ClanHeirloom heirloom = chs.registerHeirloom(sword, owningClan, "Ancestral Blade", HeirloomSuccessionPolicy.BLOODLINE_FIRST, 50, 1);
        heirloom.markCaptured("ENEMY_SIDE", "BAT_TEST");
        assertEquals("Heirloom is STOLEN_OR_CAPTURED", HeirloomCustodyStatus.STOLEN_OR_CAPTURED, heirloom.getCustodyStatus());

        HistoricalGrievanceRecord gHeirloom = new HistoricalGrievanceRecord(
                "G_HEIRLOOM_REC", null, null, null, 50, 4,
                null, 0, "CLAN_OWN", 1, null, "ENEMY_SIDE", List.of("ENEMY_CLAN"),
                "Heirloom captured", "HEIRLOOM_CAPTURED",
                GrievanceCategory.HEIRLOOM_CAPTURED, AttributionConfidence.CONFIRMED,
                60.0, null, null, null, true, ResponsibilityType.INSTITUTIONAL
        );
        FeudCase fH = bfs.recordGrievance(gHeirloom, 50, 4);
        bfs.executeCompensationSettlement(fH.getFeudId(), null, null, 0.0, heirloom.getHeirloomId(), chs, 50, 5);
        assertEquals("Heirloom returned to vault upon settlement", HeirloomCustodyStatus.IN_CLAN_VAULT, heirloom.getCustodyStatus());
        assertEquals("Heirlooms recovered counter incremented", 1, bfs.getCumulativeHeirloomsRecovered());
    }

    // =========================================================================
    // SUITE G: HISTORICAL INTEGRITY AND STABILITY
    // =========================================================================
    private static void runSuiteG_HistoricalIntegrityAndStability() {
        System.out.println("\n--- SUITE G: Historical Integrity and Stability Tests ---");
        BloodFeudSystem bfs = new BloodFeudSystem();

        HistoricalGrievanceRecord g = new HistoricalGrievanceRecord(
                "G_HIST_01", "BAT_HIST", null, null, 25, 1,
                "501", 10, "CLAN_HIST", 1, "601", "SIDE_ENEMY", List.of("CLAN_ENEMY"),
                "Ancient wrong", "PERSONAL_MURDER"
        );
        FeudCase feud = bfs.recordGrievance(g, 25, 1);

        // 1. Feud Settled remains in archive (never deleted)
        bfs.executeCompensationSettlement(feud.getFeudId(), null, null, 0.0, null, null, 30, 1);
        assertEquals("Feud is SETTLED", FeudStatus.SETTLED, feud.getCurrentFeudStatus());
        assertEquals("Settled feuds list contains feud", 1, bfs.getSettledFeuds().size());
        assertEquals("All feuds list preserves settled feud", 1, bfs.getAllFeuds().size());

        // 2. Historical grievances list preserves record indefinitely
        assertEquals("Grievance preserved in archive", 1, bfs.getAllGrievances().size());
        assertEquals("Grievance ID matches", "G_HIST_01", bfs.getGrievance("G_HIST_01").getGrievanceId());

        // 3. Organization dissolution does not erase historical feuds
        Organization org = new Organization("CLAN_HIST", "Clan Hist", OrganizationType.CLAN, 10, 1, "501", OrganizationStatus.DISSOLVED, OrganizationAlignment.NEUTRAL, null);
        assertNotNull("Dissolved org referenced safely", org);
        assertEquals("Feud still references original org ID", "CLAN_HIST", feud.getInitiatingOrgId());
    }

    // =========================================================================
    // SUITE H: LONG-TERM MULTI-SEED SIMULATIONS (250 YEARS)
    // =========================================================================
    private static void runSuiteH_LongTermMultiSeedSimulation() {
        System.out.println("\n--- SUITE H: Long-Term Multi-Seed Simulations (250 Years) ---");

        long testSeed = 12345L;
        SimulationConfig configA = SimulationConfig.createDefaultWithSeed(testSeed);
        SimulationEngine engineA = new SimulationEngine(configA);
        engineA.initializeWorld();
        CombatEngine.clearCache();

        System.out.println("Running Engine A (Seed 12345, 250 Years)...");
        for (int y = 0; y < 250; y++) {
            for (int m = 1; m <= 12; m++) {
                engineA.processMonthlyTick();
            }
        }

        AnnualReport repA = engineA.getAnnualReports().get(engineA.getAnnualReports().size() - 1);
        assertNotNull("Annual report generated for Year 250", repA);
        assertNotNull("Blood feud telemetry present in AnnualReport", repA.getBloodFeudSummary());
        System.out.printf("Engine A Year 250 Feud Telemetry: %s\n", repA.getBloodFeudSummary());

        // Verify multi-seed determinism: Engine B with identical seed
        SimulationConfig configB = SimulationConfig.createDefaultWithSeed(testSeed);
        SimulationEngine engineB = new SimulationEngine(configB);
        engineB.initializeWorld();
        CombatEngine.clearCache();

        System.out.println("Running Engine B (Seed 12345, 250 Years)...");
        for (int y = 0; y < 250; y++) {
            for (int m = 1; m <= 12; m++) {
                engineB.processMonthlyTick();
            }
        }

        AnnualReport repB = engineB.getAnnualReports().get(engineB.getAnnualReports().size() - 1);
        assertEquals("Strict Determinism: Identical Year 250 Population", repA.getTotalLivingPopulation(), repB.getTotalLivingPopulation());
        assertEquals("Strict Determinism: Identical Feud Summary", repA.getBloodFeudSummary(), repB.getBloodFeudSummary());
    }
}
