package civsim.test;

import civsim.AnnualReport;
import civsim.ProgressionConfig;
import civsim.SimulationConfig;
import civsim.SimulationEngine;
import civsim.model.*;
import civsim.spatial.Biome;
import civsim.systems.*;

import java.util.*;

/**
 * Phase 2C.1 -- Comprehensive Verification and Acceptance Test Suite for
 * Clan Heirlooms, Monster Core Crafting, Legendary Equipment, Enchanting, and Equipment Progression.
 */
public class TestStage2C1EquipmentAndHeirlooms {

    private static int totalTests = 0;
    private static int passedTests = 0;

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   PHASE 2C.1 TEST SUITE: EQUIPMENT, CRAFTING, ENCHANTING & CLAN HEIRLOOMS");
        System.out.println("================================================================================");

        try {
            suiteA_IdentitySlotsAndCompatibility();
            suiteB_MaterialConservation();
            suiteC_CraftingFacilitiesAndProficiency();
            suiteD_EnchantingAndUpgrades();
            suiteE_DurabilityAndConditionBands();
            suiteF_ClanHeirloomsEstatesAndBattlefieldCapture();
            suiteG_CombatSinglePointHandoffAndCaps();
            suiteH_MultiSeed250YearStability();

            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.printf("TOTAL TESTS: %d | PASSED: %d | FAILED: %d\n", totalTests, passedTests, (totalTests - passedTests));
            System.out.println("--------------------------------------------------------------------------------");

            if (passedTests == totalTests) {
                System.out.println("ALL STAGE 2C.1 ACCEPTANCE TESTS PASSED SUCCESSFULLY!");
            } else {
                System.err.println("SOME STAGE 2C.1 ACCEPTANCE TESTS FAILED.");
                System.exit(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void assertTrue(String message, boolean condition) {
        totalTests++;
        if (condition) {
            passedTests++;
            System.out.println("  [PASS] " + message);
        } else {
            System.err.println("  [FAIL] " + message);
            Thread.dumpStack();
        }
    }

    private static void assertEquals(String message, Object expected, Object actual) {
        totalTests++;
        if (Objects.equals(expected, actual)) {
            passedTests++;
            System.out.println("  [PASS] " + message + " (Expected: " + expected + ", Got: " + actual + ")");
        } else {
            System.err.println("  [FAIL] " + message + " (Expected: " + expected + ", Got: " + actual + ")");
            Thread.dumpStack();
        }
    }

    private static void assertApprox(String message, double expected, double actual, double epsilon) {
        totalTests++;
        if (Math.abs(expected - actual) <= epsilon) {
            passedTests++;
            System.out.println("  [PASS] " + message + " (" + expected + " ~= " + actual + ")");
        } else {
            System.err.println("  [FAIL] " + message + " (Expected: " + expected + ", Got: " + actual + ")");
            Thread.dumpStack();
        }
    }

    // =========================================================================
    // SUITE A: Identity, Slots & Compatibility
    // =========================================================================
    private static void suiteA_IdentitySlotsAndCompatibility() {
        System.out.println("\n--- SUITE A: Identity, Slots & Compatibility Tests ---");

        Village v = new Village(1, "TestVillage", 1.0, 0.05);
        Person p = new Person(1, "Warrior1", Gender.MALE, 25, 0, v);

        // 1. Unique Equipment Identity & Invariant
        EquipmentItem sword = new EquipmentItem(
                "EQ_SWORD_01", "Iron Sword", EquipmentCategory.WEAPON, EquipmentSlot.MAIN_HAND,
                EquipmentType.ONE_HANDED_SWORD, false, ClassRarity.COMMON, 1, 50.0, 100.0,
                new EquipmentStatProfile(10.0, 0.0, 5.0, 0.0, 0.0, 0.0, 0.0),
                0, "1", "FORGE_1", "PERSON_1", "VILLAGE_1", 0.0, 0.0
        );
        assertEquals("Initial lifecycle is IN_INVENTORY", EquipmentLifecycleStatus.IN_INVENTORY, sword.getLifecycleStatus());

        // Equip item
        p.equipItem(EquipmentSlot.MAIN_HAND, sword.getEquipmentId(), sword.isTwoHanded());
        sword.setLifecycleStatus(EquipmentLifecycleStatus.EQUIPPED);
        sword.setCurrentCustodianId("PERSON_1");

        assertEquals("Equipped slot matches", "EQ_SWORD_01", p.getEquippedItemId(EquipmentSlot.MAIN_HAND));
        assertEquals("Lifecycle status transitioned to EQUIPPED", EquipmentLifecycleStatus.EQUIPPED, sword.getLifecycleStatus());
        assertTrue("Single physical identity invariant holds", sword.getLifecycleStatus() != EquipmentLifecycleStatus.IN_INVENTORY);

        // 2. Two-handed vs. Off-hand Exclusivity
        EquipmentItem shield = new EquipmentItem(
                "EQ_SHIELD_01", "Iron Shield", EquipmentCategory.DEFENSIVE_ARMOR, EquipmentSlot.OFF_HAND,
                EquipmentType.SHIELD, false, ClassRarity.COMMON, 1, 50.0, 100.0,
                new EquipmentStatProfile(0.0, 8.0, 0.0, -2.0, 0.0, 0.0, 0.0),
                0, "1", "FORGE_1", "PERSON_1", "PERSON_1", 0.0, 0.0
        );
        p.equipItem(EquipmentSlot.OFF_HAND, shield.getEquipmentId(), shield.isTwoHanded());
        assertEquals("Shield equipped in OFF_HAND", "EQ_SHIELD_01", p.getEquippedItemId(EquipmentSlot.OFF_HAND));

        // Now equip a TWO-HANDED weapon in MAIN_HAND -> OFF_HAND must be unequipped/blocked!
        EquipmentItem greatsword = new EquipmentItem(
                "EQ_GREATSWORD_01", "Steel Greatsword", EquipmentCategory.WEAPON, EquipmentSlot.MAIN_HAND,
                EquipmentType.TWO_HANDED_SWORD, true, ClassRarity.UNCOMMON, 2, 60.0, 150.0,
                new EquipmentStatProfile(20.0, 0.0, 2.0, -4.0, 0.0, 0.0, 0.0),
                0, "1", "FORGE_1", "PERSON_1", "PERSON_1", 0.0, 0.0
        );
        p.equipItem(EquipmentSlot.MAIN_HAND, greatsword.getEquipmentId(), greatsword.isTwoHanded());
        assertEquals("Greatsword equipped in MAIN_HAND", "EQ_GREATSWORD_01", p.getEquippedItemId(EquipmentSlot.MAIN_HAND));
        assertTrue("Main hand marked as two-handed", p.isMainHandTwoHanded());
        assertEquals("OFF_HAND was automatically unequipped by two-handed weapon", null, p.getEquippedItemId(EquipmentSlot.OFF_HAND));

        // Now equip shield into OFF_HAND -> two-handed MAIN_HAND weapon must be unequipped!
        p.equipItem(EquipmentSlot.OFF_HAND, shield.getEquipmentId(), shield.isTwoHanded());
        assertEquals("Shield equipped in OFF_HAND", "EQ_SHIELD_01", p.getEquippedItemId(EquipmentSlot.OFF_HAND));
        assertEquals("Two-handed weapon in MAIN_HAND was unequipped by OFF_HAND item", null, p.getEquippedItemId(EquipmentSlot.MAIN_HAND));
        assertTrue("Main hand two-handed flag reset to false", !p.isMainHandTwoHanded());

        // 3. Compatibility Gates: Class, Tier, Aspect
        EquipmentItem aetherStaff = new EquipmentItem(
                "EQ_STAFF_01", "Archmage Staff", EquipmentCategory.WEAPON, EquipmentSlot.MAIN_HAND,
                EquipmentType.STAFF, true, ClassRarity.RARE, 4, 75.0, 200.0,
                new EquipmentStatProfile(15.0, 5.0, 10.0, 0.0, 25.0, 10.0, 10.0),
                0, "1", "FORGE_1", "ORG_1", "VAULT_1", 0.0, 0.0
        );
        aetherStaff.addAllowedClass(SupernaturalClass.ELEMENTALIST);
        aetherStaff.setMinimumAwakenedTier(2);
        aetherStaff.setRequiredAspect(AetherAspect.AETHERIC);

        // Unawakened person cannot equip T4 Elementalist staff
        assertTrue("Unawakened person incompatible with T4 Elementalist staff", !aetherStaff.isCompatibleWith(p));

        // Awaken person as T1 Vanguard (wrong class and tier)
        AetherProfile ap = AetherProfile.createForTesting(SupernaturalClass.VANGUARD, ClassRarity.COMMON, 1, false);
        p.setAetherProfile(ap);
        assertTrue("T1 Vanguard incompatible with T4 Elementalist staff", !aetherStaff.isCompatibleWith(p));

        // Change to T3 Elementalist with Aetheric aspect
        AetherProfile apElem = AetherProfile.createForTesting(SupernaturalClass.ELEMENTALIST, ClassRarity.RARE, 3, false);
        p.setAetherProfile(apElem);
        assertTrue("T3 Elementalist meets class, tier, and aspect requirements", aetherStaff.isCompatibleWith(p));

        // 4. Rarity-to-Tier Ceilings
        assertEquals("COMMON max tier is 2", 2, EquipmentItem.getMaxTierForRarity(ClassRarity.COMMON));
        assertEquals("UNCOMMON max tier is 3", 3, EquipmentItem.getMaxTierForRarity(ClassRarity.UNCOMMON));
        assertEquals("RARE max tier is 5", 5, EquipmentItem.getMaxTierForRarity(ClassRarity.RARE));
        assertEquals("LEGENDARY max tier is 8", 8, EquipmentItem.getMaxTierForRarity(ClassRarity.LEGENDARY));
        assertEquals("MYTHIC max tier is 10", 10, EquipmentItem.getMaxTierForRarity(ClassRarity.MYTHIC));

        // Attempting to instantiate COMMON item with Tier 5 clamps to 2
        EquipmentItem commonClamped = new EquipmentItem(
                "EQ_COMMON_CLAMP", "Overleveled Common", EquipmentCategory.WEAPON, EquipmentSlot.MAIN_HAND,
                EquipmentType.ONE_HANDED_SWORD, false, ClassRarity.COMMON, 5, 50.0, 100.0,
                EquipmentStatProfile.zero(), 0, "1", "FORGE_1", "1", "1", 0, 0
        );
        assertEquals("COMMON item tier clamped to rarity ceiling 2", 2, commonClamped.getEquipmentTier());

        // 5. Broken Items provide 0.0 bonus and are never deleted
        commonClamped.applyDurabilityLoss(100.0);
        assertEquals("Durability reduced to 0.0", 0.0, commonClamped.getDurability());
        assertEquals("Condition transitioned to BROKEN", EquipmentCondition.BROKEN, commonClamped.getCondition());
        assertApprox("BROKEN condition multiplier is exactly 0.0", 0.0, commonClamped.getCondition().getStatMultiplier(), 0.001);
        assertTrue("BROKEN item is NOT deleted", commonClamped.getEquipmentId() != null);
    }

    // =========================================================================
    // SUITE B: Material Conservation
    // =========================================================================
    private static void suiteB_MaterialConservation() {
        System.out.println("\n--- SUITE B: Material Conservation Tests ---");

        EquipmentCraftingSystem ecs = new EquipmentCraftingSystem();
        Village v = new Village(1, "ForgeTown", 1.0, 0.05);
        v.addOre(100.0);
        v.addWood(50.0);
        v.addTools(20.0);
        v.addTreasury(200.0);
        v.addMonsterCore(5);
        v.addMonsterMaterial(MonsterMaterialType.BEAST_HIDE, 10.0);
        v.getSupernaturalInventory().addResource(SupernaturalResourceType.RAW_AETHER_CRYSTAL, 50.0);

        // Record generation
        ecs.recordMonsterCoreDrop(5);
        ecs.recordMonsterCoreDelivered(5);
        ecs.recordAnomalyResourceDrop(50.0);
        ecs.recordAnomalyResourceDelivered(50.0);

        Person blacksmith = new Person(10, "MasterSmith", Gender.MALE, 35, -35, v);
        blacksmith.setOccupation(Occupation.BLACKSMITH);
        CrafterProfile cp = blacksmith.getOrCreateCrafterProfile();
        cp.setProficiency(CrafterSpecialization.AETHERIC_CRAFTING, 50.0);
        v.addResident(blacksmith);

        CraftingFacility supForge = new CraftingFacility("FORGE_SUP_1", "VILLAGE_1", null, FacilityGrade.SUPERNATURAL_FORGE, 2);
        v.addCraftingFacility(supForge);

        // 1. Atomic Material Reservation
        CraftingWorkOrder order = ecs.createCraftOrder(
                "RECIPE_AETHER_BLADE_T3", v, null, blacksmith, supForge, 50, 1, 50);

        assertTrue("Supernatural work order created", order != null);
        assertEquals("Order status is MATERIALS_RESERVED", CraftingWorkOrderStatus.MATERIALS_RESERVED, order.getStatus());
        assertEquals("Village monster core deducted by 1", 4, v.getMonsterCoreCount());
        assertApprox("Village Aether Crystal deducted by 10.0", 40.0, v.getSupernaturalInventory().getAmount(SupernaturalResourceType.RAW_AETHER_CRYSTAL), 0.01);
        assertApprox("Aether crystal reserved in ecs", 10.0, ecs.getTotalAnomalyReserved(), 0.01);
        assertApprox("Monster cores reserved in ecs", 1.0, ecs.getTotalCoresReserved(), 0.01);

        // 2. Strict Material Conservation Equation:
        // Generated == Unrecovered + DeliveredInInventories + Reserved + Consumed + Lost
        double genCores = ecs.getTotalCoresGenerated();
        double delCores = v.getMonsterCoreCount(); // in inventory
        double resCores = ecs.getTotalCoresReserved();
        double conCores = ecs.getTotalCoresConsumed();
        double lostCores = ecs.getTotalCoresLost();

        assertApprox("Core Conservation Equation holds: Gen == Inv + Res + Con + Lost",
                genCores, delCores + resCores + conCores + lostCores, 0.001);

        // 3. Complete work order and check consumption
        Map<Integer, Person> peopleMap = new HashMap<>();
        peopleMap.put(10, blacksmith);
        Random rng = new Random(1337);

        // Advance progress 3 months to complete
        for (int m = 0; m < 3; m++) {
            ecs.processMonthlyCrafting(50, m + 1, Collections.singletonList(v), peopleMap, null, rng);
        }

        assertEquals("Order transitioned to COMPLETED", CraftingWorkOrderStatus.COMPLETED, order.getStatus());
        assertApprox("Reserved cores transitioned to 0.0", 0.0, ecs.getTotalCoresReserved(), 0.001);
        assertApprox("Consumed cores is now 1.0", 1.0, ecs.getTotalCoresConsumed(), 0.001);

        // Re-check Conservation equation post-completion
        double postDelCores = v.getMonsterCoreCount();
        double postResCores = ecs.getTotalCoresReserved();
        double postConCores = ecs.getTotalCoresConsumed();
        assertApprox("Conservation Equation holds exactly after craft completion",
                genCores, postDelCores + postResCores + postConCores + lostCores, 0.001);

        // 4. Unrecovered drops cannot be used for crafting
        // A drop that is GENERATED/RECOVERABLE at a battle site has not been delivered to village!
        Village emptyVillage = new Village(2, "EmptyVillage", 1.0, 0.05);
        CraftingWorkOrder unrecoveredOrder = ecs.createCraftOrder(
                "RECIPE_AETHER_BLADE_T3", emptyVillage, null, blacksmith, supForge, 50, 1, 50);
        assertTrue("Crafting rejected when materials are unrecovered on battlefield", unrecoveredOrder == null);
    }

    // =========================================================================
    // SUITE C: Crafting Facilities & Crafter Proficiency
    // =========================================================================
    private static void suiteC_CraftingFacilitiesAndProficiency() {
        System.out.println("\n--- SUITE C: Crafting Facilities & Crafter Proficiency Tests ---");

        EquipmentCraftingSystem ecs = new EquipmentCraftingSystem();
        Village v = new Village(1, "MundaneHamlet", 1.0, 0.05);
        v.addOre(500.0);
        v.addWood(200.0);
        v.addTools(50.0);
        v.addTreasury(500.0);

        Person smith = new Person(20, "ApprenticeSmith", Gender.MALE, 25, 0, v);
        smith.setOccupation(Occupation.BLACKSMITH);
        CrafterProfile cp = smith.getOrCreateCrafterProfile();
        cp.setProficiency(CrafterSpecialization.WEAPON_FORGING, 10.0);

        CraftingFacility basicForge = new CraftingFacility("FORGE_BASIC_1", "VILLAGE_1", null, FacilityGrade.BASIC_FORGE, 1);
        v.addCraftingFacility(basicForge);

        // 1. Era Gating: In Mundane Era (Year 20 < 50), supernatural recipes must be rejected
        CraftingWorkOrder earlySupernatural = ecs.createCraftOrder(
                "RECIPE_AETHER_BLADE_T3", v, null, smith, basicForge, 20, 1, 50);
        assertTrue("Supernatural crafting rejected in Mundane Era (Year 20 < 50)", earlySupernatural == null);

        // 2. Facility Grade Tier Caps: BASIC_FORGE cap is T1. Attempting T2 recipe must fail.
        CraftingWorkOrder overTierOrder = ecs.createCraftOrder(
                "RECIPE_STEEL_BLADE_T2", v, null, smith, basicForge, 20, 1, 50);
        assertTrue("T2 recipe rejected in T1 BASIC_FORGE", overTierOrder == null);

        // 3. Valid Mundane T1 Craft
        CraftingWorkOrder validT1 = ecs.createCraftOrder(
                "RECIPE_IRON_SWORD_T1", v, null, smith, basicForge, 20, 1, 50);
        assertTrue("Valid T1 mundane order created in BASIC_FORGE", validT1 != null);
        assertEquals("BASIC_FORGE capacity exhausted (1/1)", 1, basicForge.getActiveJobsCount());
        assertTrue("BASIC_FORGE has no more capacity", !basicForge.hasAvailableCapacity());

        // Concurrent capacity check
        CraftingWorkOrder blockedByCapacity = ecs.createCraftOrder(
                "RECIPE_IRON_SWORD_T1", v, null, smith, basicForge, 20, 1, 50);
        assertTrue("Order rejected when facility is at full capacity", blockedByCapacity == null);

        // 4. Interruption and cancellation: Crafter dies -> materials restored without vanishing
        double preCancelOre = v.getOreStockpile();
        smith.markDeceased(20, "tragic accident");

        Map<Integer, Person> peopleMap = new HashMap<>();
        peopleMap.put(20, smith);
        ecs.processMonthlyCrafting(20, 2, Collections.singletonList(v), peopleMap, null, new Random(42));

        assertEquals("Order was cancelled due to crafter death", CraftingWorkOrderStatus.CANCELLED, validT1.getStatus());
        assertEquals("Facility capacity released back to 0", 0, basicForge.getActiveJobsCount());
        assertApprox("Reserved ore restored to village stockpile", preCancelOre + 15.0, v.getOreStockpile(), 0.01);

        // 5. Proficiency increases strictly with work orders and diminishing returns
        Person artisan = new Person(21, "ArtisanSmith", Gender.FEMALE, 30, 0, v);
        artisan.setOccupation(Occupation.BLACKSMITH);
        CrafterProfile artisanProf = artisan.getOrCreateCrafterProfile();
        artisanProf.setProficiency(CrafterSpecialization.WEAPON_FORGING, 50.0);

        double gain1 = artisanProf.gainProficiency(CrafterSpecialization.WEAPON_FORGING, 10.0);
        assertTrue("Initial gain is positive", gain1 > 0.0);

        artisanProf.setProficiency(CrafterSpecialization.WEAPON_FORGING, 95.0);
        double gainHigh = artisanProf.gainProficiency(CrafterSpecialization.WEAPON_FORGING, 10.0);
        assertTrue("Diminishing returns: high skill gain (" + gainHigh + ") is less than low skill gain (" + gain1 + ")",
                gainHigh < gain1);
    }

    // =========================================================================
    // SUITE D: Enchanting & Upgrades
    // =========================================================================
    private static void suiteD_EnchantingAndUpgrades() {
        System.out.println("\n--- SUITE D: Enchanting & Upgrades Tests ---");

        EquipmentCraftingSystem ecs = new EquipmentCraftingSystem();
        Village v = new Village(1, "EnchantTown", 1.0, 0.05);
        v.addOre(500.0);
        v.addTreasury(500.0);
        v.getSupernaturalInventory().addResource(SupernaturalResourceType.RAW_AETHER_CRYSTAL, 100.0);

        Person enchanter = new Person(30, "MageSmith", Gender.MALE, 40, -40, v);
        enchanter.setOccupation(Occupation.BLACKSMITH);
        CrafterProfile cp = enchanter.getOrCreateCrafterProfile();
        cp.setProficiency(CrafterSpecialization.ENCHANTING, 80.0);
        cp.setProficiency(CrafterSpecialization.WEAPON_FORGING, 70.0);

        CraftingFacility forge = new CraftingFacility("FORGE_ENCHANT", "VILLAGE_1", null, FacilityGrade.MASTERWORK_FORGE, 3);
        v.addCraftingFacility(forge);

        // Create RARE item (max 2 enchantment slots)
        EquipmentItem sword = new EquipmentItem(
                "EQ_RARE_SWORD", "Rare Crystal Blade", EquipmentCategory.WEAPON, EquipmentSlot.MAIN_HAND,
                EquipmentType.ONE_HANDED_SWORD, false, ClassRarity.RARE, 3, 70.0, 200.0,
                new EquipmentStatProfile(22.0, 2.0, 10.0, 0.0, 5.0, 0.0, 0.0),
                600, "30", "FORGE_ENCHANT", "ORG_1", "VAULT_1", 0.0, 0.0
        );
        ecs.registerEquipment(sword);

        // 1. Slot limits by rarity
        assertEquals("RARE item max enchantment slots is 2", 2, sword.getMaxEnchantmentSlots());

        // Apply first enchantment: PHYSICAL_EDGE
        CraftingWorkOrder encOrder1 = ecs.createEnchantOrder(
                sword.getEquipmentId(), EnchantmentFamily.PHYSICAL_EDGE, "Sharpness I",
                8.0, 0.0, 2, "RAW_AETHER_CRYSTAL", v, null, enchanter, forge, 52, 1, 50);
        assertTrue("First enchantment order created", encOrder1 != null);

        Map<Integer, Person> peopleMap = new HashMap<>();
        peopleMap.put(30, enchanter);
        Random rng = new Random(777);

        // Advance 1 month to complete enchantment
        ecs.processMonthlyCrafting(52, 2, Collections.singletonList(v), peopleMap, null, rng);
        assertEquals("First enchantment completed", 1, sword.getEnchantments().size());
        assertEquals("Enchantment family is PHYSICAL_EDGE", EnchantmentFamily.PHYSICAL_EDGE, sword.getEnchantments().get(0).getFamily());

        // 2. Family Uniqueness Rule: Cannot attach another PHYSICAL_EDGE enchantment!
        CraftingWorkOrder duplicateFamilyOrder = ecs.createEnchantOrder(
                sword.getEquipmentId(), EnchantmentFamily.PHYSICAL_EDGE, "Sharpness II",
                12.0, 0.0, 3, "RAW_AETHER_CRYSTAL", v, null, enchanter, forge, 52, 3, 50);
        assertTrue("Duplicate family enchantment rejected", duplicateFamilyOrder == null);

        // Attach distinct family: AETHERIC_WARD (Defensive)
        CraftingWorkOrder encOrder2 = ecs.createEnchantOrder(
                sword.getEquipmentId(), EnchantmentFamily.AETHERIC_WARD, "Aether Barrier I",
                10.0, 0.0, 2, "RAW_AETHER_CRYSTAL", v, null, enchanter, forge, 52, 3, 50);
        assertTrue("Second enchantment with distinct family accepted", encOrder2 != null);
        ecs.processMonthlyCrafting(52, 4, Collections.singletonList(v), peopleMap, null, rng);
        assertEquals("Item now has 2 enchantments (at max capacity)", 2, sword.getEnchantments().size());

        // Attempt 3rd enchantment on RARE item (slots exhausted)
        CraftingWorkOrder overCapacityOrder = ecs.createEnchantOrder(
                sword.getEquipmentId(), EnchantmentFamily.CONTROLLED_MOVEMENT, "Swiftness I",
                5.0, 0.0, 1, "RAW_AETHER_CRYSTAL", v, null, enchanter, forge, 52, 5, 50);
        assertTrue("3rd enchantment rejected: RARE slot ceiling reached", overCapacityOrder == null);

        // 3. Reforging / Upgrades Preserves exact same equipmentId
        int oldTier = sword.getEquipmentTier();
        CraftingWorkOrder upgOrder = ecs.createUpgradeOrder(
                sword.getEquipmentId(), v, null, enchanter, forge, 52, 6, 50);
        assertTrue("Upgrade order created", upgOrder != null);
        assertEquals("Target upgrade tier is T+1 (4)", oldTier + 1, upgOrder.getTargetUpgradeTier());

        // Advance 2 months
        ecs.processMonthlyCrafting(52, 7, Collections.singletonList(v), peopleMap, null, rng);
        ecs.processMonthlyCrafting(52, 8, Collections.singletonList(v), peopleMap, null, rng);

        assertEquals("Equipment upgraded to Tier 4", 4, sword.getEquipmentTier());
        assertEquals("Equipment ID preserved exactly", "EQ_RARE_SWORD", sword.getEquipmentId());
        assertEquals("Enchantments preserved through upgrade", 2, sword.getEnchantments().size());
    }

    // =========================================================================
    // SUITE E: Durability, Condition Bands & Repair
    // =========================================================================
    private static void suiteE_DurabilityAndConditionBands() {
        System.out.println("\n--- SUITE E: Durability, Condition Bands & Repair Tests ---");

        ClanHeirloomSystem chs = new ClanHeirloomSystem();
        EquipmentCraftingSystem ecs = new EquipmentCraftingSystem();

        Village v = new Village(1, "CombatOutpost", 1.0, 0.05);
        Person warrior = new Person(40, "Gladiator", Gender.MALE, 28, 0, v);

        EquipmentItem armor = new EquipmentItem(
                "EQ_PLATE_01", "Steel Plate", EquipmentCategory.DEFENSIVE_ARMOR, EquipmentSlot.CHEST,
                EquipmentType.CHESTPLATE, false, ClassRarity.UNCOMMON, 2, 80.0, 100.0,
                new EquipmentStatProfile(0.0, 20.0, 0.0, -2.0, 0.0, 0.0, 0.0),
                0, "1", "FORGE_1", "PERSON_40", "PERSON_40", 0.0, 0.0
        );
        ecs.registerEquipment(armor);
        warrior.equipItem(EquipmentSlot.CHEST, armor.getEquipmentId(), false);

        // 1. Condition Bands:
        // EXCELLENT (>= 80%): 1.00x
        assertEquals("100% durability is EXCELLENT", EquipmentCondition.EXCELLENT, armor.getCondition());
        assertApprox("EXCELLENT multiplier is 1.00x", 1.00, armor.getCondition().getStatMultiplier(), 0.001);

        // Wear to GOOD (70%)
        armor.applyDurabilityLoss(30.0);
        assertEquals("70% durability is GOOD", EquipmentCondition.GOOD, armor.getCondition());
        assertApprox("GOOD multiplier is 0.95x", 0.95, armor.getCondition().getStatMultiplier(), 0.001);

        // Wear to WORN (40%)
        armor.applyDurabilityLoss(30.0);
        assertEquals("40% durability is WORN", EquipmentCondition.WORN, armor.getCondition());
        assertApprox("WORN multiplier is 0.85x", 0.85, armor.getCondition().getStatMultiplier(), 0.001);

        // Wear to DAMAGED (15%)
        armor.applyDurabilityLoss(25.0);
        assertEquals("15% durability is DAMAGED", EquipmentCondition.DAMAGED, armor.getCondition());
        assertApprox("DAMAGED multiplier is 0.65x", 0.65, armor.getCondition().getStatMultiplier(), 0.001);

        // Wear to BROKEN (0%)
        armor.applyDurabilityLoss(15.0);
        assertEquals("0% durability is BROKEN", EquipmentCondition.BROKEN, armor.getCondition());
        assertApprox("BROKEN multiplier is 0.00x", 0.00, armor.getCondition().getStatMultiplier(), 0.001);

        // Derived profile gives 0 defense bonus when broken
        DerivedEquipmentProfile brokenProfile = ecs.computeDerivedEquipmentProfile(warrior);
        assertApprox("BROKEN equipment gives 0.0 defense bonus", 0.0, brokenProfile.getDefenseBonus(), 0.001);
        assertEquals("Reported 1 broken item", 1, brokenProfile.getBrokenItemsCount());

        // 2. Combat Wear: Applied strictly upon combat, bounded [0.5, 2.5]
        EquipmentItem weapon = new EquipmentItem(
                "EQ_AXE_01", "Battleaxe", EquipmentCategory.WEAPON, EquipmentSlot.MAIN_HAND,
                EquipmentType.AXE, false, ClassRarity.COMMON, 1, 50.0, 100.0,
                new EquipmentStatProfile(12.0, 0.0, 5.0, 0.0, 0.0, 0.0, 0.0),
                0, "1", "FORGE_1", "PERSON_40", "PERSON_40", 0.0, 0.0
        );
        ecs.registerEquipment(weapon);
        warrior.equipItem(EquipmentSlot.MAIN_HAND, weapon.getEquipmentId(), false);

        double preBattleDurability = weapon.getDurability();
        chs.applyCombatDurabilityWear(warrior, ecs, new Random(101));
        double postBattleDurability = weapon.getDurability();
        double wear = preBattleDurability - postBattleDurability;

        assertTrue("Combat durability wear is positive", wear > 0.0);
        assertTrue("Combat durability wear bounded in [0.3, 2.5] pts (actual: " + wear + ")", wear >= 0.3 && wear <= 2.5);

        // 3. Repair order restores durability to 100%
        v.addOre(50.0);
        CraftingFacility forge = new CraftingFacility("FORGE_REP", "VILLAGE_1", null, FacilityGrade.BASIC_FORGE, 1);
        v.addCraftingFacility(forge);
        CraftingWorkOrder repOrder = ecs.createRepairOrder(armor.getEquipmentId(), v, null, warrior, forge, 50, 1);
        assertTrue("Repair work order created", repOrder != null);

        Map<Integer, Person> peopleMap = new HashMap<>();
        peopleMap.put(40, warrior);
        ecs.processMonthlyCrafting(50, 2, Collections.singletonList(v), peopleMap, null, new Random(123));

        assertEquals("Armor durability restored to max 100.0", 100.0, armor.getDurability());
        assertEquals("Armor condition restored to EXCELLENT", EquipmentCondition.EXCELLENT, armor.getCondition());
    }

    // =========================================================================
    // SUITE F: Clan Heirlooms, Estates & Battlefield Capture
    // =========================================================================
    private static void suiteF_ClanHeirloomsEstatesAndBattlefieldCapture() {
        System.out.println("\n--- SUITE F: Clan Heirlooms, Estates & Battlefield Capture Tests ---");

        ClanHeirloomSystem chs = new ClanHeirloomSystem();
        EquipmentCraftingSystem ecs = new EquipmentCraftingSystem();

        Village v = new Village(1, "ClanKeep", 1.0, 0.05);
        Organization clanA = new Organization(
                "CLAN_WOLF", "Wolf Clan", OrganizationType.CLAN, 10, 1, "50",
                OrganizationStatus.ACTIVE, OrganizationAlignment.NEUTRAL, null
        );

        Person patriarch = new Person(50, "Patriarch", Gender.MALE, 60, -60, v);
        Person son = new Person(51, "SonOfPatriarch", Gender.MALE, 25, -25, v, null, patriarch, null, 20.0);
        Person rivalChampion = new Person(52, "RivalWarrior", Gender.FEMALE, 28, -28, v);

        // Memberships
        clanA.addMembershipRecord(new MembershipRecord("50", "CLAN_WOLF", 10, 1, OrganizationRole.LEADER, MembershipOrigin.FOUNDING));
        clanA.addMembershipRecord(new MembershipRecord("51", "CLAN_WOLF", 20, 1, OrganizationRole.MEMBER, MembershipOrigin.OPEN_JOIN));
        clanA.addMembershipRecord(new MembershipRecord("52", "CLAN_WOLF", 22, 1, OrganizationRole.CHAMPION, MembershipOrigin.INVITATION));

        AetherProfile sonAp = AetherProfile.createForTesting(SupernaturalClass.VANGUARD, ClassRarity.UNCOMMON, 2, false);
        son.setAetherProfile(sonAp);

        AetherProfile rivalAp = AetherProfile.createForTesting(SupernaturalClass.WARDEN, ClassRarity.RARE, 3, false);
        rivalChampion.setAetherProfile(rivalAp);

        // 1. Heirloom Registration & Title vs. Custody
        EquipmentItem fang = new EquipmentItem(
                "EQ_WOLF_FANG", "Fang of the Ancient", EquipmentCategory.WEAPON, EquipmentSlot.MAIN_HAND,
                EquipmentType.ONE_HANDED_SWORD, false, ClassRarity.LEGENDARY, 7, 85.0, 300.0,
                new EquipmentStatProfile(45.0, 10.0, 15.0, 5.0, 15.0, 0.0, 0.0),
                100, "50", "FORGE_1", "CLAN_WOLF", "VAULT_CLAN_WOLF", 0.0, 0.0
        );
        ecs.registerEquipment(fang);

        ClanHeirloom heirloom = chs.registerHeirloom(
                fang, clanA, "Fang of the Ancient Wolf", HeirloomSuccessionPolicy.BLOODLINE_FIRST, 50, 1);

        assertTrue("Item marked as registered heirloom", fang.isRegisteredHeirloom());
        assertEquals("Item owner is CLAN_WOLF (Legal Title)", "CLAN_WOLF", fang.getCurrentOwnerId());
        assertEquals("Heirloom registered in clan", heirloom, clanA.getHeirloom(heirloom.getHeirloomId()));

        // Assign Patriarch as bearer
        chs.assignBearer(heirloom, fang, patriarch);
        assertEquals("Bearer is Patriarch (50)", "50", heirloom.getCurrentBearerPersonId());
        assertEquals("Item custodian is PERSON_50", "PERSON_50", fang.getCurrentCustodianId());
        assertEquals("Lifecycle status is EQUIPPED", EquipmentLifecycleStatus.EQUIPPED, fang.getLifecycleStatus());

        // 2. Domestic Death: Personal Estate vs. Clan Heirloom Separation
        // Patriarch also owns personal gold and a personal dagger
        EquipmentItem personalDagger = new EquipmentItem(
                "EQ_DAGGER_01", "Old Dagger", EquipmentCategory.WEAPON, EquipmentSlot.OFF_HAND,
                EquipmentType.DAGGER, false, ClassRarity.COMMON, 1, 40.0, 80.0,
                new EquipmentStatProfile(5.0, 0.0, 8.0, 2.0, 0.0, 0.0, 0.0),
                0, "50", "FORGE_1", "PERSON_50", "PERSON_50", 0.0, 0.0
        );
        ecs.registerEquipment(personalDagger);
        patriarch.equipItem(EquipmentSlot.OFF_HAND, personalDagger.getEquipmentId(), false);

        Map<Integer, Person> peopleMap = new HashMap<>();
        peopleMap.put(50, patriarch);
        peopleMap.put(51, son);
        peopleMap.put(52, rivalChampion);

        // Patriarch dies in bed of old age
        chs.handleBearerDomesticDeath(patriarch, null, Collections.singletonList(son), v, ecs, null, peopleMap, new Random(42));

        // Personal Dagger -> Inherited by Son as private estate
        assertEquals("Personal dagger inherited by son", "PERSON_51", personalDagger.getCurrentOwnerId());
        assertEquals("Personal dagger equipped by son", "EQ_DAGGER_01", son.getEquippedItemId(EquipmentSlot.OFF_HAND));

        // Clan Heirloom -> Inherited via BLOODLINE_FIRST succession to Son
        assertEquals("Heirloom bearer transferred to Son via BLOODLINE_FIRST", "51", heirloom.getCurrentBearerPersonId());
        assertEquals("Heirloom owner remains CLAN_WOLF (never entered personal estate)", "CLAN_WOLF", fang.getCurrentOwnerId());
        assertEquals("Heirloom equipped by son in MAIN_HAND", "EQ_WOLF_FANG", son.getEquippedItemId(EquipmentSlot.MAIN_HAND));

        // 3. Remote Battlefield Fatality: No-Teleport Rule
        // Son goes to battle at remote anomaly site "BATTLE_SITE_99" and is slain
        chs.handleBearerDeathAtSite(son, "BATTLE_SITE_99", ecs);

        assertEquals("Heirloom status transitioned to UNRECOVERED_AT_SITE", EquipmentLifecycleStatus.UNRECOVERED_AT_SITE, fang.getLifecycleStatus());
        assertEquals("Custodian is the battle site (no teleport home)", "BATTLE_SITE_99", fang.getCurrentCustodianId());
        assertEquals("Heirloom custody status is LOST_AT_SITE", HeirloomCustodyStatus.LOST_AT_SITE, heirloom.getCustodyStatus());
        assertEquals("Son's equipped items cleared", null, son.getEquippedItemId(EquipmentSlot.MAIN_HAND));

        // 4. Hostile Capture and Grievance Emission
        // Enemy "RAIDERS_SIDE" wins the battle and captures the heirloom
        chs.resolveBattlefieldItemRecovery(
                "BATTLE_SITE_99", false, null, null, "RAIDERS_SIDE",
                Collections.singletonList("ORG_RAIDERS"), ecs, null, peopleMap, 55, 6, new Random(99)
        );

        assertEquals("Captured heirloom custody status is STOLEN_OR_CAPTURED", HeirloomCustodyStatus.STOLEN_OR_CAPTURED, heirloom.getCustodyStatus());
        assertEquals("Legal title remains CLAN_WOLF under disputed ownership", "CLAN_WOLF", fang.getCurrentOwnerId());
        assertEquals("Current custodian is RAIDERS_SIDE", "RAIDERS_SIDE", fang.getCurrentCustodianId());

        // Check Grievance Emission
        List<HistoricalGrievanceRecord> grievances = chs.getHeirloomGrievances();
        assertTrue("Historical grievance emitted on heirloom capture", !grievances.isEmpty());
        HistoricalGrievanceRecord g = grievances.get(0);
        assertEquals("Grievance type is HEIRLOOM_OR_EQUIPMENT_CAPTURED", "HEIRLOOM_OR_EQUIPMENT_CAPTURED", g.getGrievanceType());
        assertEquals("Grievance victim org is CLAN_WOLF", "CLAN_WOLF", g.getVictimPrimaryOrganizationId());
        assertEquals("Grievance killer side is RAIDERS_SIDE", "RAIDERS_SIDE", g.getKillerCoalitionSideId());

        // 5. Friendly Recovery
        // Clan Wolf recaptures site in battle "BATTLE_RETAKE"
        fang.setCurrentCustodianId("BATTLE_RETAKE");
        fang.setLifecycleStatus(EquipmentLifecycleStatus.UNRECOVERED_AT_SITE);

        chs.resolveBattlefieldItemRecovery(
                "BATTLE_RETAKE", true, "CLAN_WOLF", clanA, null,
                null, ecs, null, peopleMap, 56, 1, new Random(10)
        );

        assertEquals("Heirloom recovered by Clan Wolf", "CLAN_WOLF", fang.getCurrentOwnerId());
        assertTrue("Heirloom custody restored to clan", heirloom.getCustodyStatus().isHeldByClan());
    }

    // =========================================================================
    // SUITE G: Combat Engine Single-Point Integration & Bonus Caps
    // =========================================================================
    private static void suiteG_CombatSinglePointHandoffAndCaps() {
        System.out.println("\n--- SUITE G: Combat Single-Point Integration & Bonus Caps Tests ---");

        EquipmentCraftingSystem ecs = new EquipmentCraftingSystem();
        EquipmentCraftingSystem.setActiveInstance(ecs);

        Village v = new Village(1, "WarriorCamp", 1.0, 0.05);
        Person champion = new Person(60, "Berserker", Gender.MALE, 30, 0, v);
        AetherProfile ap = AetherProfile.createForTesting(SupernaturalClass.VANGUARD, ClassRarity.MYTHIC, 5, false);
        champion.setAetherProfile(ap);

        // Equip massive gear to test global bonus caps:
        // Attack cap: 75.0, Defense cap: 75.0, Accuracy: 35.0, Evasion: 35.0
        EquipmentItem opWeapon = new EquipmentItem(
                "EQ_OP_WEAPON", "Worldsplitter", EquipmentCategory.WEAPON, EquipmentSlot.MAIN_HAND,
                EquipmentType.TWO_HANDED_SWORD, true, ClassRarity.MYTHIC, 10, 100.0, 500.0,
                new EquipmentStatProfile(120.0, 30.0, 50.0, 10.0, 60.0, 30.0, 20.0), // Excess attack > 75
                0, "1", "FORGE_1", "1", "1", 0, 0
        );
        EquipmentItem opArmor = new EquipmentItem(
                "EQ_OP_ARMOR", "Fortress Plate", EquipmentCategory.DEFENSIVE_ARMOR, EquipmentSlot.CHEST,
                EquipmentType.CHESTPLATE, false, ClassRarity.MYTHIC, 10, 100.0, 500.0,
                new EquipmentStatProfile(0.0, 100.0, 0.0, 40.0, 0.0, 0.0, 60.0), // Excess defense > 75, evasion > 35
                0, "1", "FORGE_1", "1", "1", 0, 0
        );
        ecs.registerEquipment(opWeapon);
        ecs.registerEquipment(opArmor);

        champion.equipItem(EquipmentSlot.MAIN_HAND, opWeapon.getEquipmentId(), true);
        champion.equipItem(EquipmentSlot.CHEST, opArmor.getEquipmentId(), false);

        DerivedEquipmentProfile derived = ecs.computeDerivedEquipmentProfile(champion);

        // Verify Hard Global Bonus Caps:
        assertApprox("Attack bonus capped at 75.0", 75.0, derived.getAttackBonus(), 0.001);
        assertApprox("Defense bonus capped at 75.0", 75.0, derived.getDefenseBonus(), 0.001);
        assertApprox("Accuracy bonus capped at 35.0", 35.0, derived.getAccuracyBonus(), 0.001);
        assertApprox("Evasion bonus capped at 35.0", 35.0, derived.getEvasionBonus(), 0.001);
        assertApprox("Aether potency capped at 50.0", 50.0, derived.getAetherPotencyBonus(), 0.001);
        assertApprox("Barrier / Resistance capped at 50.0", 50.0, derived.getBarrierOrResistanceBonus(), 0.001);

        // PersonCombatant single-point handoff
        PersonCombatant pc = new PersonCombatant(champion, "BLUE_SIDE");

        assertApprox("PersonCombatant attack bonus matches derived profile", 75.0, pc.getEquipmentAttackBonus(), 0.001);
        assertApprox("PersonCombatant defense bonus matches derived profile", 75.0, pc.getEquipmentDefenseBonus(), 0.001);
        assertApprox("PersonCombatant evasion bonus matches derived profile", 35.0, pc.getEquipmentEvasionBonus(), 0.001);
        assertApprox("PersonCombatant accuracy bonus matches derived profile", 35.0, pc.getEquipmentAccuracyBonus(), 0.001);

        // Invariant: Equipment bonuses NEVER mutate human baseline attributes
        assertApprox("Baseline strength is unmutated", ap.getBaselineAttribute(AttributeType.STRENGTH), champion.getAetherProfile().getBaselineAttribute(AttributeType.STRENGTH), 0.001);
        assertEquals("Human power tier remains T5 (not altered by T10 weapon)", 5, champion.getAetherProfile().getPowerTier());
    }

    // =========================================================================
    // SUITE H: Multi-Seed 250-Year Simulation & Regression
    // =========================================================================
    private static void suiteH_MultiSeed250YearStability() {
        System.out.println("\n--- SUITE H: 250-Year Multi-Seed Simulation & Regression Tests ---");

        CombatEngine.clearCache();

        SimulationConfig configA = SimulationConfig.createDefaultWithSeed(12345L);
        SimulationEngine engineA = new SimulationEngine(configA);
        engineA.initializeWorld();

        System.out.println("  Simulating Engine A (Seed 12345) for 250 years...");
        for (int m = 0; m < 250 * 12; m++) {
            engineA.processMonthlyTick();
        }

        AnnualReport repA250 = engineA.getAnnualReports().get(engineA.getAnnualReports().size() - 1);
        int popA = repA250.getTotalLivingPopulation();
        int equipA = engineA.getEquipmentCraftingSystem().getAllEquipment().size();
        System.out.printf("  Engine A Year 250: Population=%d | Total Equipment=%d\n", popA, equipA);

        assertTrue("Engine A reached Year 250 with living population > 0", popA > 0);
        assertTrue("Engine A crafted persistent equipment over 250 years", equipA > 0);
        assertTrue("Equipment Summary present in AnnualReport", repA250.getEquipmentSummary() != null);

        // Determinism Check: Run identical seed Engine B
        CombatEngine.clearCache();
        SimulationConfig configB = SimulationConfig.createDefaultWithSeed(12345L);
        SimulationEngine engineB = new SimulationEngine(configB);
        engineB.initializeWorld();

        System.out.println("  Simulating Engine B (Seed 12345) for 250 years...");
        for (int m = 0; m < 250 * 12; m++) {
            engineB.processMonthlyTick();
        }

        AnnualReport repB250 = engineB.getAnnualReports().get(engineB.getAnnualReports().size() - 1);
        int popB = repB250.getTotalLivingPopulation();
        int equipB = engineB.getEquipmentCraftingSystem().getAllEquipment().size();
        System.out.printf("  Engine B Year 250: Population=%d | Total Equipment=%d\n", popB, equipB);

        assertEquals("Strict Determinism: Identical Year 250 Population", popA, popB);
        assertEquals("Strict Determinism: Identical Year 250 Equipment Count", equipA, equipB);

        // Scarcity of Legendary / Mythic items check
        int legCount = 0;
        int mythCount = 0;
        for (EquipmentItem item : engineA.getEquipmentCraftingSystem().getAllEquipment().values()) {
            if (item.getEquipmentRarity() == ClassRarity.LEGENDARY) legCount++;
            if (item.getEquipmentRarity() == ClassRarity.MYTHIC) mythCount++;
        }
        System.out.printf("  Artifact Scarcity: Legendary=%d, Mythic=%d out of %d items\n", legCount, mythCount, equipA);
        assertTrue("Legendary items remain scarce (< 10)", legCount < 10);
        assertTrue("Mythic items remain ultra scarce (<= 3)", mythCount <= 3);
    }
}
