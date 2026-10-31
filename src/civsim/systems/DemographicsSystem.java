package civsim.systems;

import civsim.ProgressionConfig;
import civsim.SimulationConfig;
import civsim.economy.Bank;
import civsim.model.AetherProfile;
import civsim.model.Couple;
import civsim.model.DiplomaticStatus;
import civsim.model.Family;
import civsim.model.Gender;
import civsim.model.HistoricalDeathRecord;
import civsim.model.Person;
import civsim.model.SupernaturalClass;
import civsim.model.Village;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.BiConsumer;

/**
 * Handles demographic lifecycles: aging, age-based mortality curve, estate inheritance,
 * two-phase marriage matching, childbirths, kinship tree traversal,
 * and Stage 2A.5 superhuman longevity scaling.
 */
public class DemographicsSystem {

    private ProgressionConfig progressionConfig;
    private OrganizationSystem organizationSystem;
    private CombatLootListener combatLootListener;
    private final List<HistoricalDeathRecord> historicalDeathRecords = new ArrayList<>();

    public interface CombatLootListener {
        double onCombatFatalityLoot(Person victim, double eligibleNetCarriedWealth, String cause, int currentYear);
    }

    // Aggregates for completed lifetimes
    private int totalMortalDeaths = 0;
    private long sumMortalDeathAge = 0;
    private int totalAwakenedDeaths = 0;
    private long sumAwakenedDeathAge = 0;
    private final Map<Integer, Integer> awakenedDeathsByTier = new HashMap<>();
    private final Map<Integer, Long> sumAwakenedDeathAgeByTier = new HashMap<>();
    private final Map<Integer, Integer> naturalDeathsByTier = new HashMap<>();
    private final Map<Integer, Integer> hazardDeathsByTier = new HashMap<>();

    public void setProgressionConfig(ProgressionConfig progressionConfig) {
        this.progressionConfig = progressionConfig;
    }

    public ProgressionConfig getProgressionConfig() {
        return progressionConfig;
    }

    public void setOrganizationSystem(OrganizationSystem organizationSystem) {
        this.organizationSystem = organizationSystem;
    }

    public OrganizationSystem getOrganizationSystem() {
        return organizationSystem;
    }

    public void setCombatLootListener(CombatLootListener combatLootListener) {
        this.combatLootListener = combatLootListener;
    }

    public CombatLootListener getCombatLootListener() {
        return combatLootListener;
    }

    public List<HistoricalDeathRecord> getHistoricalDeathRecords() {
        return Collections.unmodifiableList(historicalDeathRecords);
    }

    public static boolean isNaturalDeath(String cause) {
        if (cause == null) return false;
        String c = cause.toLowerCase();
        return c.contains("old age") || c.contains("natural causes") || c.contains("infant illness");
    }

    public void processYearlyAgingAndDeaths(
            List<Person> allPeople,
            List<Couple> allCouples,
            List<Village> villages,
            List<Family> allFamilies,
            int currentYear,
            int effectiveDeathAge,
            Random random,
            SimulationConfig config,
            ProgressionConfig progConfig,
            BiConsumer<String, Boolean> eventRecorder) {

        if (progConfig != null) {
            this.progressionConfig = progConfig;
        }

        int awakeningStart = (config != null) ? config.getAwakeningStartYear() : 50;

        for (Person person : allPeople) {
            if (!person.isAlive()) continue;

            person.updateAgeAndOccupation();

            int age = person.getAge();
            int villageTech = person.getVillage() != null ? person.getVillage().getTechLevel() : 0;

            // Stage 2A.5: Tier longevity offset lookup
            int tierOffset = 0;
            if (this.progressionConfig != null && this.progressionConfig.isSupernaturalLifespanEnabled()) {
                tierOffset = this.progressionConfig.getTierLongevityOffset(person, currentYear, awakeningStart);
            }

            double annualDeathProbability;
            if (age <= 4) {
                // Infant mortality: tech-scaled baseline, unaffected by tier offset
                annualDeathProbability = Math.max(0.005, 0.025 - (villageTech * 0.004));
            } else if (age <= 18) {
                // Youth mortality: baseline floor, unaffected by tier offset
                annualDeathProbability = 0.004;
            } else if (age <= 50) {
                // Adult baseline mortality: unaffected by tier offset (never underflows into youth/infant)
                annualDeathProbability = 0.006;
            } else {
                // Natural old-age senescence: shift age by tierOffset, clamped at floor of 50
                int effectiveSenescenceAge = Math.max(50, age - tierOffset);
                if (effectiveSenescenceAge < 65) {
                    annualDeathProbability = 0.015 + ((effectiveSenescenceAge - 50) * 0.002);
                } else {
                    double techBonus = villageTech * 0.015;
                    double elderProgress = (double) (effectiveSenescenceAge - 65) / 18.0;
                    annualDeathProbability = Math.min(0.95, 0.06 + Math.pow(elderProgress, 2.2) - techBonus);
                }
            }

            // Finite lifespan ceiling enforcement
            int personalCeiling = effectiveDeathAge + 15 + tierOffset;
            if (age >= personalCeiling || random.nextDouble() < annualDeathProbability) {
                String cause = (age <= 4) ? "infant illness" : (age >= 65 ? "old age" : "natural causes / illness");
                killPerson(person, allCouples, currentYear, cause, config, eventRecorder);
            }
        }

        // Heap Memory cleanup: prune deceased citizens and inactive couples from active collections
        allPeople.removeIf(p -> !p.isAlive());
        allCouples.removeIf(c -> !c.isActive() || !c.getHusband().isAlive() || !c.getWife().isAlive());
        for (Village v : villages) {
            v.pruneDeceasedResidents();
        }
        for (Family fam : allFamilies) {
            fam.pruneDeceased();
        }
    }

