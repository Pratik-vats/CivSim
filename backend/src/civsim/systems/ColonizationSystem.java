package civsim.systems;

import civsim.SimulationConfig;
import civsim.model.DiplomaticStatus;
import civsim.model.Family;
import civsim.model.Gender;
import civsim.model.Person;
import civsim.model.SettlementTier;
import civsim.model.Village;
import civsim.spatial.Biome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;

/**
 * Handles territorial expansion: scouting 2D settlement coordinates, pioneer selection,
 * founding new Family dynasties, seed provisions transfer, and cleaning up collapsed ruins.
 */
public class ColonizationSystem {
    public static final String[] BASE_SURNAMES = {
            "Smith", "Miller", "Carter", "Taylor", "Cooper", "Wright", "Baker", "Clark",
            "Walker", "Ward", "Turner", "Parker", "Reed", "Cook", "Morgan", "Bell",
            "Wood", "Stone", "Hill", "Brooks", "Fisher", "Gray", "Hunt", "Mason"
    };

    private final DemographicsSystem demographicsSystem;
    private final Map<Integer, Integer> lastColonizationYearByVillage = new HashMap<>();

    public ColonizationSystem(DemographicsSystem demographicsSystem) {
        this.demographicsSystem = demographicsSystem;
    }

    public void processAnnualColonization(
            List<Village> villages,
            List<Family> allFamilies,
            int currentYear,
            Random random,
            SimulationConfig config,
            int[] villageIdCounter,
            int[] familyIdCounter,
            BiConsumer<String, Boolean> eventRecorder) {

        if (!config.isAllowColonization()) return;
        if (villages.size() >= 6) return; // Limit maximum active settlements in world to 6 for stability

        List<Village> eligibleSponsors = new ArrayList<>();
        for (Village v : villages) {
            boolean hasTier = (v.getTier() == SettlementTier.TOWN || v.getTier() == SettlementTier.CITY
                    || (villages.size() == 1 && v.getTier().ordinal() >= SettlementTier.VILLAGE.ordinal()));
            boolean hasPop = v.getLivingPopulationCount() >= config.getMinPopulationForColonization();
            boolean offCooldown = (currentYear - lastColonizationYearByVillage.getOrDefault(v.getId(), -100)) >= 15;
            boolean hasResources = v.getFoodStockpile() >= 120.0 && v.getTreasury() >= 120.0;
            if (hasTier && hasPop && offCooldown && hasResources) {
                eligibleSponsors.add(v);
            }
        }

        if (eligibleSponsors.isEmpty()) return;

        Village sponsor = eligibleSponsors.get(0);
        for (Village v : eligibleSponsors) {
            if (v.getLivingPopulationCount() > sponsor.getLivingPopulationCount()) {
                sponsor = v;
            }
        }

        List<Person> youngUnmarried = sponsor.getEligiblePioneers(19, 35);
        List<Person> malePioneers = new ArrayList<>();
        List<Person> femalePioneers = new ArrayList<>();

        for (Person p : youngUnmarried) {
            if (p.getGender() == Gender.MALE) {
                malePioneers.add(p);
            } else {
                femalePioneers.add(p);
            }
        }

        int halfParty = config.getPioneerPartySize() / 2;
        if (malePioneers.size() < halfParty || femalePioneers.size() < halfParty) {
            return;
        }

        Collections.shuffle(malePioneers, random);
        Collections.shuffle(femalePioneers, random);

        int colonyId = villageIdCounter[0]++;
        char letter = (char) ('A' + ((colonyId - 1) % 26));
        String suffix = (colonyId > 26) ? String.valueOf(colonyId / 26) : "";
        String newHamletName = "Settlement " + letter + suffix;

        // Determine 2D coordinates for the new colony:
        // Pioneers scout outward, preferentially exploring along the trade corridor towards allies/trade partners
        double bestX = 50.0;
        double bestY = 50.0;
        double bestScore = -1.0;

        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            if (!villages.isEmpty() && random.nextBoolean()) {
                Village partner = villages.get(random.nextInt(villages.size()));
                if (partner != sponsor) {
                    double directAngle = Math.atan2(partner.getY() - sponsor.getY(), partner.getX() - sponsor.getX());
                    angle = directAngle + (random.nextDouble() - 0.5) * 0.65;
                }
            }
            double dist = 18.0 + random.nextDouble() * 20.0;
            double candX = Math.max(8.0, Math.min(92.0, sponsor.getX() + dist * Math.cos(angle)));
            double candY = Math.max(8.0, Math.min(92.0, sponsor.getY() + dist * Math.sin(angle)));

            double minDist = Double.MAX_VALUE;
            for (Village v : villages) {
                double d = v.distanceTo(candX, candY);
                if (d < minDist) {
                    minDist = d;
                }
            }

            if (minDist >= 13.0 && minDist > bestScore) {
                bestScore = minDist;
                bestX = candX;
                bestY = candY;
            }
        }

