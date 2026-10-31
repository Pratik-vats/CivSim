package civsim.model;

/**
 * Phase 2B.2 — Immutable milestone or chronicle event for an Organization.
 */
public final class OrganizationEvent {

    public enum EventType {
        FOUNDING,
        LEADERSHIP_SUCCESSION,
        POLICY_CHANGE,
        MEMBER_JOINED,
        MEMBER_LEFT,
        MEMBER_EXPELLED,
        MEMBER_SUSPENDED,
        MEMBER_REINSTATED,
        ROLE_CHANGED,
        TREATY_SIGNED,
        TREATY_BROKEN,
        DIPLOMATIC_CHANGE,
        COMBAT_ENGAGEMENT,
        STATUS_CHANGE
    }

    private final String eventId;
    private final String organizationId;
    private final int year;
    private final int month;
    private final EventType eventType;
    private final String description;
    private final String relatedPersonId;
    private final String relatedOrganizationId;

    public OrganizationEvent(
            String eventId,
            String organizationId,
            int year,
            int month,
            EventType eventType,
            String description,
            String relatedPersonId,
            String relatedOrganizationId) {
        this.eventId = eventId;
        this.organizationId = organizationId;
        this.year = year;
        this.month = month;
        this.eventType = eventType != null ? eventType : EventType.STATUS_CHANGE;
        this.description = description != null ? description : "";
        this.relatedPersonId = relatedPersonId;
        this.relatedOrganizationId = relatedOrganizationId;
    }

    public OrganizationEvent(String eventId, String organizationId, int year, int month, EventType type, String desc) {
        this(eventId, organizationId, year, month, type, desc, null, null);
    }

    public String getEventId() { return eventId; }
    public String getOrganizationId() { return organizationId; }
    public int getYear() { return year; }
    public int getMonth() { return month; }
    public EventType getEventType() { return eventType; }
    public String getDescription() { return description; }
    public String getRelatedPersonId() { return relatedPersonId; }
    public String getRelatedOrganizationId() { return relatedOrganizationId; }

    @Override
    public String toString() {
        return String.format("OrgEvent[%s (Y%d.M%d) %s: %s]",
                organizationId, year, month, eventType, description);
    }
}