    public void processYearlyAgingAndDeaths(
            List<Person> allPeople,
            List<Couple> allCouples,
            List<Village> villages,
            List<Family> allFamilies,
            int currentYear,
            int effectiveDeathAge,
            Random random,
            SimulationConfig config,
            BiConsumer<String, Boolean> eventRecorder) {
        processYearlyAgingAndDeaths(allPeople, allCouples, villages, allFamilies, currentYear,
                effectiveDeathAge, random, config, this.progressionConfig, eventRecorder);
    }

    public void killPerson(
            Person person,
            List<Couple> allCouples,
            int currentYear,
            String cause,
            SimulationConfig config,
            BiConsumer<String, Boolean> eventRecorder) {
        if (!person.isAlive()) return;

        Person formerSpouse = person.getSpouse();
        distributeEstate(person, formerSpouse, cause, currentYear);

        boolean diedNow = person.markDeceased(currentYear, cause);
        if (diedNow) {
            if (person.getVillage() != null) {
                person.getVillage().recordDeathThisYear();
            }

            if (formerSpouse != null && formerSpouse.getSpouse() == person) {
                formerSpouse.clearSpouseAfterPartnerDeath();
            }
            deactivateCouplesInvolving(person, allCouples);

            // Stage 2A.5: Create immutable historical death record before pruning
            int villageTech = person.getVillage() != null ? person.getVillage().getTechLevel() : 0;
            int baseDeathAge = (config != null) ? config.getDeathAge() : 75;
            int effectiveDeathAge = baseDeathAge + villageTech;
            int awakeningStart = (config != null) ? config.getAwakeningStartYear() : 50;

            boolean natural = isNaturalDeath(cause);
            boolean awakened = person.isAwakened();
            AetherProfile ap = person.getAetherProfile();
            int finalTier = (awakened && ap != null) ? ap.getPowerTier() : 0;
            int offset = 0;
            int ceiling = effectiveDeathAge + 15;
            SupernaturalClass cls = null;
            boolean darkAspect = false;

            if (awakened && ap != null) {
                cls = ap.getSupernaturalClass();
                darkAspect = ap.isDarkAwakened();
                if (progressionConfig != null) {
                    offset = progressionConfig.getTierLongevityOffset(person, currentYear, awakeningStart);
                    ceiling = progressionConfig.getPersonalNaturalLifespanCeiling(person, effectiveDeathAge, currentYear, awakeningStart);
                }
            }

            int bAtt = (ap != null && ap.getProgressionRecord() != null) ? ap.getProgressionRecord().getTotalBreakthroughAttempts() : 0;
            int bSuc = (ap != null && ap.getProgressionRecord() != null) ? ap.getProgressionRecord().getTotalBreakthroughSuccesses() : 0;
            int bFail = (ap != null && ap.getProgressionRecord() != null) ? ap.getProgressionRecord().getTotalBreakthroughFailures() : 0;

            HistoricalDeathRecord deathRecord = new HistoricalDeathRecord(
                    person.getId(), person.getFullName(), currentYear, person.getAge(),
                    awakened, cls, darkAspect, finalTier, offset, ceiling, cause, natural,
                    bAtt, bSuc, bFail);
            person.setDeathRecord(deathRecord);
            historicalDeathRecords.add(deathRecord);

            // Update completed lifetime aggregates
            if (!awakened) {
                totalMortalDeaths++;
                sumMortalDeathAge += person.getAge();
            } else {
                totalAwakenedDeaths++;
                sumAwakenedDeathAge += person.getAge();
                awakenedDeathsByTier.put(finalTier, awakenedDeathsByTier.getOrDefault(finalTier, 0) + 1);
                sumAwakenedDeathAgeByTier.put(finalTier, sumAwakenedDeathAgeByTier.getOrDefault(finalTier, 0L) + person.getAge());
                if (natural) {
                    naturalDeathsByTier.put(finalTier, naturalDeathsByTier.getOrDefault(finalTier, 0) + 1);
                } else {
                    hazardDeathsByTier.put(finalTier, hazardDeathsByTier.getOrDefault(finalTier, 0) + 1);
                }
            }

            if (eventRecorder != null) {
                eventRecorder.accept(
                        String.format("Year %d: %s died at age %d (%s).",
                                currentYear, person.getFullName(), person.getAgeAtDeath(), cause),
                        config != null && config.isPrintIndividualEvents()
                );
            }
            if (organizationSystem != null) {
                organizationSystem.processMemberDeath(person, currentYear, 0, cause, Collections.emptyList());
            }
            person.clearAfterDeath();
        }
    }

