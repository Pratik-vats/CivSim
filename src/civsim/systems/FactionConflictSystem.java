package civsim.systems;

import civsim.ProgressionConfig;
import civsim.SimulationConfig;
import civsim.model.*;
import civsim.spatial.Biome;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Phase 2B.4 — Master Faction Conflict, Anomaly Skirmish Resolution, Site Securing,
 * and Combat Consequence System.
 * Coordinates multi-side faction contests, arrival batching, peaceful negotiations,
 * tactical skirmish execution, 50% carried wealth looting, historical grievances,
 * site securing/challenging, and strict 4-bucket resource conservation.
 */
public class FactionConflictSystem implements DemographicsSystem.CombatLootListener {

    // Authoritative Registries
    private final Map<String, WorldAnomaly> anomalies = new LinkedHashMap<>();
    private final Map<String, Expedition> expeditions = new LinkedHashMap<>();
    private final Map<String, AnomalyContestRecord> contests = new LinkedHashMap<>();
    private final List<DiscoveryRecord> discoveries = new ArrayList<>();
    private final Map<String, JointExtractionAgreement> jointAgreements = new LinkedHashMap<>();
    private final List<BattleLootRecord> lootRecords = new ArrayList<>();
    private final List<HistoricalGrievanceRecord> grievanceRecords = new ArrayList<>();
    private final List<CombatResourceRecoveryRecord> recoveryRecords = new ArrayList<>();
    private final Map<String, Double> deliveredInventories = new LinkedHashMap<>();

    // Idempotency guard
    private final Set<String> processedEventIds = new HashSet<>();

    // Active battle contest mapping for looting resolution
    private final Map<String, AnomalyContestRecord> activeBattleContestMap = new ConcurrentHashMap<>();

    // Annual Telemetry Counters
    private int annualContestsProcessed = 0;
    private int annualResolvedPeacefully = 0;
    private int annualResolvedByVictory = 0;
    private int annualStalemates = 0;
    private int annualSkirmishes = 0;
    private int annualTwoSidedSkirmishes = 0;
    private int annualMultiSidedSkirmishes = 0;
    private double annualLootedIron = 0.0;
    private int annualGrievancesLogged = 0;
    private int annualSitesSecured = 0;
    private int annualSitesChallenged = 0;
    private int annualExpiredToLair = 0;

    public FactionConflictSystem() {}

    public synchronized void resetYearlyCounters() {
        annualContestsProcessed = 0;
        annualResolvedPeacefully = 0;
        annualResolvedByVictory = 0;
        annualStalemates = 0;
        annualSkirmishes = 0;
        annualTwoSidedSkirmishes = 0;
        annualMultiSidedSkirmishes = 0;
        annualLootedIron = 0.0;
        annualGrievancesLogged = 0;
        annualSitesSecured = 0;
        annualSitesChallenged = 0;
        annualExpiredToLair = 0;
    }

    public synchronized void clear() {
        anomalies.clear();
        expeditions.clear();
        contests.clear();
        discoveries.clear();
        jointAgreements.clear();
        lootRecords.clear();
        grievanceRecords.clear();
        recoveryRecords.clear();
        deliveredInventories.clear();
        processedEventIds.clear();
        activeBattleContestMap.clear();
        resetYearlyCounters();
    }

    // =========================================================================
    // REGISTRATION & QUERY METHODS
    // =========================================================================

    public synchronized void registerAnomaly(WorldAnomaly anomaly) {
        if (anomaly != null) {
            anomalies.put(anomaly.getAnomalyId(), anomaly);
        }
    }

    public synchronized WorldAnomaly getAnomaly(String anomalyId) {
        return anomalies.get(anomalyId);
    }

    public synchronized List<WorldAnomaly> getAllAnomalies() {
        return Collections.unmodifiableList(new ArrayList<>(anomalies.values()));
    }

    public synchronized void registerExpedition(Expedition expedition) {
        if (expedition != null) {
            expeditions.put(expedition.getExpeditionId(), expedition);
        }
    }

    public synchronized Expedition getExpedition(String expeditionId) {
        return expeditions.get(expeditionId);
    }

    public synchronized List<Expedition> getAllExpeditions() {
        return Collections.unmodifiableList(new ArrayList<>(expeditions.values()));
    }

    public synchronized void registerContest(AnomalyContestRecord contest) {
        if (contest != null) {
            contests.put(contest.getContestId(), contest);
        }
    }

    public synchronized AnomalyContestRecord getContest(String contestId) {
        return contests.get(contestId);
    }

    public synchronized List<AnomalyContestRecord> getAllContests() {
        return Collections.unmodifiableList(new ArrayList<>(contests.values()));
    }

    public synchronized List<BattleLootRecord> getLootRecords() {
        return Collections.unmodifiableList(lootRecords);
    }

    public synchronized List<HistoricalGrievanceRecord> getGrievanceRecords() {
        return Collections.unmodifiableList(grievanceRecords);
    }

    public synchronized List<CombatResourceRecoveryRecord> getRecoveryRecords() {
        return Collections.unmodifiableList(recoveryRecords);
    }

    // =========================================================================
    // 1. COMBAT LOOT LISTENER (DemographicsSystem callback)
    // =========================================================================

