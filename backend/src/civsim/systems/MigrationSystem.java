package civsim.systems;

import civsim.SimulationConfig;
import civsim.model.DiplomaticStatus;
import civsim.model.Person;
import civsim.model.Village;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;

/**
 * Handles inter-settlement population mobility using a dynamic attractiveness engine:
 * famine flight, overcrowding relief, and economic wanderlust.
 */
public class MigrationSystem {
    private final DemographicsSystem demographicsSystem;

    public MigrationSystem(DemographicsSystem demographicsSystem) {
        this.demographicsSystem = demographicsSystem;
    }

    public void processAnnualMigration(
            List<Village> villages,
            int currentYear,
            Random random,
            SimulationConfig config,
            BiConsumer<String, Boolean> eventRecorder) {

        if (villages.size() < 2) return;

        Map<Village, Double> attractiveness = new LinkedHashMap<>();
        for (Village v : villages) {
            if (v.getLivingPopulationCount() > 0) {
                attractiveness.put(v, v.computeAttractivenessScore(
                        config.getFoodPerPersonPerMonth(),
                        config.getHousingCapacityPerBuilding()
                ));
            }
        }

        if (attractiveness.isEmpty()) return;

        for (Village origin : villages) {
            int pop = origin.getLivingPopulationCount();
            if (pop < 12) continue; // Don't depopulate nascent settlements below survival threshold

            int capacity = origin.getTotalHousingCapacity(config.getHousingCapacityPerBuilding());
            int homeless = Math.max(0, pop - capacity);
            boolean isFamine = origin.isInFamine();
            double originScore = attractiveness.getOrDefault(origin, 50.0);

            // Determine percentage-based migration volume
            int toMigrate = 0;
            String reason;
            if (isFamine) {
                // Famine flight: 5% to 10% of population flees to surviving settlements
                toMigrate = Math.max(2, (int) Math.round(pop * 0.06));
                reason = "famine escape";
            } else if (homeless > 0) {
                // Overcrowding relief: 12% of homeless population, up to 3.5% of total settlement
                toMigrate = Math.max(2, (int) Math.round(homeless * 0.12));
                toMigrate = Math.min(toMigrate, Math.max(4, (int) Math.round(pop * 0.035)));
                reason = "seeking housing";
            } else if (pop >= 80) {
                // Cross-city economic migration / wanderlust: 1.0% to 1.5% of population
                toMigrate = Math.max(1, (int) Math.round(pop * 0.012));
                reason = "cross-settlement economic opportunity";
            } else {
                continue;
            }

            if (toMigrate <= 0) continue;

            // Find all viable destination targets (not at war, not in famine, with food reserves)
            List<Village> viableTargets = new ArrayList<>();
            for (Village target : villages) {
                if (target == origin || target.getLivingPopulationCount() == 0) continue;
                if (origin.getRelationWith(target.getId()) == DiplomaticStatus.AT_WAR) continue;
                if (target.isInFamine()) continue;

                double targetScore = attractiveness.getOrDefault(target, 50.0);
                if (targetScore > originScore - 12.0 || homeless > 0 || isFamine) {
                    viableTargets.add(target);
                }
            }

            if (viableTargets.isEmpty()) continue;

            viableTargets.sort((a, b) -> Double.compare(
                    attractiveness.getOrDefault(b, 0.0),
                    attractiveness.getOrDefault(a, 0.0)
            ));

            List<Person> candidates = origin.getEligibleUnmarriedAdults(config.getMinMarriageAgeExclusive());
            if (candidates.size() < toMigrate) {
                for (Person p : origin.getResidents()) {
                    if (p.isAlive() && p.getAge() >= 19 && !candidates.contains(p) && p != origin.getLeader()) {
                        candidates.add(p);
                    }
                }
            }
            Collections.shuffle(candidates, random);

            int actualMoved = 0;
            for (Person migrant : candidates) {
                if (actualMoved >= toMigrate) break;

                Village destination = viableTargets.get(actualMoved % viableTargets.size());
                demographicsSystem.relocatePerson(migrant, origin, destination, currentYear, reason, config, eventRecorder);
                actualMoved++;
            }
        }
    }
}
