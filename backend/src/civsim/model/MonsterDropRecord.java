package civsim.model;

/**
 * Phase 2B.5 — Immutable record of monster cores and materials dropped upon monster fatality.
 * Drops remain at the encounter coordinates until physically recovered and delivered.
 */
public final class MonsterDropRecord {

    private final String dropEventId;
    private final String monsterId;
    private final String speciesId;
    private final MonsterThreatRank threatRank;
    private final MonsterMaterialType materialType;
    private final ResourceGrade coreGrade;
    private final double quantity;
    private final double posX;
    private final double posY;
    private final int year;
    private final int month;

    private boolean recovered;
    private String recoveredByFactionId;
    private MonsterDropStatus status;

    public MonsterDropRecord(
            String dropEventId,
            String monsterId,
            String speciesId,
            MonsterThreatRank threatRank,
            MonsterMaterialType materialType,
            ResourceGrade coreGrade,
            double quantity,
            double posX,
            double posY,
            int year,
            int month) {
        this.dropEventId = dropEventId;
        this.monsterId = monsterId;
        this.speciesId = speciesId;
        this.threatRank = threatRank != null ? threatRank : MonsterThreatRank.STANDARD;
        this.materialType = materialType != null ? materialType : MonsterMaterialType.BEAST_HIDE;
        this.coreGrade = (materialType == MonsterMaterialType.MONSTER_CORE) ? (coreGrade != null ? coreGrade : ResourceGrade.COMMON) : null;
        this.quantity = Math.max(1.0, quantity);
        this.posX = posX;
        this.posY = posY;
        this.year = year;
        this.month = month;
        this.status = MonsterDropStatus.RECOVERABLE;
        this.recovered = false;
        this.recoveredByFactionId = null;
    }

    public String getDropEventId() { return dropEventId; }
    public String getMonsterId() { return monsterId; }
    public String getSpeciesId() { return speciesId; }
    public MonsterThreatRank getThreatRank() { return threatRank; }
    public MonsterMaterialType getMaterialType() { return materialType; }
    public ResourceGrade getCoreGrade() { return coreGrade; }
    public double getQuantity() { return quantity; }
    public double getPosX() { return posX; }
    public double getPosY() { return posY; }
    public int getYear() { return year; }
    public int getMonth() { return month; }

    public MonsterDropStatus getStatus() { return status; }
    public void setStatus(MonsterDropStatus status) {
        this.status = (status != null) ? status : MonsterDropStatus.RECOVERABLE;
        if (this.status == MonsterDropStatus.DELIVERED || this.status == MonsterDropStatus.RECOVERED) {
            this.recovered = true;
        }
    }

    public boolean isRecovered() { return recovered || status == MonsterDropStatus.DELIVERED || status == MonsterDropStatus.RECOVERED; }
    public void markRecovered(String factionId) {
        this.recovered = true;
        this.status = MonsterDropStatus.DELIVERED;
        this.recoveredByFactionId = factionId;
    }
    public String getRecoveredByFactionId() { return recoveredByFactionId; }

    public boolean isCore() {
        return materialType == MonsterMaterialType.MONSTER_CORE;
    }

    @Override
    public String toString() {
        return String.format("MonsterDrop[%s from %s (%s): %.0f %s%s at (%.1f, %.1f) Rec:%b]",
                dropEventId, speciesId, threatRank.getDisplayName(), quantity, materialType.getDisplayName(),
                (coreGrade != null ? " (" + coreGrade.getDisplayName() + ")" : ""), posX, posY, recovered);
    }
}

