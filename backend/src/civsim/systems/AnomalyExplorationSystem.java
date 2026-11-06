package civsim.systems;

import civsim.ProgressionConfig;
import civsim.SimulationConfig;
import civsim.model.*;
import civsim.spatial.Biome;
import civsim.spatial.WorldGeography;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Phase 2B.3 -- Master Anomaly Scheduling, Detection, Exploration, and Expedition Dispatching System.
 * Coordinates deterministic anomaly spawning, two-stage appearance (FORMING -> REVEALED),
 * Seer precognition, supernatural sensing, private discovery records, explicit information sharing,
 * expedition planning with home garrison safeguards, spatial travel, arrival contention checking
 * (handoff to Phase 2B.4), uncontested securing/extraction, return delivery, and 6-month mutation to MONSTER_LAIR_PENDING.
 */
public class AnomalyExplorationSystem {

    public static final int MAX_ACTIVE_ANOMALIES = 5;
    public static final double MIN_DISTANCE_FROM_SETTLEMENT = 8.0;
    public static final double MIN_DISTANCE_BETWEEN_ANOMALIES = 15.0;
    public static final int MAX_COORDINATE_RETRIES = 400;
    public static final double DEFAULT_EXPEDITION_SPEED = 20.0;
    public static final double MONTHLY_FOOD_RATION_PER_PERSON = 1.2;

    // Registries
    private final Map<String, WorldAnomaly> anomalies = new LinkedHashMap<>();
    private final Map<String, Expedition> expeditions = new LinkedHashMap<>();
    private final Map<String, DiscoveryRecord> discoveries = new LinkedHashMap<>();
    private final Map<String, List<DiscoveryRecord>> discoveriesByFaction = new LinkedHashMap<>();
    private final List<InformationShareEvent> shareEvents = new ArrayList<>();
    private final List<PendingMonsterLairRecord> pendingLairs = new ArrayList<>();
    private final List<AnomalyContestRecord> pendingContests = new ArrayList<>();

    // Idempotency guards
    private final Set<String> processedMonthlyTickIds = new HashSet<>();
    private final Set<String> processedSeerCheckIds = new HashSet<>();
    private final Set<String> processedShareEventIds = new HashSet<>();

    // Scheduling State
    private int nextScheduledSpawnMonth = -1;
    private boolean initialSpawnScheduled = false;
    private int nextAnomalyIdCounter = 1;
    private int nextExpeditionIdCounter = 1;
    private int nextDiscoveryIdCounter = 1;
    private int nextContestIdCounter = 1;
    private int nextLairEventIdCounter = 1;
    private int nextShareEventIdCounter = 1;

    // Annual Telemetry Counters
    private int annualSpawns = 0;
    private int annualFailedSpawnAttempts = 0;
    private int annualCapDeferrals = 0;
    private int annualDiscoveries = 0;
    private int annualSeerPrecognitions = 0;
    private int annualExpeditionsDispatched = 0;
    private int annualExpeditionsReturned = 0;
    private int annualSitesSecured = 0;
    private int annualSitesControlled = 0;
    private int annualSitesDepleted = 0;
    private int annualContestsGenerated = 0;
    private int annualExpiredToLair = 0;
    private double annualRationsConsumedFood = 0.0;
    private double annualSupernaturalDeliveredTotal = 0.0;

    public AnomalyExplorationSystem() {}

    public synchronized void resetYearlyCounters() {
        annualSpawns = 0;
        annualFailedSpawnAttempts = 0;
        annualCapDeferrals = 0;
        annualDiscoveries = 0;
        annualSeerPrecognitions = 0;
        annualExpeditionsDispatched = 0;
        annualExpeditionsReturned = 0;
        annualSitesSecured = 0;
        annualSitesControlled = 0;
        annualSitesDepleted = 0;
        annualContestsGenerated = 0;
        annualExpiredToLair = 0;
        annualRationsConsumedFood = 0.0;
        annualSupernaturalDeliveredTotal = 0.0;
    }

    public synchronized void clear() {
        anomalies.clear();
        expeditions.clear();
        discoveries.clear();
        discoveriesByFaction.clear();
        shareEvents.clear();
        pendingLairs.clear();
        pendingContests.clear();
        processedMonthlyTickIds.clear();
        processedSeerCheckIds.clear();
        processedShareEventIds.clear();
        nextScheduledSpawnMonth = -1;
        initialSpawnScheduled = false;
        resetYearlyCounters();
    }

    // =========================================================================
    // PART 1 — WORLD EVENT SCHEDULING, ERA GATING & SPATIAL PLACEMENT
    // =========================================================================

