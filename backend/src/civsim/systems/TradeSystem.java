package civsim.systems;

import civsim.SimulationConfig;
import civsim.model.DiplomaticStatus;
import civsim.model.Occupation;
import civsim.model.Village;
import civsim.spatial.Biome;
import civsim.spatial.CaravanRoute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Handles inter-settlement commercial trade caravans across 4 physical resources
 * (Food, Iron/Ore, Tools/Weapons, and Timber) driven by biome comparative advantage,
 * merchant fleet capacity, 2D spatial routing, intermediate waypoint hubs, and transit tariffs.
 */
public class TradeSystem {

    private interface ExporterAction { double execute(Village exp, double qty, double rev); }
    private interface ImporterAction { boolean execute(Village imp, double qty, double cost); }
    private interface PriceEvaluator { double evaluate(Village exp, Village imp); }

    public void processInterSettlementTrade(
            List<Village> villages,
            int currentYear,
            Random random,
            SimulationConfig config,
            BiConsumer<String, Boolean> eventRecorder) {

        if (villages.size() < 2) return;

        Map<Integer, Integer> caravansCount = new HashMap<>();

        // 1. Food: River Valley & Coastal export agricultural surplus to Highland Mines & Forest
        executeResourceTrade(villages, currentYear, "Food",
                this::getFoodSurplus,
                this::getFoodNeed,
                (exp, qty, rev) -> exp.exportFood(qty, rev),
                (imp, qty, cost) -> imp.importFood(qty, cost),
                (exp, imp) -> (exp.getMarket().getFoodPrice() + imp.getMarket().getFoodPrice()) / 2.0,
                50.0, caravansCount, random, config, eventRecorder);

        // 2. Iron & Stone Ore: Highland Mines export mineral ore to River Valley & Coastal
        executeResourceTrade(villages, currentYear, "Iron/Ore",
                this::getOreSurplus,
                this::getOreNeed,
                (exp, qty, rev) -> exp.exportOre(qty, rev),
                (imp, qty, cost) -> imp.importOre(qty, cost),
                (exp, imp) -> 1.8,
                35.0, caravansCount, random, config, eventRecorder);

        // 3. Weapons & Tools: Industrial / Highland settlements export tools to frontier & agrarian settlements
        executeResourceTrade(villages, currentYear, "Tools/Weapons",
                this::getToolsSurplus,
                this::getToolsNeed,
                (exp, qty, rev) -> exp.exportTools(qty, rev),
                (imp, qty, cost) -> imp.importTools(qty, cost),
                (exp, imp) -> 3.2,
                25.0, caravansCount, random, config, eventRecorder);

        // 4. Timber: Forest settlements export timber to Highland Mines & River Valley
        executeResourceTrade(villages, currentYear, "Timber",
                this::getWoodSurplus,
                this::getWoodNeed,
                (exp, qty, rev) -> exp.exportWood(qty, rev),
                (imp, qty, cost) -> imp.importWood(qty, cost),
                (exp, imp) -> 1.2,
                40.0, caravansCount, random, config, eventRecorder);
    }

    private double getFoodSurplus(Village v) {
        int pop = v.getLivingPopulationCount();
        if (pop == 0) return 0.0;
        double reserve = (v.getBiome() == Biome.RIVER_VALLEY) ? (pop * 6.0) :
                         (v.getBiome() == Biome.COASTAL) ? (pop * 7.0) : (pop * 9.0);
        return Math.max(0.0, v.getFoodStockpile() - reserve);
    }

    private double getFoodNeed(Village v) {
        int pop = v.getLivingPopulationCount();
        if (pop == 0) return 0.0;
        double desired = (v.getBiome() == Biome.HIGHLAND_MINE) ? (pop * 12.0) :
                         (v.getBiome() == Biome.FOREST) ? (pop * 10.0) : (pop * 8.0);
        return Math.max(0.0, desired - v.getFoodStockpile());
    }

    private double getOreSurplus(Village v) {
        int pop = v.getLivingPopulationCount();
        if (pop == 0) return 0.0;
        double reserve = (v.getBiome() == Biome.HIGHLAND_MINE) ? (pop * 1.0 + 20.0) : (pop * 2.5 + 40.0);
        return Math.max(0.0, v.getIronStoneStockpile() - reserve);
    }

    private double getOreNeed(Village v) {
        int pop = v.getLivingPopulationCount();
        if (pop == 0) return 0.0;
        double desired = (v.getBiome() == Biome.RIVER_VALLEY) ? (pop * 3.5 + 50.0) :
                         (v.getBiome() == Biome.COASTAL) ? (pop * 3.0 + 40.0) : (pop * 2.0 + 30.0);
        return Math.max(0.0, desired - v.getIronStoneStockpile());
    }

    private double getToolsSurplus(Village v) {
        int pop = v.getLivingPopulationCount();
        if (pop == 0) return 0.0;
        double reserve = (v.getBiome() == Biome.HIGHLAND_MINE || v.getTechLevel() >= 10) ? (pop * 0.5 + 15.0) : (pop * 1.0 + 25.0);
        return Math.max(0.0, v.getToolsStockpile() - reserve);
    }

    private double getToolsNeed(Village v) {
        int pop = v.getLivingPopulationCount();
        if (pop == 0) return 0.0;
        double desired = pop * 1.2 + 25.0;
        return Math.max(0.0, desired - v.getToolsStockpile());
    }

