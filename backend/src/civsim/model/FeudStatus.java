package civsim.model;

/**
 * Phase 2C.2 — Lifecycle state of a persistent FeudCase.
 */
public enum FeudStatus {
    PENDING_ATTRIBUTION("Grievance recorded but responsible parties remain undetermined"),
    OPEN("Attribution established; claim exists with no active operations underway"),
    DIPLOMATIC_DISPUTE("Active diplomatic demands, accusations, or arbitration pursued"),
    ESCALATING("Diplomacy failed; tensions rising toward active reprisal"),
    ACTIVE_RETALIATION("Authorized armed operation currently in progress"),
    NEGOTIATION("Parties actively discussing terms of compensation or ceasefire"),
    TRUCE("Formal ceasefire or non-aggression agreement in force"),
    SETTLED("Terms fully satisfied, reconciled, or formally resolved (immutable archive)"),
    DORMANT("No eligible active participants or viable actions remain"),
    EXPIRED("No activity or claimants over historical timescale (immutable archive)"),
    TERMINATED("Ended due to total dissolution of relevant bloodlines/institutions");

    private final String description;

    FeudStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return this != SETTLED && this != EXPIRED && this != TERMINATED && this != DORMANT;
    }

    public boolean isHostile() {
        return this == ESCALATING || this == ACTIVE_RETALIATION;
    }

    public boolean isTerminal() {
        return this == SETTLED || this == EXPIRED || this == TERMINATED;
    }
}

