package civsim.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Phase 2C.1 -- Persistent, physically unique equipment entity.
 * Enforces the Single Physical Identity Invariant: an item is never duplicated
 * or
 * simultaneously equipped and in inventory.
 */
public class EquipmentItem {

    private final String equipmentId;
    private String displayName;
    private String artifactTitle;

    private final EquipmentCategory category;
    private EquipmentSlot slot;
    private final EquipmentType type;
    private final boolean twoHanded;

    private ClassRarity equipmentRarity;
    private int equipmentTier;
    private double craftQuality;
    private double durability;
    private double maximumDurability;
    private EquipmentCondition condition;
    private EquipmentLifecycleStatus lifecycleStatus;

    private final Map<String, Double> materialComposition = new HashMap<>();
    private EquipmentStatProfile baseStatBonuses;
    private final List<EnchantmentRecord> enchantments = new ArrayList<>();

    // Compatibility Gates
    private final Set<SupernaturalClass> allowedClasses = new HashSet<>();
    private int minimumAwakenedTier = 0;
    private AetherAspect requiredAspect = null;
    private double minAetherCapacity = 0.0;
    private double minAetherControl = 0.0;
    private double minWillpower = 0.0;

    // Provenance
    private final int creationTotalMonths;
    private final String creatorPersonId;
    private final String creationFacilityId;

    // Ownership & Custody
    private String currentOwnerId;
    private String currentCustodianId;
    private double posX;
    private double posY;

    // Clan Heirloom Tracking
    private boolean registeredHeirloom = false;
    private String heirloomId = null;

    public EquipmentItem(
            String equipmentId,
            String displayName,
            EquipmentCategory category,
            EquipmentSlot slot,
            EquipmentType type,
            boolean twoHanded,
            ClassRarity equipmentRarity,
            int equipmentTier,
            double craftQuality,
            double maximumDurability,
            EquipmentStatProfile baseStatBonuses,
            int creationTotalMonths,
            String creatorPersonId,
            String creationFacilityId,
            String currentOwnerId,
            String currentCustodianId,
            double posX,
            double posY) {
        if (equipmentId == null || equipmentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Equipment ID cannot be null or empty.");
        }
        this.equipmentId = equipmentId;
        this.displayName = displayName != null ? displayName : equipmentId;
        this.artifactTitle = null;
        this.category = category != null ? category : EquipmentCategory.WEAPON;
        this.slot = slot != null ? slot : EquipmentSlot.MAIN_HAND;
        this.type = type != null ? type : EquipmentType.ONE_HANDED_SWORD;
        this.twoHanded = twoHanded;
        this.equipmentRarity = equipmentRarity != null ? equipmentRarity : ClassRarity.COMMON;

        // Enforce maximum tier ceiling for rarity
        int maxTier = getMaxTierForRarity(this.equipmentRarity);
        this.equipmentTier = Math.max(1, Math.min(maxTier, equipmentTier));

        this.craftQuality = Math.max(0.0, Math.min(100.0, craftQuality));
        this.maximumDurability = Math.max(10.0, Math.min(500.0, maximumDurability));
        this.durability = this.maximumDurability;
        this.condition = EquipmentCondition.EXCELLENT;
        this.lifecycleStatus = EquipmentLifecycleStatus.IN_INVENTORY;

        this.baseStatBonuses = baseStatBonuses != null ? baseStatBonuses : EquipmentStatProfile.zero();
        this.creationTotalMonths = creationTotalMonths;
        this.creatorPersonId = creatorPersonId;
        this.creationFacilityId = creationFacilityId;
        this.currentOwnerId = currentOwnerId;
        this.currentCustodianId = currentCustodianId;
        this.posX = posX;
        this.posY = posY;
    }

    public static int getMaxTierForRarity(ClassRarity rarity) {
        if (rarity == null)
            return 2;
        switch (rarity) {
            case COMMON:
                return 2;
            case UNCOMMON:
                return 3;
            case RARE:
                return 5;
            case LEGENDARY:
                return 8;
            case MYTHIC:
                return 10;
            default:
                return 2;
        }
    }

