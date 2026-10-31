package civsim.runner;

import civsim.SimulationEngine;
import civsim.dto.*;
import civsim.model.*;
import civsim.systems.CombatEngine;
import civsim.systems.MonsterEcologySystem;
import civsim.systems.OrganizationSystem;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds deeply immutable DTOs and snapshots from the live simulation engine state.
 * Executed exclusively on the simulation worker thread.
 */
public final class SnapshotBuilder {

    private SnapshotBuilder() {}

    public static WorldTickSnapshot buildTickSnapshot(SimulationEngine engine, long version, String status, String worldId) {
        int year = engine.getTimeline().getYear();
        int month = engine.getTimeline().getMonth();
        long totalMonths = engine.getTimeline().getTotalElapsedMonths();

        int humanPop = engine.getTotalLivingPopulation();
        int monsterCount = engine.getMonsterEcologySystem().getTotalLivingMonsterCount();
        int lairCount = engine.getMonsterEcologySystem().getActiveLairs().size();
        int orgCount = engine.getOrganizationSystem().getActiveOrganizations().size();

        // Economy
        double food = 0, wood = 0, ore = 0, tools = 0, wealth = 0, treas = 0;
        List<SettlementMarkerDto> settlementMarkers = new ArrayList<>();
        List<Village> villages = engine.getVillages();

        for (Village v : villages) {
            food += v.getFoodStockpile();
            wood += v.getWoodStockpile();
            ore += v.getOreStockpile();
            tools += v.getToolsStockpile();
            wealth += v.getTotalCitizenWealth();
            treas += v.getTreasury();

            settlementMarkers.add(new SettlementMarkerDto(
                    v.getId(),
                    v.getName(),
                    v.getX(),
                    v.getY(),
                    v.getBiome() != null ? v.getBiome().name() : "RIVER_VALLEY",
                    v.getTier() != null ? v.getTier().name() : "HAMLET",
                    v.getLivingPopulationCount(),
                    v.isInFamine() ? "FAMINE" : "HEALTHY"
            ));
        }

        WorldEconomySummaryDto economy = new WorldEconomySummaryDto(
                round(food), round(wood), round(ore), round(tools), round(wealth), round(treas), 0.0
        );

        // Lairs
        List<LairMarkerDto> lairMarkers = new ArrayList<>();
        for (MonsterLair lair : engine.getMonsterEcologySystem().getActiveLairs()) {
            lairMarkers.add(new LairMarkerDto(
                    lair.getLairId(),
                    lair.getPosX(),
                    lair.getPosY(),
                    lair.getBiome() != null ? lair.getBiome().name() : "FOREST",
                    lair.getDominantThreatRank() != null ? lair.getDominantThreatRank().name() : "COMMON",
                    lair.getState() != null ? lair.getState().name() : "ACTIVE",
                    lair.getMonsterCount(),
                    lair.getTerritoryRadius()
            ));
        }

        // Anomalies (only publicly observable or discovered)
        List<AnomalyMarkerDto> anomalyMarkers = new ArrayList<>();
        for (WorldAnomaly anom : engine.getAnomalySystem().getAllAnomalies()) {
            if (!anom.isPubliclyObservable()) {
                continue;
            }
            anomalyMarkers.add(new AnomalyMarkerDto(
                    anom.getAnomalyId(),
                    anom.getPosX(),
                    anom.getPosY(),
                    anom.getState() != null ? anom.getState().name() : "ACTIVE",
                    anom.getResourceCategory() != null ? anom.getResourceCategory() : "AETHER_DEPOSIT",
                    anom.getResourceGrade() != null ? anom.getResourceGrade().name() : "COMMON",
                    round(anom.getRemainingQuantity()),
                    anom.isPubliclyObservable()
            ));
        }

        // Recent events
        List<String> eventHistory = engine.getEventHistory();
        int takeEvents = Math.min(10, eventHistory.size());
        List<String> recentEvents = (takeEvents > 0)
                ? eventHistory.subList(eventHistory.size() - takeEvents, eventHistory.size())
                : List.of();

        return new WorldTickSnapshot(
                worldId,
                version,
                totalMonths,
                totalMonths,
                year,
                month,
                status,
                year >= engine.getConfig().getAwakeningStartYear(),
                engine.getConfig().getAwakeningStartYear(),
                humanPop,
                monsterCount,
                villages.size(),
                orgCount,
                lairCount,
                economy,
                settlementMarkers,
                lairMarkers,
                anomalyMarkers,
                recentEvents
        );
    }

