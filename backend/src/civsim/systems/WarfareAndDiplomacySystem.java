package civsim.systems;

import civsim.SimulationConfig;
import civsim.model.Couple;
import civsim.model.DiplomaticStatus;
import civsim.model.Occupation;
import civsim.model.Person;
import civsim.model.Village;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;

/**
 * Handles inter-settlement diplomacy (Mutual Defense Alliances, War declarations, Peace treaties)
 * and military raids with 2D spatial logistics, march supply trains, fatigue penalties,
 * forward staging bases, and combat resolution.
 */
public class WarfareAndDiplomacySystem {
    private final DemographicsSystem demographicsSystem;

    public WarfareAndDiplomacySystem(DemographicsSystem demographicsSystem) {
        this.demographicsSystem = demographicsSystem;
    }

    public void processAnnualDiplomacyAndRaids(
            List<Village> villages,
            List<Couple> allCouples,
            int currentYear,
            Random random,
            SimulationConfig config,
            BiConsumer<String, Boolean> eventRecorder) {

        if (villages.size() < 2) return;

        for (int i = 0; i < villages.size(); i++) {
            Village aggressor = villages.get(i);
            if (aggressor.getLivingPopulationCount() < 12) continue;

            for (int j = 0; j < villages.size(); j++) {
                if (i == j) continue;
                Village target = villages.get(j);
                if (target.getLivingPopulationCount() == 0) continue;

                DiplomaticStatus status = aggressor.getRelationWith(target.getId());

                // 1. Evolve diplomatic status
                if (status == DiplomaticStatus.NEUTRAL) {
                    double roll = random.nextDouble();
                    if (roll < 0.18) {
                        aggressor.setRelationWith(target.getId(), DiplomaticStatus.ALLIED);
                        target.setRelationWith(aggressor.getId(), DiplomaticStatus.ALLIED);
                        if (eventRecorder != null) {
                            eventRecorder.accept(
                                    String.format("Year %d: %s and %s signed a Mutual Defense Alliance.",
                                            currentYear, aggressor.getName(), target.getName()),
                                    config.isPrintIndividualEvents()
                            );
                        }
                    } else if (roll < 0.18 + config.getRaidProbability()) {
                        aggressor.setRelationWith(target.getId(), DiplomaticStatus.AT_WAR);
                        target.setRelationWith(aggressor.getId(), DiplomaticStatus.AT_WAR);
                        if (eventRecorder != null) {
                            eventRecorder.accept(
                                    String.format("Year %d: Tensions escalated into WAR between %s and %s!",
                                            currentYear, aggressor.getName(), target.getName()),
                                    config.isPrintIndividualEvents()
                            );
                        }
                    }
                } else if (status == DiplomaticStatus.AT_WAR && random.nextDouble() < 0.20) {
                    // 20% chance each year to negotiate a peace treaty and return to NEUTRAL
                    aggressor.setRelationWith(target.getId(), DiplomaticStatus.NEUTRAL);
                    target.setRelationWith(aggressor.getId(), DiplomaticStatus.NEUTRAL);
                    if (eventRecorder != null) {
                        eventRecorder.accept(
                                String.format("Year %d: %s and %s signed a Peace Treaty.",
                                        currentYear, aggressor.getName(), target.getName()),
                                    config.isPrintIndividualEvents()
                        );
                    }
                    continue;
                }

                // 2. If AT_WAR (or opportunistic raid while NEUTRAL), launch military raid if Soldiers >= 2
                status = aggressor.getRelationWith(target.getId());
                if (status == DiplomaticStatus.ALLIED) {
                    continue; // Allies never raid each other
                }

                boolean launchRaid = (status == DiplomaticStatus.AT_WAR && random.nextDouble() < 0.55)
                        || (status == DiplomaticStatus.NEUTRAL && aggressor.getFoodStockpile() < 15.0 && random.nextDouble() < 0.15);

                int attackerSoldiers = aggressor.getOccupationCount(Occupation.SOLDIER);
                if (!launchRaid || attackerSoldiers < 2) {
                    continue;
                }

                // 2D Spatial Distance, Travel Time, and Military Supply Train Provisions:
                double distance = aggressor.distanceTo(target);
                int marchMonths = Math.max(1, (int) Math.round(distance / 25.0));
                double marchProvisions = attackerSoldiers * (distance * 0.04 + 2.0);

                // Council aborts military campaign if the settlement cannot afford to feed the marching army
                if (aggressor.getFoodStockpile() < marchProvisions) {
                    continue;
                }
                aggressor.deductFood(marchProvisions);

                // If neutral opportunistic raid, relation immediately becomes AT_WAR
                aggressor.setRelationWith(target.getId(), DiplomaticStatus.AT_WAR);
                target.setRelationWith(aggressor.getId(), DiplomaticStatus.AT_WAR);

                int defenderSoldiers = target.getOccupationCount(Occupation.SOLDIER);
                int alliedReinforcements = getAlliedReinforcements(villages, target, aggressor);

                double attackerWeaponMultiplier = aggressor.hasWeaponsForMilitary() ? 1.50 : 1.0;
                double defenderWeaponMultiplier = target.hasWeaponsForMilitary() ? 1.50 : 1.0;

                // March fatigue and stretched supply lines reduce attack power over long distances
                double distanceEfficiency = 1.0 / (1.0 + distance * 0.005);

                // Forward Staging Base: If any intermediate settlement W is allied with aggressor, recover fatigue
                for (Village w : villages) {
                    if (w != aggressor && w != target && aggressor.getRelationWith(w.getId()) == DiplomaticStatus.ALLIED) {
                        if (aggressor.distanceTo(w) < distance && w.distanceTo(target) < distance) {
                            distanceEfficiency = Math.min(1.0, distanceEfficiency + 0.15);
                            break;
                        }
                    }
                }

                double attackPower = attackerSoldiers * attackerWeaponMultiplier * distanceEfficiency * (1.0 + aggressor.getTechLevel() * 0.15) * (0.85 + random.nextDouble() * 0.30);
                double defensePower = (defenderSoldiers + alliedReinforcements) * defenderWeaponMultiplier * (1.0 + target.getTechLevel() * 0.15) * (0.90 + random.nextDouble() * 0.30);

                String shortTarget = target.getName().replace("Settlement ", "");
                String shortAggressor = aggressor.getName().replace("Settlement ", "");

                if (attackPower > defensePower) {
                    // Attacker breaches defenses and plunders Treasury + Food
                    double[] plunder = target.plunderTreasuryAndFood(0.25);
                    aggressor.receivePlunder(plunder[0], plunder[1]);

                    aggressor.setLastRaidEventSummary(String.format("Raided %s (+%.0fg)", shortTarget, plunder[0]));
                    target.setLastRaidEventSummary(String.format("Looted by %s (-%.0fg)", shortAggressor, plunder[0]));

                    inflictRaidCasualties(aggressor, allCouples, 1, currentYear, random, config, eventRecorder);
                    inflictRaidCasualties(target, allCouples, 2, currentYear, random, config, eventRecorder);

                    if (eventRecorder != null) {
                        eventRecorder.accept(
                                String.format("Year %d: %s marched %.1f km (%d mo, consumed %.1ff) and raided %s (Atk %.1f vs Def %.1f), plundering %.1f gold and %.1f food!",
                                        currentYear, aggressor.getName(), distance, marchMonths, marchProvisions, target.getName(), attackPower, defensePower, plunder[0], plunder[1]),
                                config.isPrintIndividualEvents()
                        );
                    }
                } else {
                    // Defenders (and any allied reinforcements) repel the raid and claim war reparations
                    double reparations = aggressor.payReparations(0.15);
                    target.receivePlunder(reparations, 0.0);

                    aggressor.setLastRaidEventSummary(String.format("Defeated at %s (-%.0fg)", shortTarget, reparations));
                    target.setLastRaidEventSummary(String.format("Repelled %s (+%.0fg)", shortAggressor, reparations));

                    inflictRaidCasualties(aggressor, allCouples, 2, currentYear, random, config, eventRecorder);
                    inflictRaidCasualties(target, allCouples, 1, currentYear, random, config, eventRecorder);

                    if (eventRecorder != null) {
                        eventRecorder.accept(
                                String.format("Year %d: %s repelled a raid from %s (Def %.1f vs Atk %.1f) and claimed %.1f gold in reparations!",
                                        currentYear, target.getName(), aggressor.getName(), defensePower, attackPower, reparations),
                                config.isPrintIndividualEvents()
                        );
                    }
                }
                break; // At most one offensive campaign per settlement per year
            }
        }
    }

