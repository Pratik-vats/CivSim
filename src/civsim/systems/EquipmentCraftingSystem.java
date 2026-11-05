package civsim.systems;

import civsim.ProgressionConfig;
import civsim.SimulationConfig;
import civsim.model.*;

import java.util.*;

/**
 * Phase 2C.1 -- Comprehensive Crafting, Enchanting, Upgrading, and Durability System.
 * Enforces strict material conservation, single physical identity, and rarity/tier ceilings.
 */
public class EquipmentCraftingSystem {

    // Master Equipment Registry
    private final Map<String, EquipmentItem> allEquipment = new LinkedHashMap<>();

    // Recipe Catalog
    private final Map<String, EquipmentRecipe> recipes = new LinkedHashMap<>();

    // Work Orders
    private final Map<String, CraftingWorkOrder> activeOrders = new LinkedHashMap<>();
    private final List<CraftingWorkOrder> completedOrders = new ArrayList<>();
    private final List<CraftingWorkOrder> cancelledOrders = new ArrayList<>();

    // Material Conservation Counters
    private double totalCoresGenerated = 0.0;
    private double totalCoresDelivered = 0.0;
    private double totalCoresReserved = 0.0;
    private double totalCoresConsumed = 0.0;
    private double totalCoresLost = 0.0;

    private double totalAnomalyGenerated = 0.0;
    private double totalAnomalyDelivered = 0.0;
    private double totalAnomalyReserved = 0.0;
    private double totalAnomalyConsumed = 0.0;
    private double totalAnomalyLost = 0.0;

    // Telemetry
    private int ordersStarted = 0;
    private int ordersCompletedCount = 0;
    private int ordersFailedCount = 0;
    private int ordersCancelledCount = 0;
    private int enchantmentsAppliedCount = 0;
    private int upgradesCompletedCount = 0;
    private int repairsCompletedCount = 0;

    // Sequence Counters
    private int nextEquipmentSeq = 1;
    private int nextWorkOrderSeq = 1;
    private int nextEnchantmentSeq = 1;

    private static volatile EquipmentCraftingSystem activeInstance = null;
    public static EquipmentCraftingSystem getActiveInstance() { return activeInstance; }
    public static void setActiveInstance(EquipmentCraftingSystem instance) { activeInstance = instance; }

    public EquipmentCraftingSystem() {
        initializeStandardRecipes();
    }

