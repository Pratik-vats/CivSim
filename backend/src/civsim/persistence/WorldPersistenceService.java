package civsim.persistence;

import civsim.AnnualReport;
import civsim.ProgressionConfig;
import civsim.SimulationConfig;
import civsim.SimulationEngine;
import civsim.dto.*;
import civsim.model.*;
import civsim.spatial.Biome;
import civsim.systems.BloodFeudSystem;
import civsim.systems.ClanHeirloomSystem;
import civsim.systems.EquipmentCraftingSystem;
import civsim.systems.OrganizationSystem;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Handles atomic serialization, integrity validation, and deterministic restoration of simulation worlds.
 * Preserves complete simulation continuation state including RNG stream position, anomalies, and branch provenance.
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
        return saveWorld(engine, customSaveId, "world-001", "CivSim World", null, null, "CONTINUE_CHECKPOINT_RNG");
    }

    public synchronized WorldSaveSummaryDto saveWorld(SimulationEngine engine, String customSaveId, String worldId) throws IOException {
        return saveWorld(engine, customSaveId, worldId, "CivSim World", null, null, "CONTINUE_CHECKPOINT_RNG");
    }

    public synchronized WorldSaveSummaryDto saveWorld(
            SimulationEngine engine,
            String customSaveId,
            String worldId,
            String worldName,
            String parentWorldId,
            String parentCheckpointId,
            String branchRngPolicy
    ) throws IOException {
        String saveId = (customSaveId != null && !customSaveId.isBlank())
                ? sanitizeSaveId(customSaveId)
                : generateSaveId(engine);

        WorldSaveData data = serializeEngine(engine, worldId, worldName, parentWorldId, parentCheckpointId, branchRngPolicy);

        Path finalPath = saveDirectory.resolve(saveId + ".json");
        Path tmpPath = saveDirectory.resolve(saveId + ".tmp");

        // Deterministic checksum computation
        data.checksum = "";
        byte[] contentBytes = objectMapper.writeValueAsBytes(data);
        String checksum = computeSha256(contentBytes);
        data.checksum = checksum;

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
        if (data.schemaVersion < 1 || data.schemaVersion > 3) {
            throw new IllegalArgumentException("Unsupported save schema version: " + data.schemaVersion);
        }

        return deserializeEngine(data);
    }

    public synchronized WorldSaveData loadSaveData(String saveId) throws IOException {
        String cleanId = sanitizeSaveId(saveId);
        Path savePath = saveDirectory.resolve(cleanId + ".json");
        if (!Files.exists(savePath)) {
            throw new NoSuchFileException("Save file not found: " + cleanId);
        }
        return objectMapper.readValue(savePath.toFile(), WorldSaveData.class);
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

    public synchronized List<WorldSaveDetailDto> listSaveDetails() {
        List<WorldSaveDetailDto> list = new ArrayList<>();
        if (!Files.exists(saveDirectory)) return list;

        File[] files = saveDirectory.toFile().listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return list;

        for (File f : files) {
            try {
                String saveId = f.getName().replace(".json", "");
                WorldSaveData data = objectMapper.readValue(f, WorldSaveData.class);
                boolean restorable = validateSaveDataIntegrity(data);
                list.add(data.toDetail(saveId, f.length(), restorable, data.checksum));
            } catch (Exception ignored) {}
        }

        list.sort((a, b) -> Long.compare(b.createdAt(), a.createdAt()));
        return list;
    }

    public synchronized WorldSaveDetailDto getSaveDetail(String saveId) throws IOException {
        String cleanId = sanitizeSaveId(saveId);
        Path savePath = saveDirectory.resolve(cleanId + ".json");
        if (!Files.exists(savePath)) {
            throw new NoSuchFileException("Save file not found: " + cleanId);
        }
        WorldSaveData data = objectMapper.readValue(savePath.toFile(), WorldSaveData.class);
        boolean restorable = validateSaveDataIntegrity(data);
        return data.toDetail(cleanId, Files.size(savePath), restorable, data.checksum);
    }

    public synchronized boolean validateSaveIntegrity(String saveId) {
        try {
            String cleanId = sanitizeSaveId(saveId);
            Path savePath = saveDirectory.resolve(cleanId + ".json");
            if (!Files.exists(savePath)) return false;
            WorldSaveData data = objectMapper.readValue(savePath.toFile(), WorldSaveData.class);
            if (!validateSaveDataIntegrity(data)) return false;
            String recordedChecksum = data.checksum;
            if (recordedChecksum == null || recordedChecksum.isEmpty()) return false;
            data.checksum = "";
            byte[] bytes = objectMapper.writeValueAsBytes(data);
            String calculated = computeSha256(bytes);
            return recordedChecksum.equalsIgnoreCase(calculated);
        } catch (Exception e) {
            return false;
        }
    }

    public synchronized boolean deleteSave(String saveId) throws IOException {
        String cleanId = sanitizeSaveId(saveId);
        Path savePath = saveDirectory.resolve(cleanId + ".json");
        if (!Files.exists(savePath)) {
            return false;
        }

        // Integrity safeguard: check if another save references this save as parentCheckpointId
        List<WorldSaveDetailDto> allSaves = listSaveDetails();
        for (WorldSaveDetailDto s : allSaves) {
            if (cleanId.equals(s.parentCheckpointId())) {
                throw new IllegalStateException("Cannot delete checkpoint " + cleanId + ": it is referenced as the parent ancestor of branch " + s.worldId());
            }
        }

        return Files.deleteIfExists(savePath);
    }

    public synchronized boolean renameSave(String saveId, String newName) throws IOException {
        String cleanId = sanitizeSaveId(saveId);
        Path savePath = saveDirectory.resolve(cleanId + ".json");
        if (!Files.exists(savePath)) {
            return false;
        }
        WorldSaveData data = objectMapper.readValue(savePath.toFile(), WorldSaveData.class);
        data.worldName = (newName != null && !newName.isBlank()) ? newName.trim() : data.worldName;

        Path tmpPath = saveDirectory.resolve(cleanId + ".tmp");
        objectMapper.writeValue(tmpPath.toFile(), data);
        Files.move(tmpPath, savePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        return true;
    }

    public synchronized BranchSummaryDto createBranch(CreateBranchRequest req) throws IOException {
        if (req == null) throw new IllegalArgumentException("Request cannot be null");
        return createBranch(req.sourceSaveId(), req.branchName(), req.rngPolicy());
    }

    public synchronized BranchSummaryDto createBranch(String sourceSaveId, String branchName, String rngPolicy) throws IOException {
        return createBranch(sourceSaveId, branchName, rngPolicy, null);
    }

    public synchronized BranchSummaryDto createBranch(String sourceSaveId, String branchName, String rngPolicy, String customBranchId) throws IOException {
        String cleanId = sanitizeSaveId(sourceSaveId);
        Path savePath = saveDirectory.resolve(cleanId + ".json");
        if (!Files.exists(savePath)) {
            throw new NoSuchFileException("Source checkpoint not found: " + cleanId);
        }

        WorldSaveData sourceData = objectMapper.readValue(savePath.toFile(), WorldSaveData.class);
        if (!validateSaveDataIntegrity(sourceData)) {
            throw new IllegalStateException("Source checkpoint " + cleanId + " failed integrity validation and cannot be forked.");
        }

        SimulationEngine branchEngine = deserializeEngine(sourceData);

        String branchWorldId = (customBranchId != null && !customBranchId.isBlank())
                ? customBranchId.trim()
                : "branch-" + UUID.randomUUID().toString().substring(0, 8);
        String name = (branchName != null && !branchName.isBlank()) ? branchName.trim() : "Timeline Branch " + branchWorldId;
        String policy = ("FORK_RANDOM_STREAM".equalsIgnoreCase(rngPolicy)) ? "FORK_RANDOM_STREAM" : "CONTINUE_CHECKPOINT_RNG";

        // Apply RNG policy
        if ("FORK_RANDOM_STREAM".equals(policy)) {
            long currentContinuationVal = branchEngine.getRandom().nextLong();
            long forkedSeed = currentContinuationVal ^ (((long) branchWorldId.hashCode()) * 0x5DEECE66DL + 11L);
            branchEngine.setRandom(new Random(forkedSeed));
        }

        String branchSaveId = "save_" + branchWorldId + "_y" + branchEngine.getTimeline().getYear() + "_m" + branchEngine.getTimeline().getMonth();
        WorldSaveSummaryDto savedSummary = saveWorld(
                branchEngine,
                branchSaveId,
                branchWorldId,
                name,
                sourceData.worldId,
                cleanId,
                policy
        );

        return new BranchSummaryDto(
                branchWorldId,
                name,
                sourceData.worldId,
                cleanId,
                branchEngine.getTimeline().getYear(),
                branchEngine.getTimeline().getMonth(),
                System.currentTimeMillis(),
                policy,
                branchEngine.getConfig().getRandomSeed(),
                branchEngine.getTimeline().getYear(),
                branchEngine.getTimeline().getMonth(),
                savedSummary.humanPopulation(),
                branchEngine.getLifecycleState().name()
        );
    }

    public synchronized List<BranchSummaryDto> listBranches() {
        List<BranchSummaryDto> branches = new ArrayList<>();
        List<WorldSaveDetailDto> details = listSaveDetails();

        for (WorldSaveDetailDto d : details) {
            if (d.parentWorldId() != null && !d.parentWorldId().isBlank()) {
                branches.add(new BranchSummaryDto(
                        d.worldId(),
                        d.worldName(),
                        d.parentWorldId(),
                        d.parentCheckpointId(),
                        d.year(),
                        d.month(),
                        d.createdAt(),
                        d.branchRngPolicy(),
                        d.worldSeed(),
                        d.year(),
                        d.month(),
                        d.livingPopulation(),
                        d.lifecycleState()
                ));
            }
        }
        return branches;
    }

    public synchronized BranchComparisonDto compareBranches(String world1Id, String world2Id) {
        WorldSaveData data1 = findLatestSaveForWorld(world1Id);
        WorldSaveData data2 = findLatestSaveForWorld(world2Id);

        String name1 = data1 != null ? data1.worldName : world1Id;
        String name2 = data2 != null ? data2.worldName : world2Id;

        List<BranchTimeSeriesPointDto> timeline1 = extractTimeSeries(data1);
        List<BranchTimeSeriesPointDto> timeline2 = extractTimeSeries(data2);

        // Detect divergence point
        boolean divergenceFound = false;
        Integer divergenceYear = null;
        Integer divergenceMonth = null;
        String divergenceDescription = "No divergence detected across recorded annual metrics.";

        int maxYear = Math.min(
                timeline1.isEmpty() ? 0 : timeline1.get(timeline1.size() - 1).year(),
                timeline2.isEmpty() ? 0 : timeline2.get(timeline2.size() - 1).year()
        );

        Map<Integer, BranchTimeSeriesPointDto> map1 = new HashMap<>();
        for (BranchTimeSeriesPointDto p : timeline1) map1.put(p.year(), p);

        for (BranchTimeSeriesPointDto p2 : timeline2) {
            BranchTimeSeriesPointDto p1 = map1.get(p2.year());
            if (p1 != null) {
                if (p1.population() != p2.population() || p1.settlementsCount() != p2.settlementsCount() || p1.battlesCount() != p2.battlesCount()) {
                    divergenceFound = true;
                    divergenceYear = p2.year();
                    divergenceMonth = 1;
                    divergenceDescription = String.format("Divergence surfaced in Year %d: %s population was %d while %s population was %d.",
                            divergenceYear, name1, p1.population(), name2, p2.population());
                    break;
                }
            }
        }

        return new BranchComparisonDto(
                world1Id,
                name1,
                world2Id,
                name2,
                divergenceFound,
                divergenceYear,
                divergenceMonth,
                divergenceDescription,
                timeline1,
                timeline2
        );
    }

    private WorldSaveData findLatestSaveForWorld(String worldId) {
        List<WorldSaveDetailDto> list = listSaveDetails();
        for (WorldSaveDetailDto d : list) {
            if (d.worldId().equals(worldId)) {
                try {
                    return loadSaveData(d.saveId());
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private List<BranchTimeSeriesPointDto> extractTimeSeries(WorldSaveData data) {
        List<BranchTimeSeriesPointDto> points = new ArrayList<>();
        if (data == null) return points;

        if (data.annualReports != null && !data.annualReports.isEmpty()) {
            for (WorldSaveData.AnnualReportSaveData ar : data.annualReports) {
                points.add(new BranchTimeSeriesPointDto(
                        ar.year,
                        ar.totalLivingPopulation,
                        ar.villageLivingPopulations.size(),
                        ar.activeClansCount,
                        ar.battlesCount,
                        data.lairs.size(),
                        data.anomalies.size(),
                        0
                ));
            }
        } else {
            // Point snapshot only
            int living = (int) data.people.stream().filter(p -> p.alive).count();
            points.add(new BranchTimeSeriesPointDto(
                    data.year,
                    living,
                    data.villages.size(),
                    data.organizations.size(),
                    0,
                    data.lairs.size(),
                    data.anomalies.size(),
                    0
            ));
        }
        points.sort(Comparator.comparingInt(BranchTimeSeriesPointDto::year));
        return points;
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

    public static boolean validateSaveDataIntegrity(WorldSaveData data) {
        if (data == null) return false;
        if (data.schemaVersion < 1 || data.schemaVersion > 3) return false;
        if (data.year < 0 || data.month < 0 || data.month > 12) return false;
        if (data.villages == null || data.villages.isEmpty()) return false;
        if (data.people == null || data.people.isEmpty()) return false;

        // Check for positive population and valid coordinates
        for (WorldSaveData.VillageSaveData v : data.villages) {
            if (v.name == null || v.name.isBlank()) return false;
            if (v.food < 0 || v.wood < 0 || v.ore < 0 || v.tools < 0) return false;
        }

        // Verify living people exist
        long livingCount = data.people.stream().filter(p -> p.alive).count();
        if (livingCount == 0 && !"HUMAN_EXTINCT".equals(data.lifecycleState)) {
            return false;
        }

        return true;
    }

    public static String computeSha256(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "CHECKSUM_UNAVAILABLE";
        }
    }

    public static String computeSha256Checksum(Path path) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = Files.readAllBytes(path);
            byte[] digest = md.digest(fileBytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "CHECKSUM_UNAVAILABLE";
        }
    }

    public static String serializeRandomToBase64(Random rng) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(rng);
            }
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    public static Random deserializeRandomFromBase64(String base64, long fallbackSeed) {
        if (base64 != null && !base64.isBlank()) {
            try {
                byte[] bytes = Base64.getDecoder().decode(base64);
                try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
                    Object obj = ois.readObject();
                    if (obj instanceof Random r) {
                        return r;
                    }
                }
            } catch (Exception ignored) {}
        }
        return (fallbackSeed != 0) ? new Random(fallbackSeed) : new Random();
    }

    private WorldSaveData serializeEngine(
            SimulationEngine engine,
            String worldId,
            String worldName,
            String parentWorldId,
            String parentCheckpointId,
            String branchRngPolicy
    ) {
        WorldSaveData data = new WorldSaveData();
        data.worldId = (worldId != null && !worldId.isBlank()) ? worldId : "world-001";
        data.worldName = (worldName != null && !worldName.isBlank()) ? worldName : "CivSim World";
        data.parentWorldId = parentWorldId;
        data.parentCheckpointId = parentCheckpointId;
        data.branchRngPolicy = (branchRngPolicy != null) ? branchRngPolicy : "CONTINUE_CHECKPOINT_RNG";
        data.branchForkYear = engine.getTimeline().getYear();
        data.branchForkMonth = engine.getTimeline().getMonth();
        data.createdAt = System.currentTimeMillis();
        data.year = engine.getTimeline().getYear();
        data.month = engine.getTimeline().getMonth();
        data.totalMonths = engine.getTimeline().getTotalElapsedMonths();
        data.randomSeed = engine.getConfig().getRandomSeed();
        data.lifecycleState = engine.getLifecycleState().name();
        data.rngStateBase64 = serializeRandomToBase64(engine.getRandom());

        data.nextPersonId = engine.getNextPersonId();
        data.nextFamilyId = engine.getNextFamilyId();
        data.nextCoupleId = engine.getNextCoupleId();
        data.nextVillageId = engine.getNextVillageId();
        data.totalRecordedPeople = engine.getTotalRecordedPeopleCount();
        data.crossVillageMarriagesThisYear = engine.getCrossVillageMarriagesThisYear();
        data.settlementUpgradesThisYear = engine.getSettlementUpgradesThisYear();

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
            vsd.housingCount = v.getHousingCount();
            vsd.leaderId = v.getLeader() != null ? v.getLeader().getId() : null;

            // Diplomatic relations
            for (Map.Entry<Integer, DiplomaticStatus> rel : v.getAllRelations().entrySet()) {
                vsd.diplomaticRelations.put(String.valueOf(rel.getKey()), rel.getValue().name());
            }

            // Crafting facilities
            for (CraftingFacility cf : v.getCraftingFacilities().values()) {
                WorldSaveData.CraftingFacilitySaveData cfsd = new WorldSaveData.CraftingFacilitySaveData();
                cfsd.facilityId = cf.getFacilityId();
                cfsd.settlementId = cf.getSettlementId();
                cfsd.ownerOrganizationId = cf.getOwnerOrganizationId();
                cfsd.grade = cf.getGrade().name();
                cfsd.concurrentCapacity = cf.getConcurrentCapacity();
                cfsd.activeJobsCount = cf.getActiveJobsCount();
                vsd.craftingFacilities.add(cfsd);
            }

            // Supernatural and monster materials
            if (v.getSupernaturalInventory() != null) {
                for (SupernaturalResourceType srt : SupernaturalResourceType.values()) {
                    double amt = v.getSupernaturalInventory().getAmount(srt);
                    if (amt > 0) vsd.supernaturalInventory.put(srt.name(), amt);
                }
            }
            if (v.getMonsterMaterials() != null) {
                for (Map.Entry<MonsterMaterialType, Double> me : v.getMonsterMaterials().entrySet()) {
                    if (me.getValue() > 0) vsd.monsterMaterials.put(me.getKey().name(), me.getValue());
                }
            }
            vsd.monsterCores = v.getMonsterCoreCount();

            // Resident order preservation
            for (Person p : v.getResidents()) {
                vsd.residentIds.add(p.getId());
            }
            vsd.bankTotalDeposits = v.getBank().getTotalDeposits();
            vsd.bankTotalLoans = v.getBank().getTotalLoansOutstanding();
            vsd.birthsThisYear = v.getBirthsThisYear();
            vsd.deathsThisYear = v.getDeathsThisYear();
            vsd.marriagesThisYear = v.getMarriagesThisYear();
            vsd.inMigrationsThisYear = v.getInMigrationsThisYear();
            vsd.outMigrationsThisYear = v.getOutMigrationsThisYear();
            vsd.consecutiveFamineMonths = v.getConsecutiveFamineMonths();
            vsd.famineDeathsThisYear = v.getFamineDeathsThisYear();
            vsd.freezingDeathsThisYear = v.getFreezingDeathsThisYear();
            vsd.lastDisasterThisYear = v.getLastDisasterThisYear() != null ? v.getLastDisasterThisYear().name() : null;
            vsd.lastRaidEventSummary = v.getLastRaidEventSummary();
            vsd.foodPrice = v.getMarket().getFoodPrice();
            vsd.annualTradeVolume = v.getMarket().getAnnualTradeVolume();
            vsd.totalTradeVolume = v.getMarket().getTotalTradeVolume();
            vsd.lastMonsterTideMonth = v.getLastMonsterTideMonth();
            vsd.peakPopulation = v.getPeakPopulation();

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

            if (p.getCrafterProfile() != null) {
                CrafterProfile cp = p.getCrafterProfile();
                WorldSaveData.CrafterProfileSaveData cpsd = new WorldSaveData.CrafterProfileSaveData();
                for (CrafterSpecialization cs : CrafterSpecialization.values()) {
                    cpsd.proficiencies.put(cs.name(), cp.getProficiency(cs));
                }
                cpsd.totalOrdersCompleted = cp.getTotalOrdersCompleted();
                cpsd.totalCrafts = cp.getTotalCrafts();
                cpsd.totalEnchants = cp.getTotalEnchants();
                cpsd.totalRepairs = cp.getTotalRepairs();
                cpsd.totalUpgrades = cp.getTotalUpgrades();
                psd.crafterProfile = cpsd;
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
            csd.lastBirthTotalMonths = c.getLastBirthTotalMonths();
            csd.nextScheduledBirthMonth = c.getNextScheduledBirthMonth();
            csd.active = c.isActive();
            data.couples.add(csd);
        }

        // Organizations
        if (engine.getOrganizationSystem() != null) {
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
                for (String mId : o.getAllMembershipRecords().keySet()) {
                    osd.memberPersonIds.add(mId);
                }
                for (String eqId : o.getClanVault().keySet()) {
                    osd.vaultEquipmentIds.add(eqId);
                }
                data.organizations.add(osd);
            }
        }

        // Anomalies
        if (engine.getAnomalySystem() != null) {
            for (WorldAnomaly a : engine.getAnomalySystem().getAllAnomalies()) {
                WorldSaveData.AnomalySaveData asd = new WorldSaveData.AnomalySaveData();
                asd.id = a.getAnomalyId();
                asd.x = a.getPosX();
                asd.y = a.getPosY();
                asd.state = a.getState() != null ? a.getState().name() : "STABLE";
                asd.category = a.getResourceCategory();
                asd.grade = a.getResourceGrade() != null ? a.getResourceGrade().name() : "COMMON";
                asd.initialQty = a.getInitialQuantity();
                asd.remainingQty = a.getRemainingQuantity();
                asd.observable = a.isPubliclyObservable();
                asd.ownerId = a.getControllingOwnerId();
                asd.revealedYear = a.getPublicAppearanceMonth() / 12;
                asd.discoveredYear = a.getFirstDiscoveryMonth() != null ? a.getFirstDiscoveryMonth() / 12 : a.getPublicAppearanceMonth() / 12;
                data.anomalies.add(asd);
            }
        }

        // Monster Lairs
        if (engine.getMonsterEcologySystem() != null) {
            for (MonsterLair ml : engine.getMonsterEcologySystem().getAllLairs()) {
                WorldSaveData.LairSaveData lsd = new WorldSaveData.LairSaveData();
                lsd.id = ml.getLairId();
                lsd.sourceAnomalyId = ml.getSourceAnomalyId();
                lsd.x = ml.getPosX();
                lsd.y = ml.getPosY();
                lsd.biome = ml.getBiome() != null ? ml.getBiome().name() : "PLAINS";
                lsd.threatRank = ml.getDominantThreatRank() != null ? ml.getDominantThreatRank().name() : "STANDARD";
                lsd.state = ml.getState() != null ? ml.getState().name() : "ACTIVE";
                lsd.capacity = ml.getPopulationCapacity();
                lsd.territoryRadius = ml.getTerritoryRadius();
                lsd.primarySpecies = ml.getPrimarySpeciesId();
                lsd.secondarySpecies = ml.getSecondarySpeciesId();
                lsd.emergenceYear = ml.getLastSpawnMonth() / 12;
                data.lairs.add(lsd);
            }
        }

        // Annual Reports
        for (AnnualReport ar : engine.getAnnualReports()) {
            WorldSaveData.AnnualReportSaveData arsd = new WorldSaveData.AnnualReportSaveData();
            arsd.year = ar.getYear();
            arsd.totalLivingPopulation = ar.getTotalLivingPopulation();
            arsd.livingMalePopulation = ar.getLivingMalePopulation();
            arsd.livingFemalePopulation = ar.getLivingFemalePopulation();
            arsd.villageLivingPopulations = new HashMap<>(ar.getVillageLivingPopulations());
            arsd.birthsDuringYear = ar.getBirthsDuringYear();
            arsd.deathsDuringYear = ar.getDeathsDuringYear();
            arsd.marriagesDuringYear = ar.getMarriagesDuringYear();
            arsd.migrationsDuringYear = ar.getMigrationsDuringYear();
            arsd.settlementUpgradesDuringYear = ar.getSettlementUpgradesDuringYear();
            arsd.activeFamilies = ar.getActiveFamilies();
            arsd.combatSummary = ar.getCombatSummary();
            arsd.awakeningSummary = ar.getAwakeningSummary();
            arsd.organizationSummary = ar.getOrganizationSummary();
            arsd.monsterEcologySummary = ar.getMonsterEcologySummary();
            arsd.equipmentSummary = ar.getEquipmentSummary();
            arsd.bloodFeudSummary = ar.getBloodFeudSummary();
            data.annualReports.add(arsd);
        }

        // Crafting System and Equipment
        if (engine.getEquipmentCraftingSystem() != null) {
            EquipmentCraftingSystem ecs = engine.getEquipmentCraftingSystem();
            WorldSaveData.CraftingSystemSaveData cssd = new WorldSaveData.CraftingSystemSaveData();
            cssd.nextEquipmentSeq = ecs.getNextEquipmentSeq();
            cssd.nextWorkOrderSeq = ecs.getNextWorkOrderSeq();
            cssd.nextEnchantmentSeq = ecs.getNextEnchantmentSeq();
            cssd.ordersStarted = ecs.getOrdersStarted();
            cssd.ordersCompletedCount = ecs.getOrdersCompletedCount();
            cssd.ordersFailedCount = ecs.getOrdersFailedCount();
            cssd.ordersCancelledCount = ecs.getOrdersCancelledCount();
            cssd.enchantmentsAppliedCount = ecs.getEnchantmentsAppliedCount();
            cssd.upgradesCompletedCount = ecs.getUpgradesCompletedCount();
            cssd.repairsCompletedCount = ecs.getRepairsCompletedCount();
            cssd.totalCoresGenerated = ecs.getTotalCoresGenerated();
            cssd.totalCoresDelivered = ecs.getTotalCoresDelivered();
            cssd.totalCoresReserved = ecs.getTotalCoresReserved();
            cssd.totalCoresConsumed = ecs.getTotalCoresConsumed();
            cssd.totalCoresLost = ecs.getTotalCoresLost();
            cssd.totalAnomalyGenerated = ecs.getTotalAnomalyGenerated();
            cssd.totalAnomalyDelivered = ecs.getTotalAnomalyDelivered();
            cssd.totalAnomalyReserved = ecs.getTotalAnomalyReserved();
            cssd.totalAnomalyConsumed = ecs.getTotalAnomalyConsumed();
            cssd.totalAnomalyLost = ecs.getTotalAnomalyLost();

            for (EquipmentItem item : ecs.getAllEquipment().values()) {
                WorldSaveData.EquipmentItemSaveData eisd = new WorldSaveData.EquipmentItemSaveData();
                eisd.equipmentId = item.getEquipmentId();
                eisd.displayName = item.getDisplayName();
                eisd.artifactTitle = item.getArtifactTitle();
                eisd.category = item.getCategory().name();
                eisd.slot = item.getSlot().name();
                eisd.type = item.getType().name();
                eisd.twoHanded = item.isTwoHanded();
                eisd.rarity = item.getEquipmentRarity().name();
                eisd.tier = item.getEquipmentTier();
                eisd.craftQuality = item.getCraftQuality();
                eisd.durability = item.getDurability();
                eisd.maximumDurability = item.getMaximumDurability();
                eisd.condition = item.getCondition().name();
                eisd.lifecycleStatus = item.getLifecycleStatus().name();
                eisd.creationTotalMonths = item.getCreationTotalMonths();
                eisd.creatorPersonId = item.getCreatorPersonId();
                eisd.creationFacilityId = item.getCreationFacilityId();
                eisd.currentOwnerId = item.getCurrentOwnerId();
                eisd.currentCustodianId = item.getCurrentCustodianId();
                eisd.posX = item.getPosX();
                eisd.posY = item.getPosY();
                eisd.registeredHeirloom = item.isRegisteredHeirloom();
                eisd.heirloomId = item.getHeirloomId();

                if (item.getBaseStatBonuses() != null) {
                    WorldSaveData.EquipmentStatProfileSaveData bsd = new WorldSaveData.EquipmentStatProfileSaveData();
                    bsd.attackBonus = item.getBaseStatBonuses().getAttackBonus();
                    bsd.defenseBonus = item.getBaseStatBonuses().getDefenseBonus();
                    bsd.accuracyBonus = item.getBaseStatBonuses().getAccuracyBonus();
                    bsd.evasionBonus = item.getBaseStatBonuses().getEvasionBonus();
                    bsd.aetherPotencyBonus = item.getBaseStatBonuses().getAetherPotencyBonus();
                    bsd.aetherCostEfficiency = item.getBaseStatBonuses().getAetherCostEfficiency();
                    bsd.barrierOrResistanceBonus = item.getBaseStatBonuses().getBarrierOrResistanceBonus();
                    eisd.baseStats = bsd;
                }

                for (EnchantmentRecord enc : item.getEnchantments()) {
                    WorldSaveData.EnchantmentRecordSaveData ersd = new WorldSaveData.EnchantmentRecordSaveData();
                    ersd.enchantmentId = enc.getEnchantmentId();
                    ersd.displayName = enc.getDisplayName();
                    ersd.family = enc.getFamily().name();
                    ersd.primaryBonus = enc.getPrimaryBonus();
                    ersd.secondaryBonus = enc.getSecondaryBonus();
                    ersd.potencyTier = enc.getPotencyTier();
                    ersd.catalystType = enc.getCatalystUsed();
                    eisd.enchantments.add(ersd);
                }

                eisd.materialComposition.putAll(item.getMaterialComposition());
                cssd.allEquipment.add(eisd);
            }

            for (CraftingWorkOrder order : ecs.getActiveOrders().values()) {
                cssd.activeOrders.add(serializeOrder(order));
            }

            for (CraftingWorkOrder order : ecs.getCompletedOrders()) {
                cssd.completedOrders.add(serializeOrder(order));
            }

            data.craftingSystem = cssd;
        }

        // Heirlooms
        if (engine.getClanHeirloomSystem() != null) {
            for (ClanHeirloom h : engine.getClanHeirloomSystem().getAllHeirlooms().values()) {
                WorldSaveData.HeirloomSaveData hsd = new WorldSaveData.HeirloomSaveData();
                hsd.heirloomId = h.getHeirloomId();
                hsd.equipmentId = h.getEquipmentId();
                hsd.owningClanId = h.getOwningClanId();
                hsd.artifactTitle = h.getArtifactTitle();
                hsd.registrationYear = h.getRegistrationYear();
                hsd.registrationMonth = h.getRegistrationMonth();
                hsd.successionPolicy = h.getSuccessionPolicy() != null ? h.getSuccessionPolicy().name() : "MERIT_FIRST";
                hsd.currentBearerPersonId = h.getCurrentBearerPersonId();
                hsd.custodyStatus = h.getCustodyStatus() != null ? h.getCustodyStatus().name() : "IN_CLAN_VAULT";
                data.heirlooms.add(hsd);
            }
        }

        // Blood Feuds
        if (engine.getBloodFeudSystem() != null) {
            for (civsim.model.FeudCase feud : engine.getBloodFeudSystem().getAllFeuds()) {
                WorldSaveData.BloodFeudSaveData bsd = new WorldSaveData.BloodFeudSaveData();
                bsd.feudId = feud.getFeudId();
                bsd.instigatorOrgId = feud.getInitiatingOrgId();
                bsd.targetOrgId = feud.getOpposingOrgId();
                bsd.hatredLevel = feud.getCurrentSeverity();
                bsd.state = feud.getCurrentFeudStatus().name();
                bsd.feudType = feud.getFeudType();
                bsd.activeEscalationTier = feud.getCurrentEscalationLevel().ordinal();
                data.bloodFeuds.add(bsd);
            }
        }

        return data;
    }

    private WorldSaveData.CraftingWorkOrderSaveData serializeOrder(CraftingWorkOrder order) {
        WorldSaveData.CraftingWorkOrderSaveData osd = new WorldSaveData.CraftingWorkOrderSaveData();
        osd.workOrderId = order.getWorkOrderId();
        osd.orderType = order.getOrderType().name();
        osd.facilityId = order.getFacilityId();
        osd.settlementId = order.getSettlementId();
        osd.organizationId = order.getOrganizationId();
        osd.crafterPersonId = order.getCrafterPersonId();
        osd.recipeId = order.getRecipeId();
        osd.targetEquipmentId = order.getTargetEquipmentId();
        osd.targetUpgradeTier = order.getTargetUpgradeTier();
        osd.status = order.getStatus() != null ? order.getStatus().name() : "MATERIALS_RESERVED";
        osd.startYear = order.getStartYear();
        osd.startMonth = order.getStartMonth();
        osd.totalMonthsRequired = order.getTotalMonthsRequired();
        osd.monthsProgressed = order.getMonthsProgressed();
        osd.reservedIron = order.getReservedIron();
        osd.reservedOre = order.getReservedOre();
        osd.reservedWood = order.getReservedWood();
        osd.reservedTools = order.getReservedTools();
        for (Map.Entry<SupernaturalResourceType, Double> e : order.getReservedSupernatural().entrySet()) {
            osd.reservedSupernatural.put(e.getKey().name(), e.getValue());
        }
        for (Map.Entry<MonsterMaterialType, Double> e : order.getReservedMonster().entrySet()) {
            osd.reservedMonster.put(e.getKey().name(), e.getValue());
        }
        if (order.getReservedCoreGrade() != null) {
            osd.reservedCoreGrade = order.getReservedCoreGrade().name();
        }
        if (order.getPendingEnchantment() != null) {
            EnchantmentRecord enc = order.getPendingEnchantment();
            WorldSaveData.EnchantmentRecordSaveData ersd = new WorldSaveData.EnchantmentRecordSaveData();
            ersd.enchantmentId = enc.getEnchantmentId();
            ersd.displayName = enc.getDisplayName();
            ersd.family = enc.getFamily().name();
            ersd.primaryBonus = enc.getPrimaryBonus();
            ersd.secondaryBonus = enc.getSecondaryBonus();
            ersd.potencyTier = enc.getPotencyTier();
            ersd.catalystType = enc.getCatalystUsed();
            osd.pendingEnchantment = ersd;
        }
        return osd;
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
        engine.setCrossVillageMarriagesThisYear(data.crossVillageMarriagesThisYear);
        engine.setSettlementUpgradesThisYear(data.settlementUpgradesThisYear);
        engine.setLifecycleState(SimulationLifecycleState.valueOf(data.lifecycleState != null ? data.lifecycleState : "PAUSED"));
        engine.clearEntitiesForLoad();

        // Restore deterministic Random state
        Random restoredRng = deserializeRandomFromBase64(data.rngStateBase64, data.randomSeed);
        engine.setRandom(restoredRng);

        // Maps for linkage
        Map<Integer, Village> villageMap = new HashMap<>();
        Map<Integer, Family> familyMap = new HashMap<>();
        Map<Integer, Person> personMap = new HashMap<>();

        // 1. Rebuild Villages
        for (WorldSaveData.VillageSaveData vsd : data.villages) {
            Biome b = Biome.valueOf(vsd.biome);
            Village v = new Village(vsd.id, vsd.name, 2.0, 0.04, vsd.x, vsd.y, b);
            v.setTier(SettlementTier.valueOf(vsd.tier));
            v.getStockpile().setStockpiles(vsd.food, vsd.wood, vsd.ore, vsd.tools);
            v.getHousing().setHousingCount(vsd.housingCount > 0 ? vsd.housingCount : 3);
            v.setTreasury(vsd.treasury);
            v.setTechLevel(vsd.techLevel);
            v.setFortificationLevel(vsd.fortification);

            // Restore Crafting Facilities
            if (vsd.craftingFacilities != null && !vsd.craftingFacilities.isEmpty()) {
                for (WorldSaveData.CraftingFacilitySaveData cfsd : vsd.craftingFacilities) {
                    CraftingFacility cf = new CraftingFacility(
                            cfsd.facilityId, cfsd.settlementId, cfsd.ownerOrganizationId,
                            FacilityGrade.valueOf(cfsd.grade), cfsd.concurrentCapacity);
                    cf.setActiveJobsCount(cfsd.activeJobsCount);
                    v.addCraftingFacility(cf);
                }
            } else if (v.getCraftingFacilities().isEmpty()) {
                v.addCraftingFacility(new CraftingFacility("FORGE_" + v.getId() + "_1", "VILLAGE_" + v.getId(), null, FacilityGrade.BASIC_FORGE, 3));
            }

            // Restore Supernatural Inventory
            if (vsd.supernaturalInventory != null) {
                for (Map.Entry<String, Double> entry : vsd.supernaturalInventory.entrySet()) {
                    try {
                        v.getSupernaturalInventory().addResource(SupernaturalResourceType.valueOf(entry.getKey()), entry.getValue());
                    } catch (Exception ignored) {}
                }
            }

            // Restore Monster Materials
            if (vsd.monsterMaterials != null) {
                for (Map.Entry<String, Double> entry : vsd.monsterMaterials.entrySet()) {
                    try {
                        v.addMonsterMaterial(MonsterMaterialType.valueOf(entry.getKey()), entry.getValue());
                    } catch (Exception ignored) {}
                }
            }
            if (vsd.monsterCores > 0) {
                v.addMonsterCore(vsd.monsterCores);
            }

            // Restore Diplomatic Relations
            if (vsd.diplomaticRelations != null) {
                for (Map.Entry<String, String> entry : vsd.diplomaticRelations.entrySet()) {
                    try {
                        int otherId = Integer.parseInt(entry.getKey());
                        DiplomaticStatus st = DiplomaticStatus.valueOf(entry.getValue());
                        v.setRelationWith(otherId, st);
                    } catch (Exception ignored) {}
                }
            }

            // Restore Banking and Yearly Accumulators
            v.getBank().setTotalDeposits(vsd.bankTotalDeposits);
            v.getBank().setTotalLoansOutstanding(vsd.bankTotalLoans);
            v.setBirthsThisYear(vsd.birthsThisYear);
            v.setDeathsThisYear(vsd.deathsThisYear);
            v.setMarriagesThisYear(vsd.marriagesThisYear);
            v.setInMigrationsThisYear(vsd.inMigrationsThisYear);
            v.setOutMigrationsThisYear(vsd.outMigrationsThisYear);
            v.setConsecutiveFamineMonths(vsd.consecutiveFamineMonths);
            v.setFamineDeathsThisYear(vsd.famineDeathsThisYear);
            v.setFreezingDeathsThisYear(vsd.freezingDeathsThisYear);
            if (vsd.lastDisasterThisYear != null) {
                try {
                    v.setLastDisasterThisYear(DisasterType.valueOf(vsd.lastDisasterThisYear));
                } catch (Exception ignored) {}
            }
            v.setLastRaidEventSummary(vsd.lastRaidEventSummary);
            if (vsd.foodPrice > 0) v.getMarket().setFoodPrice(vsd.foodPrice);
            v.getMarket().setAnnualTradeVolume(vsd.annualTradeVolume);
            v.getMarket().setTotalTradeVolume(vsd.totalTradeVolume);
            v.setLastMonsterTideMonth(vsd.lastMonsterTideMonth);
            v.setPeakPopulation(vsd.peakPopulation);

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
        Map<Integer, WorldSaveData.VillageSaveData> vsdMap = new HashMap<>();
        for (WorldSaveData.VillageSaveData vsd : data.villages) {
            vsdMap.put(vsd.id, vsd);
        }

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

            if (psd.crafterProfile != null) {
                CrafterProfile cp = p.getOrCreateCrafterProfile();
                if (psd.crafterProfile.proficiencies != null) {
                    for (Map.Entry<String, Double> pe : psd.crafterProfile.proficiencies.entrySet()) {
                        try {
                            cp.setProficiency(CrafterSpecialization.valueOf(pe.getKey()), pe.getValue());
                        } catch (Exception ignored) {}
                    }
                }
                cp.setTotalOrdersCompleted(psd.crafterProfile.totalOrdersCompleted);
                cp.setTotalCrafts(psd.crafterProfile.totalCrafts);
                cp.setTotalEnchants(psd.crafterProfile.totalEnchants);
                cp.setTotalRepairs(psd.crafterProfile.totalRepairs);
                cp.setTotalUpgrades(psd.crafterProfile.totalUpgrades);
            }

            if (psd.equippedItems != null) {
                for (Map.Entry<String, String> eqEntry : psd.equippedItems.entrySet()) {
                    try {
                        p.setEquippedItemDirect(EquipmentSlot.valueOf(eqEntry.getKey()), eqEntry.getValue());
                    } catch (Exception ignored) {}
                }
            }

            WorldSaveData.VillageSaveData vsdForP = (psd.villageId != null) ? vsdMap.get(psd.villageId) : null;
            if (v != null && p.isAlive() && (vsdForP == null || vsdForP.residentIds == null || vsdForP.residentIds.isEmpty())) {
                v.addResident(p);
            }

            if (f != null) {
                f.addMember(p);
            }

            engine.addPerson(p);
            personMap.put(p.getId(), p);
        }

        // 3a. Rebuild Village Residents in exact preserved order
        for (WorldSaveData.VillageSaveData vsd : data.villages) {
            Village v = villageMap.get(vsd.id);
            if (v != null && vsd.residentIds != null && !vsd.residentIds.isEmpty()) {
                for (Integer resId : vsd.residentIds) {
                    Person p = personMap.get(resId);
                    if (p != null && p.isAlive()) {
                        v.addResident(p);
                    }
                }
            }
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
                c.setActive(csd.active);
                if (csd.lastBirthTotalMonths != null) {
                    c.setLastBirthTotalMonths(csd.lastBirthTotalMonths);
                }
                c.setNextScheduledBirthMonth(csd.nextScheduledBirthMonth);
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

        // 7. Rebuild Anomalies
        if (data.anomalies != null && !data.anomalies.isEmpty() && engine.getAnomalySystem() != null) {
            for (WorldSaveData.AnomalySaveData asd : data.anomalies) {
                ResourceGrade grade;
                try {
                    grade = ResourceGrade.valueOf(asd.grade != null ? asd.grade : "COMMON");
                } catch (Exception e) {
                    grade = ResourceGrade.COMMON;
                }
                WorldAnomaly anom = new WorldAnomaly(
                        asd.id,
                        asd.id,
                        asd.x,
                        asd.y,
                        Biome.RIVER_VALLEY,
                        SupernaturalResourceType.RAW_AETHER_CRYSTAL,
                        grade,
                        (int) Math.max(1, asd.initialQty),
                        asd.revealedYear * 12
                );
                anom.extractResources(Math.max(0.0, asd.initialQty - asd.remainingQty));
                if (asd.ownerId != null) anom.setControllingOwnerId(asd.ownerId);
                if (asd.observable) anom.transitionToRevealed();
                engine.getAnomalySystem().addAnomaly(anom);
            }
        }

        // 8. Rebuild Monster Lairs
        if (data.lairs != null && !data.lairs.isEmpty() && engine.getMonsterEcologySystem() != null) {
            for (WorldSaveData.LairSaveData lsd : data.lairs) {
                MonsterThreatRank rank;
                try {
                    rank = MonsterThreatRank.valueOf(lsd.threatRank != null ? lsd.threatRank : "STANDARD");
                } catch (Exception e) {
                    rank = MonsterThreatRank.STANDARD;
                }
                Biome b;
                try {
                    b = Biome.valueOf(lsd.biome != null ? lsd.biome : "FOREST");
                } catch (Exception e) {
                    b = Biome.FOREST;
                }
                MonsterLair ml = new MonsterLair(
                        lsd.id,
                        lsd.sourceAnomalyId != null ? lsd.sourceAnomalyId : "anom-0",
                        lsd.x,
                        lsd.y,
                        b,
                        SupernaturalResourceType.RAW_AETHER_CRYSTAL,
                        ResourceGrade.COMMON,
                        lsd.primarySpecies != null ? lsd.primarySpecies : "ShadowWolf",
                        lsd.secondarySpecies != null ? lsd.secondarySpecies : "CorruptedBear",
                        lsd.capacity > 0 ? lsd.capacity : 25,
                        lsd.territoryRadius > 0 ? lsd.territoryRadius : 36.0,
                        rank,
                        lsd.emergenceYear * 12
                );
                engine.getMonsterEcologySystem().addLair(ml);
            }
        }

        // 9. Rebuild Annual Reports
        if (data.annualReports != null && !data.annualReports.isEmpty()) {
            for (WorldSaveData.AnnualReportSaveData arsd : data.annualReports) {
                AnnualReport ar = new AnnualReport(
                        arsd.year,
                        arsd.totalLivingPopulation,
                        arsd.livingMalePopulation,
                        arsd.livingFemalePopulation,
                        arsd.villageLivingPopulations != null ? arsd.villageLivingPopulations : Map.of(),
                        arsd.birthsDuringYear,
                        arsd.deathsDuringYear,
                        arsd.marriagesDuringYear,
                        0,
                        arsd.migrationsDuringYear,
                        arsd.settlementUpgradesDuringYear,
                        75,
                        0,
                        0,
                        0,
                        0,
                        arsd.activeFamilies,
                        0,
                        "",
                        List.of(),
                        arsd.awakeningSummary != null ? arsd.awakeningSummary : "",
                        "",
                        "",
                        "",
                        arsd.combatSummary != null ? arsd.combatSummary : "",
                        arsd.organizationSummary != null ? arsd.organizationSummary : "",
                        "",
                        "",
                        "",
                        "",
                        ""
                );
                engine.addAnnualReport(ar);
            }
        }

        // 10. Rebuild Equipment and Crafting System
        if (data.craftingSystem != null && engine.getEquipmentCraftingSystem() != null) {
            EquipmentCraftingSystem ecs = engine.getEquipmentCraftingSystem();
            ecs.setNextEquipmentSeq(data.craftingSystem.nextEquipmentSeq);
            ecs.setNextWorkOrderSeq(data.craftingSystem.nextWorkOrderSeq);
            ecs.setNextEnchantmentSeq(data.craftingSystem.nextEnchantmentSeq);
            ecs.setOrdersStarted(data.craftingSystem.ordersStarted);
            ecs.setOrdersCompletedCount(data.craftingSystem.ordersCompletedCount);
            ecs.setOrdersFailedCount(data.craftingSystem.ordersFailedCount);
            ecs.setOrdersCancelledCount(data.craftingSystem.ordersCancelledCount);
            ecs.setEnchantmentsAppliedCount(data.craftingSystem.enchantmentsAppliedCount);
            ecs.setUpgradesCompletedCount(data.craftingSystem.upgradesCompletedCount);
            ecs.setRepairsCompletedCount(data.craftingSystem.repairsCompletedCount);
            ecs.setTotalCoresGenerated(data.craftingSystem.totalCoresGenerated);
            ecs.setTotalCoresDelivered(data.craftingSystem.totalCoresDelivered);
            ecs.setTotalCoresReserved(data.craftingSystem.totalCoresReserved);
            ecs.setTotalCoresConsumed(data.craftingSystem.totalCoresConsumed);
            ecs.setTotalCoresLost(data.craftingSystem.totalCoresLost);
            ecs.setTotalAnomalyGenerated(data.craftingSystem.totalAnomalyGenerated);
            ecs.setTotalAnomalyDelivered(data.craftingSystem.totalAnomalyDelivered);
            ecs.setTotalAnomalyReserved(data.craftingSystem.totalAnomalyReserved);
            ecs.setTotalAnomalyConsumed(data.craftingSystem.totalAnomalyConsumed);
            ecs.setTotalAnomalyLost(data.craftingSystem.totalAnomalyLost);

            if (data.craftingSystem.allEquipment != null) {
                for (WorldSaveData.EquipmentItemSaveData eisd : data.craftingSystem.allEquipment) {
                    EquipmentStatProfile baseStats = (eisd.baseStats != null)
                            ? new EquipmentStatProfile(eisd.baseStats.attackBonus, eisd.baseStats.defenseBonus, eisd.baseStats.accuracyBonus, eisd.baseStats.evasionBonus, eisd.baseStats.aetherPotencyBonus, eisd.baseStats.aetherCostEfficiency, eisd.baseStats.barrierOrResistanceBonus)
                            : EquipmentStatProfile.zero();
                    EquipmentItem item = new EquipmentItem(
                            eisd.equipmentId,
                            eisd.displayName,
                            EquipmentCategory.valueOf(eisd.category),
                            EquipmentSlot.valueOf(eisd.slot),
                            EquipmentType.valueOf(eisd.type),
                            eisd.twoHanded,
                            ClassRarity.valueOf(eisd.rarity),
                            eisd.tier,
                            eisd.craftQuality,
                            eisd.maximumDurability,
                            baseStats,
                            eisd.creationTotalMonths,
                            eisd.creatorPersonId,
                            eisd.creationFacilityId,
                            eisd.currentOwnerId,
                            eisd.currentCustodianId,
                            eisd.posX,
                            eisd.posY
                    );
                    item.setDurability(eisd.durability);
                    if (eisd.condition != null) item.setCondition(EquipmentCondition.valueOf(eisd.condition));
                    if (eisd.lifecycleStatus != null) item.setLifecycleStatus(EquipmentLifecycleStatus.valueOf(eisd.lifecycleStatus));
                    item.setRegisteredHeirloom(eisd.registeredHeirloom);
                    item.setHeirloomId(eisd.heirloomId);
                    item.setArtifactTitle(eisd.artifactTitle);
                    if (eisd.enchantments != null) {
                        for (WorldSaveData.EnchantmentRecordSaveData ersd : eisd.enchantments) {
                            item.addEnchantmentDirect(new EnchantmentRecord(ersd.enchantmentId, ersd.displayName, EnchantmentFamily.valueOf(ersd.family), ersd.primaryBonus, ersd.secondaryBonus, ersd.potencyTier, ersd.catalystType));
                        }
                    }
                    if (eisd.materialComposition != null) {
                        for (Map.Entry<String, Double> me : eisd.materialComposition.entrySet()) {
                            item.addMaterialComposition(me.getKey(), me.getValue());
                        }
                    }
                    ecs.restoreEquipment(item);
                }
            }

            if (data.craftingSystem.activeOrders != null) {
                for (WorldSaveData.CraftingWorkOrderSaveData osd : data.craftingSystem.activeOrders) {
                    ecs.restoreActiveOrder(deserializeOrder(osd, ecs));
                }
            }

            if (data.craftingSystem.completedOrders != null) {
                for (WorldSaveData.CraftingWorkOrderSaveData osd : data.craftingSystem.completedOrders) {
                    ecs.restoreCompletedOrder(deserializeOrder(osd, ecs));
                }
            }
        }

        // 11. Rebuild Organizations
        if (data.organizations != null && engine.getOrganizationSystem() != null) {
            for (WorldSaveData.OrgSaveData osd : data.organizations) {
                Organization org = new Organization(
                        osd.id,
                        osd.name,
                        OrganizationType.valueOf(osd.type),
                        osd.foundingYear,
                        osd.foundingMonth,
                        osd.founderId,
                        OrganizationStatus.valueOf(osd.status),
                        OrganizationAlignment.valueOf(osd.alignment),
                        OrganizationPolicyProfile.createDefault(OrganizationType.valueOf(osd.type))
                );
                if (osd.leaderId != null) org.setCurrentLeaderPersonId(osd.leaderId);
                org.setReputation(osd.reputation);
                org.setBattlesWon(osd.battlesWon);
                org.setBattlesParticipated(osd.battlesTotal);

                if (osd.memberPersonIds != null) {
                    for (String pId : osd.memberPersonIds) {
                        org.addMembershipRecord(new MembershipRecord(
                                pId, org.getOrganizationId(), osd.foundingYear, osd.foundingMonth,
                                OrganizationRole.MEMBER, MembershipStatus.ACTIVE, MembershipOrigin.OPEN_JOIN, 50.0));
                    }
                }

                if (osd.vaultEquipmentIds != null) {
                    for (String eqId : osd.vaultEquipmentIds) {
                        EquipmentItem it = engine.getEquipmentCraftingSystem().getEquipment(eqId);
                        if (it != null) org.addVaultEquipment(it);
                    }
                }

                engine.getOrganizationSystem().registerOrganization(org);
            }
        }

        // 12. Rebuild Clan Heirlooms
        if (data.heirlooms != null && engine.getClanHeirloomSystem() != null) {
            for (WorldSaveData.HeirloomSaveData hsd : data.heirlooms) {
                ClanHeirloom h = new ClanHeirloom(
                        hsd.heirloomId,
                        hsd.equipmentId,
                        hsd.owningClanId,
                        hsd.artifactTitle,
                        hsd.registrationYear,
                        hsd.registrationMonth,
                        HeirloomSuccessionPolicy.valueOf(hsd.successionPolicy)
                );
                if (hsd.currentBearerPersonId != null) h.setCurrentBearerPersonId(hsd.currentBearerPersonId);
                if (hsd.custodyStatus != null) h.setCustodyStatus(HeirloomCustodyStatus.valueOf(hsd.custodyStatus));
                engine.getClanHeirloomSystem().restoreHeirloom(h);

                Organization clan = engine.getOrganizationSystem().getOrganization(hsd.owningClanId);
                if (clan != null) clan.registerHeirloom(h);
            }
        }

        // 13. Rebuild Blood Feuds
        if (data.bloodFeuds != null && engine.getBloodFeudSystem() != null) {
            for (WorldSaveData.BloodFeudSaveData bsd : data.bloodFeuds) {
                FeudEscalationLevel lvl = FeudEscalationLevel.values()[Math.min(FeudEscalationLevel.values().length - 1, Math.max(0, bsd.activeEscalationTier))];
                civsim.model.FeudCase feud = new civsim.model.FeudCase(
                        bsd.feudId,
                        bsd.feudId,
                        null,
                        0,
                        bsd.hatredLevel,
                        FeudStatus.valueOf(bsd.state),
                        bsd.instigatorOrgId,
                        List.of(),
                        bsd.instigatorOrgId,
                        bsd.targetOrgId,
                        null,
                        null,
                        bsd.feudType
                );
                feud.setCurrentEscalationLevel(lvl);
                feud.setCurrentSeverity(bsd.hatredLevel);
                feud.setCurrentFeudStatus(FeudStatus.valueOf(bsd.state));
                engine.getBloodFeudSystem().registerFeud(feud);
            }
        }

        engine.setInitialized(true);
        return engine;
    }

    private CraftingWorkOrder deserializeOrder(WorldSaveData.CraftingWorkOrderSaveData osd, EquipmentCraftingSystem ecs) {
        EquipmentRecipe recipe = (osd.recipeId != null) ? ecs.getRecipe(osd.recipeId) : null;
        EnchantmentRecord enc = null;
        if (osd.pendingEnchantment != null) {
            enc = new EnchantmentRecord(
                    osd.pendingEnchantment.enchantmentId,
                    osd.pendingEnchantment.displayName,
                    EnchantmentFamily.valueOf(osd.pendingEnchantment.family),
                    osd.pendingEnchantment.primaryBonus,
                    osd.pendingEnchantment.secondaryBonus,
                    osd.pendingEnchantment.potencyTier,
                    osd.pendingEnchantment.catalystType
            );
        }
        Map<SupernaturalResourceType, Double> resAnom = new EnumMap<>(SupernaturalResourceType.class);
        if (osd.reservedSupernatural != null) {
            for (Map.Entry<String, Double> e : osd.reservedSupernatural.entrySet()) {
                try { resAnom.put(SupernaturalResourceType.valueOf(e.getKey()), e.getValue()); } catch (Exception ignored) {}
            }
        }
        Map<MonsterMaterialType, Double> resMon = new EnumMap<>(MonsterMaterialType.class);
        if (osd.reservedMonster != null) {
            for (Map.Entry<String, Double> e : osd.reservedMonster.entrySet()) {
                try { resMon.put(MonsterMaterialType.valueOf(e.getKey()), e.getValue()); } catch (Exception ignored) {}
            }
        }
        ResourceGrade coreGrade = null;
        if (osd.reservedCoreGrade != null) {
            try { coreGrade = ResourceGrade.valueOf(osd.reservedCoreGrade); } catch (Exception ignored) {}
        }
        CraftingWorkOrder order = new CraftingWorkOrder(
                osd.workOrderId,
                CraftingWorkOrderType.valueOf(osd.orderType),
                osd.facilityId,
                osd.settlementId,
                osd.organizationId,
                osd.crafterPersonId,
                osd.recipeId,
                recipe,
                osd.targetEquipmentId,
                enc,
                osd.targetUpgradeTier,
                osd.startYear,
                osd.startMonth,
                osd.totalMonthsRequired,
                osd.reservedIron,
                osd.reservedOre,
                osd.reservedWood,
                osd.reservedTools,
                resAnom,
                resMon,
                coreGrade
        );
        if (osd.status != null) {
            try { order.setStatus(CraftingWorkOrderStatus.valueOf(osd.status)); } catch (Exception ignored) {}
        }
        order.setMonthsProgressed(osd.monthsProgressed);
        return order;
    }
}