        Biome colonyBiome = Biome.determineBiomeAt(bestX, bestY);
        Village newHamlet = new Village(colonyId, newHamletName, config.getInitialFoodPrice(), config.getBankInterestRate(), bestX, bestY, colonyBiome);

        // Sponsor supplies the new colony with startup provisions
        sponsor.deductTreasury(80.0);
        sponsor.deductFood(60.0);
        sponsor.deductWood(30.0);
        sponsor.deductIronStone(15.0);
        newHamlet.addTreasury(80.0);
        newHamlet.addFood(80.0);
        newHamlet.addWood(30.0);
        newHamlet.addIronStone(15.0);
        newHamlet.addTools(10.0);
        lastColonizationYearByVillage.put(sponsor.getId(), currentYear);

        // New colony starts ALLIED with its mother settlement
        sponsor.setRelationWith(newHamlet.getId(), DiplomaticStatus.ALLIED);
        newHamlet.setRelationWith(sponsor.getId(), DiplomaticStatus.ALLIED);

        List<Person> expedition = new ArrayList<>();
        for (int i = 0; i < halfParty; i++) {
            Person maleP = malePioneers.get(i);
            Person femaleP = femalePioneers.get(i);
            expedition.add(maleP);
            expedition.add(femaleP);

            // Establish BRAND NEW Family dynasties for the pioneers in the new colony!
            int famId = familyIdCounter[0]++;
            String baseSurname = BASE_SURNAMES[(famId - 1) % BASE_SURNAMES.length];
            String newSurname = baseSurname + "-" + famId;

            Family colonyFamily = new Family(famId, newSurname, currentYear, maleP);
            colonyFamily.addMember(maleP);
            colonyFamily.addMember(femaleP);
            maleP.setFamily(colonyFamily);
            femaleP.setFamily(colonyFamily);
            allFamilies.add(colonyFamily);
        }

        villages.add(newHamlet);

        for (Person pioneer : expedition) {
            demographicsSystem.relocatePerson(pioneer, sponsor, newHamlet, currentYear, "pioneer colonization expedition", config, eventRecorder);
        }
        newHamlet.electLeaderIfNeeded();

        if (eventRecorder != null) {
            eventRecorder.accept(
                    String.format("Year %d: %s sponsored a colonization expedition, founding %s with %d pioneers across %d new families.",
                            currentYear, sponsor.getName(), newHamlet.getName(), expedition.size(), halfParty),
                    config.isPrintIndividualEvents()
            );
        }
    }

    public void cleanupAbandonedVillages(
            List<Village> villages,
            int currentYear,
            BiConsumer<String, Boolean> eventRecorder,
            SimulationConfig config) {

        List<Village> abandoned = new ArrayList<>();
        for (Village v : villages) {
            if (v.getLivingPopulationCount() == 0) {
                abandoned.add(v);
            }
        }

        if (!abandoned.isEmpty()) {
            for (Village v : abandoned) {
                villages.remove(v);
                for (Village remaining : villages) {
                    remaining.removeRelationWith(v.getId());
                }
                if (eventRecorder != null) {
                    eventRecorder.accept(
                            String.format("Year %d: %s was completely abandoned as its population reached 0 and collapsed into ruins.",
                                    currentYear, v.getName()),
                            config.isPrintIndividualEvents()
                    );
                }
            }
        }
    }
}