    private int getAlliedReinforcements(List<Village> villages, Village defender, Village attacker) {
        int reinforcements = 0;
        for (Village v : villages) {
            if (v == defender || v == attacker) continue;
            if (defender.getRelationWith(v.getId()) == DiplomaticStatus.ALLIED
                    && v.getRelationWith(attacker.getId()) != DiplomaticStatus.ALLIED) {
                reinforcements += v.getOccupationCount(Occupation.SOLDIER) / 2;
            }
        }
        return reinforcements;
    }

    private void inflictRaidCasualties(
            Village village,
            List<Couple> allCouples,
            int maxCasualties,
            int currentYear,
            Random random,
            SimulationConfig config,
            BiConsumer<String, Boolean> eventRecorder) {

        List<Person> soldiers = new ArrayList<>();
        List<Person> otherAdults = new ArrayList<>();

        for (Person p : village.getResidents()) {
            if (p.isAlive() && p.getAge() >= 19) {
                if (p.getOccupation() == Occupation.SOLDIER) {
                    soldiers.add(p);
                } else {
                    otherAdults.add(p);
                }
            }
        }

        Collections.shuffle(soldiers, random);
        Collections.shuffle(otherAdults, random);

        List<Person> targetPool = new ArrayList<>(soldiers);
        targetPool.addAll(otherAdults);

        int actualCasualties = Math.min(maxCasualties, targetPool.size());
        for (int i = 0; i < actualCasualties; i++) {
            demographicsSystem.killPerson(targetPool.get(i), allCouples, currentYear, "military battle", config, eventRecorder);
        }
    }
}