    public void distributeEstate(Person deceased, Person spouse) {
        distributeEstate(deceased, spouse, null, 0);
    }

    public void distributeEstate(Person deceased, Person spouse, String cause, int currentYear) {
        if (deceased.getVillage() == null) return;
        Bank localBank = deceased.getVillage().getBank();
        double initialWealth = deceased.getWealth();
        double initialBank = deceased.getBankBalance();
        double loanDebt = deceased.getLoanDebt();

        // 1. Pay bank debt first from bankBalance, then from carried wealth if needed
        double debtFromBank = Math.min(initialBank, loanDebt);
        double remainingDebt = loanDebt - debtFromBank;
        double debtFromWealth = Math.min(initialWealth, remainingDebt);
        double totalDebtPaid = debtFromBank + debtFromWealth;

        double eligibleNetCarriedWealth = Math.max(0.0, initialWealth - debtFromWealth);

        // 2. Combat Looting: if combatLootListener is registered and cause is combat death
        double lootedAmount = 0.0;
        if (combatLootListener != null && cause != null && cause.contains("slain in combat")) {
            lootedAmount = combatLootListener.onCombatFatalityLoot(deceased, eligibleNetCarriedWealth, cause, currentYear);
            lootedAmount = Math.max(0.0, Math.min(eligibleNetCarriedWealth, lootedAmount));
        }

        if (totalDebtPaid > 0) {
            localBank.repayLoan(totalDebtPaid);
            deceased.setLoanDebt(0.0);
        }
        if (initialBank > 0) {
            localBank.withdraw(initialBank);
            deceased.setBankBalance(0.0);
        }
        deceased.deductWealth(initialWealth);

        double remainingCarriedWealth = eligibleNetCarriedWealth - lootedAmount;
        double remainingBank = initialBank - debtFromBank;
        double estate = remainingCarriedWealth + remainingBank;

        if (estate <= 0) return;

        if (spouse != null && spouse.isAlive()) {
            spouse.addWealth(estate);
            return;
        }

        List<Person> livingChildren = new ArrayList<>();
        for (Person child : deceased.getChildren()) {
            if (child.isAlive()) {
                livingChildren.add(child);
            }
        }

        if (!livingChildren.isEmpty()) {
            double share = estate / livingChildren.size();
            for (Person child : livingChildren) {
                child.addWealth(share);
            }
        }

        // Phase 2C.1 Personal Equipment & Clan Heirloom Estate Processing
        if (cause == null || !cause.contains("slain in combat")) {
            EquipmentCraftingSystem ecs = EquipmentCraftingSystem.getActiveInstance();
            ClanHeirloomSystem chs = ClanHeirloomSystem.getActiveInstance();
            if (chs != null && ecs != null) {
                chs.handleBearerDomesticDeath(deceased, spouse, livingChildren, deceased.getVillage(), ecs, organizationSystem, null, null);
            }
        }
    }

    private void deactivateCouplesInvolving(Person deceased, List<Couple> allCouples) {
        for (Couple couple : allCouples) {
            if (couple.isActive() && (couple.getHusband() == deceased || couple.getWife() == deceased)) {
                couple.deactivate();
            }
        }
    }