    private void initializeStandardRecipes() {
        // --- T1 Mundane Equipment (Year 0+, Basic Forge, Tech 0) ---
        registerRecipe(new EquipmentRecipe(
                "RECIPE_IRON_SWORD_T1", "Iron Shortsword",
                EquipmentCategory.WEAPON, EquipmentSlot.MAIN_HAND, EquipmentType.ONE_HANDED_SWORD, false,
                ClassRarity.COMMON, 1, FacilityGrade.BASIC_FORGE, 0, false,
                CrafterSpecialization.WEAPON_FORGING, 5.0, 1,
                15.0, 5.0, 1.0, 10.0,
                null, null, null,
                new EquipmentStatProfile(8.0, 0.0, 5.0, 0.0, 0.0, 0.0, 0.0)
        ));

        registerRecipe(new EquipmentRecipe(
                "RECIPE_IRON_GREATSWORD_T1", "Iron Greatsword",
                EquipmentCategory.WEAPON, EquipmentSlot.MAIN_HAND, EquipmentType.TWO_HANDED_SWORD, true,
                ClassRarity.COMMON, 1, FacilityGrade.BASIC_FORGE, 0, false,
                CrafterSpecialization.WEAPON_FORGING, 8.0, 1,
                25.0, 10.0, 2.0, 15.0,
                null, null, null,
                new EquipmentStatProfile(14.0, 0.0, 2.0, -2.0, 0.0, 0.0, 0.0)
        ));

        registerRecipe(new EquipmentRecipe(
                "RECIPE_IRON_SHIELD_T1", "Iron Round Shield",
                EquipmentCategory.DEFENSIVE_ARMOR, EquipmentSlot.OFF_HAND, EquipmentType.SHIELD, false,
                ClassRarity.COMMON, 1, FacilityGrade.BASIC_FORGE, 0, false,
                CrafterSpecialization.ARMOR_FORGING, 5.0, 1,
                12.0, 8.0, 1.0, 8.0,
                null, null, null,
                new EquipmentStatProfile(0.0, 8.0, 0.0, -2.0, 0.0, 0.0, 0.0)
        ));

        registerRecipe(new EquipmentRecipe(
                "RECIPE_IRON_CHESTPLATE_T1", "Iron Cuirass",
                EquipmentCategory.DEFENSIVE_ARMOR, EquipmentSlot.CHEST, EquipmentType.CHESTPLATE, false,
                ClassRarity.COMMON, 1, FacilityGrade.BASIC_FORGE, 0, false,
                CrafterSpecialization.ARMOR_FORGING, 8.0, 2,
                30.0, 5.0, 2.0, 20.0,
                null, null, null,
                new EquipmentStatProfile(0.0, 12.0, 0.0, -4.0, 0.0, 0.0, 0.0)
        ));

        // --- T2 Mundane Masterwork (Year 0+, Advanced Forge, Tech 2) ---
        registerRecipe(new EquipmentRecipe(
                "RECIPE_STEEL_BLADE_T2", "Refined Steel Longsword",
                EquipmentCategory.WEAPON, EquipmentSlot.MAIN_HAND, EquipmentType.ONE_HANDED_SWORD, false,
                ClassRarity.UNCOMMON, 2, FacilityGrade.ADVANCED_FORGE, 2, false,
                CrafterSpecialization.WEAPON_FORGING, 20.0, 2,
                35.0, 10.0, 3.0, 30.0,
                null, null, null,
                new EquipmentStatProfile(14.0, 2.0, 8.0, 0.0, 0.0, 0.0, 0.0)
        ));

        registerRecipe(new EquipmentRecipe(
                "RECIPE_STEEL_PLATE_T2", "Refined Steel Full Plate",
                EquipmentCategory.DEFENSIVE_ARMOR, EquipmentSlot.CHEST, EquipmentType.CHESTPLATE, false,
                ClassRarity.UNCOMMON, 2, FacilityGrade.ADVANCED_FORGE, 2, false,
                CrafterSpecialization.ARMOR_FORGING, 20.0, 3,
                50.0, 10.0, 4.0, 40.0,
                null, null, null,
                new EquipmentStatProfile(0.0, 18.0, 0.0, -2.0, 0.0, 0.0, 0.0)
        ));

        registerRecipe(new EquipmentRecipe(
                "RECIPE_HUNTER_BOW_T2", "Reinforced Composite Bow",
                EquipmentCategory.WEAPON, EquipmentSlot.MAIN_HAND, EquipmentType.BOW, true,
                ClassRarity.UNCOMMON, 2, FacilityGrade.ADVANCED_FORGE, 2, false,
                CrafterSpecialization.WEAPON_FORGING, 15.0, 2,
                10.0, 30.0, 2.0, 20.0,
                null, null, null,
                new EquipmentStatProfile(12.0, 0.0, 12.0, 4.0, 0.0, 0.0, 0.0)
        ));

        // --- T3 Supernatural (Year 50+, Supernatural Forge, Tech 3) ---
        Map<SupernaturalResourceType, Double> aetherBladeAnom = new EnumMap<>(SupernaturalResourceType.class);
        aetherBladeAnom.put(SupernaturalResourceType.RAW_AETHER_CRYSTAL, 10.0);
        Map<MonsterMaterialType, Double> aetherBladeMon = new EnumMap<>(MonsterMaterialType.class);
        aetherBladeMon.put(MonsterMaterialType.MONSTER_CORE, 1.0);
        aetherBladeMon.put(MonsterMaterialType.BEAST_HIDE, 5.0);

        registerRecipe(new EquipmentRecipe(
                "RECIPE_AETHER_BLADE_T3", "Aether-Infused Blade",
                EquipmentCategory.WEAPON, EquipmentSlot.MAIN_HAND, EquipmentType.ONE_HANDED_SWORD, false,
                ClassRarity.UNCOMMON, 3, FacilityGrade.SUPERNATURAL_FORGE, 3, true,
                CrafterSpecialization.AETHERIC_CRAFTING, 35.0, 3,
                30.0, 10.0, 3.0, 50.0,
                aetherBladeAnom, aetherBladeMon, ResourceGrade.COMMON,
                new EquipmentStatProfile(20.0, 4.0, 10.0, 2.0, 8.0, 4.0, 0.0)
        ));

        // --- T5 Rare Supernatural (Year 50+, Masterwork Forge, Tech 5) ---
        Map<SupernaturalResourceType, Double> aegisAnom = new EnumMap<>(SupernaturalResourceType.class);
        aegisAnom.put(SupernaturalResourceType.ANOMALOUS_ORE, 20.0);
        Map<MonsterMaterialType, Double> aegisMon = new EnumMap<>(MonsterMaterialType.class);
        aegisMon.put(MonsterMaterialType.MONSTER_CORE, 1.0);
        aegisMon.put(MonsterMaterialType.MONSTER_BONE, 10.0);

        registerRecipe(new EquipmentRecipe(
                "RECIPE_TITAN_AEGIS_T5", "Titan Aegis Shield",
                EquipmentCategory.DEFENSIVE_ARMOR, EquipmentSlot.OFF_HAND, EquipmentType.SHIELD, false,
                ClassRarity.RARE, 5, FacilityGrade.MASTERWORK_FORGE, 5, true,
                CrafterSpecialization.ARMOR_FORGING, 55.0, 5,
                60.0, 15.0, 5.0, 150.0,
                aegisAnom, aegisMon, ResourceGrade.RARE,
                new EquipmentStatProfile(4.0, 35.0, 0.0, 4.0, 0.0, 0.0, 15.0)
        ));

        // --- T7 Legendary Weapon (Year 50+, Masterwork Forge, Tech 6) ---
        Map<SupernaturalResourceType, Double> dragonbaneAnom = new EnumMap<>(SupernaturalResourceType.class);
        dragonbaneAnom.put(SupernaturalResourceType.ANOMALOUS_ORE, 35.0);
        dragonbaneAnom.put(SupernaturalResourceType.CONDENSED_AETHER_ESSENCE, 10.0);
        Map<MonsterMaterialType, Double> dragonbaneMon = new EnumMap<>(MonsterMaterialType.class);
        dragonbaneMon.put(MonsterMaterialType.MONSTER_CORE, 1.0);

        registerRecipe(new EquipmentRecipe(
                "RECIPE_DRAGONBANE_T7", "Dragonbane Greatsword",
                EquipmentCategory.WEAPON, EquipmentSlot.MAIN_HAND, EquipmentType.TWO_HANDED_SWORD, true,
                ClassRarity.LEGENDARY, 7, FacilityGrade.MASTERWORK_FORGE, 6, true,
                CrafterSpecialization.WEAPON_FORGING, 70.0, 7,
                80.0, 25.0, 8.0, 300.0,
                dragonbaneAnom, dragonbaneMon, ResourceGrade.LEGENDARY,
                new EquipmentStatProfile(48.0, 10.0, 18.0, 0.0, 20.0, 10.0, 0.0)
        ));

        // --- T10 Mythic Artifact (Year 50+, Legendary Forge, Tech 8) ---
        Map<SupernaturalResourceType, Double> mythicAnom = new EnumMap<>(SupernaturalResourceType.class);
        mythicAnom.put(SupernaturalResourceType.CONDENSED_AETHER_ESSENCE, 30.0);
        mythicAnom.put(SupernaturalResourceType.ANOMALOUS_ORE, 50.0);
        Map<MonsterMaterialType, Double> mythicMon = new EnumMap<>(MonsterMaterialType.class);
        mythicMon.put(MonsterMaterialType.MONSTER_CORE, 1.0);
        mythicMon.put(MonsterMaterialType.SPATIAL_SHARD, 10.0);

        registerRecipe(new EquipmentRecipe(
                "RECIPE_WORLD_SPLITTER_T10", "World-Splitter Blade",
                EquipmentCategory.WEAPON, EquipmentSlot.MAIN_HAND, EquipmentType.TWO_HANDED_SWORD, true,
                ClassRarity.MYTHIC, 10, FacilityGrade.LEGENDARY_FORGE, 8, true,
                CrafterSpecialization.WEAPON_FORGING, 88.0, 12,
                120.0, 40.0, 12.0, 600.0,
                mythicAnom, mythicMon, ResourceGrade.MYTHIC,
                new EquipmentStatProfile(68.0, 18.0, 28.0, 5.0, 40.0, 25.0, 20.0)
        ));
    }

