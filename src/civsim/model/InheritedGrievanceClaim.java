package civsim.model;

/**
 * Phase 2C.2 — Tracks an inherited grievance claim across generations.
 * Separates historical awareness, social/legal right to claim, and personal acceptance.
 */
public class InheritedGrievanceClaim {
    private final String claimId;
    private final String feudId;
    private final String originalGrievanceId;
    private final String claimantPersonId;
    private final String claimantOrgId;
    private final int generation; // 0 = original victim/claimant, 1 = children, 2 = grandchildren, etc.
    private double claimStrength; // bounded [0, 100]
    private ClaimAcceptanceStatus status;
    private boolean awareOfGrievance;
    private int statusChangedYear;
    private int statusChangedMonth;

    public enum ClaimAcceptanceStatus {
        PENDING_ADULTHOOD,
        ACCEPTED,
        REJECTED,
        ABANDONED
    }

    public InheritedGrievanceClaim(
            String claimId,
            String feudId,
            String originalGrievanceId,
            String claimantPersonId,
            String claimantOrgId,
            int generation,
            double claimStrength,
            ClaimAcceptanceStatus status,
            boolean awareOfGrievance,
            int currentYear,
            int currentMonth) {
        this.claimId = claimId;
        this.feudId = feudId;
        this.originalGrievanceId = originalGrievanceId;
        this.claimantPersonId = claimantPersonId;
        this.claimantOrgId = claimantOrgId;
        this.generation = Math.max(0, generation);
        this.claimStrength = Math.max(0.0, Math.min(100.0, claimStrength));
        this.status = status != null ? status : ClaimAcceptanceStatus.ACCEPTED;
        this.awareOfGrievance = awareOfGrievance;
        this.statusChangedYear = currentYear;
        this.statusChangedMonth = currentMonth;
    }

    public String getClaimId() { return claimId; }
    public String getFeudId() { return feudId; }
    public String getOriginalGrievanceId() { return originalGrievanceId; }
    public String getClaimantPersonId() { return claimantPersonId; }
    public String getClaimantOrgId() { return claimantOrgId; }
    public int getGeneration() { return generation; }
    public double getClaimStrength() { return claimStrength; }
    public ClaimAcceptanceStatus getStatus() { return status; }
    public boolean isAwareOfGrievance() { return awareOfGrievance; }
    public int getStatusChangedYear() { return statusChangedYear; }
    public int getStatusChangedMonth() { return statusChangedMonth; }

    public void setClaimStrength(double strength) {
        this.claimStrength = Math.max(0.0, Math.min(100.0, strength));
    }

    public void setAwareOfGrievance(boolean aware) {
        this.awareOfGrievance = aware;
    }

    public void accept(int year, int month) {
        this.status = ClaimAcceptanceStatus.ACCEPTED;
        this.statusChangedYear = year;
        this.statusChangedMonth = month;
    }

    public void reject(int year, int month) {
        this.status = ClaimAcceptanceStatus.REJECTED;
        this.statusChangedYear = year;
        this.statusChangedMonth = month;
    }

    public void abandon(int year, int month) {
        this.status = ClaimAcceptanceStatus.ABANDONED;
        this.statusChangedYear = year;
        this.statusChangedMonth = month;
    }

    public boolean isActiveClaimant() {
        return awareOfGrievance && status == ClaimAcceptanceStatus.ACCEPTED && claimStrength > 5.0;
    }
}

