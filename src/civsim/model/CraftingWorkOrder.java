package civsim.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Phase 2C.1 -- Explicit lifecycle work order tracking crafting, enchanting, upgrading, or repairs.
 * Guarantees strict resource conservation through atomic reservation and explicit release on cancellation.
 */
public class CraftingWorkOrder {

    private final String workOrderId;
    private final CraftingWorkOrderType orderType;
    private final String facilityId;
    private final String settlementId;
    private final String organizationId;
    private final String crafterPersonId;

    private final String recipeId;
    private final EquipmentRecipe recipe;
    private final String targetEquipmentId;
    private final EnchantmentRecord pendingEnchantment;
    private final int targetUpgradeTier;

    private CraftingWorkOrderStatus status;
    private final int startYear;
    private final int startMonth;
    private final int totalMonthsRequired;
    private int monthsProgressed;

    // Reserved Resources
    private final double reservedIron;
    private final double reservedOre;
    private final double reservedWood;
    private final double reservedTools;
    private final Map<SupernaturalResourceType, Double> reservedSupernatural;
    private final Map<MonsterMaterialType, Double> reservedMonster;
    private final ResourceGrade reservedCoreGrade;

    // Completed Output
    private EquipmentItem createdItem;

    public CraftingWorkOrder(
            String workOrderId,
            CraftingWorkOrderType orderType,
            String facilityId,
            String settlementId,
            String organizationId,
            String crafterPersonId,
            String recipeId,
            EquipmentRecipe recipe,
            String targetEquipmentId,
            EnchantmentRecord pendingEnchantment,
            int targetUpgradeTier,
            int startYear,
            int startMonth,
            int totalMonthsRequired,
            double reservedIron,
            double reservedOre,
            double reservedWood,
            double reservedTools,
            Map<SupernaturalResourceType, Double> reservedSupernatural,
            Map<MonsterMaterialType, Double> reservedMonster,
            ResourceGrade reservedCoreGrade) {
        if (workOrderId == null) throw new IllegalArgumentException("Work order ID cannot be null.");
        this.workOrderId = workOrderId;
        this.orderType = orderType != null ? orderType : CraftingWorkOrderType.NEW_CRAFT;
        this.facilityId = facilityId;
        this.settlementId = settlementId;
        this.organizationId = organizationId;
        this.crafterPersonId = crafterPersonId;
        this.recipeId = recipeId;
        this.recipe = recipe;
        this.targetEquipmentId = targetEquipmentId;
        this.pendingEnchantment = pendingEnchantment;
        this.targetUpgradeTier = targetUpgradeTier;
        this.status = CraftingWorkOrderStatus.MATERIALS_RESERVED;
        this.startYear = startYear;
        this.startMonth = startMonth;
        this.totalMonthsRequired = Math.max(1, totalMonthsRequired);
        this.monthsProgressed = 0;
        this.reservedIron = reservedIron;
        this.reservedOre = reservedOre;
        this.reservedWood = reservedWood;
        this.reservedTools = reservedTools;
        this.reservedSupernatural = (reservedSupernatural != null) ? new EnumMap<>(reservedSupernatural) : new EnumMap<>(SupernaturalResourceType.class);
        this.reservedMonster = (reservedMonster != null) ? new EnumMap<>(reservedMonster) : new EnumMap<>(MonsterMaterialType.class);
        this.reservedCoreGrade = reservedCoreGrade;
        this.createdItem = null;
    }

    public String getWorkOrderId() { return workOrderId; }
    public CraftingWorkOrderType getOrderType() { return orderType; }
    public String getFacilityId() { return facilityId; }
    public String getSettlementId() { return settlementId; }
    public String getOrganizationId() { return organizationId; }
    public String getCrafterPersonId() { return crafterPersonId; }
    public String getRecipeId() { return recipeId; }
    public EquipmentRecipe getRecipe() { return recipe; }
    public String getTargetEquipmentId() { return targetEquipmentId; }
    public EnchantmentRecord getPendingEnchantment() { return pendingEnchantment; }
    public int getTargetUpgradeTier() { return targetUpgradeTier; }

    public CraftingWorkOrderStatus getStatus() { return status; }
    public void setStatus(CraftingWorkOrderStatus status) { this.status = status; }
    public int getStartYear() { return startYear; }
    public int getStartMonth() { return startMonth; }
    public int getTotalMonthsRequired() { return totalMonthsRequired; }
    public int getMonthsProgressed() { return monthsProgressed; }
    public void setMonthsProgressed(int months) { this.monthsProgressed = months; }

    public double getReservedIron() { return reservedIron; }
    public double getReservedOre() { return reservedOre; }
    public double getReservedWood() { return reservedWood; }
    public double getReservedTools() { return reservedTools; }
    public Map<SupernaturalResourceType, Double> getReservedSupernatural() { return Collections.unmodifiableMap(reservedSupernatural); }
    public Map<MonsterMaterialType, Double> getReservedMonster() { return Collections.unmodifiableMap(reservedMonster); }
    public ResourceGrade getReservedCoreGrade() { return reservedCoreGrade; }

    public EquipmentItem getCreatedItem() { return createdItem; }
    public void setCreatedItem(EquipmentItem item) { this.createdItem = item; }

    public boolean advanceProgress() {
        if (status == CraftingWorkOrderStatus.MATERIALS_RESERVED) {
            status = CraftingWorkOrderStatus.IN_PROGRESS;
        }
        if (status == CraftingWorkOrderStatus.IN_PROGRESS) {
            monthsProgressed++;
            return monthsProgressed >= totalMonthsRequired;
        }
        return false;
    }
}