    public void registerRecipe(EquipmentRecipe recipe) {
        if (recipe != null) {
            recipes.put(recipe.getRecipeId(), recipe);
        }
    }

    public EquipmentRecipe getRecipe(String recipeId) {
        return recipes.get(recipeId);
    }

    public Map<String, EquipmentRecipe> getAllRecipes() {
        return Collections.unmodifiableMap(recipes);
    }

    public EquipmentItem getEquipment(String equipmentId) {
        return allEquipment.get(equipmentId);
    }

    public Map<String, EquipmentItem> getAllEquipment() {
        return Collections.unmodifiableMap(allEquipment);
    }

    public void registerEquipment(EquipmentItem item) {
        if (item != null) {
            allEquipment.put(item.getEquipmentId(), item);
        }
    }

    /**
     * Attempts to create and atomically reserve a crafting work order.
     */
    public synchronized CraftingWorkOrder createCraftOrder(
            String recipeId,
            Village village,
            Organization clan,
            Person crafter,
            CraftingFacility facility,
            int currentYear,
            int currentMonth,
            int awakeningStartYear) {

        if (village == null || crafter == null || facility == null) return null;
        EquipmentRecipe recipe = recipes.get(recipeId);
        if (recipe == null) return null;

        // Era Gating Check
        if (recipe.isSupernatural() && currentYear < awakeningStartYear) {
            return null;
        }

        // Facility Check
        if (facility.getGrade().getMaxEquipmentTier() < recipe.getTargetTier()) {
            return null;
        }
        if (!facility.hasAvailableCapacity()) {
            return null;
        }

        // Crafter Proficiency Check
        CrafterProfile prof = crafter.getOrCreateCrafterProfile();
        if (prof.getProficiency(recipe.getSpecialization()) < recipe.getMinProficiency()) {
            return null;
        }

        // Stockpile & Currency Check
        if (village.getOreStockpile() < recipe.getOreCost()) return null;
        if (village.getWoodStockpile() < recipe.getWoodCost()) return null;
        if (village.getToolsStockpile() < recipe.getToolsCost()) return null;
        if (village.getTreasury() < recipe.getTotalIronCost()) return null;

        // Supernatural Resource Checks
        for (Map.Entry<SupernaturalResourceType, Double> entry : recipe.getSupernaturalCosts().entrySet()) {
            if (village.getSupernaturalInventory().getAmount(entry.getKey()) < entry.getValue()) {
                return null;
            }
        }

        // Monster Material Checks
        for (Map.Entry<MonsterMaterialType, Double> entry : recipe.getMonsterMaterialCosts().entrySet()) {
            if (entry.getKey() == MonsterMaterialType.MONSTER_CORE) {
                if (village.getMonsterCoreCount() < entry.getValue().intValue()) {
                    return null;
                }
            } else {
                if (village.getMonsterMaterial(entry.getKey()) < entry.getValue()) {
                    return null;
                }
            }
        }

        // --- Atomic Material Reservation ---
        village.deductOre(recipe.getOreCost());
        village.deductWood(recipe.getWoodCost());
        village.deductTools(recipe.getToolsCost());
        village.deductTreasury(recipe.getTotalIronCost());

        Map<SupernaturalResourceType, Double> reservedAnom = new EnumMap<>(SupernaturalResourceType.class);
        for (Map.Entry<SupernaturalResourceType, Double> entry : recipe.getSupernaturalCosts().entrySet()) {
            double deducted = village.getSupernaturalInventory().deductResource(entry.getKey(), entry.getValue());
            reservedAnom.put(entry.getKey(), deducted);
            totalAnomalyReserved += deducted;
        }

        Map<MonsterMaterialType, Double> reservedMon = new EnumMap<>(MonsterMaterialType.class);
        for (Map.Entry<MonsterMaterialType, Double> entry : recipe.getMonsterMaterialCosts().entrySet()) {
            if (entry.getKey() == MonsterMaterialType.MONSTER_CORE) {
                int cores = entry.getValue().intValue();
                village.deductMonsterCore(cores);
                reservedMon.put(MonsterMaterialType.MONSTER_CORE, (double) cores);
                totalCoresReserved += cores;
            } else {
                double deducted = village.deductMonsterMaterial(entry.getKey(), entry.getValue());
                reservedMon.put(entry.getKey(), deducted);
            }
        }

        facility.allocateJob();

        String workOrderId = "ORDER_" + (nextWorkOrderSeq++);
        String clanId = (clan != null) ? clan.getOrganizationId() : null;

        CraftingWorkOrder order = new CraftingWorkOrder(
                workOrderId,
                CraftingWorkOrderType.NEW_CRAFT,
                facility.getFacilityId(),
                "VILLAGE_" + village.getId(),
                clanId,
                String.valueOf(crafter.getId()),
                recipe.getRecipeId(),
                recipe,
                null,
                null,
                recipe.getTargetTier(),
                currentYear,
                currentMonth,
                recipe.getDurationMonths(),
                recipe.getTotalIronCost(),
                recipe.getOreCost(),
                recipe.getWoodCost(),
                recipe.getToolsCost(),
                reservedAnom,
                reservedMon,
                recipe.getMinMonsterCoreGrade()
        );

        activeOrders.put(workOrderId, order);
        ordersStarted++;
        return order;
    }

