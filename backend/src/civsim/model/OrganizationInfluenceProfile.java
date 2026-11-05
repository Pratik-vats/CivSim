package civsim.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Phase 2B.2 — Dynamically computed influence profile and power snapshot of an Organization.
 * Computed strictly from living ACTIVE members.
 */
public final class OrganizationInfluenceProfile {

    private final String organizationId;
    private final int timestampYear;
    private final int timestampMonth;
    private final int activeMemberCount;
    private final int mortalCombatantCount;
    private final int awakenedMemberCount;
    private final Map<Integer, Integer> tierCounts;
    private final double compositeInfluenceScore;

    public OrganizationInfluenceProfile(
            String organizationId,
            int timestampYear,
            int timestampMonth,
            int activeMemberCount,
            int mortalCombatantCount,
            int awakenedMemberCount,
            Map<Integer, Integer> tierCounts,
            double compositeInfluenceScore) {
        this.organizationId = organizationId;
        this.timestampYear = timestampYear;
        this.timestampMonth = timestampMonth;
        this.activeMemberCount = activeMemberCount;
        this.mortalCombatantCount = mortalCombatantCount;
        this.awakenedMemberCount = awakenedMemberCount;
        this.tierCounts = (tierCounts != null) ? Collections.unmodifiableMap(new HashMap<>(tierCounts)) : Collections.emptyMap();
        this.compositeInfluenceScore = Math.max(0.0, compositeInfluenceScore);
    }

    public String getOrganizationId() { return organizationId; }
    public int getTimestampYear() { return timestampYear; }
    public int getTimestampMonth() { return timestampMonth; }
    public int getActiveMemberCount() { return activeMemberCount; }
    public int getMortalCombatantCount() { return mortalCombatantCount; }
    public int getAwakenedMemberCount() { return awakenedMemberCount; }
    public Map<Integer, Integer> getTierCounts() { return tierCounts; }
    public double getCompositeInfluenceScore() { return compositeInfluenceScore; }

    public static double getTierInfluenceWeight(int tier) {
        switch (tier) {
            case 1: return 2.0;
            case 2: return 5.0;
            case 3: return 10.0;
            case 4: return 18.0;
            case 5: return 30.0;
            case 6: return 50.0;
            case 7: return 80.0;
            case 8: return 120.0;
            case 9: return 180.0;
            case 10: return 260.0;
            default: return 0.0;
        }
    }

    public static double getRoleInfluenceWeight(OrganizationRole role) {
        if (role == null) return 0.0;
        switch (role) {
            case LEADER: return 5.0;
            case FOUNDER: return 4.5;
            case ELDER: return 3.0;
            case OFFICER: return 2.5;
            case CHAMPION: return 4.0;
            case MEMBER: return 1.0;
            case RECRUIT: return 0.0;
            default: return 0.5;
        }
    }

    @Override
    public String toString() {
        return String.format("Influence[%s Y%d.M%d: Active:%d (Awk:%d, Mrt:%d) Score:%.1f]",
                organizationId, timestampYear, timestampMonth, activeMemberCount,
                awakenedMemberCount, mortalCombatantCount, compositeInfluenceScore);
    }
}

