package civsim.model;

import civsim.spatial.Biome;

import java.util.*;

/**
 * Phase 2B.3 / Phase 2B.4 -- World Anomaly representing a rare supernatural resource site.
 * Tracks spatial location, deposit reserves, two-stage appearance (FORMING -> REVEALED),
 * Seer precognition, discovery records, expeditions, ownership lifecycle, 6-month mutation deadline,
 * and strict conservation accounting.
 */
public class WorldAnomaly {
    private final String anomalyId;
    private final String name;
    private final double posX;
    private final double posY;
    private final Biome biome;
    private final SupernaturalResourceType supernaturalResourceType;
    private final AnomalyResourceType resourceType; // legacy adapter
    private final ResourceGrade resourceGrade;

    private final double initialQuantity;
    private double remainingQuantity;
    private double cumulativeExtractedTotal;

    private AnomalyState state;
    private final int formationMonth;
    private int publicAppearanceMonth;
    private Integer firstDiscoveryMonth;
    private int unclaimedExpirationMonth; // 6-month mutation deadline from publicAppearanceMonth
    private boolean publiclyObservable;

    private final double baseDetectionRadius;
    private final double attractionRadius;

    private long reengagementCooldownUntilMonth;
    private String controllingOwnerId; // Coalition side ID, Settlement ID, or Organization ID
    private int securingDurationMonths;
    private int securingRemainingMonths;
    private int securingStartedMonth;
    private int securingCompleteMonth;
    private int encounterSequence;
    private String currentContestId;

    // Discovery & Expedition tracking
    private final Set<String> knownDiscovererPersonIds = new HashSet<>();
    private final Set<String> knownDiscovererFactionIds = new HashSet<>();
    private final List<DiscoveryRecord> discoveryRecords = new ArrayList<>();
    private final List<String> activeExpeditionIds = new ArrayList<>();

    // Conservation tracking buckets
    private double deliveredTotal;
    private double mutatedOrAbandonedTotal;

    /**
     * Phase 2B.3 Authoritative Full Constructor.
     */
    public WorldAnomaly(
            String anomalyId,
            String name,
            double posX,
            double posY,
            Biome biome,
            SupernaturalResourceType supernaturalResourceType,
            ResourceGrade resourceGrade,
            int initialQuantity,
            int formationMonth) {
        this.anomalyId = anomalyId;
        this.name = (name != null) ? name : anomalyId;
        this.posX = posX;
        this.posY = posY;
        this.biome = (biome != null) ? biome : Biome.RIVER_VALLEY;
        this.supernaturalResourceType = (supernaturalResourceType != null) ? supernaturalResourceType : SupernaturalResourceType.RAW_AETHER_CRYSTAL;
        this.resourceType = this.supernaturalResourceType.toLegacyType();
        this.resourceGrade = (resourceGrade != null) ? resourceGrade : ResourceGrade.COMMON;

        this.initialQuantity = Math.max(1, initialQuantity);
        this.remainingQuantity = this.initialQuantity;
        this.cumulativeExtractedTotal = 0.0;

        this.state = AnomalyState.FORMING;
        this.formationMonth = formationMonth;
        this.publicAppearanceMonth = formationMonth + 2; // default 2-month formation lead time
        this.firstDiscoveryMonth = null;
        this.unclaimedExpirationMonth = this.publicAppearanceMonth + 6; // 6 months from public appearance
        this.publiclyObservable = false;

        // Base detection: 35.0 base + 8.0 per grade step above COMMON
        this.baseDetectionRadius = 35.0 + (8.0 * this.resourceGrade.getGradeStep());
        this.attractionRadius = 1.4 * this.baseDetectionRadius;

        this.reengagementCooldownUntilMonth = 0L;
        this.controllingOwnerId = null;
        this.securingDurationMonths = 1;
        this.securingRemainingMonths = 0;
        this.securingStartedMonth = 0;
        this.securingCompleteMonth = 0;
        this.encounterSequence = 0;
        this.currentContestId = null;

        this.deliveredTotal = 0.0;
        this.mutatedOrAbandonedTotal = 0.0;
    }

