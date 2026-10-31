package civsim.test;

import civsim.SimulationConfig;
import civsim.model.*;
import civsim.systems.CombatEngine;
import civsim.systems.DemographicsSystem;
import civsim.systems.OrganizationSystem;

import java.util.*;

/**
 * Stage 2B.2 Verification Test Suite.
 * Validates Clans, Factions, Allegiances, Diplomacy, and Dark Organizations.
 */
public class TestStage2B2Organizations {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   PHASE 2B.2 TEST SUITE: CLANS, FACTIONS, ALLEGIANCES, & DIPLOMACY");
        System.out.println("================================================================================");

        try {
            runSuite1_IdentityAndMembershipInvariants();
            runSuite2_AlignmentAndDarkOrganizationIndependence();
            runSuite3_LeadershipDeathSuccessionAndDissolution();
            runSuite4_DiplomacyAndCombatBridge();

            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(String.format("TOTAL TESTS: %d | PASSED: %d | FAILED: %d",
                    testsPassed + testsFailed, testsPassed, testsFailed));
            System.out.println("--------------------------------------------------------------------------------");

            if (testsFailed > 0) {
                System.err.println("FAILED: " + testsFailed + " tests failed!");
                System.exit(1);
            } else {
                System.out.println("ALL STAGE 2B.2 ACCEPTANCE TESTS PASSED SUCCESSFULLY!");
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

    private static Village createVillage(int id, String name) {
        return new Village(id, name, 1.0, 0.05, 20.0, 20.0, civsim.spatial.Biome.RIVER_VALLEY);
    }

    private static Person createPerson(int id, String name, int age, Village v) {
        Person p = new Person(id, name, Gender.MALE, age, -age, v, null, null, null, 100.0);
        Family f = new Family(id, "Family-" + id, 0, p);
        p.setFamily(f);
        if (v != null) {
            v.addResident(p);
        }
        return p;
    }

    private static void awakenPerson(Person p, SupernaturalClass sc, boolean dark, int tier) {
        AetherProfile ap = AetherProfile.createForTesting(sc, ClassRarity.RARE, tier, dark);
        p.setAetherProfile(ap);
    }

    // =========================================================================
    // SUITE 1: IDENTITY, RESIDENCY & MEMBERSHIP INVARIANT TESTS
    // =========================================================================
    private static void runSuite1_IdentityAndMembershipInvariants() {
        System.out.println("\n--- SUITE 1: Identity, Residency & Membership Invariant Tests ---");

        SimulationConfig config = SimulationConfig.createDefault();
        OrganizationSystem orgSys = new OrganizationSystem();
        List<Person> people = new ArrayList<>();
        orgSys.setPeopleSupplier(() -> people);

        Village v1 = createVillage(1, "Village Alpha");
        Village v2 = createVillage(2, "Village Beta");

        Person p1 = createPerson(1, "Theron", 25, v1);
        Person p2 = createPerson(2, "Lysandra", 28, v1);
        Person p3 = createPerson(3, "Gideon", 30, v2);
        people.addAll(Arrays.asList(p1, p2, p3));

        // 1.1: Four Distinct Dimensions: Family != Village != Aspect != Organization
        OrganizationFoundingEvent clanEvent = new OrganizationFoundingEvent(
                "EVT_FOUND_CLAN1", "CLAN_01", "Ironfang Clan",
                OrganizationType.CLAN, OrganizationAlignment.NEUTRAL,
                String.valueOf(p1.getId()), Collections.emptyList(),
                OrganizationPolicyProfile.createDefault(OrganizationType.CLAN), 10, 0
        );
        Organization clan = orgSys.foundOrganization(clanEvent, people, config, null);
        assertTrue("Clan successfully founded in Mundane Era", clan != null);
        assertEquals("Founder enrolled as LEADER", OrganizationRole.LEADER, clan.getMembershipRecord(String.valueOf(p1.getId())).getRole());
        assertEquals("Leader is p1", String.valueOf(p1.getId()), clan.getCurrentLeaderPersonId());
        assertEquals("Primary org set on p1", "CLAN_01", p1.getPrimaryOrganizationId());
        assertTrue("p1 belongs to biological Family-1", p1.getFamily().getSurname().equals("Family-1"));
        assertTrue("p1 resides in Village Alpha", p1.getVillage().getName().equals("Village Alpha"));
        assertTrue("p1 is unawakened in Year 10", !p1.isAwakened());

        // 1.2: Multiple secondary compatible memberships with exactly ONE primaryOrganizationId
        OrganizationFoundingEvent orderEvent = new OrganizationFoundingEvent(
                "EVT_FOUND_SCHOLAR", "SCHOLARS_01", "Archive Keepers",
                OrganizationType.SCHOLARLY_ORDER, OrganizationAlignment.NEUTRAL,
                String.valueOf(p2.getId()), Collections.emptyList(),
                OrganizationPolicyProfile.createDefault(OrganizationType.SCHOLARLY_ORDER), 12, 0
        );
        Organization scholars = orgSys.foundOrganization(orderEvent, people, config, null);
        assertTrue("Scholarly order founded", scholars != null);

        // p1 voluntarily joins Scholarly Order (Clan and Scholarly Order have exclusiveMembershipRequired = false)
        boolean applied = orgSys.voluntaryJoin("EVT_JOIN_1", "SCHOLARS_01", String.valueOf(p1.getId()), 15, 0, people);
        assertTrue("p1 successfully joined secondary organization", applied);
        assertTrue("p1 has active membership in CLAN_01", p1.getActiveOrganizationIds().contains("CLAN_01"));
        assertTrue("p1 has active membership in SCHOLARS_01", p1.getActiveOrganizationIds().contains("SCHOLARS_01"));
        assertEquals("p1 retains primaryOrganizationId as CLAN_01", "CLAN_01", p1.getPrimaryOrganizationId());

        // 1.3: Duplicate membership rejection & duplicate eventId idempotency
        boolean reapply = orgSys.voluntaryJoin("EVT_JOIN_2", "SCHOLARS_01", String.valueOf(p1.getId()), 16, 0, people);
        assertTrue("Duplicate active membership request is rejected", !reapply);

        Organization dupeEvent = orgSys.foundOrganization(clanEvent, people, config, null);
        assertEquals("Duplicate founding event returns existing organization idempotently", clan, dupeEvent);

        // 1.4: Settlement migration preserves clan/org membership
        p1.setVillage(v2); // p1 moves to Village Beta
        assertEquals("p1 moved to Village Beta", v2, p1.getVillage());
        assertTrue("p1 remains active in CLAN_01 after migration", clan.isMemberActive(String.valueOf(p1.getId())));
        assertTrue("p1 remains active in SCHOLARS_01 after migration", scholars.isMemberActive(String.valueOf(p1.getId())));
    }

    // =========================================================================
    // SUITE 2: ALIGNMENT & DARK ORGANIZATION INDEPENDENCE TESTS
    // =========================================================================
    private static void runSuite2_AlignmentAndDarkOrganizationIndependence() {
        System.out.println("\n--- SUITE 2: Alignment & Dark Organization Independence Tests ---");

        SimulationConfig config = SimulationConfig.createDefault();
        OrganizationSystem orgSys = new OrganizationSystem();
        List<Person> people = new ArrayList<>();
        orgSys.setPeopleSupplier(() -> people);

        Village v1 = createVillage(1, "Village Alpha");

        Person p1 = createPerson(10, "MortalFounder", 35, v1);
        Person p2 = createPerson(11, "DarkMage", 40, v1);
        Person p3 = createPerson(12, "AethericKnight", 30, v1);
        people.addAll(Arrays.asList(p1, p2, p3));

        // 2.1: Era Gating — Supernatural organization in Mundane Era (Year < 50) MUST fail
        OrganizationFoundingEvent earlyCovenEvent = new OrganizationFoundingEvent(
                "EVT_EARLY_COVEN", "COVEN_00", "Early Shadow Coven",
                OrganizationType.DARK_COVEN, OrganizationAlignment.DARK_ALIGNED,
                String.valueOf(p1.getId()), Collections.emptyList(),
                OrganizationPolicyProfile.createDefault(OrganizationType.DARK_COVEN), 30, 0
        );
        Organization earlyCoven = orgSys.foundOrganization(earlyCovenEvent, people, config, null);
        assertTrue("DARK_COVEN founding in Mundane Era (Year 30) is rejected", earlyCoven == null);

        OrganizationFoundingEvent earlyAwakenedOrder = new OrganizationFoundingEvent(
                "EVT_EARLY_ORDER", "ORDER_00", "Early Mystic Circle",
                OrganizationType.AWAKENED_ORDER, OrganizationAlignment.AETHERIC_ALIGNED,
                String.valueOf(p1.getId()), Collections.emptyList(),
                OrganizationPolicyProfile.createDefault(OrganizationType.AWAKENED_ORDER), 49, 11
        );
        Organization earlyOrder = orgSys.foundOrganization(earlyAwakenedOrder, people, config, null);
        assertTrue("AWAKENED_ORDER founding in Year 49 is rejected", earlyOrder == null);

        // 2.2: Unawakened person cannot found AWAKENED_ORDER even in Year 50+
        OrganizationFoundingEvent unawakenedAwakenedOrder = new OrganizationFoundingEvent(
                "EVT_UNAWAKENED_ORDER", "ORDER_50", "Mortal Mystic Circle",
                OrganizationType.AWAKENED_ORDER, OrganizationAlignment.AETHERIC_ALIGNED,
                String.valueOf(p1.getId()), Collections.emptyList(),
                OrganizationPolicyProfile.createDefault(OrganizationType.AWAKENED_ORDER), 50, 0
        );
        Organization failedOrder = orgSys.foundOrganization(unawakenedAwakenedOrder, people, config, null);
        assertTrue("AWAKENED_ORDER founded by unawakened person is rejected", failedOrder == null);

        // Awaken p2 as DARK and p3 as AETHERIC
        awakenPerson(p2, SupernaturalClass.RIFTWALKER, true, 3);
        awakenPerson(p3, SupernaturalClass.WARDEN, false, 3);
        assertTrue("p2 is Dark Awakened", p2.getAetherProfile().isDarkAwakened());
        assertTrue("p3 is Aetheric Awakened", !p3.getAetherProfile().isDarkAwakened());

        // 2.3: DARK-awakened individuals are NOT auto-enrolled into DARK_COVENs
        assertEquals("p2 has no active organizations initially", 0, p2.getActiveOrganizationIds().size());

        // 2.4: Legitimate Dark Coven founding at Year 50+ by p2
        OrganizationFoundingEvent darkCovenEvent = new OrganizationFoundingEvent(
                "EVT_DARK_COVEN", "COVEN_01", "Nightfall Syndicate",
                OrganizationType.DARK_COVEN, OrganizationAlignment.DARK_ALIGNED,
                String.valueOf(p2.getId()), Collections.emptyList(),
                OrganizationPolicyProfile.createDefault(OrganizationType.DARK_COVEN), 50, 0
        );
        Organization darkCoven = orgSys.foundOrganization(darkCovenEvent, people, config, null);
        assertTrue("DARK_COVEN founded at Year 50 by dark-awakened founder", darkCoven != null);
        assertEquals("Coven leader is p2", String.valueOf(p2.getId()), darkCoven.getCurrentLeaderPersonId());

        // 2.5: Joining a DARK_COVEN or changing Org alignment does not mutate individual Aspect
        Person p4 = createPerson(13, "Sellsword", 22, v1);
        people.add(p4);
        OrganizationFoundingEvent mercEvent = new OrganizationFoundingEvent(
                "EVT_MERC", "MERC_01", "Iron Vanguard",
                OrganizationType.MERCENARY_COMPANY, OrganizationAlignment.NEUTRAL,
                String.valueOf(p4.getId()), Collections.emptyList(),
                OrganizationPolicyProfile.createDefault(OrganizationType.MERCENARY_COMPANY), 51, 0
        );
        Organization mercs = orgSys.foundOrganization(mercEvent, people, config, null);
        assertTrue("Mercenary company founded", mercs != null);

        // Change alignment of Mercenaries to DARK_ALIGNED
        mercs.setAlignment(OrganizationAlignment.DARK_ALIGNED);
        assertEquals("Merc alignment is now DARK_ALIGNED", OrganizationAlignment.DARK_ALIGNED, mercs.getAlignment());
        assertTrue("p4 supernatural aspect is still unawakened / unaffected", !p4.isAwakened());

        // p3 (AETHERIC Warden) joins the Dark Aligned Mercenary company
        boolean p3Joined = orgSys.voluntaryJoin("EVT_JOIN_P3", "MERC_01", String.valueOf(p3.getId()), 52, 1, people);
        assertTrue("Aetheric p3 joined Dark-aligned Mercenaries", p3Joined);
        assertTrue("p3 remains strictly AETHERIC (not dark awakened)", !p3.getAetherProfile().isDarkAwakened());
    }

    // =========================================================================
    // SUITE 3: LEADERSHIP, DEATH, SUCCESSION & DISSOLUTION TESTS
    // =========================================================================
    private static void runSuite3_LeadershipDeathSuccessionAndDissolution() {
        System.out.println("\n--- SUITE 3: Leadership, Death, Succession & Dissolution Tests ---");

        SimulationConfig config = SimulationConfig.createDefault();
        DemographicsSystem demoSys = new DemographicsSystem();
        OrganizationSystem orgSys = new OrganizationSystem();
        demoSys.setOrganizationSystem(orgSys);
        List<Person> people = new ArrayList<>();
        orgSys.setPeopleSupplier(() -> people);

        Village v1 = createVillage(1, "Stronghold");

        // Create 3 members for an Awakened Order with MASTERY_AND_TIER_MERIT succession
        Person leader = createPerson(20, "Grandmaster", 55, v1);
        Person elder = createPerson(21, "ElderAdept", 45, v1);
        Person prodigy = createPerson(22, "YoungProdigy", 25, v1);
        people.addAll(Arrays.asList(leader, elder, prodigy));

        awakenPerson(leader, SupernaturalClass.ELEMENTALIST, false, 5);
        leader.getAetherProfile().getProgressionRecord().setMastery(60.0);

        awakenPerson(elder, SupernaturalClass.ELEMENTALIST, false, 3);
        elder.getAetherProfile().getProgressionRecord().setMastery(40.0);

        awakenPerson(prodigy, SupernaturalClass.ELEMENTALIST, false, 6);
        prodigy.getAetherProfile().getProgressionRecord().setMastery(85.0);

        OrganizationPolicyProfile meritPolicy = OrganizationPolicyProfile.createDefaultAwakenedOrderPolicy();

        OrganizationFoundingEvent orderEvent = new OrganizationFoundingEvent(
                "EVT_MERIT_ORDER", "ORDER_MERIT", "Arcane Citadel",
                OrganizationType.AWAKENED_ORDER, OrganizationAlignment.AETHERIC_ALIGNED,
                String.valueOf(leader.getId()),
                Arrays.asList(String.valueOf(elder.getId()), String.valueOf(prodigy.getId())),
                meritPolicy, 55, 0
        );
        Organization order = orgSys.foundOrganization(orderEvent, people, config, null);
        assertTrue("Order founded with MASTERY_AND_TIER_MERIT", order != null);
        assertEquals("Initial leader is Grandmaster (p20)", String.valueOf(leader.getId()), order.getCurrentLeaderPersonId());
        assertEquals("Active member count is 3", 3, order.getActiveMemberCount());

        // 3.1: Leader dies -> immediate deterministic succession picking highest capability (Prodigy p22)
        demoSys.killPerson(leader, Collections.emptyList(), 60, "battle wounds", config, null);
        assertTrue("Leader is marked dead", !leader.isAlive());
        MembershipRecord leaderRecord = order.getMembershipRecord(String.valueOf(leader.getId()));
        assertEquals("Leader membership marked LEFT (ENDED_BY_DEATH)", MembershipStatus.LEFT, leaderRecord.getStatus());
        assertEquals("New leader is YoungProdigy (p22) due to highest Tier/Mastery",
                String.valueOf(prodigy.getId()), order.getCurrentLeaderPersonId());
        assertEquals("Prodigy role promoted to LEADER", OrganizationRole.LEADER, order.getMembershipRecord(String.valueOf(prodigy.getId())).getRole());

        // 3.2: Dynamic influence profiling excludes dead members
        OrganizationInfluenceProfile influence = order.computeInfluenceProfile(buildPeopleMap(people), 60, 0);
        assertTrue("Influence computed successfully", influence != null);
        assertEquals("Living active members counted is 2", 2, influence.getActiveMemberCount());
        assertTrue("Aggregate influence score is positive", influence.getCompositeInfluenceScore() > 0.0);

        // 3.3: Total member loss leads to DISSOLVED status
        demoSys.killPerson(prodigy, Collections.emptyList(), 65, "old age", config, null);
        assertEquals("Elder (p21) succeeds prodigy as leader", String.valueOf(elder.getId()), order.getCurrentLeaderPersonId());

        demoSys.killPerson(elder, Collections.emptyList(), 70, "illness", config, null);
        assertEquals("Active member count is 0", 0, order.getActiveMemberCount());
        assertEquals("Organization is DISSOLVED", OrganizationStatus.DISSOLVED, order.getStatus());
        assertTrue("Dissolved organization cannot recruit", !order.getStatus().canRecruit());

        // Rejection of join to dissolved organization
        Person newRecruit = createPerson(23, "Wanderer", 20, v1);
        people.add(newRecruit);
        boolean joinDissolved = orgSys.voluntaryJoin("EVT_JOIN_DIS", "ORDER_MERIT", String.valueOf(newRecruit.getId()), 71, 0, people);
        assertTrue("Cannot join a DISSOLVED organization", !joinDissolved);

        // 3.4: Heap-pruning & GC safety: person.clearAfterDeath() cleared person state
        assertTrue("Deceased leader active org set cleared", leader.getActiveOrganizationIds().isEmpty());
        assertTrue("Deceased leader primary org cleared", leader.getPrimaryOrganizationId() == null);
        assertTrue("Deceased elder active org set cleared", elder.getActiveOrganizationIds().isEmpty());
    }

    // =========================================================================
    // SUITE 4: DIPLOMACY & COMBAT BRIDGE TESTS
    // =========================================================================
    private static void runSuite4_DiplomacyAndCombatBridge() {
        System.out.println("\n--- SUITE 4: Diplomacy & Combat Bridge Tests ---");

        SimulationConfig config = SimulationConfig.createDefault();
        OrganizationSystem orgSys = new OrganizationSystem();
        List<Person> people = new ArrayList<>();
        orgSys.setPeopleSupplier(() -> people);

        Village v1 = createVillage(1, "Solitude");
        Village v2 = createVillage(2, "Stormhold");

        Person leaderA = createPerson(30, "WarlordA", 35, v1);
        Person warriorA1 = createPerson(31, "WarriorA1", 28, v1);
        Person leaderB = createPerson(40, "WarlordB", 38, v2);
        Person warriorB1 = createPerson(41, "WarriorB1", 25, v2);
        Person leaderC = createPerson(50, "WarlordC", 42, v1);
        people.addAll(Arrays.asList(leaderA, warriorA1, leaderB, warriorB1, leaderC));

        // Create 3 organizations: ORG_A (Mercenary), ORG_B (Mercenary), ORG_C (Clan)
        Organization orgA = orgSys.foundOrganization(new OrganizationFoundingEvent(
                "EVT_OA", "ORG_A", "Alpha Cohort", OrganizationType.MERCENARY_COMPANY,
                OrganizationAlignment.NEUTRAL, String.valueOf(leaderA.getId()),
                Collections.singletonList(String.valueOf(warriorA1.getId())),
                OrganizationPolicyProfile.createDefault(OrganizationType.MERCENARY_COMPANY), 50, 0
        ), people, config, null);

        Organization orgB = orgSys.foundOrganization(new OrganizationFoundingEvent(
                "EVT_OB", "ORG_B", "Beta Legion", OrganizationType.MERCENARY_COMPANY,
                OrganizationAlignment.NEUTRAL, String.valueOf(leaderB.getId()),
                Collections.singletonList(String.valueOf(warriorB1.getId())),
                OrganizationPolicyProfile.createDefault(OrganizationType.MERCENARY_COMPANY), 50, 0
        ), people, config, null);

        Organization orgC = orgSys.foundOrganization(new OrganizationFoundingEvent(
                "EVT_OC", "ORG_C", "Gamma Alliance", OrganizationType.CLAN,
                OrganizationAlignment.NEUTRAL, String.valueOf(leaderC.getId()),
                Collections.emptyList(),
                OrganizationPolicyProfile.createDefault(OrganizationType.CLAN), 50, 0
        ), people, config, null);

        // 4.1: Pairwise relationship scores are symmetric and clamped to [-100, 100]
        orgSys.adjustRelationship("EVT_REL_1", "ORG_A", "ORG_B", 45.0, 50, 1);
        assertDoubleEquals("Relationship A -> B is 45.0", 45.0,
                orgSys.getOrCreateRelationship("ORG_A", "ORG_B", 50, 1).getRelationshipScore(), 0.01);
        assertDoubleEquals("Relationship B -> A is symmetric (45.0)", 45.0,
                orgSys.getOrCreateRelationship("ORG_B", "ORG_A", 50, 1).getRelationshipScore(), 0.01);

        orgSys.adjustRelationship("EVT_REL_2", "ORG_A", "ORG_B", 200.0, 50, 2);
        assertDoubleEquals("Score is clamped to +100.0", 100.0,
                orgSys.getOrCreateRelationship("ORG_A", "ORG_B", 50, 2).getRelationshipScore(), 0.01);

        orgSys.adjustRelationship("EVT_REL_3", "ORG_A", "ORG_B", -300.0, 50, 3);
        assertDoubleEquals("Score is clamped to -100.0", -100.0,
                orgSys.getOrCreateRelationship("ORG_A", "ORG_B", 50, 3).getRelationshipScore(), 0.01);

        // 4.2: Treaties: ALLIED alliance between ORG_A and ORG_C
        orgSys.signTreaty("TREATY_AC_01", "ORG_A", "ORG_C", DiplomaticStatus.ALLIED, 50, 4);
        OrganizationRelationship relAC = orgSys.getOrCreateRelationship("ORG_A", "ORG_C", 50, 4);
        assertTrue("Treaty is active", relAC.isTreatyActive());
        assertEquals("Diplomatic status is ALLIED", DiplomaticStatus.ALLIED, relAC.getDiplomaticStatus());

        // 4.3: Hostility between ORG_A and ORG_B marks combat enemies without auto-spawning wars or mutating Village diplomacy
        orgSys.setDiplomaticStatus("EVT_WAR_AB", "ORG_A", "ORG_B", DiplomaticStatus.HOSTILE, 50, 5);
        OrganizationRelationship relAB = orgSys.getOrCreateRelationship("ORG_A", "ORG_B", 50, 5);
        assertTrue("ORG_A and ORG_B are HOSTILE", relAB.getDiplomaticStatus().isHostile());

        // 4.4: resolveEncounterSides merges allied coalitions
        // Combatants: leaderA (ORG_A), leaderC (ORG_C), leaderB (ORG_B)
        Combatant cA = new PersonCombatant(leaderA, null);
        Combatant cC = new PersonCombatant(leaderC, null);
        Combatant cB = new PersonCombatant(leaderB, null);
        List<Combatant> encounterGroup = Arrays.asList(cA, cC, cB);

        Map<Combatant, String> sides = orgSys.resolveEncounterSides(encounterGroup);
        String sideA = sides.get(cA);
        String sideC = sides.get(cC);
        String sideB = sides.get(cB);

        assertEquals("Allied ORG_A and ORG_C merged into identical coalition side", sideA, sideC);
        assertTrue("Hostile ORG_B placed on distinct side from ORG_A coalition", !sideB.equals(sideA));

        // 4.5: processCompletedBattleResult: Idempotency, relationship penalty, and treaty break
        // Construct BattleResult where sideA (ORG_A) defeats sideB (ORG_B) with 1 fatality
        Map<String, Integer> initialCounts = new HashMap<>();
        initialCounts.put("ORG_ORG_A", 2);
        initialCounts.put("ORG_ORG_B", 1);

        BattleResult mockResult = new BattleResult(
                "BATTLE_001", 50, 6,
                BattleType.FACTION_CONFLICT, BattleOutcome.VICTORY, "ORG_ORG_A",
                3,
                initialCounts,
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.singletonList("PERSON_" + leaderB.getId()),
                Collections.emptyList(),
                Collections.emptyList(),
                0,
                "Decisive clash in valley"
        );

        // Reset score towards positive before signing ceasefire so penalty margin can be measured without clamping
        orgSys.adjustRelationship("EVT_RESET_AB", "ORG_A", "ORG_B", 150.0, 50, 5);

        // Sign a ceasefire treaty between ORG_A and ORG_B first to test battle treaty break
        orgSys.signTreaty("CEASEFIRE_AB", "ORG_A", "ORG_B", DiplomaticStatus.CEASEFIRE, 50, 5);
        assertTrue("Ceasefire signed prior to battle", orgSys.getOrCreateRelationship("ORG_A", "ORG_B", 50, 5).isTreatyActive());

        double preBattleScore = orgSys.getOrCreateRelationship("ORG_A", "ORG_B", 50, 5).getRelationshipScore();
        orgSys.processCompletedBattleResult(mockResult, 50, 6, people);

        OrganizationRelationship postRel = orgSys.getOrCreateRelationship("ORG_A", "ORG_B", 50, 6);
        assertTrue("Active treaty broken due to battle engagement", !postRel.isTreatyActive());
        assertTrue("Relationship score penalized due to armed conflict", postRel.getRelationshipScore() <= preBattleScore - 25.0);
        assertEquals("Org A battle record incremented (1 win)", 1, orgA.getBattlesWon());
        assertEquals("Org B battle record incremented (1 loss)", 1, orgB.getBattlesParticipated() - orgB.getBattlesWon());

        // Idempotency check: process same BattleResult a second time
        double secondScore = postRel.getRelationshipScore();
        orgSys.processCompletedBattleResult(mockResult, 50, 6, people);
        assertDoubleEquals("Second consumption of same BattleResult is idempotent (score unchanged)",
                secondScore, postRel.getRelationshipScore(), 0.001);
        assertEquals("Org A battles won still 1 (not duplicated)", 1, orgA.getBattlesWon());
    }

    private static Map<String, Person> buildPeopleMap(List<Person> people) {
        Map<String, Person> map = new HashMap<>();
        for (Person p : people) {
            map.put(String.valueOf(p.getId()), p);
            map.put("PERSON_" + p.getId(), p);
        }
        return map;
    }
}
