package civsim.spatial;

import civsim.model.DiplomaticStatus;
import civsim.model.Village;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Calculates and manages a 2D spatial trade caravan route between two settlements,
 * dynamically discovering intermediate settlements as stopover waypoints to optimize
 * travel safety, reduce wilderness freight costs, and pay transit tolls.
 */
public class CaravanRoute {
    private final Village origin;
    private final Village destination;
    private final List<Village> waypoints;
    private final double directDistance;
    private final double totalDistance;
    private final int travelTimeMonths;

    private CaravanRoute(Village origin, Village destination, List<Village> waypoints, double directDistance, double totalDistance) {
        this.origin = origin;
        this.destination = destination;
        this.waypoints = Collections.unmodifiableList(waypoints);
        this.directDistance = directDistance;
        this.totalDistance = totalDistance;
        // Caravans travel approx 25 km per month
        this.travelTimeMonths = Math.max(1, (int) Math.ceil(totalDistance / 25.0));
    }

    /**
     * Computes the optimal caravan route between origin and destination.
     * If one or more active settlements lie within the travel corridor between the two points,
     * the caravan adjusts its route to include them as intermediate stopovers.
     */
    public static CaravanRoute planRoute(Village origin, Village destination, List<Village> allVillages) {
        double directDist = origin.distanceTo(destination);
        if (directDist <= 0.001) {
            return new CaravanRoute(origin, destination, Collections.emptyList(), 0.0, 0.0);
        }

        List<WaypointCandidate> candidates = new ArrayList<>();

        double dx = destination.getX() - origin.getX();
        double dy = destination.getY() - origin.getY();

        for (Village v : allVillages) {
            if (v == origin || v == destination || v.getLivingPopulationCount() == 0) {
                continue;
            }

            // Exclude hostile settlements from acting as friendly trading waypoints
            if (origin.getRelationWith(v.getId()) == DiplomaticStatus.AT_WAR
                    || destination.getRelationWith(v.getId()) == DiplomaticStatus.AT_WAR) {
                continue;
            }

            double dOrigin = origin.distanceTo(v);
            double dDest = v.distanceTo(destination);

            // Waypoint must be strictly closer than the total direct distance
            if (dOrigin >= directDist || dDest >= directDist) {
                continue;
            }

            // Scalar projection along the direct origin-to-destination vector (0.0 to 1.0)
            double vx = v.getX() - origin.getX();
            double vy = v.getY() - origin.getY();
            double projection = (vx * dx + vy * dy) / (directDist * directDist);

            if (projection <= 0.08 || projection >= 0.92) {
                continue; // Too close to origin or destination endpoints
            }

            // Perpendicular distance from line segment
            double perpDist = Math.abs(dx * vy - dy * vx) / directDist;

            // Must not deviate excessively from corridor (max 18 km lateral deviation)
            // and detour distance must not exceed 28% extra distance
            if (perpDist <= 18.0 && (dOrigin + dDest) <= directDist * 1.28) {
                candidates.add(new WaypointCandidate(v, projection, dOrigin));
            }
        }

        // Sort waypoints sequentially by projection along the route
        candidates.sort(Comparator.comparingDouble(c -> c.projection));

        List<Village> selectedWaypoints = new ArrayList<>();
        double currentTotal = 0.0;
        Village current = origin;

        for (WaypointCandidate c : candidates) {
            selectedWaypoints.add(c.village);
            currentTotal += current.distanceTo(c.village);
            current = c.village;
        }
        currentTotal += current.distanceTo(destination);

        // If no waypoints selected, travel direct across wilderness
        if (selectedWaypoints.isEmpty()) {
            currentTotal = directDist;
        }

        return new CaravanRoute(origin, destination, selectedWaypoints, directDist, currentTotal);
    }

    /**
     * Calculates the transport cost in gold for transporting a given quantity of goods.
     * Routes with established intermediate waypoints receive a 35% discount on freight fees
     * due to roadside inns, paved roads, and security.
     */
    public double calculateTransportCost(double tradeQuantity, double unitValue) {
        double baseRatePerKmPerUnit = 0.0035; // Freight transport cost per unit per km
        double routeModifier = hasWaypoints() ? 0.65 : 1.00; // Paved waypoint corridor discount
        double freight = tradeQuantity * totalDistance * baseRatePerKmPerUnit * routeModifier;

        // Minimum transport fee of 0.5 gold
        return Math.max(0.5, freight);
    }

    /**
     * Transit tariff paid to each waypoint settlement along the adjusted route.
     */
    public double getWaypointTariffPerStop(double totalCargoValue) {
        return Math.max(0.4, totalCargoValue * 0.03);
    }

    public boolean hasWaypoints() {
        return !waypoints.isEmpty();
    }

    public List<Village> getWaypoints() {
        return waypoints;
    }

    public Village getOrigin() {
        return origin;
    }

    public Village getDestination() {
        return destination;
    }

    public double getDirectDistance() {
        return directDistance;
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public int getTravelTimeMonths() {
        return travelTimeMonths;
    }

    public String formatRouteSummary() {
        if (!hasWaypoints()) {
            return String.format("%s -> %s (Direct %.1f km)", origin.getName(), destination.getName(), directDistance);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(origin.getName());
        for (Village wp : waypoints) {
            sb.append(" -> ").append(wp.getName()).append(" [Hub]");
        }
        sb.append(" -> ").append(destination.getName());
        sb.append(String.format(" (Adjusted %.1f km)", totalDistance));
        return sb.toString();
    }

    private static class WaypointCandidate {
        final Village village;
        final double projection;
        final double distFromOrigin;

        WaypointCandidate(Village village, double projection, double distFromOrigin) {
            this.village = village;
            this.projection = projection;
            this.distFromOrigin = distFromOrigin;
        }
    }
}