    /**
     * Creates an explicit enchantment work order for an existing equipment item.
     */
    public synchronized CraftingWorkOrder createEnchantOrder(
            String equipmentId,
            EnchantmentFamily family,
            String displayName,
            double primaryBonus,
            double secondaryBonus,
            int potencyTier,
            String catalystType,
            Village village,
            Organization clan,
            Person crafter,
            CraftingFacility facility,
            int currentYear,
            int currentMonth,
            int awakeningStartYear) {

        if (currentYear < awakeningStartYear) return null; // Era gating
        EquipmentItem item = allEquipment.get(equipmentId);
        if (item == null || crafter == null || facility == null || village == null) return null;

        if (!item.canAcceptEnchantment(family)) {
            return null; // Enforce rarity slot ceiling and family uniqueness
        }
        if (!facility.hasAvailableCapacity()) {
            return null;
        }

        // Enchanting requires Aetheric or Enchanting specialization
        CrafterProfile prof = crafter.getOrCreateCrafterProfile();
        double profLevel = Math.max(prof.getProficiency(CrafterSpecialization.ENCHANTING),
                prof.getProficiency(CrafterSpecialization.AETHERIC_CRAFTING));
        if (profLevel < 20.0 + (potencyTier * 6.0)) {
            return null;
        }

        // Catalyst reservation
        double catalystCost = 5.0 * potencyTier;
        if (village.getSupernaturalInventory().getAmount(SupernaturalResourceType.RAW_AETHER_CRYSTAL) < catalystCost) {
            return null;
        }
        double deductedAnom = village.getSupernaturalInventory().deductResource(SupernaturalResourceType.RAW_AETHER_CRYSTAL, catalystCost);
        totalAnomalyReserved += deductedAnom;

        facility.allocateJob();

        String encId = "ENC_" + (nextEnchantmentSeq++);
        EnchantmentRecord enc = new EnchantmentRecord(
                encId, displayName, family, primaryBonus, secondaryBonus, potencyTier, catalystType);

        Map<SupernaturalResourceType, Double> resAnom = new EnumMap<>(SupernaturalResourceType.class);
        resAnom.put(SupernaturalResourceType.RAW_AETHER_CRYSTAL, deductedAnom);

        String workOrderId = "ORDER_ENC_" + (nextWorkOrderSeq++);
        CraftingWorkOrder order = new CraftingWorkOrder(
                workOrderId,
                CraftingWorkOrderType.ENCHANT,
                facility.getFacilityId(),
                "VILLAGE_" + village.getId(),
                (clan != null) ? clan.getOrganizationId() : null,
                String.valueOf(crafter.getId()),
                null,
                null,
                equipmentId,
                enc,
                item.getEquipmentTier(),
                currentYear,
                currentMonth,
                Math.max(1, potencyTier / 2),
                20.0 * potencyTier,
                0.0, 0.0, 0.0,
                resAnom,
                null,
                null
        );

        activeOrders.put(workOrderId, order);
        ordersStarted++;
        return order;
    }

    /**
     * Creates an equipment upgrade/reforging order.
     * Preserves exact same equipmentId, provenance, and heirloom status!
     */
    public synchronized CraftingWorkOrder createUpgradeOrder(
            String equipmentId,
            Village village,
            Organization clan,
            Person crafter,
            CraftingFacility facility,
            int currentYear,
            int currentMonth,
            int awakeningStartYear) {

        EquipmentItem item = allEquipment.get(equipmentId);
        if (item == null || crafter == null || facility == null || village == null) return null;

        int currentTier = item.getEquipmentTier();
        int maxTier = EquipmentItem.getMaxTierForRarity(item.getEquipmentRarity());
        if (currentTier >= maxTier) {
            return null; // Enforce rarity tier ceiling
        }

        int targetTier = currentTier + 1;
        if (facility.getGrade().getMaxEquipmentTier() < targetTier) {
            return null;
        }
        if (!facility.hasAvailableCapacity()) {
            return null;
        }

        double oreCost = 20.0 * targetTier;
        double ironCost = 30.0 * targetTier;
        if (village.getOreStockpile() < oreCost || village.getTreasury() < ironCost) {
            return null;
        }

        village.deductOre(oreCost);
        village.deductTreasury(ironCost);
        facility.allocateJob();

        String workOrderId = "ORDER_UPG_" + (nextWorkOrderSeq++);
        CraftingWorkOrder order = new CraftingWorkOrder(
                workOrderId,
                CraftingWorkOrderType.UPGRADE,
                facility.getFacilityId(),
                "VILLAGE_" + village.getId(),
                (clan != null) ? clan.getOrganizationId() : null,
                String.valueOf(crafter.getId()),
                null,
                null,
                equipmentId,
                null,
                targetTier,
                currentYear,
                currentMonth,
                Math.max(1, targetTier / 2),
                ironCost,
                oreCost, 0.0, 0.0,
                null, null, null
        );

        activeOrders.put(workOrderId, order);
        ordersStarted++;
        return order;
    }