    public static WorldSummaryDto buildSummary(SimulationEngine engine, String status, String worldId) {
        int year = engine.getTimeline().getYear();
        int month = engine.getTimeline().getMonth();
        long totalMonths = engine.getTimeline().getTotalElapsedMonths();

        int humanPop = 0;
        int males = 0;
        int females = 0;
        int awakened = 0;
        double food = 0, wood = 0, ore = 0, tools = 0, wealth = 0, treas = 0;

        for (Person p : engine.getAllPeople()) {
            if (p.isAlive()) {
                humanPop++;
                if (p.getGender() == Gender.MALE) males++;
                else females++;
                if (p.isAwakened()) awakened++;
                wealth += p.getWealth();
            }
        }

        for (Village v : engine.getVillages()) {
            food += v.getFoodStockpile();
            wood += v.getWoodStockpile();
            ore += v.getOreStockpile();
            tools += v.getToolsStockpile();
            treas += v.getTreasury();
        }

        String era = year < engine.getConfig().getAwakeningStartYear() ? "Mundane Era" : "Awakening Era";

        return new WorldSummaryDto(
                worldId,
                year,
                month,
                totalMonths,
                status,
                humanPop,
                males,
                females,
                engine.getVillages().size(),
                engine.getOrganizationSystem().getActiveOrganizations().size(),
                awakened,
                engine.getMonsterEcologySystem().getTotalLivingMonsterCount(),
                engine.getMonsterEcologySystem().getActiveLairs().size(),
                round(food),
                round(wood),
                round(ore),
                round(tools),
                round(wealth),
                round(treas),
                era
        );
    }

    public static WorldMapSnapshot buildMapSnapshot(SimulationEngine engine, long version, String worldId) {
        int year = engine.getTimeline().getYear();
        int month = engine.getTimeline().getMonth();
        long totalMonths = engine.getTimeline().getTotalElapsedMonths();

        List<MapSettlementDto> settlements = new ArrayList<>();
        for (Village v : engine.getVillages()) {
            settlements.add(new MapSettlementDto(
                    v.getId(),
                    v.getName(),
                    v.getX(),
                    v.getY(),
                    v.getBiome() != null ? v.getBiome().name() : "RIVER_VALLEY",
                    v.getTier() != null ? v.getTier().name() : "HAMLET",
                    v.getLivingPopulationCount(),
                    v.getFortificationLevel(),
                    v.getTechLevel(),
                    v.getLeader() != null ? v.getLeader().getName() : "None",
                    v.isInFamine()
            ));
        }

        List<MapLairDto> lairs = new ArrayList<>();
        for (MonsterLair lair : engine.getMonsterEcologySystem().getAllLairs()) {
            lairs.add(new MapLairDto(
                    lair.getLairId(),
                    lair.getSourceAnomalyId(),
                    lair.getPosX(),
                    lair.getPosY(),
                    lair.getBiome() != null ? lair.getBiome().name() : "FOREST",
                    lair.getDominantThreatRank() != null ? lair.getDominantThreatRank().name() : "COMMON",
                    lair.getState() != null ? lair.getState().name() : "ACTIVE",
                    lair.getMonsterCount(),
                    lair.getPopulationCapacity(),
                    lair.getTerritoryRadius(),
                    lair.getPrimarySpeciesId(),
                    lair.getSecondarySpeciesId()
            ));
        }

        List<MapAnomalyDto> anomalies = new ArrayList<>();
        for (WorldAnomaly anom : engine.getAnomalySystem().getAllAnomalies()) {
            if (!anom.isPubliclyObservable()) {
                continue;
            }
            anomalies.add(new MapAnomalyDto(
                    anom.getAnomalyId(),
                    anom.getPosX(),
                    anom.getPosY(),
                    anom.getState() != null ? anom.getState().name() : "ACTIVE",
                    anom.getResourceCategory() != null ? anom.getResourceCategory() : "AETHER_DEPOSIT",
                    anom.getResourceGrade() != null ? anom.getResourceGrade().name() : "COMMON",
                    round(anom.getInitialQuantity()),
                    round(anom.getRemainingQuantity()),
                    anom.isPubliclyObservable(),
                    anom.getControllingOwnerId(),
                    anom.getSecuringRemainingMonths()
            ));
        }

        List<MapExpeditionDto> expeditions = new ArrayList<>();
        for (Expedition exp : engine.getAnomalySystem().getActiveExpeditions()) {
            expeditions.add(new MapExpeditionDto(
                    exp.getExpeditionId(),
                    exp.getOriginVillageId(),
                    exp.getTargetAnomalyId(),
                    exp.getCurrentX(),
                    exp.getCurrentY(),
                    exp.getStatus() != null ? exp.getStatus().name() : "EN_ROUTE",
                    exp.getPersonnel().size(),
                    round(exp.getCarriedCargo())
            ));
        }

        return new WorldMapSnapshot(
                worldId,
                version,
                totalMonths,
                year,
                month,
                120.0,
                120.0,
                settlements,
                lairs,
                anomalies,
                expeditions
        );
    }

