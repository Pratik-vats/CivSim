package civsim.model;

/**
 * Phase 2C.1 -- Persistent workshop/forge facility within a settlement or clan.
 */
public class CraftingFacility {

    private final String facilityId;
    private final String settlementId;
    private String ownerOrganizationId;
    private FacilityGrade grade;
    private int concurrentCapacity;
    private int activeJobsCount;

    public CraftingFacility(
            String facilityId,
            String settlementId,
            String ownerOrganizationId,
            FacilityGrade grade,
            int concurrentCapacity) {
        this.facilityId = facilityId;
        this.settlementId = settlementId;
        this.ownerOrganizationId = ownerOrganizationId;
        this.grade = grade != null ? grade : FacilityGrade.BASIC_FORGE;
        this.concurrentCapacity = Math.max(1, concurrentCapacity);
        this.activeJobsCount = 0;
    }

    public String getFacilityId() { return facilityId; }
    public String getSettlementId() { return settlementId; }
    public String getOwnerOrganizationId() { return ownerOrganizationId; }
    public FacilityGrade getGrade() { return grade; }
    public int getConcurrentCapacity() { return concurrentCapacity; }
    public int getActiveJobsCount() { return activeJobsCount; }
    public void setActiveJobsCount(int count) { this.activeJobsCount = Math.max(0, count); }

    public void setOwnerOrganizationId(String ownerOrganizationId) {
        this.ownerOrganizationId = ownerOrganizationId;
    }

    public void upgradeGrade(FacilityGrade newGrade) {
        if (newGrade != null && newGrade.ordinal() > this.grade.ordinal()) {
            this.grade = newGrade;
        }
    }

    public boolean hasAvailableCapacity() {
        return activeJobsCount < concurrentCapacity;
    }

    public boolean allocateJob() {
        if (hasAvailableCapacity()) {
            activeJobsCount++;
            return true;
        }
        return false;
    }

    public void releaseJob() {
        if (activeJobsCount > 0) {
            activeJobsCount--;
        }
    }
}

