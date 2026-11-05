package civsim.systems;

import civsim.Season;
import civsim.economy.Bank;
import civsim.economy.Market;
import civsim.model.DiplomaticStatus;
import civsim.model.MonthlyCasualty;
import civsim.model.Occupation;
import civsim.model.Person;
import civsim.model.Village;
import civsim.spatial.Biome;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Handles monthly agricultural yields, resource extraction (timber, mining, blacksmithing),
 * food consumption, famine mortality, winter frost exposure, wages, banking, taxation, and technology.
 */
public class ProductionAndSurvivalSystem {

    public List<MonthlyCasualty> simulateMonthlyEconomy(Village village, Season season, double foodPerPerson, double baseFoodPerFarmer, Random random) {
        List<MonthlyCasualty> casualties = new ArrayList<>();
        int livingPop = village.getLivingPopulationCount();
        if (livingPop == 0) return casualties;

        int farmers = village.getOccupationCount(Occupation.FARMER);
        int lumberjacks = village.getOccupationCount(Occupation.LUMBERJACK);
        int miners = village.getOccupationCount(Occupation.MINER);
        int blacksmiths = village.getOccupationCount(Occupation.BLACKSMITH);
        int merchants = village.getOccupationCount(Occupation.MERCHANT);
        int builders = village.getOccupationCount(Occupation.BUILDER);
        int soldiers = village.getOccupationCount(Occupation.SOLDIER);

        double techMultiplier = 1.0 + (village.getTechLevel() * 0.10);
        boolean hasTools = village.hasToolsForFarming();
        double toolFarmMultiplier = hasTools ? 1.25 : 1.0;

        // 1. Wood Production (Lumberjacks)
        double woodProduced = lumberjacks * 3.0 * (1.0 + (village.getTechLevel() * 0.08)) * (hasTools ? 1.25 : 1.0) * village.getBiome().getWoodYieldMultiplier();
        village.addWood(woodProduced);

        // 2. Stone & Iron Ore Extraction (Miners)
        double oreProduced = miners * 2.2 * (1.0 + (village.getTechLevel() * 0.10)) * (hasTools ? 1.25 : 1.0) * village.getBiome().getOreYieldMultiplier();
        village.addIronStone(oreProduced);

        // 3. Blacksmithing (Tools & Weapons)
        double toolsCrafted = 0.0;
        if (blacksmiths > 0) {
            double maxCraftable = blacksmiths * 1.5 * (1.0 + (village.getTechLevel() * 0.12));
            double affordableByOre = village.getIronStoneStockpile() / 1.0;
            double affordableByWood = village.getWoodStockpile() / 0.5;
            toolsCrafted = Math.min(maxCraftable, Math.min(affordableByOre, affordableByWood));
            if (toolsCrafted > 0) {
                village.deductIronStone(toolsCrafted * 1.0);
                village.deductWood(toolsCrafted * 0.5);
                village.addTools(toolsCrafted);
            }
        }

        // 4. Tool wear & tear degradation
        int activeWorkers = farmers + soldiers + lumberjacks + miners;
        village.getStockpile().degradeTools(activeWorkers);

        // 5. Agricultural Food Production (Seasons + Arable Land Limit)
        int arableLimit = village.getArableLandSlots();
        double effectiveFarmers = (farmers <= arableLimit)
                ? farmers
                : (arableLimit + (farmers - arableLimit) * 0.20);

        double effectiveFoodPerFarmer = baseFoodPerFarmer * techMultiplier * toolFarmMultiplier * season.getCropYieldMultiplier() * village.getBiome().getCropYieldMultiplier();
        double foodProduced = effectiveFarmers * effectiveFoodPerFarmer;
        if (village.getBiome() == Biome.COASTAL) {
            foodProduced += livingPop * 0.35; // Coastal fishing / seafood baseline
        } else if (village.getBiome() == Biome.HIGHLAND_MINE) {
            foodProduced += livingPop * 0.28; // Highland alpine hunting, herding & foraging baseline
        }
        village.addFood(foodProduced);

        // 6. Food Consumption (Seasonality: 1.5x in Winter)
        double foodConsumed = livingPop * foodPerPerson * season.getFoodConsumptionMultiplier();
        if (village.getFoodStockpile() >= foodConsumed) {
            village.deductFood(foodConsumed);
            village.setConsecutiveFamineMonths(0);
        } else {
            // Food Deficit: Famine
            double deficit = foodConsumed - village.getFoodStockpile();
            double deficitFraction = foodConsumed > 0 ? (deficit / foodConsumed) : 1.0;
            village.deductFood(village.getFoodStockpile());
            village.setConsecutiveFamineMonths(village.getConsecutiveFamineMonths() + 1);

            int famineMo = village.getConsecutiveFamineMonths();
            double baseRisk;
            if (famineMo <= 1) {
                baseRisk = 0.02 * deficitFraction;
            } else if (famineMo == 2) {
                baseRisk = 0.05 * deficitFraction;
            } else if (famineMo <= 4) {
                baseRisk = 0.12 * deficitFraction;
            } else {
                baseRisk = Math.min(0.35, 0.18 + (famineMo - 4) * 0.03);
            }

            for (Person p : village.getResidents()) {
                if (!p.isAlive()) continue;
                double personalRisk = baseRisk;
                if (p.getAge() <= 5) personalRisk *= 1.8;
                else if (p.getAge() >= 65) personalRisk *= 1.6;
                if (p.getWealth() >= 10.0) personalRisk *= 0.5;

                if (random.nextDouble() < personalRisk) {
                    casualties.add(new MonthlyCasualty(p, "starvation / famine"));
                    village.addFamineDeath();
                }
            }
        }

        // 7. Winter Firewood Heating
        if (season == Season.WINTER) {
            double woodNeeded = livingPop * season.getFirewoodPerPersonPerMonth();
            if (village.getWoodStockpile() >= woodNeeded) {
                village.deductWood(woodNeeded);
            } else {
                double woodShortage = woodNeeded > 0 ? ((woodNeeded - village.getWoodStockpile()) / woodNeeded) : 1.0;
                village.deductWood(village.getWoodStockpile());
                double freezeRisk = woodShortage * 0.06;
                for (Person p : village.getResidents()) {
                    if (!p.isAlive()) continue;
                    double personalRisk = freezeRisk;
                    if (p.getAge() <= 5) personalRisk *= 2.0;
                    else if (p.getAge() >= 65) personalRisk *= 2.2;

                    if (random.nextDouble() < personalRisk) {
                        casualties.add(new MonthlyCasualty(p, "winter frost / hypothermia"));
                        village.addFreezingDeath();
                    }
                }
            }
        }

        // 8. Market pricing & wage distribution
        village.getMarket().updateMarketPrices(village.getFoodStockpile(), livingPop);
        double foodPrice = village.getMarket().getFoodPrice();

        for (Person p : village.getResidents()) {
            if (!p.isAlive()) continue;
            switch (p.getOccupation()) {
                case FARMER -> p.addWealth(Math.max(0.4, (effectiveFoodPerFarmer * 0.35) * foodPrice));
                case LUMBERJACK -> p.addWealth(1.2 + (woodProduced / Math.max(1, lumberjacks) * 0.15));
                case MINER -> p.addWealth(1.4 + (oreProduced / Math.max(1, miners) * 0.15));
                case BLACKSMITH -> p.addWealth(1.6 + (toolsCrafted / Math.max(1, blacksmiths) * 0.20));
                case MERCHANT -> p.addWealth((1.2 + (merchants * 0.15)) * foodPrice);
                case BUILDER -> p.addWealth(1.5);
                case SOLDIER -> p.addWealth(1.2);
                default -> {}
            }
        }

        return casualties;
    }

