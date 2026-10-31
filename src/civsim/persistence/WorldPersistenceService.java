package civsim.persistence;

import civsim.ProgressionConfig;
import civsim.SimulationConfig;
import civsim.SimulationEngine;
import civsim.dto.WorldSaveSummaryDto;
import civsim.model.*;
import civsim.spatial.Biome;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Handles atomic serialization and deterministic restoration of simulation worlds.
 */
@Service
public class WorldPersistenceService {

    private final Path saveDirectory;
    private final ObjectMapper objectMapper;

    public WorldPersistenceService() {
        this(Paths.get("saves"));
    }

    public WorldPersistenceService(Path saveDirectory) {
        this.saveDirectory = saveDirectory;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        try {
            if (!Files.exists(this.saveDirectory)) {
                Files.createDirectories(this.saveDirectory);
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not create save directory: " + e.getMessage());
        }
    }

    public WorldPersistenceService(String saveDirectoryPath) {
        this(Paths.get(saveDirectoryPath));
    }

    public Path getSaveDirectory() {
        return saveDirectory;
    }

    public synchronized WorldSaveSummaryDto saveWorld(SimulationEngine engine, String customSaveId) throws IOException {
        return saveWorld(engine, customSaveId, "default_world");
    }

    public synchronized WorldSaveSummaryDto saveWorld(SimulationEngine engine, String customSaveId, String worldId) throws IOException {
        String saveId = (customSaveId != null && !customSaveId.isBlank())
                ? sanitizeSaveId(customSaveId)
                : generateSaveId(engine);

        WorldSaveData data = serializeEngine(engine, worldId);

        Path finalPath = saveDirectory.resolve(saveId + ".json");
        Path tmpPath = saveDirectory.resolve(saveId + ".tmp");

        // Write to temporary file first
        objectMapper.writeValue(tmpPath.toFile(), data);

        // Atomic move
        Files.move(tmpPath, finalPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);

        return data.toSummary(saveId);
    }

    public synchronized SimulationEngine loadWorld(String saveId) throws IOException {
        String cleanId = sanitizeSaveId(saveId);
        Path savePath = saveDirectory.resolve(cleanId + ".json");

        if (!Files.exists(savePath)) {
            throw new NoSuchFileException("Save file not found: " + cleanId);
        }

        WorldSaveData data = objectMapper.readValue(savePath.toFile(), WorldSaveData.class);
        if (data.schemaVersion != 1) {
            throw new IllegalArgumentException("Unsupported save schema version: " + data.schemaVersion);
        }

        return deserializeEngine(data);
    }

    public synchronized List<WorldSaveSummaryDto> listSaves() {
        List<WorldSaveSummaryDto> list = new ArrayList<>();
        if (!Files.exists(saveDirectory)) return list;

        File[] files = saveDirectory.toFile().listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return list;

        for (File f : files) {
            try {
                String saveId = f.getName().replace(".json", "");
                WorldSaveData data = objectMapper.readValue(f, WorldSaveData.class);
                list.add(data.toSummary(saveId));
            } catch (Exception ignored) {}
        }

        list.sort((a, b) -> Long.compare(b.createdAt(), a.createdAt()));
        return list;
    }

    public static String sanitizeSaveId(String saveId) {
        if (saveId == null || !saveId.matches("^[a-zA-Z0-9_-]+$")) {
            throw new IllegalArgumentException("Invalid save ID. Only alphanumeric, dashes, and underscores allowed.");
        }
        return saveId;
    }

    private String generateSaveId(SimulationEngine engine) {
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return String.format("save_%s_y%d_m%d", stamp, engine.getTimeline().getYear(), engine.getTimeline().getMonth());
    }

    private WorldSaveData serializeEngine(SimulationEngine engine, String worldId) {
        WorldSaveData data = new WorldSaveData();
        data.worldId = (worldId != null) ? worldId : "world-default";
        data.createdAt = System.currentTimeMillis();
        data.year = engine.getTimeline().getYear();
        data.month = engine.getTimeline().getMonth();
        data.totalMonths = engine.getTimeline().getTotalElapsedMonths();
        data.randomSeed = engine.getConfig().getRandomSeed();
        data.lifecycleState = engine.getLifecycleState().name();

        data.nextPersonId = engine.getNextPersonId();
        data.nextFamilyId = engine.getNextFamilyId();
        data.nextCoupleId = engine.getNextCoupleId();
        data.nextVillageId = engine.getNextVillageId();
        data.totalRecordedPeople = engine.getTotalRecordedPeopleCount();

        // Villages
        for (Village v : engine.getVillages()) {
            WorldSaveData.VillageSaveData vsd = new WorldSaveData.VillageSaveData();
            vsd.id = v.getId();
            vsd.name = v.getName();
            vsd.x = v.getX();
            vsd.y = v.getY();
            vsd.biome = v.getBiome().name();
            vsd.tier = v.getTier().name();
            vsd.food = v.getFoodStockpile();
            vsd.wood = v.getWoodStockpile();
            vsd.ore = v.getOreStockpile();
            vsd.tools = v.getToolsStockpile();
            vsd.treasury = v.getTreasury();
            vsd.techLevel = v.getTechLevel();
            vsd.fortification = v.getFortificationLevel();
            vsd.leaderId = v.getLeader() != null ? v.getLeader().getId() : null;
            data.villages.add(vsd);
        }

        // Families
        for (Family f : engine.getAllFamilies()) {
            WorldSaveData.FamilySaveData fsd = new WorldSaveData.FamilySaveData();
            fsd.id = f.getId();
            fsd.surname = f.getSurname();
            fsd.foundedYear = f.getFoundedYear();
            fsd.founderPersonId = f.getFounder() != null ? f.getFounder().getId() : null;
            data.families.add(fsd);
        }

        // People
        for (Person p : engine.getAllPeople()) {
            WorldSaveData.PersonSaveData psd = new WorldSaveData.PersonSaveData();
            psd.id = p.getId();
            psd.name = p.getName();
            psd.gender = p.getGender().name();
            psd.age = p.getAge();
            psd.birthYear = p.getBirthYear();
            psd.deathYear = p.getDeathYear();
            psd.ageAtDeath = p.getAgeAtDeath();
            psd.causeOfDeath = p.getCauseOfDeath();
            psd.alive = p.isAlive();
            psd.villageId = p.getVillage() != null ? p.getVillage().getId() : null;
            psd.familyId = p.getFamily() != null ? p.getFamily().getId() : null;
            psd.occupation = p.getOccupation() != null ? p.getOccupation().name() : "CITIZEN";
            psd.wealth = p.getWealth();
            psd.bankBalance = p.getBankBalance();
            psd.loanDebt = p.getLoanDebt();

            if (p.isAwakened() && p.getAetherProfile() != null) {
                AetherProfile ap = p.getAetherProfile();
                psd.isAwakened = true;
                psd.supernaturalClass = ap.getSupernaturalClass().name();
                psd.aspect = ap.getAspect().name();
                psd.rarity = ap.getRarity().name();
                psd.tier = ap.getPowerTier();
                psd.mastery = ap.getMastery();
                psd.luck = ap.getLuck();
                psd.greed = ap.getGreed();
                psd.darkResonance = ap.getDarkResonance();
                psd.derivedPower = ap.getDerivedPower();
            }

            psd.fatherId = p.getFather() != null ? p.getFather().getId() : null;
            psd.motherId = p.getMother() != null ? p.getMother().getId() : null;
            psd.spouseId = p.getSpouse() != null ? p.getSpouse().getId() : null;
            for (Person c : p.getChildren()) {
                psd.childrenIds.add(c.getId());
            }
            for (Map.Entry<EquipmentSlot, String> e : p.getEquippedItemIds().entrySet()) {
                psd.equippedItems.put(e.getKey().name(), e.getValue());
            }
            data.people.add(psd);
        }

        // Couples
        for (Couple c : engine.getAllCouples()) {
            WorldSaveData.CoupleSaveData csd = new WorldSaveData.CoupleSaveData();
            csd.id = c.getId();
            csd.partner1Id = c.getHusband() != null ? c.getHusband().getId() : 0;
            csd.partner2Id = c.getWife() != null ? c.getWife().getId() : 0;
            csd.marriageYear = c.getMarriageYear();
            csd.plannedChildren = c.getTargetChildren();
            csd.childrenBorn = c.getChildrenBorn();
            data.couples.add(csd);
        }

        // Organizations
        for (Organization o : engine.getOrganizationSystem().getAllOrganizations()) {
            WorldSaveData.OrgSaveData osd = new WorldSaveData.OrgSaveData();
            osd.id = o.getOrganizationId();
            osd.name = o.getDisplayName();
            osd.type = o.getOrganizationType().name();
            osd.alignment = o.getAlignment().name();
            osd.status = o.getStatus().name();
            osd.foundingYear = o.getFoundingYear();
            osd.foundingMonth = o.getFoundingMonth();
            osd.founderId = o.getFounderPersonId();
            osd.leaderId = o.getCurrentLeaderPersonId();
            osd.reputation = o.getReputation();
            osd.battlesWon = o.getBattlesWon();
            osd.battlesTotal = o.getBattlesParticipated();
            for (ClanHeirloom h : o.getHeirlooms().values()) {
                osd.heirloomNames.add(h.getCustomName());
            }
            data.organizations.add(osd);
        }

        return data;
    }

    private SimulationEngine deserializeEngine(WorldSaveData data) {
        SimulationConfig config = SimulationConfig.createDefaultWithSeed(data.randomSeed);
        SimulationEngine engine = new SimulationEngine(config);

        // Timeline
        engine.getTimeline().setYearAndMonth(data.year, data.month);

        // Sequence counters
        engine.setNextPersonId(data.nextPersonId);
        engine.setNextFamilyId(data.nextFamilyId);
        engine.setNextCoupleId(data.nextCoupleId);
        engine.setNextVillageId(data.nextVillageId);
        engine.setTotalRecordedPeopleCount(data.totalRecordedPeople);
        engine.setLifecycleState(SimulationLifecycleState.valueOf(data.lifecycleState));
        engine.clearEntitiesForLoad();

        // Maps for linkage
        Map<Integer, Village> villageMap = new HashMap<>();
        Map<Integer, Family> familyMap = new HashMap<>();
        Map<Integer, Person> personMap = new HashMap<>();

        // 1. Rebuild Villages
        for (WorldSaveData.VillageSaveData vsd : data.villages) {
            Biome b = Biome.valueOf(vsd.biome);
            Village v = new Village(vsd.id, vsd.name, 2.0, 0.04, vsd.x, vsd.y, b);
            v.setTier(SettlementTier.valueOf(vsd.tier));
            v.getStockpile().addFood(vsd.food);
            v.getStockpile().addWood(vsd.wood);
            v.getStockpile().addOre(vsd.ore);
            v.getStockpile().addTools(vsd.tools);
            v.setTreasury(vsd.treasury);
            v.setTechLevel(vsd.techLevel);
            v.setFortificationLevel(vsd.fortification);
            engine.addVillage(v);
            villageMap.put(v.getId(), v);
        }

        // 2. Rebuild Families
        for (WorldSaveData.FamilySaveData fsd : data.families) {
            Family f = new Family(fsd.id, fsd.surname, fsd.foundedYear, null);
            engine.addFamily(f);
            familyMap.put(f.getId(), f);
        }

        // 3. Rebuild People
        for (WorldSaveData.PersonSaveData psd : data.people) {
            Gender g = Gender.valueOf(psd.gender);
            Village v = villageMap.get(psd.villageId);
            Family f = familyMap.get(psd.familyId);

            Person p = new Person(psd.id, psd.name, g, psd.age, psd.birthYear, v, null, null, f, psd.wealth);
            p.setBankBalance(psd.bankBalance);
            p.setLoanDebt(psd.loanDebt);
            p.setOccupation(Occupation.valueOf(psd.occupation));

            if (!psd.alive) {
                p.markDeceased(psd.deathYear != null ? psd.deathYear : data.year, psd.causeOfDeath);
            }

            if (psd.isAwakened && psd.supernaturalClass != null) {
                SupernaturalClass sc = SupernaturalClass.valueOf(psd.supernaturalClass);
                AetherAspect asp = AetherAspect.valueOf(psd.aspect);
                ClassRarity rar = ClassRarity.valueOf(psd.rarity);
                AetherProfile ap = AetherProfile.createAwakened(sc, asp, rar, psd.tier, psd.mastery);
                p.setAetherProfile(ap);
            }

            if (v != null && p.isAlive()) {
                v.addResident(p);
            }

            if (f != null) {
                f.addMember(p);
            }

            engine.addPerson(p);
            personMap.put(p.getId(), p);
        }

        // 3b. Link Family Founders
        for (WorldSaveData.FamilySaveData fsd : data.families) {
            if (fsd.founderPersonId != null) {
                Family f = familyMap.get(fsd.id);
                Person founder = personMap.get(fsd.founderPersonId);
                if (f != null && founder != null) {
                    f.setFounder(founder);
                }
            }
        }

        // 4. Link Kinship (Mother, Father, Spouse, Children)
        for (WorldSaveData.PersonSaveData psd : data.people) {
            Person p = personMap.get(psd.id);
            if (p != null) {
                if (psd.fatherId != null) p.setFather(personMap.get(psd.fatherId));
                if (psd.motherId != null) p.setMother(personMap.get(psd.motherId));
                if (psd.spouseId != null) p.setSpouse(personMap.get(psd.spouseId));
                for (Integer cid : psd.childrenIds) {
                    Person child = personMap.get(cid);
                    if (child != null && !p.getChildren().contains(child)) {
                        p.addChild(child);
                    }
                }
            }
        }

        // 5. Rebuild Couples
        for (WorldSaveData.CoupleSaveData csd : data.couples) {
            Person p1 = personMap.get(csd.partner1Id);
            Person p2 = personMap.get(csd.partner2Id);
            if (p1 != null && p2 != null) {
                Couple c = new Couple(csd.id, p1, p2, csd.marriageYear, (long) Math.max(0, csd.marriageYear - 1) * 12, csd.plannedChildren, 12);
                c.setChildrenBorn(csd.childrenBorn);
                engine.addCouple(c);
            }
        }

        // 6. Link Village Leaders
        for (WorldSaveData.VillageSaveData vsd : data.villages) {
            if (vsd.leaderId != null) {
                Village v = villageMap.get(vsd.id);
                if (v != null) {
                    v.setLeader(personMap.get(vsd.leaderId));
                }
            }
        }

        engine.setInitialized(true);
        return engine;
    }
}
