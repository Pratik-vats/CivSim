package civsim.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Phase 2B.2 — Unified, authoritative model of a Clan, Faction, or Organization.
 * Maintains membership by personId (String) to guarantee heap-pruning and GC safety.
 */
public class Organization {

    private final String organizationId;
    private final String displayName;
    private final OrganizationType organizationType;
    private final int foundingYear;
    private final int foundingMonth;
    private final String founderPersonId;

    private String currentLeaderPersonId;
    private OrganizationStatus status;
    private OrganizationAlignment alignment;
    private OrganizationPolicyProfile policyProfile;
    private double reputation;
    private final SupernaturalResourceInventory supernaturalInventory = new SupernaturalResourceInventory();

    // Phase 2C.1: Clan Vault & Heirlooms
    private final Map<String, EquipmentItem> clanVault = new LinkedHashMap<>();
    private final Map<String, ClanHeirloom> heirlooms = new LinkedHashMap<>();

    // Phase 2C.2: Feud Policy & Active Feuds
    private OrganizationalFeudPolicy feudPolicy;
    private final Set<String> activeFeudIds = new LinkedHashSet<>();

    // Membership records indexed by personId
    private final Map<String, MembershipRecord> membershipRecords = new LinkedHashMap<>();

    // Event chronicle & Idempotency
    private final List<OrganizationEvent> eventHistory = new ArrayList<>();
    private final Set<String> processedEventIds = new HashSet<>();

    // Combat & Activity Telemetry
    private int battlesParticipated = 0;
    private int battlesWon = 0;
    private int battlesWithdrawn = 0;
    private int memberCasualties = 0;

    public Organization(
            String organizationId,
            String displayName,
            OrganizationType organizationType,
            int foundingYear,
            int foundingMonth,
            String founderPersonId,
            OrganizationStatus status,
            OrganizationAlignment alignment,
            OrganizationPolicyProfile policyProfile) {
        this.organizationId = organizationId;
        this.displayName = displayName != null ? displayName : organizationId;
        this.organizationType = organizationType != null ? organizationType : OrganizationType.CLAN;
        this.foundingYear = foundingYear;
        this.foundingMonth = foundingMonth;
        this.founderPersonId = founderPersonId;
        this.currentLeaderPersonId = founderPersonId;
        this.status = status != null ? status : OrganizationStatus.ACTIVE;
        this.alignment = alignment != null ? alignment : OrganizationAlignment.NEUTRAL;
        this.policyProfile = policyProfile != null ? policyProfile : OrganizationPolicyProfile.createDefault(this.organizationType);
        this.feudPolicy = OrganizationalFeudPolicy.createDefault(this.alignment, this.organizationType);
        this.reputation = 0.0;
    }

    public String getOrganizationId() { return organizationId; }
    public String getDisplayName() { return displayName; }
    public OrganizationType getOrganizationType() { return organizationType; }
    public int getFoundingYear() { return foundingYear; }
    public int getFoundingMonth() { return foundingMonth; }
    public String getFounderPersonId() { return founderPersonId; }
    public String getCurrentLeaderPersonId() { return currentLeaderPersonId; }
    public OrganizationStatus getStatus() { return status; }
    public OrganizationAlignment getAlignment() { return alignment; }
    public OrganizationPolicyProfile getPolicyProfile() { return policyProfile; }
    public double getReputation() { return reputation; }
    public SupernaturalResourceInventory getSupernaturalInventory() { return supernaturalInventory; }
    public int getBattlesParticipated() { return battlesParticipated; }
    public int getBattlesWon() { return battlesWon; }
    public int getBattlesWithdrawn() { return battlesWithdrawn; }
    public int getMemberCasualties() { return memberCasualties; }

    public void setCurrentLeaderPersonId(String currentLeaderPersonId) {
        this.currentLeaderPersonId = currentLeaderPersonId;
    }

    public void setBattlesParticipated(int battlesParticipated) {
        this.battlesParticipated = battlesParticipated;
    }

    public void setBattlesWon(int battlesWon) {
        this.battlesWon = battlesWon;
    }

    public void setPolicyProfile(OrganizationPolicyProfile policyProfile) {
        if (policyProfile != null) {
            this.policyProfile = policyProfile;
        }
    }

    public void setAlignment(OrganizationAlignment alignment) {
        if (alignment != null) {
            this.alignment = alignment;
        }
    }

    public void adjustReputation(double delta) {
        this.reputation = Math.max(-100.0, Math.min(100.0, this.reputation + delta));
    }

    public void setReputation(double rep) {
        this.reputation = Math.max(-100.0, Math.min(100.0, rep));
    }