    public void assignOccupations(Village village, double foodPerPerson, double baseFoodPerFarmer, int capacityPerBuilding) {
        List<Person> workingAgeAdults = new ArrayList<>();
        for (Person p : village.getResidents()) {
            if (p.isAlive() && p.getAge() >= 19 && p.getAge() < 65) {
                workingAgeAdults.add(p);
            }
        }

        int totalWorkers = workingAgeAdults.size();
        if (totalWorkers == 0) return;

        int livingPop = village.getLivingPopulationCount();
        double techMultiplier = 1.0 + (village.getTechLevel() * 0.10);
        double toolMultiplier = village.hasToolsForFarming() ? 1.25 : 1.0;
        double effectiveFoodPerFarmer = baseFoodPerFarmer * techMultiplier * toolMultiplier;
        double monthlyFoodNeeded = livingPop * foodPerPerson;

        double monthsReserve = monthlyFoodNeeded > 0 ? (village.getFoodStockpile() / monthlyFoodNeeded) : 10.0;
        int arableLimit = village.getArableLandSlots();

        // 1. Target farmers
        int targetFarmers;
        int neededFarmers = (int) Math.ceil((monthlyFoodNeeded * 1.25) / (effectiveFoodPerFarmer * 0.925));
        if (monthsReserve < 3.0 || village.isInFamine()) {
            targetFarmers = Math.max(neededFarmers, (int) Math.round(totalWorkers * 0.70));
        } else if (monthsReserve < 6.0) {
            targetFarmers = Math.max(neededFarmers, (int) Math.round(totalWorkers * 0.45));
        } else if (monthsReserve < 9.0) {
            targetFarmers = Math.max(neededFarmers, (int) Math.round(totalWorkers * 0.35));
        } else {
            targetFarmers = Math.min(neededFarmers, (int) Math.round(totalWorkers * 0.22));
        }
        if (!village.isInFamine() && monthsReserve >= 3.0) {
            targetFarmers = Math.min(targetFarmers, arableLimit);
        }
        targetFarmers = Math.min(totalWorkers, Math.max(1, targetFarmers));

        int remainingWorkers = totalWorkers - targetFarmers;

        // 2. Target Lumberjacks
        int targetLumberjacks = 0;
        if (remainingWorkers > 0) {
            double targetWood = (livingPop * 2.0) + 30.0;
            if (village.getWoodStockpile() < targetWood) {
                targetLumberjacks = Math.max(1, (int) Math.round(remainingWorkers * 0.20));
            } else {
                targetLumberjacks = Math.max(1, (int) Math.round(remainingWorkers * 0.08));
            }
            targetLumberjacks = Math.min(remainingWorkers, targetLumberjacks);
        }
        remainingWorkers -= targetLumberjacks;

        // 3. Target Miners
        int targetMiners = 0;
        if (remainingWorkers > 0) {
            double targetOre = (livingPop * 0.8) + 25.0;
            if (village.getIronStoneStockpile() < targetOre) {
                targetMiners = Math.max(1, (int) Math.round(remainingWorkers * 0.18));
            } else {
                targetMiners = Math.max(1, (int) Math.round(remainingWorkers * 0.08));
            }
            targetMiners = Math.min(remainingWorkers, targetMiners);
        }
        remainingWorkers -= targetMiners;

        // 4. Target Blacksmiths
        int targetBlacksmiths = 0;
        if (remainingWorkers > 0) {
            double targetTools = (targetFarmers + (livingPop * 0.2)) * 0.6;
            if (village.getToolsStockpile() < targetTools && village.getIronStoneStockpile() >= 2.0) {
                targetBlacksmiths = Math.max(1, (int) Math.round(remainingWorkers * 0.16));
            } else {
                targetBlacksmiths = Math.max(1, (int) Math.round(remainingWorkers * 0.06));
            }
            targetBlacksmiths = Math.min(remainingWorkers, targetBlacksmiths);
        }
        remainingWorkers -= targetBlacksmiths;

        // 5. Target Builders
        int currentCapacity = village.getTotalHousingCapacity(capacityPerBuilding);
        int targetBuilders = 0;
        if (remainingWorkers > 0) {
            boolean canBuild = !village.getTier().hasHousingCap() || (village.getHousingCount() < village.getInfrastructureSlots());
            if (livingPop > currentCapacity && canBuild && village.getWoodStockpile() >= 4.0) {
                targetBuilders = Math.max(1, (int) Math.round(remainingWorkers * 0.38));
            } else if (livingPop >= currentCapacity * 0.85 && canBuild && village.getWoodStockpile() >= 4.0) {
                targetBuilders = Math.max(1, (int) Math.round(remainingWorkers * 0.16));
            } else {
                targetBuilders = Math.min(remainingWorkers, (int) Math.round(remainingWorkers * 0.05));
            }
            targetBuilders = Math.min(remainingWorkers, targetBuilders);
        }
        remainingWorkers -= targetBuilders;

        // 6. Target Soldiers
        int targetSoldiers = 0;
        if (remainingWorkers > 0) {
            boolean hasWar = false;
            for (Person p : village.getResidents()) {
                if (p.isAlive() && village.getRelationWith(p.getId()) == DiplomaticStatus.AT_WAR) {
                    hasWar = true;
                    break;
                }
            }
            if (hasWar) {
                targetSoldiers = Math.max(2, (int) Math.round(remainingWorkers * 0.50));
            } else {
                targetSoldiers = Math.max(1, (int) Math.round(remainingWorkers * 0.20));
            }
            targetSoldiers = Math.min(remainingWorkers, targetSoldiers);
        }
        remainingWorkers -= targetSoldiers;

        // 7. Remaining are merchants
        int assigned = 0;
        for (Person p : workingAgeAdults) {
            if (assigned < targetFarmers) {
                p.setOccupation(Occupation.FARMER);
            } else if (assigned < targetFarmers + targetLumberjacks) {
                p.setOccupation(Occupation.LUMBERJACK);
            } else if (assigned < targetFarmers + targetLumberjacks + targetMiners) {
                p.setOccupation(Occupation.MINER);
            } else if (assigned < targetFarmers + targetLumberjacks + targetMiners + targetBlacksmiths) {
                p.setOccupation(Occupation.BLACKSMITH);
            } else if (assigned < targetFarmers + targetLumberjacks + targetMiners + targetBlacksmiths + targetBuilders) {
                p.setOccupation(Occupation.BUILDER);
            } else if (assigned < targetFarmers + targetLumberjacks + targetMiners + targetBlacksmiths + targetBuilders + targetSoldiers) {
                p.setOccupation(Occupation.SOLDIER);
            } else {
                p.setOccupation(Occupation.MERCHANT);
            }
            assigned++;
        }
    }