    @Override
    public synchronized double onCombatFatalityLoot(
            Person victim,
            double eligibleNetCarriedWealth,
            String cause,
            int currentYear) {

        if (victim == null || eligibleNetCarriedWealth <= 0.0 || cause == null) {
            return 0.0;
        }

        // Only apply to ANOMALY_SKIRMISH combat fatalities
        if (!cause.contains("ANOMALY_SKIRMISH")) {
            return 0.0;
        }

        // Parse battleId from cause: e.g. "slain in combat (ANOMALY_SKIRMISH ANOMALY_SKIRMISH:anomalyId:contestId:SEQ:1)"
        String battleId = extractBattleIdFromCause(cause);
        if (battleId == null) {
            return 0.0;
        }

        String eventId = "LOOT:" + battleId + ":" + victim.getId();
        if (!processedEventIds.add(eventId)) {
            return 0.0; // Idempotent: already processed
        }

        // Calculate up to 50% net liquid carried personal wealth
        double personalWealthLootFraction = 0.50;
        double lootedIron = Math.floor(eligibleNetCarriedWealth * personalWealthLootFraction);
        if (lootedIron <= 0.0) {
            return 0.0;
        }

        CombatState cs = victim.getCombatState();
        String killerPersonId = (cs != null) ? cs.getLastKillerPersonId() : null;
        String killerSideId = (cs != null) ? cs.getLastKillerSideId() : null;

        AnomalyContestRecord contest = activeBattleContestMap.get(battleId);
        String anomalyId = (contest != null) ? contest.getAnomalyId() : "UNKNOWN";
        String contestId = (contest != null) ? contest.getContestId() : "UNKNOWN";
        String beneficiarySide = (killerSideId != null) ? killerSideId : ((contest != null) ? contest.getWinningCoalitionSideId() : "VICTOR_COALITION");

        BattleLootRecord lootRecord = new BattleLootRecord(
                eventId,
                battleId,
                contestId,
                anomalyId,
                String.valueOf(victim.getId()),
                killerPersonId,
                beneficiarySide,
                lootedIron,
                victim.getWealth(),
                victim.getLoanDebt(),
                eligibleNetCarriedWealth - lootedIron,
                currentYear,
                0
        );

        lootRecords.add(lootRecord);
        annualLootedIron += lootedIron;

        return lootedIron;
    }

    private String extractBattleIdFromCause(String cause) {
        int idx = cause.indexOf("ANOMALY_SKIRMISH ");
        if (idx >= 0) {
            int end = cause.indexOf(")", idx);
            if (end > idx) {
                return cause.substring(idx + 17, end).trim();
            }
            return cause.substring(idx + 17).trim();
        }
        return null;
    }

    // =========================================================================
    // 2. COALITION RESOLUTION & EXPEDITION GROUPING
    // =========================================================================

    /**
     * Resolves expeditions into coalition sides based on organization allegiance and formal ALLIED diplomacy.
     */
    public synchronized Map<String, List<Expedition>> resolveCoalitions(
            List<Expedition> presentExpeditions,
            OrganizationSystem orgSys,
            Map<String, Person> peopleMap) {

        Map<String, List<Expedition>> coalitions = new LinkedHashMap<>();
        if (presentExpeditions == null || presentExpeditions.isEmpty()) {
            return coalitions;
        }

        // Step 1: Assign initial coalition key based on sponsor organization
        Map<Expedition, String> initialSideMap = new HashMap<>();
        for (Expedition exp : presentExpeditions) {
            String orgId = exp.getSponsorId();
            if (orgId == null || orgId.isEmpty()) {
                // Fallback to primary organization of expedition leader
                List<Person> personnel = exp.getLivingPresentPersonnel(peopleMap);
                if (!personnel.isEmpty() && personnel.get(0).getPrimaryOrganizationId() != null) {
                    orgId = personnel.get(0).getPrimaryOrganizationId();
                } else {
                    orgId = "INDEPENDENT_" + exp.getExpeditionId();
                }
            }
            initialSideMap.put(exp, "COALITION_" + orgId);
        }

        // Step 2: Merge allied coalitions if organizations share an active ALLIED treaty
        Map<String, String> sideAliases = new HashMap<>();
        List<String> distinctSides = new ArrayList<>(new HashSet<>(initialSideMap.values()));

        if (orgSys != null) {
            for (int i = 0; i < distinctSides.size(); i++) {
                String sideA = distinctSides.get(i);
                if (!sideA.startsWith("COALITION_")) continue;
                String orgA = sideA.substring(10);

                for (int j = i + 1; j < distinctSides.size(); j++) {
                    String sideB = distinctSides.get(j);
                    if (!sideB.startsWith("COALITION_")) continue;
                    String orgB = sideB.substring(10);

                    if (orgSys.areAllied(orgA, orgB)) {
                        String rootA = findRootAlias(sideA, sideAliases);
                        String rootB = findRootAlias(sideB, sideAliases);
                        if (!rootA.equals(rootB)) {
                            sideAliases.put(rootB, rootA);
                        }
                    }
                }
            }
        }

        // Step 3: Populate final coalition map
        for (Expedition exp : presentExpeditions) {
            String initialSide = initialSideMap.get(exp);
            String canonicalSide = findRootAlias(initialSide, sideAliases);
            coalitions.computeIfAbsent(canonicalSide, k -> new ArrayList<>()).add(exp);
        }

        return coalitions;
    }