    public static SettlementDetailDto buildSettlementDetail(Village v, List<Village> allVillages) {
        if (v == null) return null;
        List<String> facilities = v.getCraftingFacilities().values().stream()
                .map(f -> f.getFacilityId() + " (" + f.getGrade() + ")")
                .collect(Collectors.toList());

        return new SettlementDetailDto(
                v.getId(),
                v.getName(),
                v.getX(),
                v.getY(),
                v.getBiome() != null ? v.getBiome().name() : "RIVER_VALLEY",
                v.getTier() != null ? v.getTier().name() : "HAMLET",
                v.getLivingPopulationCount(),
                v.getLivingMaleCount(),
                v.getLivingFemaleCount(),
                v.getLeader() != null ? v.getLeader().getId() : null,
                v.getLeader() != null ? v.getLeader().getName() : "None",
                round(v.getFoodStockpile()),
                round(v.getWoodStockpile()),
                round(v.getOreStockpile()),
                round(v.getToolsStockpile()),
                round(v.getTreasury()),
                v.getTechLevel(),
                round(v.getFortificationLevel()),
                v.getTotalHousingCapacity(8),
                v.getPeakPopulation(),
                v.getMonsterCoreCount(),
                v.isInFamine(),
                v.buildDiplomacyAndRaidStatus(allVillages),
                facilities,
                v.getStoredEquipment().size()
        );
    }

    public static PersonSummaryDto buildPersonSummary(Person p) {
        if (p == null) return null;
        AetherProfile ap = p.getAetherProfile();
        return new PersonSummaryDto(
                p.getId(),
                p.getName(),
                p.getGender().name(),
                p.getAge(),
                p.isAlive(),
                p.getBirthYear(),
                p.getDeathYear(),
                p.getVillage() != null ? p.getVillage().getName() : "None",
                p.getOccupation() != null ? p.getOccupation().name() : "UNEMPLOYED",
                round(p.getWealth()),
                p.isAwakened(),
                (ap != null && ap.getSupernaturalClass() != null) ? ap.getSupernaturalClass().name() : "NONE",
                (ap != null && ap.getAspect() != null) ? ap.getAspect().name() : "NONE",
                (ap != null && ap.getRarity() != null) ? ap.getRarity().name() : "NONE",
                (ap != null) ? ap.getPowerTier() : 0,
                (ap != null) ? round(ap.getDerivedPower()) : 0.0
        );
    }