    public synchronized void evaluateMonthlySpawning(
            int currentYear,
            int currentMonth,
            long currentTotalMonths,
            List<Village> villages,
            Random random,
            SimulationConfig config,
            BiConsumer<String, Boolean> eventRecorder) {

        int awakeningStartYear = (config != null) ? config.getAwakeningStartYear() : 50;
        if (currentYear < awakeningStartYear) {
            return; // Mundane Era: Zero supernatural anomalies scheduled or spawned
        }

        long awakeningStartTotalMonths = (long) awakeningStartYear * 12;
        if (currentTotalMonths < awakeningStartTotalMonths) {
            return;
        }

        // Initialize scheduled spawn delay at Year 50
        if (!initialSpawnScheduled) {
            int initialDelay = 6 + random.nextInt(13); // [6, 18] simulated months
            nextScheduledSpawnMonth = (int) awakeningStartTotalMonths + initialDelay;
            initialSpawnScheduled = true;
        }

        // Check if scheduled spawn month has arrived
        if (currentTotalMonths >= nextScheduledSpawnMonth) {
            int activeCount = countActiveAnomalies();
            if (activeCount >= MAX_ACTIVE_ANOMALIES) {
                // Cap deferral rule: defer by 3 months without burning coordinate draws
                nextScheduledSpawnMonth = (int) currentTotalMonths + 3;
                annualCapDeferrals++;
                return;
            }

            // Validated Coordinate Selection
            boolean placed = false;
            double chosenX = 0, chosenY = 0;
            Biome chosenBiome = Biome.RIVER_VALLEY;

            for (int attempt = 0; attempt < MAX_COORDINATE_RETRIES; attempt++) {
                double candX = random.nextDouble() * WorldGeography.WIDTH;
                double candY = random.nextDouble() * WorldGeography.HEIGHT;
                if (!WorldGeography.isAnomalyLocation(candX, candY)) continue;

                // 1. Distance from all living settlements >= 8.0
                boolean okSettlement = true;
                if (villages != null) {
                    for (Village v : villages) {
                        if (v.getLivingPopulationCount() > 0 && Math.hypot(v.getX() - candX, v.getY() - candY) < MIN_DISTANCE_FROM_SETTLEMENT) {
                            okSettlement = false;
                            break;
                        }
                    }
                }
                if (!okSettlement) continue;

                // 2. Distance from all currently active anomalies >= 15.0
                boolean okAnomalies = true;
                for (WorldAnomaly a : anomalies.values()) {
                    if (a.getState().isActive() && Math.hypot(a.getPosX() - candX, a.getPosY() - candY) < MIN_DISTANCE_BETWEEN_ANOMALIES) {
                        okAnomalies = false;
                        break;
                    }
                }
                if (!okAnomalies) continue;

                placed = true;
                chosenX = candX;
                chosenY = candY;
                chosenBiome = Biome.determineBiomeAt(chosenX, chosenY);
                break;
            }

            if (placed) {
                SupernaturalResourceType resType = SupernaturalResourceType.sampleByWeight(random);
                int baseQty = resType.sampleBaseQuantity(random);
                ResourceGrade grade = ResourceGrade.sampleGrade(random);
                int initQty = grade.calculateInitializedQuantity(baseQty);

                String anomId = "ANOM_" + (nextAnomalyIdCounter++);
                String name = generateAnomalyName(resType, grade, chosenBiome);

                WorldAnomaly anomaly = new WorldAnomaly(
                        anomId,
                        name,
                        chosenX,
                        chosenY,
                        chosenBiome,
                        resType,
                        grade,
                        initQty,
                        (int) currentTotalMonths
                );
                anomalies.put(anomId, anomaly);

                // Schedule subsequent spawn interval in [12, 36] simulated months
                int nextInterval = 12 + random.nextInt(25);
                nextScheduledSpawnMonth = (int) currentTotalMonths + nextInterval;
                annualSpawns++;

                if (eventRecorder != null) {
                    eventRecorder.accept(String.format("Year %d (Month %d): A hidden supernatural anomaly (%s, %s) began FORMING at (%.1f, %.1f)!",
                            currentYear, currentMonth, resType.getDisplayName(), grade.getDisplayName(), chosenX, chosenY), false);
                }
            } else {
                annualFailedSpawnAttempts++;
                // Retry in 3 months
                nextScheduledSpawnMonth = (int) currentTotalMonths + 3;
            }
        }
    }

    public synchronized int countActiveAnomalies() {
        int count = 0;
        for (WorldAnomaly a : anomalies.values()) {
            if (a.getState().isActive()) {
                count++;
            }
        }
        return count;
    }

    private String generateAnomalyName(SupernaturalResourceType type, ResourceGrade grade, Biome biome) {
        String prefix = grade == ResourceGrade.MYTHIC ? "Primordial " :
                (grade == ResourceGrade.LEGENDARY ? "Celestial " :
                        (grade == ResourceGrade.RARE ? "Radiant " : ""));
        switch (type) {
            case RAW_AETHER_CRYSTAL:
                return prefix + "Aether Crystal Deposit";
            case ANOMALOUS_ORE:
                return prefix + "Anomalous Mineral Vein";
            case CONDENSED_AETHER_ESSENCE:
                return prefix + "Aether Spring";
            default:
                return "Supernatural Anomaly";
        }
    }

    // =========================================================================
    // PART 3 — TWO-STAGE APPEARANCE, DETECTION & SEER PRECOGNITION
    // =========================================================================

    public synchronized void processMonthlyAppearanceAndDetection(
            int currentYear,
            int currentMonth,
            long currentTotalMonths,
            List<Person> allPeople,
            List<Village> villages,
            OrganizationSystem orgSys,
            ProgressionSystem progSys,
            ProgressionConfig progConfig,
            Random random,
            BiConsumer<String, Boolean> eventRecorder) {

        for (WorldAnomaly anomaly : anomalies.values()) {
            // Check terminal states
            if (anomaly.getState().isTerminal()) {
                continue;
            }

            // 1. Stage Transition: FORMING -> REVEALED
            if (currentTotalMonths >= anomaly.getPublicAppearanceMonth() && anomaly.getState() == AnomalyState.FORMING) {
                anomaly.transitionToRevealed();
                if (eventRecorder != null) {
                    eventRecorder.accept(String.format("Year %d (Month %d): Anomaly %s (%s, %s) has manifested publicly at (%.1f, %.1f)!",
                            currentYear, currentMonth, anomaly.getName(),
                            anomaly.getSupernaturalResourceType().getDisplayName(),
                            anomaly.getResourceGrade().getDisplayName(),
                            anomaly.getPosX(), anomaly.getPosY()), false);
                }
            }

            // 2. Seer Precognition during FORMING window
            if (anomaly.getState() == AnomalyState.FORMING &&
                    (anomaly.getPublicAppearanceMonth() - currentTotalMonths) <= 2 &&
                    (anomaly.getPublicAppearanceMonth() - currentTotalMonths) >= 0) {

                evaluateSeerPrecognition(anomaly, (int) currentTotalMonths, allPeople, orgSys, progSys, progConfig, random, eventRecorder);
            }

            // 3. Supernatural Sensing & Direct Scouting (When REVEALED / UNCLAIMED / DISCOVERED)
            if (anomaly.isPubliclyObservable()) {
                evaluatePublicSensingAndScouting(anomaly, (int) currentTotalMonths, allPeople, villages, orgSys, random, eventRecorder);
            }

            // 4. Six-Month Unclaimed Mutation Check
            if (anomaly.hasMutationDeadlineExpired(currentTotalMonths)) {
                triggerUnclaimedMutation(anomaly, (int) currentTotalMonths, eventRecorder);
            }
        }
    }

