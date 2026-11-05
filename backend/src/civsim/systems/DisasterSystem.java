package civsim.systems;

import civsim.SimulationConfig;
import civsim.model.Couple;
import civsim.model.Occupation;
import civsim.model.Person;
import civsim.model.Village;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;

/**
 * Handles annual natural disasters (Drought, Plague, Earthquake) and their mitigations.
 */
public class DisasterSystem {
    private final DemographicsSystem demographicsSystem;

    public DisasterSystem(DemographicsSystem demographicsSystem) {
        this.demographicsSystem = demographicsSystem;
    }

    public void processAnnualDisasters(
            List<Village> villages,
            List<Couple> allCouples,
            int currentYear,
            Random random,
            SimulationConfig config,
            BiConsumer<String, Boolean> eventRecorder) {

        for (Village village : villages) {
            if (village.getLivingPopulationCount() == 0) continue;

            if (random.nextDouble() < config.getDisasterProbabilityPerYear()) {
                int pick = random.nextInt(3);
                switch (pick) {
                    case 0 -> {
                        village.applyDrought();
                        if (eventRecorder != null) {
                            eventRecorder.accept(
                                    String.format("Year %d: DROUGHT struck %s! Food reserves dropped to %.1f.",
                                            currentYear, village.getName(), village.getFoodStockpile()),
                                    config.isPrintIndividualEvents()
                            );
                        }
                    }
                    case 1 -> {
                        village.markPlagueOccurred();
                        int soldiers = village.getOccupationCount(Occupation.SOLDIER);
                        int maxVictims = Math.max(1, 3 - (soldiers / 3) - (village.getTechLevel() / 2));
                        List<Person> living = new ArrayList<>();
                        for (Person p : village.getResidents()) {
                            if (p.isAlive()) {
                                living.add(p);
                            }
                        }
                        Collections.shuffle(living, random);
                        int victims = Math.min(maxVictims, living.size() / 4);
                        for (int i = 0; i < victims; i++) {
                            demographicsSystem.killPerson(living.get(i), allCouples, currentYear, "plague", config, eventRecorder);
                        }
                        if (eventRecorder != null) {
                            eventRecorder.accept(
                                    String.format("Year %d: PLAGUE outbreak in %s claimed %d lives.",
                                            currentYear, village.getName(), victims),
                                    config.isPrintIndividualEvents()
                            );
                        }
                    }
                    case 2 -> {
                        village.applyEarthquake();
                        if (eventRecorder != null) {
                            eventRecorder.accept(
                                    String.format("Year %d: EARTHQUAKE damaged housing in %s (Houses remaining: %d).",
                                            currentYear, village.getName(), village.getHousingCount()),
                                    config.isPrintIndividualEvents()
                            );
                        }
                    }
                }
            }
        }
    }
}
