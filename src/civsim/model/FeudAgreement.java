package civsim.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Phase 2C.2 — Persistent agreement (truce or settlement) between feud participants.
 */
public class FeudAgreement {
    public enum AgreementType {
        TRUCE,
        SETTLEMENT
    }

    private final String agreementId;
    private final String feudId;
    private final AgreementType agreementType;
    private final int startTotalMonths;
    private final int durationMonths; // -1 for permanent
    private final List<String> signatorySideIds;
    private final List<String> signatoryOrgIds;

    private double compensationIronRequired;
    private double compensationIronPaid;
    private String heirloomIdRequired;
    private boolean heirloomReturned;

    private boolean active;
    private boolean violated;
    private String violationReason;

    public FeudAgreement(
            String agreementId,
            String feudId,
            AgreementType agreementType,
            int startTotalMonths,
            int durationMonths,
            List<String> signatorySideIds,
            List<String> signatoryOrgIds,
            double compensationIronRequired,
            String heirloomIdRequired) {
        this.agreementId = agreementId;
        this.feudId = feudId;
        this.agreementType = agreementType != null ? agreementType : AgreementType.TRUCE;
        this.startTotalMonths = startTotalMonths;
        this.durationMonths = durationMonths;
        this.signatorySideIds = new ArrayList<>(signatorySideIds != null ? signatorySideIds : Collections.emptyList());
        this.signatoryOrgIds = new ArrayList<>(signatoryOrgIds != null ? signatoryOrgIds : Collections.emptyList());
        this.compensationIronRequired = Math.max(0.0, compensationIronRequired);
        this.compensationIronPaid = 0.0;
        this.heirloomIdRequired = heirloomIdRequired;
        this.heirloomReturned = (heirloomIdRequired == null);
        this.active = true;
        this.violated = false;
        this.violationReason = null;
    }

    public String getAgreementId() { return agreementId; }
    public String getFeudId() { return feudId; }
    public AgreementType getAgreementType() { return agreementType; }
    public int getStartTotalMonths() { return startTotalMonths; }
    public int getDurationMonths() { return durationMonths; }
    public List<String> getSignatorySideIds() { return Collections.unmodifiableList(signatorySideIds); }
    public List<String> getSignatoryOrgIds() { return Collections.unmodifiableList(signatoryOrgIds); }
    public double getCompensationIronRequired() { return compensationIronRequired; }
    public double getCompensationIronPaid() { return compensationIronPaid; }
    public String getHeirloomIdRequired() { return heirloomIdRequired; }
    public boolean isHeirloomReturned() { return heirloomReturned; }
    public boolean isActive() { return active; }
    public boolean isViolated() { return violated; }
    public String getViolationReason() { return violationReason; }

    public boolean isExpired(int currentTotalMonths) {
        if (durationMonths < 0) return false; // Permanent
        return currentTotalMonths >= (startTotalMonths + durationMonths);
    }

    public boolean isSatisfied() {
        boolean compMet = (compensationIronPaid >= compensationIronRequired - 0.001);
        boolean heirloomMet = (heirloomIdRequired == null || heirloomReturned);
        return compMet && heirloomMet;
    }

    public void recordPayment(double amount) {
        this.compensationIronPaid += Math.max(0.0, amount);
    }

    public void recordHeirloomReturned() {
        this.heirloomReturned = true;
    }

    public void markViolated(String reason) {
        this.violated = true;
        this.violationReason = reason;
        this.active = false;
    }

    public void terminate() {
        this.active = false;
    }
}