    // --- Phase 2C.1: Clan Vault & Heirlooms ---
    public Map<String, EquipmentItem> getClanVault() {
        return Collections.unmodifiableMap(clanVault);
    }

    public void addVaultEquipment(EquipmentItem item) {
        if (item != null) {
            clanVault.put(item.getEquipmentId(), item);
            item.setLifecycleStatus(EquipmentLifecycleStatus.IN_CLAN_VAULT);
            item.setCurrentCustodianId("VAULT_" + this.organizationId);
        }
    }

    public EquipmentItem removeVaultEquipment(String equipmentId) {
        if (equipmentId == null) return null;
        return clanVault.remove(equipmentId);
    }

    public EquipmentItem getVaultEquipment(String equipmentId) {
        if (equipmentId == null) return null;
        return clanVault.get(equipmentId);
    }

    public Map<String, ClanHeirloom> getHeirlooms() {
        return Collections.unmodifiableMap(heirlooms);
    }

    public void registerHeirloom(ClanHeirloom heirloom) {
        if (heirloom != null) {
            heirlooms.put(heirloom.getHeirloomId(), heirloom);
        }
    }

    public ClanHeirloom getHeirloom(String heirloomId) {
        if (heirloomId == null) return null;
        return heirlooms.get(heirloomId);
    }

    public boolean markEventProcessed(String eventId) {
        if (eventId == null || eventId.trim().isEmpty()) return true;
        return processedEventIds.add(eventId);
    }

    public boolean isEventProcessed(String eventId) {
        if (eventId == null) return false;
        return processedEventIds.contains(eventId);
    }

    public void addEvent(OrganizationEvent event) {
        if (event == null) return;
        if (eventHistory.size() >= 100) {
            eventHistory.remove(0); // prune oldest
        }
        eventHistory.add(event);
    }

    public List<OrganizationEvent> getEventHistory() {
        return Collections.unmodifiableList(eventHistory);
    }

    // --- Membership Invariants & Operations ---

    public boolean addMembershipRecord(MembershipRecord record) {
        if (record == null) return false;
        if (status == OrganizationStatus.DISSOLVED) return false;

        // Invariant 1: No duplicate active records per (personId, organizationId)
        MembershipRecord existing = membershipRecords.get(record.getPersonId());
        if (existing != null && existing.getStatus().isCurrentAffiliation() && record.getStatus().isCurrentAffiliation()) {
            return false; // rejected duplicate active
        }

        membershipRecords.put(record.getPersonId(), record);
        return true;
    }

    public MembershipRecord getMembershipRecord(String personId) {
        if (personId == null) return null;
        return membershipRecords.get(personId);
    }

    public Map<String, MembershipRecord> getAllMembershipRecords() {
        return Collections.unmodifiableMap(membershipRecords);
    }

    public List<MembershipRecord> getActiveMembershipRecords() {
        List<MembershipRecord> active = new ArrayList<>();
        for (MembershipRecord rec : membershipRecords.values()) {
            if (rec.isActive()) {
                active.add(rec);
            }
        }
        return active;
    }

    public int getActiveMemberCount() {
        int count = 0;
        for (MembershipRecord rec : membershipRecords.values()) {
            if (rec.isActive()) count++;
        }
        return count;
    }

    public int getMemberCount() {
        return getActiveMemberCount();
    }

    public MembershipRecord getMembership(int personId) {
        return getMembershipRecord(String.valueOf(personId));
    }

    public MembershipRecord getMembership(String personId) {
        return getMembershipRecord(personId);
    }

    public boolean isMemberActive(String personId) {
        MembershipRecord rec = getMembershipRecord(personId);
        return rec != null && rec.isActive();
    }

    public boolean isMember(String personId) {
        return isMemberActive(personId);
    }

    // --- Leadership & Status Transitions ---

    public void setLeader(String leaderPersonId, int year, int month, String reason) {
        String oldLeader = this.currentLeaderPersonId;
        this.currentLeaderPersonId = leaderPersonId;

        if (oldLeader != null && !oldLeader.equals(leaderPersonId)) {
            MembershipRecord oldRec = getMembershipRecord(oldLeader);
            if (oldRec != null && oldRec.getRole() == OrganizationRole.LEADER) {
                oldRec.setRole(OrganizationRole.ELDER, year, month);
            }
        }
        if (leaderPersonId != null) {
            MembershipRecord newRec = getMembershipRecord(leaderPersonId);
            if (newRec != null) {
                newRec.setRole(OrganizationRole.LEADER, year, month);
            }
        }

        addEvent(new OrganizationEvent(
                "EVT:LEAD:" + organizationId + ":" + year + ":" + month,
                organizationId, year, month,
                OrganizationEvent.EventType.LEADERSHIP_SUCCESSION,
                String.format("Leadership transitioned from %s to %s (%s)",
                        oldLeader != null ? oldLeader : "None",
                        leaderPersonId != null ? leaderPersonId : "None",
                        reason != null ? reason : "succession"),
                leaderPersonId, null
        ));
    }