    /**
     * Phase 2B.4 Backward Compatibility Constructor.
     */
    public WorldAnomaly(
            String anomalyId,
            String name,
            double posX,
            double posY,
            Biome biome,
            AnomalyResourceType legacyResourceType,
            double initialQuantity,
            int discoveredYear,
            int discoveredMonth,
            long discoveredTotalMonths) {
        this(
                anomalyId,
                name,
                posX,
                posY,
                biome,
                SupernaturalResourceType.fromLegacyType(legacyResourceType),
                ResourceGrade.COMMON,
                (int) Math.round(initialQuantity),
                (int) discoveredTotalMonths
        );
        // In legacy tests, anomalies were registered already revealed/unclaimed
        this.state = AnomalyState.UNCLAIMED;
        this.publiclyObservable = true;
        this.publicAppearanceMonth = (int) discoveredTotalMonths;
        this.unclaimedExpirationMonth = (int) discoveredTotalMonths + 6;
    }

    // --- Core Getters ---
    public String getAnomalyId() { return anomalyId; }
    public String getName() { return name; }
    public double getPosX() { return posX; }
    public double getPosY() { return posY; }
    public Biome getBiome() { return biome; }
    public SupernaturalResourceType getSupernaturalResourceType() { return supernaturalResourceType; }
    public AnomalyResourceType getResourceType() { return resourceType; }
    public ResourceGrade getResourceGrade() { return resourceGrade; }
    public String getResourceCategory() {
        return supernaturalResourceType != null ? supernaturalResourceType.name() : (resourceType != null ? resourceType.name() : "AETHER_DEPOSIT");
    }

    public double getInitialQuantity() { return initialQuantity; }
    public double getRemainingQuantity() { return remainingQuantity; }
    public double getCumulativeExtractedTotal() { return cumulativeExtractedTotal; }

    public AnomalyState getState() { return state; }
    public void setState(AnomalyState state) { this.state = state; }

    public int getFormationMonth() { return formationMonth; }
    public int getPublicAppearanceMonth() { return publicAppearanceMonth; }
    public Integer getFirstDiscoveryMonth() { return firstDiscoveryMonth; }
    public int getUnclaimedExpirationMonth() { return unclaimedExpirationMonth; }
    public boolean isPubliclyObservable() { return publiclyObservable; }

    public double getBaseDetectionRadius() { return baseDetectionRadius; }
    public double getAttractionRadius() { return attractionRadius; }

    public long getReengagementCooldownUntilMonth() { return reengagementCooldownUntilMonth; }
    public void setReengagementCooldownUntilMonth(long month) { this.reengagementCooldownUntilMonth = month; }

    public String getControllingOwnerId() { return controllingOwnerId; }
    public void setControllingOwnerId(String ownerId) { this.controllingOwnerId = ownerId; }

    public int getSecuringDurationMonths() { return securingDurationMonths; }
    public void setSecuringDurationMonths(int duration) { this.securingDurationMonths = duration; }

    public int getSecuringRemainingMonths() { return securingRemainingMonths; }
    public void setSecuringRemainingMonths(int months) { this.securingRemainingMonths = months; }

    public int getSecuringStartedMonth() { return securingStartedMonth; }
    public int getSecuringCompleteMonth() { return securingCompleteMonth; }

    public int getEncounterSequence() { return encounterSequence; }
    public int incrementEncounterSequence() { return ++encounterSequence; }

    public String getCurrentContestId() { return currentContestId; }
    public void setCurrentContestId(String contestId) { this.currentContestId = contestId; }

    public double getDeliveredTotal() { return deliveredTotal; }
    public void addDeliveredTotal(double amount) { this.deliveredTotal += Math.max(0.0, amount); }

    public double getMutatedOrAbandonedTotal() { return mutatedOrAbandonedTotal; }
    public void addMutatedOrAbandonedTotal(double amount) { this.mutatedOrAbandonedTotal += Math.max(0.0, amount); }

    public List<DiscoveryRecord> getDiscoveryRecords() {
        return Collections.unmodifiableList(discoveryRecords);
    }

    public List<String> getActiveExpeditionIds() {
        return Collections.unmodifiableList(activeExpeditionIds);
    }

    public void addActiveExpedition(String expeditionId) {
        if (expeditionId != null && !activeExpeditionIds.contains(expeditionId)) {
            activeExpeditionIds.add(expeditionId);
            if (this.state == AnomalyState.REVEALED || this.state == AnomalyState.DISCOVERED || this.state == AnomalyState.UNCLAIMED) {
                this.state = AnomalyState.EXPEDITION_ACTIVE;
            }
        }
    }

    public void removeActiveExpedition(String expeditionId) {
        activeExpeditionIds.remove(expeditionId);
    }

