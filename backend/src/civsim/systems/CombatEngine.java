package civsim.systems;

import civsim.ProgressionConfig;
import civsim.SimulationConfig;
import civsim.dto.BattleRoundRecordDto;
import civsim.model.AetherAspect;
import civsim.model.AttributeType;
import civsim.model.BattleOutcome;
import civsim.model.BattleRequest;
import civsim.model.BattleResult;
import civsim.model.BattleType;
import civsim.model.CombatActionType;
import civsim.model.CombatInjury;
import civsim.model.CombatInjurySeverity;
import civsim.model.CombatState;
import civsim.model.Combatant;
import civsim.model.CombatantType;
import civsim.model.Couple;
import civsim.model.DerivedPowerProfile;
import civsim.model.Person;
import civsim.model.ProgressionEvent;
import civsim.model.ProgressionEventType;
import civsim.model.SupernaturalClass;
import civsim.model.TemporaryCombatEffect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Phase 2B.1 — Central deterministic, turn-based tactical combat engine.
 * Resolves encounters between mortal soldiers, awakened champions, bonded beasts, and monsters.
 */
public class CombatEngine {

    /** Default maximum battle duration in turns */
    public static final int DEFAULT_MAX_BATTLE_ROUNDS = 12;

    /** Bounded battleId LRU cache (last 100 battles) to prevent memory accumulation */
    private static final Map<String, BattleResult> battleCache = Collections.synchronizedMap(
            new LinkedHashMap<String, BattleResult>(128, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, BattleResult> eldest) {
                    return size() > 100;
                }
            }
    );
    private static final Map<String, List<Combatant>> battleParticipants = Collections.synchronizedMap(
            new LinkedHashMap<String, List<Combatant>>(128, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, List<Combatant>> eldest) {
                    return size() > 100;
                }
            }
    );
    private static final Map<String, List<BattleRoundRecordDto>> battleRoundLogs = Collections.synchronizedMap(
            new LinkedHashMap<String, List<BattleRoundRecordDto>>(128, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, List<BattleRoundRecordDto>> eldest) {
                    return size() > 100;
                }
            }
    );

    private final int configuredMaxRounds;

    public CombatEngine() {
        this(DEFAULT_MAX_BATTLE_ROUNDS);
    }

    public CombatEngine(int maxBattleRounds) {
        this.configuredMaxRounds = maxBattleRounds > 0 ? maxBattleRounds : DEFAULT_MAX_BATTLE_ROUNDS;
    }

    public BattleResult resolve(BattleRequest request, Random random) {
        return resolveBattle(request, random, ProgressionConfig.createDefault(), null, null, null, null, null, null, this.configuredMaxRounds);
    }

    public BattleResult resolveBattle(BattleRequest request, Random random) {
        return resolveBattle(request, random, ProgressionConfig.createDefault(), null, null, null, null, null, null, this.configuredMaxRounds);
    }

    public BattleResult resolveBattle(BattleRequest request, Random random, ProgressionConfig progConfig,
                                             DemographicsSystem demographicsSystem) {
        return resolveBattle(request, random, progConfig, demographicsSystem, null, null, null, null, null, this.configuredMaxRounds);
    }

    // Annual telemetry counters
    private static int annualBattles = 0;
    private static int annualVictories = 0;
    private static int annualStalemates = 0;
    private static int annualWithdrawals = 0;
    private static int annualFatalities = 0;
    private static int annualIncapacitated = 0;
    private static int annualEscapes = 0;
    private static int annualInjuries = 0;

    // Cumulative telemetry counters
    private static int cumulativeBattles = 0;
    private static int cumulativeFatalities = 0;

    public static void clearCache() {
        battleCache.clear();
        battleParticipants.clear();
        battleRoundLogs.clear();
    }

    public static List<BattleRoundRecordDto> getCachedBattleRounds(String battleId) {
        if (battleId == null) return null;
        return battleRoundLogs.get(battleId);
    }

    public static int getBattleCacheSize() {
        return battleCache.size();
    }

    public static BattleResult getCachedBattle(String battleId) {
        return battleCache.get(battleId);
    }

    public static List<BattleResult> getAllCachedBattles() {
        return new ArrayList<>(battleCache.values());
    }

    public static void resetWorldState() {
        clearCache();
        resetYearlyCounters();
        cumulativeBattles = 0;
        cumulativeFatalities = 0;
    }

    public static void cacheBattleResultForTesting(String battleId, BattleResult result) {
        battleCache.put(battleId, result);
    }

    public static void resetYearlyCounters() {
        annualBattles = 0;
        annualVictories = 0;
        annualStalemates = 0;
        annualWithdrawals = 0;
        annualFatalities = 0;
        annualIncapacitated = 0;
        annualEscapes = 0;
        annualInjuries = 0;
    }

    public static String generateAnnualCombatSummary() {
        if (annualBattles == 0 && cumulativeBattles == 0) {
            return null;
        }
        return String.format(
                "Combat: Yr(battles:%d vic:%d stm:%d wdr:%d dth:%d incap:%d esc:%d inj:%d) | Cum(battles:%d dth:%d)",
                annualBattles, annualVictories, annualStalemates, annualWithdrawals,
                annualFatalities, annualIncapacitated, annualEscapes, annualInjuries,
                cumulativeBattles, cumulativeFatalities
        );
    }

    public static void commitCombatCasualties(BattleResult result, Collection<? extends Combatant> combatants, int currentYear, DemographicsSystem demographicsSystem, SimulationConfig config) {
        if (result == null || demographicsSystem == null || combatants == null) return;
        EquipmentCraftingSystem ecs = EquipmentCraftingSystem.getActiveInstance();
        ClanHeirloomSystem chs = ClanHeirloomSystem.getActiveInstance();
        for (String deadId : result.getFatalities()) {
            for (Combatant c : combatants) {
                if (c.getCombatantId().equals(deadId) && c.getPerson() != null && c.getPerson().isAlive()) {
                    if (chs != null && ecs != null) {
                        chs.handleBearerDeathAtSite(c.getPerson(), result.getBattleId(), ecs);
                    }
                    String cause = String.format("slain in combat (%s %s)",
                            result.getBattleType().name(), result.getBattleId());
                    demographicsSystem.killPerson(c.getPerson(), Collections.emptyList(), currentYear, cause, config, null);
                }
            }
        }
    }

    public static void commitCombatCasualties(BattleResult result, int currentYear, DemographicsSystem demographicsSystem, SimulationConfig config) {
        if (result == null || demographicsSystem == null) return;
        List<Combatant> participants = battleParticipants.get(result.getBattleId());
        if (participants != null) {
            commitCombatCasualties(result, participants, currentYear, demographicsSystem, config);
        }
    }

    public static List<Combatant> getBattleParticipants(String battleId) {
        if (battleId == null) return null;
        return battleParticipants.get(battleId);
    }

    // --- Static Public Overloads ---

    public static BattleResult resolveBattle(BattleRequest request, Random random, ProgressionConfig progConfig,
                                             DemographicsSystem demographicsSystem, AwakeningSystem awakeningSystem,
                                             ProgressionSystem progressionSystem, SimulationConfig simConfig,
                                             List<Couple> allCouples, BiConsumer<String, Boolean> eventRecorder) {
        return resolveBattle(request, random, progConfig, demographicsSystem, awakeningSystem, progressionSystem, simConfig, allCouples, eventRecorder, DEFAULT_MAX_BATTLE_ROUNDS);
    }

    /**
     * Resolves a tactical battle encounter deterministically.
     */
    public static BattleResult resolveBattle(
            BattleRequest request,
            Random random,
            ProgressionConfig progConfig,
            DemographicsSystem demographicsSystem,
            AwakeningSystem awakeningSystem,
            ProgressionSystem progressionSystem,
            SimulationConfig simConfig,
            List<Couple> allCouples,
            BiConsumer<String, Boolean> eventRecorder,
            int maxRoundsParam) {

        if (request == null || request.getBattleId() == null || request.getBattleId().trim().isEmpty()) {
            return new BattleResult(
                    (request != null) ? request.getBattleId() : "INVALID",
                    (request != null) ? request.getSimulationYear() : 0,
                    (request != null) ? request.getSimulationMonth() : 0,
                    BattleType.SETTLEMENT_WARFARE,
                    BattleOutcome.INVALID_REQUEST,
                    null, 0,
                    Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(),
                    Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                    0, "Battle rejected: null or blank battleId"
            );
        }

        // 1. Idempotency Check: return cached result without rerolling or re-applying damage
        BattleResult cached = battleCache.get(request.getBattleId());
        if (cached != null) {
            return cached;
        }

        Random rng = (random != null) ? random : new Random(request.getBattleId().hashCode());
        ProgressionConfig pConfig = (progConfig != null) ? progConfig : ProgressionConfig.createDefault();
        int maxRounds = maxRoundsParam > 0 ? maxRoundsParam : DEFAULT_MAX_BATTLE_ROUNDS;

        // 2. Pre-battle participant filtering
        Set<String> seenIds = new HashSet<>();
        List<Combatant> validCombatants = new ArrayList<>();
        Map<String, Integer> initialCountsPerSide = new HashMap<>();

        for (Combatant c : request.getCombatants()) {
            if (c == null) continue;
            if (!seenIds.add(c.getCombatantId())) continue; // filter duplicate IDs
            if (!c.isAlive()) continue;                     // filter already-dead participants

            validCombatants.add(c);
            initialCountsPerSide.put(c.getSideId(), initialCountsPerSide.getOrDefault(c.getSideId(), 0) + 1);

            // Initialize or recalculate CombatState pools
            CombatState cs = c.getCombatState();
            if (cs != null) {
                if (!cs.isInitializedInLife()) {
                    cs.initializeFullPools(
                            c.getEffectiveVitality(),
                            c.getEffectiveEndurance(),
                            c.getEffectiveAetherCapacity(),
                            c.getEffectiveAetherControl(),
                            c.isAwakened()
                    );
                } else {
                    cs.recalculateMaxPools(
                            c.getEffectiveVitality(),
                            c.getEffectiveEndurance(),
                            c.getEffectiveAetherCapacity(),
                            c.getEffectiveAetherControl(),
                            c.isAwakened()
                    );
                }
                cs.initMoraleForBattle(c.getEffectiveWillpower());
                cs.clearEncounterEffects();
            }
        }

        // Verify at least two distinct hostile sides have active combatants
        Set<String> activeSides = new HashSet<>();
        for (Combatant c : validCombatants) {
            if (c.canActInCombat()) {
                activeSides.add(c.getSideId());
            }
        }

        if (activeSides.size() < 2) {
            BattleResult invalidResult = new BattleResult(
                    request.getBattleId(),
                    request.getSimulationYear(),
                    request.getSimulationMonth(),
                    request.getBattleType(),
                    BattleOutcome.INVALID_REQUEST,
                    null, 0,
                    initialCountsPerSide, initialCountsPerSide, Collections.emptyMap(),
                    Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                    0, "Battle terminated immediately: fewer than 2 distinct active hostile sides"
            );
            battleCache.put(request.getBattleId(), invalidResult);
            return invalidResult;
        }

        Map<String, Double> damageDealtPerSide = new HashMap<>();
        for (String side : initialCountsPerSide.keySet()) {
            damageDealtPerSide.put(side, 0.0);
        }

        List<String> fatalities = new ArrayList<>();
        List<String> incapacitated = new ArrayList<>();
        List<String> escaped = new ArrayList<>();
        int injuriesInflictedCount = 0;
        int completedRounds = 0;
        boolean groupWithdrawalOccurred = false;

        long currentTotalMonths = (long) request.getSimulationYear() * 12 + request.getSimulationMonth();
        List<BattleRoundRecordDto> roundLogs = new ArrayList<>();
        int actionSeq = 1;

        // 3. Turn-Based Battle Loop
        for (int round = 1; round <= maxRounds; round++) {
            completedRounds = round;

            // Step A: Check Group Withdrawal at the start of the round
            Set<String> sidesInPlay = getSidesWithActiveCombatants(validCombatants);
            if (sidesInPlay.size() < 2) break;

            for (String side : new ArrayList<>(sidesInPlay)) {
                double avgMorale = computeSideAverageMorale(validCombatants, side);
                double sidePower = computeSideActivePower(validCombatants, side);
                double opposingPower = computeOpposingActivePower(validCombatants, side);

                if (avgMorale < 20.0 && sidePower < (0.35 * opposingPower)) {
                    // Group Withdrawal triggered!
                    groupWithdrawalOccurred = true;
                    for (Combatant c : validCombatants) {
                        if (c.getSideId().equals(side) && c.canActInCombat()) {
                            CombatInjury inj = null;
                            // Interception check: 30% chance intercepted
                            if (rng.nextDouble() < 0.30) {
                                c.getCombatState().takeDamage(8.0);
                                inj = new CombatInjury(
                                        "INJ_WITHDRAW_" + request.getBattleId() + "_" + c.getCombatantId(),
                                        CombatInjurySeverity.MINOR_INJURY,
                                        request.getSimulationYear(),
                                        request.getSimulationMonth(),
                                        currentTotalMonths + 1,
                                        "Parting strike during group withdrawal"
                                );
                                c.getCombatState().addInjury(inj);
                                injuriesInflictedCount++;
                            }
                            c.getCombatState().setEscaped(true);
                            escaped.add(c.getCombatantId());
                            roundLogs.add(new BattleRoundRecordDto(
                                    round,
                                    actionSeq++,
                                    c.getCombatantId(),
                                    c.getName(),
                                    null,
                                    null,
                                    "GROUP_WITHDRAWAL",
                                    0.0,
                                    0.0,
                                    0.0,
                                    0.0,
                                    (inj != null ? "MINOR_INJURY" : null),
                                    false,
                                    true,
                                    c.getName() + " withdrew from the battle with their unit."
                            ));
                        }
                    }
                }
            }

            // Check if withdrawal left fewer than 2 sides
            sidesInPlay = getSidesWithActiveCombatants(validCombatants);
            if (sidesInPlay.size() < 2) break;

            // Step B: Calculate Initiative and Sort Active Combatants
            List<CombatantInitiative> initiativeList = new ArrayList<>();
            for (Combatant c : validCombatants) {
                if (c.canActInCombat()) {
                    double roll = rng.nextDouble() * 5.0;
                    double init = (0.50 * c.getEffectiveAgility())
                            + (0.30 * c.getEffectivePerception())
                            + (0.20 * c.getEffectiveAetherControl())
                            + roll;
                    initiativeList.add(new CombatantInitiative(c, init));
                }
            }

            initiativeList.sort(Comparator
                    .comparingDouble(CombatantInitiative::getInitiative).reversed()
                    .thenComparing(ci -> ci.getCombatant().getCombatantId()));

            // Step C: Iterate Actions in Initiative Order
            for (CombatantInitiative ci : initiativeList) {
                Combatant actor = ci.getCombatant();
                if (!actor.isAlive() || !actor.canActInCombat()) continue;

                // Check if enemies remain
                List<Combatant> livingEnemies = getActiveEnemies(validCombatants, actor.getSideId());
                if (livingEnemies.isEmpty()) break;

                CombatState actorState = actor.getCombatState();

                // Personal Escape Evaluation if HP < 30%
                if (actorState.getCurrentHealth() < (0.30 * actorState.getMaxHealth())
                        && !actorState.hasAttemptedEscapeThisBattle()) {

                    actorState.setNormalEscapeAttemptedThisBattle(true);

                    int alliesActive = countActiveOnSide(validCombatants, actor.getSideId());
                    int enemiesActive = livingEnemies.size();
                    double outnumberedPenalty = (enemiesActive >= 2 * alliesActive) ? 0.10 : 0.0;
                    double riftwalkerBonus = (actor.isAwakened() && actor.getSupernaturalClass() == SupernaturalClass.RIFTWALKER) ? 0.15 : 0.0;

                    double pEscape = Math.max(0.05, Math.min(0.85,
                            0.40 + ((actor.getEffectiveAgility() - 20.0) / 150.0)
                                    + ((actorState.getCurrentMorale() - 50.0) / 250.0)
                                    + riftwalkerBonus - outnumberedPenalty));

                    if (rng.nextDouble() < pEscape) {
                        // Personal escape succeeded
                        actorState.setEscaped(true);
                        escaped.add(actor.getCombatantId());
                        CombatInjury escInj = new CombatInjury(
                                "INJ_ESC_" + request.getBattleId() + "_" + actor.getCombatantId(),
                                CombatInjurySeverity.MODERATE_INJURY,
                                request.getSimulationYear(),
                                request.getSimulationMonth(),
                                currentTotalMonths + 2,
                                "Wounded during desperate combat retreat"
                        );
                        actorState.addInjury(escInj);
                        injuriesInflictedCount++;
                        roundLogs.add(new BattleRoundRecordDto(
                                round,
                                actionSeq++,
                                actor.getCombatantId(),
                                actor.getName(),
                                null,
                                null,
                                "ESCAPE_SUCCESS",
                                0.0,
                                0.0,
                                0.0,
                                0.0,
                                "MODERATE_INJURY",
                                false,
                                true,
                                actor.getName() + " successfully disengaged and fled the battlefield."
                        ));
                        continue;
                    } else {
                        // Personal escape failed: loses action for this turn
                        roundLogs.add(new BattleRoundRecordDto(
                                round,
                                actionSeq++,
                                actor.getCombatantId(),
                                actor.getName(),
                                null,
                                null,
                                "ESCAPE_FAILED",
                                0.0,
                                0.0,
                                0.0,
                                0.0,
                                null,
                                false,
                                false,
                                actor.getName() + " attempted to retreat but failed to disengage."
                        ));
                        continue;
                    }
                }

                // AI Action & Target Selection
                boolean actionExecuted = false;

                // Priority 1: VITALIST healing an injured ally below 50% HP
                if (actor.isAwakened() && actor.getSupernaturalClass() == SupernaturalClass.VITALIST
                        && actorState.getCurrentAether() >= 7.0 && actorState.getCurrentStamina() >= 2.0) {

                    Combatant injuredAlly = findMostInjuredAlly(validCombatants, actor);
                    if (injuredAlly != null && injuredAlly.getCombatState().getCurrentHealth() < (0.50 * injuredAlly.getCombatState().getMaxHealth())) {
                        actorState.consumeAether(7.0);
                        actorState.consumeStamina(2.0);

                        DerivedPowerProfile prof = actor.getDerivedPowerProfile();
                        double sustain = (prof != null) ? prof.getSustainedEfficiency() : 10.0;
                        double maxHealPerAction = 0.25 * injuredAlly.getCombatState().getMaxHealth();
                        double healAmount = (10.0 + (0.35 * sustain)) * (0.80 + (0.40 * (actor.getMastery() / 100.0)));
                        double actualHeal = Math.min(injuredAlly.getCombatState().getMaxHealth() - injuredAlly.getCombatState().getCurrentHealth(),
                                Math.min(maxHealPerAction, healAmount));

                        injuredAlly.getCombatState().heal(actualHeal);
                        actionExecuted = true;
                        roundLogs.add(new BattleRoundRecordDto(
                                round,
                                actionSeq++,
                                actor.getCombatantId(),
                                actor.getName(),
                                injuredAlly.getCombatantId(),
                                injuredAlly.getName(),
                                "VITALIST_HEAL",
                                0.0,
                                Math.round(actualHeal * 10.0) / 10.0,
                                2.0,
                                7.0,
                                null,
                                false,
                                false,
                                actor.getName() + " restored " + String.format("%.1f", actualHeal) + " health to " + injuredAlly.getName() + "."
                        ));
                    }
                }

                // Priority 2: WARDEN projecting barrier on unshielded ally or self
                if (!actionExecuted && actor.isAwakened() && actor.getSupernaturalClass() == SupernaturalClass.WARDEN
                        && actorState.getCurrentAether() >= 7.0 && actorState.getCurrentStamina() >= 3.0) {

                    Combatant barrierTarget = findBarrierTarget(validCombatants, actor);
                    if (barrierTarget != null) {
                        actorState.consumeAether(7.0);
                        actorState.consumeStamina(3.0);

                        DerivedPowerProfile prof = actor.getDerivedPowerProfile();
                        double defRes = (prof != null) ? prof.getDefensiveResilience() : 10.0;
                        double barrierCap = 15.0 + (0.60 * defRes);

                        barrierTarget.getCombatState().addEffect(new TemporaryCombatEffect(
                                TemporaryCombatEffect.EffectType.WARDEN_BARRIER, 2, barrierCap));
                        actionExecuted = true;
                        roundLogs.add(new BattleRoundRecordDto(
                                round,
                                actionSeq++,
                                actor.getCombatantId(),
                                actor.getName(),
                                barrierTarget.getCombatantId(),
                                barrierTarget.getName(),
                                "WARDEN_BARRIER",
                                0.0,
                                0.0,
                                3.0,
                                7.0,
                                null,
                                false,
                                false,
                                actor.getName() + " projected a protective barrier over " + barrierTarget.getName() + "."
                        ));
                    }
                }

                // Priority 3: Supernatural Primary Ability
                if (!actionExecuted && actor.isAwakened() && canExecutePrimaryAbility(actor)) {
                    Combatant target = selectDeterministicTarget(livingEnemies, actor);
                    if (target != null) {
                        double beforeHp = target.getCombatState().getCurrentHealth();
                        int beforeInj = target.getCombatState().getActiveInjuries().size();
                        resolvePrimaryAbility(actor, target, rng, request, currentTotalMonths, damageDealtPerSide,
                                fatalities, incapacitated, validCombatants);
                        double afterHp = target.getCombatState().getCurrentHealth();
                        double dealt = Math.max(0.0, beforeHp - afterHp);
                        boolean defeated = !target.isAlive() || target.getCombatState().isIncapacitated();
                        String inj = (target.getCombatState().getActiveInjuries().size() > beforeInj) ? "INJURY" : null;
                        roundLogs.add(new BattleRoundRecordDto(
                                round,
                                actionSeq++,
                                actor.getCombatantId(),
                                actor.getName(),
                                target.getCombatantId(),
                                target.getName(),
                                "ABILITY_" + actor.getSupernaturalClass().name(),
                                Math.round(dealt * 10.0) / 10.0,
                                0.0,
                                0.0,
                                0.0,
                                inj,
                                defeated,
                                false,
                                actor.getName() + " unleashed " + actor.getSupernaturalClass().getDisplayName() + " ability on " + target.getName() + " for " + String.format("%.1f", dealt) + " damage."
                        ));
                        actionExecuted = true;
                    }
                }

                // Priority 4: Physical Attack
                if (!actionExecuted && actorState.getCurrentStamina() >= 3.0) {
                    Combatant target = selectDeterministicTarget(livingEnemies, actor);
                    if (target != null) {
                        actorState.consumeStamina(3.0);
                        double beforeHp = target.getCombatState().getCurrentHealth();
                        int beforeInj = target.getCombatState().getActiveInjuries().size();
                        resolvePhysicalAttack(actor, target, 1.0, 0.0, 0.0, 0.0, rng, request, currentTotalMonths,
                                damageDealtPerSide, fatalities, incapacitated, validCombatants);
                        double afterHp = target.getCombatState().getCurrentHealth();
                        double dealt = Math.max(0.0, beforeHp - afterHp);
                        boolean defeated = !target.isAlive() || target.getCombatState().isIncapacitated();
                        String inj = (target.getCombatState().getActiveInjuries().size() > beforeInj) ? "INJURY" : null;
                        roundLogs.add(new BattleRoundRecordDto(
                                round,
                                actionSeq++,
                                actor.getCombatantId(),
                                actor.getName(),
                                target.getCombatantId(),
                                target.getName(),
                                "PHYSICAL_ATTACK",
                                Math.round(dealt * 10.0) / 10.0,
                                0.0,
                                3.0,
                                0.0,
                                inj,
                                defeated,
                                false,
                                actor.getName() + " struck " + target.getName() + " for " + String.format("%.1f", dealt) + " damage."
                        ));
                        actionExecuted = true;
                    }
                }

                // Priority 5: Defend (recovers +4.0 stamina, applies defensive stance)
                if (!actionExecuted) {
                    actorState.restoreStamina(4.0);
                    actorState.addEffect(new TemporaryCombatEffect(
                            TemporaryCombatEffect.EffectType.DEFENSIVE_STANCE, 1, 0.20));
                    roundLogs.add(new BattleRoundRecordDto(
                            round,
                            actionSeq++,
                            actor.getCombatantId(),
                            actor.getName(),
                            null,
                            null,
                            "DEFEND",
                            0.0,
                            0.0,
                            -4.0,
                            0.0,
                            null,
                            false,
                            false,
                            actor.getName() + " took a defensive stance and recovered stamina."
                    ));
                }
            }

            // Step D: Upkeep — decrement temporary effects
            for (Combatant c : validCombatants) {
                if (c.getCombatState() != null) {
                    c.getCombatState().tickEffects();
                }
            }

            // Morale penalty for outnumbered sides
            sidesInPlay = getSidesWithActiveCombatants(validCombatants);
            for (String side : sidesInPlay) {
                int activeCount = countActiveOnSide(validCombatants, side);
                int opposingCount = countOpposingActive(validCombatants, side);
                if (opposingCount >= (2 * activeCount)) {
                    modifySideMorale(validCombatants, side, -5.0);
                }
            }

            if (sidesInPlay.size() < 2) break;
        }

        // 4. Final Outcome Determination
        Set<String> remainingActiveSides = getSidesWithActiveCombatants(validCombatants);
        BattleOutcome outcome;
        String victoriousSideId = null;

        if (remainingActiveSides.size() == 1) {
            victoriousSideId = remainingActiveSides.iterator().next();
            outcome = groupWithdrawalOccurred ? BattleOutcome.WITHDRAWAL : BattleOutcome.VICTORY;
        } else if (remainingActiveSides.isEmpty()) {
            outcome = BattleOutcome.STALEMATE;
        } else {
            outcome = BattleOutcome.STALEMATE; // Reached maxBattleRounds
        }

        // 5. Post-Battle Commitments & Side Effects

        // A. Commit confirmed fatalities through DemographicsSystem
        if (demographicsSystem != null) {
            for (String deadId : fatalities) {
                for (Combatant c : validCombatants) {
                    if (c.getCombatantId().equals(deadId) && c.getPerson() != null && c.getPerson().isAlive()) {
                        String deathCause = String.format("slain in combat (%s %s)",
                                request.getBattleType().name(), request.getBattleId());
                        demographicsSystem.killPerson(
                                c.getPerson(),
                                allCouples != null ? allCouples : Collections.emptyList(),
                                request.getSimulationYear(),
                                deathCause,
                                simConfig,
                                eventRecorder
                        );
                    }
                }
            }
        }

        // B. Post-battle persistent injuries for survivors ending below 50% HP
        for (Combatant c : validCombatants) {
            if (c.isAlive() && c.getCombatState() != null) {
                CombatState cs = c.getCombatState();
                if (cs.getCurrentHealth() < (0.50 * cs.getMaxHealth()) && cs.getActiveInjuries().isEmpty()) {
                    CombatInjurySeverity sev = (cs.getCurrentHealth() < (0.30 * cs.getMaxHealth()))
                            ? CombatInjurySeverity.MODERATE_INJURY
                            : CombatInjurySeverity.MINOR_INJURY;
                    int dur = (sev == CombatInjurySeverity.MODERATE_INJURY) ? 2 : 1;
                    CombatInjury inj = new CombatInjury(
                            "INJ_POST_" + request.getBattleId() + "_" + c.getCombatantId(),
                            sev,
                            request.getSimulationYear(),
                            request.getSimulationMonth(),
                            currentTotalMonths + dur,
                            "Injuries sustained in tactical combat"
                    );
                    cs.addInjury(inj);
                    injuriesInflictedCount++;
                }
            }
        }

        // C. Post-battle progression events for surviving awakened participants
        if (progressionSystem != null) {
            for (Combatant c : validCombatants) {
                if (c.isAlive() && c.isAwakened() && c.getPerson() != null) {
                    String eventId = "COMBAT_EXP:" + request.getBattleId() + ":" + c.getPerson().getId();
                    ProgressionEvent progEvt = new ProgressionEvent(
                            eventId,
                            c.getPerson().getId(),
                            ProgressionEventType.DEMANDING_EVENT_SURVIVAL,
                            request.getSimulationYear(),
                            request.getSimulationMonth(),
                            1.2
                    );
                    progressionSystem.processEvent(c.getPerson(), progEvt, pConfig);
                }
            }
        }

        // D. Crimson Crucible awakening checks for surviving unawakened mortal trauma
        if (awakeningSystem != null && simConfig != null
                && request.getSimulationYear() >= simConfig.getAwakeningStartYear()) {
            for (Combatant c : validCombatants) {
                if (c.isAlive() && !c.isAwakened() && c.getPerson() != null) {
                    CombatState cs = c.getCombatState();
                    if (cs != null && (cs.hasSevereInjury() || cs.getCurrentHealth() < (0.25 * cs.getMaxHealth()))) {
                        String eventId = "CRUCIBLE_COMBAT:" + request.getBattleId() + ":" + c.getPerson().getId();
                        awakeningSystem.attemptCrimsonCrucibleCombat(
                                c.getPerson(),
                                eventId,
                                request.getSimulationYear(),
                                rng,
                                pConfig,
                                eventRecorder
                        );
                    }
                }
            }
        }

        // E. Clear encounter-only effects
        for (Combatant c : validCombatants) {
            if (c.getCombatState() != null) {
                c.getCombatState().clearEncounterEffects();
            }
        }

        // Build Final Counts Map
        Map<String, Integer> finalLivingCountsPerSide = new HashMap<>();
        for (Combatant c : validCombatants) {
            if (c.isAlive() && c.getCombatState() != null && !c.getCombatState().isDead()) {
                finalLivingCountsPerSide.put(c.getSideId(), finalLivingCountsPerSide.getOrDefault(c.getSideId(), 0) + 1);
            }
        }

        String summary = String.format("Battle %s (%s) concluded with %s after %d rounds (Victor: %s). Fatalities: %d, Incapacitated: %d, Escaped: %d",
                request.getBattleId(), request.getBattleType().getDisplayName(), outcome.getDisplayName(),
                completedRounds, (victoriousSideId != null ? victoriousSideId : "None"),
                fatalities.size(), incapacitated.size(), escaped.size());

        BattleResult result = new BattleResult(
                request.getBattleId(),
                request.getSimulationYear(),
                request.getSimulationMonth(),
                request.getBattleType(),
                outcome,
                victoriousSideId,
                completedRounds,
                initialCountsPerSide,
                finalLivingCountsPerSide,
                damageDealtPerSide,
                fatalities,
                incapacitated,
                escaped,
                injuriesInflictedCount,
                summary
        );

        battleCache.put(request.getBattleId(), result);
        battleParticipants.put(request.getBattleId(), new ArrayList<>(validCombatants));
        battleRoundLogs.put(request.getBattleId(), Collections.unmodifiableList(roundLogs));

        // Update Telemetry
        annualBattles++;
        cumulativeBattles++;
        if (outcome == BattleOutcome.VICTORY) annualVictories++;
        else if (outcome == BattleOutcome.WITHDRAWAL || outcome == BattleOutcome.PARTIAL_WITHDRAWAL) annualWithdrawals++;
        else if (outcome == BattleOutcome.STALEMATE) annualStalemates++;

        annualFatalities += fatalities.size();
        cumulativeFatalities += fatalities.size();
        annualIncapacitated += incapacitated.size();
        annualEscapes += escaped.size();
        annualInjuries += injuriesInflictedCount;

        // Phase 2C.1 Usage durability wear on battle participants
        EquipmentCraftingSystem ecs = EquipmentCraftingSystem.getActiveInstance();
        ClanHeirloomSystem chs = ClanHeirloomSystem.getActiveInstance();
        if (ecs != null && chs != null) {
            for (Combatant c : validCombatants) {
                if (c.getPerson() != null && c.getPerson().isAlive()) {
                    chs.applyCombatDurabilityWear(c.getPerson(), ecs, rng);
                }
            }
        }

        return result;
    }

    // --- Monthly Timeline Recovery ---

    /**
     * Phase 2B.1 — Monthly timeline recovery for all living entities with CombatState.
     */
    public static void processMonthlyRecovery(
            List<Person> allPeople,
            int currentYear,
            int currentMonth,
            long currentTotalMonths,
            SimulationConfig config) {

        if (allPeople == null) return;

        for (Person p : allPeople) {
            if (!p.isAlive()) continue;
            CombatState cs = p.getCombatState();
            if (cs == null) continue;

            // 1. Clear expired injuries
            cs.clearExpiredInjuries(currentTotalMonths);

            // 2. Resource recovery (only if not starving)
            boolean starving = (p.getVillage() != null && p.getVillage().isInFamine());
            if (!starving) {
                double healthRecoveryFraction = cs.hasSevereInjury() ? 0.15 : 0.35;
                cs.heal(healthRecoveryFraction * cs.getMaxHealth());
                cs.restoreStamina(0.60 * cs.getMaxStamina());
                cs.restoreAether(0.50 * cs.getMaxAether());
            }
        }
    }

    // --- Tactical Resolution Helpers ---

    private static boolean canExecutePrimaryAbility(Combatant actor) {
        SupernaturalClass cls = actor.getSupernaturalClass();
        if (cls == null) return false;
        CombatState cs = actor.getCombatState();

        switch (cls) {
            case VANGUARD:
                return cs.getCurrentStamina() >= 6.0 && cs.getCurrentAether() >= 3.0;
            case ELEMENTALIST:
                double stamCost = (actor.getAspect() == AetherAspect.DARK) ? 5.0 : 3.0;
                return cs.getCurrentAether() >= 8.0 && cs.getCurrentStamina() >= stamCost;
            case SEER:
                return cs.getCurrentAether() >= 5.0 && cs.getCurrentStamina() >= 2.0;
            case BEASTBINDER:
                return cs.getCurrentAether() >= 5.0 && cs.getCurrentStamina() >= 3.0;
            case RIFTWALKER:
                return cs.getCurrentAether() >= 9.0 && cs.getCurrentStamina() >= 4.0;
            default:
                return false;
        }
    }

    private static void resolvePrimaryAbility(
            Combatant actor, Combatant target, Random rng, BattleRequest request,
            long currentTotalMonths, Map<String, Double> damageDealtPerSide,
            List<String> fatalities, List<String> incapacitated, List<Combatant> allCombatants) {

        SupernaturalClass cls = actor.getSupernaturalClass();
        CombatState cs = actor.getCombatState();
        DerivedPowerProfile prof = actor.getDerivedPowerProfile();

        switch (cls) {
            case VANGUARD:
                cs.consumeStamina(6.0);
                cs.consumeAether(3.0);
                cs.addEffect(new TemporaryCombatEffect(TemporaryCombatEffect.EffectType.VANGUARD_REINFORCEMENT, 1, 0.25));
                resolvePhysicalAttack(actor, target, 1.35, 0.0, 0.0, 0.0, rng, request, currentTotalMonths,
                        damageDealtPerSide, fatalities, incapacitated, allCombatants);
                break;

            case ELEMENTALIST:
                double stamCost = (actor.getAspect() == AetherAspect.DARK) ? 5.0 : 3.0;
                cs.consumeAether(8.0);
                cs.consumeStamina(stamCost);
                double elemPotency = 12.0 + (0.85 * (prof != null ? prof.getOffensivePotency() : 15.0));
                double dealt = resolveElementalAttack(actor, target, elemPotency, rng, request, currentTotalMonths,
                        fatalities, incapacitated, allCombatants);
                damageDealtPerSide.put(actor.getSideId(), damageDealtPerSide.getOrDefault(actor.getSideId(), 0.0) + dealt);

                if (actor.getAspect() == AetherAspect.DARK && dealt > 0.0) {
                    target.getCombatState().modifyMorale(-5.0);
                    target.getCombatState().consumeStamina(5.0);
                }
                break;

            case SEER:
                cs.consumeAether(5.0);
                cs.consumeStamina(2.0);
                // Mark target with SeerInsight
                target.getCombatState().addEffect(new TemporaryCombatEffect(
                        TemporaryCombatEffect.EffectType.SEER_INSIGHT, 2, 0.12, target.getCombatantId()));
                // Boost personal dodge
                cs.addEffect(new TemporaryCombatEffect(
                        TemporaryCombatEffect.EffectType.SEER_INSIGHT, 2, 0.08));
                double seerPotency = 8.0 + (0.40 * (prof != null ? prof.getOffensivePotency() : 10.0));
                resolveElementalAttack(actor, target, seerPotency, rng, request, currentTotalMonths,
                        fatalities, incapacitated, allCombatants);
                break;

            case BEASTBINDER:
                cs.consumeAether(5.0);
                cs.consumeStamina(3.0);
                boolean hasBeast = hasAlliedBeast(allCombatants, actor.getSideId());
                double mult = hasBeast ? 1.25 : 1.15;
                if (hasBeast) {
                    for (Combatant c : allCombatants) {
                        if (c.getSideId().equals(actor.getSideId()) && c.getEntityType() == CombatantType.BONDED_BEAST) {
                            c.getCombatState().addEffect(new TemporaryCombatEffect(
                                    TemporaryCombatEffect.EffectType.BEAST_COORDINATION, 2, 0.20));
                            c.getCombatState().modifyMorale(10.0);
                        }
                    }
                }
                resolvePhysicalAttack(actor, target, mult, 0.0, 0.0, 0.0, rng, request, currentTotalMonths,
                        damageDealtPerSide, fatalities, incapacitated, allCombatants);
                break;

            case RIFTWALKER:
                cs.consumeAether(9.0);
                cs.consumeStamina(4.0);
                cs.addEffect(new TemporaryCombatEffect(TemporaryCombatEffect.EffectType.RIFTWALKER_BLINK, 1, 0.20));
                double riftPotency = 10.0 + (0.75 * (prof != null ? prof.getOffensivePotency() : 12.0));
                // Flanking strike bypasses 30% of target defense mitigation
                double riftDealt = resolveElementalAttackWithBypass(actor, target, riftPotency, 0.30, rng, request,
                        currentTotalMonths, fatalities, incapacitated, allCombatants);
                damageDealtPerSide.put(actor.getSideId(), damageDealtPerSide.getOrDefault(actor.getSideId(), 0.0) + riftDealt);
                break;

            default:
                break;
        }
    }

    private static void resolvePhysicalAttack(
            Combatant attacker, Combatant target, double actionCoefficient,
            double accuracyModifier, double evasionModifier, double bypassDefenseFraction,
            Random rng, BattleRequest request, long currentTotalMonths,
            Map<String, Double> damageDealtPerSide, List<String> fatalities,
            List<String> incapacitated, List<Combatant> allCombatants) {

        double attackPotency = attacker.getPhysicalAttackRating();

        // Check if attacker has BeastCoordination
        TemporaryCombatEffect beastCoord = attacker.getCombatState().getEffect(TemporaryCombatEffect.EffectType.BEAST_COORDINATION);
        if (beastCoord != null) {
            attackPotency *= (1.0 + beastCoord.getMagnitude());
        }

        // Exhaustion penalty
        if (attacker.getCombatState().getCurrentStamina() <= 0.0) {
            attackPotency *= 0.80;
        }

        double damageDealt = executeDamageResolution(
                attacker, target, attackPotency, actionCoefficient,
                accuracyModifier, evasionModifier, bypassDefenseFraction,
                rng, request, currentTotalMonths, fatalities, incapacitated, allCombatants
        );

        damageDealtPerSide.put(attacker.getSideId(),
                damageDealtPerSide.getOrDefault(attacker.getSideId(), 0.0) + damageDealt);
    }

    private static double resolveElementalAttack(
            Combatant attacker, Combatant target, double potency,
            Random rng, BattleRequest request, long currentTotalMonths,
            List<String> fatalities, List<String> incapacitated, List<Combatant> allCombatants) {
        return executeDamageResolution(attacker, target, potency, 1.0, 0.0, 0.0, 0.0,
                rng, request, currentTotalMonths, fatalities, incapacitated, allCombatants);
    }

    private static double resolveElementalAttackWithBypass(
            Combatant attacker, Combatant target, double potency, double bypassDefenseFraction,
            Random rng, BattleRequest request, long currentTotalMonths,
            List<String> fatalities, List<String> incapacitated, List<Combatant> allCombatants) {
        return executeDamageResolution(attacker, target, potency, 1.0, 0.0, 0.0, bypassDefenseFraction,
                rng, request, currentTotalMonths, fatalities, incapacitated, allCombatants);
    }

    private static double executeDamageResolution(
            Combatant attacker, Combatant target, double actionAttackPotency, double actionCoefficient,
            double accuracyModifier, double evasionModifier, double bypassDefenseFraction,
            Random rng, BattleRequest request, long currentTotalMonths,
            List<String> fatalities, List<String> incapacitated, List<Combatant> allCombatants) {

        // Step 1: Hit Chance
        double seerAccuracyBonus = 0.0;
        TemporaryCombatEffect targetInsight = target.getCombatState().getEffect(TemporaryCombatEffect.EffectType.SEER_INSIGHT);
        if (targetInsight != null && target.getCombatantId().equals(targetInsight.getTargetCombatantId())) {
            seerAccuracyBonus = 0.12;
        }

        double hitChance = Math.max(0.45, Math.min(0.95,
                0.75 + (((attacker.getEffectiveAgility() + attacker.getEffectivePerception())
                        - (target.getEffectiveAgility() + target.getEffectivePerception())) / 300.0)
                        + accuracyModifier + seerAccuracyBonus));

        if (rng.nextDouble() > hitChance) {
            return 0.0; // MISS
        }

        // Step 2: Dodge Chance
        double seerDodgePenalty = (targetInsight != null) ? -0.10 : 0.0;
        double targetBlinkEvasion = 0.0;
        TemporaryCombatEffect targetBlink = target.getCombatState().getEffect(TemporaryCombatEffect.EffectType.RIFTWALKER_BLINK);
        if (targetBlink != null) {
            targetBlinkEvasion = targetBlink.getMagnitude();
        }

        double dodgeChance = Math.max(0.05, Math.min(0.40,
                0.05 + (target.getEffectiveAgility() / 250.0)
                        + evasionModifier + targetBlinkEvasion + seerDodgePenalty + target.getEquipmentEvasionBonus()));

        // Exhaustion penalty to dodge
        if (target.getCombatState().getCurrentStamina() <= 0.0) {
            dodgeChance *= 0.80;
        }

        if (rng.nextDouble() <= dodgeChance) {
            return 0.0; // DODGE
        }

        // Step 3: Raw Damage & Bounded Mitigation
        double damageVariation = 0.85 + (rng.nextDouble() * 0.30); // uniform [0.85, 1.15]
        double rawDamage = actionAttackPotency * actionCoefficient * damageVariation;

        // Defensive Stance
        TemporaryCombatEffect stance = target.getCombatState().getEffect(TemporaryCombatEffect.EffectType.DEFENSIVE_STANCE);
        if (stance != null) {
            rawDamage *= (1.0 - stance.getMagnitude());
        }

        // Vanguard Reinforcement
        TemporaryCombatEffect vanguardReinforce = target.getCombatState().getEffect(TemporaryCombatEffect.EffectType.VANGUARD_REINFORCEMENT);
        if (vanguardReinforce != null) {
            rawDamage *= (1.0 - vanguardReinforce.getMagnitude());
        }

        double effDefRating = target.getPhysicalDefenseRating();
        double defenseMitigation = Math.min(0.75 * rawDamage, effDefRating * 0.65);
        if (bypassDefenseFraction > 0.0) {
            defenseMitigation *= (1.0 - bypassDefenseFraction);
        }

        // Warden Barrier absorption
        double postBarrierDamage = rawDamage;
        TemporaryCombatEffect barrier = target.getCombatState().getEffect(TemporaryCombatEffect.EffectType.WARDEN_BARRIER);
        if (barrier != null && barrier.getMagnitude() > 0.0) {
            double absorbed = Math.min(rawDamage, barrier.getMagnitude());
            barrier.deductMagnitude(absorbed);
            postBarrierDamage = Math.max(0.0, rawDamage - absorbed);
        }

        double finalDamage;
        if (postBarrierDamage <= 0.0) {
            finalDamage = 0.0;
        } else {
            finalDamage = Math.max(1.0, postBarrierDamage - defenseMitigation);
        }

        double prevHealth = target.getCombatState().getCurrentHealth();
        target.getCombatState().takeDamage(finalDamage);

        // Morale drop when falling below 30% HP
        if (prevHealth >= (0.30 * target.getCombatState().getMaxHealth())
                && target.getCombatState().getCurrentHealth() < (0.30 * target.getCombatState().getMaxHealth())) {
            target.getCombatState().modifyMorale(-12.0);
        }

        // Severe/Moderate hit check
        if (finalDamage > (0.25 * target.getCombatState().getMaxHealth())) {
            CombatInjurySeverity sev = (finalDamage > (0.40 * target.getCombatState().getMaxHealth()))
                    ? CombatInjurySeverity.MODERATE_INJURY
                    : CombatInjurySeverity.MINOR_INJURY;
            CombatInjury inj = new CombatInjury(
                    "INJ_HIT_" + request.getBattleId() + "_" + target.getCombatantId(),
                    sev,
                    request.getSimulationYear(),
                    request.getSimulationMonth(),
                    currentTotalMonths + (sev == CombatInjurySeverity.MODERATE_INJURY ? 2 : 1),
                    "Heavy combat impact"
            );
            target.getCombatState().addInjury(inj);
        }

        // Fatal Resolution vs. Incapacitation at 0 HP
        if (target.getCombatState().getCurrentHealth() <= 0.0 && !target.getCombatState().isDead()) {
            resolveZeroHp(attacker, target, prevHealth, finalDamage, attacker.getSideId(), rng, fatalities, incapacitated, allCombatants);
        }

        return finalDamage;
    }

    private static void resolveZeroHp(
            Combatant attacker, Combatant target, double prevHealth, double finalDamage, String attackerSideId,
            Random rng, List<String> fatalities, List<String> incapacitated, List<Combatant> allCombatants) {

        double excessDamage = Math.max(0.0, finalDamage - prevHealth);
        double overkillRatio = excessDamage / target.getCombatState().getMaxHealth();
        double survivalResistance = ((target.getEffectiveVitality() + target.getEffectiveEndurance() + target.getEffectiveWillpower()) / 300.0)
                + ((target.getEffectiveLuck() - 20.0) / 400.0);

        double deathProb = Math.max(0.10, Math.min(0.98,
                0.35 + (1.25 * overkillRatio) - (0.35 * survivalResistance)));

        if (rng.nextDouble() < deathProb) {
            // FATAL OUTCOME
            target.getCombatState().setDead(true);
            if (attacker != null) {
                target.getCombatState().setLastKillerCombatantId(attacker.getCombatantId());
                if (attacker.getPerson() != null) {
                    target.getCombatState().setLastKillerPersonId(String.valueOf(attacker.getPerson().getId()));
                }
                target.getCombatState().setLastKillerSideId(attackerSideId);
            }
            fatalities.add(target.getCombatantId());

            // Morale adjustments
            boolean isCommander = isHighestTierOnSide(allCombatants, target);
            double moraleLoss = isCommander ? -18.0 : -8.0;
            modifySideMorale(allCombatants, target.getSideId(), moraleLoss);
            modifySideMorale(allCombatants, attackerSideId, 6.0);
        } else {
            // NON-FATAL INCAPACITATION
            target.getCombatState().setCurrentHealth(1.0);
            target.getCombatState().setIncapacitated(true);
            incapacitated.add(target.getCombatantId());

            CombatInjury severeInj = new CombatInjury(
                    "INJ_INCAP_" + target.getCombatantId(),
                    CombatInjurySeverity.SEVERE_INJURY,
                    0, 0, 999999L,
                    "Incapacitating combat trauma"
            );
            target.getCombatState().addInjury(severeInj);
        }
    }

    // --- Search & Selection Helpers ---

    private static Combatant selectDeterministicTarget(List<Combatant> enemies, Combatant actor) {
        if (enemies.isEmpty()) return null;

        List<ScoredTarget> scoredList = new ArrayList<>();
        for (Combatant e : enemies) {
            double score = 100.0;

            // Vanguard taunt/aggro
            if (e.isAwakened() && e.getSupernaturalClass() == SupernaturalClass.VANGUARD) {
                score += 50.0;
            }

            // Backline squishy targeting for Riftwalker / Seer
            if (actor.isAwakened() && (actor.getSupernaturalClass() == SupernaturalClass.RIFTWALKER || actor.getSupernaturalClass() == SupernaturalClass.SEER)) {
                if (e.isAwakened() && (e.getSupernaturalClass() == SupernaturalClass.ELEMENTALIST || e.getSupernaturalClass() == SupernaturalClass.VITALIST)) {
                    score += 60.0;
                } else if (e.getCombatState().getCurrentHealth() < (0.50 * e.getCombatState().getMaxHealth())) {
                    score += 40.0;
                }
            }

            // Focus marked target
            TemporaryCombatEffect insight = e.getCombatState().getEffect(TemporaryCombatEffect.EffectType.SEER_INSIGHT);
            if (insight != null && e.getCombatantId().equals(insight.getTargetCombatantId())) {
                score += 20.0;
            }

            // Low health priority
            score += (1.0 - (e.getCombatState().getCurrentHealth() / e.getCombatState().getMaxHealth())) * 30.0;

            scoredList.add(new ScoredTarget(e, score));
        }

        scoredList.sort(Comparator
                .comparingDouble(ScoredTarget::getScore).reversed()
                .thenComparing(st -> st.getCombatant().getCombatantId()));

        return scoredList.get(0).getCombatant();
    }

    private static Combatant findMostInjuredAlly(List<Combatant> all, Combatant actor) {
        Combatant best = null;
        double lowestFraction = 1.0;

        for (Combatant c : all) {
            if (c.getSideId().equals(actor.getSideId()) && !c.getCombatantId().equals(actor.getCombatantId())
                    && c.isAlive() && c.canActInCombat()) {
                double frac = c.getCombatState().getCurrentHealth() / c.getCombatState().getMaxHealth();
                if (frac < 1.0 && frac < lowestFraction) {
                    lowestFraction = frac;
                    best = c;
                }
            }
        }
        return best;
    }

    private static Combatant findBarrierTarget(List<Combatant> all, Combatant actor) {
        // First check frontline Vanguard without barrier
        for (Combatant c : all) {
            if (c.getSideId().equals(actor.getSideId()) && c.isAlive() && c.canActInCombat()) {
                if (c.isAwakened() && c.getSupernaturalClass() == SupernaturalClass.VANGUARD
                        && !c.getCombatState().hasEffect(TemporaryCombatEffect.EffectType.WARDEN_BARRIER)) {
                    return c;
                }
            }
        }
        // Then self if lacking barrier
        if (!actor.getCombatState().hasEffect(TemporaryCombatEffect.EffectType.WARDEN_BARRIER)) {
            return actor;
        }
        // Then any ally lacking barrier
        for (Combatant c : all) {
            if (c.getSideId().equals(actor.getSideId()) && c.isAlive() && c.canActInCombat()) {
                if (!c.getCombatState().hasEffect(TemporaryCombatEffect.EffectType.WARDEN_BARRIER)) {
                    return c;
                }
            }
        }
        return null;
    }

    private static boolean hasAlliedBeast(List<Combatant> all, String sideId) {
        for (Combatant c : all) {
            if (c.getSideId().equals(sideId) && c.getEntityType() == CombatantType.BONDED_BEAST && c.canActInCombat()) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> getSidesWithActiveCombatants(List<Combatant> all) {
        Set<String> sides = new HashSet<>();
        for (Combatant c : all) {
            if (c.canActInCombat()) {
                sides.add(c.getSideId());
            }
        }
        return sides;
    }

    private static List<Combatant> getActiveEnemies(List<Combatant> all, String sideId) {
        List<Combatant> enemies = new ArrayList<>();
        for (Combatant c : all) {
            if (!c.getSideId().equals(sideId) && c.canActInCombat()) {
                enemies.add(c);
            }
        }
        return enemies;
    }

    private static int countActiveOnSide(List<Combatant> all, String sideId) {
        int count = 0;
        for (Combatant c : all) {
            if (c.getSideId().equals(sideId) && c.canActInCombat()) {
                count++;
            }
        }
        return count;
    }

    private static int countOpposingActive(List<Combatant> all, String sideId) {
        int count = 0;
        for (Combatant c : all) {
            if (!c.getSideId().equals(sideId) && c.canActInCombat()) {
                count++;
            }
        }
        return count;
    }

    private static double computeSideAverageMorale(List<Combatant> all, String sideId) {
        double sum = 0.0;
        int count = 0;
        for (Combatant c : all) {
            if (c.getSideId().equals(sideId) && c.canActInCombat()) {
                sum += c.getCombatState().getCurrentMorale();
                count++;
            }
        }
        return (count > 0) ? (sum / count) : 0.0;
    }

    private static double computeSideActivePower(List<Combatant> all, String sideId) {
        double sum = 0.0;
        for (Combatant c : all) {
            if (c.getSideId().equals(sideId) && c.canActInCombat()) {
                if (c.isAwakened() && c.getDerivedPowerProfile() != null) {
                    sum += c.getDerivedPowerProfile().getOverallEffectivePower();
                } else {
                    sum += c.getPhysicalAttackRating();
                }
            }
        }
        return sum;
    }

    private static double computeOpposingActivePower(List<Combatant> all, String sideId) {
        double sum = 0.0;
        for (Combatant c : all) {
            if (!c.getSideId().equals(sideId) && c.canActInCombat()) {
                if (c.isAwakened() && c.getDerivedPowerProfile() != null) {
                    sum += c.getDerivedPowerProfile().getOverallEffectivePower();
                } else {
                    sum += c.getPhysicalAttackRating();
                }
            }
        }
        return sum;
    }

    private static void modifySideMorale(List<Combatant> all, String sideId, double delta) {
        for (Combatant c : all) {
            if (c.getSideId().equals(sideId) && c.getCombatState() != null) {
                c.getCombatState().modifyMorale(delta);
            }
        }
    }

    private static boolean isHighestTierOnSide(List<Combatant> all, Combatant target) {
        int targetTier = target.getPowerTier();
        for (Combatant c : all) {
            if (c.getSideId().equals(target.getSideId()) && !c.getCombatantId().equals(target.getCombatantId())) {
                if (c.getPowerTier() > targetTier) {
                    return false;
                }
            }
        }
        return true;
    }

    // --- Helper Tuples ---

    private static class CombatantInitiative {
        private final Combatant combatant;
        private final double initiative;

        public CombatantInitiative(Combatant combatant, double initiative) {
            this.combatant = combatant;
            this.initiative = initiative;
        }

        public Combatant getCombatant() { return combatant; }
        public double getInitiative() { return initiative; }
    }

    private static class ScoredTarget {
        private final Combatant combatant;
        private final double score;

        public ScoredTarget(Combatant combatant, double score) {
            this.combatant = combatant;
            this.score = score;
        }

        public Combatant getCombatant() { return combatant; }
        public double getScore() { return score; }
    }
}