    public void clearLeader() {
        this.currentLeaderPersonId = null;
    }

    public void setStatus(OrganizationStatus newStatus, int year, int month, String reason) {
        if (newStatus == null || this.status == newStatus) return;
        OrganizationStatus old = this.status;
        this.status = newStatus;
        if (newStatus == OrganizationStatus.DISSOLVED || newStatus == OrganizationStatus.DORMANT) {
            clearLeader();
        }
        addEvent(new OrganizationEvent(
                "EVT:STATUS:" + organizationId + ":" + year + ":" + month,
                organizationId, year, month,
                OrganizationEvent.EventType.STATUS_CHANGE,
                String.format("Status changed from %s to %s (%s)", old.getDisplayName(), newStatus.getDisplayName(), reason != null ? reason : "transition")
        ));
    }

    // --- Combat Telemetry Hook ---

    public void recordBattleEngagement(boolean victory, boolean withdrawal, int casualtiesSuffered) {
        this.battlesParticipated++;
        if (victory) this.battlesWon++;
        if (withdrawal) this.battlesWithdrawn++;
        this.memberCasualties += Math.max(0, casualtiesSuffered);
    }

    // --- Deterministic Leadership Succession Evaluation ---

    /**
     * Evaluates succession deterministically when current leader dies, departs, or is ineligible.
     */
    public String evaluateSuccession(Map<String, Person> livingPeople, int currentYear, int currentMonth) {
        if (status == OrganizationStatus.DISSOLVED) {
            return null;
        }

        // Verify or clear current leader if dead or departed
        if (currentLeaderPersonId != null) {
            Person currentLeader = livingPeople.get(currentLeaderPersonId);
            MembershipRecord rec = getMembershipRecord(currentLeaderPersonId);
            if (currentLeader == null || !currentLeader.isAlive() || rec == null || !rec.isActive()) {
                clearLeader();
            } else {
                return currentLeaderPersonId; // Leader is still alive and active
            }
        }

        // Filter eligible adult active members (role != RECRUIT, age >= 18)
        List<MembershipRecord> candidates = new ArrayList<>();
        for (MembershipRecord rec : membershipRecords.values()) {
            if (!rec.isActive() || rec.getRole().isProbationary()) continue;
            Person p = livingPeople.get(rec.getPersonId());
            if (p != null && p.isAlive() && p.getAge() >= 18) {
                candidates.add(rec);
            }
        }

        if (candidates.isEmpty()) {
            if (getActiveMemberCount() == 0) {
                setStatus(OrganizationStatus.DISSOLVED, currentYear, currentMonth, "All members deceased or departed");
            } else {
                setStatus(OrganizationStatus.DORMANT, currentYear, currentMonth, "Zero eligible adult candidates for leadership");
            }
            return null;
        }

        SuccessionPolicy policy = policyProfile.getSuccessionPolicy();
        Person founder = (founderPersonId != null) ? livingPeople.get(founderPersonId) : null;

        // Score each candidate deterministically
        MembershipRecord bestCandidate = null;
        double bestScore = -Double.MAX_VALUE;

        // Sort first by tie-breaker: earliest joined (lowest year*12+month), then lowest personId
        candidates.sort(Comparator.comparingInt((MembershipRecord r) -> r.getJoinedYear() * 12 + r.getJoinedMonth())
                .thenComparing(MembershipRecord::getPersonId));

        for (MembershipRecord candidate : candidates) {
            Person person = livingPeople.get(candidate.getPersonId());
            if (person == null) continue;

            double roleScore = candidate.getRole().getRankScore();
            double tenureYears = Math.max(0, currentYear - candidate.getJoinedYear());

            // Capability score
            double capabilityScore = 20.0;
            if (person.isAwakened() && person.getAetherProfile() != null) {
                capabilityScore = person.getAetherProfile().getMastery() * (1.0 + person.getAetherProfile().getPowerTier());
            } else {
                capabilityScore = (person.getEffectiveAttribute(AttributeType.STRENGTH)
                        + person.getEffectiveAttribute(AttributeType.AGILITY)
                        + person.getEffectiveAttribute(AttributeType.VITALITY)
                        + person.getEffectiveAttribute(AttributeType.ENDURANCE)) / 4.0;
            }

            // Kinship score
            double kinshipScore = 0.0;
            if (founder != null && founder.getFamily() != null && person.getFamily() != null
                    && founder.getFamily().getId() == person.getFamily().getId()) {
                kinshipScore = 100.0;
            }

            // Leadership attributes
            double leadershipScore = person.getEffectiveAttribute(AttributeType.WILLPOWER)
                    + person.getEffectiveAttribute(AttributeType.INTELLIGENCE);

            double totalScore = (policy.getWeightRoleRank() * roleScore)
                    + (policy.getWeightTenure() * tenureYears)
                    + (policy.getWeightPower() * capabilityScore)
                    + (policy.getWeightKinship() * kinshipScore)
                    + (policy.getWeightLeadership() * leadershipScore);

            if (totalScore > bestScore) {
                bestScore = totalScore;
                bestCandidate = candidate;
            }
        }

        if (bestCandidate != null) {
            setLeader(bestCandidate.getPersonId(), currentYear, currentMonth,
                    String.format("Succession under %s (Score: %.1f)", policy.getDisplayName(), bestScore));
            return bestCandidate.getPersonId();
        }

        return null;
    }