    /**
     * Creates an equipment repair order.
     */
    public synchronized CraftingWorkOrder createRepairOrder(
            String equipmentId,
            Village village,
            Organization clan,
            Person crafter,
            CraftingFacility facility,
            int currentYear,
            int currentMonth) {

        EquipmentItem item = allEquipment.get(equipmentId);
        if (item == null || crafter == null || facility == null || village == null) return null;
        if (item.getDurability() >= item.getMaximumDurability()) {
            return null; // Already full durability
        }
        if (!facility.hasAvailableCapacity()) {
            return null;
        }

        double oreCost = 5.0 * item.getEquipmentTier();
        if (village.getOreStockpile() < oreCost) return null;

        village.deductOre(oreCost);
        facility.allocateJob();

        String workOrderId = "ORDER_REP_" + (nextWorkOrderSeq++);
        CraftingWorkOrder order = new CraftingWorkOrder(
                workOrderId,
                CraftingWorkOrderType.REPAIR,
                facility.getFacilityId(),
                "VILLAGE_" + village.getId(),
                (clan != null) ? clan.getOrganizationId() : null,
                String.valueOf(crafter.getId()),
                null,
                null,
                equipmentId,
                null,
                item.getEquipmentTier(),
                currentYear,
                currentMonth,
                1,
                5.0,
                oreCost, 0.0, 0.0,
                null, null, null
        );

        activeOrders.put(workOrderId, order);
        ordersStarted++;
        return order;
    }

    /**
     * Monthly progression tick for all active crafting work orders.
     */
    public synchronized void processMonthlyCrafting(
            int currentYear,
            int currentMonth,
            List<Village> villages,
            Map<Integer, Person> peopleMap,
            OrganizationSystem organizationSystem,
            Random random) {

        List<CraftingWorkOrder> ordersToProcess = new ArrayList<>(activeOrders.values());

        for (CraftingWorkOrder order : ordersToProcess) {
            // Find village
            Village v = null;
            if (order.getSettlementId() != null) {
                for (Village vill : villages) {
                    if (("VILLAGE_" + vill.getId()).equals(order.getSettlementId())) {
                        v = vill;
                        break;
                    }
                }
            }

            // Find crafter
            Person crafter = null;
            if (order.getCrafterPersonId() != null) {
                try {
                    int pId = Integer.parseInt(order.getCrafterPersonId());
                    crafter = peopleMap.get(pId);
                } catch (NumberFormatException ignored) {}
            }

            // Find facility
            CraftingFacility facility = null;
            if (v != null) {
                facility = v.getCraftingFacility(order.getFacilityId());
            }

            // Interruption check: crafter dead or facility gone
            if (crafter == null || !crafter.isAlive() || facility == null) {
                cancelOrderAndReconcile(order, v, facility);
                continue;
            }

            // Advance progress
            boolean finished = order.advanceProgress();
            if (finished) {
                completeWorkOrder(order, v, crafter, facility, currentYear, currentMonth, random);
            }
        }
    }