    public void buildHousingIfNeeded(Village village, int capacityPerBuilding) {
        int livingPop = village.getLivingPopulationCount();
        int buildersCount = village.getOccupationCount(Occupation.BUILDER);
        if (buildersCount <= 0) return;

        int capacity = village.getTotalHousingCapacity(capacityPerBuilding);
        boolean canBuild = !village.getTier().hasHousingCap() || (village.getHousingCount() < village.getInfrastructureSlots());
        if (livingPop > capacity && canBuild) {
            int neededHouses = (int) Math.ceil((double) (livingPop - capacity) / capacityPerBuilding);
            int maxHouses = Math.min(neededHouses, Math.max(1, buildersCount / 2));
            if (village.getTier().hasHousingCap()) {
                maxHouses = Math.min(maxHouses, village.getInfrastructureSlots() - village.getHousingCount());
            }

            double costPerHouseGold = 40.0;
            double costPerHouseWood = 4.0;
            int woodAffordable = (int) Math.floor(village.getWoodStockpile() / costPerHouseWood);
            maxHouses = Math.min(maxHouses, woodAffordable);
            if (maxHouses <= 0) return;

            int built = 0;
            for (int i = 0; i < maxHouses; i++) {
                boolean funded = false;
                if (village.getTreasury() >= costPerHouseGold) {
                    village.deductTreasury(costPerHouseGold);
                    funded = true;
                } else {
                    Person sponsor = null;
                    for (Person p : village.getResidents()) {
                        if (p.isAlive() && p.getAge() >= 19) {
                            if (sponsor == null || p.getWealth() > sponsor.getWealth()) {
                                sponsor = p;
                            }
                        }
                    }
                    if (sponsor != null && sponsor.getWealth() >= costPerHouseGold * 0.5) {
                        double payment = Math.min(sponsor.getWealth(), costPerHouseGold);
                        sponsor.deductWealth(payment);
                        double shortfall = costPerHouseGold - payment;
                        if (shortfall <= 0 || village.getBank().issueLoan(shortfall)) {
                            if (shortfall > 0) sponsor.setLoanDebt(sponsor.getLoanDebt() + shortfall);
                            funded = true;
                        }
                    }
                }

                if (funded) {
                    village.deductWood(costPerHouseWood);
                    built++;
                    village.getHousing().addHousing(1);
                } else {
                    break;
                }
            }

            if (built > 0) {
                double totalWages = built * costPerHouseGold;
                double wagePerBuilder = totalWages / buildersCount;
                for (Person p : village.getResidents()) {
                    if (p.isAlive() && p.getOccupation() == Occupation.BUILDER) {
                        p.addWealth(wagePerBuilder);
                    }
                }
            }
        }
    }