    public static PersonDetailDto buildPersonDetail(Person p, OrganizationSystem orgSys) {
        if (p == null) return null;
        AetherProfile ap = p.getAetherProfile();

        Map<String, String> items = new LinkedHashMap<>();
        for (Map.Entry<EquipmentSlot, String> e : p.getEquippedItemIds().entrySet()) {
            items.put(e.getKey().name(), e.getValue());
        }

        List<Integer> childIds = p.getChildren().stream().map(Person::getId).collect(Collectors.toList());

        List<String> orgMemberships = new ArrayList<>();
        if (orgSys != null) {
            for (Organization org : orgSys.getAllOrganizations()) {
                if (org.getMembership(p.getId()) != null) {
                    orgMemberships.add(org.getDisplayName() + " (" + org.getMembership(p.getId()).getRole() + ")");
                }
            }
        }

        return new PersonDetailDto(
                p.getId(),
                p.getName(),
                p.getGender().name(),
                p.getAge(),
                p.isAlive(),
                p.getBirthYear(),
                p.getDeathYear(),
                p.getAgeAtDeath(),
                p.getCauseOfDeath(),
                p.getVillage() != null ? p.getVillage().getId() : null,
                p.getVillage() != null ? p.getVillage().getName() : "None",
                p.getOccupation() != null ? p.getOccupation().name() : "UNEMPLOYED",
                round(p.getWealth()),
                round(p.getBankBalance()),
                round(p.getLoanDebt()),
                p.isAwakened(),
                (ap != null && ap.getSupernaturalClass() != null) ? ap.getSupernaturalClass().name() : "NONE",
                (ap != null && ap.getAspect() != null) ? ap.getAspect().name() : "NONE",
                (ap != null && ap.getRarity() != null) ? ap.getRarity().name() : "NONE",
                (ap != null) ? ap.getPowerTier() : 0,
                (ap != null) ? round(ap.getMastery()) : 0.0,
                (ap != null) ? round(ap.getLuck()) : 0.0,
                (ap != null) ? round(ap.getGreed()) : 0.0,
                (ap != null) ? round(ap.getDarkResonance()) : 0.0,
                (ap != null) ? round(ap.getDerivedPower()) : 0.0,
                items,
                p.getFamily() != null ? p.getFamily().getId() : null,
                p.getFamily() != null ? p.getFamily().getSurname() : "None",
                p.getFather() != null ? p.getFather().getId() : null,
                p.getFather() != null ? p.getFather().getName() : "None",
                p.getMother() != null ? p.getMother().getId() : null,
                p.getMother() != null ? p.getMother().getName() : "None",
                p.getSpouse() != null ? p.getSpouse().getId() : null,
                p.getSpouse() != null ? p.getSpouse().getName() : "None",
                childIds,
                orgMemberships
        );
    }

    public static OrganizationSummaryDto buildOrganizationSummary(Organization org) {
        if (org == null) return null;
        return new OrganizationSummaryDto(
                org.getOrganizationId(),
                org.getDisplayName(),
                org.getOrganizationType() != null ? org.getOrganizationType().name() : "CLAN",
                org.getAlignment() != null ? org.getAlignment().name() : "NEUTRAL",
                org.getStatus() != null ? org.getStatus().name() : "ACTIVE",
                org.getFoundingYear(),
                org.getCurrentLeaderPersonId(),
                "Leader-" + org.getCurrentLeaderPersonId(),
                round(org.getReputation()),
                org.getMemberCount(),
                org.getHeirlooms().size(),
                org.getBattlesWon(),
                org.getBattlesParticipated()
        );
    }

    public static OrganizationDetailDto buildOrganizationDetail(Organization org) {
        if (org == null) return null;
        List<String> heirloomNames = org.getHeirlooms().values().stream()
                .map(ClanHeirloom::getCustomName)
                .collect(Collectors.toList());

        List<String> feudIds = new ArrayList<>(org.getActiveFeudIds());

        List<String> events = org.getEventHistory().stream()
                .map(e -> String.format("Y%d M%d: %s - %s", e.getYear(), e.getMonth(), e.getEventType(), e.getDescription()))
                .collect(Collectors.toList());

        return new OrganizationDetailDto(
                org.getOrganizationId(),
                org.getDisplayName(),
                org.getOrganizationType() != null ? org.getOrganizationType().name() : "CLAN",
                org.getAlignment() != null ? org.getAlignment().name() : "NEUTRAL",
                org.getStatus() != null ? org.getStatus().name() : "ACTIVE",
                org.getFoundingYear(),
                org.getFoundingMonth(),
                org.getFounderPersonId(),
                org.getCurrentLeaderPersonId(),
                "Leader-" + org.getCurrentLeaderPersonId(),
                round(org.getReputation()),
                org.getMemberCount(),
                org.getHeirlooms().size(),
                heirloomNames,
                org.getBattlesWon(),
                org.getBattlesParticipated(),
                org.getMemberCasualties(),
                feudIds,
                events
        );
    }

    public static BattleDetailDto buildBattleDetail(BattleResult r) {
        if (r == null) return null;
        return new BattleDetailDto(
                r.getBattleId(),
                r.getSimulationYear(),
                r.getSimulationMonth(),
                r.getBattleType() != null ? r.getBattleType().name() : "SKIRMISH",
                r.getOutcome() != null ? r.getOutcome().name() : "VICTORY",
                r.getVictoriousSideId(),
                r.getTotalRounds(),
                r.getTotalInjuriesInflicted(),
                r.getInitialCountsPerSide(),
                r.getFinalLivingCountsPerSide(),
                r.getDamageDealtPerSide(),
                r.getFatalities(),
                r.getIncapacitated(),
                r.getEscaped(),
                r.getSummaryText()
        );
    }