    private void completeWorkOrder(
            CraftingWorkOrder order,
            Village village,
            Person crafter,
            CraftingFacility facility,
            int currentYear,
            int currentMonth,
            Random random) {

        CrafterProfile prof = crafter.getOrCreateCrafterProfile();

        switch (order.getOrderType()) {
            case NEW_CRAFT: {
                EquipmentRecipe recipe = order.getRecipe();
                if (recipe == null) {
                    cancelOrderAndReconcile(order, village, facility);
                    return;
                }

                // Deterministic Quality Calculation:
                // clamp(0.35 * CrafterProficiency + 0.25 * FacilityScore + 0.20 * MaterialGradeScore
                //       + 0.10 * (EffectiveIntelligence + EffectiveAetherControl) + roll(-5.0, 5.0), 5.0, 100.0)
                double crafterProf = prof.getProficiency(recipe.getSpecialization());
                double facilityScore = facility.getGrade().getFacilityScore() * 16.0;
                double materialScore = recipe.getTargetRarity().ordinal() * 25.0;
                double crafterInt = crafter.getEffectiveAttribute(AttributeType.INTELLIGENCE);
                double crafterCtrl = crafter.getEffectiveAttribute(AttributeType.AETHER_CONTROL);
                double roll = (random.nextDouble() * 10.0) - 5.0;

                double rawQuality = 0.35 * crafterProf + 0.25 * facilityScore + 0.20 * materialScore
                        + 0.10 * (crafterInt + crafterCtrl) + roll;
                double craftQuality = Math.max(5.0, Math.min(100.0, rawQuality));

                // Quality-adjusted stats
                double qualityMult = 0.85 + 0.30 * (craftQuality / 100.0);
                EquipmentStatProfile adjustedStats = recipe.getBaseStats().scaled(qualityMult);

                double maxDurability = Math.min(500.0, 50.0 + (recipe.getTargetTier() * 25.0) + (craftQuality * 1.5));
                String equipmentId = "EQ_" + (nextEquipmentSeq++);

                int totalMonths = (currentYear * 12) + currentMonth;
                String ownerId = (order.getOrganizationId() != null) ? order.getOrganizationId() : "VILLAGE_" + village.getId();

                EquipmentItem newItem = new EquipmentItem(
                        equipmentId,
                        recipe.getName(),
                        recipe.getCategory(),
                        recipe.getSlot(),
                        recipe.getType(),
                        recipe.isTwoHanded(),
                        recipe.getTargetRarity(),
                        recipe.getTargetTier(),
                        craftQuality,
                        maxDurability,
                        adjustedStats,
                        totalMonths,
                        String.valueOf(crafter.getId()),
                        facility.getFacilityId(),
                        ownerId,
                        "VILLAGE_" + village.getId(),
                        village.getX(),
                        village.getY()
                );

                // Record consumed material composition
                newItem.addMaterialComposition("Ore", order.getReservedOre());
                newItem.addMaterialComposition("Wood", order.getReservedWood());
                newItem.addMaterialComposition("Tools", order.getReservedTools());
                for (Map.Entry<SupernaturalResourceType, Double> e : order.getReservedSupernatural().entrySet()) {
                    newItem.addMaterialComposition(e.getKey().name(), e.getValue());
                    totalAnomalyConsumed += e.getValue();
                    totalAnomalyReserved -= e.getValue();
                }
                for (Map.Entry<MonsterMaterialType, Double> e : order.getReservedMonster().entrySet()) {
                    newItem.addMaterialComposition(e.getKey().name(), e.getValue());
                    if (e.getKey() == MonsterMaterialType.MONSTER_CORE) {
                        totalCoresConsumed += e.getValue();
                        totalCoresReserved -= e.getValue();
                    }
                }

                allEquipment.put(equipmentId, newItem);
                village.addEquipment(newItem);
                order.setCreatedItem(newItem);

                // Advance Crafter Proficiency with diminishing returns
                prof.gainProficiency(recipe.getSpecialization(), 4.0);
                prof.recordOrderCompletion(CraftingWorkOrderType.NEW_CRAFT);

                ordersCompletedCount++;
                break;
            }

            case ENCHANT: {
                EquipmentItem item = allEquipment.get(order.getTargetEquipmentId());
                EnchantmentRecord enc = order.getPendingEnchantment();
                if (item != null && enc != null) {
                    // Check success chance based on crafter skill
                    double crafterProf = Math.max(prof.getProficiency(CrafterSpecialization.ENCHANTING),
                            prof.getProficiency(CrafterSpecialization.AETHERIC_CRAFTING));
                    double successChance = 0.50 + (crafterProf / 200.0);
                    if (random.nextDouble() <= successChance) {
                        item.addEnchantment(enc);
                        enchantmentsAppliedCount++;
                        prof.gainProficiency(CrafterSpecialization.ENCHANTING, 5.0);
                        prof.recordOrderCompletion(CraftingWorkOrderType.ENCHANT);
                    } else {
                        // Failed enchantment: durability loss 15%
                        item.applyDurabilityLoss(item.getMaximumDurability() * 0.15);
                        ordersFailedCount++;
                    }
                    // Catalyst consumed regardless of success/failure
                    for (double val : order.getReservedSupernatural().values()) {
                        totalAnomalyConsumed += val;
                        totalAnomalyReserved -= val;
                    }
                }
                ordersCompletedCount++;
                break;
            }

            case UPGRADE: {
                EquipmentItem item = allEquipment.get(order.getTargetEquipmentId());
                if (item != null) {
                    item.upgradeTier(order.getTargetUpgradeTier());
                    // Boost base stats slightly
                    item.setBaseStatBonuses(item.getBaseStatBonuses().scaled(1.20));
                    upgradesCompletedCount++;
                    prof.gainProficiency(CrafterSpecialization.WEAPON_FORGING, 4.0);
                    prof.recordOrderCompletion(CraftingWorkOrderType.UPGRADE);
                }
                ordersCompletedCount++;
                break;
            }

            case REPAIR: {
                EquipmentItem item = allEquipment.get(order.getTargetEquipmentId());
                if (item != null) {
                    item.repair(item.getMaximumDurability());
                    repairsCompletedCount++;
                    prof.gainProficiency(CrafterSpecialization.REPAIR_AND_RESTORATION, 3.0);
                    prof.recordOrderCompletion(CraftingWorkOrderType.REPAIR);
                }
                ordersCompletedCount++;
                break;
            }
        }

        facility.releaseJob();
        order.setStatus(CraftingWorkOrderStatus.COMPLETED);
        activeOrders.remove(order.getWorkOrderId());
        if (completedOrders.size() >= 200) {
            completedOrders.subList(0, 50).clear();
        }
        completedOrders.add(order);
    }