    private void evaluateSeerPrecognition(
            WorldAnomaly anomaly,
            int currentTotalMonths,
            List<Person> allPeople,
            OrganizationSystem orgSys,
            ProgressionSystem progSys,
            ProgressionConfig progConfig,
            Random random,
            BiConsumer<String, Boolean> eventRecorder) {

        if (allPeople == null) return;

        for (Person person : allPeople) {
            if (!person.isAlive()) continue;
            AetherProfile ap = person.getAetherProfile();
            if (ap == null || !ap.isAwakened() || ap.getSupernaturalClass() != SupernaturalClass.SEER) {
                continue;
            }

            String seerCheckKey = "SEER_CHK:" + anomaly.getAnomalyId() + ":" + person.getId();
            if (!processedSeerCheckIds.add(seerCheckKey)) {
                continue; // roll precognition at most once per seer per anomaly
            }

            double pX = (person.getVillage() != null) ? person.getVillage().getX() : 50.0;
            double pY = (person.getVillage() != null) ? person.getVillage().getY() : 50.0;
            double dist = Math.hypot(pX - anomaly.getPosX(), pY - anomaly.getPosY());
            int tier = ap.getPowerTier();
            double effPerception = ap.getEffectivePerception();
            double seerRange = anomaly.getBaseDetectionRadius() + (10.0 * tier) + (0.3 * effPerception);

            if (dist <= seerRange) {
                double mastery = (ap.getProgressionRecord() != null) ? ap.getProgressionRecord().getMastery() : 10.0;
                double proximityBonus = (seerRange > 0.0) ? Math.max(0.0, (1.0 - (dist / seerRange)) * 0.30) : 0.0;
                double checkProb = 0.50 + (0.06 * tier) + (0.005 * effPerception) + (0.002 * mastery) + proximityBonus;
                if (random.nextDouble() < Math.min(0.95, checkProb)) {
                    // Success! Calculate bounded accuracy error
                    double maxErr = Math.max(0.0, 15.0 - (0.10 * mastery) - (0.08 * effPerception));
                    double errX = (random.nextDouble() * 2 - 1) * maxErr;
                    double errY = (random.nextDouble() * 2 - 1) * maxErr;
                    double reportedX = anomaly.getPosX() + errX;
                    double reportedY = anomaly.getPosY() + errY;

                    double confidence = Math.max(0.35, Math.min(0.95, 0.40 + (0.08 * tier) + (mastery * 0.004)));
                    String orgId = determinePrimaryFactionId(person, orgSys);

                    String discId = "DISC_SEER:" + anomaly.getAnomalyId() + ":" + person.getId();
                    DiscoveryRecord disc = new DiscoveryRecord(
                            discId,
                            anomaly.getAnomalyId(),
                            String.valueOf(person.getId()),
                            orgId,
                            currentTotalMonths,
                            DetectionMethod.SEER_PRECOGNITION,
                            confidence,
                            anomaly.getResourceGrade(),
                            false,
                            reportedX,
                            reportedY,
                            false
                    );

                    registerDiscovery(disc, anomaly);
                    annualSeerPrecognitions++;

                    // Submit Stage 2A.4 Progression Event for Seer
                    if (progSys != null && progConfig != null) {
                        ProgressionEvent seerEvent = new ProgressionEvent(
                                "SEER_PRECOG:Y" + (currentTotalMonths / 12) + ":M" + (currentTotalMonths % 12) + ":P" + person.getId(),
                                person.getId(),
                                ProgressionEventType.OCCUPATIONAL_ABILITY_USE,
                                currentTotalMonths / 12,
                                (currentTotalMonths % 12) + 1,
                                1.30
                        );
                        progSys.processEvent(person, seerEvent, progConfig);
                    }

                    if (eventRecorder != null) {
                        eventRecorder.accept(String.format("Year %d (Month %d): Seer %s foresaw a forming anomaly %s with %.0f%% confidence!",
                                currentTotalMonths / 12, currentTotalMonths % 12, person.getFullName(),
                                anomaly.getName(), confidence * 100.0), false);
                    }
                }
            }
        }
    }