    public static int getMaxEnchantmentSlotsForRarity(ClassRarity rarity) {
        if (rarity == null)
            return 0;
        switch (rarity) {
            case COMMON:
                return 0;
            case UNCOMMON:
                return 1;
            case RARE:
                return 2;
            case LEGENDARY:
                return 3;
            case MYTHIC:
                return 4;
            default:
                return 0;
        }
    }

    public int getMaxEnchantmentSlots() {
        return getMaxEnchantmentSlotsForRarity(this.equipmentRarity);
    }

    public boolean canAcceptEnchantment(EnchantmentFamily family) {
        if (family == null)
            return false;
        if (enchantments.size() >= getMaxEnchantmentSlots()) {
            return false;
        }
        for (EnchantmentRecord rec : enchantments) {
            if (rec.getFamily() == family) {
                return false; // Strict rule: max one enchantment per family
            }
        }
        return true;
    }

    public boolean addEnchantment(EnchantmentRecord enchantment) {
        if (enchantment == null)
            return false;
        if (!canAcceptEnchantment(enchantment.getFamily())) {
            return false;
        }
        enchantments.add(enchantment);
        return true;
    }

    public void applyDurabilityLoss(double loss) {
        if (loss <= 0.0)
            return;
        this.durability = Math.max(0.0, this.durability - loss);
        this.condition = EquipmentCondition.fromDurability(this.durability, this.maximumDurability);
    }

    public void repair(double amount) {
        if (amount <= 0.0)
            return;
        this.durability = Math.min(this.maximumDurability, this.durability + amount);
        this.condition = EquipmentCondition.fromDurability(this.durability, this.maximumDurability);
    }

    public boolean upgradeTier(int newTier) {
        int maxTier = getMaxTierForRarity(this.equipmentRarity);
        if (newTier > this.equipmentTier && newTier <= maxTier) {
            this.equipmentTier = newTier;
            return true;
        }
        return false;
    }

    public boolean isCompatibleWith(Person person) {
        if (person == null || !person.isAlive())
            return false;

        // Check if combat incapacitated
        CombatState cs = person.getCombatState();
        if (cs != null && cs.isIncapacitated()) {
            return false;
        }

        // Check awakened tier requirement
        int personTier = person.isAwakened() && person.getAetherProfile() != null
                ? person.getAetherProfile().getPowerTier()
                : 0;
        if (personTier < minimumAwakenedTier) {
            return false;
        }

        // Check class requirement
        if (!allowedClasses.isEmpty()) {
            SupernaturalClass sc = person.isAwakened() && person.getAetherProfile() != null
                    ? person.getAetherProfile().getSupernaturalClass()
                    : null;
            if (sc == null || !allowedClasses.contains(sc)) {
                return false;
            }
        }

        // Check aspect requirement
        if (requiredAspect != null) {
            AetherProfile ap = person.getAetherProfile();
            AetherAspect asp = null;
            if (ap != null && ap.isDarkAwakened()) {
                asp = AetherAspect.DARK;
            } else if (ap != null && ap.isAwakened()) {
                asp = AetherAspect.AETHERIC;
            }
            if (asp != requiredAspect) {
                return false;
            }
        }

        // Check attribute requirements
        if (minAetherCapacity > 0.0
                && person.getEffectiveAttribute(AttributeType.AETHER_CAPACITY) < minAetherCapacity) {
            return false;
        }
        if (minAetherControl > 0.0 && person.getEffectiveAttribute(AttributeType.AETHER_CONTROL) < minAetherControl) {
            return false;
        }
        if (minWillpower > 0.0 && person.getEffectiveAttribute(AttributeType.WILLPOWER) < minWillpower) {
            return false;
        }

        return true;
    }

