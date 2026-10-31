package civsim.model;

/**
 * Phase 2B.4 -- Immutable record of personal carried wealth looted upon combat fatality.
 * Satisfies the strict estate conservation invariant:
 * InitialPersonalWealth == DebtPaid + LootedAmount + InheritedOrTreasuryEscheatedAmount.
 */
public final class BattleLootRecord {
    private final String lootRecordId;
    private final String battleId;
    private final String contestId;
    private final String anomalyId;
    private final String victimPersonId;
    private final String killerPersonId;
    private final String beneficiaryCoalitionSideId;
    private final double lootedIron;
    private final double victimInitialWealth;
    private final double debtPaid;
    private final double inheritedOrEscheated;
    private final int year;
    private final int month;

    public BattleLootRecord(
            String lootRecordId,
            String battleId,
            String contestId,
            String anomalyId,
            String victimPersonId,
            String killerPersonId,
            String beneficiaryCoalitionSideId,
            double lootedIron,
            double victimInitialWealth,
            double debtPaid,
            double inheritedOrEscheated,
            int year,
            int month) {
        this.lootRecordId = lootRecordId;
        this.battleId = battleId;
        this.contestId = contestId;
        this.anomalyId = anomalyId;
        this.victimPersonId = victimPersonId;
        this.killerPersonId = killerPersonId;
        this.beneficiaryCoalitionSideId = beneficiaryCoalitionSideId;
        this.lootedIron = lootedIron;
        this.victimInitialWealth = victimInitialWealth;
        this.debtPaid = debtPaid;
        this.inheritedOrEscheated = inheritedOrEscheated;
        this.year = year;
        this.month = month;
    }

    public String getLootRecordId() { return lootRecordId; }
    public String getBattleId() { return battleId; }
    public String getContestId() { return contestId; }
    public String getAnomalyId() { return anomalyId; }
    public String getVictimPersonId() { return victimPersonId; }
    public String getKillerPersonId() { return killerPersonId; }
    public String getBeneficiaryCoalitionSideId() { return beneficiaryCoalitionSideId; }
    public double getLootedIron() { return lootedIron; }
    public double getVictimInitialWealth() { return victimInitialWealth; }
    public double getDebtPaid() { return debtPaid; }
    public double getInheritedOrEscheated() { return inheritedOrEscheated; }
    public int getYear() { return year; }
    public int getMonth() { return month; }

    @Override
    public String toString() {
        return String.format("BattleLootRecord[%s: %.1f Iron looted from P%s by %s in %s (Init:%.1f, Debt:%.1f, Inherited:%.1f)]",
                lootRecordId, lootedIron, victimPersonId, beneficiaryCoalitionSideId, battleId,
                victimInitialWealth, debtPaid, inheritedOrEscheated);
    }
}
