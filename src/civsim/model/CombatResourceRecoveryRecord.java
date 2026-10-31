package civsim.model;

/**
 * Phase 2B.4 -- Record of resource recovery from defeated/retreating expeditions.
 * Guarantees zero creation or destruction of resources:
 * CarriedBefore == SurvivorRetained + ReturnedToDeposit + TransferredToVictor.
 */
public final class CombatResourceRecoveryRecord {
    private final String recoveryId;
    private final String battleId;
    private final String anomalyId;
    private final String defeatedExpeditionId;
    private final double survivorRetainedQuantity;
    private final double returnedToAnomalyDeposit;
    private final double transferredToVictorQuantity;
    private final String beneficiaryExpeditionId;
    private final int year;
    private final int month;

    public CombatResourceRecoveryRecord(
            String recoveryId,
            String battleId,
            String anomalyId,
            String defeatedExpeditionId,
            double survivorRetainedQuantity,
            double returnedToAnomalyDeposit,
            double transferredToVictorQuantity,
            String beneficiaryExpeditionId,
            int year,
            int month) {
        this.recoveryId = recoveryId;
        this.battleId = battleId;
        this.anomalyId = anomalyId;
        this.defeatedExpeditionId = defeatedExpeditionId;
        this.survivorRetainedQuantity = survivorRetainedQuantity;
        this.returnedToAnomalyDeposit = returnedToAnomalyDeposit;
        this.transferredToVictorQuantity = transferredToVictorQuantity;
        this.beneficiaryExpeditionId = beneficiaryCoalitionIdOrExpeditionId(beneficiaryExpeditionId);
        this.year = year;
        this.month = month;
    }

    private static String beneficiaryCoalitionIdOrExpeditionId(String id) {
        return id != null ? id : "NONE";
    }

    public String getRecoveryId() { return recoveryId; }
    public String getBattleId() { return battleId; }
    public String getAnomalyId() { return anomalyId; }
    public String getDefeatedExpeditionId() { return defeatedExpeditionId; }
    public double getSurvivorRetainedQuantity() { return survivorRetainedQuantity; }
    public double getReturnedToAnomalyDeposit() { return returnedToAnomalyDeposit; }
    public double getTransferredToVictorQuantity() { return transferredToVictorQuantity; }
    public String getBeneficiaryExpeditionId() { return beneficiaryExpeditionId; }
    public int getYear() { return year; }
    public int getMonth() { return month; }

    @Override
    public String toString() {
        return String.format("CombatResourceRecovery[%s: Defeated %s -> Retained:%.1f, Deposit:%.1f, Victor(%s):%.1f]",
                recoveryId, defeatedExpeditionId, survivorRetainedQuantity,
                returnedToAnomalyDeposit, beneficiaryExpeditionId, transferredToVictorQuantity);
    }
}