    // Getters and Setters
    public String getEquipmentId() {
        return equipmentId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getArtifactTitle() {
        return artifactTitle;
    }

    public void setArtifactTitle(String artifactTitle) {
        this.artifactTitle = artifactTitle;
    }

    public EquipmentCategory getCategory() {
        return category;
    }

    public EquipmentSlot getSlot() {
        return slot;
    }

    public void setSlot(EquipmentSlot slot) {
        if (slot != null)
            this.slot = slot;
    }

    public EquipmentType getType() {
        return type;
    }

    public boolean isTwoHanded() {
        return twoHanded;
    }

    public ClassRarity getEquipmentRarity() {
        return equipmentRarity;
    }

    public void setEquipmentRarity(ClassRarity equipmentRarity) {
        this.equipmentRarity = equipmentRarity;
    }

    public int getEquipmentTier() {
        return equipmentTier;
    }

    public double getCraftQuality() {
        return craftQuality;
    }

    public void setCraftQuality(double craftQuality) {
        this.craftQuality = Math.max(0.0, Math.min(100.0, craftQuality));
    }

    public double getDurability() {
        return durability;
    }

    public double getMaximumDurability() {
        return maximumDurability;
    }

    public void setMaximumDurability(double max) {
        this.maximumDurability = Math.max(10.0, Math.min(500.0, max));
    }

    public EquipmentCondition getCondition() {
        return condition;
    }

    public EquipmentLifecycleStatus getLifecycleStatus() {
        return lifecycleStatus;
    }

    public void setLifecycleStatus(EquipmentLifecycleStatus status) {
        this.lifecycleStatus = status;
    }

    public Map<String, Double> getMaterialComposition() {
        return Collections.unmodifiableMap(materialComposition);
    }

    public void addMaterialComposition(String material, double amount) {
        materialComposition.put(material, materialComposition.getOrDefault(material, 0.0) + amount);
    }

    public EquipmentStatProfile getBaseStatBonuses() {
        return baseStatBonuses;
    }

    public void setBaseStatBonuses(EquipmentStatProfile baseStatBonuses) {
        this.baseStatBonuses = baseStatBonuses;
    }

    public List<EnchantmentRecord> getEnchantments() {
        return Collections.unmodifiableList(enchantments);
    }

    public Set<SupernaturalClass> getAllowedClasses() {
        return Collections.unmodifiableSet(allowedClasses);
    }

    public void addAllowedClass(SupernaturalClass sc) {
        if (sc != null)
            allowedClasses.add(sc);
    }

    public int getMinimumAwakenedTier() {
        return minimumAwakenedTier;
    }

    public void setMinimumAwakenedTier(int tier) {
        this.minimumAwakenedTier = tier;
    }

    public AetherAspect getRequiredAspect() {
        return requiredAspect;
    }

    public void setRequiredAspect(AetherAspect requiredAspect) {
        this.requiredAspect = requiredAspect;
    }

    public double getMinAetherCapacity() {
        return minAetherCapacity;
    }

    public void setMinAetherCapacity(double val) {
        this.minAetherCapacity = val;
    }

    public double getMinAetherControl() {
        return minAetherControl;
    }

    public void setMinAetherControl(double val) {
        this.minAetherControl = val;
    }

    public double getMinWillpower() {
        return minWillpower;
    }

    public void setMinWillpower(double val) {
        this.minWillpower = val;
    }

    public int getCreationTotalMonths() {
        return creationTotalMonths;
    }

    public String getCreatorPersonId() {
        return creatorPersonId;
    }

    public String getCreationFacilityId() {
        return creationFacilityId;
    }

    public String getCurrentOwnerId() {
        return currentOwnerId;
    }

    public void setCurrentOwnerId(String currentOwnerId) {
        this.currentOwnerId = currentOwnerId;
    }

    public String getCurrentCustodianId() {
        return currentCustodianId;
    }

    public void setCurrentCustodianId(String currentCustodianId) {
        this.currentCustodianId = currentCustodianId;
    }

    public double getPosX() {
        return posX;
    }

    public double getPosY() {
        return posY;
    }

    public void setPosition(double x, double y) {
        this.posX = x;
        this.posY = y;
    }

    public boolean isRegisteredHeirloom() {
        return registeredHeirloom;
    }

    public void setRegisteredHeirloom(boolean registeredHeirloom) {
        this.registeredHeirloom = registeredHeirloom;
    }

    public String getHeirloomId() {
        return heirloomId;
    }

    public void setHeirloomId(String heirloomId) {
        this.heirloomId = heirloomId;
    }
}