    public double collectAnnualTaxes(Village village, double taxRate) {
        double collected = 0.0;
        for (Person p : village.getResidents()) {
            if (p.isAlive() && p.getAge() >= 19 && p.getWealth() > 20.0) {
                double taxable = p.getWealth() - 20.0;
                double tax = taxable * taxRate;
                if (p.deductWealth(tax)) {
                    village.addTreasury(tax);
                    collected += tax;
                }
            }
        }
        return collected;
    }

    public boolean investInTechnology(Village village, double baseTechCost) {
        double requiredCost = baseTechCost * (village.getTechLevel() + 1);
        if (village.getTreasury() >= requiredCost + 40.0) {
            village.deductTreasury(requiredCost);
            village.incrementTechLevel();
            return true;
        }
        return false;
    }

    public void processAnnualBankingAndWealth(Village village) {
        Bank bank = village.getBank();
        double rate = bank.getInterestRate();
        double loanRatio = bank.getTotalDeposits() > 0 ? (bank.getTotalLoansOutstanding() / bank.getTotalDeposits()) : 0.0;
        double effectiveDepositRate = Math.min(rate * 0.35, Math.max(0.005, rate * loanRatio));

        for (Person p : village.getResidents()) {
            if (!p.isAlive()) continue;

            if (p.getBankBalance() > 0) {
                double interest = p.getBankBalance() * effectiveDepositRate;
                p.setBankBalance(p.getBankBalance() + interest);
                bank.deposit(interest);
            }

            if (p.getLoanDebt() > 0) {
                double loanInterest = p.getLoanDebt() * rate;
                p.setLoanDebt(p.getLoanDebt() + loanInterest);
                bank.issueLoan(loanInterest);

                double availableForRepayment = Math.max(0.0, p.getWealth() - 15.0);
                double payment = Math.min(p.getLoanDebt(), availableForRepayment);
                if (payment > 0 && p.deductWealth(payment)) {
                    p.setLoanDebt(p.getLoanDebt() - payment);
                    bank.repayLoan(payment);
                }
            }

            if (p.getWealth() > 40.0) {
                double toDeposit = (p.getWealth() - 40.0) * 0.20;
                if (p.deductWealth(toDeposit)) {
                    p.setBankBalance(p.getBankBalance() + toDeposit);
                    bank.deposit(toDeposit);
                }
            }

            // Luxury spending for affluent citizens
            if (p.getWealth() > 100.0) {
                double lifestyleSpending = (p.getWealth() - 100.0) * 0.05;
                p.deductWealth(lifestyleSpending);
                village.addTreasury(lifestyleSpending * 0.15); // Luxury sales tax
            }
        }
    }
}