    public int matchMarriages(
            List<Village> villages,
            List<Couple> allCouples,
            int currentYear,
            long currentTotalMonths,
            Random random,
            SimulationConfig config,
            int[] coupleIdCounter,
            BiConsumer<String, Boolean> eventRecorder) {

        int crossMarriages = 0;

        for (Village village : villages) {
            List<Person> eligibleMales = village.getEligibleUnmarriedMales(config.getMinMarriageAgeExclusive());
            List<Person> eligibleFemales = village.getEligibleUnmarriedFemales(config.getMinMarriageAgeExclusive());

            Collections.shuffle(eligibleMales, random);
            Collections.shuffle(eligibleFemales, random);

            matchPool(eligibleMales, eligibleFemales, allCouples, currentYear, currentTotalMonths, false,
                    random, config, coupleIdCounter, eventRecorder);
        }

        if (config.isAllowCrossVillageMarriage() && villages.size() > 1) {
            List<Person> remainingMales = new ArrayList<>();
            List<Person> remainingFemales = new ArrayList<>();

            for (Village village : villages) {
                remainingMales.addAll(village.getEligibleUnmarriedMales(config.getMinMarriageAgeExclusive()));
                remainingFemales.addAll(village.getEligibleUnmarriedFemales(config.getMinMarriageAgeExclusive()));
            }

            Collections.shuffle(remainingMales, random);
            Collections.shuffle(remainingFemales, random);

            crossMarriages = matchPool(remainingMales, remainingFemales, allCouples, currentYear, currentTotalMonths, true,
                    random, config, coupleIdCounter, eventRecorder);
        }

        return crossMarriages;
    }

    private int matchPool(
            List<Person> males,
            List<Person> females,
            List<Couple> allCouples,
            int currentYear,
            long currentTotalMonths,
            boolean crossVillagePhase,
            Random random,
            SimulationConfig config,
            int[] coupleIdCounter,
            BiConsumer<String, Boolean> eventRecorder) {

        int crossCount = 0;
        boolean[] femaleMatched = new boolean[females.size()];

        for (Person male : males) {
            if (!male.isEligibleForMarriage(config.getMinMarriageAgeExclusive())) continue;

            for (int j = 0; j < females.size(); j++) {
                if (femaleMatched[j]) continue;
                Person female = females.get(j);
                if (!female.isEligibleForMarriage(config.getMinMarriageAgeExclusive())) continue;

                if (crossVillagePhase) {
                    if (male.getVillage() == female.getVillage()) continue;
                    if (male.getVillage().getRelationWith(female.getVillage().getId()) == DiplomaticStatus.AT_WAR) continue;
                }

                if (!male.isCompatibleAgeWith(female, config.getMaxMarriageAgeDifference())) continue;
                if (male.isRelatedTo(female, config.getMaxKinshipGenerations(), config.isPreventSameFamilyMarriage())) continue;

                femaleMatched[j] = true;

                Village maleVillage = male.getVillage();
                Village femaleVillage = female.getVillage();
                Village sharedVillage = maleVillage;

                if (maleVillage != femaleVillage) {
                    if (maleVillage.getLivingPopulationCount() > femaleVillage.getLivingPopulationCount()) {
                        relocatePerson(male, maleVillage, femaleVillage, currentYear, "cross-settlement marriage", config, eventRecorder);
                        sharedVillage = femaleVillage;
                    } else {
                        relocatePerson(female, femaleVillage, maleVillage, currentYear, "cross-settlement marriage", config, eventRecorder);
                        sharedVillage = maleVillage;
                    }
                    crossCount++;
                }

                male.marry(female);
                sharedVillage.recordMarriageThisYear();

                int plannedChildren = config.getMinChildrenPerCouple()
                        + random.nextInt(config.getMaxChildrenPerCouple() - config.getMinChildrenPerCouple() + 1);

                Couple couple = new Couple(
                        coupleIdCounter[0]++,
                        male,
                        female,
                        currentYear,
                        currentTotalMonths,
                        plannedChildren,
                        config.getMinBirthIntervalMonths()
                );
                allCouples.add(couple);

                if (eventRecorder != null) {
                    eventRecorder.accept(
                            String.format("Year %d: %s (age %d) married %s (age %d) (settled in %s, planned children: %d).",
                                    currentYear, male.getFullName(), male.getAge(),
                                    female.getFullName(), female.getAge(), sharedVillage.getName(), plannedChildren),
                            config.isPrintIndividualEvents()
                    );
                }
                break;
            }
        }
        return crossCount;
    }

