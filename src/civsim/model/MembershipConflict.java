package civsim.model;

import java.util.Collections;
import java.util.List;

/**
 * Phase 2B.2 — Conflict between two organization memberships held by the same person.
 */
public class MembershipConflict {
    private final String personId;
    private final String existingOrganizationId;
    private final String attemptedOrganizationId;
    private final String reason;

    public MembershipConflict(String personId, String existingOrganizationId, String attemptedOrganizationId, String reason) {
        this.personId = personId;
        this.existingOrganizationId = existingOrganizationId;
        this.attemptedOrganizationId = attemptedOrganizationId;
        this.reason = reason;
    }

    public String getPersonId() { return personId; }
    public String getExistingOrganizationId() { return existingOrganizationId; }
    public String getAttemptedOrganizationId() { return attemptedOrganizationId; }
    public String getReason() { return reason; }

    @Override
    public String toString() {
        return String.format("MembershipConflict[%s: existing=%s vs attempted=%s (%s)]",
                personId, existingOrganizationId, attemptedOrganizationId, reason);
    }
}

