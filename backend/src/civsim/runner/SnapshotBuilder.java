package civsim.runner;

import civsim.AnnualReport;
import civsim.SimulationConfig;
import civsim.SimulationEngine;
import civsim.Timeline;
import civsim.dto.*;
import civsim.model.*;
import civsim.systems.AnomalyExplorationSystem;
import civsim.systems.BloodFeudSystem;
import civsim.systems.ClanHeirloomSystem;
import civsim.systems.CombatEngine;
import civsim.systems.EquipmentCraftingSystem;
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
            if (!anom.isPubliclyObservable() || anom.getRemainingQuantity() <= 0.0) {
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
            int pop = v.getLivingPopulationCount();
            double radius = Math.max(8.0, 6.0 + Math.sqrt(Math.max(1, pop)) * 0.75);
            double milStrength = v.getFortificationLevel() * 10.0 + pop * 0.8 + v.getToolsStockpile() * 0.4;
            double prosperity = v.getTotalLiquidWealth() + v.getTreasury() + v.getFoodStockpile() * 0.1;

            double minThreatDist = 999.0;
            if (engine.getMonsterEcologySystem() != null) {
                for (MonsterLair lair : engine.getMonsterEcologySystem().getAllLairs()) {
                    if (lair.getState() == MonsterLairState.ACTIVE) {
                        double dx = v.getX() - lair.getPosX();
                        double dy = v.getY() - lair.getPosY();
                        double dist = Math.sqrt(dx * dx + dy * dy);
                        if (dist < minThreatDist) minThreatDist = dist;
                    }
                }
            }
            double threatScore = Math.max(0.0, Math.min(100.0, (60.0 - minThreatDist) * 2.0));

            Map<String, Integer> clanCounts = new HashMap<>();
            for (Person p : v.getResidents()) {
                if (p.isAlive() && p.getFamily() != null && p.getFamily().getSurname() != null && !p.getFamily().getSurname().isBlank()) {
                    clanCounts.merge(p.getFamily().getSurname(), 1, Integer::sum);
                }
            }
            String dominantClan = clanCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("Independent");

            List<Integer> alliedIds = new ArrayList<>();
            List<Integer> hostileIds = new ArrayList<>();
            for (Village other : engine.getVillages()) {
                if (other.getId() == v.getId()) continue;
                DiplomaticStatus status = v.getRelationWith(other.getId());
                if (status == DiplomaticStatus.ALLIED) alliedIds.add(other.getId());
                else if (status == DiplomaticStatus.AT_WAR) hostileIds.add(other.getId());
            }

            settlements.add(new MapSettlementDto(
                    v.getId(),
                    v.getName(),
                    v.getX(),
                    v.getY(),
                    v.getBiome() != null ? v.getBiome().name() : "RIVER_VALLEY",
                    v.getTier() != null ? v.getTier().name() : "HAMLET",
                    pop,
                    v.getFortificationLevel(),
                    v.getTechLevel(),
                    v.getLeader() != null ? v.getLeader().getName() : "None",
                    v.isInFamine(),
                    round(radius),
                    round(milStrength),
                    round(prosperity),
                    round(threatScore),
                    dominantClan,
                    alliedIds,
                    hostileIds
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
            if (!anom.isPubliclyObservable() || anom.getRemainingQuantity() <= 0.0) {
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
                    civsim.spatial.WorldGeography.WIDTH,
                    civsim.spatial.WorldGeography.HEIGHT,
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
        return buildPersonDetail(p, orgSys, null, null);
    }

    public static PersonDetailDto buildPersonDetail(Person p, OrganizationSystem orgSys, EquipmentCraftingSystem ecs, ClanHeirloomSystem chs) {
        if (p == null) return null;
        AetherProfile ap = p.getAetherProfile();

        Map<String, String> items = new LinkedHashMap<>();
        Map<String, EquipmentItemDto> equipDetails = new LinkedHashMap<>();
        for (Map.Entry<EquipmentSlot, String> e : p.getEquippedItemIds().entrySet()) {
            items.put(e.getKey().name(), e.getValue());
            if (ecs != null && e.getValue() != null) {
                EquipmentItem eq = ecs.getEquipment(e.getValue());
                if (eq != null) {
                    List<String> enchStrs = eq.getEnchantments().stream()
                            .map(en -> en.getFamily() + " (T" + en.getPotencyTier() + " +" + round(en.getPrimaryBonus()) + ")")
                            .collect(Collectors.toList());
                    equipDetails.put(e.getKey().name(), new EquipmentItemDto(
                            eq.getEquipmentId(),
                            eq.getDisplayName(),
                            e.getKey().name(),
                            eq.getType().name(),
                            eq.getCategory().name(),
                            eq.getEquipmentRarity().name(),
                            eq.getEquipmentTier(),
                            eq.getCondition().name(),
                            round(eq.getDurability()),
                            round(eq.getMaximumDurability()),
                            eq.getCreatorPersonId(),
                            eq.getCreationTotalMonths() / 12,
                            eq.isRegisteredHeirloom(),
                            eq.getHeirloomId(),
                            enchStrs
                    ));
                }
            }
        }

        Map<String, AttributeScoresDto> attrMap = new LinkedHashMap<>();
        for (AttributeType attr : AttributeType.values()) {
            double base = 0.0, awk = 0.0, dev = 0.0, temp = 0.0, eff = 0.0;
            if (ap != null) {
                base = ap.getBaselineAttribute(attr);
                awk = ap.getAwakeningBonus(attr);
                dev = ap.getDevelopmentBonus(attr);
                temp = ap.getTemporaryBonus(attr);
                eff = ap.getEffectiveAttributeDouble(attr);
            } else {
                eff = p.getEffectiveAttribute(attr);
                base = eff;
            }
            attrMap.put(attr.name(), new AttributeScoresDto(
                    attr.getDisplayName(),
                    round(base),
                    round(awk),
                    round(dev),
                    round(temp),
                    round(eff)
            ));
        }

        double patk = (ap != null) ? round(ap.getEffectiveAttributeDouble(AttributeType.STRENGTH) * 1.5) : 10.0;
        double pdef = (ap != null) ? round(ap.getEffectiveAttributeDouble(AttributeType.ENDURANCE) * 1.2) : 8.0;
        double acc = (ap != null) ? round(ap.getEffectiveAttributeDouble(AttributeType.PERCEPTION) * 1.1) : 10.0;
        double eva = (ap != null) ? round(ap.getEffectiveAttributeDouble(AttributeType.AGILITY) * 1.1) : 10.0;
        double pot = (ap != null && p.isAwakened()) ? round(ap.getEffectiveAttributeDouble(AttributeType.AETHER_CONTROL) * 2.0) : 0.0;
        double bar = (ap != null && p.isAwakened()) ? round(ap.getEffectiveAttributeDouble(AttributeType.AETHER_CAPACITY) * 1.5) : 0.0;

        double curHp = 100.0, maxHp = 100.0, curStam = 100.0, maxStam = 100.0;
        List<String> injuries = new ArrayList<>();
        if (p.getCombatState() != null) {
            curHp = round(p.getCombatState().getCurrentHealth());
            maxHp = round(p.getCombatState().getMaxHealth());
            curStam = round(p.getCombatState().getCurrentStamina());
            maxStam = round(p.getCombatState().getMaxStamina());
            if (p.getCombatState().getActiveInjuries() != null) {
                for (CombatInjury inj : p.getCombatState().getActiveInjuries()) {
                    injuries.add(inj.getDescription() + " (" + inj.getSeverity() + ")");
                }
            }
        }
        PersonCombatProfileDto combatProf = new PersonCombatProfileDto(
                patk, pdef, acc, eva, pot, bar, curHp, maxHp, curStam, maxStam, injuries
        );

        int maxTier = (ap != null && ap.getRarity() != null) ? ap.getRarity().getMaxPowerTier() : 0;
        int bAtt = (ap != null && ap.getProgressionRecord() != null) ? ap.getProgressionRecord().getTotalBreakthroughAttempts() : 0;
        int bSuc = (ap != null && ap.getProgressionRecord() != null) ? ap.getProgressionRecord().getTotalBreakthroughSuccesses() : 0;

        List<Integer> childIds = p.getChildren().stream().map(Person::getId).collect(Collectors.toList());

        List<String> orgMemberships = new ArrayList<>();
        if (orgSys != null) {
            for (Organization org : orgSys.getAllOrganizations()) {
                if (org.getMembership(p.getId()) != null) {
                    orgMemberships.add(org.getDisplayName() + " (" + org.getMembership(p.getId()).getRole() + ")");
                }
            }
        }

        List<String> achievements = new ArrayList<>();
        if (p.isAwakened() && ap != null) {
            achievements.add("Awakened as " + ap.getSupernaturalClass() + " (" + ap.getAspect() + ")");
            if (ap.getPowerTier() >= 3) {
                achievements.add("Reached Tier " + ap.getPowerTier() + " Realm");
            }
        }
        if (p.getAge() >= 65) {
            achievements.add("Venerable Elder of " + (p.getVillage() != null ? p.getVillage().getName() : "the realm"));
        }
        if (p.getWealth() >= 50.0) {
            achievements.add("Prosperous Citizen (" + round(p.getWealth()) + " Silver)");
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
                maxTier,
                (ap != null) ? round(ap.getMastery()) : 0.0,
                (ap != null) ? round(ap.getLuck()) : 0.0,
                (ap != null) ? round(ap.getGreed()) : 0.0,
                (ap != null) ? round(ap.getDarkResonance()) : 0.0,
                (ap != null) ? round(ap.getDerivedPower()) : 0.0,
                bAtt,
                bSuc,
                items,
                equipDetails,
                combatProf,
                attrMap,
                p.getFamily() != null ? p.getFamily().getId() : null,
                p.getFamily() != null ? p.getFamily().getSurname() : "None",
                p.getFather() != null ? p.getFather().getId() : null,
                p.getFather() != null ? p.getFather().getName() : "None",
                p.getMother() != null ? p.getMother().getId() : null,
                p.getMother() != null ? p.getMother().getName() : "None",
                p.getSpouse() != null ? p.getSpouse().getId() : null,
                p.getSpouse() != null ? p.getSpouse().getName() : "None",
                childIds,
                orgMemberships,
                achievements
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

    public static FamilySummaryDto buildFamilySummary(Family fam, List<Person> allPeople) {
        if (fam == null) return null;
        Person founder = fam.getOriginalFounder() != null ? fam.getOriginalFounder() : fam.getFounder();
        Integer founderId = founder != null ? founder.getId() : null;
        String founderName = founder != null ? founder.getName() : "Unknown";

        List<Person> members = fam.getAllHistoricalMembers();
        if (members.isEmpty()) members = fam.getMembers();

        Person mostInfluential = null;
        double maxPower = -1;
        Person highestTierPerson = null;
        int maxTier = 0;

        for (Person p : members) {
            double pwr = p.getWealth() + (p.isAwakened() && p.getAetherProfile() != null ? p.getAetherProfile().getDerivedPower() : 0);
            if (pwr > maxPower) {
                maxPower = pwr;
                mostInfluential = p;
            }
            if (p.isAwakened() && p.getAetherProfile() != null && p.getAetherProfile().getPowerTier() > maxTier) {
                maxTier = p.getAetherProfile().getPowerTier();
                highestTierPerson = p;
            }
        }

        int minBirth = founder != null ? founder.getBirthYear() : fam.getFoundedYear();
        int maxBirth = minBirth;
        for (Person p : members) {
            if (p.getBirthYear() > maxBirth) maxBirth = p.getBirthYear();
        }
        int genCount = Math.max(1, (maxBirth - minBirth) / 20 + 1);

        return new FamilySummaryDto(
                fam.getId(),
                fam.getSurname(),
                fam.getFoundedYear(),
                founderId,
                founderName,
                fam.getLivingMemberCount(),
                fam.getDeceasedMemberCount(),
                fam.getTotalRecordedLineageCount(),
                genCount,
                mostInfluential != null ? mostInfluential.getId() : null,
                mostInfluential != null ? mostInfluential.getName() : null,
                highestTierPerson != null ? highestTierPerson.getId() : null,
                highestTierPerson != null ? highestTierPerson.getName() : null,
                maxTier
        );
    }

    public static FamilyDetailDto buildFamilyDetail(Family fam, List<Person> allPeople, OrganizationSystem orgSys) {
        if (fam == null) return null;
        FamilySummaryDto summary = buildFamilySummary(fam, allPeople);
        List<PersonSummaryDto> living = fam.getMembers().stream()
                .filter(Person::isAlive)
                .map(SnapshotBuilder::buildPersonSummary)
                .collect(Collectors.toList());

        List<PersonSummaryDto> deceased = fam.getAllHistoricalMembers().stream()
                .filter(p -> !p.isAlive())
                .map(SnapshotBuilder::buildPersonSummary)
                .collect(Collectors.toList());

        Set<String> clanSet = new LinkedHashSet<>();
        if (orgSys != null) {
            for (Person p : fam.getAllHistoricalMembers()) {
                for (Organization org : orgSys.getAllOrganizations()) {
                    if (org.getMembership(p.getId()) != null) {
                        clanSet.add(org.getDisplayName());
                    }
                }
            }
        }

        return new FamilyDetailDto(summary, living, deceased, new ArrayList<>(clanSet));
    }

    public static GraphDataDto buildFamilyTree(Family fam, SimulationEngine engine) {
        if (fam == null) return new GraphDataDto(List.of(), List.of());
        List<Person> members = fam.getAllHistoricalMembers();
        if (members.isEmpty()) members = fam.getMembers();

        List<GraphNodeDto> nodes = new ArrayList<>();
        List<GraphEdgeDto> edges = new ArrayList<>();

        Person founder = fam.getOriginalFounder() != null ? fam.getOriginalFounder() : fam.getFounder();
        int baseYear = founder != null ? founder.getBirthYear() : fam.getFoundedYear();

        Map<Integer, List<Person>> byGen = new TreeMap<>();
        for (Person p : members) {
            int gen = Math.max(0, (p.getBirthYear() - baseYear) / 20);
            byGen.computeIfAbsent(gen, k -> new ArrayList<>()).add(p);
        }

        for (Map.Entry<Integer, List<Person>> entry : byGen.entrySet()) {
            int gen = entry.getKey();
            List<Person> genPeople = entry.getValue();
            for (int i = 0; i < genPeople.size(); i++) {
                Person p = genPeople.get(i);
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("id", p.getId());
                data.put("name", p.getName());
                data.put("gender", p.getGender().name());
                data.put("alive", p.isAlive());
                data.put("birthYear", p.getBirthYear());
                data.put("deathYear", p.getDeathYear() != null ? p.getDeathYear() : -1);
                data.put("age", p.getAge());
                data.put("isAwakened", p.isAwakened());
                AetherProfile ap = p.getAetherProfile();
                data.put("supernaturalClass", ap != null && ap.getSupernaturalClass() != null ? ap.getSupernaturalClass().name() : "NONE");
                data.put("tier", ap != null ? ap.getPowerTier() : 0);
                data.put("spouseId", p.getSpouse() != null ? p.getSpouse().getId() : 0);
                data.put("spouseName", p.getSpouse() != null ? p.getSpouse().getName() : "");
                data.put("generation", gen + 1);
                data.put("x", i * 220 + 50);
                data.put("y", gen * 140 + 50);

                nodes.add(new GraphNodeDto(
                        String.valueOf(p.getId()),
                        p.getName(),
                        "familyMember",
                        data
                ));

                if (p.getFather() != null) {
                    edges.add(new GraphEdgeDto(
                            "e-pat-" + p.getFather().getId() + "-" + p.getId(),
                            String.valueOf(p.getFather().getId()),
                            String.valueOf(p.getId()),
                            "parent-child",
                            "Child"
                    ));
                } else if (p.getMother() != null) {
                    edges.add(new GraphEdgeDto(
                            "e-mat-" + p.getMother().getId() + "-" + p.getId(),
                            String.valueOf(p.getMother().getId()),
                            String.valueOf(p.getId()),
                            "parent-child",
                            "Child"
                    ));
                }

                if (p.getSpouse() != null && p.getId() < p.getSpouse().getId()) {
                    edges.add(new GraphEdgeDto(
                            "e-marr-" + p.getId() + "-" + p.getSpouse().getId(),
                            String.valueOf(p.getId()),
                            String.valueOf(p.getSpouse().getId()),
                            "marriage",
                            "Spouse"
                    ));
                }
            }
        }

        return new GraphDataDto(nodes, edges);
    }

    public static GraphDataDto buildPersonLineageTree(Person p, SimulationEngine engine) {
        if (p == null) return new GraphDataDto(List.of(), List.of());
        List<GraphNodeDto> nodes = new ArrayList<>();
        List<GraphEdgeDto> edges = new ArrayList<>();
        Set<Integer> addedIds = new HashSet<>();

        // Add root person
        addPersonTreeNode(p, 0, 100, nodes, addedIds);

        // Parents (level -1)
        if (p.getFather() != null) {
            addPersonTreeNode(p.getFather(), -100, 0, nodes, addedIds);
            edges.add(new GraphEdgeDto("e-pat-" + p.getFather().getId() + "-" + p.getId(), String.valueOf(p.getFather().getId()), String.valueOf(p.getId()), "parent-child", "Father"));
        }
        if (p.getMother() != null) {
            addPersonTreeNode(p.getMother(), 100, 0, nodes, addedIds);
            edges.add(new GraphEdgeDto("e-mat-" + p.getMother().getId() + "-" + p.getId(), String.valueOf(p.getMother().getId()), String.valueOf(p.getId()), "parent-child", "Mother"));
        }

        // Spouse
        if (p.getSpouse() != null) {
            addPersonTreeNode(p.getSpouse(), 180, 100, nodes, addedIds);
            edges.add(new GraphEdgeDto("e-sp-" + p.getId() + "-" + p.getSpouse().getId(), String.valueOf(p.getId()), String.valueOf(p.getSpouse().getId()), "marriage", "Spouse"));
        }

        // Children (level +1)
        List<Person> children = p.getChildren();
        for (int i = 0; i < children.size(); i++) {
            Person child = children.get(i);
            int cx = (i - children.size() / 2) * 160;
            addPersonTreeNode(child, cx, 200, nodes, addedIds);
            edges.add(new GraphEdgeDto("e-ch-" + p.getId() + "-" + child.getId(), String.valueOf(p.getId()), String.valueOf(child.getId()), "parent-child", "Child"));
        }

        return new GraphDataDto(nodes, edges);
    }

    private static void addPersonTreeNode(Person p, int x, int y, List<GraphNodeDto> nodes, Set<Integer> addedIds) {
        if (!addedIds.add(p.getId())) return;
        AetherProfile ap = p.getAetherProfile();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", p.getId());
        data.put("name", p.getName());
        data.put("gender", p.getGender().name());
        data.put("alive", p.isAlive());
        data.put("birthYear", p.getBirthYear());
        data.put("deathYear", p.getDeathYear() != null ? p.getDeathYear() : -1);
        data.put("age", p.getAge());
        data.put("isAwakened", p.isAwakened());
        data.put("supernaturalClass", ap != null && ap.getSupernaturalClass() != null ? ap.getSupernaturalClass().name() : "NONE");
        data.put("tier", ap != null ? ap.getPowerTier() : 0);
        data.put("x", x);
        data.put("y", y);

        nodes.add(new GraphNodeDto(String.valueOf(p.getId()), p.getName(), "familyMember", data));
    }

    public static HeirloomSummaryDto buildHeirloomSummary(ClanHeirloom h, EquipmentCraftingSystem ecs, OrganizationSystem orgSys, SimulationEngine engine) {
        if (h == null) return null;
        EquipmentItem item = (ecs != null) ? ecs.getEquipment(h.getEquipmentId()) : null;
        Organization owningOrg = (orgSys != null) ? orgSys.getOrganization(h.getOwningClanId()) : null;
        String bearerName = "None";
        if (h.getCurrentBearerPersonId() != null) {
            try {
                Person bearer = engine.getPerson(Integer.parseInt(h.getCurrentBearerPersonId()));
                if (bearer != null) bearerName = bearer.getName();
            } catch (Exception ignored) {}
        }

        return new HeirloomSummaryDto(
                h.getHeirloomId(),
                h.getEquipmentId(),
                h.getArtifactTitle(),
                item != null ? item.getType().name() : "RELIC",
                item != null ? item.getEquipmentRarity().name() : "LEGENDARY",
                item != null ? item.getEquipmentTier() : 3,
                item != null ? item.getCondition().name() : "PRISTINE",
                item != null ? round(item.getDurability()) : 100.0,
                item != null ? round(item.getMaximumDurability()) : 100.0,
                h.getOwningClanId(),
                owningOrg != null ? owningOrg.getDisplayName() : h.getOwningClanId(),
                h.getCurrentBearerPersonId(),
                bearerName,
                h.getCustodyStatus().name(),
                h.getSuccessionPolicy().name(),
                h.getRegistrationYear()
        );
    }

    public static HeirloomDetailDto buildHeirloomDetail(ClanHeirloom h, EquipmentCraftingSystem ecs, OrganizationSystem orgSys, SimulationEngine engine) {
        if (h == null) return null;
        EquipmentItem item = (ecs != null) ? ecs.getEquipment(h.getEquipmentId()) : null;
        Organization owningOrg = (orgSys != null) ? orgSys.getOrganization(h.getOwningClanId()) : null;

        String bearerName = "None";
        if (h.getCurrentBearerPersonId() != null) {
            try {
                Person bearer = engine.getPerson(Integer.parseInt(h.getCurrentBearerPersonId()));
                if (bearer != null) bearerName = bearer.getName();
            } catch (Exception ignored) {}
        }

        String creatorName = "Ancient Artificer";
        if (item != null && item.getCreatorPersonId() != null) {
            try {
                Person creator = engine.getPerson(Integer.parseInt(item.getCreatorPersonId()));
                if (creator != null) creatorName = creator.getName();
            } catch (Exception ignored) {}
        }

        List<String> enchantments = new ArrayList<>();
        if (item != null) {
            enchantments = item.getEnchantments().stream()
                    .map(en -> en.getFamily() + " (T" + en.getPotencyTier() + " +" + round(en.getPrimaryBonus()) + ")")
                    .collect(Collectors.toList());
        }

        List<BearerRecordDto> pastBearers = new ArrayList<>();
        for (String bId : h.getPastBearerIds()) {
            String bName = "Bearer #" + bId;
            try {
                Person bp = engine.getPerson(Integer.parseInt(bId));
                if (bp != null) bName = bp.getName();
            } catch (Exception ignored) {}
            pastBearers.add(new BearerRecordDto(bId, bName, "Past Custodian", "Succession under " + h.getSuccessionPolicy()));
        }

        return new HeirloomDetailDto(
                h.getHeirloomId(),
                h.getEquipmentId(),
                h.getArtifactTitle(),
                item != null ? item.getType().name() : "RELIC",
                item != null ? item.getEquipmentRarity().name() : "LEGENDARY",
                item != null ? item.getEquipmentTier() : 3,
                item != null ? String.format("%.1f", item.getCraftQuality()) : "1.0",
                item != null ? item.getCondition().name() : "PRISTINE",
                item != null ? round(item.getDurability()) : 100.0,
                item != null ? round(item.getMaximumDurability()) : 100.0,
                enchantments,
                item != null ? item.getCreatorPersonId() : null,
                creatorName,
                h.getOwningClanId(),
                owningOrg != null ? owningOrg.getDisplayName() : h.getOwningClanId(),
                h.getCurrentBearerPersonId(),
                bearerName,
                h.getCustodyStatus().name(),
                h.getSuccessionPolicy().name(),
                h.getRegistrationYear(),
                h.getRegistrationMonth(),
                pastBearers,
                h.getBattleChronicle()
        );
    }

    public static GraphDataDto buildHeirloomSuccessionGraph(ClanHeirloom h, SimulationEngine engine) {
        if (h == null) return new GraphDataDto(List.of(), List.of());
        List<GraphNodeDto> nodes = new ArrayList<>();
        List<GraphEdgeDto> edges = new ArrayList<>();

        List<String> chain = new ArrayList<>(h.getPastBearerIds());
        if (h.getCurrentBearerPersonId() != null && !chain.contains(h.getCurrentBearerPersonId())) {
            chain.add(h.getCurrentBearerPersonId());
        }

        String prevId = null;
        for (int i = 0; i < chain.size(); i++) {
            String bId = chain.get(i);
            String name = "Bearer #" + bId;
            boolean alive = true;
            try {
                Person p = engine.getPerson(Integer.parseInt(bId));
                if (p != null) {
                    name = p.getName();
                    alive = p.isAlive();
                }
            } catch (Exception ignored) {}

            boolean isCurrent = bId.equals(h.getCurrentBearerPersonId());
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", bId);
            data.put("name", name);
            data.put("isCurrent", isCurrent);
            data.put("alive", alive);
            data.put("step", i + 1);
            data.put("x", i * 220 + 60);
            data.put("y", 100);

            nodes.add(new GraphNodeDto("node-" + bId, name, "heirloomBearer", data));

            if (prevId != null) {
                edges.add(new GraphEdgeDto(
                        "edge-" + prevId + "-" + bId,
                        "node-" + prevId,
                        "node-" + bId,
                        "succession",
                        "Passed to"
                ));
            }
            prevId = bId;
        }

        return new GraphDataDto(nodes, edges);
    }

    public static GraphDataDto buildOrganizationDiplomacyGraph(Organization targetOrg, OrganizationSystem orgSys) {
        if (targetOrg == null || orgSys == null) return new GraphDataDto(List.of(), List.of());
        List<GraphNodeDto> nodes = new ArrayList<>();
        List<GraphEdgeDto> edges = new ArrayList<>();

        Map<String, Object> targetData = new LinkedHashMap<>();
        targetData.put("id", targetOrg.getOrganizationId());
        targetData.put("name", targetOrg.getDisplayName());
        targetData.put("type", targetOrg.getOrganizationType().name());
        targetData.put("alignment", targetOrg.getAlignment().name());
        targetData.put("members", targetOrg.getMemberCount());
        targetData.put("x", 300);
        targetData.put("y", 200);
        nodes.add(new GraphNodeDto(targetOrg.getOrganizationId(), targetOrg.getDisplayName(), "orgCenter", targetData));

        List<Organization> others = orgSys.getActiveOrganizations();
        int angleIdx = 0;
        int count = Math.min(8, others.size() - 1);
        for (Organization o : others) {
            if (o.getOrganizationId().equals(targetOrg.getOrganizationId())) continue;
            if (angleIdx >= 8) break;

            double angle = (2 * Math.PI / Math.max(1, count)) * angleIdx;
            int x = (int) (300 + 220 * Math.cos(angle));
            int y = (int) (200 + 160 * Math.sin(angle));
            angleIdx++;

            Map<String, Object> oData = new LinkedHashMap<>();
            oData.put("id", o.getOrganizationId());
            oData.put("name", o.getDisplayName());
            oData.put("type", o.getOrganizationType().name());
            oData.put("alignment", o.getAlignment().name());
            oData.put("members", o.getMemberCount());
            oData.put("x", x);
            oData.put("y", y);
            nodes.add(new GraphNodeDto(o.getOrganizationId(), o.getDisplayName(), "orgPeer", oData));

            OrganizationRelationship oRel = orgSys.getRelationship(targetOrg.getOrganizationId(), o.getOrganizationId());
            String rel = (oRel != null && oRel.getDiplomaticStatus() != null)
                    ? oRel.getDiplomaticStatus().name()
                    : "NEUTRAL";

            edges.add(new GraphEdgeDto(
                    "diplo-" + targetOrg.getOrganizationId() + "-" + o.getOrganizationId(),
                    targetOrg.getOrganizationId(),
                    o.getOrganizationId(),
                    rel.toLowerCase(),
                    rel
            ));
        }

        return new GraphDataDto(nodes, edges);
    }

    public static LeaderboardDto buildLeaderboard(SimulationEngine engine, String category) {
        return buildLeaderboard(engine, category, null);
    }

    public static LeaderboardDto buildLeaderboard(SimulationEngine engine, String category, Integer targetYear) {
        String cat = (category != null) ? category.toUpperCase().trim() : "POWER";
        int currentYear = engine.getTimeline().getYear();
        int month = engine.getTimeline().getMonth();

        if (targetYear != null && targetYear != currentYear) {
            return new LeaderboardDto(
                    cat,
                    targetYear,
                    12,
                    false,
                    "Historical ranking unavailable — insufficient recorded snapshot data.",
                    List.of()
            );
        }

        List<LeaderboardEntryDto> entries = new ArrayList<>();
        switch (cat) {
            case "TIER": {
                List<Person> sorted = engine.getAllPeople().stream()
                        .filter(Person::isAlive)
                        .filter(Person::isAwakened)
                        .sorted((a, b) -> {
                            int tComp = Integer.compare(b.getAetherProfile().getPowerTier(), a.getAetherProfile().getPowerTier());
                            if (tComp != 0) return tComp;
                            return Double.compare(b.getAetherProfile().getDerivedPower(), a.getAetherProfile().getDerivedPower());
                        })
                        .limit(25)
                        .toList();

                int rank = 1;
                for (Person p : sorted) {
                    AetherProfile ap = p.getAetherProfile();
                    entries.add(new LeaderboardEntryDto(
                            rank++,
                            String.valueOf(p.getId()),
                            p.getName(),
                            ap.getPowerTier(),
                            String.format("T%d %s (%s)", ap.getPowerTier(), ap.getSupernaturalClass(), ap.getRarity()),
                            "Mastery: " + round(ap.getMastery()) + " | Power: " + round(ap.getDerivedPower())
                    ));
                }
                break;
            }
            case "WARRIORS": {
                List<Person> sorted = engine.getAllPeople().stream()
                        .filter(Person::isAlive)
                        .sorted((a, b) -> {
                            double pA = a.isAwakened() ? a.getAetherProfile().getDerivedPower() : a.getEffectiveAttribute(AttributeType.STRENGTH);
                            double pB = b.isAwakened() ? b.getAetherProfile().getDerivedPower() : b.getEffectiveAttribute(AttributeType.STRENGTH);
                            return Double.compare(pB, pA);
                        })
                        .limit(25)
                        .toList();

                int rank = 1;
                for (Person p : sorted) {
                    double pwr = p.isAwakened() ? p.getAetherProfile().getDerivedPower() : p.getEffectiveAttribute(AttributeType.STRENGTH);
                    entries.add(new LeaderboardEntryDto(
                            rank++,
                            String.valueOf(p.getId()),
                            p.getName(),
                            round(pwr),
                            p.isAwakened() ? p.getAetherProfile().getSupernaturalClass().name() : "Warrior",
                            "Combat Power: " + round(pwr) + " | Settlement: " + (p.getVillage() != null ? p.getVillage().getName() : "None")
                    ));
                }
                break;
            }
            case "WEALTH": {
                List<Person> sorted = engine.getAllPeople().stream()
                        .filter(Person::isAlive)
                        .sorted((a, b) -> Double.compare(b.getWealth(), a.getWealth()))
                        .limit(25)
                        .toList();

                int rank = 1;
                for (Person p : sorted) {
                    entries.add(new LeaderboardEntryDto(
                            rank++,
                            String.valueOf(p.getId()),
                            p.getName(),
                            round(p.getWealth()),
                            p.getOccupation() != null ? p.getOccupation().name() : "CITIZEN",
                            "Personal Wealth: " + round(p.getWealth()) + " Silver | Bank: " + round(p.getBankBalance())
                    ));
                }
                break;
            }
            case "INFLUENCE": {
                List<Person> sorted = engine.getAllPeople().stream()
                        .filter(Person::isAlive)
                        .sorted((a, b) -> {
                            double infA = a.getAge() * 0.5 + (a.isAwakened() ? a.getAetherProfile().getPowerTier() * 10 : 0);
                            double infB = b.getAge() * 0.5 + (b.isAwakened() ? b.getAetherProfile().getPowerTier() * 10 : 0);
                            return Double.compare(infB, infA);
                        })
                        .limit(25)
                        .toList();

                int rank = 1;
                for (Person p : sorted) {
                    double inf = p.getAge() * 0.5 + (p.isAwakened() ? p.getAetherProfile().getPowerTier() * 10 : 0);
                    entries.add(new LeaderboardEntryDto(
                            rank++,
                            String.valueOf(p.getId()),
                            p.getName(),
                            round(inf),
                            p.getOccupation() != null ? p.getOccupation().name() : "CITIZEN",
                            "Influence: " + round(inf) + " | Age: " + p.getAge() + (p.isAwakened() ? " (Awakened)" : "")
                    ));
                }
                break;
            }
            case "CLANS": {
                List<Organization> sorted = engine.getOrganizationSystem().getActiveOrganizations().stream()
                        .filter(o -> o.getOrganizationType() == OrganizationType.CLAN)
                        .sorted((a, b) -> Double.compare(b.getReputation(), a.getReputation()))
                        .limit(25)
                        .toList();

                int rank = 1;
                for (Organization org : sorted) {
                    entries.add(new LeaderboardEntryDto(
                            rank++,
                            org.getOrganizationId(),
                            org.getDisplayName(),
                            round(org.getReputation()),
                            org.getOrganizationType() + " | " + org.getAlignment(),
                            "Members: " + org.getMemberCount() + " | Stock: " + round(org.getSupernaturalInventory().getTotalAmount()) + " | Heirlooms: " + org.getHeirlooms().size()
                    ));
                }
                break;
            }
            case "ORGANIZATIONS": {
                List<Organization> sorted = engine.getOrganizationSystem().getActiveOrganizations().stream()
                        .sorted((a, b) -> Integer.compare(b.getMemberCount(), a.getMemberCount()))
                        .limit(25)
                        .toList();

                int rank = 1;
                for (Organization org : sorted) {
                    entries.add(new LeaderboardEntryDto(
                            rank++,
                            org.getOrganizationId(),
                            org.getDisplayName(),
                            org.getMemberCount(),
                            org.getOrganizationType() + " | " + org.getAlignment(),
                            "Influence: " + round(org.getReputation()) + " | Battles: " + org.getBattlesWon()
                    ));
                }
                break;
            }
            case "CRAFTSMEN": {
                List<Person> sorted = engine.getAllPeople().stream()
                        .filter(Person::isAlive)
                        .filter(p -> p.getCrafterProfile() != null || p.getOccupation() == Occupation.BLACKSMITH)
                        .sorted((a, b) -> {
                            double cA = a.getCrafterProfile() != null ? a.getCrafterProfile().getAverageProficiency() : a.getEffectiveAttribute(AttributeType.AETHER_CONTROL);
                            double cB = b.getCrafterProfile() != null ? b.getCrafterProfile().getAverageProficiency() : b.getEffectiveAttribute(AttributeType.AETHER_CONTROL);
                            return Double.compare(cB, cA);
                        })
                        .limit(25)
                        .toList();

                int rank = 1;
                for (Person p : sorted) {
                    double exp = p.getCrafterProfile() != null ? p.getCrafterProfile().getAverageProficiency() : p.getEffectiveAttribute(AttributeType.AETHER_CONTROL);
                    entries.add(new LeaderboardEntryDto(
                            rank++,
                            String.valueOf(p.getId()),
                            p.getName(),
                            round(exp),
                            p.getCrafterProfile() != null ? "Master Crafter" : "Blacksmith",
                            "Crafting Skill: " + round(exp) + " | Settlement: " + (p.getVillage() != null ? p.getVillage().getName() : "None")
                    ));
                }
                break;
            }
            case "MONSTER_SLAYERS": {
                List<Person> sorted = engine.getAllPeople().stream()
                        .filter(Person::isAlive)
                        .filter(Person::isAwakened)
                        .sorted((a, b) -> Double.compare(b.getAetherProfile().getDerivedPower(), a.getAetherProfile().getDerivedPower()))
                        .limit(25)
                        .toList();

                int rank = 1;
                for (Person p : sorted) {
                    AetherProfile ap = p.getAetherProfile();
                    entries.add(new LeaderboardEntryDto(
                            rank++,
                            String.valueOf(p.getId()),
                            p.getName(),
                            round(ap.getDerivedPower()),
                            ap.getSupernaturalClass().name() + " Slayer",
                            "Slayer Rating: " + round(ap.getDerivedPower()) + " | Tier: " + ap.getPowerTier()
                    ));
                }
                break;
            }
            case "LONGEST_LIVING": {
                List<Person> sorted = engine.getAllPeopleIncludingDeceased().stream()
                        .sorted((a, b) -> Integer.compare(b.getAge(), a.getAge()))
                        .limit(25)
                        .toList();

                int rank = 1;
                for (Person p : sorted) {
                    entries.add(new LeaderboardEntryDto(
                            rank++,
                            String.valueOf(p.getId()),
                            p.getName(),
                            p.getAge(),
                            p.isAlive() ? "Living (" + p.getAge() + " yrs)" : "Deceased (Age " + p.getAge() + ")",
                            "Birth Y" + p.getBirthYear() + (p.isAlive() ? "" : " | Died Y" + p.getDeathYear() + " (" + p.getCauseOfDeath() + ")")
                    ));
                }
                break;
            }
            case "LEADERS": {
                List<Person> sorted = engine.getAllPeople().stream()
                        .filter(Person::isAlive)
                        .filter(p -> p.getVillage() != null && p.getVillage().getLeader() == p)
                        .sorted((a, b) -> Integer.compare(b.getVillage().getLivingPopulationCount(), a.getVillage().getLivingPopulationCount()))
                        .limit(25)
                        .toList();

                int rank = 1;
                for (Person p : sorted) {
                    entries.add(new LeaderboardEntryDto(
                            rank++,
                            String.valueOf(p.getId()),
                            p.getName(),
                            p.getVillage().getLivingPopulationCount(),
                            "Mayor of " + p.getVillage().getName(),
                            "Governing Pop: " + p.getVillage().getLivingPopulationCount() + " | Tech: " + p.getVillage().getTechLevel()
                    ));
                }
                break;
            }
            case "POWER":
            default: {
                List<Person> sorted = engine.getAllPeople().stream()
                        .filter(Person::isAlive)
                        .filter(Person::isAwakened)
                        .sorted((a, b) -> Double.compare(
                                b.getAetherProfile().getDerivedPower(),
                                a.getAetherProfile().getDerivedPower()
                        ))
                        .limit(25)
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
        }

        return new LeaderboardDto(cat, currentYear, month, true, null, entries);
    }

    public static PersonComparisonDto buildPersonComparison(Person p1, Person p2, SimulationEngine engine) {
        if (p1 == null || p2 == null) return null;
        PersonDetailDto d1 = buildPersonDetail(p1, engine.getOrganizationSystem(), engine.getEquipmentCraftingSystem(), engine.getClanHeirloomSystem());
        PersonDetailDto d2 = buildPersonDetail(p2, engine.getOrganizationSystem(), engine.getEquipmentCraftingSystem(), engine.getClanHeirloomSystem());

        String date = "Year " + engine.getTimeline().getYear() + ", Month " + engine.getTimeline().getMonth();
        String pwrSum = (d1.derivedPower() > d2.derivedPower())
                ? d1.name() + " has superior combat power (" + d1.derivedPower() + " vs " + d2.derivedPower() + ")"
                : (d2.derivedPower() > d1.derivedPower())
                ? d2.name() + " has superior combat power (" + d2.derivedPower() + " vs " + d1.derivedPower() + ")"
                : "Both individuals possess equivalent combat power";

        String wealthSum = (d1.wealth() > d2.wealth())
                ? d1.name() + " holds more personal wealth (" + d1.wealth() + " vs " + d2.wealth() + " Silver)"
                : (d2.wealth() > d1.wealth())
                ? d2.name() + " holds more personal wealth (" + d2.wealth() + " vs " + d1.wealth() + " Silver)"
                : "Both individuals have identical personal wealth";

        return new PersonComparisonDto(d1, d2, date, pwrSum, wealthSum);
    }

    public static OrganizationComparisonDto buildOrganizationComparison(Organization o1, Organization o2, SimulationEngine engine) {
        if (o1 == null || o2 == null) return null;
        OrganizationDetailDto d1 = buildOrganizationDetail(o1);
        OrganizationDetailDto d2 = buildOrganizationDetail(o2);

        String date = "Year " + engine.getTimeline().getYear() + ", Month " + engine.getTimeline().getMonth();
        String infSum = (d1.reputation() > d2.reputation())
                ? d1.name() + " wields higher regional influence (" + d1.reputation() + " vs " + d2.reputation() + ")"
                : (d2.reputation() > d1.reputation())
                ? d2.name() + " wields higher regional influence (" + d2.reputation() + " vs " + d1.reputation() + ")"
                : "Both organizations command equivalent regional influence";

        String milSum = (d1.battlesWon() > d2.battlesWon())
                ? d1.name() + " has more documented battle victories (" + d1.battlesWon() + " vs " + d2.battlesWon() + ")"
                : (d2.battlesWon() > d1.battlesWon())
                ? d2.name() + " has more documented battle victories (" + d2.battlesWon() + " vs " + d1.battlesWon() + ")"
                : "Both organizations have matched combat records";

        return new OrganizationComparisonDto(d1, d2, date, infSum, milSum);
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

    // --- Phase 3.4 World Activity & Historical Chronicle Builders ---

    public static AnnualChronicleDto buildAnnualChronicle(AnnualReport report, List<AnnualReport> allReports, Timeline timeline, SimulationConfig config, SimulationEngine engine) {
        if (report == null) return null;
        int year = report.getYear();
        int closingPop = report.getTotalLivingPopulation();
        int openingPop = closingPop;
        if (year > 0 && allReports != null) {
            for (AnnualReport prev : allReports) {
                if (prev.getYear() == year - 1) {
                    openingPop = prev.getTotalLivingPopulation();
                    break;
                }
            }
        }
        int births = report.getBirthsDuringYear();
        int deaths = report.getDeathsDuringYear();
        int marriages = report.getMarriagesDuringYear();
        int crossSettlementMarriages = report.getCrossVillageMarriagesDuringYear();
        int migrations = report.getMigrationsDuringYear();
        int settlementCount = (report.getVillageLivingPopulations() != null && !report.getVillageLivingPopulations().isEmpty())
                ? report.getVillageLivingPopulations().size() : (engine != null ? engine.getVillages().size() : 2);
        int activeClanCount = (engine != null && engine.getOrganizationSystem() != null)
                ? engine.getOrganizationSystem().getActiveOrganizations().size() : 0;

        int battlesCount = 0;
        for (BattleResult b : CombatEngine.getAllCachedBattles()) {
            if (b.getSimulationYear() == year) battlesCount++;
        }

        int tidesCount = 0;
        if (engine != null && engine.getMonsterEcologySystem() != null) {
            for (MonsterTide t : engine.getMonsterEcologySystem().getTides().values()) {
                if (t.getMobilizedYear() == year || t.getArrivalYear() == year) tidesCount++;
            }
        }

        int anomaliesCount = 0;
        if (engine != null && engine.getAnomalySystem() != null) {
            for (WorldAnomaly a : engine.getAnomalySystem().getAllAnomalies()) {
                if (a.isPubliclyObservable()) anomaliesCount++;
            }
        }

        int activeFeudsCount = (engine != null && engine.getBloodFeudSystem() != null)
                ? engine.getBloodFeudSystem().getActiveFeuds().size() : 0;

        int awakeningStart = (config != null) ? config.getAwakeningStartYear() : 50;
        String era = (year < awakeningStart) ? "Mundane Era" : "Awakening Era";

        return new AnnualChronicleDto(
                year,
                (long) year * 12 + 11,
                era,
                openingPop,
                closingPop,
                births,
                deaths,
                marriages,
                migrations,
                settlementCount,
                activeClanCount,
                battlesCount,
                tidesCount,
                anomaliesCount,
                report.getAwakeningSummary() != null ? report.getAwakeningSummary() : "",
                report.getCombatSummary() != null ? report.getCombatSummary() : "",
                report.getOrganizationSummary() != null ? report.getOrganizationSummary() : "",
                report.getMonsterEcologySummary() != null ? report.getMonsterEcologySummary() : "",
                report.getEquipmentSummary() != null ? report.getEquipmentSummary() : "",
                report.getBloodFeudSummary() != null ? report.getBloodFeudSummary() : "",
                report.getVillageLivingPopulations() != null ? report.getVillageLivingPopulations() : Map.of()
        );
    }

    public static List<CivilizationMilestoneDto> buildCivilizationMilestones(SimulationEngine engine) {
        List<CivilizationMilestoneDto> list = new ArrayList<>();
        if (engine == null) return list;

        int curYear = engine.getTimeline().getYear();
        int curMonth = engine.getTimeline().getMonth();
        int awakeningYear = engine.getConfig().getAwakeningStartYear();

        // Milestone 1: Genesis of Civilization
        list.add(new CivilizationMilestoneDto(
                0, 0,
                "Genesis of Civilization",
                "Founding lineages established initial settlements and formed the first civil societies.",
                "FOUNDING",
                "Mundane Era",
                "CRITICAL"
        ));

        // Milestone 2: Awakening Emergence
        if (curYear >= awakeningYear) {
            list.add(new CivilizationMilestoneDto(
                    awakeningYear, 0,
                    "Dawn of the Awakening",
                    "Supernatural aether began surging across the world, unveiling magical affinities and ancient anomalies.",
                    "AWAKENING",
                    "Awakening Era",
                    "CRITICAL"
            ));
        }

        // Milestone 3: First Clan Establishment
        if (engine.getOrganizationSystem() != null && !engine.getOrganizationSystem().getAllOrganizations().isEmpty()) {
            Organization firstOrg = engine.getOrganizationSystem().getAllOrganizations().get(0);
            list.add(new CivilizationMilestoneDto(
                    firstOrg.getFoundingYear(), firstOrg.getFoundingMonth(),
                    "First Clan Emergence: " + firstOrg.getDisplayName(),
                    "The martial and economic brotherhood " + firstOrg.getDisplayName() + " was formally founded.",
                    "ORGANIZATION",
                    (firstOrg.getFoundingYear() < awakeningYear) ? "Mundane Era" : "Awakening Era",
                    "MAJOR"
            ));
        }

        // Milestone 4: First Monster Horde / MonsterTide
        if (engine.getMonsterEcologySystem() != null && !engine.getMonsterEcologySystem().getTides().isEmpty()) {
            MonsterTide firstTide = engine.getMonsterEcologySystem().getTides().values().iterator().next();
            list.add(new CivilizationMilestoneDto(
                    firstTide.getMobilizedYear(), firstTide.getMobilizedMonth(),
                    "Great MonsterTide Mobilization",
                    "A catastrophic invasion force mobilized from lair " + firstTide.getSourceLairId() + " marching on " + firstTide.getTargetVillageName() + ".",
                    "MONSTER_TIDE",
                    (firstTide.getMobilizedYear() < awakeningYear) ? "Mundane Era" : "Awakening Era",
                    "CRITICAL"
            ));
        }

        // Milestone 5: Extinction check
        if (engine.isHumanExtinct()) {
            list.add(new CivilizationMilestoneDto(
                    curYear, curMonth,
                    "Fall of Humanity",
                    "The final living human has fallen. The civilized mortal era has come to an end.",
                    "EXTINCTION",
                    (curYear < awakeningYear) ? "Mundane Era" : "Awakening Era",
                    "CRITICAL"
            ));
        }

        list.sort(Comparator.comparingInt(CivilizationMilestoneDto::year)
                .thenComparingInt(CivilizationMilestoneDto::month));
        return list;
    }

    public static MonsterTideSummaryDto buildMonsterTideSummary(MonsterTide tide, MonsterEcologySystem mes, long currentTotalMonths) {
        if (tide == null) return null;
        int totalMonsters = tide.getWave1MonsterIds().size() + tide.getWave2MonsterIds().size() + tide.getWave3MonsterIds().size();

        return new MonsterTideSummaryDto(
                tide.getTideId(),
                tide.getSourceLairId(),
                tide.getTargetVillageId(),
                tide.getTargetVillageName(),
                tide.getState() != null ? tide.getState().name() : "APPROACHING",
                tide.getMobilizedYear(),
                tide.getMobilizedMonth(),
                tide.getArrivalYear(),
                tide.getArrivalMonth(),
                totalMonsters,
                tide.getOutcome() != null ? tide.getOutcome().name() : "PENDING",
                tide.isSeerDetected(),
                tide.getWarningLeadMonths()
        );
    }

    public static MonsterTideDetailDto buildMonsterTideDetail(MonsterTide tide, MonsterEcologySystem mes, long currentTotalMonths) {
        if (tide == null) return null;
        MonsterLair lair = (mes != null) ? mes.getLair(tide.getSourceLairId()) : null;
        double lairX = (lair != null) ? lair.getPosX() : 60.0;
        double lairY = (lair != null) ? lair.getPosY() : 60.0;

        double curX, curY;
        if (tide.getState() == MonsterTideState.INVADING || tide.getState() == MonsterTideState.RESOLVED) {
            curX = tide.getTargetX();
            curY = tide.getTargetY();
        } else {
            double prog = Math.min(1.0, Math.max(0.0, (double) (currentTotalMonths - tide.getMobilizedTotalMonths()) / Math.max(1, tide.getTravelDurationMonths())));
            curX = round(lairX + prog * (tide.getTargetX() - lairX));
            curY = round(lairY + prog * (tide.getTargetY() - lairY));
        }

        int totalMonsters = tide.getWave1MonsterIds().size() + tide.getWave2MonsterIds().size() + tide.getWave3MonsterIds().size();

        return new MonsterTideDetailDto(
                tide.getTideId(),
                tide.getSourceLairId(),
                tide.getTargetVillageId(),
                tide.getTargetVillageName(),
                round(lairX),
                round(lairY),
                round(tide.getTargetX()),
                round(tide.getTargetY()),
                curX,
                curY,
                tide.getState() != null ? tide.getState().name() : "APPROACHING",
                tide.getMobilizedYear(),
                tide.getMobilizedMonth(),
                tide.getArrivalYear(),
                tide.getArrivalMonth(),
                tide.getTravelDurationMonths(),
                tide.isSeerDetected(),
                tide.getWarningLeadMonths(),
                tide.getOutcome() != null ? tide.getOutcome().name() : "PENDING",
                tide.getWave1MonsterIds(),
                tide.getWave2MonsterIds(),
                tide.getWave3MonsterIds(),
                totalMonsters,
                tide.getSlainMonsterIds().size(),
                tide.getSurvivingRetreatingMonsterIds().size(),
                tide.getDefenderFatalities(),
                tide.getCivilianFatalities(),
                round(tide.getStructuralDamagePercent()),
                round(tide.getFoodDestroyed()),
                null
        );
    }

    public static ExpeditionSummaryDto buildExpeditionSummary(Expedition exp, SimulationEngine engine) {
        if (exp == null) return null;

        return new ExpeditionSummaryDto(
                exp.getExpeditionId(),
                exp.getSponsorId(),
                exp.getSponsorType(),
                exp.getTargetAnomalyId(),
                exp.getStatus() != null ? exp.getStatus().name() : "EN_ROUTE",
                exp.getRosterPersonIds().size(),
                round(exp.getCurrentX()),
                round(exp.getCurrentY()),
                round(exp.getCarriedResources()),
                round(exp.getMaxCarryCapacity()),
                round(exp.getSupplies()),
                (long) exp.getDepartureYear() * 12 + exp.getDepartureMonth(),
                exp.getArrivalTotalMonth()
        );
    }

    public static ExpeditionDetailDto buildExpeditionDetail(Expedition exp, SimulationEngine engine) {
        if (exp == null) return null;
        String anomName = exp.getTargetAnomalyId();
        if (engine != null && engine.getAnomalySystem() != null) {
            WorldAnomaly anom = engine.getAnomalySystem().getAnomaly(exp.getTargetAnomalyId());
            if (anom != null) anomName = anom.getName();
        }

        List<String> rosterNames = new ArrayList<>();
        for (String pid : exp.getRosterPersonIds()) {
            String name = "Mortal #" + pid;
            if (engine != null) {
                try {
                    Person p = engine.getPerson(Integer.parseInt(pid));
                    if (p != null) name = p.getName();
                } catch (Exception ignored) {}
            }
            rosterNames.add(name);
        }

        return new ExpeditionDetailDto(
                exp.getExpeditionId(),
                exp.getSponsorId(),
                exp.getSponsorType(),
                exp.getTargetAnomalyId(),
                anomName,
                round(exp.getOriginX()),
                round(exp.getOriginY()),
                round(exp.getTargetX()),
                round(exp.getTargetY()),
                round(exp.getCurrentX()),
                round(exp.getCurrentY()),
                exp.getStatus() != null ? exp.getStatus().name() : "EN_ROUTE",
                exp.getClaimObjective() != null ? exp.getClaimObjective().name() : "EXPLORE",
                exp.getRosterPersonIds(),
                rosterNames,
                round(exp.getCarriedResources()),
                round(exp.getMaxCarryCapacity()),
                round(exp.getSupplies()),
                (long) exp.getDepartureYear() * 12 + exp.getDepartureMonth(),
                exp.getArrivalTotalMonth(),
                exp.getReturnArrivalMonth()
        );
    }

    public static AnomalyDetailDto buildAnomalyDetail(WorldAnomaly anom, AnomalyExplorationSystem aes) {
        if (anom == null) return null;
        int contestCount = 0;
        if (aes != null) {
            for (AnomalyContestRecord c : aes.getPendingContests()) {
                if (c.getAnomalyId().equals(anom.getAnomalyId())) contestCount++;
            }
        }
        int revealedYear = anom.getPublicAppearanceMonth() / 12;
        int discoveredYear = (anom.getFirstDiscoveryMonth() != null) ? anom.getFirstDiscoveryMonth() / 12 : -1;

        return new AnomalyDetailDto(
                anom.getAnomalyId(),
                anom.getResourceCategory() != null ? anom.getResourceCategory() : "AETHER_DEPOSIT",
                anom.getResourceGrade() != null ? anom.getResourceGrade().name() : "COMMON",
                round(anom.getPosX()),
                round(anom.getPosY()),
                anom.getState() != null ? anom.getState().name() : "ACTIVE",
                round(anom.getInitialQuantity()),
                round(anom.getRemainingQuantity()),
                anom.getControllingOwnerId(),
                anom.isPubliclyObservable(),
                revealedYear,
                discoveredYear,
                contestCount
        );
    }

    public static List<AnomalyContestDto> buildAnomalyContests(String anomalyId, AnomalyExplorationSystem aes) {
        List<AnomalyContestDto> list = new ArrayList<>();
        if (aes == null) return list;
        for (AnomalyContestRecord rec : aes.getPendingContests()) {
            if (anomalyId == null || rec.getAnomalyId().equals(anomalyId)) {
                list.add(new AnomalyContestDto(
                        rec.getContestId(),
                        rec.getAnomalyId(),
                        rec.getCreatedYear(),
                        rec.getCreatedMonth(),
                        rec.getParticipatingExpeditionIds(),
                        rec.getWinningCoalitionSideId(),
                        rec.getState() != null && rec.getState().isResolved(),
                        rec.getBattleId() != null,
                        rec.getBattleId()
                ));
            }
        }
        return list;
    }

    public static FeudSummaryDto buildFeudSummary(FeudCase f, BloodFeudSystem bfs) {
        if (f == null) return null;
        String initiator = (f.getInitiatingOrgId() != null) ? f.getInitiatingOrgId()
                : (f.getInitiatingFamilyId() != null ? "Family_" + f.getInitiatingFamilyId() : f.getInitiatingSideId());
        String target = (f.getOpposingOrgId() != null) ? f.getOpposingOrgId()
                : (f.getOpposingFamilyId() != null ? "Family_" + f.getOpposingFamilyId()
                : (!f.getOpposingSideIds().isEmpty() ? f.getOpposingSideIds().get(0) : "Unknown"));

        int startingYear = f.getOriginTotalMonths() / 12;
        int lastActionYear = (f.getLastActivityTotalMonths() > 0) ? f.getLastActivityTotalMonths() / 12 : startingYear;

        return new FeudSummaryDto(
                f.getFeudId(),
                f.getOriginGrievanceId(),
                initiator != null ? initiator : "Unknown",
                target != null ? target : "Unknown",
                f.getCurrentEscalationLevel() != null ? f.getCurrentEscalationLevel().name() : "LEVEL_0",
                f.getCurrentFeudStatus() != null ? f.getCurrentFeudStatus().name() : "OPEN",
                f.getActionHistory().size(),
                startingYear,
                lastActionYear
        );
    }

    public static FeudDetailDto buildFeudDetail(FeudCase f, BloodFeudSystem bfs) {
        if (f == null) return null;
        HistoricalGrievanceRecord g = (bfs != null) ? bfs.getGrievance(f.getOriginGrievanceId()) : null;
        String incident = (g != null && g.getDescription() != null) ? g.getDescription() : "Historical grievance";
        String severity = (g != null) ? String.format("%.1f", g.getSeverity()) : "MODERATE";

        String initiator = (f.getInitiatingOrgId() != null) ? f.getInitiatingOrgId()
                : (f.getInitiatingFamilyId() != null ? "Family_" + f.getInitiatingFamilyId() : f.getInitiatingSideId());
        String target = (f.getOpposingOrgId() != null) ? f.getOpposingOrgId()
                : (f.getOpposingFamilyId() != null ? "Family_" + f.getOpposingFamilyId()
                : (!f.getOpposingSideIds().isEmpty() ? f.getOpposingSideIds().get(0) : "Unknown"));

        List<FeudActionRecordDto> actionDtos = new ArrayList<>();
        for (FeudActionRecord a : f.getActionHistory()) {
            String actorId = a.getInitiatorPersonId() != null ? a.getInitiatorPersonId() : a.getInitiatorOrgId();
            String targetId = a.getTargetPersonId() != null ? a.getTargetPersonId() : a.getTargetOrgId();
            actionDtos.add(new FeudActionRecordDto(
                    a.getActionId(),
                    a.getActionType() != null ? a.getActionType().name() : "DISPUTE",
                    actorId != null ? actorId : "Unknown",
                    targetId != null ? targetId : "Unknown",
                    a.getSimulationYear(),
                    a.getSimulationMonth(),
                    a.getDescription(),
                    null
            ));
        }

        List<String> clans = new ArrayList<>();
        if (f.getInitiatingOrgId() != null) clans.add(f.getInitiatingOrgId());
        if (f.getOpposingOrgId() != null) clans.add(f.getOpposingOrgId());

        List<String> fams = new ArrayList<>();
        if (f.getInitiatingFamilyId() != null) fams.add(String.valueOf(f.getInitiatingFamilyId()));
        if (f.getOpposingFamilyId() != null) fams.add(String.valueOf(f.getOpposingFamilyId()));

        return new FeudDetailDto(
                f.getFeudId(),
                f.getOriginGrievanceId(),
                incident,
                severity,
                initiator != null ? initiator : "Unknown",
                target != null ? target : "Unknown",
                f.getCurrentEscalationLevel() != null ? f.getCurrentEscalationLevel().name() : "LEVEL_0",
                f.getCurrentFeudStatus() != null ? f.getCurrentFeudStatus().name() : "OPEN",
                actionDtos,
                clans,
                fams
        );
    }

    public static List<WorldActivityEventDto> buildWorldActivityEvents(SimulationEngine engine) {
        List<WorldActivityEventDto> events = new ArrayList<>();
        if (engine == null) return events;

        String worldId = "world-001";
        int seq = 1;

        // 1. Ingest Battles
        for (BattleResult b : CombatEngine.getAllCachedBattles()) {
            long totalMonths = (long) b.getSimulationYear() * 12 + b.getSimulationMonth();
            String sev = (!b.getFatalities().isEmpty()) ? "CRITICAL" : (!b.getIncapacitated().isEmpty() ? "WARNING" : "INFO");
            List<String> related = new ArrayList<>();
            related.addAll(b.getFatalities());
            related.addAll(b.getIncapacitated());
            related.addAll(b.getEscaped());

            events.add(new WorldActivityEventDto(
                    "EVT_BATTLE_" + b.getBattleId(),
                    worldId,
                    b.getSimulationYear(),
                    b.getSimulationMonth(),
                    totalMonths,
                    seq++,
                    "BATTLE",
                    b.getBattleType() != null ? b.getBattleType().name() : "SKIRMISH",
                    "Battle " + b.getBattleId() + " (" + (b.getOutcome() != null ? b.getOutcome().getDisplayName() : "Resolved") + ")",
                    b.getSummaryText(),
                    sev,
                    "Field of Combat",
                    null,
                    null,
                    b.getBattleId(),
                    "BATTLE",
                    related,
                    "RECORDED"
            ));
        }

        // 2. Ingest MonsterTides
        if (engine.getMonsterEcologySystem() != null) {
            for (MonsterTide t : engine.getMonsterEcologySystem().getTides().values()) {
                MonsterLair lair = engine.getMonsterEcologySystem().getLair(t.getSourceLairId());
                double x = (lair != null) ? lair.getPosX() : t.getTargetX();
                double y = (lair != null) ? lair.getPosY() : t.getTargetY();
                int tideMonsters = t.getWave1MonsterIds().size() + t.getWave2MonsterIds().size() + t.getWave3MonsterIds().size();

                // Mobilization event
                events.add(new WorldActivityEventDto(
                        "EVT_TIDE_MOB_" + t.getTideId(),
                        worldId,
                        t.getMobilizedYear(),
                        t.getMobilizedMonth(),
                        t.getMobilizedTotalMonths(),
                        seq++,
                        "MONSTER_TIDE",
                        "MOBILIZATION",
                        "MonsterTide Horde Mobilized (" + tideMonsters + " beasts)",
                        "Horde spawned from " + t.getSourceLairId() + " advancing on " + t.getTargetVillageName() + ".",
                        "WARNING",
                        t.getTargetVillageName(),
                        round(x),
                        round(y),
                        t.getTideId(),
                        "MONSTER_TIDE",
                        List.of(t.getSourceLairId(), "VILLAGE_" + t.getTargetVillageId()),
                        "RECORDED"
                ));

                // If resolved, arrival event
                if (t.getState() == MonsterTideState.RESOLVED) {
                    long arrMonths = t.getArrivalTotalMonths();
                    int arrYear = (int) (arrMonths / 12);
                    int arrMonth = (int) (arrMonths % 12);
                    String outcomeStr = (t.getOutcome() != null) ? t.getOutcome().name() : "RESOLVED";
                    String sev = (t.getDefenderFatalities() > 0 || t.getCivilianFatalities() > 0) ? "CRITICAL" : "MAJOR";

                    events.add(new WorldActivityEventDto(
                            "EVT_TIDE_RES_" + t.getTideId(),
                            worldId,
                            arrYear,
                            arrMonth,
                            arrMonths,
                            seq++,
                            "MONSTER_TIDE",
                            "RESOLUTION",
                            "MonsterTide " + outcomeStr + " at " + t.getTargetVillageName(),
                            String.format("Defenders killed: %d, Civilians killed: %d, Damage: %.1f%%, Food lost: %.0f",
                                    t.getDefenderFatalities(), t.getCivilianFatalities(), t.getStructuralDamagePercent(), t.getFoodDestroyed()),
                            sev,
                            t.getTargetVillageName(),
                            round(t.getTargetX()),
                            round(t.getTargetY()),
                            t.getTideId(),
                            "MONSTER_TIDE",
                            List.of(t.getSourceLairId(), "VILLAGE_" + t.getTargetVillageId()),
                            "RECORDED"
                    ));
                }
            }
        }

        // 3. Ingest Expeditions
        if (engine.getAnomalySystem() != null) {
            for (Expedition e : engine.getAnomalySystem().getAllExpeditions()) {
                events.add(new WorldActivityEventDto(
                        "EVT_EXP_" + e.getExpeditionId(),
                        worldId,
                        e.getDepartureYear(),
                        e.getDepartureMonth(),
                        (long) e.getDepartureYear() * 12 + e.getDepartureMonth(),
                        seq++,
                        "EXPEDITION",
                        e.getStatus() != null ? e.getStatus().name() : "DEPARTED",
                        "Expedition " + e.getExpeditionId() + " (" + e.getClaimObjective() + ")",
                        "Dispatched by " + e.getSponsorId() + " targeting anomaly " + e.getTargetAnomalyId() + ". Personnel: " + e.getRosterPersonIds().size(),
                        "INFO",
                        "Wilderness",
                        round(e.getCurrentX()),
                        round(e.getCurrentY()),
                        e.getExpeditionId(),
                        "EXPEDITION",
                        e.getRosterPersonIds(),
                        "RECORDED"
                ));
            }
        }

        // 4. Ingest Feud Actions
        if (engine.getBloodFeudSystem() != null) {
            for (FeudCase f : engine.getBloodFeudSystem().getAllFeuds()) {
                for (FeudActionRecord a : f.getActionHistory()) {
                    long totalMonths = (long) a.getSimulationYear() * 12 + a.getSimulationMonth();
                    List<String> related = new ArrayList<>();
                    if (a.getInitiatorPersonId() != null) related.add(a.getInitiatorPersonId());
                    if (a.getTargetPersonId() != null) related.add(a.getTargetPersonId());

                    events.add(new WorldActivityEventDto(
                            "EVT_FEUD_" + a.getActionId(),
                            worldId,
                            a.getSimulationYear(),
                            a.getSimulationMonth(),
                            totalMonths,
                            seq++,
                            "FEUD",
                            a.getActionType() != null ? a.getActionType().name() : "ESCALATION",
                            "Blood Feud Action: " + (a.getActionType() != null ? a.getActionType().name() : "Conflict"),
                            a.getDescription(),
                            "WARNING",
                            "Settlement Grounds",
                            null,
                            null,
                            f.getFeudId(),
                            "FEUD",
                            related,
                            "RECORDED"
                    ));
                }
            }
        }

        // 5. Ingest Simulation Raw Events
        List<String> rawHistory = engine.getEventHistory();
        for (int i = 0; i < rawHistory.size(); i++) {
            String line = rawHistory.get(i);
            int year = 0;
            int month = 0;
            if (line.startsWith("Year ")) {
                try {
                    int colon = line.indexOf(':');
                    if (colon > 5) {
                        String yPart = line.substring(5, colon).trim();
                        int pOpen = yPart.indexOf('(');
                        if (pOpen > 0) {
                            String yStr = yPart.substring(0, pOpen).trim();
                            year = Integer.parseInt(yStr);
                            int mIdx = yPart.indexOf("Month ");
                            if (mIdx > 0) {
                                int pClose = yPart.indexOf(')', mIdx);
                                if (pClose > 0) {
                                    month = Math.max(0, Integer.parseInt(yPart.substring(mIdx + 6, pClose).trim()) - 1);
                                }
                            }
                        } else {
                            year = Integer.parseInt(yPart);
                        }
                    }
                } catch (Exception ignored) {}
            }

            long totalMonths = (long) year * 12 + month;
            String lower = line.toLowerCase();
            String cat = "GENERAL";
            String sub = "STATUS";
            String sev = "INFO";
            if (lower.contains("famine") || lower.contains("starved") || lower.contains("perished") || lower.contains("extinct")) {
                cat = "CIVILIZATION";
                sub = "FAMINE_DEATH";
                sev = "CRITICAL";
            } else if (lower.contains("slain") || lower.contains("killed") || lower.contains("died")) {
                cat = "CIVILIZATION";
                sub = "MORTALITY";
                sev = "WARNING";
            } else if (lower.contains("awakened")) {
                cat = "AWAKENING";
                sub = "AWAKENING_SURGE";
                sev = "MAJOR";
            } else if (lower.contains("founded") || lower.contains("born") || lower.contains("married")) {
                cat = "CIVILIZATION";
                sub = "DEMOGRAPHICS";
                sev = "INFO";
            }

            events.add(new WorldActivityEventDto(
                    "EVT_RAW_" + (i + 1),
                    worldId,
                    year,
                    month,
                    totalMonths,
                    seq++,
                    cat,
                    sub,
                    cat + " Update: Year " + year,
                    line,
                    sev,
                    "World",
                    null,
                    null,
                    "HISTORICAL_" + (i + 1),
                    "LOG",
                    List.of(),
                    "RECORDED"
            ));
        }

        // Canonical Sort: totalMonths desc, sequence desc, id desc
        events.sort((a, b) -> {
            int cmp = Long.compare(b.totalMonths(), a.totalMonths());
            if (cmp != 0) return cmp;
            int cmpSeq = Integer.compare(b.sequence(), a.sequence());
            if (cmpSeq != 0) return cmpSeq;
            return b.id().compareTo(a.id());
        });

        return events;
    }

    // =========================================================================
    // Phase 3.5 Historical Snapshot, Timeline, Map & Time-Travel Builders
    // =========================================================================
    // Phase 3.5: Historical Time Travel & Historical Inspection Snapshots
    // =========================================================================

    public static HistoricalMapSnapshotDto buildHistoricalMapSnapshot(SimulationEngine engine, int targetYear, int targetMonth, String worldId) {
        long targetTotalMonths = (long) targetYear * 12 + targetMonth;
        double width = civsim.spatial.WorldGeography.WIDTH;
        double height = civsim.spatial.WorldGeography.HEIGHT;

        AnnualReport reportForYear = null;
        for (AnnualReport ar : engine.getAnnualReports()) {
            if (ar.getYear() == targetYear) {
                reportForYear = ar;
                break;
            }
        }

        List<MapSettlementDto> settlementMarkers = new ArrayList<>();
        if (reportForYear != null && reportForYear.getVillageStats() != null && !reportForYear.getVillageStats().isEmpty()) {
            for (VillageAnnualStats stats : reportForYear.getVillageStats()) {
                Village v = engine.getVillages().stream()
                        .filter(vil -> vil.getName().equalsIgnoreCase(stats.getVillageName()))
                        .findFirst().orElse(null);
                double x = v != null ? v.getX() : stats.getX();
                double y = v != null ? v.getY() : stats.getY();
                String biome = stats.getBiome() != null ? stats.getBiome().name() : (v != null && v.getBiome() != null ? v.getBiome().name() : "RIVER_VALLEY");
                double fort = v != null ? v.getFortificationLevel() : 0.0;
                int tech = stats.getTechLevel();
                String leader = stats.getLeaderName() != null ? stats.getLeaderName() : "None";
                boolean famine = stats.getDisaster() == DisasterType.DROUGHT || stats.getFamineDeaths() > 0;
                int vId = v != null ? v.getId() : (stats.getVillageName().hashCode() & 0x7fffffff);

                settlementMarkers.add(new MapSettlementDto(
                        vId,
                        stats.getVillageName(),
                        x,
                        y,
                        biome,
                        stats.getTier() != null ? stats.getTier().name() : "HAMLET",
                        stats.getLivingPopulation(),
                        fort,
                        tech,
                        leader,
                        famine
                ));
            }
        } else {
            // Live or Year 0 state before annual reports
            for (Village v : engine.getVillages()) {
                settlementMarkers.add(new MapSettlementDto(
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
        }

        List<MapLairDto> lairMarkers = new ArrayList<>();
        if (engine.getMonsterEcologySystem() != null) {
            for (MonsterLair ml : engine.getMonsterEcologySystem().getAllLairs()) {
                int emergenceYear = ml.getLastSpawnMonth() / 12;
                if (emergenceYear <= targetYear) {
                    lairMarkers.add(new MapLairDto(
                            ml.getLairId(),
                            ml.getSourceAnomalyId(),
                            ml.getPosX(),
                            ml.getPosY(),
                            ml.getBiome() != null ? ml.getBiome().name() : "FOREST",
                            ml.getDominantThreatRank() != null ? ml.getDominantThreatRank().name() : "COMMON",
                            ml.getState() != null ? ml.getState().name() : "ACTIVE",
                            ml.getMonsterCount(),
                            ml.getPopulationCapacity(),
                            ml.getTerritoryRadius(),
                            ml.getPrimarySpeciesId(),
                            ml.getSecondarySpeciesId()
                    ));
                }
            }
        }

        List<MapAnomalyDto> anomalyMarkers = new ArrayList<>();
        if (engine.getAnomalySystem() != null) {
            for (WorldAnomaly anom : engine.getAnomalySystem().getAllAnomalies()) {
                int appearanceYear = anom.getPublicAppearanceMonth() / 12;
                // Historical anomaly quantities are not stored at month resolution yet. Only
                // use live quantities for the current selected month; never project today's
                // remaining reserve into an earlier historical view.
                boolean currentSelectedMonth = targetTotalMonths == engine.getTimeline().getTotalElapsedMonths();
                if (anom.isPubliclyObservable() && appearanceYear <= targetYear
                        && (currentSelectedMonth ? anom.getRemainingQuantity() > 0.0 : false)) {
                    anomalyMarkers.add(new MapAnomalyDto(
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
            }
        }

        List<MapExpeditionDto> expMarkers = new ArrayList<>();
        if (targetYear == engine.getTimeline().getYear() && engine.getAnomalySystem() != null) {
            for (Expedition exp : engine.getAnomalySystem().getActiveExpeditions()) {
                expMarkers.add(new MapExpeditionDto(
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
        }

        HistoricalDataResolution res = (targetYear == engine.getTimeline().getYear())
                ? HistoricalDataResolution.FULL_HISTORICAL_STATE
                : (reportForYear != null ? HistoricalDataResolution.AGGREGATED_HISTORICAL_DATA : HistoricalDataResolution.PARTIAL_HISTORICAL_STATE);

        return new HistoricalMapSnapshotDto(
                worldId,
                targetYear,
                targetMonth,
                targetTotalMonths,
                width,
                height,
                settlementMarkers,
                lairMarkers,
                anomalyMarkers,
                expMarkers,
                res,
                targetYear != engine.getTimeline().getYear()
        );
    }

    public static HistoricalWorldSummaryDto buildHistoricalWorldSummary(SimulationEngine engine, int targetYear, int targetMonth, String worldId) {
        long targetTotalMonths = (long) targetYear * 12 + targetMonth;
        String era = targetYear >= 50 ? "Awakening Era" : "Mundane Era";

        AnnualReport report = null;
        for (AnnualReport ar : engine.getAnnualReports()) {
            if (ar.getYear() == targetYear) {
                report = ar;
                break;
            }
        }

        int livingPop;
        int settlementCount;
        int clanCount;
        int battlesCount = (int) CombatEngine.getAllCachedBattles().stream().filter(b -> b.getSimulationYear() <= targetYear).count();
        int lairsCount = (engine.getMonsterEcologySystem() != null) ? (int) engine.getMonsterEcologySystem().getAllLairs().stream().filter(l -> l.getLastSpawnMonth() / 12 <= targetYear).count() : 0;
        int anomaliesCount = (engine.getAnomalySystem() != null) ? (int) engine.getAnomalySystem().getAllAnomalies().stream().filter(a -> a.isPubliclyObservable() && a.getPublicAppearanceMonth() / 12 <= targetYear).count() : 0;
        HistoricalDataResolution res;
        List<String> availableFields = new ArrayList<>();
        List<String> missingFields = new ArrayList<>();

        if (report != null) {
            livingPop = report.getTotalLivingPopulation();
            settlementCount = report.getVillageLivingPopulations() != null ? report.getVillageLivingPopulations().size() : engine.getVillages().size();
            clanCount = report.getActiveFamilies();
            res = (targetYear == engine.getTimeline().getYear() && targetMonth == engine.getTimeline().getMonth())
                    ? HistoricalDataResolution.FULL_HISTORICAL_STATE
                    : HistoricalDataResolution.AGGREGATED_HISTORICAL_DATA;
            availableFields.addAll(List.of("totalLivingPopulation", "villagePopulations", "births", "deaths", "marriages", "migrations", "activeClans", "battles", "lairs", "anomalies", "era"));
        } else {
            livingPop = engine.getTotalLivingPopulation();
            settlementCount = engine.getVillages().size();
            clanCount = (engine.getOrganizationSystem() != null) ? engine.getOrganizationSystem().getActiveOrganizations().size() : 0;
            res = (targetYear == engine.getTimeline().getYear()) ? HistoricalDataResolution.FULL_HISTORICAL_STATE : HistoricalDataResolution.PARTIAL_HISTORICAL_STATE;
            availableFields.addAll(List.of("currentPopulation", "settlementsCount", "activeClansCount", "era"));
            if (targetYear != engine.getTimeline().getYear()) {
                missingFields.addAll(List.of("historicalDemographics", "annualVitalStats"));
            }
        }

        return new HistoricalWorldSummaryDto(
                worldId,
                targetYear,
                targetMonth,
                targetTotalMonths,
                era,
                livingPop,
                settlementCount,
                clanCount,
                battlesCount,
                lairsCount,
                anomaliesCount,
                res,
                availableFields,
                missingFields
        );
    }

    public static HistoricalTimelineDto buildHistoricalTimeline(SimulationEngine engine, String worldId, civsim.persistence.WorldPersistenceService persistenceService) {
        int minYear = 0;
        int maxYear = engine.getTimeline().getYear();
        int currentLiveYear = engine.getTimeline().getYear();
        int currentLiveMonth = engine.getTimeline().getMonth();

        List<HistoricalSnapshotMetadataDto> snapshots = new ArrayList<>();
        List<Integer> milestoneYears = List.of(0, 50);
        Set<Integer> checkpointYearSet = new TreeSet<>();

        if (persistenceService != null) {
            for (WorldSaveDetailDto save : persistenceService.listSaveDetails()) {
                checkpointYearSet.add(save.year());
                snapshots.add(new HistoricalSnapshotMetadataDto(
                        save.saveId(),
                        worldId,
                        save.year(),
                        save.month(),
                        save.totalMonths(),
                        save.createdAt(),
                        "FULL_CHECKPOINT",
                        HistoricalDataResolution.FULL_HISTORICAL_STATE,
                        "3.5",
                        2,
                        save.isRestorable(),
                        save.checksum(),
                        save.worldSeed(),
                        save.worldName()
                ));
            }
        }

        for (AnnualReport ar : engine.getAnnualReports()) {
            boolean isCheckpoint = checkpointYearSet.contains(ar.getYear());
            snapshots.add(new HistoricalSnapshotMetadataDto(
                    "snap_annual_y" + ar.getYear(),
                    worldId,
                    ar.getYear(),
                    12,
                    (long) ar.getYear() * 12 + 12,
                    System.currentTimeMillis(),
                    "ANNUAL_ARCHIVE",
                    HistoricalDataResolution.FULL_HISTORICAL_STATE,
                    "3.5",
                    2,
                    isCheckpoint,
                    "ANNUAL_REPORT_VALID",
                    engine.getConfig().getRandomSeed(),
                    "Annual Record Year " + ar.getYear()
            ));
        }

        snapshots.sort(Comparator.comparingInt(HistoricalSnapshotMetadataDto::year));

        return new HistoricalTimelineDto(
                worldId,
                minYear,
                maxYear,
                currentLiveYear,
                currentLiveMonth,
                snapshots,
                milestoneYears,
                new ArrayList<>(checkpointYearSet)
        );
    }

    public static HistoricalDateComparisonDto buildHistoricalDateComparison(SimulationEngine engine, int year1, int year2, String worldId) {
        AnnualReport ar1 = null;
        AnnualReport ar2 = null;
        for (AnnualReport ar : engine.getAnnualReports()) {
            if (ar.getYear() == year1) ar1 = ar;
            if (ar.getYear() == year2) ar2 = ar;
        }

        int pop1 = ar1 != null ? ar1.getTotalLivingPopulation() : 0;
        int pop2 = ar2 != null ? ar2.getTotalLivingPopulation() : 0;
        int deltaPop = pop2 - pop1;
        double growth = pop1 > 0 ? ((double) deltaPop / pop1) * 100.0 : 0.0;

        int s1 = ar1 != null && ar1.getVillageLivingPopulations() != null ? ar1.getVillageLivingPopulations().size() : engine.getVillages().size();
        int s2 = ar2 != null && ar2.getVillageLivingPopulations() != null ? ar2.getVillageLivingPopulations().size() : engine.getVillages().size();

        int c1 = ar1 != null ? ar1.getActiveFamilies() : ((engine.getOrganizationSystem() != null) ? engine.getOrganizationSystem().getActiveOrganizations().size() : 0);
        int c2 = ar2 != null ? ar2.getActiveFamilies() : ((engine.getOrganizationSystem() != null) ? engine.getOrganizationSystem().getActiveOrganizations().size() : 0);

        int b1 = (int) CombatEngine.getAllCachedBattles().stream().filter(b -> b.getSimulationYear() <= year1).count();
        int b2 = (int) CombatEngine.getAllCachedBattles().stream().filter(b -> b.getSimulationYear() <= year2).count();

        int t1 = (engine.getMonsterEcologySystem() != null) ? (int) engine.getMonsterEcologySystem().getTides().values().stream().filter(t -> t.getMobilizedYear() <= year1).count() : 0;
        int t2 = (engine.getMonsterEcologySystem() != null) ? (int) engine.getMonsterEcologySystem().getTides().values().stream().filter(t -> t.getMobilizedYear() <= year2).count() : 0;

        int a1 = (engine.getAnomalySystem() != null) ? (int) engine.getAnomalySystem().getAllAnomalies().stream().filter(a -> a.isPubliclyObservable() && a.getPublicAppearanceMonth() / 12 <= year1).count() : 0;
        int a2 = (engine.getAnomalySystem() != null) ? (int) engine.getAnomalySystem().getAllAnomalies().stream().filter(a -> a.isPubliclyObservable() && a.getPublicAppearanceMonth() / 12 <= year2).count() : 0;

        Map<String, Integer> vpop1 = ar1 != null && ar1.getVillageLivingPopulations() != null ? ar1.getVillageLivingPopulations() : Map.of();
        Map<String, Integer> vpop2 = ar2 != null && ar2.getVillageLivingPopulations() != null ? ar2.getVillageLivingPopulations() : Map.of();

        return new HistoricalDateComparisonDto(
                worldId,
                year1,
                year2,
                pop1,
                pop2,
                deltaPop,
                Math.round(growth * 10.0) / 10.0,
                s1,
                s2,
                c1,
                c2,
                b1,
                b2,
                t1,
                t2,
                a1,
                a2,
                vpop1,
                vpop2
        );
    }

    public static SettlementHistoryDto buildSettlementHistory(SimulationEngine engine, int settlementId) {
        Village targetVillage = null;
        for (Village v : engine.getVillages()) {
            if (v.getId() == settlementId) {
                targetVillage = v;
                break;
            }
        }

        if (targetVillage == null) return null;

        List<SettlementHistoryPointDto> points = new ArrayList<>();
        for (AnnualReport ar : engine.getAnnualReports()) {
            if (ar.getVillageStats() != null) {
                for (VillageAnnualStats vas : ar.getVillageStats()) {
                    if (vas.getVillageName().equalsIgnoreCase(targetVillage.getName())) {
                        points.add(new SettlementHistoryPointDto(
                                vas.getYear(),
                                vas.getLivingPopulation(),
                                vas.getFoodStockpile(),
                                vas.getWoodStockpile(),
                                vas.getIronStoneStockpile(),
                                vas.getToolsStockpile(),
                                vas.getTreasury(),
                                vas.getTechLevel(),
                                vas.getHousingCapacity(),
                                0.0,
                                vas.getTier().name(),
                                vas.getLeaderName() != null ? vas.getLeaderName() : "None"
                        ));
                    }
                }
            }
        }

        points.sort(Comparator.comparingInt(SettlementHistoryPointDto::year));

        return new SettlementHistoryDto(
                targetVillage.getId(),
                targetVillage.getName(),
                targetVillage.getBiome() != null ? targetVillage.getBiome().name() : "RIVER_VALLEY",
                targetVillage.getX(),
                targetVillage.getY(),
                0,
                false,
                null,
                points
        );
    }

    public static PersonDetailDto buildHistoricalPersonProfile(SimulationEngine engine, int personId, int targetYear) {
        Person p = engine.getAllPeopleIncludingDeceased().stream()
                .filter(person -> person.getId() == personId)
                .findFirst()
                .orElse(null);
        if (p == null) return null;
        if (p.getBirthYear() > targetYear) {
            return null; // Not yet born
        }

        boolean aliveAtDate = (p.getDeathYear() == null || p.getDeathYear() >= targetYear);
        int ageAtDate = Math.max(0, targetYear - p.getBirthYear());
        Integer deathYearAtDate = (p.getDeathYear() != null && p.getDeathYear() <= targetYear) ? p.getDeathYear() : null;

        boolean awakenedAtDate = p.isAwakened() && targetYear >= 50;

        PersonDetailDto liveProfile = buildPersonDetail(p, engine.getOrganizationSystem(), engine.getEquipmentCraftingSystem(), engine.getClanHeirloomSystem());
        return new PersonDetailDto(
                liveProfile.id(),
                liveProfile.name(),
                liveProfile.gender(),
                ageAtDate,
                aliveAtDate,
                p.getBirthYear(),
                deathYearAtDate,
                deathYearAtDate != null ? p.getAgeAtDeath() : null,
                deathYearAtDate != null ? p.getCauseOfDeath() : null,
                liveProfile.villageId(),
                liveProfile.villageName(),
                liveProfile.occupation(),
                liveProfile.wealth(),
                liveProfile.bankBalance(),
                liveProfile.loanDebt(),
                awakenedAtDate,
                awakenedAtDate ? liveProfile.supernaturalClass() : "NONE",
                awakenedAtDate ? liveProfile.aspect() : "NONE",
                awakenedAtDate ? liveProfile.rarity() : "COMMON",
                awakenedAtDate ? liveProfile.tier() : 0,
                liveProfile.maxTier(),
                awakenedAtDate ? liveProfile.mastery() : 0.0,
                liveProfile.luck(),
                liveProfile.greed(),
                liveProfile.darkResonance(),
                awakenedAtDate ? liveProfile.derivedPower() : 0.0,
                liveProfile.breakthroughAttempts(),
                liveProfile.breakthroughSuccesses(),
                liveProfile.equippedItems(),
                liveProfile.equipmentDetails(),
                liveProfile.combatProfile(),
                liveProfile.attributes(),
                liveProfile.familyId(),
                liveProfile.familySurname(),
                liveProfile.fatherId(),
                liveProfile.fatherName(),
                liveProfile.motherId(),
                liveProfile.motherName(),
                liveProfile.spouseId(),
                liveProfile.spouseName(),
                liveProfile.childrenIds(),
                liveProfile.organizationMemberships(),
                liveProfile.achievements()
        );
    }

    // =========================================================================
    // Phase 3.6: Advanced Map Overlays, Heatmaps & Battle Replay
    // =========================================================================

    public static WorldOverlaysDto buildWorldOverlays(SimulationEngine engine, String worldId) {
        int year = engine.getTimeline().getYear();
        int month = engine.getTimeline().getMonth();
        return buildOverlaysInternal(engine, year, month, false, worldId);
    }

    public static WorldOverlaysDto buildHistoricalOverlays(SimulationEngine engine, int targetYear, int targetMonth, String worldId) {
        return buildOverlaysInternal(engine, targetYear, targetMonth, true, worldId);
    }

    private static WorldOverlaysDto buildOverlaysInternal(SimulationEngine engine, int year, int month, boolean isHistorical, String worldId) {
        List<Village> villages = engine.getVillages();
        List<SettlementTerritoryDto> territories = new ArrayList<>();
        List<DiplomaticLinkDto> diplomaticLinks = new ArrayList<>();
        List<ActiveConflictDto> activeConflicts = new ArrayList<>();
        List<MonsterInvasionVectorDto> monsterInvasions = new ArrayList<>();

        List<HeatmapPointDto> threatPoints = new ArrayList<>();
        List<HeatmapPointDto> aetherPoints = new ArrayList<>();
        List<HeatmapPointDto> popDensityPoints = new ArrayList<>();
        List<HeatmapPointDto> prosperityPoints = new ArrayList<>();

        Set<String> activeHistoricalVillageNames = new HashSet<>();
        if (isHistorical) {
            for (AnnualReport ar : engine.getAnnualReports()) {
                if (ar.getYear() == year) {
                    if (ar.getVillageStats() != null) {
                        for (VillageAnnualStats vs : ar.getVillageStats()) {
                            activeHistoricalVillageNames.add(vs.getVillageName());
                        }
                    }
                    break;
                }
            }
        }

        // 1. Settlement Territories & Demographics Heatmap
        for (Village v : villages) {
            if (isHistorical && !activeHistoricalVillageNames.isEmpty() && !activeHistoricalVillageNames.contains(v.getName())) {
                continue;
            }
            int pop = v.getLivingPopulationCount();
            double radius = Math.max(8.0, 6.0 + Math.sqrt(Math.max(1, pop)) * 0.75);

            // Dominant clan/surname
            Map<String, Integer> surnameCounts = new HashMap<>();
            for (Person p : v.getResidents()) {
                if (p.isAlive() && p.getFamily() != null && p.getFamily().getSurname() != null && !p.getFamily().getSurname().isBlank()) {
                    surnameCounts.merge(p.getFamily().getSurname(), 1, Integer::sum);
                }
            }
            String dominantClan = surnameCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("Independent");

            List<Integer> alliedIds = new ArrayList<>();
            List<Integer> hostileIds = new ArrayList<>();
            for (Village other : villages) {
                if (other.getId() == v.getId()) continue;
                DiplomaticStatus status = v.getRelationWith(other.getId());
                if (status == DiplomaticStatus.ALLIED) alliedIds.add(other.getId());
                else if (status == DiplomaticStatus.AT_WAR) hostileIds.add(other.getId());
            }

            territories.add(new SettlementTerritoryDto(
                    v.getId(),
                    v.getName(),
                    v.getX(),
                    v.getY(),
                    round(radius),
                    v.getTier() != null ? v.getTier().name() : "HAMLET",
                    dominantClan,
                    alliedIds,
                    hostileIds
            ));

            // Population Density Heatmap point
            double popIntensity = Math.min(1.0, pop / 350.0);
            popDensityPoints.add(new HeatmapPointDto(v.getX(), v.getY(), round(popIntensity), round(radius * 1.4), "VIL_" + v.getId(), v.getName()));

            // Economic Prosperity Heatmap point
            double wealthTotal = v.getTotalLiquidWealth() + v.getTreasury();
            double prosperityIntensity = Math.min(1.0, wealthTotal / 250.0);
            prosperityPoints.add(new HeatmapPointDto(v.getX(), v.getY(), round(prosperityIntensity), round(radius * 1.2), "PROSP_" + v.getId(), v.getName()));
        }

        // 2. Diplomatic Links (Bilateral)
        for (int i = 0; i < villages.size(); i++) {
            Village v1 = villages.get(i);
            if (isHistorical && !activeHistoricalVillageNames.isEmpty() && !activeHistoricalVillageNames.contains(v1.getName())) continue;
            for (int j = i + 1; j < villages.size(); j++) {
                Village v2 = villages.get(j);
                if (isHistorical && !activeHistoricalVillageNames.isEmpty() && !activeHistoricalVillageNames.contains(v2.getName())) continue;

                DiplomaticStatus st = v1.getRelationWith(v2.getId());
                if (st == DiplomaticStatus.ALLIED || st == DiplomaticStatus.AT_WAR) {
                    diplomaticLinks.add(new DiplomaticLinkDto(
                            v1.getId(), v1.getName(), v1.getX(), v1.getY(),
                            v2.getId(), v2.getName(), v2.getX(), v2.getY(),
                            st.name(),
                            st == DiplomaticStatus.ALLIED ? round(25.0 + (v1.getLivingPopulationCount() + v2.getLivingPopulationCount()) * 0.1) : 0.0,
                            st == DiplomaticStatus.ALLIED ? "Commercial Trade" : "Border Feud"
                    ));
                }
            }
        }

        // 3. Monster Threat Points & Invasions
        if (engine.getMonsterEcologySystem() != null) {
            MonsterEcologySystem mes = engine.getMonsterEcologySystem();
            for (MonsterLair lair : mes.getAllLairs()) {
                if (lair.getState() != MonsterLairState.ACTIVE) continue;
                if (isHistorical && (lair.getLastSpawnMonth() / 12) > year) continue;

                double threatMultiplier = (lair.getDominantThreatRank() != null) ? lair.getDominantThreatRank().getPowerMultiplier() / 3.0 : 0.35;
                double threatIntensity = Math.min(1.0, (lair.getMonsterCount() / Math.max(1.0, lair.getPopulationCapacity())) * threatMultiplier);
                threatPoints.add(new HeatmapPointDto(lair.getPosX(), lair.getPosY(), round(threatIntensity), round(lair.getTerritoryRadius()), lair.getLairId(), lair.getPrimarySpeciesId() + " Lair"));
            }

            // Active Monster Tides
            long currentTotalMonths = (long) year * 12 + month;
            for (MonsterTide tide : mes.getTides().values()) {
                if (isHistorical && (tide.getMobilizedYear() > year || tide.getArrivalYear() < year)) continue;
                if (!isHistorical && (tide.getState() == MonsterTideState.RESOLVED)) continue;

                MonsterLair lair = mes.getLair(tide.getSourceLairId());
                double lairX = (lair != null) ? lair.getPosX() : 60.0;
                double lairY = (lair != null) ? lair.getPosY() : 60.0;

                double curX, curY;
                if (tide.getState() == MonsterTideState.INVADING || tide.getState() == MonsterTideState.RESOLVED) {
                    curX = tide.getTargetX();
                    curY = tide.getTargetY();
                } else {
                    double prog = Math.min(1.0, Math.max(0.0, (double) (currentTotalMonths - tide.getMobilizedTotalMonths()) / Math.max(1, tide.getTravelDurationMonths())));
                    curX = round(lairX + prog * (tide.getTargetX() - lairX));
                    curY = round(lairY + prog * (tide.getTargetY() - lairY));
                }

                monsterInvasions.add(new MonsterInvasionVectorDto(
                        tide.getTideId(),
                        lairX,
                        lairY,
                        tide.getTargetX(),
                        tide.getTargetY(),
                        curX,
                        curY,
                        tide.getState().name(),
                        tide.getTotalMobilizedCount(),
                        tide.getTargetVillageName(),
                        tide.getTargetVillageId()
                ));

                threatPoints.add(new HeatmapPointDto(curX, curY, 0.95, 20.0, tide.getTideId(), "MonsterTide Vanguard"));
            }
        }

        // 4. Aetheric / Anomaly Activity
        if (engine.getAnomalySystem() != null) {
            for (WorldAnomaly anom : engine.getAnomalySystem().getAllAnomalies()) {
                if (!anom.isPubliclyObservable()) continue;
                if (isHistorical && (anom.getPublicAppearanceMonth() / 12) > year) continue;

                double gradeMult = (anom.getResourceGrade() != null) ? anom.getResourceGrade().getValueMultiplier() / 3.0 : 0.40;
                double aetherIntensity = Math.min(1.0, (anom.getRemainingQuantity() / Math.max(1.0, anom.getInitialQuantity())) * gradeMult);
                aetherPoints.add(new HeatmapPointDto(anom.getPosX(), anom.getPosY(), round(aetherIntensity), 18.0, anom.getAnomalyId(), anom.getResourceCategory()));
            }
        }

        // 5. Active Conflicts (from CombatEngine cached battles)
        for (BattleResult battle : CombatEngine.getAllCachedBattles()) {
            if (isHistorical) {
                if (battle.getSimulationYear() != year) continue;
            } else {
                if (Math.abs(battle.getSimulationYear() - year) > 1) continue;
            }

            double bx = 60.0;
            double by = 60.0;
            if (!villages.isEmpty()) {
                int targetId = Math.abs(battle.getBattleId().hashCode()) % villages.size();
                Village bv = villages.get(targetId);
                bx = bv.getX();
                by = bv.getY();
            }

            activeConflicts.add(new ActiveConflictDto(
                    battle.getBattleId(),
                    "Battle of " + battle.getVictoriousSideId(),
                    bx,
                    by,
                    battle.getBattleType() != null ? battle.getBattleType().name() : "SKIRMISH",
                    battle.getTotalRounds() * 4,
                    battle.getSimulationYear(),
                    battle.getSimulationMonth(),
                    battle.getOutcome() != null ? battle.getOutcome().name() : "VICTORY"
            ));
        }

        HeatmapsDto heatmaps = new HeatmapsDto(threatPoints, aetherPoints, popDensityPoints, prosperityPoints);
        return new WorldOverlaysDto(worldId, year, month, isHistorical, territories, diplomaticLinks, activeConflicts, monsterInvasions, heatmaps);
    }

    private static double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