    public void processScheduledBirths(
            List<Couple> allCouples,
            List<Person> allPeople,
            int currentYear,
            long currentTotalMonths,
            Random random,
            SimulationConfig config,
            int[] personIdCounter,
            int[] totalRecordedPeopleCounter,
            BiConsumer<String, Boolean> eventRecorder) {

        for (Couple couple : allCouples) {
            if (couple.canHaveChildNow(
                    currentTotalMonths,
                    config.getMinBirthIntervalMonths(),
                    config.getMaxFemaleFertilityAge())) {

                Person mother = couple.getWife();
                Village village = mother.getVillage();
                if (village.isInFamine()) continue; // Famine suppression

                Person father = couple.getHusband();
                Family childFamily = (father.getFamily() != null) ? father.getFamily() : mother.getFamily();

                int childId = personIdCounter[0]++;
                String childName = "Person" + childId;
                Gender childGender = random.nextBoolean() ? Gender.MALE : Gender.FEMALE;

                Person newborn = new Person(
                        childId,
                        childName,
                        childGender,
                        0,
                        currentYear,
                        village,
                        mother,
                        father,
                        childFamily,
                        config.getInitialPersonalWealth()
                );

                // Phase 2A: Aether inheritance — if both parents have profiles, child inherits alleles
                civsim.model.AetherProfile motherProfile = mother.getAetherProfile();
                civsim.model.AetherProfile fatherProfile = father.getAetherProfile();
                if (motherProfile != null && fatherProfile != null) {
                    civsim.model.AetherProfile childProfile = civsim.model.AetherProfile.inherit(motherProfile, fatherProfile, random);
                    newborn.setAetherProfile(childProfile);
                }

                if (childFamily != null) {
                    childFamily.addMember(newborn);
                }
                allPeople.add(newborn);
                totalRecordedPeopleCounter[0]++;
                village.addResident(newborn);
                mother.addChild(newborn);
                father.addChild(newborn);

                couple.recordBirth(currentTotalMonths, config.getMinBirthIntervalMonths());
                village.recordBirthThisYear();

                String relationWord = (childGender == Gender.MALE) ? "son" : "daughter";
                if (eventRecorder != null) {
                    eventRecorder.accept(
                            String.format("Year %d: %s and %s (age %d) had a %s named %s.",
                                    currentYear, father.getFullName(), mother.getFullName(), mother.getAge(),
                                    relationWord, newborn.getFullName()),
                            config.isPrintIndividualEvents()
                    );
                }
            }
        }
    }

    public void relocatePerson(
            Person person,
            Village fromVillage,
            Village toVillage,
            int currentYear,
            String reason,
            SimulationConfig config,
            BiConsumer<String, Boolean> eventRecorder) {
        if (fromVillage == toVillage) return;

        double savings = person.getBankBalance();
        if (savings > 0 && fromVillage.getBank().withdraw(savings)) {
            toVillage.getBank().deposit(savings);
        }

        fromVillage.migrateOut(person);
        toVillage.migrateIn(person);
        person.recordMigration(toVillage);

        if (eventRecorder != null) {
            eventRecorder.accept(
                    String.format("Year %d: %s migrated from %s to %s (%s).",
                            currentYear, person.getFullName(), fromVillage.getName(), toVillage.getName(), reason),
                    config.isPrintIndividualEvents()
            );
        }
    }

    // --- Stage 2A.5: Annual Longevity Telemetry & Completed Lifetime Analytics ---

