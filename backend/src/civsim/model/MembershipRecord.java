package civsim.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Phase 2B.2 — Authoritative record of an individual's membership in an Organization.
 * Stores only personId (String) to guarantee heap-pruning safety upon death.
 */
public class MembershipRecord {

    private final String personId;
    private final String organizationId;
    private final int joinedYear;
    private final int joinedMonth;
    private final MembershipOrigin origin;

    private OrganizationRole role;
    private MembershipStatus status;
    private double commitmentScore;

    private Integer departureYear;
    private Integer departureMonth;
    private String departureReason;

    private final List<String> milestones;

    public MembershipRecord(
            String personId,
            String organizationId,
            int joinedYear,
            int joinedMonth,
            OrganizationRole role,
            MembershipStatus status,
            MembershipOrigin origin,
            double commitmentScore) {
        this.personId = personId;
        this.organizationId = organizationId;
        this.joinedYear = joinedYear;
        this.joinedMonth = joinedMonth;
        this.role = role != null ? role : OrganizationRole.MEMBER;
        this.status = status != null ? status : MembershipStatus.ACTIVE;
        this.origin = origin != null ? origin : MembershipOrigin.OPEN_JOIN;
        this.commitmentScore = Math.max(0.0, Math.min(100.0, commitmentScore));
        this.departureYear = null;
        this.departureMonth = null;
        this.departureReason = null;
        this.milestones = new ArrayList<>();
        this.milestones.add(String.format("Y%d.M%d: Joined as %s via %s (Loyalty: %.1f)",
                joinedYear, joinedMonth, this.role.getDisplayName(), this.origin.getDisplayName(), this.commitmentScore));
    }

    public MembershipRecord(String personId, String organizationId, int year, int month,
                            OrganizationRole role, MembershipOrigin origin) {
        this(personId, organizationId, year, month, role, MembershipStatus.ACTIVE, origin, 60.0);
    }

    public String getPersonId() { return personId; }
    public String getOrganizationId() { return organizationId; }
    public int getJoinedYear() { return joinedYear; }
    public int getJoinedMonth() { return joinedMonth; }
    public MembershipOrigin getOrigin() { return origin; }
    public OrganizationRole getRole() { return role; }
    public MembershipStatus getStatus() { return status; }
    public double getCommitmentScore() { return commitmentScore; }
    public Integer getDepartureYear() { return departureYear; }
    public Integer getDepartureMonth() { return departureMonth; }
    public String getDepartureReason() { return departureReason; }
    public List<String> getMilestones() { return Collections.unmodifiableList(milestones); }

    public boolean isActive() {
        return status == MembershipStatus.ACTIVE;
    }

    public boolean isInvited() {
        return status == MembershipStatus.INVITED;
    }

    public boolean isSuspended() {
        return status == MembershipStatus.SUSPENDED;
    }

    public boolean isTerminated() {
        return status == MembershipStatus.LEFT || status == MembershipStatus.EXPELLED;
    }

    public void setRole(OrganizationRole newRole, int year, int month) {
        if (newRole == null || this.role == newRole) return;
        OrganizationRole old = this.role;
        this.role = newRole;
        addMilestone(String.format("Y%d.M%d: Role changed from %s to %s", year, month, old.getDisplayName(), newRole.getDisplayName()));
    }

    public void activateFromInvitation(OrganizationRole initialRole, int year, int month) {
        this.status = MembershipStatus.ACTIVE;
        if (initialRole != null) {
            this.role = initialRole;
        }
        addMilestone(String.format("Y%d.M%d: Accepted invitation -> ACTIVE as %s", year, month, this.role.getDisplayName()));
    }

    public void suspend(int year, int month, String reason) {
        this.status = MembershipStatus.SUSPENDED;
        addMilestone(String.format("Y%d.M%d: Suspended (%s)", year, month, reason != null ? reason : "unspecified"));
    }

    public void reinstate(int year, int month) {
        this.status = MembershipStatus.ACTIVE;
        addMilestone(String.format("Y%d.M%d: Reinstated to ACTIVE standing", year, month));
    }

    public void terminate(MembershipStatus finalStatus, int year, int month, String reason) {
        if (finalStatus != MembershipStatus.LEFT && finalStatus != MembershipStatus.EXPELLED) {
            finalStatus = MembershipStatus.LEFT;
        }
        this.status = finalStatus;
        this.departureYear = year;
        this.departureMonth = month;
        this.departureReason = reason != null ? reason : "unknown";
        addMilestone(String.format("Y%d.M%d: Terminated as %s (%s)", year, month, finalStatus.getDisplayName(), this.departureReason));
    }

    public void markEndedByDeath(int year, int month, String cause) {
        this.status = MembershipStatus.LEFT;
        this.departureYear = year;
        this.departureMonth = month;
        this.departureReason = "Deceased: " + (cause != null ? cause : "natural causes");
        addMilestone(String.format("Y%d.M%d: Membership ended by death (%s)", year, month, this.departureReason));
    }

    public void adjustCommitment(double delta) {
        this.commitmentScore = Math.max(0.0, Math.min(100.0, this.commitmentScore + delta));
    }

    public void setCommitmentScore(double score) {
        this.commitmentScore = Math.max(0.0, Math.min(100.0, score));
    }

    private void addMilestone(String milestone) {
        if (milestones.size() >= 20) {
            milestones.remove(1); // Keep first joining milestone, prune intermediate
        }
        milestones.add(milestone);
    }

    @Override
    public String toString() {
        return String.format("MembershipRecord[%s in %s: %s (%s, Loyalty: %.1f)]",
                personId, organizationId, role.getDisplayName(), status.getDisplayName(), commitmentScore);
    }
}