    private String findRootAlias(String side, Map<String, String> aliases) {
        String curr = side;
        while (aliases.containsKey(curr)) {
            curr = aliases.get(curr);
        }
        return curr;
    }

    // =========================================================================
    // 3. PRE-BATTLE PEACEFUL NEGOTIATION (PeacefulResolutionEvaluator)
    // =========================================================================

    /**
     * Evaluates peaceful resolution opportunities before scheduling combat.
     * Returns true if the contest was fully resolved peacefully without combat.
     */
    public synchronized boolean evaluatePeacefulResolution(
            AnomalyContestRecord contest,
            WorldAnomaly anomaly,
            Map<String, List<Expedition>> coalitions,
            OrganizationSystem orgSys,
            Map<String, Person> peopleMap,
            int year,
            int month) {

        if (contest == null || anomaly == null || coalitions == null || coalitions.size() < 2) {
            return false;
        }

        contest.setState(ContestState.NEGOTIATING);

        // CASE A: SCOUTING WITHDRAWAL
        // Any coalition consisting entirely of SCOUTING_ONLY expeditions records intelligence and withdraws peacefully
        List<String> withdrawingCoalitions = new ArrayList<>();
        for (Map.Entry<String, List<Expedition>> entry : coalitions.entrySet()) {
            boolean allScouting = true;
            for (Expedition exp : entry.getValue()) {
                if (exp.getClaimObjective() != ClaimObjective.SCOUTING_ONLY) {
                    allScouting = false;
                    break;
                }
            }
            if (allScouting) {
                withdrawingCoalitions.add(entry.getKey());
                for (Expedition exp : entry.getValue()) {
                    exp.setStatus(ExpeditionStatus.RETURNING);
                    recordDiscovery(exp.getSponsorId(), anomaly, year, month);
                }
            }
        }

        for (String cId : withdrawingCoalitions) {
            coalitions.remove(cId);
        }

        if (coalitions.size() == 1) {
            String winningCoalition = coalitions.keySet().iterator().next();
            contest.transitionState(ContestState.RESOLVED_PEACEFULLY,
                    "Scouting expeditions withdrew peacefully; sole claiming coalition secures site: " + winningCoalition);
            contest.setWinningCoalitionSideId(winningCoalition);
            anomaly.transitionToSecuring(winningCoalition, anomaly.getSecuringDurationMonths());
            annualResolvedPeacefully++;
            annualSitesSecured++;
            return true;
        } else if (coalitions.isEmpty()) {
            contest.transitionState(ContestState.CANCELLED, "All expeditions were scouting only and withdrew peacefully");
            annualResolvedPeacefully++;
            return true;
        }

        // CASE B: COOPERATIVE JOINT EXTRACTION
        // If all remaining coalitions either have JOINT_EXTRACTION or have friendly/allied relations (>= +30) with no rival/hostile
        boolean allAgreeToJoint = true;
        List<String> remainingCoalitionIds = new ArrayList<>(coalitions.keySet());

        for (int i = 0; i < remainingCoalitionIds.size(); i++) {
            String sideA = remainingCoalitionIds.get(i);
            String orgA = sideA.startsWith("COALITION_") ? sideA.substring(10) : sideA;

            boolean sideAExplicitJoint = hasJointObjective(coalitions.get(sideA));

            for (int j = i + 1; j < remainingCoalitionIds.size(); j++) {
                String sideB = remainingCoalitionIds.get(j);
                String orgB = sideB.startsWith("COALITION_") ? sideB.substring(10) : sideB;

                boolean sideBExplicitJoint = hasJointObjective(coalitions.get(sideB));

                if (sideAExplicitJoint && sideBExplicitJoint) {
                    continue; // Both explicitly want joint extraction
                }

                if (orgSys != null) {
                    double score = orgSys.getRelationshipScore(orgA, orgB);
                    DiplomaticStatus status = orgSys.getDiplomaticStatus(orgA, orgB);
                    if (status.isHostile() || status == DiplomaticStatus.RIVAL || score < 30.0) {
                        allAgreeToJoint = false;
                        break;
                    }
                } else {
                    allAgreeToJoint = false;
                    break;
                }
            }
            if (!allAgreeToJoint) break;
        }

        if (allAgreeToJoint) {
            // Calculate proportional shares based on coalition personnel
            Map<String, Double> shares = new LinkedHashMap<>();
            double totalPower = 0.0;
            Map<String, Double> powerMap = new HashMap<>();

            for (String sideId : remainingCoalitionIds) {
                double pwr = computeCoalitionPower(coalitions.get(sideId), peopleMap);
                powerMap.put(sideId, pwr);
                totalPower += pwr;
            }

            for (String sideId : remainingCoalitionIds) {
                double share = (totalPower > 0.0) ? (powerMap.get(sideId) / totalPower) : (1.0 / remainingCoalitionIds.size());
                shares.put(sideId, share);
            }

            JointExtractionAgreement agreement = new JointExtractionAgreement(
                    "AGREEMENT:" + anomaly.getAnomalyId() + ":" + contest.getContestId(),
                    anomaly.getAnomalyId(),
                    contest.getContestId(),
                    remainingCoalitionIds,
                    shares,
                    year,
                    month
            );

            jointAgreements.put(agreement.getAgreementId(), agreement);
            contest.transitionState(ContestState.RESOLVED_PEACEFULLY,
                    "Cooperative Joint Extraction Agreement established between: " + remainingCoalitionIds);
            anomaly.transitionToControlled(); // Jointly controlled
            anomaly.setControllingOwnerId("JOINT_COALITION");
            annualResolvedPeacefully++;
            return true;
        }

        // CASE C: OVERMATCHED PRAGMATIC WITHDRAWAL
        // If a weaker coalition has <= 30% of strongest rival's power, leader Greed < 75, and relationship not sworn hostile (score > -85)
        double strongestPower = 0.0;
        String strongestSide = null;

        for (String sideId : remainingCoalitionIds) {
            double pwr = computeCoalitionPower(coalitions.get(sideId), peopleMap);
            if (pwr > strongestPower) {
                strongestPower = pwr;
                strongestSide = sideId;
            }
        }

        List<String> pragmaticWithdrawals = new ArrayList<>();
        for (String sideId : remainingCoalitionIds) {
            if (sideId.equals(strongestSide)) continue;
            double pwr = computeCoalitionPower(coalitions.get(sideId), peopleMap);

            if (strongestPower > 0.0 && (pwr / strongestPower) <= 0.30) {
                // Check leadership recklessness / hostility
                boolean reckless = false;
                List<Expedition> exps = coalitions.get(sideId);
                for (Expedition exp : exps) {
                    List<Person> personnel = exp.getLivingPresentPersonnel(peopleMap);
                    if (!personnel.isEmpty()) {
                        Person leader = personnel.get(0);
                        if (leader.getAetherProfile() != null && leader.getAetherProfile().getGreed() >= 0.75) {
                            reckless = true;
                            break;
                        }
                    }
                }

                String orgA = sideId.startsWith("COALITION_") ? sideId.substring(10) : sideId;
                String orgStrong = strongestSide.startsWith("COALITION_") ? strongestSide.substring(10) : strongestSide;
                boolean swornHostile = (orgSys != null && orgSys.getRelationshipScore(orgA, orgStrong) <= -85.0);

                if (!reckless && !swornHostile) {
                    pragmaticWithdrawals.add(sideId);
                    for (Expedition exp : exps) {
                        exp.setStatus(ExpeditionStatus.RETURNING);
                    }
                }
            }
        }

        for (String sideId : pragmaticWithdrawals) {
            coalitions.remove(sideId);
        }

        if (coalitions.size() == 1) {
            String winningCoalition = coalitions.keySet().iterator().next();
            contest.transitionState(ContestState.RESOLVED_PEACEFULLY,
                    "Overmatched rival coalitions executed pragmatic withdrawal; winning coalition: " + winningCoalition);
            contest.setWinningCoalitionSideId(winningCoalition);
            anomaly.transitionToSecuring(winningCoalition, anomaly.getSecuringDurationMonths());
            annualResolvedPeacefully++;
            annualSitesSecured++;
            return true;
        }

        // If opposing coalitions remain with incompatible claims -> proceed to battle
        contest.setState(ContestState.BATTLE_SCHEDULED);
        return false;
    }

