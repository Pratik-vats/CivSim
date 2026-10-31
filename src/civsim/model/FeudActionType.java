package civsim.model;

/**
 * Phase 2C.2 — Concrete action types undertaken within a FeudCase.
 */
public enum FeudActionType {
    // Diplomatic Actions
    PUBLIC_ACCUSATION(false, true, false, "Public condemnation or formal accusation"),
    COMPENSATION_DEMAND(false, true, false, "Formal demand for restitution or financial blood-money"),
    HEIRLOOM_RESTITUTION_DEMAND(false, true, false, "Formal demand for return of captured clan heirloom"),
    ALLIANCE_WITHDRAWAL(false, true, false, "Formal withdrawal from alliance or mutual pact"),
    TRADE_SUSPENSION(false, true, false, "Sanctions and trade embargo against opposing party"),
    ARBITRATION_REQUEST(false, true, false, "Appeal to a neutral elder, lord, or third-party clan for arbitration"),
    DIPLOMATIC_ULTIMATUM(false, true, false, "Ultimatum demanding satisfaction under threat of hostilities"),

    // Personal / Family Actions
    FORMAL_CHALLENGE(true, false, false, "Formal honor challenge issued directly to responsible individual"),
    LAWFUL_PURSUIT(true, false, false, "Lawful apprehension or tracking of confirmed fugitive culprit"),
    PERSONAL_ARTIFACT_RECOVERY(true, false, false, "Personal retrieval attempt for stolen personal equipment"),

    // Organizational / Military Actions
    LIMITED_ARMED_EXPEDITION(true, false, false, "Targeted expedition aimed at enforcing justice or reprisal"),
    DEFENSIVE_MOBILIZATION(false, false, false, "Heightened defensive posture and garrison strengthening"),
    INTERCEPTION_EXPEDITION(true, false, false, "Dispatching fighters to intercept enemy transit or caravan"),
    HEIRLOOM_RECOVERY_OPERATION(true, false, false, "Covert or armed expedition to reacquire a stolen clan heirloom"),
    ORGANIZED_ARMED_RETALIATION(true, false, false, "Major military raid or punitive campaign against responsible clan"),
    DEFENSIVE_ALLIANCE(false, true, false, "Forming a mutual defense coalition against an aggressive rival"),

    // Resolution & Peace Actions
    FORMAL_APOLOGY(false, false, true, "Formal admission of wrongdoing and public apology"),
    COMPENSATION_PAYMENT(false, false, true, "Transfer of currency or goods in settlement of grievance"),
    HEIRLOOM_RETURN(false, false, true, "Physical return of stolen clan heirloom to rightful owners"),
    TRUCE_OFFER(false, true, true, "Proposal of a binding ceasefire"),
    TRUCE_ACCEPTED(false, true, true, "Acceptance and ratification of a temporary ceasefire"),
    PEACE_TREATY_SIGNED(false, true, true, "Ratification of a permanent peace treaty settling the feud"),
    VOLUNTARY_RECONCILIATION(false, false, true, "Voluntary forgiveness and reconciliation by claimant"),
    CLAIM_ABANDONED(false, false, true, "Claimant formally drops claim without compensation");

    private final boolean hostile;
    private final boolean diplomatic;
    private final boolean resolution;
    private final String description;

    FeudActionType(boolean hostile, boolean diplomatic, boolean resolution, String description) {
        this.hostile = hostile;
        this.diplomatic = diplomatic;
        this.resolution = resolution;
        this.description = description;
    }

    public boolean isHostile() { return hostile; }
    public boolean isDiplomatic() { return diplomatic; }
    public boolean isResolution() { return resolution; }
    public String getDescription() { return description; }
}