    // --- Dynamic Influence Profiling ---

    public OrganizationInfluenceProfile computeInfluenceProfile(Map<String, Person> livingPeople, int currentYear, int currentMonth) {
        int activeCount = 0;
        int mortalCount = 0;
        int awakenedCount = 0;
        Map<Integer, Integer> tierCounts = new HashMap<>();
        double activeSum = 0.0;

        for (MembershipRecord rec : membershipRecords.values()) {
            if (!rec.isActive()) continue;
            Person p = (livingPeople != null) ? livingPeople.get(rec.getPersonId()) : null;
            if (p == null || !p.isAlive()) continue;

            activeCount++;
            double roleWeight = OrganizationInfluenceProfile.getRoleInfluenceWeight(rec.getRole());
            double tierInfluence = 0.0;

            if (p.isAwakened() && p.getAetherProfile() != null) {
                awakenedCount++;
                int tier = p.getAetherProfile().getPowerTier();
                tierCounts.put(tier, tierCounts.getOrDefault(tier, 0) + 1);
                tierInfluence = OrganizationInfluenceProfile.getTierInfluenceWeight(tier);
            } else {
                mortalCount++;
            }

            activeSum += (1.0 + roleWeight + tierInfluence);
        }

        // Leadership factor
        double leadershipFactor = 0.8;
        if (currentLeaderPersonId != null && livingPeople != null) {
            Person leader = livingPeople.get(currentLeaderPersonId);
            if (leader != null && leader.isAlive()) {
                double will = leader.getEffectiveAttribute(AttributeType.WILLPOWER);
                double intel = leader.getEffectiveAttribute(AttributeType.INTELLIGENCE);
                leadershipFactor = Math.max(0.5, Math.min(1.8, 0.5 + ((will + intel) / 150.0)));
            }
        }

        // Reputation factor: 1.0 + (reputation / 200.0) -> range [0.5, 1.5]
        double reputationFactor = Math.max(0.5, Math.min(1.5, 1.0 + (reputation / 200.0)));

        double compositeScore = activeSum * leadershipFactor * reputationFactor;

        return new OrganizationInfluenceProfile(
                organizationId, currentYear, currentMonth,
                activeCount, mortalCount, awakenedCount,
                tierCounts, compositeScore
        );
    }

    public OrganizationalFeudPolicy getFeudPolicy() {
        if (feudPolicy == null) {
            feudPolicy = OrganizationalFeudPolicy.createDefault(this.alignment, this.organizationType);
        }
        return feudPolicy;
    }

    public void setFeudPolicy(OrganizationalFeudPolicy feudPolicy) {
        this.feudPolicy = feudPolicy;
    }

    public Set<String> getActiveFeudIds() {
        return Collections.unmodifiableSet(activeFeudIds);
    }

    public void addActiveFeud(String feudId) {
        if (feudId != null) activeFeudIds.add(feudId);
    }

    public void removeActiveFeud(String feudId) {
        if (feudId != null) activeFeudIds.remove(feudId);
    }

    @Override
    public String toString() {
        return String.format("Organization[%s (%s, %s): %s | Leader:%s Active:%d Rep:%.1f]",
                organizationId, organizationType.getDisplayName(), alignment.getDisplayName(),
                status.getDisplayName(), currentLeaderPersonId != null ? currentLeaderPersonId : "None",
                getActiveMemberCount(), reputation);
    }
}