    private void evaluatePublicSensingAndScouting(
            WorldAnomaly anomaly,
            int currentTotalMonths,
            List<Person> allPeople,
            List<Village> villages,
            OrganizationSystem orgSys,
            Random random,
            BiConsumer<String, Boolean> eventRecorder) {

        // 1. Direct Scouting from nearby settlements (dist <= 18.0)
        if (villages != null) {
            for (Village v : villages) {
                if (v.getLivingPopulationCount() <= 0) continue;
                String vId = "VILLAGE_" + v.getId();
                if (anomaly.isKnownByFaction(vId)) continue;

                double dist = Math.hypot(v.getX() - anomaly.getPosX(), v.getY() - anomaly.getPosY());
                if (dist <= 18.0) {
                    String discId = "DISC_SCOUT:" + anomaly.getAnomalyId() + ":" + vId;
                    DiscoveryRecord disc = new DiscoveryRecord(
                            discId,
                            anomaly.getAnomalyId(),
                            null,
                            vId,
                            currentTotalMonths,
                            DetectionMethod.DIRECT_SCOUTING,
                            1.00,
                            anomaly.getResourceGrade(),
                            true,
                            anomaly.getPosX(),
                            anomaly.getPosY(),
                            false
                    );
                    registerDiscovery(disc, anomaly);
                    if (eventRecorder != null) {
                        eventRecorder.accept(String.format("Year %d (Month %d): Scouts from %s sighted %s at (%.1f, %.1f)!",
                                currentTotalMonths / 12, currentTotalMonths % 12, v.getName(), anomaly.getName(), anomaly.getPosX(), anomaly.getPosY()), false);
                    }
                }
            }
        }

        // 2. Supernatural Sensing by awakened individuals within detection radius
        if (allPeople != null) {
            for (Person p : allPeople) {
                if (!p.isAlive()) continue;
                AetherProfile ap = p.getAetherProfile();
                if (ap == null || !ap.isAwakened()) continue;

                String orgId = determinePrimaryFactionId(p, orgSys);
                if (anomaly.isKnownByFaction(orgId) || anomaly.isKnownByPerson(String.valueOf(p.getId()))) {
                    continue;
                }

                double pX = (p.getVillage() != null) ? p.getVillage().getX() : 50.0;
                double pY = (p.getVillage() != null) ? p.getVillage().getY() : 50.0;
                double dist = Math.hypot(pX - anomaly.getPosX(), pY - anomaly.getPosY());
                if (dist <= anomaly.getBaseDetectionRadius()) {
                    double roll = random.nextDouble();
                    int tier = ap.getPowerTier();
                    double senseProb = 0.25 + (0.06 * tier) + (0.003 * ap.getEffectivePerception());
                    if (roll < senseProb) {
                        String discId = "DISC_SENSE:" + anomaly.getAnomalyId() + ":" + p.getId();
                        DiscoveryRecord disc = new DiscoveryRecord(
                                discId,
                                anomaly.getAnomalyId(),
                                String.valueOf(p.getId()),
                                orgId,
                                currentTotalMonths,
                                DetectionMethod.SUPERNATURAL_SENSING,
                                0.85,
                                anomaly.getResourceGrade(),
                                false,
                                anomaly.getPosX(),
                                anomaly.getPosY(),
                                false
                        );
                        registerDiscovery(disc, anomaly);
                    }
                }
            }
        }
    }

    public synchronized void registerDiscovery(DiscoveryRecord record, WorldAnomaly anomaly) {
        if (record == null) return;
        discoveries.put(record.getDiscoveryId(), record);
        if (anomaly != null) {
            anomaly.recordDiscovery(record);
        }
        if (record.getOwningOrganizationId() != null) {
            discoveriesByFaction.computeIfAbsent(record.getOwningOrganizationId(), k -> new ArrayList<>()).add(record);
        }
        annualDiscoveries++;
    }

    private String determinePrimaryFactionId(Person person, OrganizationSystem orgSys) {
        if (person == null) return "UNAFFILIATED";
        if (orgSys != null) {
            for (Organization org : orgSys.getActiveOrganizations()) {
                MembershipRecord rec = org.getMembershipRecord(String.valueOf(person.getId()));
                if (rec != null && rec.getStatus() == MembershipStatus.ACTIVE) {
                    return org.getOrganizationId();
                }
            }
        }
        if (person.getVillage() != null) {
            return "VILLAGE_" + person.getVillage().getId();
        }
        return "PERSON_" + person.getId();
    }

    // =========================================================================
    // PART 4 — INFORMATION SHARING
    // =========================================================================

    public synchronized boolean shareDiscovery(
            InformationShareEvent event,
            OrganizationSystem orgSys,
            BiConsumer<String, Boolean> eventRecorder) {

        if (event == null) return false;
        if (!processedShareEventIds.add(event.getEventId())) {
            return false; // idempotent
        }

        DiscoveryRecord source = discoveries.get(event.getDiscoveryId());
        if (source == null) return false;

        // Factions must be allied or have policy permission if both are formal organizations
        if (orgSys != null && orgSys.getOrganization(event.getSharingPartyId()) != null &&
                orgSys.getOrganization(event.getRecipientPartyId()) != null &&
                !orgSys.areAllied(event.getSharingPartyId(), event.getRecipientPartyId())) {
            return false;
        }

        source.addSharedRecipient(event.getRecipientPartyId());
        shareEvents.add(event);

        // Create recipient discovery copy
        String recDiscId = "DISC_SHARED:" + event.getAnomalyId() + ":" + event.getRecipientPartyId();
        DiscoveryRecord recipientRecord = new DiscoveryRecord(
                recDiscId,
                event.getAnomalyId(),
                null,
                event.getRecipientPartyId(),
                event.getShareTotalMonths(),
                DetectionMethod.SHARED_INTELLIGENCE,
                event.getInformationConfidence(),
                source.getEstimatedGrade(),
                source.isExactQuantityKnown(),
                source.getReportedX(),
                source.getReportedY(),
                false
        );

        WorldAnomaly anomaly = anomalies.get(event.getAnomalyId());
        registerDiscovery(recipientRecord, anomaly);

        if (eventRecorder != null) {
            eventRecorder.accept(String.format("Year %d (Month %d): %s shared intelligence on anomaly %s with %s.",
                    event.getShareTotalMonths() / 12, event.getShareTotalMonths() % 12,
                    event.getSharingPartyId(), event.getAnomalyId(), event.getRecipientPartyId()), false);
        }
        return true;
    }

    // =========================================================================
    // PART 5 — EXPEDITION PLANNING, GARRISON SAFEGUARDS & DISPATCHING
    // =========================================================================