    private boolean hasJointObjective(List<Expedition> exps) {
        if (exps == null) return false;
        for (Expedition e : exps) {
            if (e.getClaimObjective() == ClaimObjective.JOINT_EXTRACTION) return true;
        }
        return false;
    }

    private double computeCoalitionPower(List<Expedition> exps, Map<String, Person> peopleMap) {
        if (exps == null) return 0.0;
        double sum = 0.0;
        for (Expedition e : exps) {
            for (Person p : e.getLivingPresentPersonnel(peopleMap)) {
                PersonCombatant c = new PersonCombatant(p, "TEMP");
                DerivedPowerProfile prof = c.getDerivedPowerProfile();
                if (prof != null) {
                    sum += prof.getOverallEffectivePower();
                } else {
                    sum += c.getPhysicalAttackRating();
                }
            }
        }
        return Math.max(1.0, sum);
    }

    private void recordDiscovery(String discovererId, WorldAnomaly anomaly, int year, int month) {
        String discId = "DISC:" + anomaly.getAnomalyId() + ":" + discovererId;
        if (processedEventIds.add(discId)) {
            discoveries.add(new DiscoveryRecord(discId, anomaly.getAnomalyId(), discovererId, year, month,
                    (year * 12L) + month, anomaly.getPosX(), anomaly.getPosY()));
        }
    }

    // =========================================================================
    // 4. BATTLE REQUEST CONSTRUCTION & SKIRMISH RESOLUTION
    // =========================================================================

