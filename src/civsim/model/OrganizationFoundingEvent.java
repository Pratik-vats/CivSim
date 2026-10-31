package civsim.model;

import java.util.Collections;
import java.util.List;

/**
 * Phase 2B.2 — Explicit, immutable founding event used to instantiate an Organization deterministically.
 */
public final class OrganizationFoundingEvent {

    private final String eventId;
    private final String organizationId;
    private final String displayName;
    private final OrganizationType organizationType;
    private final OrganizationAlignment alignment;
    private final String founderPersonId;
    private final List<String> initialMemberIds;
    private final OrganizationPolicyProfile policyProfile;
    private final int year;
    private final int month;

    public OrganizationFoundingEvent(
            String eventId,
            String organizationId,
            String displayName,
            OrganizationType organizationType,
            OrganizationAlignment alignment,
            String founderPersonId,
            List<String> initialMemberIds,
            OrganizationPolicyProfile policyProfile,
            int year,
            int month) {
        this.eventId = eventId;
        this.organizationId = organizationId;
        this.displayName = displayName != null ? displayName : organizationId;
        this.organizationType = organizationType != null ? organizationType : OrganizationType.CLAN;
        this.alignment = alignment != null ? alignment : OrganizationAlignment.NEUTRAL;
        this.founderPersonId = founderPersonId;
        this.initialMemberIds = (initialMemberIds != null) ? List.copyOf(initialMemberIds) : Collections.emptyList();
        this.policyProfile = policyProfile != null ? policyProfile : OrganizationPolicyProfile.createDefault(this.organizationType);
        this.year = year;
        this.month = month;
    }

    public String getEventId() { return eventId; }
    public String getOrganizationId() { return organizationId; }
    public String getDisplayName() { return displayName; }
    public OrganizationType getOrganizationType() { return organizationType; }
    public OrganizationAlignment getAlignment() { return alignment; }
    public String getFounderPersonId() { return founderPersonId; }
    public List<String> getInitialMemberIds() { return initialMemberIds; }
    public OrganizationPolicyProfile getPolicyProfile() { return policyProfile; }
    public int getYear() { return year; }
    public int getMonth() { return month; }
}