    public synchronized void evaluateAndDispatchExpeditions(
            int currentYear,
            int currentMonth,
            long currentTotalMonths,
            List<Person> allPeople,
            List<Village> villages,
            OrganizationSystem orgSys,
            BiConsumer<String, Boolean> eventRecorder) {

        if (villages == null || allPeople == null) return;

        Map<String, Person> personMap = new HashMap<>();
        for (Person p : allPeople) {
            personMap.put(String.valueOf(p.getId()), p);
        }

        // Iterate over factions that hold discoveries
        for (Map.Entry<String, List<DiscoveryRecord>> entry : discoveriesByFaction.entrySet()) {
            String factionId = entry.getKey();
            List<DiscoveryRecord> knownList = entry.getValue();

            for (DiscoveryRecord discovery : knownList) {
                WorldAnomaly anomaly = anomalies.get(discovery.getAnomalyId());
                if (anomaly == null || anomaly.getState().isTerminal()) {
                    continue;
                }

                // Do not dispatch if faction already has an active expedition for this anomaly
                boolean hasActiveExp = false;
                for (Expedition e : expeditions.values()) {
                    if (e.getSponsorId().equals(factionId) &&
                            e.getTargetAnomalyId().equals(anomaly.getAnomalyId()) &&
                            e.getStatus().isActive()) {
                        hasActiveExp = true;
                        break;
                    }
                }
                if (hasActiveExp) continue;

                // Find origin settlement
                Village originVillage = findOriginVillage(factionId, villages, orgSys, personMap);
                if (originVillage == null || originVillage.getLivingPopulationCount() < 2) {
                    continue;
                }

                // Check Garrison Safeguard:
                // Must retain at least max(2, 35% of local soldiers)
                int totalSoldiers = 0;
                int totalAwakened = 0;
                List<Person> eligibleCandidates = new ArrayList<>();

                for (Person p : originVillage.getResidents()) {
                    if (!p.isAlive()) continue;
                    if (p.getOccupation() == Occupation.SOLDIER) totalSoldiers++;
                    AetherProfile ap = p.getAetherProfile();
                    if (ap != null && ap.isAwakened()) totalAwakened++;

                    // Candidate eligibility: age >= 18 mortal / 16 awakened, healthy, not on another expedition
                    int minAge = (ap != null && ap.isAwakened()) ? 16 : 18;
                    if (p.getAge() >= minAge && !isPersonOnActiveExpedition(String.valueOf(p.getId()))) {
                        eligibleCandidates.add(p);
                    }
                }

                int requiredGarrisonSoldiers = Math.max(2, (int) Math.floor(totalSoldiers * 0.35));
                int availableSoldiers = Math.max(0, totalSoldiers - requiredGarrisonSoldiers);
                int requiredAwakenedGarrison = totalAwakened >= 2 ? 1 : 0;
                int availableAwakened = Math.max(0, totalAwakened - requiredAwakenedGarrison);

                if (availableSoldiers < 1 && availableAwakened < 1) {
                    continue; // deferred: insufficient garrison
                }

                if (eligibleCandidates.size() < 2) {
                    continue; // deferred: insufficient personnel
                }

                // Distance & Travel Months
                double dist = Math.hypot(originVillage.getX() - anomaly.getPosX(), originVillage.getY() - anomaly.getPosY());
                int oneWayTravelMonths = Math.max(1, (int) Math.ceil(dist / DEFAULT_EXPEDITION_SPEED));

                // Rations needed = 2 * oneWayTravelMonths * count * 1.2
                int maxParty = Math.min(3, availableSoldiers + availableAwakened);
                int partySize = Math.max(1, Math.min(maxParty, eligibleCandidates.size()));
                double requiredFood = 2.0 * oneWayTravelMonths * partySize * MONTHLY_FOOD_RATION_PER_PERSON;

                if (originVillage.getStockpile().getFoodStockpile() < requiredFood + 50.0) {
                    continue; // deferred: insufficient supplies
                }

                // Centralized Desirability Score
                double estValue = discovery.getEstimatedGrade().getValueMultiplier() * 40.0;
                double distancePenalty = dist * 0.40;
                double supplyCostPenalty = requiredFood * 0.50;
                double score = estValue - distancePenalty - supplyCostPenalty;

                if (score > 0) {
                    // Assemble roster
                    List<String> roster = new ArrayList<>();
                    for (int i = 0; i < partySize; i++) {
                        roster.add(String.valueOf(eligibleCandidates.get(i).getId()));
                    }

                    // Deduct supplies atomically from origin settlement
                    originVillage.getStockpile().deductFood(requiredFood);

                    String expId = "EXP_" + (nextExpeditionIdCounter++);
                    Expedition expedition = new Expedition(
                            expId,
                            factionId,
                            factionId.startsWith("VILLAGE_") ? "SETTLEMENT" : "ORGANIZATION",
                            anomaly.getAnomalyId(),
                            originVillage.getX(),
                            originVillage.getY(),
                            ClaimObjective.EXCLUSIVE_CLAIM,
                            roster,
                            partySize * 50.0,
                            requiredFood,
                            currentYear,
                            currentMonth
                    );

                    expedition.setTargetPosition(anomaly.getPosX(), anomaly.getPosY());
                    expedition.setOneWayTravelMonths(oneWayTravelMonths);
                    expedition.setArrivalTotalMonth(currentTotalMonths + oneWayTravelMonths);

                    expeditions.put(expId, expedition);
                    anomaly.addActiveExpedition(expId);
                    annualExpeditionsDispatched++;
                    annualRationsConsumedFood += requiredFood;

                    if (eventRecorder != null) {
                        eventRecorder.accept(String.format("Year %d (Month %d): %s dispatched Expedition %s (size %d) to %s (ETA %d months).",
                                currentYear, currentMonth, factionId, expId, partySize, anomaly.getName(), oneWayTravelMonths), false);
                    }
                }
            }
        }
    }

    private Village findOriginVillage(
            String factionId,
            List<Village> villages,
            OrganizationSystem orgSys,
            Map<String, Person> personMap) {

        if (factionId.startsWith("VILLAGE_")) {
            try {
                int vId = Integer.parseInt(factionId.substring("VILLAGE_".length()));
                for (Village v : villages) {
                    if (v.getId() == vId) return v;
                }
            } catch (Exception ignored) {}
        } else if (orgSys != null) {
            Organization org = orgSys.getOrganization(factionId);
            if (org != null && org.getCurrentLeaderPersonId() != null) {
                Person leader = personMap.get(org.getCurrentLeaderPersonId());
                if (leader != null && leader.getVillage() != null) {
                    return leader.getVillage();
                }
            }
        }
        return (villages != null && !villages.isEmpty()) ? villages.get(0) : null;
    }