    private void cancelOrderAndReconcile(CraftingWorkOrder order, Village village, CraftingFacility facility) {
        if (village != null) {
            // Restore reserved materials to village
            village.addOre(order.getReservedOre());
            village.addWood(order.getReservedWood());
            village.addTools(order.getReservedTools());
            village.addTreasury(order.getReservedIron());

            for (Map.Entry<SupernaturalResourceType, Double> e : order.getReservedSupernatural().entrySet()) {
                village.getSupernaturalInventory().addResource(e.getKey(), e.getValue());
                totalAnomalyReserved -= e.getValue();
            }
            for (Map.Entry<MonsterMaterialType, Double> e : order.getReservedMonster().entrySet()) {
                if (e.getKey() == MonsterMaterialType.MONSTER_CORE) {
                    village.addMonsterCore(e.getValue().intValue());
                    totalCoresReserved -= e.getValue();
                } else {
                    village.addMonsterMaterial(e.getKey(), e.getValue());
                }
            }
        }

        if (facility != null) {
            facility.releaseJob();
        }

        order.setStatus(CraftingWorkOrderStatus.CANCELLED);
        activeOrders.remove(order.getWorkOrderId());
        if (cancelledOrders.size() >= 200) {
            cancelledOrders.subList(0, 50).clear();
        }
        cancelledOrders.add(order);
        ordersCancelledCount++;
    }

    /**
     * Authoritative single-point calculation of a person's derived equipment profile.
     */
    public DerivedEquipmentProfile computeDerivedEquipmentProfile(Person person) {
        if (person == null || !person.isAlive()) {
            return DerivedEquipmentProfile.empty();
        }

        Map<EquipmentSlot, String> equipped = person.getEquippedItemIds();
        if (equipped.isEmpty()) {
            return DerivedEquipmentProfile.empty();
        }

        double rawAttack = 0.0;
        double rawDefense = 0.0;
        double rawAccuracy = 0.0;
        double rawEvasion = 0.0;
        double rawPotency = 0.0;
        double rawEfficiency = 0.0;
        double rawBarrier = 0.0;

        int totalCount = 0;
        int brokenCount = 0;

        for (Map.Entry<EquipmentSlot, String> entry : equipped.entrySet()) {
            EquipmentItem item = allEquipment.get(entry.getValue());
            if (item == null) continue;
            totalCount++;

            // Check compatibility gates
            if (!item.isCompatibleWith(person)) {
                continue;
            }

            EquipmentCondition cond = item.getCondition();
            if (cond == EquipmentCondition.BROKEN) {
                brokenCount++;
                continue; // 0.0x bonus, broken items provide nothing
            }

            double mult = cond.getStatMultiplier();

            // Base stats scaled by condition multiplier
            EquipmentStatProfile base = item.getBaseStatBonuses();
            if (base != null) {
                rawAttack += base.getAttackBonus() * mult;
                rawDefense += base.getDefenseBonus() * mult;
                rawAccuracy += base.getAccuracyBonus() * mult;
                rawEvasion += base.getEvasionBonus() * mult;
                rawPotency += base.getAetherPotencyBonus() * mult;
                rawEfficiency += base.getAetherCostEfficiency() * mult;
                rawBarrier += base.getBarrierOrResistanceBonus() * mult;
            }

            // Enchantments scaled by condition multiplier
            for (EnchantmentRecord enc : item.getEnchantments()) {
                switch (enc.getFamily()) {
                    case PHYSICAL_EDGE:
                        rawAttack += enc.getPrimaryBonus() * mult;
                        break;
                    case AETHERIC_EDGE:
                        rawAttack += enc.getPrimaryBonus() * mult;
                        rawPotency += enc.getSecondaryBonus() * mult;
                        break;
                    case ELEMENTAL_AFFINITY:
                        rawPotency += enc.getPrimaryBonus() * mult;
                        break;
                    case MONSTER_BANE:
                        rawAttack += enc.getPrimaryBonus() * mult;
                        break;
                    case AETHERIC_WARD:
                        rawBarrier += enc.getPrimaryBonus() * mult;
                        break;
                    case ELEMENTAL_RESISTANCE:
                        rawDefense += enc.getPrimaryBonus() * mult;
                        break;
                    case REINFORCEMENT:
                        rawDefense += enc.getPrimaryBonus() * mult;
                        break;
                    case IMPACT_ABSORPTION:
                        rawDefense += enc.getPrimaryBonus() * mult;
                        break;
                    case AETHER_CHANNELING:
                        rawEfficiency += enc.getPrimaryBonus() * mult;
                        break;
                    case FOCUS_ENHANCEMENT:
                        rawPotency += enc.getPrimaryBonus() * mult;
                        break;
                    case PERCEPTION_ENHANCEMENT:
                        rawAccuracy += enc.getPrimaryBonus() * mult;
                        break;
                    case CONTROLLED_MOVEMENT:
                        rawEvasion += enc.getPrimaryBonus() * mult;
                        break;
                }
            }
        }

        return new DerivedEquipmentProfile(
                rawAttack, rawDefense, rawAccuracy, rawEvasion,
                rawPotency, rawEfficiency, rawBarrier,
                totalCount, brokenCount
        );
    }

    // Material Conservation Counters
    public void recordMonsterCoreDrop(int count) { totalCoresGenerated += count; }
    public void recordMonsterCoreDelivered(int count) { totalCoresDelivered += count; }
    public void recordMonsterCoreLost(int count) { totalCoresLost += count; }

    public void recordAnomalyResourceDrop(double amount) { totalAnomalyGenerated += amount; }
    public void recordAnomalyResourceDelivered(double amount) { totalAnomalyDelivered += amount; }
    public void recordAnomalyResourceLost(double amount) { totalAnomalyLost += amount; }

    public double getTotalCoresGenerated() { return totalCoresGenerated; }
    public double getTotalCoresDelivered() { return totalCoresDelivered; }
    public double getTotalCoresReserved() { return totalCoresReserved; }
    public double getTotalCoresConsumed() { return totalCoresConsumed; }
    public double getTotalCoresLost() { return totalCoresLost; }