    // --- State Transitions ---

    public void transitionToRevealed() {
        this.publiclyObservable = true;
        if (this.state == AnomalyState.FORMING) {
            this.state = AnomalyState.REVEALED;
        }
    }

    public void recordDiscovery(DiscoveryRecord record) {
        if (record == null) return;
        this.discoveryRecords.add(record);
        if (record.getDiscovererPersonId() != null) {
            this.knownDiscovererPersonIds.add(record.getDiscovererPersonId());
        }
        if (record.getOwningOrganizationId() != null) {
            this.knownDiscovererFactionIds.add(record.getOwningOrganizationId());
        }
        if (this.firstDiscoveryMonth == null) {
            this.firstDiscoveryMonth = record.getDiscoveryTotalMonths();
            if (this.state == AnomalyState.REVEALED || this.state == AnomalyState.UNCLAIMED) {
                this.state = AnomalyState.DISCOVERED;
            }
        }
    }

    public boolean isKnownByFaction(String factionOrVillageId) {
        if (factionOrVillageId == null) return false;
        return knownDiscovererFactionIds.contains(factionOrVillageId);
    }

    public boolean isKnownByPerson(String personId) {
        if (personId == null) return false;
        return knownDiscovererPersonIds.contains(personId);
    }

    public boolean hasMutationDeadlineExpired(long currentTotalMonths) {
        return (state != AnomalyState.CONTROLLED && state != AnomalyState.DEPLETED && state != AnomalyState.MONSTER_LAIR_PENDING)
                && currentTotalMonths >= unclaimedExpirationMonth;
    }

    public boolean isReengagementCooldownActive(long currentTotalMonths) {
        return currentTotalMonths < reengagementCooldownUntilMonth;
    }

    public void transitionToSecuring(String winningCoalitionId, int durationMonths, int currentTotalMonths) {
        this.state = AnomalyState.SECURING;
        this.controllingOwnerId = winningCoalitionId;
        this.securingDurationMonths = Math.max(1, durationMonths);
        this.securingRemainingMonths = this.securingDurationMonths;
        this.securingStartedMonth = currentTotalMonths;
    }

    public void transitionToSecuring(String winningCoalitionId, int durationMonths) {
        transitionToSecuring(winningCoalitionId, durationMonths, (int) this.formationMonth);
    }

    public void transitionToControlled(int currentTotalMonths) {
        this.state = AnomalyState.CONTROLLED;
        this.securingRemainingMonths = 0;
        this.securingCompleteMonth = currentTotalMonths;
    }

    public void transitionToControlled() {
        transitionToControlled((int) this.formationMonth);
    }

    public void transitionToContested(String contestId) {
        this.state = AnomalyState.CONTESTED;
        this.currentContestId = contestId;
    }

    public void transitionToMonsterLair() {
        this.state = AnomalyState.MONSTER_LAIR_PENDING;
        this.mutatedOrAbandonedTotal += this.remainingQuantity;
        this.remainingQuantity = 0.0;
    }

    public double extractResources(double requestedAmount) {
        if (state != AnomalyState.CONTROLLED || remainingQuantity <= 0.0) {
            return 0.0;
        }
        double extracted = Math.min(remainingQuantity, Math.max(0.0, requestedAmount));
        remainingQuantity -= extracted;
        cumulativeExtractedTotal += extracted;
        if (remainingQuantity <= 0.0001) {
            remainingQuantity = 0.0;
            state = AnomalyState.DEPLETED;
        }
        return extracted;
    }

    public void returnResources(double amount) {
        if (amount <= 0.0) return;
        this.remainingQuantity += amount;
        this.cumulativeExtractedTotal = Math.max(0.0, this.cumulativeExtractedTotal - amount);
        if (this.state == AnomalyState.DEPLETED && this.remainingQuantity > 0.0) {
            this.state = AnomalyState.CONTROLLED;
        }
    }

    @Override
    public String toString() {
        return String.format("WorldAnomaly[%s (%s, %s): %s | Pos:(%.1f, %.1f) Rem:%.1f/%.1f Owner:%s Exp:M%d]",
                anomalyId, supernaturalResourceType.getDisplayName(), resourceGrade.getDisplayName(), state.getDisplayName(),
                posX, posY, remainingQuantity, initialQuantity, controllingOwnerId, unclaimedExpirationMonth);
    }
}