    public synchronized boolean isPersonOnActiveExpedition(String personId) {
        if (personId == null) return false;
        for (Expedition e : expeditions.values()) {
            if (e.getStatus().isActive() && e.getRosterPersonIds().contains(personId)) {
                return true;
            }
        }
        return false;
    }

    // =========================================================================
    // PART 6 & 7 — PHYSICAL TRAVEL, ARRIVAL, CONTENTION, SECURING & EXTRACTION
    // =========================================================================

    public synchronized void processMonthlyTravelAndContention(
            int currentYear,
            int currentMonth,
            long currentTotalMonths,
            OrganizationSystem orgSys,
            List<Village> villages,
            BiConsumer<String, Boolean> eventRecorder) {

        // 1. Process movement and arrivals
        for (Expedition expedition : expeditions.values()) {
            if (!expedition.getStatus().isActive()) continue;

            WorldAnomaly anomaly = anomalies.get(expedition.getTargetAnomalyId());
            if (anomaly == null) continue;

            // Travel to Anomaly
            if (expedition.getStatus() == ExpeditionStatus.EN_ROUTE_TO_ANOMALY) {
                if (currentTotalMonths >= expedition.getArrivalTotalMonth()) {
                    // Check if anomaly has manifested
                    if (currentTotalMonths < anomaly.getPublicAppearanceMonth()) {
                        expedition.setStatus(ExpeditionStatus.WAITING_FOR_MANIFESTATION);
                    } else {
                        expedition.setStatus(ExpeditionStatus.PRESENT_AT_ANOMALY);
                        expedition.setPosition(anomaly.getPosX(), anomaly.getPosY());
                    }
                }
            } else if (expedition.getStatus() == ExpeditionStatus.WAITING_FOR_MANIFESTATION) {
                if (currentTotalMonths >= anomaly.getPublicAppearanceMonth()) {
                    expedition.setStatus(ExpeditionStatus.PRESENT_AT_ANOMALY);
                    expedition.setPosition(anomaly.getPosX(), anomaly.getPosY());
                }
            }

            // Return Journey to Home
            if (expedition.getStatus() == ExpeditionStatus.RETURNING) {
                if (currentTotalMonths >= expedition.getReturnArrivalMonth()) {
                    // Delivered!
                    expedition.setStatus(ExpeditionStatus.RETURNED);
                    double carried = expedition.getCarriedResources();
                    if (carried > 0.0) {
                        deliverResourcesToSponsor(expedition, anomaly, carried, villages, orgSys);
                        anomaly.addDeliveredTotal(carried);
                        expedition.setCarriedResources(0.0);
                        annualSupernaturalDeliveredTotal += carried;
                    }
                    expedition.setStatus(ExpeditionStatus.DISBANDED);
                    anomaly.removeActiveExpedition(expedition.getExpeditionId());
                    annualExpeditionsReturned++;

                    if (eventRecorder != null) {
                        eventRecorder.accept(String.format("Year %d (Month %d): Expedition %s returned home and delivered %.1f %s!",
                                currentYear, currentMonth, expedition.getExpeditionId(), carried,
                                anomaly.getSupernaturalResourceType().getDisplayName()), false);
                    }
                }
            }
        }

        // 2. Evaluate Contention vs Uncontested Securing for each anomaly
        for (WorldAnomaly anomaly : anomalies.values()) {
            if (anomaly.getState().isTerminal() || anomaly.getState() == AnomalyState.FORMING) {
                continue;
            }

            // Gather all expeditions currently present at the site
            List<Expedition> present = new ArrayList<>();
            for (Expedition e : expeditions.values()) {
                if (e.getTargetAnomalyId().equals(anomaly.getAnomalyId()) && e.getStatus().isAtSite()) {
                    present.add(e);
                }
            }

            if (present.isEmpty()) {
                continue;
            }

            // Group into coalition sides
            Map<String, List<Expedition>> coalitions = resolveCoalitions(present, orgSys);

            if (coalitions.size() > 1) {
                // MULTI-FACTION RIVALRY -> Immediately CONTESTED!
                if (anomaly.getState() != AnomalyState.CONTESTED) {
                    anomaly.transitionToContested("CONT_" + (nextContestIdCounter++));
                    AnomalyContestRecord contest = new AnomalyContestRecord(
                            anomaly.getCurrentContestId(),
                            anomaly.getAnomalyId(),
                            anomaly.getEncounterSequence(),
                            ContestState.PENDING_RESOLUTION,
                            currentYear,
                            currentMonth,
                            currentTotalMonths,
                            new ArrayList<>(coalitions.keySet())
                    );
                    pendingContests.add(contest);
                    annualContestsGenerated++;

                    if (eventRecorder != null) {
                        eventRecorder.accept(String.format("Year %d (Month %d): Contested claim erupted at %s between %d rival coalitions! (Queued for 2B.4)",
                                currentYear, currentMonth, anomaly.getName(), coalitions.size()), false);
                    }
                }
            } else if (coalitions.size() == 1) {
                // Exactly ONE coalition present
                String winningSide = coalitions.keySet().iterator().next();

                if (anomaly.getState() == AnomalyState.REVEALED ||
                        anomaly.getState() == AnomalyState.DISCOVERED ||
                        anomaly.getState() == AnomalyState.EXPEDITION_ACTIVE ||
                        anomaly.getState() == AnomalyState.UNCLAIMED) {

                    anomaly.transitionToSecuring(winningSide, 1, (int) currentTotalMonths);
                    for (Expedition e : present) {
                        e.setStatus(ExpeditionStatus.SECURING_SITE);
                    }
                    annualSitesSecured++;

                } else if (anomaly.getState() == AnomalyState.SECURING) {
                    // Securing duration completed without challenge
                    anomaly.transitionToControlled((int) currentTotalMonths);
                    for (Expedition e : present) {
                        e.setStatus(ExpeditionStatus.EXTRACTING);
                    }
                    annualSitesControlled++;

                    if (eventRecorder != null) {
                        eventRecorder.accept(String.format("Year %d (Month %d): %s has established full CONTROL over %s!",
                                currentYear, currentMonth, winningSide, anomaly.getName()), false);
                    }
                }

                // Controlled Resource Extraction
                if (anomaly.getState() == AnomalyState.CONTROLLED) {
                    for (Expedition exp : present) {
                        if (exp.getStatus() == ExpeditionStatus.EXTRACTING || exp.getStatus() == ExpeditionStatus.PRESENT_AT_ANOMALY) {
                            double availableCap = exp.getMaxCarryCapacity() - exp.getCarriedResources();
                            if (availableCap > 0.0 && anomaly.getRemainingQuantity() > 0.0) {
                                double extractRequest = Math.min(20.0, availableCap);
                                double extracted = anomaly.extractResources(extractRequest);
                                exp.addCarriedResources(extracted);
                            }

                            // If expedition is full or deposit is depleted, begin return journey
                            if (exp.getCarriedResources() >= exp.getMaxCarryCapacity() || anomaly.getRemainingQuantity() <= 0.0) {
                                exp.startReturnJourney(currentTotalMonths);
                            }
                        }
                    }

                    if (anomaly.getRemainingQuantity() <= 0.0 && anomaly.getState() == AnomalyState.DEPLETED) {
                        annualSitesDepleted++;
                        if (eventRecorder != null) {
                            eventRecorder.accept(String.format("Year %d (Month %d): Anomaly %s has been completely DEPLETED!",
                                    currentYear, currentMonth, anomaly.getName()), false);
                        }
                    }
                }
            }
        }
    }