    public static LeaderboardDto buildLeaderboard(SimulationEngine engine, String category) {
        String cat = (category != null) ? category.toUpperCase().trim() : "POWER";
        List<LeaderboardEntryDto> entries = new ArrayList<>();
        int year = engine.getTimeline().getYear();
        int month = engine.getTimeline().getMonth();

        switch (cat) {
            case "TIER":
            case "POWER": {
                List<Person> sorted = engine.getAllPeople().stream()
                        .filter(Person::isAlive)
                        .filter(Person::isAwakened)
                        .sorted((a, b) -> Double.compare(
                                b.getAetherProfile().getDerivedPower(),
                                a.getAetherProfile().getDerivedPower()
                        ))
                        .limit(20)
                        .toList();

                int rank = 1;
                for (Person p : sorted) {
                    AetherProfile ap = p.getAetherProfile();
                    entries.add(new LeaderboardEntryDto(
                            rank++,
                            String.valueOf(p.getId()),
                            p.getName(),
                            round(ap.getDerivedPower()),
                            String.format("T%d %s (%s)", ap.getPowerTier(), ap.getSupernaturalClass(), ap.getRarity()),
                            "Mastery: " + round(ap.getMastery()) + " | Age: " + p.getAge()
                    ));
                }
                break;
            }
            case "WEALTH": {
                List<Person> sorted = engine.getAllPeople().stream()
                        .filter(Person::isAlive)
                        .sorted((a, b) -> Double.compare(b.getWealth(), a.getWealth()))
                        .limit(20)
                        .toList();

                int rank = 1;
                for (Person p : sorted) {
                    entries.add(new LeaderboardEntryDto(
                            rank++,
                            String.valueOf(p.getId()),
                            p.getName(),
                            round(p.getWealth()),
                            p.getOccupation() != null ? p.getOccupation().name() : "CITIZEN",
                            p.getVillage() != null ? p.getVillage().getName() : "None"
                    ));
                }
                break;
            }
            case "CLANS":
            case "ORGANIZATIONS": {
                List<Organization> sorted = engine.getOrganizationSystem().getActiveOrganizations().stream()
                        .sorted((a, b) -> Integer.compare(b.getMemberCount(), a.getMemberCount()))
                        .limit(20)
                        .toList();

                int rank = 1;
                for (Organization org : sorted) {
                    entries.add(new LeaderboardEntryDto(
                            rank++,
                            org.getOrganizationId(),
                            org.getDisplayName(),
                            org.getMemberCount(),
                            org.getOrganizationType() + " | " + org.getAlignment(),
                            "Heirlooms: " + org.getHeirlooms().size() + " | Battles Won: " + org.getBattlesWon()
                    ));
                }
                break;
            }
            case "SLAYERS":
            default: {
                // Default fallback to powerful individuals
                List<Person> sorted = engine.getAllPeople().stream()
                        .filter(Person::isAlive)
                        .sorted((a, b) -> Double.compare(b.getWealth() + (b.isAwakened() ? b.getAetherProfile().getDerivedPower() : 0),
                                a.getWealth() + (a.isAwakened() ? a.getAetherProfile().getDerivedPower() : 0)))
                        .limit(20)
                        .toList();

                int rank = 1;
                for (Person p : sorted) {
                    entries.add(new LeaderboardEntryDto(
                            rank++,
                            String.valueOf(p.getId()),
                            p.getName(),
                            round(p.isAwakened() ? p.getAetherProfile().getDerivedPower() : p.getWealth()),
                            p.isAwakened() ? "Awakened " + p.getAetherProfile().getSupernaturalClass() : "Mortal",
                            p.getVillage() != null ? p.getVillage().getName() : "None"
                    ));
                }
                break;
            }
        }

        return new LeaderboardDto(cat, year, month, entries);
    }