    private double getWoodSurplus(Village v) {
        int pop = v.getLivingPopulationCount();
        if (pop == 0) return 0.0;
        double reserve = (v.getBiome() == Biome.FOREST) ? (pop * 2.0 + 30.0) : (pop * 4.0 + 60.0);
        return Math.max(0.0, v.getWoodStockpile() - reserve);
    }

    private double getWoodNeed(Village v) {
        int pop = v.getLivingPopulationCount();
        if (pop == 0) return 0.0;
        double desired = (v.getBiome() == Biome.HIGHLAND_MINE || v.getBiome() == Biome.RIVER_VALLEY) ? (pop * 5.0 + 80.0) : (pop * 3.5 + 50.0);
        return Math.max(0.0, desired - v.getWoodStockpile());
    }

    private void executeResourceTrade(
            List<Village> villages,
            int currentYear,
            String resourceName,
            Function<Village, Double> surplusCalc,
            Function<Village, Double> needCalc,
            ExporterAction exporterAction,
            ImporterAction importerAction,
            PriceEvaluator priceEval,
            double maxTradeCap,
            Map<Integer, Integer> caravansCount,
            Random random,
            SimulationConfig config,
            BiConsumer<String, Boolean> eventRecorder) {

        List<Village> exporters = new ArrayList<>();
        for (Village v : villages) {
            if (v.getLivingPopulationCount() > 0 && surplusCalc.apply(v) > 5.0) {
                exporters.add(v);
            }
        }

        List<Village> importers = new ArrayList<>();
        for (Village v : villages) {
            if (v.getLivingPopulationCount() > 0 && needCalc.apply(v) > 5.0) {
                importers.add(v);
            }
        }

        if (exporters.isEmpty() || importers.isEmpty()) return;

        exporters.sort((a, b) -> Double.compare(surplusCalc.apply(b), surplusCalc.apply(a)));
        importers.sort((a, b) -> Double.compare(needCalc.apply(b), needCalc.apply(a)));

        for (Village exp : exporters) {
            int expCap = Math.max(1, exp.getOccupationCount(Occupation.MERCHANT) + exp.getTier().ordinal() + 1);
            if (caravansCount.getOrDefault(exp.getId(), 0) >= expCap) continue;

            for (Village imp : importers) {
                if (exp == imp) continue;
                if (exp.getRelationWith(imp.getId()) == DiplomaticStatus.AT_WAR) continue;

                int impCap = Math.max(1, imp.getOccupationCount(Occupation.MERCHANT) + imp.getTier().ordinal() + 1);
                if (caravansCount.getOrDefault(imp.getId(), 0) >= impCap) continue;

                double surplus = surplusCalc.apply(exp);
                double need = needCalc.apply(imp);
                if (surplus < 5.0 || need < 5.0) continue;

                double unitPrice = priceEval.evaluate(exp, imp);
                double liquidPurchasingPower = (imp.getTotalLiquidWealth() * 0.35) + (imp.getTreasury() * 0.40);
                if (liquidPurchasingPower < unitPrice) continue;

                double maxExportable = Math.min(maxTradeCap, surplus * 0.40);
                double maxAffordable = liquidPurchasingPower / unitPrice;
                double tradeUnits = Math.min(maxExportable, Math.min(need, maxAffordable));

                if (tradeUnits < 1.0) continue;

                CaravanRoute route = CaravanRoute.planRoute(exp, imp, villages);
                double transportCost = route.calculateTransportCost(tradeUnits, unitPrice);
                double cargoCost = tradeUnits * unitPrice;
                double totalCost = cargoCost + transportCost;

                if (importerAction.execute(imp, tradeUnits, totalCost)) {
                    exporterAction.execute(exp, tradeUnits, cargoCost);

                    if (route.hasWaypoints()) {
                        double tariffPerStop = route.getWaypointTariffPerStop(cargoCost);
                        for (Village wp : route.getWaypoints()) {
                            wp.collectWaypointTariff(tariffPerStop);
                        }
                    }

                    if (exp.getRelationWith(imp.getId()) == DiplomaticStatus.NEUTRAL && random.nextDouble() < 0.35) {
                        exp.setRelationWith(imp.getId(), DiplomaticStatus.ALLIED);
                        imp.setRelationWith(exp.getId(), DiplomaticStatus.ALLIED);
                    }

                    caravansCount.put(exp.getId(), caravansCount.getOrDefault(exp.getId(), 0) + 1);
                    caravansCount.put(imp.getId(), caravansCount.getOrDefault(imp.getId(), 0) + 1);

                    if (eventRecorder != null) {
                        if (route.hasWaypoints()) {
                            String hubNames = route.getWaypoints().stream().map(Village::getName).collect(Collectors.joining(", "));
                            eventRecorder.accept(
                                    String.format("Year %d: Trade Caravan shipped %.1f %s from %s to %s via waypoint hub %s (dist %.1f km, travel %d mo, transport %.1fg).",
                                            currentYear, tradeUnits, resourceName, exp.getName(), imp.getName(), hubNames,
                                            route.getTotalDistance(), route.getTravelTimeMonths(), transportCost),
                                    config.isPrintIndividualEvents()
                            );
                        } else {
                            eventRecorder.accept(
                                    String.format("Year %d: Trade Caravan shipped %.1f %s from %s to %s across direct wilderness (dist %.1f km, travel %d mo, transport %.1fg).",
                                            currentYear, tradeUnits, resourceName, exp.getName(), imp.getName(),
                                            route.getTotalDistance(), route.getTravelTimeMonths(), transportCost),
                                    config.isPrintIndividualEvents()
                            );
                        }
                    }
                    break;
                }
            }
        }
    }
}