    private Map<String, List<Expedition>> resolveCoalitions(List<Expedition> present, OrganizationSystem orgSys) {
        Map<String, List<Expedition>> coalitions = new LinkedHashMap<>();
        for (Expedition exp : present) {
            String sponsor = exp.getSponsorId();
            String matchedCoalition = null;

            for (String existingCoalition : coalitions.keySet()) {
                if (existingCoalition.equals(sponsor)) {
                    matchedCoalition = existingCoalition;
                    break;
                }
                if (orgSys != null && orgSys.areAllied(sponsor, existingCoalition)) {
                    matchedCoalition = existingCoalition;
                    break;
                }
            }

            if (matchedCoalition != null) {
                coalitions.get(matchedCoalition).add(exp);
            } else {
                List<Expedition> list = new ArrayList<>();
                list.add(exp);
                coalitions.put(sponsor, list);
            }
        }
        return coalitions;
    }

    private void deliverResourcesToSponsor(
            Expedition exp,
            WorldAnomaly anomaly,
            double carried,
            List<Village> villages,
            OrganizationSystem orgSys) {

        String sponsorId = exp.getSponsorId();
        SupernaturalResourceType rType = anomaly.getSupernaturalResourceType();

        if (sponsorId.startsWith("VILLAGE_") && villages != null) {
            try {
                int vId = Integer.parseInt(sponsorId.substring("VILLAGE_".length()));
                for (Village v : villages) {
                    if (v.getId() == vId) {
                        v.getSupernaturalInventory().addResource(rType, carried);
                        return;
                    }
                }
            } catch (Exception ignored) {}
        } else if (orgSys != null) {
            Organization org = orgSys.getOrganization(sponsorId);
            if (org != null) {
                org.getSupernaturalInventory().addResource(rType, carried);
                return;
            }
        }

        // Fallback: deliver to first village
        if (villages != null && !villages.isEmpty()) {
            villages.get(0).getSupernaturalInventory().addResource(rType, carried);
        }
    }

    // =========================================================================
    // PART 8 — SIX-MONTH UNCLAIMED MUTATION (MONSTER_LAIR_PENDING)
    // =========================================================================

    private void triggerUnclaimedMutation(WorldAnomaly anomaly, int currentTotalMonths, BiConsumer<String, Boolean> eventRecorder) {
        if (anomaly.getState() == AnomalyState.CONTROLLED ||
                anomaly.getState() == AnomalyState.DEPLETED ||
                anomaly.getState() == AnomalyState.MONSTER_LAIR_PENDING) {
            return;
        }

        boolean wasContested = (anomaly.getState() == AnomalyState.CONTESTED);
        double rem = anomaly.getRemainingQuantity();
        anomaly.transitionToMonsterLair();

        String lairEvtId = "LAIR_EVT_" + (nextLairEventIdCounter++);
        PendingMonsterLairRecord record = new PendingMonsterLairRecord(
                lairEvtId,
                anomaly.getAnomalyId(),
                anomaly.getPosX(),
                anomaly.getPosY(),
                anomaly.getSupernaturalResourceType(),
                anomaly.getResourceGrade(),
                rem,
                wasContested,
                currentTotalMonths,
                "AETHERIC_CORRUPTION_" + anomaly.getResourceGrade().name()
        );
        pendingLairs.add(record);
        annualExpiredToLair++;

        if (eventRecorder != null) {
            eventRecorder.accept(String.format("Year %d (Month %d): Unclaimed anomaly %s reached 6-month mutation deadline and mutated into MONSTER_LAIR_PENDING!",
                    currentTotalMonths / 12, currentTotalMonths % 12, anomaly.getName()), false);
        }
    }

    // =========================================================================
    // CONSERVATION LAW VALIDATION
    // =========================================================================