    public static List<SearchResultDto> searchEntities(SimulationEngine engine, String query, int limit) {
        if (query == null || query.isBlank()) return List.of();
        String q = query.trim().toLowerCase();
        int max = Math.max(1, Math.min(limit, 50));
        List<SearchResultDto> results = new ArrayList<>();

        // 1. Settlements
        for (Village v : engine.getVillages()) {
            if (v.getName().toLowerCase().contains(q) || ("settlement " + v.getId()).contains(q)) {
                results.add(new SearchResultDto(
                        String.valueOf(v.getId()),
                        v.getName(),
                        "SETTLEMENT",
                        v.getX(),
                        v.getY(),
                        v.getTier() + " (" + v.getBiome() + ")",
                        "Population: " + v.getLivingPopulationCount() + " | Tech: " + v.getTechLevel()
                ));
                if (results.size() >= max) return results;
            }
        }

        // 2. Monster Lairs
        for (MonsterLair lair : engine.getMonsterEcologySystem().getAllLairs()) {
            if (lair.getLairId().toLowerCase().contains(q)
                    || (lair.getPrimarySpeciesId() != null && lair.getPrimarySpeciesId().toLowerCase().contains(q))
                    || (lair.getDominantThreatRank() != null && lair.getDominantThreatRank().name().toLowerCase().contains(q))) {
                results.add(new SearchResultDto(
                        lair.getLairId(),
                        "Lair " + lair.getLairId() + " (" + lair.getPrimarySpeciesId() + ")",
                        "LAIR",
                        lair.getPosX(),
                        lair.getPosY(),
                        "Threat: " + lair.getDominantThreatRank() + " | State: " + lair.getState(),
                        "Monsters: " + lair.getMonsterCount() + " / " + lair.getPopulationCapacity()
                ));
                if (results.size() >= max) return results;
            }
        }

        // 3. Observable Anomalies
        for (WorldAnomaly anom : engine.getAnomalySystem().getAllAnomalies()) {
            if (!anom.isPubliclyObservable()) continue;
            if (anom.getAnomalyId().toLowerCase().contains(q)
                    || anom.getName().toLowerCase().contains(q)
                    || anom.getResourceGrade().name().toLowerCase().contains(q)) {
                results.add(new SearchResultDto(
                        anom.getAnomalyId(),
                        anom.getName(),
                        "ANOMALY",
                        anom.getPosX(),
                        anom.getPosY(),
                        anom.getResourceGrade() + " " + anom.getResourceCategory(),
                        "State: " + anom.getState() + " | Remaining: " + round(anom.getRemainingQuantity())
                ));
                if (results.size() >= max) return results;
            }
        }

        // 4. Organizations
        for (Organization org : engine.getOrganizationSystem().getActiveOrganizations()) {
            if (org.getDisplayName().toLowerCase().contains(q) || org.getOrganizationId().toLowerCase().contains(q)) {
                double x = 60.0, y = 60.0;
                String leaderIdStr = org.getCurrentLeaderPersonId();
                if (leaderIdStr != null) {
                    try {
                        int leaderId = Integer.parseInt(leaderIdStr);
                        Person leader = engine.getPerson(leaderId);
                        if (leader != null && leader.getVillage() != null) {
                            x = leader.getVillage().getX();
                            y = leader.getVillage().getY();
                        }
                    } catch (Exception ignored) {}
                }
                results.add(new SearchResultDto(
                        org.getOrganizationId(),
                        org.getDisplayName(),
                        "ORGANIZATION",
                        x,
                        y,
                        org.getOrganizationType() + " | " + org.getAlignment(),
                        "Members: " + org.getMemberCount() + " | Battles: " + org.getBattlesWon()
                ));
                if (results.size() >= max) return results;
            }
        }

        // 5. Living People
        for (Person p : engine.getAllPeople()) {
            if (!p.isAlive()) continue;
            if (p.getName().toLowerCase().contains(q) || String.valueOf(p.getId()).equals(q)) {
                double x = (p.getVillage() != null) ? p.getVillage().getX() : 60.0;
                double y = (p.getVillage() != null) ? p.getVillage().getY() : 60.0;
                results.add(new SearchResultDto(
                        String.valueOf(p.getId()),
                        p.getName(),
                        "PERSON",
                        x,
                        y,
                        p.isAwakened() ? "Awakened " + p.getAetherProfile().getSupernaturalClass() : "Mortal",
                        "Age: " + p.getAge() + " | Settlement: " + (p.getVillage() != null ? p.getVillage().getName() : "None")
                ));
                if (results.size() >= max) return results;
            }
        }

        return results;
    }

    private static double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