    public double getTotalAnomalyGenerated() { return totalAnomalyGenerated; }
    public double getTotalAnomalyDelivered() { return totalAnomalyDelivered; }
    public double getTotalAnomalyReserved() { return totalAnomalyReserved; }
    public double getTotalAnomalyConsumed() { return totalAnomalyConsumed; }
    public double getTotalAnomalyLost() { return totalAnomalyLost; }

    // Telemetry Getters
    public int getOrdersStarted() { return ordersStarted; }
    public int getOrdersCompletedCount() { return ordersCompletedCount; }
    public int getOrdersFailedCount() { return ordersFailedCount; }
    public int getOrdersCancelledCount() { return ordersCancelledCount; }
    public int getEnchantmentsAppliedCount() { return enchantmentsAppliedCount; }
    public int getUpgradesCompletedCount() { return upgradesCompletedCount; }
    public int getRepairsCompletedCount() { return repairsCompletedCount; }

    public String generateAnnualSummary(ClanHeirloomSystem heirloomSystem) {
        int total = allEquipment.size();
        int common = 0, uncommon = 0, rare = 0, legendary = 0, mythic = 0;
        int equipped = 0, inv = 0, vault = 0, unrec = 0;

        for (EquipmentItem item : allEquipment.values()) {
            switch (item.getEquipmentRarity()) {
                case COMMON: common++; break;
                case UNCOMMON: uncommon++; break;
                case RARE: rare++; break;
                case LEGENDARY: legendary++; break;
                case MYTHIC: mythic++; break;
            }
            switch (item.getLifecycleStatus()) {
                case EQUIPPED: equipped++; break;
                case IN_INVENTORY: inv++; break;
                case IN_CLAN_VAULT: vault++; break;
                case UNRECOVERED_AT_SITE: unrec++; break;
                default: break;
            }
        }

        int hReg = heirloomSystem != null ? heirloomSystem.getHeirloomsRegisteredCount() : 0;
        int hInh = heirloomSystem != null ? heirloomSystem.getHeirloomsInheritedCount() : 0;
        int hLost = heirloomSystem != null ? heirloomSystem.getHeirloomsLostCount() : 0;
        int hCap = heirloomSystem != null ? heirloomSystem.getHeirloomsStolenOrCapturedCount() : 0;
        int hRec = heirloomSystem != null ? heirloomSystem.getHeirloomsRecoveredCount() : 0;

        return String.format("Equip[Tot:%d Rarity(C:%d U:%d R:%d L:%d M:%d) State(Eq:%d Inv:%d Vlt:%d Lost:%d)] | Orders[Comp:%d Act:%d] | Heirlooms[Reg:%d Inh:%d Lost:%d Cap:%d Rec:%d]",
                total, common, uncommon, rare, legendary, mythic, equipped, inv, vault, unrec,
                ordersCompletedCount, activeOrders.size(),
                hReg, hInh, hLost, hCap, hRec);
    }

    public Map<String, CraftingWorkOrder> getActiveOrders() { return Collections.unmodifiableMap(activeOrders); }
    public List<CraftingWorkOrder> getCompletedOrders() { return Collections.unmodifiableList(completedOrders); }

    public synchronized void restoreEquipment(EquipmentItem item) {
        if (item != null) allEquipment.put(item.getEquipmentId(), item);
    }

    public synchronized void restoreActiveOrder(CraftingWorkOrder order) {
        if (order != null) activeOrders.put(order.getWorkOrderId(), order);
    }

    public synchronized void restoreCompletedOrder(CraftingWorkOrder order) {
        if (order != null) completedOrders.add(order);
    }

    public int getNextEquipmentSeq() { return nextEquipmentSeq; }
    public void setNextEquipmentSeq(int seq) { this.nextEquipmentSeq = Math.max(1, seq); }

    public int getNextWorkOrderSeq() { return nextWorkOrderSeq; }
    public void setNextWorkOrderSeq(int seq) { this.nextWorkOrderSeq = Math.max(1, seq); }

    public int getNextEnchantmentSeq() { return nextEnchantmentSeq; }
    public void setNextEnchantmentSeq(int seq) { this.nextEnchantmentSeq = Math.max(1, seq); }

    public void setOrdersStarted(int v) { this.ordersStarted = v; }
    public void setOrdersCompletedCount(int v) { this.ordersCompletedCount = v; }
    public void setOrdersFailedCount(int v) { this.ordersFailedCount = v; }
    public void setOrdersCancelledCount(int v) { this.ordersCancelledCount = v; }
    public void setEnchantmentsAppliedCount(int v) { this.enchantmentsAppliedCount = v; }
    public void setUpgradesCompletedCount(int v) { this.upgradesCompletedCount = v; }
    public void setRepairsCompletedCount(int v) { this.repairsCompletedCount = v; }

    public void setTotalCoresGenerated(double v) { this.totalCoresGenerated = v; }
    public void setTotalCoresDelivered(double v) { this.totalCoresDelivered = v; }
    public void setTotalCoresReserved(double v) { this.totalCoresReserved = v; }
    public void setTotalCoresConsumed(double v) { this.totalCoresConsumed = v; }
    public void setTotalCoresLost(double v) { this.totalCoresLost = v; }

    public void setTotalAnomalyGenerated(double v) { this.totalAnomalyGenerated = v; }
    public void setTotalAnomalyDelivered(double v) { this.totalAnomalyDelivered = v; }
    public void setTotalAnomalyReserved(double v) { this.totalAnomalyReserved = v; }
    public void setTotalAnomalyConsumed(double v) { this.totalAnomalyConsumed = v; }
    public void setTotalAnomalyLost(double v) { this.totalAnomalyLost = v; }
}