    /**
     * Executes an anomaly skirmish battle deterministically via Phase 2B.1 CombatEngine.
     */
    public synchronized BattleResult resolveAnomalySkirmish(
            AnomalyContestRecord contest,
            WorldAnomaly anomaly,
            Map<String, List<Expedition>> coalitions,
            Random random,
            ProgressionConfig progConfig,
            DemographicsSystem demographicsSystem,
            AwakeningSystem awakeningSystem,
            ProgressionSystem progressionSystem,
            OrganizationSystem organizationSystem,
            SimulationConfig simConfig,
            List<Couple> allCouples,
            BiConsumer<String, Boolean> eventRecorder,
            Map<String, Person> peopleMap,
            int year,
            int month,
            long currentTotalMonths) {

        if (contest == null || anomaly == null || coalitions == null || coalitions.size() < 2) {
            return null;
        }

        // Check if anomaly is depleted, lair, or expired
        if (anomaly.getState().isTerminal() || anomaly.getRemainingQuantity() <= 0.0) {
            contest.transitionState(ContestState.CANCELLED, "Deposit depleted or expired before combat");
            return null;
        }

        // Check stalemate cooldown
        if (anomaly.isReengagementCooldownActive(currentTotalMonths)) {
            contest.setState(ContestState.UNRESOLVED_STALEMATE);
            return null; // On cooldown
        }

        // Assemble eligible combatants across all coalitions
        List<Combatant> battleCombatants = new ArrayList<>();
        Map<String, Integer> activeCountsPerSide = new LinkedHashMap<>();

        for (Map.Entry<String, List<Expedition>> entry : coalitions.entrySet()) {
            String coalitionSideId = entry.getKey();
            int activeOnSide = 0;

            for (Expedition exp : entry.getValue()) {
                for (Person p : exp.getLivingPresentPersonnel(peopleMap)) {
                    // Filter: alive, not incapacitated, not already dead/escaped
                    CombatState cs = p.getCombatState();
                    if (cs != null && (cs.isDead() || cs.isIncapacitated() || cs.isEscaped() || cs.getCurrentHealth() <= 0.0)) {
                        continue;
                    }
                    Combatant c = new PersonCombatant(p, coalitionSideId);
                    battleCombatants.add(c);
                    activeOnSide++;
                }
            }
            activeCountsPerSide.put(coalitionSideId, activeOnSide);
        }

        // Verify at least 2 distinct coalitions retain at least 1 active combatant
        List<String> validSides = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : activeCountsPerSide.entrySet()) {
            if (entry.getValue() > 0) {
                validSides.add(entry.getKey());
            }
        }

        if (validSides.size() < 2) {
            if (validSides.size() == 1) {
                String winningSide = validSides.get(0);
                contest.transitionState(ContestState.RESOLVED_BY_VICTORY,
                        "Sole coalition with active combatants secures site without battle: " + winningSide);
                contest.setWinningCoalitionSideId(winningSide);
                anomaly.transitionToSecuring(winningSide, anomaly.getSecuringDurationMonths());
                annualResolvedByVictory++;
                annualSitesSecured++;
            } else {
                contest.transitionState(ContestState.CANCELLED, "Zero active combatants present across all coalitions");
            }
            return null;
        }

        // Construct deterministic BattleRequest
        String battleId = "ANOMALY_SKIRMISH:" + anomaly.getAnomalyId() + ":" + contest.getContestId() + ":SEQ:" + contest.getEncounterSequence();
        contest.setBattleId(battleId);
        contest.setState(ContestState.BATTLE_IN_PROGRESS);
        activeBattleContestMap.put(battleId, contest);

        BattleRequest request = new BattleRequest(
                battleId,
                year,
                month,
                BattleType.ANOMALY_SKIRMISH,
                anomaly.getPosX(),
                anomaly.getPosY(),
                1.0, // Open terrain
                battleCombatants
        );

