package civsim.model;

import java.util.*;

/**
 * Phase 2B.4 / Phase 2B.3 -- Physical Expedition dispatched to explore, claim, or extract a World Anomaly.
 * Roster references person IDs as strings to prevent circular strong references and heap-pruning issues.
 */
public class Expedition {
    private final String expeditionId;
    private final String sponsorId;
    private final String sponsorType;
    private final String targetAnomalyId;
    private final double originX;
    private final double originY;
    private double targetX;
    private double targetY;
    private double currentX;
    private double currentY;
    private ExpeditionStatus status;
    private ClaimObjective claimObjective;
    private final List<String> rosterPersonIds;
    private double carriedResources;
    private double maxCarryCapacity;
    private double supplies;
    private long arrivalTotalMonth;
    private long returnArrivalMonth;
    private int oneWayTravelMonths;
    private final int departureYear;
    private final int departureMonth;
    private final long departureTotalMonths;

    public Expedition(
            String expeditionId,
            String sponsorId,
            String sponsorType,
            String targetAnomalyId,
            double startX,
            double startY,
            ClaimObjective claimObjective,
            List<String> rosterPersonIds,
            double maxCarryCapacity,
            double supplies,
            int departureYear,
            int departureMonth) {
        this.expeditionId = expeditionId;
        this.sponsorId = sponsorId;
        this.sponsorType = sponsorType != null ? sponsorType : "ORGANIZATION";
        this.targetAnomalyId = targetAnomalyId;
        this.originX = startX;
        this.originY = startY;
        this.targetX = startX;
        this.targetY = startY;
        this.currentX = startX;
        this.currentY = startY;
        this.status = ExpeditionStatus.EN_ROUTE_TO_ANOMALY;
        this.claimObjective = claimObjective != null ? claimObjective : ClaimObjective.EXCLUSIVE_CLAIM;
        this.rosterPersonIds = new ArrayList<>(rosterPersonIds != null ? rosterPersonIds : Collections.emptyList());
        this.carriedResources = 0.0;
        this.maxCarryCapacity = Math.max(0.0, maxCarryCapacity);
        this.supplies = Math.max(0.0, supplies);
        this.arrivalTotalMonth = 0L;
        this.returnArrivalMonth = 0L;
        this.oneWayTravelMonths = 1;
        this.departureYear = departureYear;
        this.departureMonth = departureMonth;
        this.departureTotalMonths = (long) departureYear * 12 + departureMonth;
    }

    public String getExpeditionId() { return expeditionId; }
    public String getSponsorId() { return sponsorId; }
    public String getOriginVillageId() { return sponsorId; }
    public String getSponsorType() { return sponsorType; }
    public String getTargetAnomalyId() { return targetAnomalyId; }
    public double getOriginX() { return originX; }
    public double getOriginY() { return originY; }
    public double getTargetX() { return targetX; }
    public double getTargetY() { return targetY; }
    public void setTargetPosition(double x, double y) { this.targetX = x; this.targetY = y; }
    public double getCurrentX() { return currentX; }
    public double getCurrentY() { return currentY; }
    public void setPosition(double x, double y) { this.currentX = x; this.currentY = y; }

    public ExpeditionStatus getStatus() { return status; }
    public void setStatus(ExpeditionStatus status) { this.status = status; }

    public ClaimObjective getClaimObjective() { return claimObjective; }
    public void setClaimObjective(ClaimObjective claimObjective) { this.claimObjective = claimObjective; }

    public List<String> getRosterPersonIds() { return Collections.unmodifiableList(rosterPersonIds); }
    public List<String> getPersonnel() { return Collections.unmodifiableList(rosterPersonIds); }

    public double getCarriedResources() { return carriedResources; }
    public double getCarriedCargo() { return carriedResources; }
    public void setCarriedResources(double amount) { this.carriedResources = Math.max(0.0, amount); }
    public void addCarriedResources(double amount) { this.carriedResources += Math.max(0.0, amount); }
    public void deductCarriedResources(double amount) { this.carriedResources = Math.max(0.0, this.carriedResources - amount); }

    public double getMaxCarryCapacity() { return maxCarryCapacity; }
    public void setMaxCarryCapacity(double cap) { this.maxCarryCapacity = cap; }

    public double getSupplies() { return supplies; }
    public void consumeSupplies(double amount) { this.supplies = Math.max(0.0, this.supplies - amount); }

    public long getArrivalTotalMonth() { return arrivalTotalMonth; }
    public void setArrivalTotalMonth(long arrivalMonth) { this.arrivalTotalMonth = arrivalMonth; }

    public long getReturnArrivalMonth() { return returnArrivalMonth; }
    public void setReturnArrivalMonth(long returnMonth) { this.returnArrivalMonth = returnMonth; }

    public int getOneWayTravelMonths() { return oneWayTravelMonths; }
    public void setOneWayTravelMonths(int months) { this.oneWayTravelMonths = Math.max(1, months); }

    public int getDepartureYear() { return departureYear; }
    public int getDepartureMonth() { return departureMonth; }
    public long getDepartureTotalMonths() { return departureTotalMonths; }

    public boolean isAtCoordinates(double x, double y, double tolerance) {
        return Math.hypot(this.currentX - x, this.currentY - y) <= tolerance;
    }

    public List<Person> getLivingPresentPersonnel(Map<String, Person> peopleMap) {
        List<Person> present = new ArrayList<>();
        if (peopleMap == null) return present;
        for (String pId : rosterPersonIds) {
            Person p = peopleMap.get(pId);
            if (p != null && p.isAlive()) {
                present.add(p);
            }
        }
        return present;
    }

    public int getLivingMemberCount(Map<String, Person> peopleMap) {
        return getLivingPresentPersonnel(peopleMap).size();
    }

    public void removeParticipant(String personId) {
        if (personId != null) {
            rosterPersonIds.remove(personId);
        }
    }

    public void startReturnJourney(long currentTotalMonths) {
        this.status = ExpeditionStatus.RETURNING;
        this.returnArrivalMonth = currentTotalMonths + this.oneWayTravelMonths;
    }

    @Override
    public String toString() {
        return String.format("Expedition[%s (%s): %s -> Anomaly %s | Status:%s Carried:%.1f/%.1f]",
                expeditionId, sponsorType, sponsorId, targetAnomalyId, status.getDisplayName(), carriedResources, maxCarryCapacity);
    }
}