    public synchronized ResourceConservationSnapshot validateConservation(String anomalyId) {
        WorldAnomaly a = anomalies.get(anomalyId);
        if (a == null) return null;

        double remainingInDeposit = a.getRemainingQuantity();
        double carried = 0.0;
        for (Expedition e : expeditions.values()) {
            if (e.getTargetAnomalyId().equals(anomalyId)) {
                carried += e.getCarriedResources();
            }
        }
        double delivered = a.getDeliveredTotal();
        double mutatedOrAbandoned = a.getMutatedOrAbandonedTotal();
        double totalTracked = remainingInDeposit + carried + delivered + mutatedOrAbandoned;
        double discrepancy = Math.abs(totalTracked - a.getInitialQuantity());

        return new ResourceConservationSnapshot(
                anomalyId,
                a.getInitialQuantity(),
                remainingInDeposit,
                carried,
                delivered,
                mutatedOrAbandoned
        );
    }

    public synchronized boolean validateAllConservation(double tolerance) {
        for (WorldAnomaly a : anomalies.values()) {
            ResourceConservationSnapshot snap = validateConservation(a.getAnomalyId());
            if (snap != null && !snap.isConserved(tolerance)) {
                return false;
            }
        }
        return true;
    }

    // =========================================================================
    // REGISTRY QUERIES & GETTERS
    // =========================================================================

    public synchronized void registerAnomaly(WorldAnomaly anomaly) {
        if (anomaly != null) {
            anomalies.put(anomaly.getAnomalyId(), anomaly);
        }
    }

    public synchronized void registerExpedition(Expedition expedition) {
        if (expedition != null) {
            expeditions.put(expedition.getExpeditionId(), expedition);
        }
    }

    public synchronized void registerContest(AnomalyContestRecord contest) {
        if (contest != null) {
            pendingContests.add(contest);
        }
    }

    public synchronized WorldAnomaly getAnomaly(String id) { return anomalies.get(id); }
    public synchronized List<WorldAnomaly> getAllAnomalies() { return Collections.unmodifiableList(new ArrayList<>(anomalies.values())); }
    public synchronized void addAnomaly(WorldAnomaly anomaly) {
        if (anomaly != null && anomaly.getAnomalyId() != null) {
            anomalies.put(anomaly.getAnomalyId(), anomaly);
        }
    }
    public synchronized Expedition getExpedition(String id) { return expeditions.get(id); }
    public synchronized List<Expedition> getAllExpeditions() { return Collections.unmodifiableList(new ArrayList<>(expeditions.values())); }
    public synchronized List<Expedition> getActiveExpeditions() {
        List<Expedition> active = new ArrayList<>();
        for (Expedition e : expeditions.values()) {
            if (e.getStatus() != null && e.getStatus().isActive()) {
                active.add(e);
            }
        }
        return Collections.unmodifiableList(active);
    }
    public synchronized DiscoveryRecord getDiscovery(String id) { return discoveries.get(id); }
    public synchronized List<DiscoveryRecord> getAllDiscoveries() { return Collections.unmodifiableList(new ArrayList<>(discoveries.values())); }
    public synchronized List<InformationShareEvent> getShareEvents() { return Collections.unmodifiableList(shareEvents); }
    public synchronized List<PendingMonsterLairRecord> getPendingLairs() { return Collections.unmodifiableList(pendingLairs); }
    public synchronized List<AnomalyContestRecord> getPendingContests() { return Collections.unmodifiableList(pendingContests); }

    public int getAnnualSpawns() { return annualSpawns; }
    public int getAnnualFailedSpawnAttempts() { return annualFailedSpawnAttempts; }
    public int getAnnualCapDeferrals() { return annualCapDeferrals; }
    public int getAnnualDiscoveries() { return annualDiscoveries; }
    public int getAnnualSeerPrecognitions() { return annualSeerPrecognitions; }
    public int getAnnualExpeditionsDispatched() { return annualExpeditionsDispatched; }
    public int getAnnualExpeditionsReturned() { return annualExpeditionsReturned; }
    public int getAnnualSitesSecured() { return annualSitesSecured; }
    public int getAnnualSitesControlled() { return annualSitesControlled; }
    public int getAnnualSitesDepleted() { return annualSitesDepleted; }
    public int getAnnualContestsGenerated() { return annualContestsGenerated; }
    public int getAnnualExpiredToLair() { return annualExpiredToLair; }
    public double getAnnualRationsConsumedFood() { return annualRationsConsumedFood; }
    public double getAnnualSupernaturalDeliveredTotal() { return annualSupernaturalDeliveredTotal; }

    public synchronized String generateAnnualSummary(int year) {
        int forming = 0, revealed = 0, discovered = 0, activeExp = 0, securing = 0, controlled = 0, contested = 0, depleted = 0, lairs = 0;
        for (WorldAnomaly a : anomalies.values()) {
            switch (a.getState()) {
                case FORMING: forming++; break;
                case REVEALED: revealed++; break;
                case DISCOVERED: discovered++; break;
                case EXPEDITION_ACTIVE: activeExp++; break;
                case SECURING: securing++; break;
                case CONTROLLED: controlled++; break;
                case CONTESTED: contested++; break;
                case DEPLETED: depleted++; break;
                case MONSTER_LAIR_PENDING: lairs++; break;
                default: break;
            }
        }

        return String.format(
                "Anomalies[Form:%d Rev:%d Disc:%d Exp:%d Sec:%d Ctrl:%d Cnt:%d Dep:%d Lair:%d] | " +
                "Spawns:%d Disc:%d (Seer:%d) | Expeditions[Disp:%d Ret:%d FoodRation:%.0f] | Delivered:%.0f",
                forming, revealed, discovered, activeExp, securing, controlled, contested, depleted, lairs,
                annualSpawns, annualDiscoveries, annualSeerPrecognitions,
                annualExpeditionsDispatched, annualExpeditionsReturned, annualRationsConsumedFood, annualSupernaturalDeliveredTotal
        );
    }
}