        // Submit to CombatEngine
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
                15 // Max rounds
        );

        // Update telemetry counters
        annualSkirmishes++;
        if (validSides.size() == 2) {
            annualTwoSidedSkirmishes++;
        } else {
            annualMultiSidedSkirmishes++;
        }

        // Record HistoricalGrievanceRecords for each fatality
        for (String deadId : result.getFatalities()) {
            String pId = deadId.startsWith("PERSON_") ? deadId.substring(7) : deadId;
            Person victim = peopleMap.get(pId);
            if (victim != null) {
                CombatState cs = victim.getCombatState();
                String killerPersonId = (cs != null) ? cs.getLastKillerPersonId() : null;
                String killerSideId = (cs != null) ? cs.getLastKillerSideId() : result.getVictoriousSideId();

                List<String> responsibleOrgs = new ArrayList<>();
                for (String side : validSides) {
                    if (!side.equals("COALITION_" + victim.getPrimaryOrganizationId())) {
                        if (side.startsWith("COALITION_")) {
                            responsibleOrgs.add(side.substring(10));
                        }
                    }
                }

                HistoricalGrievanceRecord grievance = new HistoricalGrievanceRecord(
                        "GRIEVANCE:" + battleId + ":" + victim.getId(),
                        battleId,
                        contest.getContestId(),
                        anomaly.getAnomalyId(),
                        year,
                        month,
                        String.valueOf(victim.getId()),
                        victim.getFamily() != null ? victim.getFamily().getId() : 0,
                        victim.getPrimaryOrganizationId() != null ? victim.getPrimaryOrganizationId() : "NONE",
                        victim.getVillage() != null ? victim.getVillage().getId() : null,
                        killerPersonId,
                        killerSideId,
                        responsibleOrgs,
                        "Slain in anomaly skirmish " + battleId
                );

                grievanceRecords.add(grievance);
                annualGrievancesLogged++;
            }
        }

        // Invoke Phase 2B.2 OrganizationSystem completed battle handling at most once
        if (organizationSystem != null) {
            organizationSystem.processCompletedBattleResult(result, year, month, peopleMap.values());
        }

        // Post-Battle Site State & Expedition Handling
        handlePostBattleSiteConsequences(contest, anomaly, coalitions, result, peopleMap, currentTotalMonths);

        activeBattleContestMap.remove(battleId);
        return result;
    }

    // =========================================================================
    // 5. POST-BATTLE SITE SECURING & RESOURCE RECOVERY
    // =========================================================================

    private void handlePostBattleSiteConsequences(
            AnomalyContestRecord contest,
            WorldAnomaly anomaly,
            Map<String, List<Expedition>> coalitions,
            BattleResult result,
            Map<String, Person> peopleMap,
            long currentTotalMonths) {

        String victorSide = result.getVictoriousSideId();
        boolean clearVictory = victorSide != null && (result.getOutcome() == BattleOutcome.VICTORY || result.getOutcome() == BattleOutcome.WITHDRAWAL);

        if (clearVictory) {
            // Check if victorious coalition actually has at least 1 living active member present
            List<Expedition> victorExps = coalitions.get(victorSide);
            int victorLivingCount = 0;
            if (victorExps != null) {
                for (Expedition exp : victorExps) {
                    for (Person p : exp.getLivingPresentPersonnel(peopleMap)) {
                        CombatState cs = p.getCombatState();
                        if (cs == null || (!cs.isDead() && !cs.isEscaped() && cs.getCurrentHealth() > 0.0)) {
                            victorLivingCount++;
                        }
                    }
                }
            }

            if (victorLivingCount >= 1) {
                contest.transitionState(ContestState.RESOLVED_BY_VICTORY,
                        "Skirmish resolved by victory for " + victorSide + " (" + victorLivingCount + " survivors present)");
                contest.setWinningCoalitionSideId(victorSide);
                anomaly.transitionToSecuring(victorSide, anomaly.getSecuringDurationMonths());
                annualResolvedByVictory++;
                annualSitesSecured++;

                // Defeated expeditions retreat or are destroyed; recover carried resources
                for (Map.Entry<String, List<Expedition>> entry : coalitions.entrySet()) {
                    if (entry.getKey().equals(victorSide)) continue;

                    for (Expedition defExp : entry.getValue()) {
                        int defSurvivors = defExp.getLivingMemberCount(peopleMap);
                        if (defSurvivors > 0) {
                            defExp.setStatus(ExpeditionStatus.RETURNING);
                        } else {
                            defExp.setStatus(ExpeditionStatus.DESTROYED);
                        }

                        // Resource recovery: carryCapPerPerson = 10.0
                        double carryCapPerPerson = 10.0;
                        double survivorCapacity = defSurvivors * carryCapPerPerson;
                        double initialCarried = defExp.getCarriedResources();

                        if (initialCarried > 0.0) {
                            double retained = Math.min(initialCarried, survivorCapacity);
                            double excess = initialCarried - retained;
                            defExp.setCarriedResources(retained);

                            double transferredToVictor = 0.0;
                            double returnedToDeposit = excess;

                            // Victor on-site expedition can claim excess if it has spare capacity
                            if (victorExps != null && !victorExps.isEmpty() && excess > 0.0) {
                                Expedition victorExp = victorExps.get(0);
                                double spareCap = victorExp.getMaxCarryCapacity() - victorExp.getCarriedResources();
                                if (spareCap > 0.0) {
                                    transferredToVictor = Math.min(excess, spareCap);
                                    victorExp.addCarriedResources(transferredToVictor);
                                    returnedToDeposit = excess - transferredToVictor;
                                }
                            }

                            if (returnedToDeposit > 0.0) {
                                anomaly.returnResources(returnedToDeposit);
                            }

                            CombatResourceRecoveryRecord rec = new CombatResourceRecoveryRecord(
                                    "RECOVERY:" + result.getBattleId() + ":" + defExp.getExpeditionId(),
                                    result.getBattleId(),
                                    anomaly.getAnomalyId(),
                                    defExp.getExpeditionId(),
                                    retained,
                                    returnedToDeposit,
                                    transferredToVictor,
                                    victorSide,
                                    result.getSimulationYear(),
                                    result.getSimulationMonth()
                            );
                            recoveryRecords.add(rec);
                        }
                    }
                }
                return;
            }
        }

        // STALEMATE or Multi-Side Survivors
        contest.transitionState(ContestState.UNRESOLVED_STALEMATE, "Battle ended in stalemate or mutual exhaustion");
        anomaly.setState(AnomalyState.CONTESTED);
        anomaly.setReengagementCooldownUntilMonth(currentTotalMonths + 2L); // 2-month cooldown
        contest.setReengagementCooldownUntilMonth(currentTotalMonths + 2L);
        annualStalemates++;
    }

    // =========================================================================
    // 6. CHALLENGING A SECURING OR CONTROLLED SITE
    // =========================================================================

    /**
     * Handles arrival of a non-allied expedition challenging a SECURING or CONTROLLED site.
     */
    public synchronized AnomalyContestRecord challengeSecuringOrControlledSite(
            WorldAnomaly anomaly,
            Expedition challengerExpedition,
            OrganizationSystem orgSys,
            int year,
            int month,
            long currentTotalMonths) {

        if (anomaly == null || challengerExpedition == null) return null;
        if (anomaly.getState() != AnomalyState.SECURING && anomaly.getState() != AnomalyState.CONTROLLED) {
            return null;
        }

        String currentController = anomaly.getControllingOwnerId();
        String challengerOrg = challengerExpedition.getSponsorId();

        // If challenger is allied with current controller, no challenge occurs
        if (orgSys != null && currentController != null && currentController.startsWith("COALITION_")) {
            String controllerOrg = currentController.substring(10);
            if (orgSys.areAllied(controllerOrg, challengerOrg)) {
                return null;
            }
        }

        // Pause extraction and transition to CONTESTED
        anomaly.transitionToContested("CONTEST:" + anomaly.getAnomalyId() + ":SEQ:" + anomaly.incrementEncounterSequence());
        annualSitesChallenged++;

        String contestId = anomaly.getCurrentContestId();
        AnomalyContestRecord contest = new AnomalyContestRecord(
                contestId,
                anomaly.getAnomalyId(),
                anomaly.getEncounterSequence(),
                ContestState.PENDING_RESOLUTION,
                year,
                month,
                currentTotalMonths,
                Collections.singletonList(challengerExpedition.getExpeditionId())
        );

        contests.put(contest.getContestId(), contest);
        return contest;
    }

    // =========================================================================
    // 7. MONTHLY ORCHESTRATION PIPELINE
    // =========================================================================

    /**
     * Monthly simulation tick advancing anomalies, muster timers, mutation deadlines,
     * arrival batching, and contest resolutions.
     */
    public synchronized void processMonthlyContests(
            int currentYear,
            int currentMonth,
            long currentTotalMonths,
            List<Person> allPeople,
            SimulationConfig config,
            ProgressionConfig progConfig,
            DemographicsSystem demographicsSystem,
            AwakeningSystem awakeningSystem,
            ProgressionSystem progressionSystem,
            OrganizationSystem organizationSystem,
            List<Couple> allCouples,
            BiConsumer<String, Boolean> eventRecorder) {
        processMonthlyContests(currentYear, currentMonth, currentTotalMonths, allPeople, config,
                progConfig, demographicsSystem, awakeningSystem, progressionSystem,
                organizationSystem, allCouples, null, eventRecorder);
    }

    public synchronized void processMonthlyContests(
            int currentYear,
            int currentMonth,
            long currentTotalMonths,
            List<Person> allPeople,
            SimulationConfig config,
            ProgressionConfig progConfig,
            DemographicsSystem demographicsSystem,
            AwakeningSystem awakeningSystem,
            ProgressionSystem progressionSystem,
            OrganizationSystem organizationSystem,
            List<Couple> allCouples,
            Random seededRandom,
            BiConsumer<String, Boolean> eventRecorder) {

        // Parity with Mundane Era: Zero anomaly skirmishes in Years 0-49
        int awakeningStart = (config != null) ? config.getAwakeningStartYear() : 50;
        if (currentYear < awakeningStart) {
            return;
        }

        Map<String, Person> peopleMap = buildPersonMap(allPeople);
        Random random = (seededRandom != null) ? seededRandom : new Random(currentYear * 1000L + currentMonth * 31L);

        // Step A: Advance securing timers and mutation deadlines
        for (WorldAnomaly anomaly : anomalies.values()) {
            if (anomaly.getState() == AnomalyState.SECURING) {
                int rem = anomaly.getSecuringRemainingMonths() - 1;
                anomaly.setSecuringRemainingMonths(rem);
                if (rem <= 0) {
                    anomaly.transitionToControlled();
                    if (eventRecorder != null) {
                        eventRecorder.accept(
                                String.format("Year %d (Month %d): Anomaly %s successfully secured and now CONTROLLED by %s.",
                                        currentYear, currentMonth, anomaly.getName(), anomaly.getControllingOwnerId()),
                                config != null && config.isPrintIndividualEvents()
                        );
                    }
                }
            }

            // 6-Month Mutation Deadline Precedence
            if (anomaly.hasMutationDeadlineExpired(currentTotalMonths)) {
                AnomalyContestRecord activeContest = contests.get(anomaly.getCurrentContestId());
                if (activeContest == null) {
                    for (AnomalyContestRecord c : contests.values()) {
                        if (anomaly.getAnomalyId().equals(c.getAnomalyId()) && c.getState() != ContestState.EXPIRED && !c.getState().isResolved()) {
                            activeContest = c;
                            break;
                        }
                    }
                }
                if (activeContest != null && activeContest.getState() != ContestState.EXPIRED && !activeContest.getState().isResolved()) {
                    activeContest.transitionState(ContestState.EXPIRED, "6-month mutation deadline struck; site mutated into monster lair");
                }
                anomaly.transitionToMonsterLair();
                annualExpiredToLair++;
                if (eventRecorder != null) {
                    eventRecorder.accept(
                            String.format("Year %d (Month %d): Anomaly %s unclaimed after 6 months; mutated into MONSTER_LAIR_PENDING!",
                                    currentYear, currentMonth, anomaly.getName()),
                            config != null && config.isPrintIndividualEvents()
                    );
                }
            }
        }

        // Step B: Evaluate active contests
        for (AnomalyContestRecord contest : new ArrayList<>(contests.values())) {
            if (contest.getState().isTerminal()) continue;

            WorldAnomaly anomaly = anomalies.get(contest.getAnomalyId());
            if (anomaly == null || anomaly.getState().isTerminal()) continue;

            annualContestsProcessed++;

            // Collect expeditions present at anomaly site
            List<Expedition> presentExpeditions = new ArrayList<>();
            for (String expId : contest.getParticipatingExpeditionIds()) {
                Expedition exp = expeditions.get(expId);
                if (exp != null && exp.getStatus().isActive() && exp.isAtCoordinates(anomaly.getPosX(), anomaly.getPosY(), 2.0)) {
                    presentExpeditions.add(exp);
                }
            }

            Map<String, List<Expedition>> coalitions = resolveCoalitions(presentExpeditions, organizationSystem, peopleMap);
            contest.setCoalitionSideIds(coalitions.keySet());

            // Peaceful negotiation check
            boolean resolvedPeacefully = evaluatePeacefulResolution(contest, anomaly, coalitions, organizationSystem, peopleMap, currentYear, currentMonth);
            if (resolvedPeacefully) {
                continue;
            }

            // Tactical Skirmish Resolution
            resolveAnomalySkirmish(
                    contest, anomaly, coalitions, random, progConfig,
                    demographicsSystem, awakeningSystem, progressionSystem,
                    organizationSystem, config, allCouples, eventRecorder,
                    peopleMap, currentYear, currentMonth, currentTotalMonths
            );
        }
    }

    // =========================================================================
    // 8. 4-BUCKET RESOURCE CONSERVATION ACCOUNTING
    // =========================================================================

    public synchronized ResourceConservationSnapshot validateConservation(String anomalyId) {
        WorldAnomaly anomaly = anomalies.get(anomalyId);
        if (anomaly == null) return null;

        double deposit = anomaly.getRemainingQuantity();
        double carried = 0.0;
        for (Expedition exp : expeditions.values()) {
            if (anomalyId.equals(exp.getTargetAnomalyId()) && exp.getStatus().isActive()) {
                carried += exp.getCarriedResources();
            }
        }

        double delivered = anomaly.getDeliveredTotal();
        double mutatedOrAbandoned = anomaly.getMutatedOrAbandonedTotal();

        return new ResourceConservationSnapshot(
                anomalyId,
                anomaly.getInitialQuantity(),
                deposit,
                carried,
                delivered,
                mutatedOrAbandoned
        );
    }

    public synchronized boolean validateAllConservation(double tolerance) {
        for (WorldAnomaly anomaly : anomalies.values()) {
            ResourceConservationSnapshot snap = validateConservation(anomaly.getAnomalyId());
            if (snap != null && !snap.isConserved(tolerance)) {
                return false;
            }
        }
        return true;
    }

    // =========================================================================
    // 9. TELEMETRY & ANNUAL SUMMARY GENERATION
    // =========================================================================

    public synchronized String generateAnnualSummary(int currentYear) {
        if (anomalies.isEmpty() && contests.isEmpty()) {
            return null;
        }

        int activeAnomalies = 0;
        int securedAnomalies = 0;
        int controlledAnomalies = 0;
        int contestedAnomalies = 0;
        int lairAnomalies = 0;

        for (WorldAnomaly a : anomalies.values()) {
            switch (a.getState()) {
                case SECURING: securedAnomalies++; break;
                case CONTROLLED: controlledAnomalies++; break;
                case CONTESTED:
                case UNRESOLVED_STALEMATE: contestedAnomalies++; break;
                case MONSTER_LAIR_PENDING: lairAnomalies++; break;
                default: break;
            }
            if (!a.getState().isTerminal()) {
                activeAnomalies++;
            }
        }

        return String.format(
                "Anomalies[Act:%d Ctrl:%d Sec:%d Cnt:%d Lair:%d] | Skirmishes[Yr:%d 2-way:%d Multi:%d Stalemate:%d] | Loot:%.0fi Griev:%d",
                activeAnomalies, controlledAnomalies, securedAnomalies, contestedAnomalies, lairAnomalies,
                annualSkirmishes, annualTwoSidedSkirmishes, annualMultiSidedSkirmishes, annualStalemates,
                annualLootedIron, annualGrievancesLogged
        );
    }

    private Map<String, Person> buildPersonMap(Collection<Person> people) {
        Map<String, Person> map = new HashMap<>();
        if (people == null) return map;
        for (Person p : people) {
            if (p != null) {
                map.put(String.valueOf(p.getId()), p);
                map.put("PERSON_" + p.getId(), p);
            }
        }
        return map;
    }
}
