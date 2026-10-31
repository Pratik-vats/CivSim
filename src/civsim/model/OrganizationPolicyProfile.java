package civsim.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * Phase 2B.2 — Configurable operational, succession, exclusivity, and recruitment policy profile.
 */
public class OrganizationPolicyProfile {

    private final boolean exclusiveMembershipRequired;
    private final Set<OrganizationType> forbiddenOrganizationTypes;
    private final Set<OrganizationAlignment> forbiddenAlignments;
    private final Integer requiredSettlementId;
    private final boolean allowCrossSettlement;
    private final SuccessionPolicy successionPolicy;
    private final Set<AetherAspect> allowedAspects;
    private final int minAge;
    private final boolean requiresAwakened;
    private final int minTier;
    private final double minMastery;
    private final boolean openRecruitment;

    public OrganizationPolicyProfile(
            boolean exclusiveMembershipRequired,
            Set<OrganizationType> forbiddenOrganizationTypes,
            Set<OrganizationAlignment> forbiddenAlignments,
            Integer requiredSettlementId,
            boolean allowCrossSettlement,
            SuccessionPolicy successionPolicy,
            Set<AetherAspect> allowedAspects,
            int minAge,
            boolean requiresAwakened,
            int minTier,
            double minMastery,
            boolean openRecruitment) {
        this.exclusiveMembershipRequired = exclusiveMembershipRequired;
        this.forbiddenOrganizationTypes = forbiddenOrganizationTypes != null
                ? Collections.unmodifiableSet(new HashSet<>(forbiddenOrganizationTypes))
                : Collections.emptySet();
        this.forbiddenAlignments = forbiddenAlignments != null
                ? Collections.unmodifiableSet(new HashSet<>(forbiddenAlignments))
                : Collections.emptySet();
        this.requiredSettlementId = requiredSettlementId;
        this.allowCrossSettlement = allowCrossSettlement;
        this.successionPolicy = successionPolicy != null ? successionPolicy : SuccessionPolicy.LINEAGE_AND_SENIORITY;
        this.allowedAspects = allowedAspects != null
                ? Collections.unmodifiableSet(new HashSet<>(allowedAspects))
                : Collections.emptySet();
        this.minAge = Math.max(0, minAge);
        this.requiresAwakened = requiresAwakened;
        this.minTier = Math.max(0, minTier);
        this.minMastery = Math.max(0.0, minMastery);
        this.openRecruitment = openRecruitment;
    }

    public boolean isExclusiveMembershipRequired() { return exclusiveMembershipRequired; }
    public Set<OrganizationType> getForbiddenOrganizationTypes() { return forbiddenOrganizationTypes; }
    public Set<OrganizationAlignment> getForbiddenAlignments() { return forbiddenAlignments; }
    public Integer getRequiredSettlementId() { return requiredSettlementId; }
    public boolean isAllowCrossSettlement() { return allowCrossSettlement; }
    public SuccessionPolicy getSuccessionPolicy() { return successionPolicy; }
    public Set<AetherAspect> getAllowedAspects() { return allowedAspects; }
    public int getMinAge() { return minAge; }
    public boolean isRequiresAwakened() { return requiresAwakened; }
    public int getMinTier() { return minTier; }
    public double getMinMastery() { return minMastery; }
    public boolean isOpenRecruitment() { return openRecruitment; }

    public boolean isAspectAllowed(AetherAspect aspect) {
        if (allowedAspects.isEmpty()) return true;
        if (aspect == null) return allowedAspects.contains(AetherAspect.AETHERIC);
        return allowedAspects.contains(aspect);
    }

    // --- Preset Factories ---

    public static OrganizationPolicyProfile createDefault(OrganizationType type) {
        switch (type) {
            case CLAN: return createDefaultClanPolicy();
            case AWAKENED_ORDER: return createDefaultAwakenedOrderPolicy();
            case DARK_COVEN: return createDefaultDarkCovenPolicy();
            case MERCENARY_COMPANY: return createDefaultMercenaryPolicy();
            case SCHOLARLY_ORDER: return createDefaultScholarlyOrderPolicy();
            case SETTLEMENT_GUARD: return createDefaultSettlementGuardPolicy(null);
            case RELIGIOUS_OR_PHILOSOPHICAL_ORDER: return createDefaultReligiousOrderPolicy();
            default: return createDefaultClanPolicy();
        }
    }

    public static OrganizationPolicyProfile createDefaultClanPolicy() {
        return new OrganizationPolicyProfile(
                false,
                Collections.emptySet(),
                Collections.emptySet(),
                null,
                true,
                SuccessionPolicy.LINEAGE_AND_SENIORITY,
                Collections.emptySet(), // any aspect
                0,
                false,
                0,
                0.0,
                false
        );
    }

    public static OrganizationPolicyProfile createDefaultAwakenedOrderPolicy() {
        return new OrganizationPolicyProfile(
                false,
                Collections.emptySet(),
                Set.of(OrganizationAlignment.DARK_ALIGNED),
                null,
                true,
                SuccessionPolicy.MASTERY_AND_TIER_MERIT,
                Set.of(AetherAspect.AETHERIC),
                16,
                true,
                1,
                10.0,
                true
        );
    }

    public static OrganizationPolicyProfile createDefaultDarkCovenPolicy() {
        return new OrganizationPolicyProfile(
                false,
                Collections.emptySet(),
                Set.of(OrganizationAlignment.AETHERIC_ALIGNED),
                null,
                true,
                SuccessionPolicy.AMBITION_AND_POWER,
                Set.of(AetherAspect.DARK),
                16,
                false, // can recruit mortals with high greed / resonance
                0,
                0.0,
                false
        );
    }

    public static OrganizationPolicyProfile createDefaultMercenaryPolicy() {
        return new OrganizationPolicyProfile(
                false,
                Collections.emptySet(),
                Collections.emptySet(),
                null,
                true,
                SuccessionPolicy.COMBAT_PROWESS_AND_EXPERIENCE,
                Collections.emptySet(),
                18,
                false,
                0,
                0.0,
                true
        );
    }

    public static OrganizationPolicyProfile createDefaultScholarlyOrderPolicy() {
        return new OrganizationPolicyProfile(
                false,
                Collections.emptySet(),
                Collections.emptySet(),
                null,
                true,
                SuccessionPolicy.COUNCIL_OR_ELDER_ELECTION,
                Collections.emptySet(),
                18,
                false,
                0,
                0.0,
                true
        );
    }

    public static OrganizationPolicyProfile createDefaultSettlementGuardPolicy(Integer settlementId) {
        return new OrganizationPolicyProfile(
                false,
                Set.of(OrganizationType.DARK_COVEN),
                Set.of(OrganizationAlignment.DARK_ALIGNED),
                settlementId,
                settlementId == null, // false if tied to settlement
                SuccessionPolicy.SETTLEMENT_APPOINTED,
                Collections.emptySet(),
                18,
                false,
                0,
                0.0,
                true
        );
    }

    public static OrganizationPolicyProfile createDefaultReligiousOrderPolicy() {
        return new OrganizationPolicyProfile(
                false,
                Collections.emptySet(),
                Set.of(OrganizationAlignment.DARK_ALIGNED),
                null,
                true,
                SuccessionPolicy.COUNCIL_OR_ELDER_ELECTION,
                Collections.emptySet(),
                16,
                false,
                0,
                0.0,
                true
        );
    }
}