    public String generateAnnualLongevitySummary(List<Person> allPeople, int currentYear, SimulationConfig config) {
        int totalLivingAwakened = 0;
        double sumLivingAge = 0;
        int sumLivingOffset = 0;
        int ceilingIncreasesThisYear = 0;
        Map<Integer, Integer> livingCountByTier = new TreeMap<>();
        Map<Integer, Double> livingAgeSumByTier = new TreeMap<>();

        for (Person p : allPeople) {
            if (!p.isAlive()) continue;
            AetherProfile ap = p.getAetherProfile();
            if (ap == null || !ap.isAwakened()) continue;

            totalLivingAwakened++;
            int age = p.getAge();
            sumLivingAge += age;

            int tier = ap.getPowerTier();
            livingCountByTier.put(tier, livingCountByTier.getOrDefault(tier, 0) + 1);
            livingAgeSumByTier.put(tier, livingAgeSumByTier.getOrDefault(tier, 0.0) + age);

            int offset = (progressionConfig != null) ? progressionConfig.getTierLongevityOffset(tier) : 0;
            sumLivingOffset += offset;

            if (ap.getLastTierAdvancementYear() == currentYear) {
                ceilingIncreasesThisYear++;
            }
        }

        if (totalLivingAwakened == 0 && totalAwakenedDeaths == 0) {
            return null;
        }

        double avgLivingAge = (totalLivingAwakened > 0) ? (sumLivingAge / totalLivingAwakened) : 0.0;
        double avgLivingOffset = (totalLivingAwakened > 0) ? ((double) sumLivingOffset / totalLivingAwakened) : 0.0;

        StringBuilder tierSb = new StringBuilder();
        for (Map.Entry<Integer, Integer> entry : livingCountByTier.entrySet()) {
            int t = entry.getKey();
            int count = entry.getValue();
            double avgAge = livingAgeSumByTier.get(t) / count;
            if (tierSb.length() > 0) tierSb.append(" ");
            tierSb.append(String.format("T%d:%d(%.0fy)", t, count, avgAge));
        }
        if (tierSb.length() == 0) tierSb.append("None");

        double avgMortalDeathAge = (totalMortalDeaths > 0) ? ((double) sumMortalDeathAge / totalMortalDeaths) : 0.0;
        double avgAwakenedDeathAge = (totalAwakenedDeaths > 0) ? ((double) sumAwakenedDeathAge / totalAwakenedDeaths) : 0.0;

        int totalNat = 0, totalHaz = 0;
        for (int c : naturalDeathsByTier.values()) totalNat += c;
        for (int c : hazardDeathsByTier.values()) totalHaz += c;

        return String.format(
                "AwkLiving:%d (avgAge:%.1f, avgOff:+%.1fy) | [%s] | CeilUpgrades:%d | Deceased: MrtAvg:%.1fy AwkAvg:%.1fy (Nat:%d Haz:%d)",
                totalLivingAwakened, avgLivingAge, avgLivingOffset, tierSb.toString(),
                ceilingIncreasesThisYear, avgMortalDeathAge, avgAwakenedDeathAge, totalNat, totalHaz);
    }

    public int getTotalMortalDeaths() { return totalMortalDeaths; }
    public long getSumMortalDeathAge() { return sumMortalDeathAge; }
    public double getAverageAgeAtDeathMortal() {
        return totalMortalDeaths > 0 ? ((double) sumMortalDeathAge / totalMortalDeaths) : 0.0;
    }

    public int getTotalAwakenedDeaths() { return totalAwakenedDeaths; }
    public long getSumAwakenedDeathAge() { return sumAwakenedDeathAge; }
    public double getAverageAgeAtDeathAwakened() {
        return totalAwakenedDeaths > 0 ? ((double) sumAwakenedDeathAge / totalAwakenedDeaths) : 0.0;
    }

    public double getAverageAgeAtDeathAwakened(int tier) {
        int count = awakenedDeathsByTier.getOrDefault(tier, 0);
        return count > 0 ? ((double) sumAwakenedDeathAgeByTier.getOrDefault(tier, 0L) / count) : 0.0;
    }

    public int getAwakenedDeathsCount(int tier) {
        return awakenedDeathsByTier.getOrDefault(tier, 0);
    }

    public int getNaturalDeathsCount(int tier) {
        return naturalDeathsByTier.getOrDefault(tier, 0);
    }

    public int getHazardDeathsCount(int tier) {
        return hazardDeathsByTier.getOrDefault(tier, 0);
    }

    public int getTotalAwakenedNaturalDeaths() {
        int total = 0;
        for (int c : naturalDeathsByTier.values()) total += c;
        return total;
    }

    public int getTotalAwakenedHazardDeaths() {
        int total = 0;
        for (int c : hazardDeathsByTier.values()) total += c;
        return total;
    }

    public void killPerson(
            Person person,
            int currentYear,
            String cause,
            SimulationConfig config) {
        killPerson(person, Collections.emptyList(), currentYear, cause, config, null);
    }

    public Map<Integer, Integer> getAwakenedDeathsByTier() {
        return Collections.unmodifiableMap(awakenedDeathsByTier);
    }

    public Map<Integer, Integer> getNaturalDeathsByTier() {
        return Collections.unmodifiableMap(naturalDeathsByTier);
    }

    public Map<Integer, Integer> getHazardDeathsByTier() {
        return Collections.unmodifiableMap(hazardDeathsByTier);
    }
}
